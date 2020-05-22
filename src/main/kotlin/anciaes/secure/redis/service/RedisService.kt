package anciaes.secure.redis.service

interface RedisService {
    fun set(key: String, value: String): String
    fun get(key: String): String
    fun del(key: String): String

    fun zadd(key: String, score: String, value: String): String
    fun zrangeByScore(key: String, min: String, max: String): List<String>

    fun flushAll(): String
}
