package com.example.recipedeck.data.recipe

import com.example.recipedeck.data.notBlankStringOrFail
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeIngredient
import com.example.recipedeck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.assertNotNull

val onion = Item(ItemId("item-onion"), notBlankStringOrFail("玉ねぎ"))
val pork = Item(ItemId("item-pork"), notBlankStringOrFail("豚肉"))
val salt = Item(ItemId("item-salt"), notBlankStringOrFail("塩"))

fun amountOrFail(numerator: Long, denominator: Long, notation: QuantityNotation, unit: AmountUnit?): Amount =
    Amount(assertNotNull(Quantity.of(numerator, denominator)), notation, unit)

fun recipe(
    id: String,
    name: String = "生姜焼き",
    status: RecipeStatus = RecipeStatus.READY,
    photoRef: String? = null,
    memo: String? = null,
    ingredients: List<RecipeIngredient> = emptyList(),
) = Recipe(
    id = RecipeId(id),
    name = notBlankStringOrFail(name),
    status = status,
    photoRef = photoRef,
    memo = memo?.let(::notBlankStringOrFail),
    ingredients = ingredients.toImmutableList(),
)
