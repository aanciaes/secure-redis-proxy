package anciaes.secure.redis.configuration

import anciaes.secure.redis.utils.toHexString
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.security.MessageDigest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LoggingInterceptor : HandlerInterceptorAdapter() {

    private val logger: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    private val messageDigest: MessageDigest = MessageDigest.getInstance("sha-256")

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        exception: Exception?
    ) {
        logger.info(
            "${request.remoteAddr} -- [${System.currentTimeMillis()}] \"${request.scheme} ${request.method} ${request.requestURI} ${request.protocol}\" ${response.status} - ${response.bufferSize} ${
                request.getHeader(
                    "User-Agent"
                ) ?: ""
            } -- User: ${hashString(request.userPrincipal.name)} Role: ${hashString((request.userPrincipal as KeycloakAuthenticationToken).authorities.first().authority)}"
        )
    }

    private fun hashString(data: String): String {
        messageDigest.update(data.toByteArray())
        return messageDigest.digest().toHexString()
    }
}
