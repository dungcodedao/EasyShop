package com.example.easyshop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho AppUtil - chỉ test các pure functions không cần Android Context.
 * formatCurrency/formatPrice cần getString (Context) nên không test ở JVM unit test.
 */
class AppUtilTest {

    // ─── parsePrice ─────────────────────────────────────────────────────────────

    @Test
    fun `parsePrice should correctly parse Vietnamese dot-thousands format`() {
        assertEquals(80000.0, AppUtil.parsePrice("80.000"), 0.1)
        assertEquals(1500000.0, AppUtil.parsePrice("1.500.000"), 0.1)
    }

    @Test
    fun `parsePrice should correctly parse comma-thousands format`() {
        assertEquals(80000.0, AppUtil.parsePrice("80,000"), 0.1)
        assertEquals(1500000.0, AppUtil.parsePrice("1,500,000"), 0.1)
    }

    @Test
    fun `parsePrice should handle currency suffix correctly`() {
        assertEquals(80000.0, AppUtil.parsePrice("80.000đ"), 0.1)
        assertEquals(80000.0, AppUtil.parsePrice("80,000đ"), 0.1)
    }

    @Test
    fun `parsePrice should handle decimal numbers`() {
        assertEquals(1500.50, AppUtil.parsePrice("1,500.50"), 0.1)
    }

    @Test
    fun `parsePrice should return 0 for empty string`() {
        assertEquals(0.0, AppUtil.parsePrice(""), 0.1)
    }

    // ─── generateOrderNumber ─────────────────────────────────────────────────────

    @Test
    fun `generateOrderNumber should start with ES- and have length 11`() {
        val orderNum = AppUtil.generateOrderNumber()
        assertTrue("Order number should start with 'ES-'", orderNum.startsWith("ES-"))
        assertEquals("Order number should be 11 chars", 11, orderNum.length) // ES- + 8 chars
    }

    @Test
    fun `generateOrderNumber should be uppercase`() {
        val orderNum = AppUtil.generateOrderNumber()
        assertEquals(orderNum, orderNum.uppercase())
    }

    // ─── translateSystemError (internal, tested via reflection-friendly visibility) ─

    @Test
    fun `translateSystemError should pass through unknown errors unchanged`() {
        val unknownError = "Nguyên văn lỗi không rõ"
        val result = AppUtil.translateSystemError(unknownError)
        assertEquals(unknownError, result)
    }

    @Test
    fun `translateSystemError should return empty string for null or blank`() {
        assertEquals("", AppUtil.translateSystemError(null))
        assertEquals("", AppUtil.translateSystemError(""))
        assertEquals("", AppUtil.translateSystemError("   "))
    }

    @Test
    fun `translateSystemError should return non-empty string for known error codes`() {
        // Chỉ verify rằng known error codes KHÔNG bị pass-through (có translation)
        val knownCodes = listOf(
            "invalid-credential",
            "email-already-in-use",
            "network-request-failed",
            "user-not-found",
            "wrong-password"
        )
        knownCodes.forEach { code ->
            val result = AppUtil.translateSystemError(code)
            assertTrue("'$code' should be translated, not returned as-is", result != code)
            assertTrue("'$code' translation should not be empty", result.isNotEmpty())
        }
    }
}
