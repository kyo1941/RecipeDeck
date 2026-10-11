package com.example.recipe_deck.domain.amount

import kotlin.test.assertNotNull

fun quantityOrFail(numerator: Long, denominator: Long = 1): Quantity = assertNotNull(Quantity.of(numerator, denominator))

fun multiplierOrFail(numerator: Long, denominator: Long = 1): Multiplier = assertNotNull(Multiplier.of(numerator, denominator))
