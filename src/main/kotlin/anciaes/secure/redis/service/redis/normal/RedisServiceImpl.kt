package anciaes.secure.redis.service.redis.normal

import anciaes.secure.redis.exceptions.KeyNotFoundException
import anciaes.secure.redis.exceptions.ValueWronglyFormatted
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RedisResponses
import anciaes.secure.redis.model.ZRangeTuple
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.utils.JedisPoolConstructors
import java.util.concurrent.TimeUnit
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

class RedisServiceImpl(props: ApplicationProperties) : RedisService {

    private val jedisPool: JedisPool = buildJedisClient(props)

    init {
        jedisPool.resource.use { jedis ->
            if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
        }
    }

    override fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String {
        jedisPool.resource.use { jedis ->
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
    }

    override fun get(key: String): String {
        return jedisPool.resource.use { jedis -> jedis.get(key) ?: RedisResponses.NIL }
    }

    override fun del(key: String): String {
        jedisPool.resource.use { jedis ->
            return if (jedis.del(key) == 1L) RedisResponses.OK else RedisResponses.NOK
        }
    }

    override fun zadd(key: String, score: Double, value: String): String {
        jedisPool.resource.use { jedis ->
            return if (jedis.zadd(key, score, value) == 1L) RedisResponses.OK else RedisResponses.NOK
        }
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<ZRangeTuple> {
        jedisPool.resource.use { jedis ->
            val res = jedis.zrangeByScoreWithScores(key, min, max)
            return res.map { ZRangeTuple(it.element, it.score) }
        }
    }

    override fun sum(key: String, value: Int): String {
        jedisPool.resource.use { jedis ->
            val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
            val oldValueLong =
                oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

            return jedis.set(key, (oldValueLong + value).toString())
        }
    }

    override fun diff(key: String, value: Int): String {
        jedisPool.resource.use { jedis ->
            val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
            val oldValueLong =
                oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

            return jedis.set(key, (oldValueLong - value).toString())
        }
    }

    override fun mult(key: String, value: Int): String {
        jedisPool.resource.use { jedis ->
            val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
            val oldValueLong =
                oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

            return jedis.set(key, (oldValueLong * value).toString())
        }
    }

    override fun sAdd(key: String, vararg values: String): String {
        jedisPool.resource.use { jedis ->
            return if (jedis.sadd(key, *values) > 0) RedisResponses.OK else RedisResponses.NOK
        }
    }

    override fun sMembers(key: String, search: String?): List<String> {
        jedisPool.resource.use { jedis ->
            val rst = jedis.smembers(key)

            return if (search != null) rst.filter { it.contains(search) } else rst.toList()
        }
    }

    override fun flushAll(): String {
        jedisPool.resource.use { jedis ->
            return jedis.flushAll()
        }
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
