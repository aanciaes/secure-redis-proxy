package anciaes.secure.redis.utils

import anciaes.secure.redis.model.ApplicationProperties
import java.util.Properties

object ConfigurationUtils {

    fun loadApplicationConfigurations(): ApplicationProperties {
        val configurationFile = "/application.conf"
        val props = readPropertiesFile(configurationFile)

        return ApplicationProperties(
            props.getProperty("application.secure")?.toBoolean() ?: false,
            props.getProperty("redis.host") ?: "localhost",
            props.getProperty("redis.port")?.toInt() ?: 6379,

            props.getProperty("redis.auth.username"),
            props.getProperty("redis.auth.password"),

            props.getProperty("redis.tls")?.toBoolean() ?: false,
            props.getProperty("redis.keystore.path"),
            props.getProperty("redis.keystore.password"),
            props.getProperty("redis.truststore.path"),
            props.getProperty("redis.truststore.password"),

            props.getProperty("redis.cluster")?.toBoolean() ?: false,
            props.getProperty("redis.cluster.nodes")?.split(",")?.map { it.trim() },

            props.getProperty("key.encryption.det.secret"),
            props.getProperty("key.encryption.ope.secret"),

            props.getProperty("data.encryption.ciphersuite"),
            props.getProperty("data.encryption.provider"),
            props.getProperty("data.encryption.secret"),

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
