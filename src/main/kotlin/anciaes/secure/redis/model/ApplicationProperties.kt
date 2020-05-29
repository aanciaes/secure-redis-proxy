package anciaes.secure.redis.model

data class ApplicationProperties (
    val secure: Boolean,

    val redisHost: String,
    val redisPort: Int,

    val redisUsername: String?,
    val redisPassword: String?,

    val tlsEnabled: Boolean,
    val tlsKeystorePath: String?,
    val tlsKeystorePassword: String?,
    val tlsTruststorePath: String?,
    val tlsTruststorePassword: String?,

    val isCluster: Boolean,
    val clusterContactNodes: List<String>?,

    val keyEncryptionDetSecret: String?,
    val keyEncryptionOpeSecret: String?,

    val dataEncryptionCipherSuite: String?,
    val dataEncryptionProvider: String?,
    val dataEncryptionSecret: String?,

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
