package anciaes.secure.redis.service.redis.normal

/* ktlint-disable */
import anciaes.secure.redis.exceptions.FunctionNotImplementedException
import anciaes.secure.redis.exceptions.KeyNotFoundException
import anciaes.secure.redis.exceptions.ValueWronglyFormatted
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RedisResponses
import anciaes.secure.redis.model.ZRangeTuple
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.utils.SSLUtils
import java.util.HashSet
import java.util.concurrent.TimeUnit
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.params.SetParams

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
        return jedis.get(key) ?: RedisResponses.NIL
    }

    override fun del(key: String): String {
        return if (jedis.del(key) == 1L) RedisResponses.OK else RedisResponses.NOK
    }

    override fun zadd(key: String, score: Double, value: String): String {
        return if (jedis.zadd(key, score, value) == 1L) RedisResponses.OK else RedisResponses.NOK
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<ZRangeTuple> {
        val res = jedis.zrangeByScoreWithScores(key, min, max)
        return res.map { ZRangeTuple(it.element, it.score) }
    }

    override fun sum(key: String, value: Int): String {
        val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val oldValueLong =
            oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

        return jedis.set(key, (oldValueLong + value).toString())
    }

    override fun diff(key: String, value: Int): String {
        val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val oldValueLong =
            oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

        return jedis.set(key, (oldValueLong - value).toString())
    }

    override fun mult(key: String, value: Int): String {
        val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val oldValueLong =
            oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

        return jedis.set(key, (oldValueLong * value).toString())
    }

    override fun sAdd(key: String, vararg values: String): String {
        return if (jedis.sadd(key, *values) > 0) RedisResponses.OK else RedisResponses.NOK
    }

    override fun sMembers(key: String, search: String?): List<String> {
        val rst = jedis.smembers(key)

        return if (search != null) rst.filter { it.contains(search) } else rst.toList()
    }

    override fun flushAll(): String {
        throw FunctionNotImplementedException("Error: Flush all for cluster is not supported yet...")
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
