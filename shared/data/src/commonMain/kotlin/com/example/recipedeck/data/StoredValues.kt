package com.example.recipedeck.data

import com.example.recipedeck.domain.NotBlankString

internal fun storedNotBlankStringOf(value: String, description: () -> String): NotBlankString =
    NotBlankString.of(value)?.takeIf { it.value == value }
        ?: throw InvalidStoredRecordException("${description()}が空白だけか、前後に空白がある: \"$value\"")
