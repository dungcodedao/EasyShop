package com.example.easyshop.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Quản lý ngôn ngữ riêng biệt cho User và Admin.
 *
 * Cơ chế:
 * - Lưu: user_lang / admin_lang (SharedPrefs) — riêng biệt theo role
 * - Lưu: current_lang — ngôn ngữ đang áp dụng (dùng cho attachBaseContext)
 * - Apply: wrapContext() trong attachBaseContext của MainActivity → áp dụng khi recreate
 * - Trigger: gọi Activity.recreate() sau khi thay đổi → UI refresh hoàn toàn
 */
object LanguageManager {

    private const val PREFS_NAME     = "easyshop_language_prefs"
    private const val KEY_USER_LANG  = "user_lang"
    private const val KEY_ADMIN_LANG = "admin_lang"
    private const val KEY_CURRENT    = "current_lang"   // ngôn ngữ đang thực sự áp dụng

    const val LANG_VI = "vi"
    const val LANG_EN = "en"

    // ── Context wrapping (gọi trong attachBaseContext) ─────────────────────────

    /**
     * Bọc context với locale đã lưu. Gọi trong MainActivity.attachBaseContext().
     * Đây là cách DUY NHẤT đảm bảo locale thay đổi ngay cả trên API < 33.
     */
    fun wrapContext(base: Context): Context {
        val lang   = getCurrentLang(base)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    // ── Setters (lưu + áp dụng) ───────────────────────────────────────────────

    /** Lưu ngôn ngữ User, đánh dấu là current, notify AppCompat (API 33+). */
    fun setUserLang(context: Context, langTag: String) {
        prefs(context).edit()
            .putString(KEY_USER_LANG, langTag)
            .putString(KEY_CURRENT, langTag)
            .apply()
        notifyAppCompat(langTag)
    }

    /** Lưu ngôn ngữ Admin, đánh dấu là current, notify AppCompat (API 33+). */
    fun setAdminLang(context: Context, langTag: String) {
        prefs(context).edit()
            .putString(KEY_ADMIN_LANG, langTag)
            .putString(KEY_CURRENT, langTag)
            .apply()
        notifyAppCompat(langTag)
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    fun getUserLang(context: Context): String =
        prefs(context).getString(KEY_USER_LANG, LANG_VI) ?: LANG_VI

    fun getAdminLang(context: Context): String =
        prefs(context).getString(KEY_ADMIN_LANG, LANG_VI) ?: LANG_VI

    fun getCurrentLang(context: Context): String =
        prefs(context).getString(KEY_CURRENT, LANG_VI) ?: LANG_VI

    // ── Startup helpers ────────────────────────────────────────────────────────

    /** Áp dụng locale User khi User section khởi động (dùng ở màn home). */
    fun applyUserLocale(context: Context) {
        val lang = getUserLang(context)
        prefs(context).edit().putString(KEY_CURRENT, lang).apply()
        notifyAppCompat(lang)
    }

    /** Áp dụng locale Admin khi Admin section khởi động. */
    fun applyAdminLocale(context: Context) {
        val lang = getAdminLang(context)
        prefs(context).edit().putString(KEY_CURRENT, lang).apply()
        notifyAppCompat(lang)
    }

    // ── Display ────────────────────────────────────────────────────────────────

    fun displayName(langTag: String): String = when (langTag) {
        LANG_EN -> "English"
        else    -> "Tiếng Việt"
    }

    // ── Private ────────────────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Thông báo cho hệ thống Android 13+ biết locale đã thay đổi. */
    private fun notifyAppCompat(langTag: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(langTag)
        )
    }
}

// ── Category name translations ────────────────────────────────────────────────

/**
 * Dịch tên danh mục theo "Translations Map Pattern":
 *
 * Firestore document structure:
 * {
 *   "name": "Điện thoại",           ← tên gốc (VI)
 *   "translations": {                ← map tự động mở rộng
 *     "vi": "Điện thoại",
 *     "en": "Smartphone",
 *     "ja": "スマートフォン",          ← thêm ngôn ngữ mới không cần sửa code
 *   }
 * }
 *
 * Thứ tự ưu tiên:
 *   1. translations[langTag] từ Firestore  (chuẩn nhất, admin quản lý)
 *   2. fallbackMap tĩnh trong code         (dùng cho categories đã có sẵn)
 *   3. Tên gốc (name)                      (an toàn tuyệt đối)
 */
object CategoryTranslations {

    /** Bảng dịch tĩnh VI → EN (fallback khi Firestore chưa có translations) */
    private val fallbackMap: Map<String, String> = mapOf(
        "Máy tính"      to "Computer",
        "Laptop"        to "Laptop",
        "Macbook"       to "MacBook",
        "Điện thoại"    to "Smartphone",
        "Tivi"          to "TV",
        "Đồng hồ"       to "Watch",
        "Máy ảnh"       to "Camera",
        "Tai nghe"      to "Headphone",
        "Loa"           to "Speaker",
        "Bàn phím"      to "Keyboard",
        "Chuột"         to "Mouse",
        "Máy tính bảng" to "Tablet",
        "Phụ kiện"      to "Accessory",
        "Gaming"        to "Gaming",
    )

    /**
     * Trả về tên category đúng ngôn ngữ.
     *
     * @param defaultName   Tên gốc từ Firestore (field `name`)
     * @param translations  Map đa ngôn ngữ từ Firestore (field `translations`)
     * @param langTag       Ngôn ngữ hiện tại, ví dụ: "en", "vi", "ja"
     */
    fun localize(
        defaultName: String,
        translations: Map<String, String> = emptyMap(),
        langTag: String = LanguageManager.LANG_VI
    ): String {
        if (langTag == LanguageManager.LANG_VI) return defaultName
        // 1. Tra translations map từ Firestore
        translations[langTag]?.takeIf { it.isNotBlank() }?.let { return it }
        // 2. Fallback về bảng tĩnh trong code (chỉ hỗ trợ EN)
        if (langTag == LanguageManager.LANG_EN) {
            fallbackMap[defaultName]?.let { return it }
        }
        // 3. Trả về tên gốc
        return defaultName
    }

    // ── Convenience overload cho code hiện tại ───────────────────────────────

    /** Overload đơn giản dùng Boolean isEnglish (backward compatible) */
    fun localize(defaultName: String, isEnglish: Boolean): String =
        localize(
            defaultName = defaultName,
            langTag = if (isEnglish) LanguageManager.LANG_EN else LanguageManager.LANG_VI
        )
}
