package com.example.recipedeck.ui.recipe

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.recipe.ObserveRecipeListResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun recipe(id: String, name: String, status: RecipeStatus = RecipeStatus.READY) = Recipe(
        id = RecipeId(id),
        name = NotBlankString.of(name) ?: fail("名前が空白になっている"),
        status = status,
        photoRef = null,
        memo = null,
        ingredients = persistentListOf(),
    )

    private fun TestScope.subscribe(viewModel: RecipeListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun 最初の結果が届くまでは読み込み中になる() = runTest {
        val results = MutableSharedFlow<ObserveRecipeListResult>()
        val viewModel = RecipeListViewModel(observeRecipeList = { results }, leftoverEdits = FakeLeftoverRecipeEditStore())
        subscribe(viewModel)

        assertEquals(RecipeListUiState.Loading, viewModel.uiState.value)

        results.emit(ObserveRecipeListResult.Listed(persistentSetOf()))

        assertEquals(RecipeListUiState.NoRecipes, viewModel.uiState.value)
    }

    @Test
    fun Recipeが1件もなければそのことを状態にする() = runTest {
        val viewModel = RecipeListViewModel(observeRecipeList = { flowOf(ObserveRecipeListResult.Listed(persistentSetOf())) }, leftoverEdits = FakeLeftoverRecipeEditStore())
        subscribe(viewModel)

        assertEquals(RecipeListUiState.NoRecipes, viewModel.uiState.value)
    }

    @Test
    fun Recipeを名前の順に並べ名前が同じならIDの順に並べる() = runTest {
        val recipes = persistentSetOf(
            recipe("recipe-3", "生姜焼き"),
            recipe("recipe-2", "サラダ"),
            recipe("recipe-1", "生姜焼き"),
        )
        val viewModel = RecipeListViewModel(observeRecipeList = { flowOf(ObserveRecipeListResult.Listed(recipes)) }, leftoverEdits = FakeLeftoverRecipeEditStore())
        subscribe(viewModel)

        val expected = persistentListOf(
            RecipeListItem(RecipeId("recipe-2"), "サラダ", isDraft = false),
            RecipeListItem(RecipeId("recipe-1"), "生姜焼き", isDraft = false),
            RecipeListItem(RecipeId("recipe-3"), "生姜焼き", isDraft = false),
        )
        assertEquals(RecipeListUiState.Shown(expected), viewModel.uiState.value)
    }

    @Test
    fun 下書きのRecipeはそのことを行に持つ() = runTest {
        val recipes = persistentSetOf(recipe("recipe-1", "生姜焼き", RecipeStatus.DRAFT), recipe("recipe-2", "麻婆豆腐"))
        val viewModel = RecipeListViewModel(observeRecipeList = { flowOf(ObserveRecipeListResult.Listed(recipes)) }, leftoverEdits = FakeLeftoverRecipeEditStore())
        subscribe(viewModel)

        val expected = persistentListOf(
            RecipeListItem(RecipeId("recipe-1"), "生姜焼き", isDraft = true),
            RecipeListItem(RecipeId("recipe-2"), "麻婆豆腐", isDraft = false),
        )
        assertEquals(RecipeListUiState.Shown(expected), viewModel.uiState.value)
    }

    @Test
    fun 読み込みに失敗したらそのことを状態にする() = runTest {
        val viewModel = RecipeListViewModel(observeRecipeList = { flowOf(ObserveRecipeListResult.Unexpected(IllegalStateException())) }, leftoverEdits = FakeLeftoverRecipeEditStore())
        subscribe(viewModel)

        assertEquals(RecipeListUiState.LoadFailed, viewModel.uiState.value)
    }

    @Test
    fun 結果が届き直すたびに状態を置き換える() = runTest {
        val results = MutableSharedFlow<ObserveRecipeListResult>()
        val viewModel = RecipeListViewModel(observeRecipeList = { results }, leftoverEdits = FakeLeftoverRecipeEditStore())
        subscribe(viewModel)

        results.emit(ObserveRecipeListResult.Listed(persistentSetOf(recipe("recipe-1", "生姜焼き"))))
        assertEquals(RecipeListUiState.Shown(persistentListOf(RecipeListItem(RecipeId("recipe-1"), "生姜焼き", isDraft = false))), viewModel.uiState.value)

        results.emit(ObserveRecipeListResult.Unexpected(IllegalStateException()))
        assertEquals(RecipeListUiState.LoadFailed, viewModel.uiState.value)

        results.emit(ObserveRecipeListResult.Listed(persistentSetOf()))
        assertEquals(RecipeListUiState.NoRecipes, viewModel.uiState.value)
    }

    @Test
    fun 再試行するとRecipeの一覧を読み直して新しい結果を状態にする() = runTest {
        var calls = 0
        val viewModel = RecipeListViewModel(
            observeRecipeList = {
                calls++
                if (calls == 1) {
                    flowOf(ObserveRecipeListResult.Unexpected(IllegalStateException()))
                } else {
                    flowOf(ObserveRecipeListResult.Listed(persistentSetOf()))
                }
            },
            leftoverEdits = FakeLeftoverRecipeEditStore(),
        )
        subscribe(viewModel)
        assertEquals(RecipeListUiState.LoadFailed, viewModel.uiState.value)

        viewModel.retry()

        assertEquals(RecipeListUiState.NoRecipes, viewModel.uiState.value)
        assertEquals(2, calls)
    }

    @Test
    fun 作りかけの新しいRecipeの残った編集があるかを示し破棄できる() = runTest {
        val leftoverEdits = FakeLeftoverRecipeEditStore(
            LeftoverRecipeEditKey.NewRecipe to LeftoverRecipeEdit(name = "途中", isDraft = false, memo = "", ingredients = emptyList()),
        )
        val viewModel = RecipeListViewModel(observeRecipeList = { flowOf(ObserveRecipeListResult.Listed(persistentSetOf())) }, leftoverEdits = leftoverEdits)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.hasLeftoverNewRecipe.collect {} }
        assertEquals(true, viewModel.hasLeftoverNewRecipe.value)

        viewModel.discardLeftoverNewRecipe()

        assertEquals(false, viewModel.hasLeftoverNewRecipe.value)
    }
}
