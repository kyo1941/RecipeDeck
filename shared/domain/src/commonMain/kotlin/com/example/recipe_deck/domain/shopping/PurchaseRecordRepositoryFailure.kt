package com.example.recipe_deck.domain.shopping

sealed interface PurchaseRecordRepositoryFailure {
    data class Unexpected(val cause: Throwable) : PurchaseRecordRepositoryFailure
}
