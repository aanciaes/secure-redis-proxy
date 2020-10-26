package anciaes.secure.redis.configuration

import anciaes.secure.redis.model.ReplicationMode
import anciaes.secure.redis.service.RedisClusterImpl
import anciaes.secure.redis.service.RedisService
import anciaes.secure.redis.service.RedisServiceImpl
import anciaes.secure.redis.service.SecureRedisClusterImpl
import anciaes.secure.redis.service.SecureRedisServiceImpl
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
