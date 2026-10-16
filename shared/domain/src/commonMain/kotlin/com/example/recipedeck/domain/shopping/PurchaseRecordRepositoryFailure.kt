package com.example.recipedeck.domain.shopping

sealed interface PurchaseRecordRepositoryFailure {
    data class Unexpected(val cause: Throwable) : PurchaseRecordRepositoryFailure
}
