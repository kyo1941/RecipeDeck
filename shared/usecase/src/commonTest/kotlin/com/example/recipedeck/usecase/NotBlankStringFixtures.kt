package com.example.recipedeck.usecase

import com.example.recipedeck.domain.NotBlankString
import kotlin.test.assertNotNull

fun notBlankStringOrFail(raw: String): NotBlankString = assertNotNull(NotBlankString.of(raw))
