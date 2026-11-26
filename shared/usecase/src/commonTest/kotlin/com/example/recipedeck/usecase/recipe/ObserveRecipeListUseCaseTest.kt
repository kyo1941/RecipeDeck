package com.example.recipedeck.usecase.recipe

import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.notBlankStringOrFail
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveRecipeListUseCaseTest {
    private val recipeRepository = FakeRecipeRepository()
    private val observeRecipeList = ObserveRecipeListUseCase(recipeRepository)

    private fun recipe(id: String, name: String) = Recipe(
        id = RecipeId(id),
        name = notBlankStringOrFail(name),
        status = RecipeStatus.READY,
        photoRef = null,
        memo = null,
        ingredients = persistentListOf(),
    )

    @Test
    fun 登録されているRecipeを返す() = runTest {
        val gingerPork = recipe("recipe-1", "生姜焼き")
        val salad = recipe("recipe-2", "サラダ")
        recipeRepository.savedRecipes += listOf(gingerPork, salad)

        assertEquals(ObserveRecipeListResult.Listed(persistentSetOf(gingerPork, salad)), observeRecipeList().first())
    }

    @Test
    fun Recipeの読み込みに失敗したら原因を持った予期しない失敗を返す() = runTest {
        val cause = IllegalStateException("保存先から読み込めない")
        recipeRepository.findFailure = RecipeRepositoryFailure.Unexpected(cause)

        assertEquals(ObserveRecipeListResult.Unexpected(cause), observeRecipeList().first())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun Recipeが保存されると返し直す() = runTest {
        val results = mutableListOf<ObserveRecipeListResult>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observeRecipeList().toList(results) }
        val gingerPork = recipe("recipe-1", "生姜焼き")

        recipeRepository.save(gingerPork)

        assertEquals(ObserveRecipeListResult.Listed(persistentSetOf(gingerPork)), results.last())
    }
}
