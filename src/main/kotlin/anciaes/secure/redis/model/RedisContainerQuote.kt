package anciaes.secure.redis.model

data class ApplicationAttestation(val redis: List<RedisNodeRemoteAttestation>, val proxy: RemoteAttestation)

data class RedisNodeRemoteAttestation(val node: String, val quote: AttestationQuote)

data class RemoteAttestation(val quote: AttestationQuote)
data class AttestationQuote(val challenges: List<AttestationChallenge>, val nonce: Int, val quoteSignature: String)
data class AttestationChallenge(val filename: String, val hash: String)
