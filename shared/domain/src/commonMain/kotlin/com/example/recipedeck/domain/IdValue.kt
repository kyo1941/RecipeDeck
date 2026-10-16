@file:OptIn(ExperimentalUuidApi::class)

package com.example.recipedeck.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun generateIdValue(): String = Uuid.random().toString()
