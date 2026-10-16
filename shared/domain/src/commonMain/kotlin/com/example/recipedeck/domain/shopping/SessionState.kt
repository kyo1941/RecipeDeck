package com.example.recipedeck.domain.shopping

import kotlin.time.Instant

sealed interface SessionState {
    data object Active : SessionState

    data class Completed(val completedAt: Instant) : SessionState
}
