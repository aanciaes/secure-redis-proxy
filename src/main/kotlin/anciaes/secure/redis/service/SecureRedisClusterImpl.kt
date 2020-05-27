package anciaes.secure.redis.service

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
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SecureRedisClusterImpl(props: Properties) : RedisService {

    private val jedis: JedisCluster = buildJedisClusterClient(props)

    // Homo Det Keys
    private val homoDetSecret = props.getProperty("encryption.det.secret")
    private val secretKey = HomoDet.keyFromString(homoDetSecret)

    // Homo Ope Int Keys
    val opeSecret = props.getProperty("encryption.ope.secret")
    val ope = HomoOpeInt(opeSecret)

    // Value Keys
    private val valueProvider = props.getProperty("encryption.value.provider")
    private val valueCipherSuite = props.getProperty("encryption.value.algorithm")
    private val valueAlgorithm = valueCipherSuite.split("/")[0]
    private val valueSecret = props.getProperty("encryption.value.secret")
    private val valueKey = SecretKeySpec(valueSecret.toByteArray(Charset.defaultCharset()), valueAlgorithm)

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
        val cipher: Cipher = Cipher.getInstance(valueCipherSuite, valueProvider)
        cipher.init(Cipher.ENCRYPT_MODE, valueKey)

        cipher.update(value.toByteArray(Charset.defaultCharset()))
        val encryptedBytes = cipher.doFinal()
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    private fun decryptValue(cipherText: String): String {
        val cipher: Cipher = Cipher.getInstance(valueCipherSuite, valueProvider)
        cipher.init(Cipher.DECRYPT_MODE, valueKey)

        cipher.update(Base64.getDecoder().decode(cipherText))
        val encryptedBytes = cipher.doFinal()
        return String(encryptedBytes)
    }

    private fun buildJedisClusterClient(props: Properties): JedisCluster {
        val clusterNodes = props.getProperty("redis.cluster.nodes")?.split(",")?.map { it.trim() }
            ?: throw RuntimeException("No cluster nodes provided")

        val jedisClusterNodes: MutableSet<HostAndPort> = HashSet()
        clusterNodes.forEach {
            jedisClusterNodes.add(HostAndPort.parseString(it))
        }

        val username = props.getProperty("redis.auth.username")
        val password = props.getProperty("redis.auth.password")

        val tls = props.getProperty("redis.tls")?.toBoolean() ?: false
        return if (tls) {
            val clientKeyStore = props.getProperty("redis.tls.keystore.path")
            val clientKeyStorePassword = props.getProperty("redis.tls.keystore.password")
            val clientTrustStore = props.getProperty("redis.tls.truststore.path")
            val clientTrustStorePassword = props.getProperty("redis.tls.truststore.password")

            if (clientKeyStore.isNullOrBlank() || clientKeyStorePassword.isNullOrBlank() || clientTrustStore.isNullOrBlank() || clientTrustStorePassword.isNullOrBlank()) {
                throw RuntimeException("There are missing TLS configurations. Check application.conf file")
            }

            val sslContext = SSLUtils.getSSLContext(
                clientKeyStore,
                clientKeyStorePassword,
                clientTrustStore,
                clientTrustStorePassword
            )

            JedisCluster(
                jedisClusterNodes,
                2000,
                2000,
                5,
                username,
                password,
                "redis-cluster",
                GenericObjectPoolConfig<Any>(),
                true,
                sslContext,
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
                username,
                password,
                "default",
                GenericObjectPoolConfig<Any>(),
                false
            )
        }
    }
}
