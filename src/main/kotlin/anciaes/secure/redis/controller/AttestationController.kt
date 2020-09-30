package anciaes.secure.redis.controller

/* ktlint-disable */
import anciaes.secure.redis.model.ApplicationAttestation
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.service.AttestationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse
/* ktlint-enable */

@RestController
@RequestMapping(path = ["/attest"])
class AttestationController {

    @Autowired
    lateinit var attestationService: AttestationService

    @RequestMapping(method = [RequestMethod.GET], path = ["", "/}"])
    fun attest(@RequestParam nonce: String, response: HttpServletResponse): ApplicationAttestation {
        val redisAttestation = attestationService.attestRedis(nonce)
        println (redisAttestation)
        val proxyAttestation = attestationService.attestProxy()

        return ApplicationAttestation(redisAttestation, proxyAttestation)
    }
}
