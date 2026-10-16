package com.example.recipedeck.domain.shopping

sealed interface ShoppingSessionRepositoryFailure {
    data class Unexpected(val cause: Throwable) : ShoppingSessionRepositoryFailure
}
