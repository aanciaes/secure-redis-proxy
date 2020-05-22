package hlib.hj.mlib

import org.junit.jupiter.api.Test

class HomoOpeIntTest {

    @Test
    fun `Test that encrypted values maintain order` () {
        val ope = HomoOpeInt()

        val one = 1
        val two = 2
        val three = 3

        assert(one < two)
        assert(one < three)
        assert(two < three)

        val oneEnc = ope.encrypt(one)
        val twoEnc = ope.encrypt(two)
        val threeEnc = ope.encrypt(three)

        assert(oneEnc < twoEnc)
        assert(oneEnc < threeEnc)
        assert(twoEnc < threeEnc)

        val oneDec = ope.decrypt(oneEnc)
        val twoDec = ope.decrypt(twoEnc)
        val threeDec = ope.decrypt(threeEnc)

        assert(oneDec < twoDec)
        assert(oneDec < threeDec)
        assert(twoDec < threeDec)
    }
}
