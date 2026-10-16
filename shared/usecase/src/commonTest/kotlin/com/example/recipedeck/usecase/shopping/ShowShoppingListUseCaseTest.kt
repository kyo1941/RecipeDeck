package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.shopping.AddedRecipeIngredient
import com.example.recipedeck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipedeck.domain.shopping.ShoppingSession
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipedeck.usecase.multiplierOrFail
import com.example.recipedeck.usecase.notBlankStringOrFail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ShowShoppingListUseCaseTest {
    private val shoppingSessionRepository = FakeShoppingSessionRepository()
    private val purchaseRecordRepository = FakePurchaseRecordRepository()
    private val showShoppingList = ShowShoppingListUseCase(shoppingSessionRepository, purchaseRecordRepository)

    private fun sessionWithMapoTofu() = ShoppingSession.create(Instant.parse("2026-10-12T10:00:00Z")).withAddedRecipe(
        sourceRecipeId = RecipeId("recipe-mapo-tofu"),
        recipeName = notBlankStringOrFail("麻婆豆腐"),
        multiplier = multiplierOrFail(1),
        ingredients = listOf(
            AddedRecipeIngredient(notBlankStringOrFail("長ネギ"), itemId = null, amount = null, isExcluded = false),
            AddedRecipeIngredient(notBlankStringOrFail("豆腐"), itemId = null, amount = null, isExcluded = false),
        ),
    )

    @Test
    fun 進行中のSessionの買い物リストを購入の記録を載せて返す() = runTest {
        val session = sessionWithMapoTofu()
        shoppingSessionRepository.savedSessions += session
        val purchased = setOf(session.recipes.single().ingredients.first().id)
        purchaseRecordRepository.purchasedSourceIds[session.id] = purchased

        val result = showShoppingList()

        assertEquals(ShowShoppingListResult.Shown(session.toShoppingList(purchased)), result)
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

    @Test
    fun 購入の記録の読み込みに失敗したら買い物リストを返さず原因を持った予期しない失敗を返す() = runTest {
        shoppingSessionRepository.savedSessions += sessionWithMapoTofu()
        val cause = IllegalStateException("保存先から読み込めない")
        purchaseRecordRepository.findFailure = PurchaseRecordRepositoryFailure.Unexpected(cause)

        assertEquals(ShowShoppingListResult.Unexpected(cause), showShoppingList())
    }
}
