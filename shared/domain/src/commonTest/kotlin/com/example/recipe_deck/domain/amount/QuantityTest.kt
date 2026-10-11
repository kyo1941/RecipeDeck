package com.example.recipe_deck.domain.amount

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QuantityTest {
    @Test
    fun 約分された値として扱われる() {
        val quantity = assertNotNull(Quantity.of(2, 4))

        assertEquals(1, quantity.numerator)
        assertEquals(2, quantity.denominator)
        assertEquals(Quantity.of(1, 2), quantity)
    }

    @Test
    fun 分母を省略すると整数になる() {
        assertEquals(Quantity.of(200, 1), Quantity.of(200))
    }

    @Test
    fun 分数同士を誤差なく足せる() {
        assertEquals(quantityOrFail(5, 6), quantityOrFail(1, 2).plus(quantityOrFail(1, 3)))
    }

    @Test
    fun 足した結果も約分される() {
        assertEquals(quantityOrFail(1), quantityOrFail(1, 2).plus(quantityOrFail(1, 2)))
    }

    @Test
    fun 数量0は作れない() {
        assertNull(Quantity.of(0))
    }

    @Test
    fun 負の数量は作れない() {
        assertNull(Quantity.of(-1, 2))
        assertNull(Quantity.of(1, -2))
    }

    @Test
    fun 分母0は作れない() {
        assertNull(Quantity.of(1, 0))
    }

    @Test
    fun 積がLongの範囲を超えると足した結果は得られない() {
        val huge = quantityOrFail(1, Long.MAX_VALUE)

        assertNull(huge.plus(quantityOrFail(1, Long.MAX_VALUE - 1)))
    }

    @Test
    fun 和がLongの範囲を超えると足した結果は得られない() {
        assertNull(quantityOrFail(Long.MAX_VALUE).plus(quantityOrFail(Long.MAX_VALUE)))
    }
}
