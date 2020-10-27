package anciaes.secure.redis.configuration

import anciaes.secure.redis.utils.toHexString
import java.security.MessageDigest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

class LoggingInterceptor : HandlerInterceptorAdapter() {

    private val logger: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    private val messageDigest: MessageDigest = MessageDigest.getInstance("sha-256")

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        exception: Exception?
    ) {

        val hashedUsername = (request.userPrincipal ?: null)?.let {
            hashString(it.name)
        } ?: "Not Found. Proxy it is probably running in development mode"

        val hashedRole = (request.userPrincipal ?: null)?.let {
            hashString((it as KeycloakAuthenticationToken).authorities.first().authority)
        } ?: "Not Found. Proxy it is probably running in development mode"

        logger.info(
            "${request.remoteAddr} -- [${System.currentTimeMillis()}] \"${request.scheme} ${request.method} ${request.requestURI} ${request.protocol}\" ${response.status} - ${response.bufferSize} ${
                request.getHeader(
                    "User-Agent"
                ) ?: ""
            } -- User: $hashedUsername Role: $hashedRole"
        )
    }

    private fun hashString(data: String): String {
        messageDigest.update(data.toByteArray())
        return messageDigest.digest().toHexString()
    }
}
