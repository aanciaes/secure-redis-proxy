package anciaes.secure.redis.service

import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.utils.SSLUtils
import hlib.hj.mlib.HomoDet
import hlib.hj.mlib.HomoOpeInt
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.params.SetParams
import java.nio.charset.Charset
import java.util.Base64
import java.util.HashSet
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SecureRedisClusterImpl(props: ApplicationProperties) : RedisService {

    private val jedis: JedisCluster = buildJedisClusterClient(props)

    // Homo Det Keys
    private val secretKey = HomoDet.keyFromString(props.keyEncryptionDetSecret)

    // Homo Ope Int Keys
    val ope = HomoOpeInt(props.keyEncryptionOpeSecret)

    // Value Keys
    private val dataEncryptionProvider = props.dataEncryptionProvider
    private val dataEncryptionCipherSuite = props.dataEncryptionCipherSuite
    private val valueAlgorithm = props.dataEncryptionCipherSuite!!.split("/")[0]
    private val valueKey =
        SecretKeySpec(props.dataEncryptionSecret!!.toByteArray(Charset.defaultCharset()), valueAlgorithm)

    override fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        val encryptedValue = encryptValue(value)

        return if (expiration != null) {
            val expirationParams = SetParams()
            when (timeUnit) {
                TimeUnit.MILLISECONDS -> expirationParams.px(expiration)
                else -> expirationParams.ex(expiration.toInt())
            }

            jedis.set(encryptedKey, encryptedValue, expirationParams)
        } else {
            jedis.set(encryptedKey, encryptedValue)
        }
    }

    override fun get(key: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        return when (val value = jedis.get(encryptedKey)) {
            null -> "(nil)"
            else -> decryptValue(value)
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

        val encryptedValue = encryptValue(value)
        return if (jedis.zadd(encryptedKey, encryptedScoreDouble, encryptedValue) == 1L) "OK" else "NOK"
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

        return jedis.zrangeByScore(encryptedKey, encryptedMin, encryptedMax).toList().map { decryptValue(it) }
    }

    override fun flushAll(): String {
        return "Error: Flush all for cluster is not supported yet..."
    }

    private fun encryptValue(value: String): String {
        val cipher: Cipher = Cipher.getInstance(dataEncryptionCipherSuite, dataEncryptionProvider)
        cipher.init(Cipher.ENCRYPT_MODE, valueKey)

        cipher.update(value.toByteArray(Charset.defaultCharset()))
        val encryptedBytes = cipher.doFinal()
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    private fun decryptValue(cipherText: String): String {
        val cipher: Cipher = Cipher.getInstance(dataEncryptionCipherSuite, dataEncryptionProvider)
        cipher.init(Cipher.DECRYPT_MODE, valueKey)

        cipher.update(Base64.getDecoder().decode(cipherText))
        val encryptedBytes = cipher.doFinal()
        return String(encryptedBytes)
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
