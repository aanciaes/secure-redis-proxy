package anciaes.secure.redis.utils

/* ktlint-disable */
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RedisNode
import anciaes.secure.redis.model.ReplicationMode
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.Properties

/* ktlint-enable */

private const val dev = "dev"
private const val prod = "prod"

@Component
object ConfigurationUtils {

    @Bean
    fun loadApplicationConfigurations(): ApplicationProperties {
        val profile = System.getenv("spring_profile") ?: "dev"

        val configurationFile = if (profile == prod) {
            "/prod-application.conf"
        } else {
            "/application.conf"
        }

        val props = readPropertiesFile(configurationFile)

        return ApplicationProperties(
            props.getProperty("redis.encrypted")?.toBoolean() ?: false,
            props.getProperty("redis.encrypted.homomorphic")?.toBoolean() ?: true,
            props.getProperty("redis.host") ?: "localhost",
            props.getProperty("redis.port")?.toInt() ?: 6379,

            props.getProperty("redis.auth")?.toBoolean() ?: false,
            props.getProperty("redis.auth.username"),
            props.getProperty("redis.auth.password"),

            props.getProperty("redis.tls")?.toBoolean() ?: false,
            props.getProperty("redis.tls.keystore.path"),
            props.getProperty("redis.tls.keystore.password"),
            props.getProperty("redis.tls.truststore.path"),
            props.getProperty("redis.tls.truststore.password"),

            props.getProperty("redis.replication")?.toBoolean() ?: false,
            ReplicationMode.valueOf(props.getProperty("redis.replication.mode") ?: ReplicationMode.MasterSlave.name),
            props.getProperty("redis.replication.nodes")?.replace("\\s".toRegex(), "")?.split(",")?.map {
                val separator = it.trim().split(":")
                val host = separator.first()
                val ports = separator.last().split("|")

                RedisNode(host, ports.first(), ports.last())
            },

            props.getProperty("key.encryption.det.secret"),
            props.getProperty("key.encryption.ope.secret"),
            props.getProperty("key.encryption.add.secret"),
            props.getProperty("key.encryption.search.secret"),

            props.getProperty("data.encryption.ciphersuite"),
            props.getProperty("data.encryption.provider"),
            props.getProperty("data.encryption.keystore.type"),
            props.getProperty("data.encryption.keystore.path"),
            props.getProperty("data.encryption.keystore.password"),
            props.getProperty("data.encryption.keystore.keyName"),
            props.getProperty("data.encryption.keystore.keyPassword"),

            props.getProperty("data.signature.algorithm"),
            props.getProperty("data.signature.provider"),
            props.getProperty("data.signature.keystore.type"),
            props.getProperty("data.signature.keystore.path"),
            props.getProperty("data.signature.keystore.password"),
            props.getProperty("data.signature.keystore.keyName"),
            props.getProperty("data.signature.keystore.keyPassword"),

            props.getProperty("data.hmac.algorithm"),
            props.getProperty("data.hmac.provider"),
            props.getProperty("data.hmac.keystore.type"),
            props.getProperty("data.hmac.keystore.path"),
            props.getProperty("data.hmac.keystore.password"),
            props.getProperty("data.hmac.keystore.keyName"),
            props.getProperty("data.hmac.keystore.keyPassword")
        )
    }

    private fun readPropertiesFile(fileName: String): Properties {
        val inputStream = object {}.javaClass.getResourceAsStream(fileName)
        val props = Properties()
        props.load(inputStream)

        return props
    }
}
