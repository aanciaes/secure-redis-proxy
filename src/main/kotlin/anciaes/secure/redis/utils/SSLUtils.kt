package anciaes.secure.redis.utils

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

object SSLUtils {

    fun getSSLContext(
        clientKeyStorePath: String,
        clientKeyStorePassword: String,
        clientTrustStorePath: String,
        clientTrustStorePassword: String
    ): SSLSocketFactory? {

        /*
          Client key and certificates are sent to server so it can authenticate the client
         */
        val clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val inputStream: InputStream =
            FileInputStream(File(clientKeyStorePath))
        clientKeyStore.load(inputStream, clientKeyStorePassword.toCharArray())
        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        )
        keyManagerFactory.init(clientKeyStore, clientKeyStorePassword.toCharArray())

        /*
         * CA certificate is used to authenticate server
         */
        val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val caInputStream: InputStream =
            FileInputStream(File(clientTrustStorePath))
        caKeyStore.load(caInputStream, clientTrustStorePassword.toCharArray())
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(caKeyStore)

        /*
         * Create SSL socket factory
         */
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)

        /*
         * Return the newly created socket factory object
         */
        return context.socketFactory
    }
}
