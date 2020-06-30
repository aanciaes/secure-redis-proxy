package anciaes.secure.redis.service

/* ktlint-disable */
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.utils.SSLUtils
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.params.SetParams
import java.util.HashSet
import java.util.concurrent.TimeUnit
/* ktlint-enable */

class RedisClusterImpl(props: ApplicationProperties) : RedisService {

    private val jedis: JedisCluster = buildJedisClusterClient(props)

    override fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String {
        return if (expiration != null) {
            val expirationParams = SetParams()
            when (timeUnit) {
                TimeUnit.MILLISECONDS -> expirationParams.px(expiration)
                else -> expirationParams.ex(expiration.toInt())
            }

            jedis.set(key, value, expirationParams)
        } else {
            jedis.set(key, value)
        }
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
        return "Error: Flush all for cluster is not supported yet..."
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
