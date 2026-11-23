package com.example.recipedeck.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.recipe.ShowRecipeResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class RecipeViewModel(
    showRecipe: () -> Flow<ShowRecipeResult>,
) : ViewModel() {
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RecipeUiState> = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest { showRecipe() }
        .onEach { if (it is ShowRecipeResult.Unexpected) logger.e(it.cause) { "Recipe を読み込めなかった" } }
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RecipeUiState.Loading)

    fun retry() {
        retryRequests.tryEmit(Unit)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val logger = Logger.withTag("RecipeViewModel")
    }
}

fun interface RecipeViewModelFactory {
    fun create(recipeId: RecipeId): RecipeViewModel
}

sealed interface RecipeUiState {
    data object Loading : RecipeUiState
    data class Shown(
        val name: String,
        val isDraft: Boolean,
        val ingredients: ImmutableList<RecipeIngredientItem>,
        val memo: String?,
    ) : RecipeUiState
    data object NoLongerExists : RecipeUiState
    data object LoadFailed : RecipeUiState
}

data class RecipeIngredientItem(
    val name: String,
    val amount: Amount?,
)

private fun ShowRecipeResult.toUiState(): RecipeUiState = when (this) {
    is ShowRecipeResult.Shown -> RecipeUiState.Shown(
        name = recipe.name.value,
        isDraft = recipe.status == RecipeStatus.DRAFT,
        ingredients = ingredients.map { RecipeIngredientItem(it.item.displayName.value, it.amount) }.toImmutableList(),
        memo = recipe.memo?.value,
    )
    ShowRecipeResult.RecipeNoLongerExists -> RecipeUiState.NoLongerExists
    is ShowRecipeResult.Unexpected -> RecipeUiState.LoadFailed
}
