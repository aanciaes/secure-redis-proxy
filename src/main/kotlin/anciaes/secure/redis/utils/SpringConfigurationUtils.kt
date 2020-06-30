package anciaes.secure.redis.utils

import anciaes.secure.redis.service.RedisClusterImpl
import anciaes.secure.redis.service.RedisService
import anciaes.secure.redis.service.RedisServiceImpl
import anciaes.secure.redis.service.SecureRedisClusterImpl
import anciaes.secure.redis.service.SecureRedisServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
object SpringConfigurationUtils {

    @Bean
    fun loadCorrectRedisImplementation(): RedisService {
        val props = ConfigurationUtils.loadApplicationConfigurations()

        return if (props.secure) {

            if (props.isCluster) {
                println("Initializing Secure Redis Cluster...")
                SecureRedisClusterImpl(props)
            } else {
                println("Initializing Secure Redis...")
                SecureRedisServiceImpl(props)
            }
        } else {
            if (props.isCluster) {
                println("Initializing Non-Secure Redis Cluster...")
                RedisClusterImpl(props)
            } else {
                println("Initializing Non-Secure Redis...")
                RedisServiceImpl(props)
            }
        }
    }
}
