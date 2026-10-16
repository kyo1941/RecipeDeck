package com.example.recipedeck.domain.amount

@ConsistentCopyVisibility
data class Quantity private constructor(
    val numerator: Long,
    val denominator: Long,
) {
    fun plus(other: Quantity): Quantity? {
        val left = numerator.timesOrNull(other.denominator) ?: return null
        val right = other.numerator.timesOrNull(denominator) ?: return null
        val sumNumerator = left.plusOrNull(right) ?: return null
        val sumDenominator = denominator.timesOrNull(other.denominator) ?: return null
        return of(sumNumerator, sumDenominator)
    }

    fun times(multiplier: Multiplier): Quantity? {
        val productNumerator = numerator.timesOrNull(multiplier.numerator) ?: return null
        val productDenominator = denominator.timesOrNull(multiplier.denominator) ?: return null
        return of(productNumerator, productDenominator)
    }

    companion object {
        fun of(numerator: Long, denominator: Long = 1): Quantity? {
            if (numerator <= 0 || denominator <= 0) return null
            val divisor = gcd(numerator, denominator)
            return Quantity(numerator / divisor, denominator / divisor)
        }
    }
}
