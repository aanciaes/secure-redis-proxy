package anciaes.secure.redis.service.redis

import anciaes.secure.redis.model.ZRangeTuple
import java.util.concurrent.TimeUnit

interface RedisService {
    fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String
    fun get(key: String): String
    fun del(key: String): String

    fun zadd(key: String, score: Double, value: String): String
    fun zrangeByScore(key: String, min: String, max: String): List<ZRangeTuple>

    fun sum(key: String, value: Int): String
    fun diff(key: String, value: Int): String
    fun mult(key: String, value: Int): String

    fun flushAll(): String
}
