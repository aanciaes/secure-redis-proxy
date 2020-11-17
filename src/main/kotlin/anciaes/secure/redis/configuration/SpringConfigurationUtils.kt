package anciaes.secure.redis.configuration

import anciaes.secure.redis.model.ReplicationMode
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.service.redis.encrypted.EncryptedNonHomoRedisServiceImpl
import anciaes.secure.redis.service.redis.encrypted.EncryptedRedisClusterImpl
import anciaes.secure.redis.service.redis.encrypted.EncryptedRedisServiceImpl
import anciaes.secure.redis.service.redis.normal.RedisClusterImpl
import anciaes.secure.redis.service.redis.normal.RedisServiceImpl
import anciaes.secure.redis.utils.ConfigurationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
object SpringConfigurationUtils {

    var logger: Logger = LoggerFactory.getLogger(SpringConfigurationUtils::class.java)

    @Bean
    fun loadCorrectRedisImplementation(): RedisService {
        val props = ConfigurationUtils.loadApplicationConfigurations()

        return if (props.redisEncrypted) {
            if (props.replicationEnabled && props.replicationMode == ReplicationMode.Cluster) {
                logger.info("Initializing Encrypted Redis Cluster...")
                EncryptedRedisClusterImpl(props)
            } else {
                if (props.redisEncryptedIsHomomorphic) {
                    logger.info("Initializing Homomorphic Encrypted Redis...")
                    EncryptedRedisServiceImpl(props)
                } else {
                    logger.info("Initializing Non Homomorphic Encrypted Redis...")
                    EncryptedNonHomoRedisServiceImpl(props)
                }
            }
        } else {
            if (props.replicationEnabled && props.replicationMode == ReplicationMode.Cluster) {
                logger.info("Initializing Non-Encrypted Redis Cluster...")
                RedisClusterImpl(props)
            } else {
                logger.info("Initializing Non-Encrypted Redis...")
                RedisServiceImpl(props)
            }
        }
    }
}
