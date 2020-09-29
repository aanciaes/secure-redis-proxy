package anciaes.secure.redis.controller

/* ktlint-disable */
import anciaes.secure.redis.exceptions.KeyNotFoundException
import anciaes.secure.redis.exceptions.UnexpectedException
import anciaes.secure.redis.exceptions.ZScoreFormatException
import anciaes.secure.redis.exceptions.ZScoreInsertException
import anciaes.secure.redis.model.GetResponse
import anciaes.secure.redis.model.RedisResponses
import anciaes.secure.redis.model.SetCommand
import anciaes.secure.redis.model.SetResponse
import anciaes.secure.redis.model.ZAddCommand
import anciaes.secure.redis.model.ZAddResponse
import anciaes.secure.redis.model.ZRangeResponse
import anciaes.secure.redis.service.RedisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse

/* ktlint-enable */

@RestController
@RequestMapping(path = ["/redis"])
class RedisController {

    @Autowired
    lateinit var redisService: RedisService

    @RequestMapping(method = [RequestMethod.GET], path = ["/{key}"])
    fun get(@PathVariable key: String, response: HttpServletResponse): GetResponse {
        val value = redisService.get(key)

        if (value == RedisResponses.NIL) {
            throw KeyNotFoundException("Value for key <$key> not found")
        }

        return GetResponse(key, redisService.get(key))
    }

    @RequestMapping(method = [RequestMethod.POST], path = ["", "/"])
    fun set(@RequestBody setCommand: SetCommand, response: HttpServletResponse): SetResponse {
        val timeUnit = when (setCommand.expTimeUnit?.toLowerCase()) {
            "ms" -> TimeUnit.MILLISECONDS
            "s" -> TimeUnit.SECONDS
            null -> null
            else -> TimeUnit.SECONDS
        }

        if (redisService.set(setCommand.key, setCommand.value, setCommand.expiration, timeUnit) == RedisResponses.OK) {
            response.status = 201
            return SetResponse(
                setCommand.key,
                setCommand.value,
                setCommand.expiration,
                timeUnit,
                buildLocation(setCommand.key)
            )
        } else {
            throw UnexpectedException("Error while setting value <${setCommand.value}> for key <${setCommand.key}>")
        }
    }

    @RequestMapping(method = [RequestMethod.DELETE], path = ["/{key}"])
    fun del(@PathVariable key: String, response: HttpServletResponse) {
        if (redisService.del(key) == RedisResponses.NOK) {
            throw KeyNotFoundException("Value for key <$key> not found")
        }

        response.status = HttpStatus.NO_CONTENT.value()
    }

    @RequestMapping(method = [RequestMethod.POST], path = ["/zadd"])
    fun zAdd(@RequestBody zAddCommand: ZAddCommand): ZAddResponse {
        val scoreDouble = try {
            zAddCommand.score.toDouble()
        } catch (e: NumberFormatException) {
            throw ZScoreFormatException("Score <${zAddCommand.score}> could not be parsed. Score should be a number")
        }

        return if (redisService.zadd(zAddCommand.key, scoreDouble, zAddCommand.value) == RedisResponses.OK) {
            ZAddResponse(zAddCommand.key, zAddCommand.score, zAddCommand.value, buildLocation(zAddCommand.key, "zadd"))
        } else {
            throw ZScoreInsertException("Error while setting value <${zAddCommand.value}> for key <${zAddCommand.key}> with score <${zAddCommand.score}>. Value should be unique in the set.")
        }
    }

    @RequestMapping(method = [RequestMethod.GET], path = ["/zadd/{key}"])
    fun zRangeByScore(
        @PathVariable key: String,
        @RequestParam(required = false, defaultValue = "-inf") min: String,
        @RequestParam(required = false, defaultValue = "inf") max: String
    ): ZRangeResponse {
        try {
            if (min != "-inf" && min != "inf") {
                min.toDouble()
            }

            if (max != "-inf" && max != "inf") {
                max.toDouble()
            }
        } catch (e: NumberFormatException) {
            throw ZScoreFormatException("Min and Max must be a number or [-inf, inf]")
        }

        return ZRangeResponse(key, redisService.zrangeByScore(key, min, max))
    }

    @RequestMapping(method = [RequestMethod.DELETE], path = ["", "/"])
    fun flushAll(response: HttpServletResponse) {
        if (redisService.flushAll() == RedisResponses.OK) {
            response.status = HttpStatus.NO_CONTENT.value()
        } else {
            throw UnexpectedException("Unexpected error while flushing all")
        }
    }

    private fun buildLocation(key: String, prefix: String? = null): String {
        return if (prefix == null) {
            "/$key"
        } else {
            "/$prefix/$key"
        }
    }
}
