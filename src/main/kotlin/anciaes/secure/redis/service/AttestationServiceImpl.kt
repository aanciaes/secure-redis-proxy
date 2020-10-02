package anciaes.secure.redis.service

/* ktlint-disable */
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.AttestationChallenge
import anciaes.secure.redis.model.AttestationQuote
import anciaes.secure.redis.model.RemoteAttestation
import anciaes.secure.redis.utils.KeystoreUtils
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import khttp.get
import khttp.structures.authorization.BasicAuthorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

/* ktlint-enable */

@Service
class AttestationServiceImpl : AttestationService {

    @Value("\${spring.profiles.active}")
    private val activeProfile: String? = null

    // This values are not subject to user configuration so they are hardcoded
    val redisAttestationPort = 8541
    val proxyIdentityKeyKeystoreType = "PKCS12"
    val proxyIdentityKeyKeystore =
        if (activeProfile == "prod") "production-keystores/proxy-attestation-identity-key.p12" else "keystores/proxy-attestation-identity-key.p12"
    val proxyIdentityKeyKeystorePassword =
        if (activeProfile == "prod") "Gwb!KMcF37rYHsTmHiLkFs9ms" else "Lcq6jCG-GFhnfqLK4PhyvWFj_"
    val proxyIdentityKeyName =
        if (activeProfile == "prod") "proxy-attestation-identity-key" else "proxy-attetstation-identity-key"
    val proxyIdentityKeyNamePassword =
        if (activeProfile == "prod") "Gwb!KMcF37rYHsTmHiLkFs9ms" else "Lcq6jCG-GFhnfqLK4PhyvWFj_"
    val proxyJarChallenge =
        if (activeProfile == "prod") "/home/secure-proxy-redis/secure-redis-proxy-0.3.jar" else "mock-files/mock-proxy-java-jar"
    val proxyMrEnclaveChallenge =
        if (activeProfile == "prod") "/home/secure-proxy-redis/mrenclave" else "mock-files/mrenclave-mock"

    val attestationSignatureAlgorithm = "SHA512withRSA"
    val attestationSignatureProvider = "SunRsaSign"
    val attestationHashingAlgorithm = "sha-256"

    val remoteAttestationServerUsername = "redis-proxy-cd497e6b-10cf-4ed2-be63-18b6b16375f6"
    val remoteAttestationServerPassword = "2grvTkqUhS7wE4R4WRjnuXTLzsxnAu"
    //

    @Autowired
    lateinit var props: ApplicationProperties

    override fun attestRedis(nonce: String): RemoteAttestation {
        val redisAttestEndpoint = "http://${props.redisHost}:$redisAttestationPort/attest?nonce=$nonce"
        return jacksonObjectMapper().readValue(
            get(
                redisAttestEndpoint,
                auth = BasicAuthorization(remoteAttestationServerUsername, remoteAttestationServerPassword)
            ).text
        )
    }

    override fun attestProxy(nonce: String): RemoteAttestation {
        val jarHash = hashFile(proxyJarChallenge)
        val jarChallenge = AttestationChallenge(proxyJarChallenge, jarHash)

        val mrEnclave = readMrEnclave(proxyMrEnclaveChallenge)
        val mrEnclaveChallenge = AttestationChallenge(proxyMrEnclaveChallenge, mrEnclave)

        val noncePlusOne = nonce.toInt() + 1
        val quoteSignature = signData("$jarChallenge|$mrEnclave|$noncePlusOne")

        return RemoteAttestation(
            AttestationQuote(
                listOf(jarChallenge, mrEnclaveChallenge),
                noncePlusOne,
                quoteSignature
            )
        )
    }

    private fun hashFile(file: String): String {
        val digest: MessageDigest = MessageDigest.getInstance(attestationHashingAlgorithm)

        val fis = FileInputStream(file)
        val byteArray = ByteArray(1024)
        var bytesCount: Int

        while (fis.read(byteArray).also { bytesCount = it } != -1) {
            digest.update(byteArray, 0, bytesCount)
        }
        fis.close()

        val bytes: ByteArray = digest.digest()
        return bytes.toHexString()
    }

    private fun readMrEnclave(filePath: String): String {
        val file = File(filePath)
        return file.readText()
    }

    private fun signData(data: String): String {
        val signature: Signature = Signature.getInstance(
            attestationSignatureAlgorithm,
            attestationSignatureProvider
        )

        val signingKey = KeystoreUtils.getKeyPairFromKeyStore(
            proxyIdentityKeyKeystoreType,
            proxyIdentityKeyKeystore,
            proxyIdentityKeyKeystorePassword,
            proxyIdentityKeyName,
            proxyIdentityKeyNamePassword
        )!!

        signature.initSign(signingKey.private)

        signature.update(data.toByteArray())
        val sigBytes: ByteArray = signature.sign()

        return Base64.getEncoder().encodeToString(sigBytes)
    }

    private fun ByteArray.toHexString(): String {
        val sb = StringBuilder()
        this.forEach { b ->
            sb.append(String.format("%02X", b))
        }

        return sb.toString().toLowerCase()
    }
}
