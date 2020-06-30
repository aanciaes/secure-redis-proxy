package hlib.hj.mlib

/* ktlint-disable */
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.crypto.SecretKey
/* ktlint-enable */

class HomoDetTest {

    @Test
    fun `Test that encryption and decryption works correctly`() {
        val key: SecretKey = HomoDet.generateKey()
        val clearText = UUID.randomUUID().toString()

        val encryptedText = HomoDet.encrypt(key, clearText)
        val decryptedText = HomoDet.decrypt(key, encryptedText)

        assertEquals(clearText, decryptedText)
    }

    @Test
    fun `Two encryptions with same key and same input, outputs same string`() {
        val key: SecretKey = HomoDet.generateKey()
        val clearText = UUID.randomUUID().toString()

        val encryptedTextOne = HomoDet.encrypt(key, clearText)
        val encryptedTextTwo = HomoDet.encrypt(key, clearText)

        assertEquals(encryptedTextOne, encryptedTextTwo)

        val decryptedTextOne = HomoDet.decrypt(key, encryptedTextOne)
        val decryptedTextTwo = HomoDet.decrypt(key, encryptedTextTwo)

        assertEquals(decryptedTextOne, decryptedTextTwo)
        assertEquals(clearText, decryptedTextOne)
        assertEquals(clearText, decryptedTextTwo)
    }
}
