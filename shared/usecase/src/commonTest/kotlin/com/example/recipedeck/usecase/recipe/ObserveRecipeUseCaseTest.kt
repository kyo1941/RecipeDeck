package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.item.ItemRepositoryFailure
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeIngredient
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.item.FakeItemRepository
import com.example.recipedeck.usecase.notBlankStringOrFail
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveRecipeUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val itemRepository = FakeItemRepository()
    private val observeRecipe = ObserveRecipeUseCase(recipeRepository, itemRepository)

    private val pork = Item(ItemId("item-pork"), notBlankStringOrFail("豚肉"))
    private val onion = Item(ItemId("item-onion"), notBlankStringOrFail("たまねぎ"))

    private fun recipe(vararg items: Item) = Recipe(
        id = RecipeId("recipe-1"),
        name = notBlankStringOrFail("生姜焼き"),
        status = RecipeStatus.READY,
        photoRef = null,
        memo = null,
        ingredients = items.map { RecipeIngredient(it.id, null) }.toImmutableList(),
    )

    @Test
    fun Recipeを材料の並びのままItemと組にして返す() = runTest {
        itemRepository.savedItems += listOf(pork, onion)
        val gingerPork = recipe(onion, pork)
        recipeRepository.savedRecipes += gingerPork

        val expected = ObserveRecipeResult.Found(gingerPork, persistentListOf(ObserveRecipeResult.Found.Ingredient(onion, null), ObserveRecipeResult.Found.Ingredient(pork, null)))
        assertEquals(expected, observeRecipe(RecipeId("recipe-1")).first())
    }

    @Test
    fun Recipeがなければ見つからないことを返す() = runTest {
        assertEquals(ObserveRecipeResult.RecipeNoLongerExists, observeRecipe(RecipeId("recipe-1")).first())
    }

    @Test
    fun 材料が指すItemが見つからなければ予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe(pork)

        assertIs<ObserveRecipeResult.Unexpected>(observeRecipe(RecipeId("recipe-1")).first())
    }

    @Test
    fun Recipeの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        recipeRepository.findFailure = RecipeRepositoryFailure.Unexpected(cause)

        assertEquals(ObserveRecipeResult.Unexpected(cause), observeRecipe(RecipeId("recipe-1")).first())
    }

    @Test
    fun Itemの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        recipeRepository.savedRecipes += recipe(pork)
        val cause = IllegalStateException("保存先から読み込めない")
        itemRepository.findFailure = ItemRepositoryFailure.Unexpected(cause)

        assertEquals(ObserveRecipeResult.Unexpected(cause), observeRecipe(RecipeId("recipe-1")).first())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun Recipeが保存されると返し直す() = runTest {
        itemRepository.savedItems += listOf(pork, onion)
        recipeRepository.savedRecipes += recipe(pork)
        val results = mutableListOf<ObserveRecipeResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observeRecipe(RecipeId("recipe-1")).toList(results) }

        val edited = recipe(pork, onion)
        recipeRepository.save(edited)

        val expected = ObserveRecipeResult.Found(edited, persistentListOf(ObserveRecipeResult.Found.Ingredient(pork, null), ObserveRecipeResult.Found.Ingredient(onion, null)))
        assertEquals(expected, results.last())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun Itemの表示名が変わると返し直す() = runTest {
        itemRepository.savedItems += pork
        val gingerPork = recipe(pork)
        recipeRepository.savedRecipes += gingerPork
        val results = mutableListOf<ObserveRecipeResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observeRecipe(RecipeId("recipe-1")).toList(results) }

        val renamed = pork.copy(displayName = notBlankStringOrFail("豚こま肉"))
        itemRepository.save(renamed)

        assertEquals(ObserveRecipeResult.Found(gingerPork, persistentListOf(ObserveRecipeResult.Found.Ingredient(renamed, null))), results.last())
    }
}
