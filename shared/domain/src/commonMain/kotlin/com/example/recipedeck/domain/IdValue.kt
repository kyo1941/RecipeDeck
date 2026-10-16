package com.example.recipedeck.domain

import kotlin.uuid.Uuid

internal fun generateIdValue(): String = Uuid.random().toString()
