package anciaes.secure.redis.service

import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.RemoteAttestation
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.security.MessageDigest


@Service
class AttestationServiceImpl : AttestationService {

    @Autowired
    lateinit var props: ApplicationProperties

    override fun attestRedis(nonce: String): RemoteAttestation {
        val redisAttestEndpoint = "http://${props.redisHost}:${props.redisAttestationPort}/attest?nonce=$nonce"

        return jacksonObjectMapper().readValue(get(redisAttestEndpoint).text)
    }

    override fun attestProxy(): RemoteAttestation {
        val hash = hashFile("Dockerfile")
        println(hash)
        // TODO: Add keystore, jar, mrenclave files to application.conf
        // TODO: Hash and sign proxy jar
        // TODO: Create MRENCLAVE file / generate it dinamically. Then sign it
        // TODO: Response
        TODO("Not yet implemented")
    }

    private fun hashFile(file: String): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

        val fis = FileInputStream(file)

        val byteArray = ByteArray(1024)
        var bytesCount = 0

        while (fis.read(byteArray).also { bytesCount = it } != -1) {
            digest.update(byteArray, 0, bytesCount)
        }
        fis.close()

        val bytes: ByteArray = digest.digest()
        val sb = StringBuilder()
        bytes.forEach { b ->
            sb.append(String.format("%02X", b))
        }

        return sb.toString().toLowerCase()
    }
}
