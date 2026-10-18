package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.TransactionRunnerFailure
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.FakeTransactionRunner
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.ingredient.ItemInput
import com.example.recipedeck.usecase.item.FakeItemRepository
import com.example.recipedeck.usecase.notBlankStringOrFail
import com.example.recipedeck.usecase.quantityOrFail
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val itemRepository = FakeItemRepository()
    private val transactionRunner = FakeTransactionRunner()
    private val createRecipe = CreateRecipeUseCase(recipeRepository, itemRepository, transactionRunner)

    private val groundMeat = Item(ItemId("item-1"), notBlankStringOrFail("ひき肉"))
    private val tofu = Item(ItemId("item-2"), notBlankStringOrFail("豆腐"))

    private fun ingredient(
        item: ItemInput,
        quantity: Quantity? = null,
        unit: AmountUnit? = null,
    ) = IngredientInput(item, quantity, QuantityNotation.FRACTION, unit)

    @Test
    fun 指定された名前と状態でRecipeを保存する() = runTest {
        val result = createRecipe(name = "麻婆豆腐", status = RecipeStatus.READY, ingredients = emptyList())

        val saved = recipeRepository.savedRecipes.single()
        assertEquals(CreateRecipeResult.Created(saved.id, persistentListOf()), result)
        assertEquals("麻婆豆腐", saved.name.value)
        assertEquals(RecipeStatus.READY, saved.status)
        assertEquals(emptyList(), saved.ingredients)
    }

    @Test
    fun 名前の前後の空白を除いて保存する() = runTest {
        createRecipe(name = "　麻婆豆腐 \n", status = RecipeStatus.DRAFT, ingredients = emptyList())

        assertEquals("麻婆豆腐", recipeRepository.savedRecipes.single().name.value)
    }

    @Test
    fun 名前が空白だけなら保存せずに失敗を返す() = runTest {
        val result = createRecipe(name = " 　\n", status = RecipeStatus.DRAFT, ingredients = emptyList())

        assertEquals(CreateRecipeResult.NameRequired, result)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun 材料を持ったRecipeと新しいItemを保存して既存に寄せたItemを返す() = runTest {
        itemRepository.savedItems += listOf(groundMeat, tofu)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(ItemInput.Selected(tofu.id), quantityOrFail(1), AmountUnit.PIECE),
                ingredient(ItemInput.Typed("長ネギ")),
                ingredient(ItemInput.Typed(" ひき肉")),
            ),
        )

        val saved = recipeRepository.savedRecipes.single()
        val created = itemRepository.savedItems.single { it.displayName.value == "長ネギ" }
        assertEquals(CreateRecipeResult.Created(saved.id, persistentListOf(groundMeat)), result)
        assertEquals(listOf(tofu.id, created.id, groundMeat.id), saved.ingredients.map { it.itemId })
        assertEquals(
            listOf(Amount(quantityOrFail(1), QuantityNotation.FRACTION, AmountUnit.PIECE), null, null),
            saved.ingredients.map { it.amount },
        )
    }

    @Test
    fun 不正な材料の位置を返して何も保存しない() = runTest {
        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(
                ingredient(ItemInput.Typed(" 　"), quantityOrFail(1), AmountUnit.PIECE),
                ingredient(ItemInput.Typed("長ネギ")),
                ingredient(ItemInput.Typed(""), quantityOrFail(200), AmountUnit.GRAM),
            ),
        )

        assertEquals(CreateRecipeResult.IngredientDisplayNameRequired(persistentListOf(0, 2)), result)
        assertEquals(emptyList(), itemRepository.savedItems)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Itemの読み込みに失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先を読めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemInput.Typed("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Itemの保存に失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        itemRepository.saveFailure = ItemRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemInput.Typed("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
        assertEquals(emptyList(), recipeRepository.savedRecipes)
    }

    @Test
    fun Recipeの保存に失敗したら取り消して原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先に書き込めない")
        recipeRepository.saveFailure = RecipeRepositoryFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemInput.Typed("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
        assertTrue(transactionRunner.rolledBack)
    }

    @Test
    fun トランザクションが失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("データベースを開けない")
        transactionRunner.runFailure = TransactionRunnerFailure.Unexpected(cause)

        val result = createRecipe(
            name = "麻婆豆腐",
            status = RecipeStatus.DRAFT,
            ingredients = listOf(ingredient(ItemInput.Typed("長ネギ"))),
        )

        assertEquals(CreateRecipeResult.Unexpected(cause), result)
    }
}
