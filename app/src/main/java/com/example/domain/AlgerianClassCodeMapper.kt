package com.example.domain

/**
 * Maps Algerian digitization platform (منصة الرقمنة - وزارة التربية الوطنية)
 * numeric sheet codes to human-readable class designations.
 *
 * Example:
 *  "2400004" -> Level 4, Section 4 -> "4م4" (4M4)
 *  "2200001" -> Level 2, Section 1 -> "2م1" (2M1)
 *  "2100003" -> Level 1, Section 3 -> "1م3" (1M3)
 *  "2300002" -> Level 3, Section 2 -> "3م2" (3M2)
 */
object AlgerianClassCodeMapper {

    data class ParsedClassInfo(
        val classCode: String,
        val nameArabic: String,
        val nameFrench: String,
        val level: Int,
        val sectionNumber: Int,
        val isRecognizedPattern: Boolean
    )

    // Regex for standard Algerian digitization code: 2 + [level 1-4] + [zeros] + [section]
    private val DIGITIZED_PATTERN = Regex("^2([1-4])0*([1-9]\\d*)$")

    // Alternative pattern if already named like 4M4, 4م4, 2AM1, etc.
    private val ALTERNATIVE_PATTERN = Regex("^([1-4])\\s*(?:AM|am|M|m|م)?\\s*0*([1-9]\\d*)$")

    fun parseSheetCode(rawCode: String): ParsedClassInfo {
        val trimmed = rawCode.trim()

        // 1. Try standard 7-digit ministerial format (e.g. 2400004)
        val matchDig = DIGITIZED_PATTERN.find(trimmed)
        if (matchDig != null) {
            val level = matchDig.groupValues[1].toInt()
            val section = matchDig.groupValues[2].toInt()
            return ParsedClassInfo(
                classCode = trimmed,
                nameArabic = "${level}م${section}",
                nameFrench = "${level}M${section}",
                level = level,
                sectionNumber = section,
                isRecognizedPattern = true
            )
        }

        // 2. Try alternative format (e.g. 4M4 or 4م4)
        val matchAlt = ALTERNATIVE_PATTERN.find(trimmed)
        if (matchAlt != null) {
            val level = matchAlt.groupValues[1].toInt()
            val section = matchAlt.groupValues[2].toInt()
            return ParsedClassInfo(
                classCode = trimmed,
                nameArabic = "${level}م${section}",
                nameFrench = "${level}M${section}",
                level = level,
                sectionNumber = section,
                isRecognizedPattern = true
            )
        }

        // 3. Fallback for custom or direct names
        return ParsedClassInfo(
            classCode = trimmed,
            nameArabic = trimmed,
            nameFrench = trimmed,
            level = extractFirstDigit(trimmed) ?: 1,
            sectionNumber = extractLastDigit(trimmed) ?: 1,
            isRecognizedPattern = false
        )
    }

    private fun extractFirstDigit(text: String): Int? {
        val digit = text.firstOrNull { it.isDigit() }?.digitToIntOrNull()
        return if (digit in 1..4) digit else null
    }

    private fun extractLastDigit(text: String): Int? {
        val digits = text.filter { it.isDigit() }
        return digits.lastOrNull()?.digitToIntOrNull()
    }
}
