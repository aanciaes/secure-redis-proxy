package anciaes.secure.redis.service

/* ktlint-disable */
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.utils.KeystoreUtils
import anciaes.secure.redis.utils.SSLUtils
import hlib.hj.mlib.HomoDet
import hlib.hj.mlib.HomoOpeInt
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.params.SetParams
import java.nio.charset.Charset
import java.security.Signature
import java.util.Base64
import java.util.HashSet
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
/* ktlint-enable */

class SecureRedisClusterImpl(val props: ApplicationProperties) : RedisService {

    private val jedis: JedisCluster = buildJedisClusterClient(props)

    // Homo Det Keys
    private val secretKey = HomoDet.keyFromString(props.keyEncryptionDetSecret)

    // Homo Ope Int Keys
    val ope = HomoOpeInt(props.keyEncryptionOpeSecret)

    // Load KeySpecs
    private val encryptionKey = KeystoreUtils.getKeyFromKeyStore(
        props.dataEncryptionKeystoreType!!,
        props.dataEncryptionKeystore!!,
        props.dataEncryptionKeystorePassword!!,
        props.dataEncryptionKeystoreKeyName!!,
        props.dataEncryptionKeystoreKeyPassword!!
    )!!
    private val integrityKey = KeystoreUtils.getKeyFromKeyStore(
        props.dataHMacKeystoreType!!,
        props.dataHMacKeystore!!,
        props.dataHMacKeystorePassword!!,
        props.dataHMacKeyName!!,
        props.dataHMacKeyPassword!!
    )!!
    private val signingKey = KeystoreUtils.getKeyPairFromKeyStore(
        props.dataSignatureKeystoreType!!,
        props.dataSignatureKeystore!!,
        props.dataSignatureKeystorePassword!!,
        props.dataSignatureKeystoreKeyName!!,
        props.dataSignatureKeystoreKeyPassword!!
    )!!

    override fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
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
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        return when (val value = jedis.get(encryptedKey)) {
            null -> "(nil)"
            else -> getSecureValue(value)
        }
    }

    override fun del(key: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        return if (jedis.del(encryptedKey) == 1L) "OK" else "NOK"
    }

    override fun zadd(key: String, score: String, value: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        val encryptedScoreDouble = try {
            val scoreInt = score.toInt()
            ope.encrypt(scoreInt).toDouble()
        } catch (e: NumberFormatException) {
            return "NOK - Score must be a number"
        }

        val secureValue = computeSecureValue(value)
        return if (jedis.zadd(encryptedKey, encryptedScoreDouble, secureValue) == 1L) "OK" else "NOK"
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<String> {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        var encryptedMin = "-inf"
        var encryptedMax = "+inf"

        try {
            if (min != "-inf" && min != "+inf" && min != "inf" && max != "-inf" && max != "+inf" && max != "inf") {
                encryptedMin = ope.encrypt(min.toInt()).toString()
                encryptedMax = ope.encrypt(max.toInt()).toString()
            }
        } catch (e: NumberFormatException) {
            return listOf("NOK - Score must be a number of [-inf, inf, +inf]")
        }

        return jedis.zrangeByScore(encryptedKey, encryptedMin, encryptedMax).toList().map { getSecureValue(it) }
    }

    override fun flushAll(): String {
        return "Error: Flush all for cluster is not supported yet..."
    }

    private fun computeSecureValue(value: String): String {
        val encryptedValue = encryptValue(value)
        val signedValue = signData(value)

        val compositeValue = "$encryptedValue|$signedValue"
        val hash = computeIntegrityHash(compositeValue)
        return "$encryptedValue|$signedValue|$hash"
    }

    private fun getSecureValue(secureValue: String): String {
        val composite = secureValue.split("|")

        val integrityCheck = computeIntegrityHash("${composite[0]}|${composite[1]}")
        if (integrityCheck != composite[2]) {
            return "Integrity Validation Failed... Data might was tampered."
        }
        val value = decryptValue(composite[0])

        return if (verifySignature(value, composite[1])) {
            value
        } else {
            "Error verifying authenticity..."
        }
    }

    private fun encryptValue(value: String): String {
        val cipher: Cipher = Cipher.getInstance(props.dataEncryptionCipherSuite, props.dataEncryptionProvider)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)

        cipher.update(value.toByteArray(Charset.defaultCharset()))
        val encryptedBytes = cipher.doFinal()
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    private fun decryptValue(cipherText: String): String {
        val cipher: Cipher = Cipher.getInstance(props.dataEncryptionCipherSuite, props.dataEncryptionProvider)
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey)

        cipher.update(Base64.getDecoder().decode(cipherText))
        val encryptedBytes = cipher.doFinal()
        return String(encryptedBytes)
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
        applicationProperties.clusterContactNodes!!.forEach {
            jedisClusterNodes.add(HostAndPort.parseString(it))
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
