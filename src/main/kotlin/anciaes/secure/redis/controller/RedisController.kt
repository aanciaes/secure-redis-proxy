package anciaes.secure.redis.controller

import anciaes.secure.redis.model.SetCommand
import anciaes.secure.redis.service.RedisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController()
@RequestMapping(path = ["/redis"])
class RedisController {

    @Autowired
    lateinit var redisService: RedisService

    @RequestMapping(method = [RequestMethod.GET], path = ["/get"])
    fun get(@RequestParam query: String) = redisService.get(query)

    @RequestMapping(method = [RequestMethod.POST], path = ["/set"])
    fun set(@RequestBody setCommand: SetCommand): String {
        val timeUnit = when (setCommand.expTimeUnit?.toLowerCase()) {
            "ms" -> TimeUnit.MILLISECONDS
            "s" -> TimeUnit.SECONDS
            null -> null
            else -> TimeUnit.SECONDS
        }

        return redisService.set(setCommand.key, setCommand.value, setCommand.expiration, timeUnit)
    }
}
