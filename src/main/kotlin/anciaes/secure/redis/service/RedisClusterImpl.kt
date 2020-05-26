package anciaes.secure.redis.service

import anciaes.secure.redis.utils.SSLUtils
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import java.util.HashSet
import java.util.Properties

class RedisClusterServiceImpl(props: Properties) : RedisService {

    private val jedis: JedisCluster = buildRedisClusterClient(props)

    init {
        // if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
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
        return "NOK" // jedis.flushAll()
    }

    // TODO: read nodes from config
    // TODO: SET Cluster with homomorphic enc
    // TODO: Flush and ping functions on cluster

    private fun buildRedisClusterClient(props: Properties): JedisCluster {
        val clusterNodes = props.getProperty("redis.cluster.nodes")?.split(",")?.map { it.trim() } ?: throw RuntimeException("No cluster nodes provided")
        val jedisClusterNodes: MutableSet<HostAndPort> = HashSet()
        clusterNodes.forEach {
            println(it)
            jedisClusterNodes.add(HostAndPort.parseString(it))
        }

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

            val jc = JedisCluster(
                jedisClusterNodes,
                2000,
                2000,
                5,
                "default",
                "redis",
                "default",
                GenericObjectPoolConfig<Any>(),
                true,
                sslContext,
                null,
                null,
                null
            )
            println(jc.clusterNodes)
            jc

        } else {
            jedisClusterNodes.add(HostAndPort("thesis.ubuntu.server", 7000))
            val jc = JedisCluster(
                jedisClusterNodes,
                2000,
                2000,
                5,
                "redis",
                "default",
                GenericObjectPoolConfig<Any>(),
                false
            )
            jc
        }
    }
}
