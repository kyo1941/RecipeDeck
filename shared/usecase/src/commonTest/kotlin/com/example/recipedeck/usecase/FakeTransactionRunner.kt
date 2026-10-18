package com.example.recipedeck.usecase

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.TransactionRunnerFailure
import com.example.recipedeck.domain.TransactionScope

class FakeTransactionRunner : TransactionRunner {
    var rolledBack = false
        private set
    var runFailure: TransactionRunnerFailure? = null

    override suspend fun <T> run(block: suspend TransactionScope<T>.() -> T): Outcome<T, TransactionRunnerFailure> {
        runFailure?.let { return Outcome.Failure(it) }
        val scope = FakeTransactionScope<T>()
        return try {
            Outcome.Success(scope.block())
        } catch (_: RollbackSignal) {
            rolledBack = true
            Outcome.Success(scope.rollbackResults.single())
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
