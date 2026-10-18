package com.example.recipedeck.data

import androidx.room3.RoomDatabase
import androidx.room3.withWriteTransaction
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.TransactionRunnerFailure
import com.example.recipedeck.domain.TransactionScope
import kotlin.coroutines.cancellation.CancellationException

class RoomTransactionRunner(private val database: RoomDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend TransactionScope<T>.() -> T): Outcome<T, TransactionRunnerFailure> =
        try {
            val result = database.withWriteTransaction {
                val roomScope = this
                val scope = object : TransactionScope<T> {
                    override suspend fun rollback(result: T): Nothing = roomScope.rollback(result)
                }
                scope.block()
            }
            Outcome.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Outcome.Failure(TransactionRunnerFailure.Unexpected(e))
        }
}
