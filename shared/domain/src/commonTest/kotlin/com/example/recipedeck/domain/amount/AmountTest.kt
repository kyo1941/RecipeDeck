package com.example.recipedeck.domain.amount

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AmountTest {
    @Test
    fun 入力した数量と入力形式と単位が分量になる() {
        val amount = Amount.ofOrNull(quantityOrFail(1, 2), QuantityNotation.DECIMAL, AmountUnit.PIECE)

        assertEquals(Amount(quantityOrFail(1, 2), QuantityNotation.DECIMAL, AmountUnit.PIECE), amount)
    }

    @Test
    fun 単位を入力しなければ単位なしの分量になる() {
        val amount = Amount.ofOrNull(quantityOrFail(2), QuantityNotation.FRACTION, unit = null)

        assertEquals(Amount(quantityOrFail(2), QuantityNotation.FRACTION, unit = null), amount)
    }

    @Test
    fun 単位だけを入力すると分量なしになる() {
        assertNull(Amount.ofOrNull(quantity = null, QuantityNotation.FRACTION, AmountUnit.GRAM))
    }
}
