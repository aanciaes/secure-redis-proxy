package anciaes.secure.redis.utils

import java.io.FileInputStream
import java.security.Key
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate

object KeystoreUtils {

    fun getKeyPairFromKeyStore(keyStoreType: String, keyStorePath: String, keyStorePassword: String, keyName: String, keyPassword: String): KeyPair? {
        return try {
            val keyStore = KeyStore.getInstance(keyStoreType)
            val stream = FileInputStream(keyStorePath)
            keyStore.load(stream, keyStorePassword.toCharArray())
            val privateKey = keyStore.getKey(
                keyName,
                keyPassword.toCharArray()
            ) as PrivateKey
            val cert: Certificate = keyStore.getCertificate(keyName)
            val publicKey: PublicKey = cert.publicKey
            KeyPair(publicKey, privateKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getKeyFromKeyStore(keyStoreType: String, keyStorePath: String, keyStorePassword: String, keyName: String, keyPassword: String): Key? {
        return try {
            val keyStore = KeyStore.getInstance(keyStoreType)
            // Keystore where symmetric keys are stored (type JCEKS)
            val stream = FileInputStream(keyStorePath)
            keyStore.load(stream, keyStorePassword.toCharArray())
            keyStore.getKey(keyName, keyPassword.toCharArray())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }
}
