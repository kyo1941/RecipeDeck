package com.example.recipedeck.domain

sealed interface TransactionRunnerFailure {
    data class Unexpected(val cause: Throwable) : TransactionRunnerFailure
}
