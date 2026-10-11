package com.example.recipe_deck.domain.amount

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MultiplierTest {
    @Test
    fun 倍率0は作れない() {
        assertNull(Multiplier.of(0))
    }

    @Test
    fun 負の倍率は作れない() {
        assertNull(Multiplier.of(-1, 2))
        assertNull(Multiplier.of(1, -2))
    }

    @Test
    fun 分数の倍率を数量に誤差なく掛けられる() {
        assertEquals(quantityOrFail(1), quantityOrFail(3).times(multiplierOrFail(1, 3)))
    }

    @Test
    fun 倍率も分母を省略すると整数になる() {
        assertEquals(Multiplier.of(2, 1), Multiplier.of(2))
    }

    @Test
    fun 倍率も約分された値として扱われる() {
        val multiplier = assertNotNull(Multiplier.of(2, 4))

        assertEquals(1, multiplier.numerator)
        assertEquals(2, multiplier.denominator)
        assertEquals(Multiplier.of(1, 2), multiplier)
    }

    @Test
    fun 倍率も分母0は作れない() {
        assertNull(Multiplier.of(1, 0))
    }

    @Test
    fun 倍率を掛けた積がLongの範囲を超えると掛けた結果は得られない() {
        assertNull(quantityOrFail(1, Long.MAX_VALUE).times(multiplierOrFail(1, 2)))
    }
}
