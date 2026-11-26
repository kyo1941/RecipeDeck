package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.ingredient.ItemInput
import com.example.recipedeck.usecase.item.SearchItemsResult
import com.example.recipedeck.usecase.item.SearchItemsUseCase
import com.example.recipedeck.usecase.recipe.EditRecipeResult
import com.example.recipedeck.usecase.recipe.ObserveRecipeResult
import com.example.recipedeck.usecase.recipe.ObserveRecipeUseCase
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class EditRecipeViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun notBlank(raw: String): NotBlankString = NotBlankString.of(raw) ?: fail("空白になっている")
    private fun quantity(numerator: Long, denominator: Long = 1): Quantity = Quantity.of(numerator, denominator) ?: fail("数量が作れない")

    private val pork = Item(ItemId("item-pork"), notBlank("豚肉"))
    private val halfPiece = Amount(quantity(1, 2), QuantityNotation.FRACTION, AmountUnit.PIECE)
    private val gingerPork = ObserveRecipeResult.Found(
        recipe = Recipe(
            id = RecipeId("recipe-1"),
            name = notBlank("生姜焼き"),
            status = RecipeStatus.DRAFT,
            photoRef = null,
            memo = notBlank("強火で焼く"),
            ingredients = persistentListOf(RecipeIngredient(pork.id, halfPiece)),
        ),
        ingredients = persistentListOf(ObserveRecipeResult.Found.Ingredient(pork, halfPiece)),
    )

    private var editedRecipeId: RecipeId? = null
    private var editedIngredients: List<IngredientInput>? = null

    private fun viewModel(
        shown: ObserveRecipeUseCase = ObserveRecipeUseCase { flowOf(gingerPork) },
        result: EditRecipeResult = EditRecipeResult.Saved(persistentListOf()),
        searchItems: SearchItemsUseCase = SearchItemsUseCase { SearchItemsResult.Found(persistentListOf()) },
    ) = EditRecipeViewModel(
        recipeId = gingerPork.recipe.id,
        observeRecipe = shown,
        editRecipe = { recipeId, _, _, _, ingredients ->
            editedRecipeId = recipeId
            editedIngredients = ingredients
            result
        },
        searchItems = searchItems,
        leftoverEditKeeper = LeftoverRecipeEditKeeper(leftoverEdits, leftoverKey),
    )

    private val leftoverKey = LeftoverRecipeEditKey.OfRecipe(RecipeId("recipe-1"))
    private val leftoverEdits = FakeLeftoverRecipeEditStore()

    private val EditRecipeViewModel.editor: RecipeEditorUiState
        get() = assertIs<EditRecipeUiState.Editing>(uiState.value).editor

    private val EditRecipeViewModel.firstKey: Long
        get() = editor.form.ingredients.rows.first().key

    @Test
    fun Recipeを読んで入力欄にする() = runTest {
        val viewModel = viewModel()

        val form = viewModel.editor.form
        assertEquals("生姜焼き", form.name)
        assertEquals(true, form.isDraft)
        assertEquals("強火で焼く", form.memo)
        val ingredient = form.ingredients.rows.single().ingredient
        assertEquals("豚肉", ingredient.name)
        assertEquals(pork.id, ingredient.selectedItemId)
        assertEquals("1/2", ingredient.quantity.text)
        assertEquals(AmountUnit.PIECE, ingredient.unit)
    }

    @Test
    fun 読んだ材料は既存のItemとして渡し足した材料は打った名前として渡し保存できたら閉じる() = runTest {
        val viewModel = viewModel()
        viewModel.addIngredient()
        viewModel.changeIngredientName(viewModel.editor.form.ingredients.rows.last().key, "玉ねぎ")

        viewModel.save()

        assertEquals(listOf(ItemInput.Selected(pork.id), ItemInput.Typed("玉ねぎ")), editedIngredients?.map { it.item })
        assertEquals(EditRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 開いたRecipeのIDで保存する() = runTest {
        val viewModel = viewModel()

        viewModel.save()

        assertEquals(gingerPork.recipe.id, editedRecipeId)
    }

    @Test
    fun 既存の材料の名前を変えたら打った名前として渡す() = runTest {
        val viewModel = viewModel()
        viewModel.changeIngredientName(viewModel.firstKey, "豚こま肉")

        viewModel.save()

        assertEquals(ItemInput.Typed("豚こま肉"), editedIngredients?.single()?.item)
    }

    @Test
    fun 保存のときにRecipeがなければ見つからない状態にする() = runTest {
        val viewModel = viewModel(result = EditRecipeResult.RecipeNoLongerExists)

        viewModel.save()

        assertEquals(EditRecipeUiState.NoLongerExists, viewModel.uiState.value)
    }

    @Test
    fun 編集するRecipeがなければそのことを状態にする() = runTest {
        val viewModel = viewModel(shown = { flowOf(ObserveRecipeResult.RecipeNoLongerExists) })

        assertEquals(EditRecipeUiState.NoLongerExists, viewModel.uiState.value)
    }

    @Test
    fun 読み込みに失敗したら再試行で読み直す() = runTest {
        var calls = 0
        val viewModel = viewModel(
            shown = {
                calls++
                if (calls == 1) flowOf(ObserveRecipeResult.Unexpected(IllegalStateException())) else flowOf(gingerPork)
            },
        )
        assertEquals(EditRecipeUiState.LoadFailed, viewModel.uiState.value)

        viewModel.retry()

        assertEquals("生姜焼き", viewModel.editor.form.name)
    }

    @Test
    fun 読み込み中に閉じたら読み込みが終わっても閉じたままになる() = runTest {
        val shown = MutableSharedFlow<ObserveRecipeResult>()
        val viewModel = viewModel(shown = { shown })

        viewModel.requestClose()
        shown.emit(gingerPork)

        assertEquals(EditRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 見つからない画面で閉じると閉じる() = runTest {
        val viewModel = viewModel(shown = { flowOf(ObserveRecipeResult.RecipeNoLongerExists) })

        viewModel.requestClose()

        assertEquals(EditRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 何も変えていなければ確かめずに閉じる() = runTest {
        val viewModel = viewModel()

        viewModel.requestClose()

        assertEquals(EditRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 入力を元に戻していたら確かめずに閉じる() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き（甘め）")
        viewModel.changeName("生姜焼き")

        viewModel.requestClose()

        assertEquals(EditRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun カーソルを動かしただけなら確かめずに閉じる() = runTest {
        val viewModel = viewModel()
        viewModel.changeQuantity(viewModel.firstKey, TextFieldValue("1/2", TextRange(0)))

        viewModel.requestClose()

        assertEquals(EditRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 入力を変えていたら閉じる前に確かめる() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き（甘め）")

        viewModel.requestClose()

        assertTrue(viewModel.editor.isConfirmingDiscard)
    }

    @Test
    fun Itemを選んであった材料の名前を打ち直しても候補から元のItemに戻せる() = runTest {
        val viewModel = viewModel(searchItems = { SearchItemsResult.Found(persistentListOf(pork)) })
        viewModel.changeIngredientName(viewModel.firstKey, "豚")
        assertEquals<List<Item>?>(listOf(pork), viewModel.editor.shownFoundItems?.items)

        viewModel.selectItem(viewModel.firstKey, pork)

        assertEquals(pork.id, viewModel.editor.form.ingredients.rows.single().ingredient.selectedItemId)
        viewModel.save()
        assertEquals(ItemInput.Selected(pork.id), editedIngredients?.single()?.item)
    }

    @Test
    fun 残った編集があれば示す() = runTest {
        val leftover = RecipeForm.of(gingerPork).copy(name = "途中の生姜焼き").toLeftoverEdit()
        leftoverEdits.save(leftoverKey, leftover)

        val viewModel = viewModel()

        assertEquals("生姜焼き", viewModel.editor.form.name)
        assertEquals("途中の生姜焼き", viewModel.editor.offeredLeftover?.name)
    }

    @Test
    fun Recipeが見つからなければ残った編集を消す() = runTest {
        leftoverEdits.save(leftoverKey, RecipeForm.of(gingerPork).toLeftoverEdit())

        viewModel(shown = { flowOf(ObserveRecipeResult.RecipeNoLongerExists) })

        assertEquals(emptyMap(), leftoverEdits.edits.value)
    }

    @Test
    fun 分数で入力した整数の材料はメモだけ直して保存しても分数のまま() = runTest {
        val twoWhole = Amount(quantity(2), QuantityNotation.FRACTION, AmountUnit.PIECE)
        val shown = gingerPork.copy(
            recipe = gingerPork.recipe.copy(ingredients = persistentListOf(RecipeIngredient(pork.id, twoWhole))),
            ingredients = persistentListOf(ObserveRecipeResult.Found.Ingredient(pork, twoWhole)),
        )
        val viewModel = viewModel(shown = { flowOf(shown) })
        assertEquals("2/1", viewModel.editor.form.ingredients.rows.single().ingredient.quantity.text)

        viewModel.changeMemo("弱火で焼く")
        viewModel.save()

        assertEquals(QuantityNotation.FRACTION, editedIngredients?.single()?.notation)
    }
}
