package com.example.recipedeck.domain.item

import com.example.recipedeck.domain.generateIdValue
import kotlin.jvm.JvmInline

@JvmInline
value class ItemId(val value: String) {
    companion object {
        fun generate(): ItemId = ItemId(generateIdValue())
    }
}
