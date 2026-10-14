package com.example.recipe_deck.domain.shopping

import com.example.recipe_deck.domain.amount.multiplierOrFail
import com.example.recipe_deck.domain.notBlankStringOrFail
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShoppingListItemTest {
    private val mapoTofu = ShoppingListItemSourceId("mapo-tofu-green-onion")
    private val friedRice = ShoppingListItemSourceId("fried-rice-green-onion")
    private val manual = ShoppingListItemSourceId("manual-green-onion")

    private fun fromRecipe(id: ShoppingListItemSourceId, isPurchased: Boolean, isExcluded: Boolean = false) =
        ShoppingListItemSource.FromRecipe(id, notBlankStringOrFail("麻婆豆腐"), multiplierOrFail(1), amount = null, isExcluded = isExcluded, isPurchased = isPurchased)

    private fun greenOnion(vararg sources: ShoppingListItemSource) =
        ShoppingListItem(SessionItemId("green-onion"), notBlankStringOrFail("長ネギ"), itemId = null, sources.toList().toImmutableList())

    @Test
    fun 除外していない内訳がすべて購入済みなら買い物項目は購入済みになる() {
        val item = greenOnion(fromRecipe(mapoTofu, isPurchased = true), ShoppingListItemSource.Manual(manual, amount = null, isPurchased = true))

        assertTrue(item.isPurchased)
    }

    @Test
    fun 未購入の内訳が1つでもあれば買い物項目は未購入になる() {
        val item = greenOnion(fromRecipe(mapoTofu, isPurchased = true), ShoppingListItemSource.Manual(manual, amount = null, isPurchased = false))

        assertFalse(item.isPurchased)
    }

    @Test
    fun 除外した内訳は未購入でも買い物項目の購入状態に数えない() {
        val item = greenOnion(fromRecipe(mapoTofu, isPurchased = true), fromRecipe(friedRice, isPurchased = false, isExcluded = true))

        assertTrue(item.isPurchased)
    }

    @Test
    fun 内訳がすべて除外された買い物項目は購入済みと同じになる() {
        val item = greenOnion(fromRecipe(mapoTofu, isPurchased = false, isExcluded = true))

        assertTrue(item.isPurchased)
    }

    @Test
    fun 買う内訳のIDは除外した内訳を除いて手動追加を含む() {
        val item = greenOnion(
            fromRecipe(mapoTofu, isPurchased = false),
            fromRecipe(friedRice, isPurchased = false, isExcluded = true),
            ShoppingListItemSource.Manual(manual, amount = null, isPurchased = false),
        )

        assertEquals(listOf(mapoTofu, manual), item.sourceIdsToBuy)
    }
}
