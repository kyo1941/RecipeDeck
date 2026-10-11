package com.example.recipe_deck.domain.amount

@ConsistentCopyVisibility
data class Multiplier private constructor(
    val numerator: Long,
    val denominator: Long,
) {
    companion object {
        fun of(numerator: Long, denominator: Long = 1): Multiplier? {
            if (numerator <= 0 || denominator <= 0) return null
            val divisor = gcd(numerator, denominator)
            return Multiplier(numerator / divisor, denominator / divisor)
        }
    }
}
