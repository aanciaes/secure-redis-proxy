package anciaes.secure.redis.service

import hlib.hj.mlib.HomoDet
import hlib.hj.mlib.HomoOpeInt
import redis.clients.jedis.Jedis
import java.nio.charset.Charset
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SecureRedisServiceImpl(props: Properties) : RedisService {

    private val redisHost = props.getProperty("redis.host", "localhost")
    private val redisPort = props.getProperty("redis.port", "6379").toInt()
    private val jedis: Jedis = Jedis(redisHost, redisPort)

    // Homo Det Keys
    private val homoDetSecret = props.getProperty("encryption.det.secret")
    private val secretKey = HomoDet.keyFromString(homoDetSecret)

    // Homo Ope Int Keys
    val opeSecret = props.getProperty("encryption.ope.secret")
    val ope = HomoOpeInt(opeSecret)

    // Value Keys
    private val valueProvider = props.getProperty("encryption.value.provider")
    private val valueCipherSuite = props.getProperty("encryption.value.algorithm")
    private val valueAlgorithm = valueCipherSuite.split("/")[0]
    private val valueSecret = props.getProperty("encryption.value.secret")
    private val valueKey = SecretKeySpec(valueSecret.toByteArray(Charset.defaultCharset()), valueAlgorithm)

    init {
        val username = props.getProperty("redis.auth.username")
        val password = props.getProperty("redis.auth.password")

        if (!password.isNullOrBlank()) {
            // Backwards compatibility. For older Redis versions that do not support ACL
            if (username.isNullOrBlank()) {
                jedis.auth(password)
            } else {
                jedis.auth(username, password)
            }
        }

        if (jedis.ping() != "PONG") throw RuntimeException("Redis not ready")
    }

    override fun set(key: String, value: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        val encryptedValue = encryptValue(value)
        return jedis.set(encryptedKey, encryptedValue)
    }

    override fun get(key: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        return when (val value = jedis.get(encryptedKey)) {
            null -> "(nil)"
            else -> decryptValue(value)
        }
    }

    override fun del(key: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        return if (jedis.del(encryptedKey) == 1L) "OK" else "NOK"
    }

    override fun zadd(key: String, score: String, value: String): String {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        val encryptedScoreDouble = try {
            val scoreInt = score.toInt()
            ope.encrypt(scoreInt).toDouble()
        } catch (e: NumberFormatException) {
            return "NOK - Score must be a number"
        }

        val encryptedValue = encryptValue(value)
        return if (jedis.zadd(encryptedKey, encryptedScoreDouble, encryptedValue) == 1L) "OK" else "NOK"
    }

    override fun zrangeByScore(key: String, min: String, max: String): List<String> {
        val encryptedKey = HomoDet.encrypt(secretKey, key)
        var encryptedMin = "-inf"
        var encryptedMax = "+inf"

        try {
            if (min != "-inf" && min != "+inf" && min != "inf" && max != "-inf" && max != "+inf" && max != "inf") {
                encryptedMin = ope.encrypt(min.toInt()).toString()
                encryptedMax = ope.encrypt(max.toInt()).toString()
            }
        } catch (e: NumberFormatException) {
            return listOf("NOK - Score must be a number of [-inf, inf, +inf]")
        }

        return jedis.zrangeByScore(encryptedKey, encryptedMin, encryptedMax).toList().map { decryptValue(it) }
    }

    override fun flushAll(): String {
        return jedis.flushAll()
    }

    private fun encryptValue(value: String): String {
        val cipher: Cipher = Cipher.getInstance(valueCipherSuite, valueProvider)
        cipher.init(Cipher.ENCRYPT_MODE, valueKey)

        cipher.update(value.toByteArray(Charset.defaultCharset()))
        val encryptedBytes = cipher.doFinal()
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    private fun decryptValue(cipherText: String): String {
        val cipher: Cipher = Cipher.getInstance(valueCipherSuite, valueProvider)
        cipher.init(Cipher.DECRYPT_MODE, valueKey)

        cipher.update(Base64.getDecoder().decode(cipherText))
        val encryptedBytes = cipher.doFinal()
        return String(encryptedBytes)
    }
}
