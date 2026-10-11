package com.example.recipe_deck.domain.recipe

import com.example.recipe_deck.domain.amount.Amount
import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.amount.quantityOrFail
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.domain.notBlankStringOrFail
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecipeTest {
    private val greenOnion = ItemId("green-onion")

    private fun recipe(
        ingredients: ImmutableList<RecipeIngredient> = persistentListOf(),
    ) = Recipe(
        id = RecipeId("mapo-tofu"),
        name = notBlankStringOrFail("麻婆豆腐"),
        status = RecipeStatus.DRAFT,
        photoRef = null,
        memo = null,
        ingredients = ingredients,
    )

    @Test
    fun 名前だけで材料0件のRecipeを作れる() {
        assertEquals(emptyList(), recipe().ingredients)
    }

    @Test
    fun 同じItemを複数の材料行に持てる() {
        val ingredients = persistentListOf(
            RecipeIngredient(RecipeIngredientId("row-1"), greenOnion, Amount(quantityOrFail(1, 2), QuantityNotation.FRACTION, AmountUnit.PIECE)),
            RecipeIngredient(RecipeIngredientId("row-2"), greenOnion, null),
        )

        assertEquals(ingredients, recipe(ingredients = ingredients).ingredients)
    }

    @Test
    fun 同じ材料行IDを重複して持てない() {
        val ingredients = persistentListOf(
            RecipeIngredient(RecipeIngredientId("row-1"), greenOnion, null),
            RecipeIngredient(RecipeIngredientId("row-1"), ItemId("tofu"), null),
        )

        assertFailsWith<IllegalArgumentException> { recipe(ingredients = ingredients) }
    }
}
