package anciaes.secure.redis.service.redis.encrypted

import anciaes.secure.redis.exceptions.BrokenSecurityException
import anciaes.secure.redis.exceptions.KeyNotFoundException
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RedisResponses
import anciaes.secure.redis.model.ZRangeTuple
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.utils.JedisPoolConstructors
import anciaes.secure.redis.utils.KeystoreUtils
import hlib.hj.mlib.HomoDet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.Signature
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

class EncryptedNonHomoRedisServiceImpl(val props: ApplicationProperties) : RedisService {

    private val jedisPool: JedisPool = buildJedisClient(props)

    // Homo Det Keys
    private val homoDetKey = HomoDet.keyFromString(props.keyEncryptionDetSecret)

    private val iv = ByteArray(16)
    private val cipherAlgorithm = "AES/CBC/PKCS5Padding"

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
        jedisPool.resource.use { jedis ->
            if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
        }
    }

    override fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val secureValue = computeSecureValue(value)

        jedisPool.resource.use { jedis ->
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
    }

    override fun get(key: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        jedisPool.resource.use { jedis ->
            return when (val value = jedis.get(encryptedKey)) {
                null -> RedisResponses.NIL
                else -> getSecureValue(value)
            }
        }
    }

    override fun del(key: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        jedisPool.resource.use { jedis ->
            return if (jedis.del(encryptedKey) == 1L) RedisResponses.OK else RedisResponses.NOK
        }
    }

    override fun zadd(key: String, score: Double, value: String): String {
        throw NotImplementedError("Zadd is not implemented for Non Homomorphic Encryption Redis")
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<ZRangeTuple> {
        throw NotImplementedError("Zadd is not implemented for Non Homomorphic Encryption Redis")
    }

    override fun sum(key: String, value: Int): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        jedisPool.resource.use { jedis ->
            val redisSecureValue =
                jedis.get(encryptedKey) ?: throw KeyNotFoundException("Value for key <$key> not found")

            val currentValue = getSecureValue(redisSecureValue)
            val sum = currentValue.toInt() + value

            val newSecureValue = computeSecureValue(sum.toString())

            return jedis.set(encryptedKey, newSecureValue)
        }
    }

    override fun diff(key: String, value: Int): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        jedisPool.resource.use { jedis ->
            val redisSecureValue =
                jedis.get(encryptedKey) ?: throw KeyNotFoundException("Value for key <$key> not found")

            val currentValue = getSecureValue(redisSecureValue)
            val diff = currentValue.toInt() - value

            val newSecureValue = computeSecureValue(diff.toString())

            return jedis.set(encryptedKey, newSecureValue)
        }
    }

    override fun mult(key: String, value: Int): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        jedisPool.resource.use { jedis ->
            val redisSecureValue =
                jedis.get(encryptedKey) ?: throw KeyNotFoundException("Value for key <$key> not found")

            val currentValue = getSecureValue(redisSecureValue)
            val multiplication = currentValue.toInt() * value

            val newSecureValue = computeSecureValue(multiplication.toString())

            return jedis.set(encryptedKey, newSecureValue)
        }
    }

    override fun sAdd(key: String, vararg values: String): String {
        val encryptedKey = HomoDet.encrypt(homoDetKey, key)
        val encryptedValues = values.map {
            encryptValue(it)
        }

        val test = computeSecureValue(values.first())
        println(getSecureValue(test))

        jedisPool.resource.use { jedis ->
            return if (jedis.sadd(
                    encryptedKey,
                    *encryptedValues.toTypedArray()
                ) > 0
            ) RedisResponses.OK else RedisResponses.NOK
        }
    }

    override fun sMembers(key: String, search: String?): List<String> {
        jedisPool.resource.use { jedis ->
            val encryptedKey = HomoDet.encrypt(homoDetKey, key)
            val rst = jedis.smembers(encryptedKey)

            val searchTerms = search?.split(" ") ?: emptyList()

            val results = mutableListOf<String>()

            rst.forEach {
                val decryptedLine = decryptValue(it)
                val decryptedLineWords = decryptedLine.split(" ")

                if (search != null && searchTerms.isNotEmpty()) {
                    for (word in decryptedLineWords) {
                        for (searchTerm in searchTerms) {
                            if (word.contains(searchTerm)) {
                                results.add(decryptedLine)
                                break
                            }
                        }
                    }
                } else {
                    results.add(decryptedLine)
                }
            }

            return results
        }
    }

    override fun flushAll(): String {
        jedisPool.resource.use { jedis ->
            return jedis.flushAll()
        }
    }

    private fun computeSecureValue(value: String): String {
        val encryptedValue = encryptValue(value)
        val signedValue = signData(encryptedValue)

        val compositeValue = "$encryptedValue|$signedValue"
        val hash = computeIntegrityHash(compositeValue)
        return "$compositeValue|$hash"
    }

    private fun getSecureValue(secureValue: String): String {
        val composite = secureValue.split("|")

        val integrityCheck = computeIntegrityHash("${composite[0]}|${composite[1]}")
        if (integrityCheck != composite[2]) {
            throw BrokenSecurityException("Integrity Validation Failed... Data was tampered.")
        }

        return if (verifySignature(composite[0], composite[1])) {
            decryptValue(composite[0])
        } else {
            throw BrokenSecurityException("Error verifying authenticity...")
        }
    }

    private fun encryptValue(value: String): String {
        val cipher: Cipher = Cipher.getInstance(cipherAlgorithm, props.dataEncryptionProvider)

        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, IvParameterSpec(iv))

        val inputStream = ByteArrayInputStream(value.toByteArray())
        val outputStream = ByteArrayOutputStream()

        var len: Int
        do {
            val ibuf = ByteArray(1024)
            len = inputStream.read(ibuf)
            if (len > -1) {
                val obuf: ByteArray = cipher.update(ibuf, 0, len)
                outputStream.write(obuf)
            }
        } while (len != -1)

        val obuf: ByteArray = cipher.doFinal()
        outputStream.write(obuf)

        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    private fun decryptValue(cipherText: String): String {
        val cipher: Cipher = Cipher.getInstance(cipherAlgorithm, props.dataEncryptionProvider)

        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, IvParameterSpec(iv))
        val encryptedText = Base64.getDecoder().decode(cipherText)

        val inputStream = ByteArrayInputStream(encryptedText)
        val outputStream = ByteArrayOutputStream()

        var len: Int
        do {
            val ibuf = ByteArray(1024)
            len = inputStream.read(ibuf)
            if (len > -1) {
                val obuf: ByteArray = cipher.update(ibuf, 0, len)
                outputStream.write(obuf)
            }
        } while (len != -1)

        val obuf: ByteArray = cipher.doFinal()
        outputStream.write(obuf)

        return String(outputStream.toByteArray())
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

    private fun buildJedisClient(applicationProperties: ApplicationProperties): JedisPool {
        val redisAuth = applicationProperties.redisAuthentication
        val username = applicationProperties.redisUsername
        val password = applicationProperties.redisPassword

        return if (applicationProperties.tlsEnabled) {
            val clientKeyStore = applicationProperties.tlsKeystorePath
            val clientKeyStorePassword = applicationProperties.tlsKeystorePassword
            val clientTrustStore = applicationProperties.tlsTruststorePath
            val clientTrustStorePassword = applicationProperties.tlsTruststorePassword

            if (clientKeyStore.isNullOrBlank() || clientKeyStorePassword.isNullOrBlank() || clientTrustStore.isNullOrBlank() || clientTrustStorePassword.isNullOrBlank()) {
                throw RuntimeException("There are missing TLS configurations. Check application.conf file")
            }

            if (redisAuth && !password.isNullOrBlank()) {
                // Backwards compatibility. For older Redis versions that do not support ACL
                if (username.isNullOrBlank()) {
                    JedisPoolConstructors.buildTLSJedisPoolAuthenticatedLegacy(
                        applicationProperties,
                        clientKeyStore,
                        clientKeyStorePassword,
                        clientTrustStore,
                        clientTrustStorePassword
                    )
                } else {
                    JedisPoolConstructors.buildTLSJedisPoolAuthenticated(
                        applicationProperties,
                        clientKeyStore,
                        clientKeyStorePassword,
                        clientTrustStore,
                        clientTrustStorePassword
                    )
                }
            } else {
                JedisPoolConstructors.buildTLSJedisPool(
                    applicationProperties,
                    clientKeyStore,
                    clientKeyStorePassword,
                    clientTrustStore,
                    clientTrustStorePassword
                )
            }
        } else {
            return if (redisAuth && !password.isNullOrBlank()) {
                // Backwards compatibility. For older Redis versions that do not support ACL
                if (username.isNullOrBlank()) {
                    JedisPoolConstructors.buildInsecureJedisPoolAuthenticatedLegacy(applicationProperties)
                } else {
                    JedisPoolConstructors.buildInsecureJedisPoolAuthenticated(applicationProperties)
                }
            } else {
                JedisPoolConstructors.buildInsecureJedisPool(applicationProperties)
            }
        }
    }
}
