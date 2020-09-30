package anciaes.secure.redis.service

import anciaes.secure.redis.model.RemoteAttestation

interface AttestationService {
    fun attestRedis(nonce: String): RemoteAttestation
    fun attestProxy(): RemoteAttestation
}
