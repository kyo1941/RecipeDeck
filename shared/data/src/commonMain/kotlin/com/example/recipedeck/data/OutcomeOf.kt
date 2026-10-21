package com.example.recipedeck.data

import com.example.recipedeck.domain.Outcome
import kotlin.coroutines.cancellation.CancellationException

internal suspend fun <T, E> outcomeOf(
    toFailure: (Throwable) -> E,
    block: suspend () -> T,
): Outcome<T, E> =
    try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Outcome.Failure(toFailure(e))
    }
