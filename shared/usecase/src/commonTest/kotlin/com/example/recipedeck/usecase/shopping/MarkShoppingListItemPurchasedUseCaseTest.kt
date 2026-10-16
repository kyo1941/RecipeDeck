package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.shopping.AddedRecipeIngredient
import com.example.recipedeck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipedeck.domain.shopping.SessionItemId
import com.example.recipedeck.domain.shopping.ShoppingSession
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import com.example.recipedeck.usecase.multiplierOrFail
import com.example.recipedeck.usecase.notBlankStringOrFail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MarkShoppingListItemPurchasedUseCaseTest {
    private val shoppingSessionRepository = FakeShoppingSessionRepository()
    private val purchaseRecordRepository = FakePurchaseRecordRepository()
    private val markShoppingListItemPurchased = MarkShoppingListItemPurchasedUseCase(shoppingSessionRepository, purchaseRecordRepository)
    private val greenOnion = notBlankStringOrFail("長ネギ")
    private val tofu = notBlankStringOrFail("豆腐")

    private fun ingredient(displayName: NotBlankString, isExcluded: Boolean = false) =
        AddedRecipeIngredient(displayName, itemId = null, amount = null, isExcluded = isExcluded)

    private fun ShoppingSession.withAddedRecipe(recipeName: String, vararg ingredients: AddedRecipeIngredient) =
        withAddedRecipe(RecipeId("recipe-$recipeName"), notBlankStringOrFail(recipeName), multiplierOrFail(1), ingredients.toList())

    private fun savedSession(build: ShoppingSession.() -> ShoppingSession): ShoppingSession =
        ShoppingSession.create(Instant.parse("2026-10-14T10:00:00Z")).build().also { shoppingSessionRepository.savedSessions += it }

    private fun ShoppingSession.itemIdOf(displayName: NotBlankString): SessionItemId = items.single { it.displayName == displayName }.id

    @Test
    fun 購入済みにすると品目の除外していない内訳をすべて購入済みとして記録する() = runTest {
        val session = savedSession {
            withAddedRecipe("麻婆豆腐", ingredient(greenOnion)).withAddedRecipe("炒飯", ingredient(greenOnion), ingredient(greenOnion, isExcluded = true))
        }
        val (mapoTofuGreenOnion, friedRiceGreenOnion) = session.recipes.flatMap { it.ingredients }.filterNot { it.isExcluded }.map { it.id }

        val result = markShoppingListItemPurchased(session.itemIdOf(greenOnion), isPurchased = true)

        assertEquals(MarkShoppingListItemPurchasedResult.Marked, result)
        assertEquals(setOf(mapoTofuGreenOnion, friedRiceGreenOnion), purchaseRecordRepository.purchasedSourceIds[session.id])
    }

    @Test
    fun 未購入にすると品目の内訳の購入済みの記録を外しほかの品目の記録は残す() = runTest {
        val session = savedSession { withAddedRecipe("麻婆豆腐", ingredient(greenOnion), ingredient(tofu)) }
        val (greenOnionSource, tofuSource) = session.recipes.single().ingredients.map { it.id }
        purchaseRecordRepository.purchasedSourceIds[session.id] = setOf(greenOnionSource, tofuSource)

        val result = markShoppingListItemPurchased(session.itemIdOf(greenOnion), isPurchased = false)

        assertEquals(MarkShoppingListItemPurchasedResult.Marked, result)
        assertEquals(setOf(tofuSource), purchaseRecordRepository.purchasedSourceIds[session.id])
    }

    @Test
    fun 進行中のSessionが無ければ何も記録せずにそのことを返す() = runTest {
        val result = markShoppingListItemPurchased(SessionItemId("green-onion"), isPurchased = true)

        assertEquals(MarkShoppingListItemPurchasedResult.NoActiveSession, result)
        assertEquals(emptyMap(), purchaseRecordRepository.purchasedSourceIds)
    }

    @Test
    fun 進行中のSessionに品目が無ければ何も記録せずにそのことを返す() = runTest {
        savedSession { withAddedRecipe("麻婆豆腐", ingredient(greenOnion)) }

        val result = markShoppingListItemPurchased(SessionItemId("not-in-session"), isPurchased = true)

        assertEquals(MarkShoppingListItemPurchasedResult.ShoppingListItemNoLongerExists, result)
        assertEquals(emptyMap(), purchaseRecordRepository.purchasedSourceIds)
    }

    @Test
    fun 品目の内訳がすべて除外されていれば何も記録せずにそのことを返す() = runTest {
        val session = savedSession { withAddedRecipe("麻婆豆腐", ingredient(greenOnion, isExcluded = true)) }

        val result = markShoppingListItemPurchased(session.itemIdOf(greenOnion), isPurchased = false)

        assertEquals(MarkShoppingListItemPurchasedResult.AllSourcesExcluded, result)
        assertEquals(emptyMap(), purchaseRecordRepository.purchasedSourceIds)
    }

    @Test
    fun 進行中のSessionの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        shoppingSessionRepository.findFailure = ShoppingSessionRepositoryFailure.Unexpected(cause)

        assertEquals(MarkShoppingListItemPurchasedResult.Unexpected(cause), markShoppingListItemPurchased(SessionItemId("green-onion"), isPurchased = true))
    }

    @Test
    fun 購入の記録の読み込みに失敗したら何も記録せずに原因を持った予期しない失敗を返す() = runTest {
        val session = savedSession { withAddedRecipe("麻婆豆腐", ingredient(greenOnion)) }
        val cause = IllegalStateException("保存先から読み込めない")
        purchaseRecordRepository.findFailure = PurchaseRecordRepositoryFailure.Unexpected(cause)

        val result = markShoppingListItemPurchased(session.itemIdOf(greenOnion), isPurchased = true)

        assertEquals(MarkShoppingListItemPurchasedResult.Unexpected(cause), result)
        assertEquals(emptyMap(), purchaseRecordRepository.purchasedSourceIds)
    }

    @Test
    fun 購入の記録の書き込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val session = savedSession { withAddedRecipe("麻婆豆腐", ingredient(greenOnion)) }
        val cause = IllegalStateException("保存先に書き込めない")
        purchaseRecordRepository.recordFailure = PurchaseRecordRepositoryFailure.Unexpected(cause)

        assertEquals(MarkShoppingListItemPurchasedResult.Unexpected(cause), markShoppingListItemPurchased(session.itemIdOf(greenOnion), isPurchased = true))
    }
}
