package com.example.recipe_deck.usecase

import com.example.recipe_deck.domain.NotBlankString
import kotlin.test.assertNotNull

fun notBlankStringOrFail(raw: String): NotBlankString = assertNotNull(NotBlankString.of(raw))
