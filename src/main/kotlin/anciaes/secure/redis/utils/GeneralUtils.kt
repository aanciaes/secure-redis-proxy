package anciaes.secure.redis.utils

fun ByteArray.toHexString(): String {
    val sb = StringBuilder()
    this.forEach { b ->
        sb.append(String.format("%02X", b))
    }

    return sb.toString().toLowerCase()
}
