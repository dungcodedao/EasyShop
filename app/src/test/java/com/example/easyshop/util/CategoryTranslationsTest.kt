package com.example.easyshop.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTranslationsTest {

    @Test
    fun `localize with Vietnamese tag should return default name`() {
        val result = CategoryTranslations.localize(
            defaultName = "Điện thoại",
            translations = mapOf("en" to "Smartphone"),
            langTag = "vi"
        )
        assertEquals("Điện thoại", result)
    }

    @Test
    fun `localize with English tag and valid translation should return translated name`() {
        val result = CategoryTranslations.localize(
            defaultName = "Điện thoại",
            translations = mapOf("en" to "Smartphone"),
            langTag = "en"
        )
        assertEquals("Smartphone", result)
    }

    @Test
    fun `localize with English tag and missing translation should use fallback map`() {
        // "Máy tính" is in fallbackMap as "Computer"
        val result = CategoryTranslations.localize(
            defaultName = "Máy tính",
            translations = emptyMap(),
            langTag = "en"
        )
        assertEquals("Computer", result)
    }

    @Test
    fun `localize with English tag, missing translation and missing fallback should return default name`() {
        val result = CategoryTranslations.localize(
            defaultName = "Unknown Category",
            translations = emptyMap(),
            langTag = "en"
        )
        assertEquals("Unknown Category", result)
    }

    @Test
    fun `localize with Japanese tag and valid translation should return translated name`() {
        val result = CategoryTranslations.localize(
            defaultName = "Điện thoại",
            translations = mapOf("ja" to "スマートフォン"),
            langTag = "ja"
        )
        assertEquals("スマートフォン", result)
    }

    @Test
    fun `localize with Boolean overload should work correctly`() {
        // Test English
        val resultEn = CategoryTranslations.localize("Máy tính", true)
        assertEquals("Computer", resultEn)

        // Test Vietnamese
        val resultVi = CategoryTranslations.localize("Máy tính", false)
        assertEquals("Máy tính", resultVi)
    }
}
