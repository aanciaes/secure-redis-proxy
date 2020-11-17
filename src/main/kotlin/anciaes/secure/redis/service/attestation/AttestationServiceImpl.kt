package anciaes.secure.redis.service.attestation

/* ktlint-disable */
import anciaes.secure.redis.model.ApplicationProperties
import anciaes.secure.redis.model.AttestationChallenge
import anciaes.secure.redis.model.AttestationQuote
import anciaes.secure.redis.model.CpuInfo
import anciaes.secure.redis.model.RedisNodeRemoteAttestation
import anciaes.secure.redis.model.RemoteAttestation
import anciaes.secure.redis.model.SystemAttestation
import anciaes.secure.redis.utils.KeystoreUtils
import anciaes.secure.redis.utils.toHexString
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
    val proxyIdentityKeyKeystore: String
        get() = if (activeProfile == "prod") "production-keystores/proxy-attestation-identity-key.p12" else "keystores/proxy-attestation-identity-key.p12"
    val proxyIdentityKeyKeystorePassword: String
        get() = if (activeProfile == "prod") "Gwb!KMcF37rYHsTmHiLkFs9ms" else "Lcq6jCG-GFhnfqLK4PhyvWFj_"
    val proxyIdentityKeyName: String
        get() = if (activeProfile == "prod") "proxy-attestation-identity-key" else "proxy-attetstation-identity-key"
    val proxyIdentityKeyNamePassword: String
        get() = if (activeProfile == "prod") "Gwb!KMcF37rYHsTmHiLkFs9ms" else "Lcq6jCG-GFhnfqLK4PhyvWFj_"
    val proxyJarChallenge: String
        get() = if (activeProfile == "prod") "/home/secure-proxy-redis/secure-redis-proxy-1.3.0.jar" else "mock-files/mock-proxy-java-jar"
    val proxyMrEnclaveChallenge: String
        get() = if (activeProfile == "prod") "/home/secure-proxy-redis/mrenclave" else "mock-files/mrenclave-mock"
    val cpuInfoFilePath: String
        get() = if (activeProfile == "prod") "/proc/cpuinfo" else "mock-files/cpuinfo"
    val osInfoFilePath: String
        get() = if (activeProfile == "prod") "/etc/os-release" else "mock-files/os-release"

    val attestationSignatureAlgorithm = "SHA512withRSA"
    val attestationSignatureProvider = "SunRsaSign"
    val attestationHashingAlgorithm = "sha-256"

    val remoteAttestationServerUsername = "redis-proxy-cd497e6b-10cf-4ed2-be63-18b6b16375f6"
    val remoteAttestationServerPassword = "2grvTkqUhS7wE4R4WRjnuXTLzsxnAu"
    //

    @Autowired
    lateinit var props: ApplicationProperties

    override fun attestRedis(nonce: String): List<RedisNodeRemoteAttestation> {
        val redisNodeAttestation = mutableListOf<RedisNodeRemoteAttestation>()
        val redisAttestEndpoint = "http://${props.redisHost}:$redisAttestationPort/attest?nonce=$nonce"
        val remoteQuote = jacksonObjectMapper().readValue<RemoteAttestation>(
            get(
                redisAttestEndpoint,
                auth = BasicAuthorization(remoteAttestationServerUsername, remoteAttestationServerPassword)
            ).text
        )

        redisNodeAttestation.add(
            RedisNodeRemoteAttestation(
                "${props.redisHost}:$redisAttestationPort",
                remoteQuote.quote
            )
        )
        if (props.replicationEnabled) {
            val nodes = props.replicationNodes ?: listOf()
            // TODO: perform all request in an asynchronous way
            for (node in nodes) {
                val nodeQuote = jacksonObjectMapper().readValue<RemoteAttestation>(
                    get(
                        "http://${node.host}:${node.attestationPort}/attest?nonce=$nonce",
                        auth = BasicAuthorization(remoteAttestationServerUsername, remoteAttestationServerPassword)
                    ).text
                )

                redisNodeAttestation.add(
                    RedisNodeRemoteAttestation(
                        "${node.host}:${node.attestationPort}",
                        nodeQuote.quote
                    )
                )
            }
        }

        return redisNodeAttestation
    }

    override fun attestProxy(nonce: String): RemoteAttestation {
        val jarHash = hashFile(proxyJarChallenge)
        val jarChallenge = AttestationChallenge(proxyJarChallenge, jarHash)

        val mrEnclave = readMrEnclave(proxyMrEnclaveChallenge)
        val mrEnclaveChallenge = AttestationChallenge(proxyMrEnclaveChallenge, mrEnclave)

        val cpuInfo = getCpuInfo()
        val osInfo = getOsInfo()

        val noncePlusOne = nonce.toInt() + 1
        val quoteSignature =
            signData("$jarChallenge|$mrEnclave|${cpuInfo.processorCount}|${cpuInfo.processorModel}|$osInfo|$noncePlusOne")

        return RemoteAttestation(
            quote = AttestationQuote(
                listOf(jarChallenge, mrEnclaveChallenge),
                noncePlusOne,
                SystemAttestation(cpuInfo.processorCount, cpuInfo.processorModel, osInfo),
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

    private fun getCpuInfo(): CpuInfo {
        val processorCount = Runtime.getRuntime().availableProcessors()

        val cpuInfoFile = File(cpuInfoFilePath).readLines()
        val processorModelLine = cpuInfoFile.find { it.startsWith("model name") }!!
        val processorModel = processorModelLine.split(":").last().trim()

        return CpuInfo(processorCount, processorModel)
    }

    private fun getOsInfo(): String {
        val osInfoFile = File(osInfoFilePath).readLines()
        val osInfoLine = osInfoFile.find { it.startsWith("PRETTY_NAME") }!!
        return osInfoLine.split("=").last().replace("\"", "").trim()
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
        )

        signature.initSign(signingKey.private)

        signature.update(data.toByteArray())
        val sigBytes: ByteArray = signature.sign()

        return Base64.getEncoder().encodeToString(sigBytes)
    }
}
