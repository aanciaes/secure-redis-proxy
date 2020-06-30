package anciaes.secure.redis.model

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
