@file:OptIn(ExperimentalTime::class)

package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.amount.multiplierOrFail
import com.example.recipedeck.domain.amount.quantityOrFail
import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.notBlankStringOrFail
import com.example.recipedeck.domain.recipe.RecipeId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val recipeIngredient = recipe.ingredients.single()
        assertEquals(item.id, recipeIngredient.sessionItemId)
        assertEquals(onePiece, recipeIngredient.amount)
        assertFalse(recipeIngredient.isExcluded)
    }

    @Test
    fun 同じ品目の材料でもSession内の材料はそれぞれ別の内訳のIDを持つ() {
        val session = ShoppingSession.create(createdAt)
            .withAddedMapoTofu(ingredient(greenOnion, greenOnionId), ingredient(greenOnion, greenOnionId))
            .withAddedMapoTofu(ingredient(greenOnion, greenOnionId))

        val ids = session.recipes.flatMap { recipe -> recipe.ingredients.map { it.id } }
        assertEquals(3, ids.toSet().size)
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

        val (mapoTofuGreenOnion, mapoTofuDoubanjiang) = session.recipes[0].ingredients.map { it.id }
        val friedRiceGreenOnion = session.recipes[1].ingredients.single().id

        val shoppingList = session.toShoppingList(purchasedSourceIds = emptySet())

        assertEquals(session.items.map { it.id }, shoppingList.map { it.sessionItemId })
        assertEquals(listOf("長ネギ", "豆板醤"), shoppingList.map { it.displayName.value })
        assertEquals(listOf(greenOnionId, null), shoppingList.map { it.itemId })
        assertEquals(
            listOf<ShoppingListItemSource>(
                ShoppingListItemSource.FromRecipe(mapoTofuGreenOnion, notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), onePiece, isExcluded = false, isPurchased = false),
                ShoppingListItemSource.FromRecipe(friedRiceGreenOnion, notBlankStringOrFail("炒飯"), multiplierOrFail(1), halfPiece, isExcluded = false, isPurchased = false),
            ),
            shoppingList[0].sources,
        )
        assertEquals(
            listOf<ShoppingListItemSource>(ShoppingListItemSource.FromRecipe(mapoTofuDoubanjiang, notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), amount = null, isExcluded = false, isPurchased = false)),
            shoppingList[1].sources,
        )
    }

    // TODO: 手動追加のある Session を作る手段（手動追加の操作か、保存先からの読み戻し）がまだないため、Recipe 由来の内訳がなく手動追加だけの品目も買い物項目になることは確かめていない。
    //  手段ができたら確かめる。
    @Test
    fun 材料が全て除外された品目も除外した内訳を持つ項目として買い物リストに残す() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId, isExcluded = true))

        val excludedGreenOnion = session.recipes.single().ingredients.single().id

        val sources = session.toShoppingList(purchasedSourceIds = emptySet()).single().sources

        assertEquals(
            listOf<ShoppingListItemSource>(ShoppingListItemSource.FromRecipe(excludedGreenOnion, notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(2), onePiece, isExcluded = true, isPurchased = false)),
            sources,
        )
    }

    @Test
    fun 買い物リストの内訳は購入済みの内訳のIDに含まれるものだけが購入済みになる() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, greenOnionId), ingredient(doubanjiang))
        val (purchasedGreenOnion, _) = session.recipes.single().ingredients.map { it.id }

        val shoppingList = session.toShoppingList(purchasedSourceIds = setOf(purchasedGreenOnion))

        assertEquals(listOf(true, false), shoppingList.map { it.sources.single().isPurchased })
    }

    @Test
    fun 品目のないSessionの買い物リストは空になる() {
        assertEquals(emptyList(), ShoppingSession.create(createdAt).toShoppingList(purchasedSourceIds = emptySet()))
    }
}
