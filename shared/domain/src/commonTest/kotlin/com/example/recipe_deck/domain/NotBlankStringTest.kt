package com.example.recipe_deck.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotBlankStringTest {
    @Test
    fun 前後の空白を除いた文字列になる() {
        assertEquals("麻婆豆腐", NotBlankString.of("　麻婆豆腐 \n")?.value)
    }

    @Test
    fun 空白だけの文字列からは作れない() {
        assertNull(NotBlankString.of(" 　\n"))
    }

    @Test
    fun 空の文字列からは作れない() {
        assertNull(NotBlankString.of(""))
    }

    @Test
    fun 前後の空白だけが違う文字列は同じになる() {
        assertEquals(NotBlankString.of("麻婆豆腐"), NotBlankString.of(" 麻婆豆腐"))
    }
}
