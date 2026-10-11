package com.example.recipe_deck.domain.amount

internal tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

internal fun Long.timesOrNull(other: Long): Long? {
    val result = this * other
    return if (this != 0L && result / this != other) null else result
}

internal fun Long.plusOrNull(other: Long): Long? {
    val overflows = if (other > 0) this > Long.MAX_VALUE - other else this < Long.MIN_VALUE - other
    return if (overflows) null else this + other
}
