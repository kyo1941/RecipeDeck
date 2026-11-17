package com.example.recipedeck.ui.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.shopping.SessionItemId
import com.example.recipedeck.domain.shopping.ShoppingListItem
import com.example.recipedeck.usecase.shopping.ShowShoppingListResult
import kotlinx.collections.immutable.persistentListOf
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
class ShoppingListViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun shoppingListItem(displayName: String) = ShoppingListItem(
        sessionItemId = SessionItemId("session-item-$displayName"),
        displayName = NotBlankString.of(displayName) ?: fail("表示名が空白になっている"),
        itemId = null,
        sources = persistentListOf(),
    )

    private fun TestScope.subscribe(viewModel: ShoppingListViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun 最初の結果が届くまでは読み込み中になる() = runTest {
        val results = MutableSharedFlow<ShowShoppingListResult>()
        val viewModel = ShoppingListViewModel(showShoppingList = { results })
        subscribe(viewModel)

        assertEquals(ShoppingListUiState.Loading, viewModel.uiState.value)

        results.emit(ShowShoppingListResult.NoActiveSession)

        assertEquals(ShoppingListUiState.NoActiveSession, viewModel.uiState.value)
    }

    @Test
    fun 進行中のSessionが無ければそのことを状態にする() = runTest {
        val viewModel = ShoppingListViewModel(showShoppingList = { flowOf(ShowShoppingListResult.NoActiveSession) })
        subscribe(viewModel)

        assertEquals(ShoppingListUiState.NoActiveSession, viewModel.uiState.value)
    }

    @Test
    fun 買い物項目の表示名を買い物リストの順に状態にする() = runTest {
        val items = persistentListOf(shoppingListItem("長ネギ"), shoppingListItem("豆腐"))
        val viewModel = ShoppingListViewModel(showShoppingList = { flowOf(ShowShoppingListResult.Shown(items)) })
        subscribe(viewModel)

        assertEquals(ShoppingListUiState.Shown(persistentListOf("長ネギ", "豆腐")), viewModel.uiState.value)
    }

    @Test
    fun 読み込みに失敗したらそのことを状態にする() = runTest {
        val viewModel = ShoppingListViewModel(
            showShoppingList = { flowOf(ShowShoppingListResult.Unexpected(IllegalStateException())) },
        )
        subscribe(viewModel)

        assertEquals(ShoppingListUiState.LoadFailed, viewModel.uiState.value)
    }

    @Test
    fun 結果が届き直すたびに状態を置き換える() = runTest {
        val results = MutableSharedFlow<ShowShoppingListResult>()
        val viewModel = ShoppingListViewModel(showShoppingList = { results })
        subscribe(viewModel)

        results.emit(ShowShoppingListResult.Shown(persistentListOf(shoppingListItem("長ネギ"))))
        assertEquals(ShoppingListUiState.Shown(persistentListOf("長ネギ")), viewModel.uiState.value)

        results.emit(ShowShoppingListResult.Unexpected(IllegalStateException()))
        assertEquals(ShoppingListUiState.LoadFailed, viewModel.uiState.value)

        results.emit(ShowShoppingListResult.Shown(persistentListOf(shoppingListItem("豆腐"))))
        assertEquals(ShoppingListUiState.Shown(persistentListOf("豆腐")), viewModel.uiState.value)
    }

    @Test
    fun 再試行すると買い物リストを読み直して新しい結果を状態にする() = runTest {
        var calls = 0
        val viewModel = ShoppingListViewModel(
            showShoppingList = {
                calls++
                if (calls == 1) {
                    flowOf(ShowShoppingListResult.Unexpected(IllegalStateException()))
                } else {
                    flowOf(ShowShoppingListResult.NoActiveSession)
                }
            },
        )
        subscribe(viewModel)
        assertEquals(ShoppingListUiState.LoadFailed, viewModel.uiState.value)

        viewModel.retry()

        assertEquals(ShoppingListUiState.NoActiveSession, viewModel.uiState.value)
        assertEquals(2, calls)
    }
}
