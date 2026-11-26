package com.example.recipedeck.ui.recipe

import com.example.recipedeck.domain.NotBlankString
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
import com.example.recipedeck.usecase.recipe.ObserveRecipeResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
class RecipeViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun notBlank(raw: String): NotBlankString = NotBlankString.of(raw) ?: fail("空白になっている")

    private val pork = Item(ItemId("item-pork"), notBlank("豚肉"))
    private val ginger = Item(ItemId("item-ginger"), notBlank("生姜"))
    private val porkAmount = Amount(Quantity.of(200) ?: fail("数量が作れない"), QuantityNotation.DECIMAL, AmountUnit.GRAM)

    private fun found(status: RecipeStatus = RecipeStatus.READY, memo: String? = null, ingredients: List<ObserveRecipeResult.Found.Ingredient> = emptyList()) =
        ObserveRecipeResult.Found(
            recipe = Recipe(
                id = RecipeId("recipe-1"),
                name = notBlank("生姜焼き"),
                status = status,
                photoRef = null,
                memo = memo?.let(::notBlank),
                ingredients = ingredients.map { RecipeIngredient(it.item.id, it.amount) }.toImmutableList(),
            ),
            ingredients = ingredients.toImmutableList(),
        )

    private fun TestScope.subscribe(viewModel: RecipeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun 最初の結果が届くまでは読み込み中になる() = runTest {
        val results = MutableSharedFlow<ObserveRecipeResult>()
        val viewModel = RecipeViewModel(recipeId = RecipeId("recipe-1"), observeRecipe = { results })
        subscribe(viewModel)

        assertEquals(RecipeUiState.Loading, viewModel.uiState.value)

        results.emit(found())

        assertEquals(RecipeUiState.Shown("生姜焼き", isDraft = false, ingredients = persistentListOf(), memo = null), viewModel.uiState.value)
    }

    @Test
    fun 開いたRecipeのIDで読む() = runTest {
        var observedRecipeId: RecipeId? = null
        val viewModel = RecipeViewModel(
            recipeId = RecipeId("recipe-1"),
            observeRecipe = { recipeId ->
                observedRecipeId = recipeId
                flowOf(found())
            },
        )
        subscribe(viewModel)

        assertEquals(RecipeId("recipe-1"), observedRecipeId)
    }

    @Test
    fun 材料を並びのまま表示名と分量にしメモも持つ() = runTest {
        val result = found(memo = "強火で焼く", ingredients = listOf(ObserveRecipeResult.Found.Ingredient(pork, porkAmount), ObserveRecipeResult.Found.Ingredient(ginger, null)))
        val viewModel = RecipeViewModel(recipeId = RecipeId("recipe-1"), observeRecipe = { flowOf(result) })
        subscribe(viewModel)

        val expected = RecipeUiState.Shown(
            name = "生姜焼き",
            isDraft = false,
            ingredients = persistentListOf(RecipeIngredientItem("豚肉", porkAmount), RecipeIngredientItem("生姜", null)),
            memo = "強火で焼く",
        )
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun 下書きのRecipeはそのことを状態に持つ() = runTest {
        val viewModel = RecipeViewModel(recipeId = RecipeId("recipe-1"), observeRecipe = { flowOf(found(status = RecipeStatus.DRAFT)) })
        subscribe(viewModel)

        assertEquals(RecipeUiState.Shown("生姜焼き", isDraft = true, ingredients = persistentListOf(), memo = null), viewModel.uiState.value)
    }

    @Test
    fun Recipeが見つからなければそのことを状態にする() = runTest {
        val viewModel = RecipeViewModel(recipeId = RecipeId("recipe-1"), observeRecipe = { flowOf(ObserveRecipeResult.RecipeNoLongerExists) })
        subscribe(viewModel)

        assertEquals(RecipeUiState.NoLongerExists, viewModel.uiState.value)
    }

    @Test
    fun 読み込みに失敗したらそのことを状態にする() = runTest {
        val viewModel = RecipeViewModel(recipeId = RecipeId("recipe-1"), observeRecipe = { flowOf(ObserveRecipeResult.Unexpected(IllegalStateException())) })
        subscribe(viewModel)

        assertEquals(RecipeUiState.LoadFailed, viewModel.uiState.value)
    }

    @Test
    fun 再試行するとRecipeを読み直して新しい結果を状態にする() = runTest {
        var calls = 0
        val viewModel = RecipeViewModel(
            recipeId = RecipeId("recipe-1"),
            observeRecipe = {
                calls++
                if (calls == 1) flowOf(ObserveRecipeResult.Unexpected(IllegalStateException())) else flowOf(found())
            },
        )
        subscribe(viewModel)
        assertEquals(RecipeUiState.LoadFailed, viewModel.uiState.value)

        viewModel.retry()

        assertEquals(RecipeUiState.Shown("生姜焼き", isDraft = false, ingredients = persistentListOf(), memo = null), viewModel.uiState.value)
        assertEquals(2, calls)
    }
}
