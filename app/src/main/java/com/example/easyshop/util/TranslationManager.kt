package com.example.easyshop.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Singleton quản lý ML Kit on-device translation.
 *
 * - Download model ~30MB lần đầu (cần internet), sau đó offline hoàn toàn
 * - Cache translator để tránh tạo lại nhiều lần
 * - Hỗ trợ mọi cặp ngôn ngữ ML Kit hỗ trợ (59 ngôn ngữ)
 */
object TranslationManager {

    // Cache translator instances theo key "sourceLang_targetLang"
    private val translatorCache = mutableMapOf<String, com.google.mlkit.nl.translate.Translator>()

    /**
     * Dịch một đoạn text bất đồng bộ (suspend).
     *
     * @param text       Text cần dịch
     * @param sourceLang Ngôn ngữ nguồn, dùng [TranslateLanguage] constants
     * @param targetLang Ngôn ngữ đích
     * @return Text đã dịch, hoặc text gốc nếu dịch thất bại
     */
    suspend fun translate(
        text: String,
        sourceLang: String = TranslateLanguage.VIETNAMESE,
        targetLang: String = TranslateLanguage.ENGLISH
    ): String {
        if (text.isBlank()) return text
        if (sourceLang == targetLang) return text

        val translator = getOrCreateTranslator(sourceLang, targetLang)

        return try {
            // Đảm bảo model đã được download
            ensureModelDownloaded(translator)
            // Dịch
            translateText(translator, text)
        } catch (e: Exception) {
            e.printStackTrace()
            text // Fallback an toàn: trả về text gốc
        }
    }

    /**
     * Dịch một Map<String, String> (dùng cho otherDetails/specifications).
     * Dịch cả key lẫn value.
     */
    suspend fun translateMap(
        map: Map<String, String>,
        sourceLang: String = TranslateLanguage.VIETNAMESE,
        targetLang: String = TranslateLanguage.ENGLISH
    ): Map<String, String> {
        if (map.isEmpty() || sourceLang == targetLang) return map
        return map.entries.associate { (key, value) ->
            translate(key, sourceLang, targetLang) to translate(value, sourceLang, targetLang)
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun getOrCreateTranslator(
        sourceLang: String,
        targetLang: String
    ): com.google.mlkit.nl.translate.Translator {
        val key = "${sourceLang}_${targetLang}"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }

    private suspend fun ensureModelDownloaded(
        translator: com.google.mlkit.nl.translate.Translator
    ) = suspendCancellableCoroutine { cont ->
        // Không giới hạn Wifi — download trên mọi kết nối (model ~30MB, chỉ 1 lần)
        translator.downloadModelIfNeeded()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    private suspend fun translateText(
        translator: com.google.mlkit.nl.translate.Translator,
        text: String
    ): String = suspendCancellableCoroutine { cont ->
        translator.translate(text)
            .addOnSuccessListener { result -> cont.resume(result) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    /** Giải phóng tất cả translator khi không dùng nữa */
    fun closeAll() {
        translatorCache.values.forEach { it.close() }
        translatorCache.clear()
    }
}
