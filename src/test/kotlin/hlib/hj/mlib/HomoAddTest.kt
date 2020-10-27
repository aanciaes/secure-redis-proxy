package hlib.hj.mlib

/* ktlint-disable */
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.Random
/* ktlint-enable */

class HomoAddTest {

    @Test
    fun `Test that encryption and decryption works correctly`() {
        val key: PaillierKey = HomoAdd.generateKey()
        val clearText = BigInteger(Random().nextInt(10000).toString())

        val encryptedText = HomoAdd.encrypt(clearText, key)
        val decryptedText = HomoAdd.decrypt(encryptedText, key)

        assertEquals(clearText, decryptedText)
    }

    @Test
    fun `Test sum operation`() {
        val key: PaillierKey = HomoAdd.generateKey()
        val firstClearTextValue = BigInteger(Random().nextInt(10000).toString())
        val secondClearTextValue = BigInteger(Random().nextInt(10000).toString())

        val firstEncryptedValue = HomoAdd.encrypt(firstClearTextValue, key)
        val secondEncryptedValue = HomoAdd.encrypt(secondClearTextValue, key)

        val clearTextSum = firstClearTextValue.add(secondClearTextValue)
        val encryptedSum = HomoAdd.sum(firstEncryptedValue, secondEncryptedValue, key.nsquare)

        val decryptedSum = HomoAdd.decrypt(encryptedSum, key)

        assertEquals(clearTextSum, decryptedSum)
    }

    @Test
    fun `Test subtraction operation - with sum with negative value`() {
        val key: PaillierKey = HomoAdd.generateKey()
        val firstClearTextValue = BigInteger(Random().nextInt(10000).toString())
        val secondClearTextValue = BigInteger(1.toString())

        val firstEncryptedValue = HomoAdd.encrypt(firstClearTextValue, key)
        val secondEncryptedValue = HomoAdd.encrypt(secondClearTextValue, key)

        val clearTextSum = firstClearTextValue.subtract(secondClearTextValue)
        val encryptedSum = HomoAdd.dif(firstEncryptedValue, secondEncryptedValue, key.nsquare)

        val decryptedSum = HomoAdd.decrypt(encryptedSum, key)

        assertEquals(clearTextSum, decryptedSum)
    }

    @Test
    fun `Test paillier key to and from string`() {
        val paillierKey: PaillierKey = HomoAdd.generateKey()
        val keyString = HomoAdd.stringFromKey(paillierKey)

        val convertedPaillierKey = HomoAdd.keyFromString(keyString)

        assertEquals(paillierKey.g, convertedPaillierKey.g)
        assertEquals(paillierKey.lambda, convertedPaillierKey.lambda)
        assertEquals(paillierKey.mu, convertedPaillierKey.mu)
        assertEquals(paillierKey.n, convertedPaillierKey.n)
        assertEquals(paillierKey.nsquare, convertedPaillierKey.nsquare)
        assertEquals(paillierKey.p, convertedPaillierKey.p)
        assertEquals(paillierKey.q, convertedPaillierKey.q)
    }

    @Test
    fun `Test HomoAdd Multiplication`() {
        val key: PaillierKey = HomoAdd.generateKey()
        val clearTextValue = BigInteger(Random().nextInt(10).toString())
        val multValue = 2

        val firstEncryptedValue = HomoAdd.encrypt(clearTextValue, key)

        val clearTextMult = clearTextValue.multiply(BigInteger(multValue.toString()))
        val encryptedMult = HomoAdd.mult(firstEncryptedValue, multValue, key.nsquare)

        val decryptedMult = HomoAdd.decrypt(encryptedMult, key)

        assertEquals(clearTextMult, decryptedMult)
    }
}
