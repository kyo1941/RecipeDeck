package com.example.recipedeck.data

import com.example.recipedeck.domain.NotBlankString
import kotlin.enums.EnumEntries

internal fun storedNotBlankStringOf(value: String, description: () -> String): NotBlankString =
    NotBlankString.of(value)?.takeIf { it.value == value }
        ?: throw InvalidStoredRecordException("${description()}が空白だけか、前後に空白がある: \"$value\"")

internal fun <E : Enum<E>> storedEnumOf(
    value: String,
    entries: EnumEntries<E>,
    storedValueOf: (E) -> String,
    description: () -> String,
): E = entries.firstOrNull { storedValueOf(it) == value }
    ?: throw InvalidStoredRecordException("${description()}が知らない値になっている: \"$value\"")
