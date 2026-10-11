@file:OptIn(ExperimentalTime::class)

package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.amount.multiplierOrFail
import com.example.recipe_deck.domain.amount.quantityOrFail
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.item.ItemReference
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
    private val greenOnion = ItemReference.Registered(ItemId("green-onion"))
    private val doubanjiang = ItemReference.Unregistered(notBlankStringOrFail("豆板醤"))
    private val onePiece = Amount(quantityOrFail(1), QuantityNotation.FRACTION, AmountUnit.PIECE)

    private fun ingredient(
        item: ItemReference,
        amount: Amount? = onePiece,
        isExcluded: Boolean = false,
    ) = AddedRecipeIngredient(item, amount, isExcluded)

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
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion))

        val recipe = session.recipes.single()
        val item = session.items.single()
        assertEquals(mapoTofu, recipe.sourceRecipeId)
        assertEquals("麻婆豆腐", recipe.recipeName.value)
        assertEquals(multiplierOrFail(2), recipe.multiplier)
        assertEquals(greenOnion, item.item)
        assertNull(item.manualAddition)
        assertEquals(listOf(SessionRecipeIngredient(item.id, onePiece, isExcluded = false)), recipe.ingredients)
    }

    @Test
    fun 同じItemの材料が複数あってもSessionItemは1つにまとめる() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion), ingredient(greenOnion, amount = null))

        val item = session.items.single()
        assertEquals(listOf(item.id, item.id), session.recipes.single().ingredients.map { it.sessionItemId })
    }

    @Test
    fun 同じ表示名のSession限定の品目はSessionItemを1つにまとめる() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(doubanjiang), ingredient(doubanjiang))

        assertEquals(doubanjiang, session.items.single().item)
    }

    @Test
    fun 同じRecipeを2回足すと別々のSession内のRecipeになり同じ品目のSessionItemを使い回す() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion)).withAddedMapoTofu(ingredient(greenOnion))

        val item = session.items.single()
        assertEquals(2, session.recipes.size)
        assertNotEquals(session.recipes[0].id, session.recipes[1].id)
        assertEquals(listOf(item.id, item.id), session.recipes.map { it.ingredients.single().sessionItemId })
    }

    @Test
    fun 除外した材料もSessionItemを指す行として残す() {
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu(ingredient(greenOnion, isExcluded = true))

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
}
