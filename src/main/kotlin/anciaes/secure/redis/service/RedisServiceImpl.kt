package anciaes.secure.redis.service

import redis.clients.jedis.Jedis
import java.util.Properties

class RedisServiceImpl(props: Properties) : RedisService {

    private val redisHost = props.getProperty("redis.host", "localhost")
    private val redisPort = props.getProperty("redis.port", "6379").toInt()
    private val jedis: Jedis = Jedis(redisHost, redisPort)

    init {
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
}
