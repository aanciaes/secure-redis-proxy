package anciaes.secure.redis.model

data class RemoteAttestation(val quote: AttestationQuote)
data class AttestationQuote(val challenges: List<AttestationChallenge>, val nonce: Int, val quoteSignature: String)
data class AttestationChallenge(val filename: String, val hash: String)

data class ApplicationAttestation(var redis: RemoteAttestation, val proxy: RemoteAttestation)
