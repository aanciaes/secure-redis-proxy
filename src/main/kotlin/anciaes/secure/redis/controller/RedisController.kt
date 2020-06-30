package anciaes.secure.redis.controller

/* ktlint-disable */
import anciaes.secure.redis.model.SetCommand
import anciaes.secure.redis.model.ZAddCommand
import anciaes.secure.redis.service.RedisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit
/* ktlint-enable */

@RestController
@RequestMapping(path = ["/redis"])
class RedisController {

    @Autowired
    lateinit var redisService: RedisService

    @RequestMapping(method = [RequestMethod.GET], path = ["/{key}"])
    fun get(@PathVariable key: String) = redisService.get(key)

    @RequestMapping(method = [RequestMethod.POST], path = ["/"])
    fun set(@RequestBody setCommand: SetCommand): String {
        val timeUnit = when (setCommand.expTimeUnit?.toLowerCase()) {
            "ms" -> TimeUnit.MILLISECONDS
            "s" -> TimeUnit.SECONDS
            null -> null
            else -> TimeUnit.SECONDS
        }

        return redisService.set(setCommand.key, setCommand.value, setCommand.expiration, timeUnit)
    }

    @RequestMapping(method = [RequestMethod.DELETE], path = ["/{key}"])
    fun del(@PathVariable key: String) = redisService.del(key)

    @RequestMapping(method = [RequestMethod.POST], path = ["/zadd"])
    fun zAdd(@RequestBody zAddCommand: ZAddCommand): String {
        return redisService.zadd(zAddCommand.key, zAddCommand.score, zAddCommand.value)
    }

    @RequestMapping(method = [RequestMethod.GET], path = ["/zadd/{key}"])
    fun zRangeByScore(@PathVariable key: String, @RequestParam min: String, @RequestParam max: String): List<String> {
        return redisService.zrangeByScore(key, min, max)
    }

    @RequestMapping(method = [RequestMethod.DELETE], path = ["/"])
    fun flushAll(): String {
        return redisService.flushAll()
    }
}
