package com.example.recipedeck.ui.shopping

import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.shopping.SessionItemId
import com.example.recipedeck.domain.shopping.ShoppingListItem
import com.example.recipedeck.usecase.shopping.ShowShoppingListResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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

    @Test
    fun 読み込みが終わるまでは読み込み中になる() {
        val result = CompletableDeferred<ShowShoppingListResult>()

        val viewModel = ShoppingListViewModel(showShoppingList = { result.await() })

        assertEquals(ShoppingListUiState.Loading, viewModel.uiState.value)

        result.complete(ShowShoppingListResult.NoActiveSession)

        assertEquals(ShoppingListUiState.NoActiveSession, viewModel.uiState.value)
    }

    @Test
    fun 進行中のSessionが無ければそのことを状態にする() {
        val viewModel = ShoppingListViewModel(showShoppingList = { ShowShoppingListResult.NoActiveSession })

        assertEquals(ShoppingListUiState.NoActiveSession, viewModel.uiState.value)
    }

    @Test
    fun 買い物項目の表示名を買い物リストの順に状態にする() {
        val items = persistentListOf(shoppingListItem("長ネギ"), shoppingListItem("豆腐"))

        val viewModel = ShoppingListViewModel(showShoppingList = { ShowShoppingListResult.Shown(items) })

        assertEquals(ShoppingListUiState.Shown(persistentListOf("長ネギ", "豆腐")), viewModel.uiState.value)
    }

    @Test
    fun 読み込みに失敗したらそのことを状態にする() {
        val viewModel = ShoppingListViewModel(
            showShoppingList = { ShowShoppingListResult.Unexpected(IllegalStateException()) },
        )

        assertEquals(ShoppingListUiState.LoadFailed, viewModel.uiState.value)
    }
}
