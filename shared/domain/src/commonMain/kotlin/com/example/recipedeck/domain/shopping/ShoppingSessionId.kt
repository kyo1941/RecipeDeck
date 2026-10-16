package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class ShoppingSessionId(val value: String) {
    companion object {
        fun generate(): ShoppingSessionId = ShoppingSessionId(generateIdValue())
    }
}
