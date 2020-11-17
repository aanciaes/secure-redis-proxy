package anciaes.secure.redis

import anciaes.secure.redis.model.ReplicationMode
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.service.redis.encrypted.EncryptedRedisClusterImpl
import anciaes.secure.redis.service.redis.encrypted.EncryptedRedisServiceImpl
import anciaes.secure.redis.service.redis.normal.RedisClusterImpl
import anciaes.secure.redis.service.redis.normal.RedisServiceImpl
import anciaes.secure.redis.utils.ConfigurationUtils
import java.util.concurrent.TimeUnit

fun main() {
    val props = ConfigurationUtils.loadApplicationConfigurations()
    val redisService: RedisService = if (props.redisEncrypted) {

        if (props.replicationEnabled && props.replicationMode == ReplicationMode.Cluster) {
            println("Initializing Encrypted Redis Cluster...")
            EncryptedRedisClusterImpl(props)
        } else {
            println("Initializing Encrypted Redis...")
            EncryptedRedisServiceImpl(props)
        }
    } else {
        if (props.replicationEnabled && props.replicationMode == ReplicationMode.Cluster) {
            println("Initializing Non-Encrypted Redis Cluster...")
            RedisClusterImpl(props)
        } else {
            println("Initializing Non-Encrypted Redis...")
            RedisServiceImpl(props)
        }
    }

    var exit = false
    while (!exit) {
        print("$ ")
        val command = readLine()!!.split(" ")
        exit = executeCommand(redisService, command)
    }
}

fun executeCommand(redisService: RedisService, command: List<String>): Boolean {

    when (command[0]) {
        "exit" -> return true
        "set" -> {
            if (command.size != 3 && command.size != 4 && command.size != 5) {
                println("Wrong number of arguments. Usage: set <key> <value> [expiration [ms|s]]")
            } else {
                val timeUnit = when (command.getOrNull(4)) {
                    "ms" -> TimeUnit.MILLISECONDS
                    "s" -> TimeUnit.SECONDS
                    null -> null
                    else -> TimeUnit.SECONDS
                }

                try {
                    println(
                        redisService.set(
                            key = command[1],
                            value = command[2],
                            expiration = command.getOrNull(3)?.toLong(),
                            timeUnit = timeUnit
                        )
                    )
                } catch (e: NumberFormatException) {
                    println("Expiration should be an integer... Operation failed")
                }
            }
        }
        "get" -> {
            if (command.size != 2) {
                println("Wrong number of arguments. Usage: get <key>")
            } else {
                println(redisService.get(command[1]))
            }
        }
        "del" -> {
            if (command.size != 2) {
                println("Wrong number of arguments. Usage: del <key>")
            } else {
                println(redisService.del(command[1]))
            }
        }
        "zadd" -> {
            if (command.size != 4) {
                println("Wrong number of arguments. Usage: zadd <key> <score> <value>")
            } else {
                println(redisService.zadd(command[1], command[2].toDouble(), command[3]))
            }
        }
        "zrange" -> {
            if (command.size != 4) {
                println("Wrong number of arguments. Usage: zrange <key> <min> <max>")
            } else {
                println(redisService.zrangeByScore(command[1], command[2], command[3]))
            }
        }
        "flushall" -> println(redisService.flushAll())
        "help" -> printHelp()
        "", " " -> {
            /* Do nothing */
        }
        else -> println("Wrong Command. Use help to see available commands.")
    }

    return false
}

fun printHelp() {
    println("Usage: $ command [options]")
    println()
    println("List of available commands:")
    println("\tset <key> <value> [expiration [ms|s]]")
    println("\tget <key>")
    println("\tdel <key>")
    println("\tzadd <key> <score> <value>")
    println("\tzrange <key> <min> <max>")
    println("\tflushall")
    println("\thelp")
    println("\texit")
}
