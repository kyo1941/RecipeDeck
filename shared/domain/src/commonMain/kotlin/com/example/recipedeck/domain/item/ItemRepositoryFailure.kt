package com.example.recipedeck.domain.item

sealed interface ItemRepositoryFailure {
    data class Unexpected(val cause: Throwable) : ItemRepositoryFailure
}
