package anciaes.secure.redis.service.redis.unsecure

import anciaes.secure.redis.exceptions.BrokenSecurityException
import anciaes.secure.redis.exceptions.FunctionNotImplementedException
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
import hlib.hj.mlib.HomoSearch
import java.nio.charset.Charset
import java.security.Signature
import java.util.Base64
import java.util.HashSet
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.Mac
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.params.SetParams

class SecureRedisClusterImpl(val props: ApplicationProperties) : RedisService {

    private val jedis: JedisCluster = buildJedisClusterClient(props)

    // Homo Det Keys
    private val homoDetKey = HomoDet.keyFromString(props.keyEncryptionDetSecret)

    // Homo Add Key
    private val homoAddKey = HomoAdd.keyFromString(props.keyEncryptionAddSecret)

    // Homo Ope Int Keys
    val ope = HomoOpeInt(props.keyEncryptionOpeSecret)

    // Homo Search Key
    val homoSearchKey =
        HomoSearch.keyFromString("rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZWNyZXRLZXlTcGVjW0cLZuIwYU0CAAJMAAlhbGdvcml0aG10ABJMamF2YS9sYW5nL1N0cmluZztbAANrZXl0AAJbQnhwdAADQUVTdXIAAltCrPMX+AYIVOACAAB4cAAAABC3csJf7Hxw+scRJZiyvTAq")

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
        val encryptedScoreDouble = ope.encrypt(scoreInt).toDouble()

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
            encryptedMin = ope.encrypt(min.toInt()).toString()
            encryptedMax = ope.encrypt(max.toInt()).toString()
        }

        val res = jedis.zrangeByScoreWithScores(encryptedKey, encryptedMin, encryptedMax)
        return res.map { ZRangeTuple(getSecureValue(it.element), ope.decrypt(it.score.toLong()).toDouble()) }
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

    override fun sAdd(key: String, vararg values: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val encryptedValues = values.map {
            HomoSearch.encrypt(homoSearchKey, it)!!
        }

        return if (jedis.sadd(
                encryptedKey,
                *encryptedValues.toTypedArray()
            ) > 0
        ) RedisResponses.OK else RedisResponses.NOK
    }

    override fun sMembers(key: String, search: String?): List<String> {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val rst = jedis.smembers(encryptedKey)

        val encryptedSearchTerm = HomoSearch.encrypt(homoSearchKey, search)

        return if (search != null) rst.filter { HomoSearch.searchAll(encryptedSearchTerm, it) }
            .map { HomoSearch.decrypt(homoSearchKey, it) } else rst.map { HomoSearch.decrypt(homoSearchKey, it) }
    }

    override fun flushAll(): String {
        throw FunctionNotImplementedException("Error: Flush all for cluster is not supported yet...")
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

    private fun buildJedisClusterClient(applicationProperties: ApplicationProperties): JedisCluster {
        val jedisClusterNodes: MutableSet<HostAndPort> = HashSet()
        applicationProperties.replicationNodes!!.forEach {
            jedisClusterNodes.add(HostAndPort.parseString("${it.host}:${it.port}"))
        }

        return if (applicationProperties.tlsEnabled) {
            val clientKeyStore = applicationProperties.tlsKeystorePath
            val clientKeyStorePassword = applicationProperties.tlsKeystorePassword
            val clientTrustStore = applicationProperties.tlsTruststorePath
            val clientTrustStorePassword = applicationProperties.tlsTruststorePassword

            if (clientKeyStore.isNullOrBlank() || clientKeyStorePassword.isNullOrBlank() || clientTrustStore.isNullOrBlank() || clientTrustStorePassword.isNullOrBlank()) {
                throw RuntimeException("There are missing TLS configurations. Check application.conf file")
            }

            JedisCluster(
                jedisClusterNodes,
                2000,
                2000,
                5,
                applicationProperties.redisUsername,
                applicationProperties.redisPassword,
                "redis-cluster",
                GenericObjectPoolConfig<Any>(),
                true,
                SSLUtils.getSSLContext(
                    clientKeyStore,
                    clientKeyStorePassword,
                    clientTrustStore,
                    clientTrustStorePassword
                ),
                null,
                null,
                null
            )
        } else {
            JedisCluster(
                jedisClusterNodes,
                2000,
                2000,
                5,
                applicationProperties.redisUsername,
                applicationProperties.redisPassword,
                "default",
                GenericObjectPoolConfig<Any>(),
                false
            )
        }
    }
}
