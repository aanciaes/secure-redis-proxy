package anciaes.secure.redis.controller

import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/system"])
class SystemController {

    @RequestMapping(method = [RequestMethod.GET], path = ["/healthz"])
    fun healthz(response: HttpServletResponse) {
        response.status = 200
    }
}
