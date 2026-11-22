package com.example.recipedeck.data

import com.example.recipedeck.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

/**
 * 読み出しの失敗で監視が閉じると、その後の変更が届かなくなるので、失敗は読むたびに値にして流し、監視は続ける。
 * 変更の合図そのものの失敗（データベースを開けないなど）は続けようがないので、失敗を流して終わる。
 */
internal fun <T, E> RecipeDeckDatabase.observeOutcomeOf(
    vararg tables: String,
    toFailure: (Throwable) -> E,
    read: suspend () -> T,
): Flow<Outcome<T, E>> =
    flow { emitAll(invalidationTracker.createFlow(*tables)) }
        .map { outcomeOf(toFailure, read) }
        .catch { e ->
            if (e is CancellationException || e !is Exception) throw e
            emit(Outcome.Failure(toFailure(e)))
        }
