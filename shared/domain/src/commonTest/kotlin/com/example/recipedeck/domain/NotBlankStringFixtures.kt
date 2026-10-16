package com.example.recipedeck.domain

import kotlin.test.assertNotNull

fun notBlankStringOrFail(raw: String): NotBlankString = assertNotNull(NotBlankString.of(raw))
