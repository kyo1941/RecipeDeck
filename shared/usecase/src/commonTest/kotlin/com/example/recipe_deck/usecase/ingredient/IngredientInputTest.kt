package com.example.recipe_deck.usecase.ingredient

import com.example.recipe_deck.domain.amount.AmountUnit
import com.example.recipe_deck.domain.amount.Quantity
import com.example.recipe_deck.domain.amount.QuantityNotation
import com.example.recipe_deck.domain.item.ItemId
import com.example.recipe_deck.usecase.quantityOrFail
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IngredientInputTest {
    private fun ingredient(
        item: ItemInput,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
    ) = IngredientInput(item, quantity, QuantityNotation.FRACTION, unit)

    @Test
    fun 表示名がなく数量があれば不正な材料になる() {
        assertTrue(ingredient(ItemInput.Typed(" "), quantityOrFail(1), AmountUnit.PIECE).isInvalid)
    }

    @Test
    fun 表示名も分量もなければ不正な材料にならない() {
        assertFalse(ingredient(ItemInput.Typed(" 　")).isInvalid)
    }

    @Test
    fun 表示名がなく単位だけなら不正な材料にならない() {
        assertFalse(ingredient(ItemInput.Typed(""), unit = AmountUnit.GRAM).isInvalid)
    }

    @Test
    fun 表示名があれば分量がなくても不正な材料にならない() {
        assertFalse(ingredient(ItemInput.Typed("長ネギ")).isInvalid)
    }

    @Test
    fun 既存のItemを選んでいれば分量がなくても不正な材料にならない() {
        assertFalse(ingredient(ItemInput.Selected(ItemId("item-1"))).isInvalid)
    }
}
