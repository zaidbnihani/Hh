package com.junkfood.seal

import com.junkfood.seal.util.connectWithDelimiter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTextJoin() {
        assertEquals(
            connectWithDelimiter("123", "456", "789", delimiter = ","),
            listOf(123, 456, 789).joinToString(separator = ",") { it.toString() })
        assertEquals(connectWithDelimiter(delimiter = ","), "")
        assertEquals(emptyList<String>().joinToString(separator = ",") { it }, "")
    }

    @Test
    fun testJsRuntimeCommandSanitization() {
        val sensitiveArgs = listOf(
            "--cookies", "/data/user/0/com.junkfood.seal/files/cookies.txt",
            "-o", "%(title).200B.%(ext)s",
            "--add-header", "Cookie: SID=SECRET_TOKEN; HSID=SECRET_HSID",
            "--add-header", "User-Agent: Mozilla/5.0",
            "--password", "MySuperSecretPassword123",
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ"
        )

        val sanitized = com.junkfood.seal.util.JsRuntimeUtil.sanitizeCommand(sensitiveArgs)

        // Verify cookies and password paths/values are redacted
        org.junit.Assert.assertTrue(sanitized.contains("[REDACTED_COOKIE_PATH]"))
        org.junit.Assert.assertTrue(sanitized.contains("[REDACTED_PASSWORD]"))
        org.junit.Assert.assertTrue(sanitized.any { it.contains("[REDACTED]") })
        org.junit.Assert.assertFalse(sanitized.contains("/data/user/0/com.junkfood.seal/files/cookies.txt"))
        org.junit.Assert.assertFalse(sanitized.contains("MySuperSecretPassword123"))
        org.junit.Assert.assertFalse(sanitized.any { it.contains("SECRET_TOKEN") })
    }
}