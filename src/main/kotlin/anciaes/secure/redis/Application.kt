package anciaes.secure.redis

import anciaes.secure.redis.service.RedisClusterServiceImpl
import anciaes.secure.redis.service.RedisService
import anciaes.secure.redis.service.RedisServiceImpl
import anciaes.secure.redis.service.SecureRedisServiceImpl
import java.util.Properties

fun main() {
    val props = readPropertiesFile("/application.conf")
    val redisService: RedisService = if (props.getProperty("application.secure")?.toBoolean() == true) {

        val isCluster = props.getProperty("redis.cluster")?.toBoolean() ?: false

        if (isCluster) {
            println("Initializing Secure Redis Cluster...")
            SecureRedisServiceImpl(props)
        } else {
            println("Initializing Secure Redis...")
            SecureRedisServiceImpl(props)
        }
    } else {
        val isCluster = props.getProperty("redis.cluster")?.toBoolean() ?: false

        if (isCluster) {
            println("Initializing Non-Secure Redis Cluster...")
            RedisClusterServiceImpl(props)
        } else {
            println("Initializing Non-Secure Redis...")
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
            if (command.size != 3) {
                println("Wrong number of arguments. Usage: set <key> <value>")
            } else {
                println(redisService.set(command[1], command[2]))
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
                println(redisService.zadd(command[1], command[2], command[3]))
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
    println("\tset <key> <value>")
    println("\tget <key>")
    println("\tdel <key>")
    println("\tzadd <key> <score> <value>")
    println("\tzrange <key> <min> <max>")
    println("\tflushall")
    println("\thelp")
    println("\texit")
}

fun readPropertiesFile(fileName: String): Properties {
    val inputStream = object {}.javaClass.getResourceAsStream(fileName)
    val props = Properties()
    props.load(inputStream)

    return props
}
