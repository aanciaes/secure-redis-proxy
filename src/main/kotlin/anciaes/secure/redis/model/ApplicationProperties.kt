package anciaes.secure.redis.model

enum class ReplicationMode {
    MasterSlave,
    Cluster
}

data class RedisNode(val host: String, val port: String, val attestationPort: String)

data class ApplicationProperties(
    val redisEncrypted: Boolean,
    val redisEncryptedIsHomomorphic: Boolean,

    val redisHost: String,
    val redisPort: Int,

    val redisAuthentication: Boolean,
    val redisUsername: String?,
    val redisPassword: String?,

    val tlsEnabled: Boolean,
    val tlsKeystorePath: String?,
    val tlsKeystorePassword: String?,
    val tlsTruststorePath: String?,
    val tlsTruststorePassword: String?,

    val replicationEnabled: Boolean,
    val replicationMode: ReplicationMode,
    val replicationNodes: List<RedisNode>?,

    val keyEncryptionDetSecret: String?,
    val keyEncryptionOpeSecret: String?,
    val keyEncryptionAddSecret: String?,
    val keyEncryptionSearchSecret: String?,

    val dataEncryptionCipherSuite: String?,
    val dataEncryptionProvider: String?,
    val dataEncryptionKeystoreType: String?,
    val dataEncryptionKeystore: String?,
    val dataEncryptionKeystorePassword: String?,
    val dataEncryptionKeystoreKeyName: String?,
    val dataEncryptionKeystoreKeyPassword: String?,

    val dataSignatureAlgorithm: String?,
    val dataSignatureProvider: String?,
    val dataSignatureKeystoreType: String?,
    val dataSignatureKeystore: String?,
    val dataSignatureKeystorePassword: String?,
    val dataSignatureKeystoreKeyName: String?,
    val dataSignatureKeystoreKeyPassword: String?,

    val dataHMacAlgorithm: String?,
    val dataHMacProvider: String?,
    val dataHMacKeystoreType: String?,
    val dataHMacKeystore: String?,
    val dataHMacKeystorePassword: String?,
    val dataHMacKeyName: String?,
    val dataHMacKeyPassword: String?
)
