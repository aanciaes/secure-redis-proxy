package anciaes.secure.redis.service

import anciaes.secure.redis.utils.SSLUtils
import redis.clients.jedis.Jedis
import java.util.Properties

class RedisServiceImpl(props: Properties) : RedisService {

    private val jedis: Jedis = buildJedisClient(props)

    init {
        val username = props.getProperty("redis.auth.username")
        val password = props.getProperty("redis.auth.password")

        if (!password.isNullOrBlank()) {
            // Backwards compatibility. For older Redis versions that do not support ACL
            if (username.isNullOrBlank()) {
                jedis.auth(password)
            } else {
                jedis.auth(username, password)
            }
        }

        if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
    }

    override fun set(key: String, value: String): String {
        return jedis.set(key, value)
    }

    override fun get(key: String): String {
        return jedis.get(key) ?: "(nil)"
    }

    override fun del(key: String): String {
        return if (jedis.del(key) == 1L) "OK" else "NOK"
    }

    override fun zadd(key: String, score: String, value: String): String {
        val scoreDouble = try {
            score.toDouble()
        } catch (e: NumberFormatException) {
            return "NOK - Score must be a number"
        }
        return if (jedis.zadd(key, scoreDouble, value) == 1L) "OK" else "NOK"
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<String> {
        try {
            if (min != "-inf" && min != "+inf" && min != "inf" && max != "-inf" && max != "+inf" && max != "inf") {
                min.toDouble()
                max.toDouble()
            }
        } catch (e: NumberFormatException) {
            return listOf("NOK - Score must be a number of [-inf, inf, +inf]")
        }

        return jedis.zrangeByScore(key, min, max).toList()
    }

    override fun flushAll(): String {
        return jedis.flushAll()
    }

    private fun buildJedisClient(applicationProperties: Properties): Jedis {
        val redisHost = applicationProperties.getProperty("redis.host", "localhost")
        val redisPort = applicationProperties.getProperty("redis.port", "6379").toInt()

        val tls = applicationProperties.getProperty("redis.tls")?.toBoolean() ?: false

        return if (tls) {
            val clientKeyStore = applicationProperties.getProperty("redis.tls.keystore.path")
            val clientKeyStorePassword = applicationProperties.getProperty("redis.tls.keystore.password")
            val clientTrustStore = applicationProperties.getProperty("redis.tls.truststore.path")
            val clientTrustStorePassword = applicationProperties.getProperty("redis.tls.truststore.password")

            if (clientKeyStore.isNullOrBlank() || clientKeyStorePassword.isNullOrBlank() || clientTrustStore.isNullOrBlank() || clientTrustStorePassword.isNullOrBlank()) {
                throw RuntimeException("There are missing TLS configurations. Check application.conf file")
            }

            Jedis(
                redisHost,
                redisPort,
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
            Jedis(redisHost, redisPort)
        }
    }
}
