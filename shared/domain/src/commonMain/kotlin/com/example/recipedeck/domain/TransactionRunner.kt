package com.example.recipedeck.domain

interface TransactionRunner {
    suspend fun <T> run(block: suspend TransactionScope<T>.() -> T): Outcome<T, TransactionRunnerFailure>
}

interface TransactionScope<T> {
    suspend fun rollback(result: T): Nothing
}
