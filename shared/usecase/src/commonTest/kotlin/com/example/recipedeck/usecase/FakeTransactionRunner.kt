package com.example.recipedeck.usecase

import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.TransactionScope

class FakeTransactionRunner : TransactionRunner {
    var rolledBack = false
        private set

    override suspend fun <T> run(block: suspend TransactionScope<T>.() -> T): T {
        val scope = FakeTransactionScope<T>()
        return try {
            scope.block()
        } catch (_: RollbackSignal) {
            rolledBack = true
            scope.rollbackResults.single()
        }
    }
}

private class RollbackSignal : Throwable()

private class FakeTransactionScope<T> : TransactionScope<T> {
    val rollbackResults = mutableListOf<T>()

    override suspend fun rollback(result: T): Nothing {
        rollbackResults += result
        throw RollbackSignal()
    }
}
