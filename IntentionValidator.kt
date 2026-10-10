package com.intentphone.launcher.data.validator

data class ValidationResult(
    val isValid: Boolean,
    val wordCount: Int,
    val message: String?,
    val isMeaningless: Boolean
)

object IntentionValidator {
    private val repeatedCharRegex = Regex("""(.)\1{4,}""")

    fun extractValidWords(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val tokens = text.trim().split(Regex("""\s+"""))
        return tokens.mapNotNull { raw ->
            val cleaned = raw.replace(Regex("""^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"""), "")
            if (cleaned.contains(Regex("""[a-zA-Z]"""))) cleaned else null
        }
    }

    fun isMeaninglessOrRepeated(text: String, words: List<String>): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (repeatedCharRegex.containsMatchIn(trimmed)) return true
        if (words.size >= 3) {
            val lowerWords = words.map { it.lowercase() }
            val uniqueCount = lowerWords.distinct().size
            if (words.size >= 5 && uniqueCount <= 2) return true
            if (words.all { it.length == 1 }) return true
        }
        return false
    }

    fun validateQuestion1(text: String): ValidationResult {
        val words = extractValidWords(text)
        val wordCount = words.size
        if (isMeaninglessOrRepeated(text, words)) {
            return ValidationResult(false, wordCount, "Please enter a meaningful intention.", true)
        }
        if (wordCount < 5) {
            return ValidationResult(false, wordCount, "Please write at least 5 words about why you are using your phone.", false)
        }
        return ValidationResult(true, wordCount, null, false)
    }

    fun validateQuestion2(text: String): ValidationResult {
        val words = extractValidWords(text)
        val wordCount = words.size
        if (isMeaninglessOrRepeated(text, words)) {
            return ValidationResult(false, wordCount, "Please enter a meaningful intention.", true)
        }
        if (wordCount < 5) {
            return ValidationResult(false, wordCount, "Please describe what you want to accomplish in at least 5 words.", false)
        }
        return ValidationResult(true, wordCount, null, false)
    }
}