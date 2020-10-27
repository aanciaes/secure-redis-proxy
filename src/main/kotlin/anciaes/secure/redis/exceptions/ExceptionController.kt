package anciaes.secure.redis.exceptions

/* ktlint-disable */
import anciaes.secure.redis.model.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.servlet.http.HttpServletResponse
/* ktlint-enable */

class KeyNotFoundException(message: String?) : RuntimeException(message)
class ZScoreFormatException(message: String?) : RuntimeException(message)
class ZScoreInsertException(message: String?) : RuntimeException(message)
class FunctionNotImplementedException(message: String?) : RuntimeException(message)
class BrokenSecurityException(message: String?) : RuntimeException(message)
class UnexpectedException(message: String?) : RuntimeException(message)
class ValueWronglyFormatted(message: String?) : RuntimeException(message)

@RestControllerAdvice
internal class GlobalDefaultExceptionHandler {

    @ExceptionHandler(value = [KeyNotFoundException::class])
    fun keyNotFoundException(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.NOT_FOUND.value()
        return ErrorResponse(res.status, e.message)
    }

    @ExceptionHandler(value = [ZScoreFormatException::class])
    fun zScoreFormatException(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.BAD_REQUEST.value()
        return ErrorResponse(res.status, e.message)
    }

    @ExceptionHandler(value = [ZScoreInsertException::class])
    fun zScoreInsertException(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.BAD_REQUEST.value()
        return ErrorResponse(res.status, e.message)
    }

    @ExceptionHandler(value = [FunctionNotImplementedException::class])
    fun functionNotImplementedException(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        return ErrorResponse(res.status, e.message)
    }

    @ExceptionHandler(value = [BrokenSecurityException::class])
    fun brokenSecurityException(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        return ErrorResponse(res.status, e.message)
    }

    @ExceptionHandler(value = [ValueWronglyFormatted::class])
    fun valueWronglyFormatted(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.BAD_REQUEST.value()
        return ErrorResponse(res.status, e.message)
    }

    @ExceptionHandler(value = [Exception::class])
    fun unexpectedException(res: HttpServletResponse, e: Exception): ErrorResponse {
        res.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        return ErrorResponse(res.status, e.message)
    }
}
