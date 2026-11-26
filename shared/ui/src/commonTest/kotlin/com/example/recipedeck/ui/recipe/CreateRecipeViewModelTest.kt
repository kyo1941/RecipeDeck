package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Quantity
import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.ingredient.ItemInput
import com.example.recipedeck.usecase.item.SearchItemsResult
import com.example.recipedeck.usecase.recipe.CreateRecipeResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecipeViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private class CreatedRecipe(val name: String, val status: RecipeStatus, val memo: String, val ingredients: List<IngredientInput>)

    private fun quantity(numerator: Long, denominator: Long = 1): Quantity = Quantity.of(numerator, denominator) ?: fail("数量が作れない")

    private var created: CreatedRecipe? = null

    private fun notBlank(raw: String): NotBlankString = NotBlankString.of(raw) ?: fail("空白になっている")

    private val pork = Item(ItemId("item-pork"), notBlank("豚肉"))
    private val porkBelly = Item(ItemId("item-pork-belly"), notBlank("豚バラ肉"))

    private val leftoverEdits = FakeLeftoverRecipeEditStore()

    private val storedLeftover: LeftoverRecipeEdit?
        get() = leftoverEdits.edits.value[LeftoverRecipeEditKey.NewRecipe]

    private val leftover = LeftoverRecipeEdit(
        name = "途中の生姜焼き",
        isDraft = true,
        memo = "",
        ingredients = listOf(LeftoverRecipeEdit.Ingredient(name = "豚肉", selectedItemId = "item-pork", quantity = "1/2", unit = AmountUnit.PIECE)),
    )

    private fun viewModel(
        searchItems: suspend (String) -> SearchItemsResult = { SearchItemsResult.Found(persistentListOf()) },
        resumesLeftover: Boolean = false,
        result: suspend () -> CreateRecipeResult = { CreateRecipeResult.Created(RecipeId("new"), persistentListOf()) },
    ) = CreateRecipeViewModel(
        createRecipe = { name, status, memo, ingredients ->
            created = CreatedRecipe(name, status, memo, ingredients)
            result()
        },
        searchItems = searchItems,
        leftoverEditKeeper = LeftoverRecipeEditKeeper(leftoverEdits, LeftoverRecipeEditKey.NewRecipe),
        resumesLeftover = resumesLeftover,
    )

    private fun viewModel(result: suspend () -> CreateRecipeResult) = viewModel(searchItems = { SearchItemsResult.Found(persistentListOf()) }, result = result)

    private fun viewModelReadingLeftoverAfter(reading: CompletableDeferred<Unit>): CreateRecipeViewModel {
        val slowStore = object : LeftoverRecipeEditStore by leftoverEdits {
            override fun leftoverEdit(key: LeftoverRecipeEditKey) = flow {
                reading.await()
                emit(leftoverEdits.edits.value[key])
            }
        }
        return CreateRecipeViewModel(
            createRecipe = { _, _, _, _ -> CreateRecipeResult.Created(RecipeId("new"), persistentListOf()) },
            searchItems = { SearchItemsResult.Found(persistentListOf()) },
            leftoverEditKeeper = LeftoverRecipeEditKeeper(slowStore, LeftoverRecipeEditKey.NewRecipe),
            resumesLeftover = false,
        )
    }

    private val CreateRecipeViewModel.editor: RecipeEditorUiState
        get() = assertIs<CreateRecipeUiState.Editing>(uiState.value).editor

    private val CreateRecipeViewModel.firstKey: Long
        get() = editor.form.ingredients.rows.first().key

    private val CreateRecipeViewModel.firstQuantity: TextFieldValue
        get() = editor.form.ingredients.rows.first().ingredient.quantity

    private fun CreateRecipeViewModel.typeQuantity(text: String, key: Long = firstKey) =
        changeQuantity(key, TextFieldValue(text, TextRange(text.length)))

    @Test
    fun 空の材料の行が1つあり利用可能で始まる() = runTest {
        val viewModel = viewModel()

        assertEquals("", viewModel.editor.form.name)
        assertEquals(false, viewModel.editor.form.isDraft)
        assertEquals(1, viewModel.editor.form.ingredients.rows.size)
    }

    @Test
    fun 入力をそのまま渡し保存できたら閉じる() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き")
        viewModel.changeMemo("強火で焼く")
        viewModel.changeIngredientName(viewModel.firstKey, "豚肉")
        viewModel.typeQuantity("200")
        viewModel.changeUnit(viewModel.firstKey, AmountUnit.GRAM)

        viewModel.save()

        val saved = created ?: fail("保存されていない")
        assertEquals("生姜焼き", saved.name)
        assertEquals(RecipeStatus.READY, saved.status)
        assertEquals("強火で焼く", saved.memo)
        assertEquals(listOf(IngredientInput(ItemInput.Typed("豚肉"), quantity(200), QuantityNotation.DECIMAL, AmountUnit.GRAM)), saved.ingredients)
        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 下書きにすると下書きで保存する() = runTest {
        val viewModel = viewModel()
        viewModel.changeDraft(true)

        viewModel.save()

        assertEquals(RecipeStatus.DRAFT, created?.status)
    }

    @Test
    fun 分数は分数の入力形式で渡す() = runTest {
        val viewModel = viewModel()
        viewModel.typeQuantity("1/2")

        viewModel.save()

        val ingredient = created?.ingredients?.single() ?: fail("材料がない")
        assertEquals(quantity(1, 2), ingredient.quantity)
        assertEquals(QuantityNotation.FRACTION, ingredient.notation)
    }

    @Test
    fun 小数点で始まる数と終わる数は小数として渡す() = runTest {
        val viewModel = viewModel()
        viewModel.typeQuantity(".5")
        viewModel.addIngredient()
        viewModel.typeQuantity("1.", key = viewModel.editor.form.ingredients.rows.last().key)

        viewModel.save()

        val ingredients = created?.ingredients ?: fail("保存されていない")
        assertEquals(quantity(1, 2), ingredients[0].quantity)
        assertEquals(QuantityNotation.DECIMAL, ingredients[0].notation)
        assertEquals(quantity(1), ingredients[1].quantity)
    }

    @Test
    fun 数として読めない入力は数量なしとして渡す() = runTest {
        listOf("0", "0/3", "3/0", "1/", "/12", ".").forEach { text ->
            val viewModel = viewModel()
            viewModel.typeQuantity(text)

            viewModel.save()

            assertNull(created?.ingredients?.single()?.quantity, "「$text」が数量になった")
        }
    }

    @Test
    fun 数量には数字と小数点とスラッシュ以外を入れない() = runTest {
        val viewModel = viewModel()

        viewModel.typeQuantity("1a2")

        assertEquals("12", viewModel.firstQuantity.text)
    }

    @Test
    fun 数量の規則に合わない入力は受け付けない() = runTest {
        listOf("1/2/3", "1.5/2", "1.2.3", "12345", "1.2345", "12345/1", "1/12345").forEach { text ->
            val viewModel = viewModel()
            viewModel.typeQuantity("1")

            viewModel.typeQuantity(text)

            assertEquals("1", viewModel.firstQuantity.text, "「$text」を受け付けた")
        }
    }

    @Test
    fun スラッシュはカーソルの位置に入れる() = runTest {
        val viewModel = viewModel()
        viewModel.changeQuantity(viewModel.firstKey, TextFieldValue("12", TextRange(1)))

        viewModel.insertFractionSlash(viewModel.firstKey)

        assertEquals(TextFieldValue("1/2", TextRange(2)), viewModel.firstQuantity)
    }

    @Test
    fun スラッシュが入らない数量にはスラッシュを入れない() = runTest {
        val viewModel = viewModel()
        viewModel.typeQuantity("1.5")

        viewModel.insertFractionSlash(viewModel.firstKey)

        assertEquals("1.5", viewModel.firstQuantity.text)
    }

    @Test
    fun 保存を試す前は入力の不足を示さない() = runTest {
        val viewModel = viewModel()
        viewModel.typeQuantity("200")

        assertEquals(false, viewModel.editor.isNameRequired)
        assertTrue(viewModel.editor.ingredientKeysRequiringName.isEmpty())
    }

    @Test
    fun 保存を試した後は入力に合わせて不足を示す() = runTest {
        val viewModel = viewModel { CreateRecipeResult.NameRequired }
        viewModel.typeQuantity("200")

        viewModel.save()

        assertTrue(viewModel.editor.isNameRequired)
        assertEquals(setOf(viewModel.firstKey), viewModel.editor.ingredientKeysRequiringName)

        viewModel.changeName("生姜焼き")
        viewModel.changeIngredientName(viewModel.firstKey, "豚肉")

        assertEquals(false, viewModel.editor.isNameRequired)
        assertTrue(viewModel.editor.ingredientKeysRequiringName.isEmpty())
    }

    @Test
    fun 保存に失敗したら閉じずに入力を残し失敗を示す() = runTest {
        val viewModel = viewModel { CreateRecipeResult.Unexpected(IllegalStateException()) }
        viewModel.changeName("生姜焼き")

        viewModel.save()

        assertEquals(SaveStatus.Failed, viewModel.editor.saveStatus)
        assertEquals("生姜焼き", viewModel.editor.form.name)
    }

    @Test
    fun 保存している間はもう一度保存せず閉じる操作も受けない() = runTest {
        val result = CompletableDeferred<CreateRecipeResult>()
        var calls = 0
        val viewModel = viewModel {
            calls++
            result.await()
        }

        viewModel.save()
        viewModel.save()
        viewModel.requestClose()

        assertEquals(1, calls)
        assertEquals(SaveStatus.Saving, viewModel.editor.saveStatus)
        result.complete(CreateRecipeResult.Created(RecipeId("new"), persistentListOf()))
        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 確かめた後に編集を続けても保存できない入力の表示は残る() = runTest {
        val viewModel = viewModel { CreateRecipeResult.NameRequired }
        viewModel.changeMemo("強火で焼く")
        viewModel.save()

        viewModel.requestClose()
        viewModel.keepEditing()

        assertTrue(viewModel.editor.isNameRequired)
    }

    @Test
    fun 保存している間は入力を受け付けない() = runTest {
        val result = CompletableDeferred<CreateRecipeResult>()
        val viewModel = viewModel { result.await() }
        viewModel.changeName("生姜焼き")
        viewModel.save()

        viewModel.changeName("生姜焼き（甘め）")
        viewModel.addIngredient()

        assertEquals("生姜焼き", viewModel.editor.form.name)
        assertEquals(1, viewModel.editor.form.ingredients.rows.size)
        result.complete(CreateRecipeResult.Unexpected(IllegalStateException()))
    }

    @Test
    fun 何も入力していなければ確かめずに閉じる() = runTest {
        val viewModel = viewModel()

        viewModel.requestClose()

        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 入力していたら閉じる前に確かめ破棄すると閉じ続けると戻る() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き")

        viewModel.requestClose()
        assertTrue(viewModel.editor.isConfirmingDiscard)

        viewModel.keepEditing()
        assertEquals(false, viewModel.editor.isConfirmingDiscard)
        assertEquals("生姜焼き", viewModel.editor.form.name)

        viewModel.requestClose()
        viewModel.discard()
        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 材料の名前で探したItemをその行の候補にする() = runTest {
        val keywords = mutableListOf<String>()
        val viewModel = viewModel(searchItems = { keyword ->
            keywords += keyword
            SearchItemsResult.Found(persistentListOf(pork, porkBelly))
        })

        viewModel.changeIngredientName(viewModel.firstKey, "豚")

        assertEquals(listOf("豚"), keywords)
        val found = viewModel.editor.shownFoundItems
        assertEquals(viewModel.firstKey, found?.query?.ingredientKey)
        assertEquals<List<Item>?>(listOf(pork, porkBelly), found?.items)
    }

    @Test
    fun 候補を選ぶとそのItemを使う材料になり候補は消える() = runTest {
        val viewModel = viewModel(searchItems = { SearchItemsResult.Found(persistentListOf(pork)) })
        viewModel.changeName("生姜焼き")
        viewModel.changeIngredientName(viewModel.firstKey, "豚")

        viewModel.selectItem(viewModel.firstKey, pork)

        assertNull(viewModel.editor.shownFoundItems)
        assertEquals("豚肉", viewModel.editor.form.ingredients.rows.first().ingredient.name)
        viewModel.save()
        assertEquals(ItemInput.Selected(pork.id), created?.ingredients?.single()?.item)
    }

    @Test
    fun 名前を打ち直したら前の名前の候補は出さない() = runTest {
        val viewModel = viewModel(searchItems = { keyword ->
            if (keyword == "豚") SearchItemsResult.Found(persistentListOf(pork)) else CompletableDeferred<SearchItemsResult>().await()
        })
        viewModel.changeIngredientName(viewModel.firstKey, "豚")

        viewModel.changeIngredientName(viewModel.firstKey, "豚バ")

        assertNull(viewModel.editor.shownFoundItems)
    }

    @Test
    fun 前の名前の検索の結果が後から届いても当てない() = runTest {
        val slowResult = CompletableDeferred<SearchItemsResult>()
        val viewModel = viewModel(searchItems = { keyword ->
            if (keyword == "豚") slowResult.await() else SearchItemsResult.Found(persistentListOf(porkBelly))
        })
        viewModel.changeIngredientName(viewModel.firstKey, "豚")
        viewModel.changeIngredientName(viewModel.firstKey, "豚バ")

        slowResult.complete(SearchItemsResult.Found(persistentListOf(pork)))

        assertEquals<List<Item>?>(listOf(porkBelly), viewModel.editor.shownFoundItems?.items)
    }

    @Test
    fun 探せなかったときは候補を出さない() = runTest {
        val viewModel = viewModel(searchItems = { SearchItemsResult.Unexpected(IllegalStateException("読めない")) })

        viewModel.changeIngredientName(viewModel.firstKey, "豚")

        assertEquals(emptyList(), viewModel.editor.shownFoundItems?.items.orEmpty())
    }

    @Test
    fun 保存中は候補を出さない() = runTest {
        val viewModel = viewModel(
            searchItems = { SearchItemsResult.Found(persistentListOf(pork)) },
            result = { CompletableDeferred<CreateRecipeResult>().await() },
        )
        viewModel.changeIngredientName(viewModel.firstKey, "豚")

        viewModel.save()

        assertNull(viewModel.editor.shownFoundItems)
    }

    @Test
    fun 入力が変わってから1秒たつと残った編集に書く() = runTest {
        val viewModel = viewModel()

        viewModel.changeName("生姜焼き")
        advanceTimeBy(999)
        assertNull(storedLeftover)
        advanceTimeBy(2)

        assertEquals("生姜焼き", storedLeftover?.name)
    }

    @Test
    fun 開いたときの入力に戻すと残った編集を消す() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き")
        advanceTimeBy(1_001)

        viewModel.changeName("")
        advanceTimeBy(1_001)

        assertNull(storedLeftover)
    }

    @Test
    fun 保存できたら残った編集を消す() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き")
        advanceTimeBy(1_001)

        viewModel.save()

        assertNull(storedLeftover)
    }

    @Test
    fun 入力を捨てて閉じたら残った編集を消す() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き")
        advanceTimeBy(1_001)

        viewModel.requestClose()
        viewModel.discard()
        advanceTimeBy(1_001)

        assertNull(storedLeftover)
    }

    @Test
    fun 残った編集があれば示し反映すると入力がそれになる() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val viewModel = viewModel()
        assertEquals("", viewModel.editor.form.name)
        assertEquals("途中の生姜焼き", viewModel.editor.offeredLeftover?.name)

        viewModel.reflectLeftover()

        assertNull(viewModel.editor.offeredLeftover)
        val ingredient = viewModel.editor.form.ingredients.rows.single().ingredient
        assertEquals("途中の生姜焼き", viewModel.editor.form.name)
        assertEquals(ItemId("item-pork"), ingredient.selectedItemId)
        assertEquals("1/2", ingredient.quantity.text)
        assertTrue(viewModel.editor.hasUnsavedChanges)
    }

    @Test
    fun 一覧から続けると残った編集を反映して開く() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)

        val viewModel = viewModel(resumesLeftover = true)

        assertEquals("途中の生姜焼き", viewModel.editor.form.name)
        assertNull(viewModel.editor.offeredLeftover)
    }

    @Test
    fun 残った編集を選ばずに打つと打った分で書き替える() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val viewModel = viewModel()

        viewModel.changeName("親子丼")
        advanceTimeBy(1_001)

        assertEquals("親子丼", storedLeftover?.name)
        assertEquals("途中の生姜焼き", viewModel.editor.offeredLeftover?.name)
    }

    @Test
    fun 残った編集を選ばずに何も打たずに閉じたら残す() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val viewModel = viewModel()

        viewModel.requestClose()

        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
        assertEquals(leftover, storedLeftover)
    }

    @Test
    fun 示した残った編集を破棄すると消す() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val viewModel = viewModel()

        viewModel.discardLeftover()
        advanceTimeBy(1_001)

        assertNull(viewModel.editor.offeredLeftover)
        assertNull(storedLeftover)
    }

    @Test
    fun 残った編集を選ばずに打って元に戻すと残った編集に戻る() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val viewModel = viewModel()
        viewModel.changeName("親子丼")
        advanceTimeBy(1_001)

        viewModel.changeName("")
        advanceTimeBy(1_001)

        assertEquals(leftover, storedLeftover)
    }

    @Test
    fun 閉じたらまだ書いていない分を待たずに書く() = runTest {
        val viewModel = viewModel()
        viewModel.changeName("生姜焼き")

        viewModel.requestClose()
        viewModel.discard()

        assertNull(storedLeftover)
        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
    }

    @Test
    fun 残った編集を読み終えるまでは読み込み中() = runTest {
        val reading = CompletableDeferred<Unit>()
        val viewModel = viewModelReadingLeftoverAfter(reading)
        assertEquals(CreateRecipeUiState.Loading, viewModel.uiState.value)

        reading.complete(Unit)

        assertIs<CreateRecipeUiState.Editing>(viewModel.uiState.value)
    }

    @Test
    fun 保存できない入力で戻った後に同じ入力のまま押すともう一度保存する() = runTest {
        var calls = 0
        val viewModel = viewModel {
            calls++
            CreateRecipeResult.NameRequired
        }

        viewModel.save()
        viewModel.save()

        assertEquals(2, calls)
        assertEquals(SaveStatus.Idle, viewModel.editor.saveStatus)
    }

    @Test
    fun 読み込み中に閉じても残った編集には触れない() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val reading = CompletableDeferred<Unit>()
        val viewModel = viewModelReadingLeftoverAfter(reading)

        viewModel.requestClose()
        reading.complete(Unit)
        advanceTimeBy(1_001)

        assertEquals(CreateRecipeUiState.Closed, viewModel.uiState.value)
        assertEquals(leftover, storedLeftover)
    }

    @Test
    fun 残った編集を反映すると反映前に探した候補は出さない() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val viewModel = viewModel(searchItems = { SearchItemsResult.Found(persistentListOf(porkBelly)) })
        viewModel.changeIngredientName(viewModel.firstKey, "豚")
        assertEquals<List<Item>?>(listOf(porkBelly), viewModel.editor.shownFoundItems?.items)

        viewModel.reflectLeftover()

        assertNull(viewModel.editor.shownFoundItems)
    }

    @Test
    fun 候補を選ぶと走っている検索を取り消す() = runTest {
        val searching = CompletableDeferred<Unit>()
        var finished = false
        val viewModel = viewModel(searchItems = {
            searching.await()
            finished = true
            SearchItemsResult.Found(persistentListOf(pork))
        })
        viewModel.changeIngredientName(viewModel.firstKey, "豚")

        viewModel.selectItem(viewModel.firstKey, pork)
        searching.complete(Unit)

        assertEquals(false, finished)
    }

    @Test
    fun 開いただけでは残った編集を書かない() = runTest {
        leftoverEdits.save(LeftoverRecipeEditKey.NewRecipe, leftover)
        val writesBeforeOpening = leftoverEdits.writes

        viewModel()
        advanceTimeBy(1_001)

        assertEquals(writesBeforeOpening, leftoverEdits.writes)
    }

    @Test
    fun 書き込みの途中で保存できても残った編集を消す() = runTest {
        val saving = CompletableDeferred<CreateRecipeResult>()
        val viewModel = viewModel { saving.await() }
        viewModel.changeName("生姜焼き")
        viewModel.save()
        leftoverEdits.afterWrite = CompletableDeferred()
        advanceTimeBy(1_001)
        assertEquals("生姜焼き", storedLeftover?.name)

        saving.complete(CreateRecipeResult.Created(RecipeId("new"), persistentListOf()))

        assertNull(storedLeftover)
    }
}
