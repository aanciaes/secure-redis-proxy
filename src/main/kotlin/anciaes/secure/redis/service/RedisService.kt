package anciaes.secure.redis.service

import java.util.concurrent.TimeUnit

interface RedisService {
    fun set(key: String, value: String, expiration: Long?, timeUnit: TimeUnit?): String
    fun get(key: String): String
    fun del(key: String): String

    fun zadd(key: String, score: String, value: String): String
    fun zrangeByScore(key: String, min: String, max: String): List<String>

    fun flushAll(): String
}
