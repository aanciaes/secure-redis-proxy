package anciaes.secure.redis.service.redis.unsecure

import anciaes.secure.redis.exceptions.BrokenSecurityException
import anciaes.secure.redis.exceptions.KeyNotFoundException
import anciaes.secure.redis.exceptions.ValueWronglyFormatted
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RedisResponses
import anciaes.secure.redis.model.SecureValueType
import anciaes.secure.redis.model.ZRangeTuple
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.utils.KeystoreUtils
import anciaes.secure.redis.utils.SSLUtils
import hlib.hj.mlib.HomoAdd
import hlib.hj.mlib.HomoDet
import hlib.hj.mlib.HomoOpeInt
import java.nio.charset.Charset
import java.security.Signature
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.Mac
import redis.clients.jedis.Jedis
import redis.clients.jedis.params.SetParams

class SecureRedisServiceImpl(val props: ApplicationProperties) : RedisService {

    private val jedis: Jedis = buildJedisClient(props)

    // Homo Det Keys
    private val homoDetKey = HomoDet.keyFromString(props.keyEncryptionDetSecret)

    // Homo Add Key
    private val homoAddKey = HomoAdd.keyFromString(props.keyEncryptionAddSecret)

    // Homo Ope Int Keys
    val homoOpeKey = HomoOpeInt(props.keyEncryptionOpeSecret)

    // Load KeySpecs
    private val encryptionKey = KeystoreUtils.getKeyFromKeyStore(
        props.dataEncryptionKeystoreType!!,
        props.dataEncryptionKeystore!!,
        props.dataEncryptionKeystorePassword!!,
        props.dataEncryptionKeystoreKeyName!!,
        props.dataEncryptionKeystoreKeyPassword!!
    )
    private val integrityKey = KeystoreUtils.getKeyFromKeyStore(
        props.dataHMacKeystoreType!!,
        props.dataHMacKeystore!!,
        props.dataHMacKeystorePassword!!,
        props.dataHMacKeyName!!,
        props.dataHMacKeyPassword!!
    )
    private val signingKey = KeystoreUtils.getKeyPairFromKeyStore(
        props.dataSignatureKeystoreType!!,
        props.dataSignatureKeystore!!,
        props.dataSignatureKeystorePassword!!,
        props.dataSignatureKeystoreKeyName!!,
        props.dataSignatureKeystoreKeyPassword!!
    )

    init {
        val redisAuth = props.redisAuthentication
        val username = props.redisUsername
        val password = props.redisPassword

        if (redisAuth) {
            if (!password.isNullOrBlank()) {
                // Backwards compatibility. For older Redis versions that do not support ACL
                if (username.isNullOrBlank()) {
                    jedis.auth(password)
                } else {
                    jedis.auth(username, password)
                }
            }
        }

        if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
    }

    override fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val secureValue = computeSecureValue(value)

        return if (expiration != null) {
            val expirationParams = SetParams()
            when (timeUnit) {
                TimeUnit.MILLISECONDS -> expirationParams.px(expiration)
                else -> expirationParams.ex(expiration.toInt())
            }

            jedis.set(encryptedKey, secureValue, expirationParams)
        } else {
            jedis.set(encryptedKey, secureValue)
        }
    }

    override fun get(key: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        return when (val value = jedis.get(encryptedKey)) {
            null -> RedisResponses.NIL
            else -> getSecureValue(value)
        }
    }

    override fun del(key: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        return if (jedis.del(encryptedKey) == 1L) RedisResponses.OK else RedisResponses.NOK
    }

    override fun zadd(key: String, score: Double, value: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)

        val scoreInt = score.toInt()
        val encryptedScoreDouble = homoOpeKey.encrypt(scoreInt).toDouble()

        val secureValue = computeSecureValue(value)
        return if (jedis.zadd(
                encryptedKey,
                encryptedScoreDouble,
                secureValue
            ) == 1L
        ) RedisResponses.OK else RedisResponses.NOK
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<ZRangeTuple> {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        var encryptedMin = "-inf"
        var encryptedMax = "+inf"

        if (min != "-inf" && min != "+inf" && min != "inf" && max != "-inf" && max != "+inf" && max != "inf") {
            encryptedMin = homoOpeKey.encrypt(min.toInt()).toString()
            encryptedMax = homoOpeKey.encrypt(max.toInt()).toString()
        }

        val res = jedis.zrangeByScoreWithScores(encryptedKey, encryptedMin, encryptedMax)
        return res.map { ZRangeTuple(getSecureValue(it.element), homoOpeKey.decrypt(it.score.toLong()).toDouble()) }
    }

    override fun sum(key: String, value: Int): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val redisLine = jedis.get(encryptedKey) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val composite = redisLine.split("|")

        composite.first().let {
            if (SecureValueType.valueOf(it) != SecureValueType.ADD) {
                throw ValueWronglyFormatted("Cannot make arithmetic operations with non number values")
            }
        }

        val secureValueToSum = HomoAdd.encrypt(value.toBigInteger(), homoAddKey)
        val secureValue = composite[1].toBigInteger()
        val secureSum = HomoAdd.sum(secureValue, secureValueToSum, homoAddKey.nsquare)

        val signedValue = signData(secureSum.toString())

        val compositeValue = "${SecureValueType.ADD}|$secureSum|$signedValue"
        val hash = computeIntegrityHash(compositeValue)
        return jedis.set(encryptedKey, "$compositeValue|$hash")
    }

    override fun diff(key: String, value: Int): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val redisLine = jedis.get(encryptedKey) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val composite = redisLine.split("|")

        composite.first().let {
            if (SecureValueType.valueOf(it) != SecureValueType.ADD) {
                throw ValueWronglyFormatted("Cannot make arithmetic operations with non number values")
            }
        }

        val secureValueToSubtract = HomoAdd.encrypt(value.toBigInteger(), homoAddKey)
        val secureValue = composite[1].toBigInteger()
        val secureSum = HomoAdd.dif(secureValue, secureValueToSubtract, homoAddKey.nsquare)

        val signedValue = signData(secureSum.toString())

        val compositeValue = "${SecureValueType.ADD}|$secureSum|$signedValue"
        val hash = computeIntegrityHash(compositeValue)
        return jedis.set(encryptedKey, "$compositeValue|$hash")
    }

    override fun mult(key: String, value: Int): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val redisLine = jedis.get(encryptedKey) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val composite = redisLine.split("|")

        composite.first().let {
            if (SecureValueType.valueOf(it) != SecureValueType.ADD) {
                throw ValueWronglyFormatted("Cannot make arithmetic operations with non number values")
            }
        }

        val secureValue = composite[1].toBigInteger()
        val secureSum = HomoAdd.mult(secureValue, value, homoAddKey.nsquare)

        val signedValue = signData(secureSum.toString())

        val compositeValue = "${SecureValueType.ADD}|$secureSum|$signedValue"
        val hash = computeIntegrityHash(compositeValue)
        return jedis.set(encryptedKey, "$compositeValue|$hash")
    }

    override fun flushAll(): String {
        return jedis.flushAll()
    }

    private fun computeSecureValue(value: String): String {
        val arithmeticValue = value.toBigIntegerOrNull()
        var valueType = SecureValueType.RND

        val encryptedValue = if (arithmeticValue == null) {
            encryptValue(value)
        } else {
            valueType = SecureValueType.ADD
            HomoAdd.encrypt(arithmeticValue, homoAddKey).toString()
        }
        val signedValue = signData(encryptedValue)

        val compositeValue = "$valueType|$encryptedValue|$signedValue"
        val hash = computeIntegrityHash(compositeValue)
        return "$compositeValue|$hash"
    }

    private fun getSecureValue(secureValue: String): String {
        val composite = secureValue.split("|")
        val valueType = SecureValueType.valueOf(composite.first())

        val integrityCheck = computeIntegrityHash("${composite[0]}|${composite[1]}|${composite[2]}")
        if (integrityCheck != composite[3]) {
            throw BrokenSecurityException("Integrity Validation Failed... Data was tampered.")
        }

        return if (verifySignature(composite[1], composite[2])) {
            decryptValue(valueType, composite[1])
        } else {
            throw BrokenSecurityException("Error verifying authenticity...")
        }
    }

    private fun encryptValue(value: String): String {
        val cipher: Cipher = Cipher.getInstance(props.dataEncryptionCipherSuite, props.dataEncryptionProvider)

        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        cipher.update(value.toByteArray(Charset.defaultCharset()))

        val encryptedBytes = cipher.doFinal()
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    private fun decryptValue(secureValueType: SecureValueType, cipherText: String): String {
        return when (secureValueType) {
            SecureValueType.RND -> {
                val cipher: Cipher = Cipher.getInstance(props.dataEncryptionCipherSuite, props.dataEncryptionProvider)

                cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
                cipher.update(Base64.getDecoder().decode(cipherText))

                val encryptedBytes = cipher.doFinal()
                String(encryptedBytes)
            }
            SecureValueType.ADD -> {
                HomoAdd.decrypt(cipherText.toBigInteger(), homoAddKey).toString()
            }
        }
    }

    private fun signData(data: String): String {
        val signature: Signature = Signature.getInstance(
            props.dataSignatureAlgorithm,
            props.dataSignatureProvider
        )

        signature.initSign(signingKey.private)

        signature.update(data.toByteArray())
        val sigBytes: ByteArray = signature.sign()

        return Base64.getEncoder().encodeToString(sigBytes)
    }

    private fun verifySignature(data: String, signatureString: String): Boolean {
        val signature: Signature = Signature.getInstance(
            props.dataSignatureAlgorithm,
            props.dataSignatureProvider
        )

        signature.initVerify(signingKey.public)

        signature.update(data.toByteArray())

        return signature.verify(Base64.getDecoder().decode(signatureString))
    }

    private fun computeIntegrityHash(test: String): String {
        val hMac = Mac.getInstance(props.dataHMacAlgorithm, props.dataHMacProvider)

        hMac.init(integrityKey)
        hMac.update(test.toByteArray())

        return Base64.getEncoder().encodeToString(hMac.doFinal())
    }

    private fun buildJedisClient(applicationProperties: ApplicationProperties): Jedis {

        return if (applicationProperties.tlsEnabled) {
            val clientKeyStore = applicationProperties.tlsKeystorePath
            val clientKeyStorePassword = applicationProperties.tlsKeystorePassword
            val clientTrustStore = applicationProperties.tlsTruststorePath
            val clientTrustStorePassword = applicationProperties.tlsTruststorePassword

            if (clientKeyStore.isNullOrBlank() || clientKeyStorePassword.isNullOrBlank() || clientTrustStore.isNullOrBlank() || clientTrustStorePassword.isNullOrBlank()) {
                throw RuntimeException("There are missing TLS configurations. Check application.conf file")
            }

            Jedis(
                applicationProperties.redisHost,
                applicationProperties.redisPort,
                true,
                SSLUtils.getSSLContext(
                    clientKeyStore,
                    clientKeyStorePassword,
                    clientTrustStore,
                    clientTrustStorePassword
                ),
                null,
                null
            )
        } else {
            Jedis(applicationProperties.redisHost, applicationProperties.redisPort)
        }
    }
}
