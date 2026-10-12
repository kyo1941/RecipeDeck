@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.amount.multiplierOrFail
import com.example.recipe_deck.domain.amount.quantityOrFail
import com.example.recipe_deck.domain.NotBlankString
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.notBlankStringOrFail
import com.example.recipe_deck.domain.recipe.RecipeId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ShoppingSessionTest {
    private val createdAt = Instant.parse("2026-10-04T10:00:00Z")
    private val mapoTofu = RecipeId("mapo-tofu")
    private val friedRice = RecipeId("fried-rice")
    private val greenOnionId = ItemId("green-onion")
    private val greenOnion = notBlankStringOrFail("長ネギ")
    private val doubanjiang = notBlankStringOrFail("豆板醤")
    private val onePiece = Amount(quantityOrFail(1), QuantityNotation.FRACTION, AmountUnit.PIECE)

    private fun ingredient(
        displayName: NotBlankString,
        itemId: ItemId? = null,
        amount: Amount? = onePiece,
        isExcluded: Boolean = false,
    ) = AddedRecipeIngredient(displayName, itemId, amount, isExcluded)

    private fun ShoppingSession.withAddedMapoTofu(vararg ingredients: AddedRecipeIngredient) =
        withAddedRecipe(mapoTofu, notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), ingredients.toList())

    @Test
    fun 作ったSessionは進行中で中身が空になる() {
        val session = ShoppingSession.create(createdAt)

        assertEquals(SessionState.Active, session.state)
        assertEquals(createdAt, session.createdAt)
        assertEquals(emptyList(), session.recipes)
        assertEquals(emptyList(), session.items)
    }

    @Test
    fun Recipeを足すと元のRecipeと名前と倍率と材料を写したSession内のRecipeができる() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId))

        val recipe = session.recipes.single()
        val item = session.items.single()
        assertEquals(mapoTofu, recipe.sourceRecipeId)
        assertEquals("麻婆豆腐", recipe.recipeName.value)
        assertEquals(multiplierOrFail(2), recipe.multiplier)
        assertEquals(greenOnion, item.displayName)
        assertEquals(greenOnionId, item.itemId)
        assertNull(item.manualAddition)
        assertEquals(listOf(SessionRecipeIngredient(item.id, onePiece, isExcluded = false)), recipe.ingredients)
    }

    @Test
    fun 同じItemの材料が複数あってもSessionItemは1つにまとめる() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId), ingredient(greenOnion, greenOnionId, amount = null))

        val item = session.items.single()
        assertEquals(listOf(item.id, item.id), session.recipes.single().ingredients.map { it.sessionItemId })
    }

    @Test
    fun 同じ表示名のSession限定の品目はSessionItemを1つにまとめる() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(doubanjiang), ingredient(doubanjiang))

        val item = session.items.single()
        assertEquals(doubanjiang, item.displayName)
        assertNull(item.itemId)
    }

    @Test
    fun 同じItemでも表示名が違えば別のSessionItemにする() {
        val session = ShoppingSession.create(createdAt)
            .withAddedMapoTofu(ingredient(greenOnion, greenOnionId))
            .withAddedMapoTofu(ingredient(notBlankStringOrFail("白ネギ"), greenOnionId))

        assertEquals(listOf("長ネギ", "白ネギ"), session.items.map { it.displayName.value })
        assertEquals(listOf(greenOnionId, greenOnionId), session.items.map { it.itemId })
    }

    @Test
    fun 同じ表示名でもItemとSession限定の品目は別のSessionItemにする() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId), ingredient(greenOnion))

        assertEquals(listOf(greenOnionId, null), session.items.map { it.itemId })
    }

    @Test
    fun 同じRecipeを2回足すと別々のSession内のRecipeになり同じ品目のSessionItemを使い回す() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId)).withAddedMapoTofu(ingredient(greenOnion, greenOnionId))

        val item = session.items.single()
        assertEquals(2, session.recipes.size)
        assertNotEquals(session.recipes[0].id, session.recipes[1].id)
        assertEquals(listOf(item.id, item.id), session.recipes.map { it.ingredients.single().sessionItemId })
    }

    @Test
    fun 除外した材料もSessionItemを指す行として残す() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId, isExcluded = true))

        val recipeIngredient = session.recipes.single().ingredients.single()
        assertEquals(session.items.single().id, recipeIngredient.sessionItemId)
        assertTrue(recipeIngredient.isExcluded)
    }

    @Test
    fun 材料のないRecipeを足してもSessionItemは増えない() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu()

        assertEquals(1, session.recipes.size)
        assertEquals(emptyList(), session.items)
    }

    // TODO: 手動追加のある Session を作る手段（手動追加の操作か、保存先からの読み戻し）がまだないため、手動追加が内訳の最後に並ぶことは確かめていない。
    //  手段ができたら確かめる。
    @Test
    fun 買い物リストはSessionItemごとに項目を作り全てのSession内のRecipeからその品目の材料を内訳として集める() {
        val halfPiece = Amount(quantityOrFail(1, 2), QuantityNotation.FRACTION, AmountUnit.PIECE)
        val session = ShoppingSession.create(createdAt)
            .withAddedMapoTofu(ingredient(greenOnion, greenOnionId), ingredient(doubanjiang, amount = null))
            .withAddedRecipe(friedRice, notBlankStringOrFail("炒飯"), multiplierOrFail(1), listOf(ingredient(greenOnion, greenOnionId, amount = halfPiece)))

        val shoppingList = session.toShoppingList()

        assertEquals(session.items.map { it.id }, shoppingList.map { it.sessionItemId })
        assertEquals(listOf("長ネギ", "豆板醤"), shoppingList.map { it.displayName.value })
        assertEquals(listOf(greenOnionId, null), shoppingList.map { it.itemId })
        assertEquals(
            listOf<ShoppingListItemSource>(
                ShoppingListItemSource.FromRecipe(notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), onePiece, isExcluded = false),
                ShoppingListItemSource.FromRecipe(notBlankStringOrFail("炒飯"), multiplierOrFail(1), halfPiece, isExcluded = false),
            ),
            shoppingList[0].sources,
        )
        assertEquals(
            listOf<ShoppingListItemSource>(ShoppingListItemSource.FromRecipe(notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), amount = null, isExcluded = false)),
            shoppingList[1].sources,
        )
    }

    // TODO: 手動追加のある Session を作る手段（手動追加の操作か、保存先からの読み戻し）がまだないため、Recipe 由来の内訳がなく手動追加だけの品目も買い物項目になることは確かめていない。
    //  手段ができたら確かめる。
    @Test
    fun 材料が全て除外された品目も除外した内訳を持つ項目として買い物リストに残す() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId, isExcluded = true))

        val sources = session.toShoppingList().single().sources

        assertEquals(
            listOf<ShoppingListItemSource>(ShoppingListItemSource.FromRecipe(notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), onePiece, isExcluded = true)),
            sources,
        )
    }

    @Test
    fun 品目のないSessionの買い物リストは空になる() {
        assertEquals(emptyList(), ShoppingSession.create(createdAt).toShoppingList())
    }
}
