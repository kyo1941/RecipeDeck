package com.example.recipedeck.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.recipe.ShowRecipeListResult
import com.example.recipedeck.usecase.recipe.ShowRecipeListUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeListViewModel(
    showRecipeList: ShowRecipeListUseCase,
    private val leftoverEdits: LeftoverRecipeEditStore,
) : ViewModel() {
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RecipeListUiState> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest { showRecipeList() }
        .onEach { if (it is ShowRecipeListResult.Unexpected) logger.e(it.cause) { "Recipe の一覧を読み込めなかった" } }
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RecipeListUiState.Loading)

    val hasLeftoverNewRecipe: StateFlow<Boolean> = leftoverEdits.leftoverEdit(LeftoverRecipeEditKey.NewRecipe)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun discardLeftoverNewRecipe() {
        viewModelScope.launch { leftoverEdits.delete(LeftoverRecipeEditKey.NewRecipe) }
    }

    fun retry() {
        retryRequests.tryEmit(Unit)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val logger = Logger.withTag("RecipeListViewModel")
    }
}

sealed interface RecipeListUiState {
    data object Loading : RecipeListUiState
    data object NoRecipes : RecipeListUiState
    data class Shown(val items: ImmutableList<RecipeListItem>) : RecipeListUiState
    data object LoadFailed : RecipeListUiState
}

data class RecipeListItem(
    val id: RecipeId,
    val name: String,
    val isDraft: Boolean,
)

private fun ShowRecipeListResult.toUiState(): RecipeListUiState = when (this) {
    is ShowRecipeListResult.Shown -> if (recipes.isEmpty()) {
        RecipeListUiState.NoRecipes
    } else {
        RecipeListUiState.Shown(
            recipes
                .sortedWith(compareBy({ it.name.value }, { it.id.value }))
                .map { RecipeListItem(id = it.id, name = it.name.value, isDraft = it.status == RecipeStatus.DRAFT) }
                .toImmutableList(),
        )
    }
    is ShowRecipeListResult.Unexpected -> RecipeListUiState.LoadFailed
}
