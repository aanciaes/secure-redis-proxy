package anciaes.secure.redis.configuration

import anciaes.secure.redis.model.ReplicationMode
import anciaes.secure.redis.service.redis.RedisClusterImpl
import anciaes.secure.redis.service.redis.RedisService
import anciaes.secure.redis.service.redis.RedisServiceImpl
import anciaes.secure.redis.service.redis.SecureRedisClusterImpl
import anciaes.secure.redis.service.redis.SecureRedisServiceImpl
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

        return if (!props.secure) {

            if (props.replicationEnabled && props.replicationMode == ReplicationMode.Cluster) {
                logger.info("Initializing Encrypted Redis Cluster...")
                SecureRedisClusterImpl(props)
            } else {
                logger.info("Initializing Encrypted Redis...")
                SecureRedisServiceImpl(props)
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
