package anciaes.secure.redis.security

import org.keycloak.adapters.KeycloakConfigResolver
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties
import org.keycloak.adapters.springsecurity.KeycloakConfiguration
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy

@KeycloakConfiguration
internal class SecurityConfig : KeycloakWebSecurityConfigurerAdapter() {

    @Value("\${spring.profiles.active}")
    private val activeProfile: String? = null

    /**
     * Registers the KeycloakAuthenticationProvider with the authentication manager.
     */
    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        val keycloakAuthenticationProvider: KeycloakAuthenticationProvider = keycloakAuthenticationProvider()
        auth.authenticationProvider(keycloakAuthenticationProvider)
    }

    /**
     * Provide a session authentication strategy bean which should be of type
     * RegisterSessionAuthenticationStrategy for public or confidential applications
     * and NullAuthenticatedSessionStrategy for bearer-only applications.
     */
    @Bean
    override fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
        return RegisterSessionAuthenticationStrategy(SessionRegistryImpl())
    }

    /**
     * Use properties in application.properties instead of keycloak.json
     */
    @Bean
    @Primary
    fun keycloakConfigResolver(properties: KeycloakSpringBootProperties?): KeycloakConfigResolver {
        return CustomKeycloakSpringBootConfigResolver(properties)
    }

    /**
     * Secure appropriate endpoints
     */
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        super.configure(http)

        if (activeProfile == "dev") {
            http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/**").permitAll()
                .anyRequest().permitAll()
        } else {
            http.csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/redis/*", "/redis/zadd/*", "/redis/sadd/*").hasAnyAuthority("BasicUser", "Administrator")
                .antMatchers(HttpMethod.PUT, "/redis/*/sum", "/redis/*/diff", "/redis/*/mult").hasAnyAuthority("BasicUser", "Administrator")
                .antMatchers(HttpMethod.POST, "/redis", "/redis/", "/redis/zadd", "/redis/sadd").hasAuthority("Administrator")
                .antMatchers(HttpMethod.DELETE, "/", "/*").hasAuthority("Administrator")
                .antMatchers(HttpMethod.GET, "/attest", "/attest/").hasAuthority("Administrator")
                .antMatchers(HttpMethod.GET, "/system/healthz").anonymous()
                .anyRequest().denyAll()
        }
    }
}
