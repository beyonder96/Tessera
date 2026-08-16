package com.example.utils

/**
 * Converte com segurança strings contendo números no formato brasileiro (com vírgula)
 * ou internacional (com ponto) para Double.
 * 
 * Exemplos:
 * "15,50" -> 15.50
 * "1.250,50" -> 1250.50
 * "1250.50" -> 1250.50
 * "" / "abc" -> null
 */
fun String?.toDoubleClean(): Double? {
    if (this.isNullOrBlank()) return null
    val clean = this.trim()
    val hasTrailingSeparator = clean.endsWith(".") || clean.endsWith(",")
    val normalizedClean = if (hasTrailingSeparator) clean.dropLast(1) else clean
    val lastComma = normalizedClean.lastIndexOf(',')
    val lastPoint = normalizedClean.lastIndexOf('.')
    
    return try {
        if (lastComma > lastPoint) {
            // Formato BR: "1.500,50" ou "50,25"
            val normalized = normalizedClean.replace(".", "").replace(',', '.')
            normalized.toDoubleOrNull()
        } else if (lastPoint > lastComma) {
            // Formato EN: "1,500.50" ou "50.25"
            val normalized = normalizedClean.replace(",", "")
            normalized.toDoubleOrNull()
        } else {
            normalizedClean.toDoubleOrNull()
        }
    } catch (e: Exception) {
        null
    }
}

fun String?.toDoubleCleanOrZero(): Double {
    return this.toDoubleClean() ?: 0.0
}
