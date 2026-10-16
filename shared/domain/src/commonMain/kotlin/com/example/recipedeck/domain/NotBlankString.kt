package com.example.recipedeck.domain

import kotlin.jvm.JvmInline

@JvmInline
value class NotBlankString private constructor(val value: String) {
    companion object {
        fun of(raw: String): NotBlankString? = raw.trim().takeIf { it.isNotEmpty() }?.let(::NotBlankString)
    }
}
