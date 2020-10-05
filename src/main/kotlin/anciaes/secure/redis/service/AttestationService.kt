package anciaes.secure.redis.service

import anciaes.secure.redis.model.RedisNodeRemoteAttestation
import anciaes.secure.redis.model.RemoteAttestation

interface AttestationService {
    fun attestRedis(nonce: String): List<RedisNodeRemoteAttestation>
    fun attestProxy(nonce: String): RemoteAttestation
}
