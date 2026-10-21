package com.example.recipedeck.data

import androidx.room3.RoomDatabase
import androidx.room3.withWriteTransaction
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.TransactionRunnerFailure
import com.example.recipedeck.domain.TransactionScope

class RoomTransactionRunner(private val database: RoomDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend TransactionScope<T>.() -> T): Outcome<T, TransactionRunnerFailure> =
        outcomeOf(TransactionRunnerFailure::Unexpected) {
            database.withWriteTransaction {
                val roomScope = this
                val scope = object : TransactionScope<T> {
                    override suspend fun rollback(result: T): Nothing = roomScope.rollback(result)
                }
                scope.block()
            }
        }
}
