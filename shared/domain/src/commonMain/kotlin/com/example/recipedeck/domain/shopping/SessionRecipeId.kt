package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class SessionRecipeId(val value: String) {
    companion object {
        fun generate(): SessionRecipeId = SessionRecipeId(generateIdValue())
    }
}
