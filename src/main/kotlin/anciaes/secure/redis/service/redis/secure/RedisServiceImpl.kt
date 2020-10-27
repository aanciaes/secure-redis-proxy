package anciaes.secure.redis.service.redis.secure

/* ktlint-disable */
import anciaes.secure.redis.exceptions.KeyNotFoundException
import anciaes.secure.redis.exceptions.ValueWronglyFormatted
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RedisResponses
import anciaes.secure.redis.model.ZRangeTuple
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.utils.SSLUtils
import redis.clients.jedis.Jedis
import redis.clients.jedis.params.SetParams
import java.util.concurrent.TimeUnit

/* ktlint-enable */

class RedisServiceImpl(props: ApplicationProperties) : RedisService {

    private val jedis: Jedis = buildJedisClient(props)

    init {
        val redisAuth = props.redisAuthentication
        val username = props.redisUsername
        val password = props.redisPassword

        if (redisAuth) {
            if (!password.isNullOrBlank()) {
                // Backwards compatibility. For older Redis versions that do not support ACL
                if (username.isNullOrBlank()) {
                    jedis.auth(password)
                } else {
                    jedis.auth(username, password)
                }
            }
        }

        if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
    }

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

    override fun sum(key: String, value: Long): String {
        val oldValue = jedis.get(key) ?: throw KeyNotFoundException("Value for key <$key> not found")
        val oldValueLong = oldValue.toLongOrNull() ?: throw ValueWronglyFormatted("Value to be added should be a number")

        return jedis.set(key, (oldValueLong + value).toString())
    }

    override fun flushAll(): String {
        return jedis.flushAll()
    }

    private fun buildJedisClient(applicationProperties: ApplicationProperties): Jedis {
        return if (applicationProperties.tlsEnabled) {
            val clientKeyStore = applicationProperties.tlsKeystorePath
            val clientKeyStorePassword = applicationProperties.tlsKeystorePassword
            val clientTrustStore = applicationProperties.tlsTruststorePath
            val clientTrustStorePassword = applicationProperties.tlsTruststorePassword

            if (clientKeyStore.isNullOrBlank() || clientKeyStorePassword.isNullOrBlank() || clientTrustStore.isNullOrBlank() || clientTrustStorePassword.isNullOrBlank()) {
                throw RuntimeException("There are missing TLS configurations. Check application.conf file")
            }

            Jedis(
                applicationProperties.redisHost,
                applicationProperties.redisPort,
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
            Jedis(applicationProperties.redisHost, applicationProperties.redisPort)
        }
    }
}
