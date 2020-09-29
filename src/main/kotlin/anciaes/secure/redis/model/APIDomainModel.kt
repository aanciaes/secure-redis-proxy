package anciaes.secure.redis.model

import java.util.concurrent.TimeUnit

class RedisResponses {
    companion object {
        const val OK = "OK"
        const val NIL = "(nil)"
        const val NOK = "NOK"
    }
}

data class SetCommand(
    val key: String,
    val value: String,
    val expiration: Long?,
    val expTimeUnit: String?
)

data class ZAddCommand(
    val key: String,
    val score: String,
    val value: String
)

data class ErrorResponse(val status: Int, val message: String?)

data class SetResponse(
    val key: String,
    val value: String,
    val expiration: Long?,
    val expTimeUnit: TimeUnit?,
    val location: String
)

data class GetResponse(val key: String, val value: String)

data class ZAddResponse(val key: String, val score: String, val valueAdded: String, val location: String)

data class ZRangeResponse(val key: String, val values: List<ZRangeTuple>)

data class ZRangeTuple(val value: String, val score: Double)
