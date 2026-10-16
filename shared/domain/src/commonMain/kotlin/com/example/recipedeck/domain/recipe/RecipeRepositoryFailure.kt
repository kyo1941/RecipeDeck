package com.example.recipedeck.domain.recipe

sealed interface RecipeRepositoryFailure {
    data class Unexpected(val cause: Throwable) : RecipeRepositoryFailure
}
