package com.example.recipe_deck.domain

import kotlin.test.assertNotNull

fun notBlankStringOrFail(raw: String): NotBlankString = assertNotNull(NotBlankString.of(raw))
