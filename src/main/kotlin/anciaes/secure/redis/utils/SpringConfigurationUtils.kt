package anciaes.secure.redis.utils

import anciaes.secure.redis.service.RedisClusterImpl
import anciaes.secure.redis.service.RedisService
import anciaes.secure.redis.service.RedisServiceImpl
import anciaes.secure.redis.service.SecureRedisClusterImpl
import anciaes.secure.redis.service.SecureRedisServiceImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.filter.CommonsRequestLoggingFilter


@Component
object SpringConfigurationUtils {

    var logger: Logger = LoggerFactory.getLogger(SpringConfigurationUtils::class.java)

    @Bean
    fun loadCorrectRedisImplementation(): RedisService {
        val props = ConfigurationUtils.loadApplicationConfigurations()

        return if (props.secure) {

            if (props.isCluster) {
                logger.info("Initializing Secure Redis Cluster...")
                SecureRedisClusterImpl(props)
            } else {
                logger.info("Initializing Secure Redis...")
                SecureRedisServiceImpl(props)
            }
        } else {
            if (props.isCluster) {
                logger.info("Initializing Non-Secure Redis Cluster...")
                RedisClusterImpl(props)
            } else {
                logger.info("Initializing Non-Secure Redis...")
                RedisServiceImpl(props)
            }
        }
    }
}
