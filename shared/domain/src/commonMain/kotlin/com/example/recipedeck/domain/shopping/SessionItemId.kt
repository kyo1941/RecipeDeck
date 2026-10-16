package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class SessionItemId(val value: String) {
    companion object {
        fun generate(): SessionItemId = SessionItemId(generateIdValue())
    }
}
