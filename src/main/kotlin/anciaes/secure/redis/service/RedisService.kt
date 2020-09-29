package anciaes.secure.redis.service

import anciaes.secure.redis.model.ZRangeTuple
import java.util.concurrent.TimeUnit

interface RedisService {
    fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String
    fun get(key: String): String
    fun del(key: String): String

    fun zadd(key: String, score: Double, value: String): String
    fun zrangeByScore(key: String, min: String, max: String): List<ZRangeTuple>

    fun flushAll(): String
}
