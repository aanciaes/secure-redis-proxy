package hlib.hj.mlib

import org.junit.jupiter.api.Test

class HomoSearchTest {

    @Test
    fun `Test homo search`() {
        val key = HomoSearch.generateKey()
        val searchTerm = "world"
        val clearText = "hello world from homo search test"

        val encryptedText = HomoSearch.encrypt(key, clearText)

        val encryptedSearchTerm = HomoSearch.wordDigest64(key, searchTerm)

        assert(HomoSearch.pesquisa(encryptedSearchTerm, encryptedText))

        val searchSubText = "hello search"
        val encryptedSearchSubText = HomoSearch.encrypt(key, searchSubText)

        assert(HomoSearch.searchAll(encryptedSearchSubText, encryptedText))
    }
}
