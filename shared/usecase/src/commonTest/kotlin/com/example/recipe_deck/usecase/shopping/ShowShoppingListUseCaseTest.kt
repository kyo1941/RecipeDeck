@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.usecase.shopping

import com.example.recipe_deck.domain.recipe.RecipeId
import com.example.recipe_deck.domain.shopping.AddedRecipeIngredient
import com.example.recipe_deck.domain.shopping.ShoppingSession
import com.example.recipe_deck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipe_deck.usecase.multiplierOrFail
import com.example.recipe_deck.usecase.notBlankStringOrFail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ShowShoppingListUseCaseTest {
    private val shoppingSessionRepository = FakeShoppingSessionRepository()
    private val showShoppingList = ShowShoppingListUseCase(shoppingSessionRepository)

    @Test
    fun 進行中のSessionの買い物リストを返す() = runTest {
        val session = ShoppingSession.create(Instant.parse("2026-10-12T10:00:00Z")).withAddedRecipe(
            sourceRecipeId = RecipeId("recipe-mapo-tofu"),
            recipeName = notBlankStringOrFail("麻婆豆腐"),
            multiplier = multiplierOrFail(1),
            ingredients = listOf(AddedRecipeIngredient(notBlankStringOrFail("長ネギ"), itemId = null, amount = null, isExcluded = false)),
        )
        shoppingSessionRepository.savedSessions += session

        val result = showShoppingList()

        assertEquals(ShowShoppingListResult.Shown(session.toShoppingList()), result)
    }

    @Test
    fun 進行中のSessionが無ければそのことを返す() = runTest {
        assertEquals(ShowShoppingListResult.NoActiveSession, showShoppingList())
    }

    @Test
    fun 進行中のSessionの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        shoppingSessionRepository.findFailure = ShoppingSessionRepositoryFailure.Unexpected(cause)

        assertEquals(ShowShoppingListResult.Unexpected(cause), showShoppingList())
    }
}
