package anciaes.secure.redis.utils

import anciaes.secure.redis.model.ApplicationProperties
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

fun ByteArray.toHexString(): String {
    val sb = StringBuilder()
    this.forEach { b ->
        sb.append(String.format("%02X", b))
    }

    return sb.toString().toLowerCase()
}

object JedisPoolConstructors {
    fun buildTLSJedisPoolAuthenticated(
        applicationProperties: ApplicationProperties,
        clientKeyStore: String,
        clientKeyStorePassword: String,
        clientTrustStore: String,
        clientTrustStorePassword: String
    ): JedisPool {
        return JedisPool(
            JedisPoolConfig(),
            applicationProperties.redisHost,
            applicationProperties.redisPort,
            2000,
            2000,
            applicationProperties.redisUsername,
            applicationProperties.redisPassword,
            0,
            null,
            true,
            SSLUtils.getSSLContext(
                clientKeyStore,
                clientKeyStorePassword,
                clientTrustStore,
                clientTrustStorePassword
            ),
            null,
            null
        )
    }

    fun buildTLSJedisPoolAuthenticatedLegacy(
        applicationProperties: ApplicationProperties,
        clientKeyStore: String,
        clientKeyStorePassword: String,
        clientTrustStore: String,
        clientTrustStorePassword: String
    ): JedisPool {
        return JedisPool(
            JedisPoolConfig(),
            applicationProperties.redisHost,
            applicationProperties.redisPort,
            2000,
            applicationProperties.redisPassword,
            true,
            SSLUtils.getSSLContext(
                clientKeyStore,
                clientKeyStorePassword,
                clientTrustStore,
                clientTrustStorePassword
            ),
            null,
            null
        )
    }

    fun buildTLSJedisPool(
        applicationProperties: ApplicationProperties,
        clientKeyStore: String,
        clientKeyStorePassword: String,
        clientTrustStore: String,
        clientTrustStorePassword: String
    ): JedisPool {
        return JedisPool(
            JedisPoolConfig(),
            applicationProperties.redisHost,
            applicationProperties.redisPort,
            true,
            SSLUtils.getSSLContext(
                clientKeyStore,
                clientKeyStorePassword,
                clientTrustStore,
                clientTrustStorePassword
            ),
            null,
            null
        )
    }

    fun buildInsecureJedisPool(applicationProperties: ApplicationProperties): JedisPool {
        return JedisPool(JedisPoolConfig(), applicationProperties.redisHost, applicationProperties.redisPort)
    }

    fun buildInsecureJedisPoolAuthenticated(applicationProperties: ApplicationProperties): JedisPool {
        return JedisPool(
            JedisPoolConfig(),
            applicationProperties.redisHost,
            applicationProperties.redisPort,
            2000,
            applicationProperties.redisUsername,
            applicationProperties.redisPassword
        )
    }

    fun buildInsecureJedisPoolAuthenticatedLegacy(applicationProperties: ApplicationProperties): JedisPool {
        return JedisPool(
            JedisPoolConfig(),
            applicationProperties.redisHost,
            applicationProperties.redisPort,
            2000,
            applicationProperties.redisUsername
        )
    }
}
