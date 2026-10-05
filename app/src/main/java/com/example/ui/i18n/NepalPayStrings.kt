package com.example.ui.i18n

import java.text.DecimalFormat

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    NEPALI("ne", "नेपाली")
}

object NprFormatter {
    private val formatter = DecimalFormat("#,##,##0.00")

    fun format(amount: Double, lang: AppLanguage = AppLanguage.ENGLISH, showCode: Boolean = true): String {
        val formattedNumber = formatter.format(amount)
        return if (lang == AppLanguage.NEPALI) {
            if (showCode) "रु. $formattedNumber (NPR)" else "रु. $formattedNumber"
        } else {
            if (showCode) "NPR रु. $formattedNumber" else "रु. $formattedNumber"
        }
    }
}

data class BiText(
    val en: String,
    val ne: String
) {
    fun get(lang: AppLanguage): String = if (lang == AppLanguage.NEPALI) ne else en
}

fun tr(lang: AppLanguage, en: String, ne: String): String =
    if (lang == AppLanguage.NEPALI) ne else en
