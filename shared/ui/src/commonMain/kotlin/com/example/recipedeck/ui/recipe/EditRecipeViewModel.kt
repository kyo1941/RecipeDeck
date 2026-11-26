package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.item.SearchItemsResult
import com.example.recipedeck.usecase.recipe.EditRecipeResult
import com.example.recipedeck.usecase.recipe.ShowRecipeResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class EditRecipeViewModel(
    private val showRecipe: () -> Flow<ShowRecipeResult>,
    private val editRecipe: suspend (name: String, status: RecipeStatus, memo: String, ingredients: List<IngredientInput>) -> EditRecipeResult,
    private val searchItems: suspend (keyword: String) -> SearchItemsResult,
    private val leftoverEditKeeper: LeftoverRecipeEditKeeper,
) : ViewModel(), RecipeEditorActions {
    private val _uiState = MutableStateFlow<EditRecipeUiState>(EditRecipeUiState.Loading)
    val uiState: StateFlow<EditRecipeUiState> = _uiState.asStateFlow()

    private val editors = _uiState.mapNotNull { (it as? EditRecipeUiState.Editing)?.editor }

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch { searchItemsWhenQueried() }
        leftoverEditKeeper.keepSaving(viewModelScope, editors.map { it.leftoverToKeep })
    }

    fun retry() {
        if (_uiState.value != EditRecipeUiState.LoadFailed) return
        _uiState.value = EditRecipeUiState.Loading
        viewModelScope.launch { load() }
    }

    override fun changeName(name: String) = updateForm { it.copy(name = name) }

    override fun changeDraft(isDraft: Boolean) = updateForm { it.copy(isDraft = isDraft) }

    override fun changeMemo(memo: String) = updateForm { it.copy(memo = memo) }

    override fun changeIngredientName(key: Long, name: String) =
        updateEditor { it.withIngredientNameChanged(key, name) }

    override fun selectItem(key: Long, item: Item) = updateEditor { it.withItemSelected(key, item) }

    override fun changeQuantity(key: Long, quantity: TextFieldValue) =
        updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withQuantity(quantity) } }

    override fun insertFractionSlash(key: Long) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withFractionSlash() } }

    override fun changeUnit(key: Long, unit: AmountUnit?) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withUnit(unit) } }

    override fun addIngredient() = updateForm { it.withIngredientAdded() }

    override fun removeIngredient(key: Long) = updateForm { it.withIngredientRemoved(key) }

    override fun reflectLeftover() = updateEditor { it.withLeftoverReflected() }

    override fun discardLeftover() = updateEditor { it.copy(offeredLeftover = null) }

    override fun save() {
        val saving = currentEditor()?.saveRequested() ?: return
        _uiState.value = EditRecipeUiState.Editing(saving)
        viewModelScope.launch {
            val form = saving.form
            when (val result = editRecipe(form.name, form.status, form.memo, form.ingredients.toInputs())) {
                is EditRecipeResult.Saved -> applyOutcome { it.saved() }
                EditRecipeResult.NameRequired, is EditRecipeResult.IngredientDisplayNameRequired -> updateEditor { it.saveRejected() }
                // TODO: Recipe の削除を作るときに、入力を新しいレシピとして保存するかを尋ねる動線を足す（spec の未確定事項）。今は入力が失われる。
                EditRecipeResult.RecipeNoLongerExists -> if (currentEditor()?.saveStatus == SaveStatus.Saving) showNoLongerExists()
                is EditRecipeResult.Unexpected -> {
                    logger.e(result.cause) { "Recipe を保存できなかった" }
                    updateEditor { it.saveFailed() }
                }
            }
        }
    }

    override fun requestClose() {
        if (_uiState.value is EditRecipeUiState.Editing) applyOutcome { it.closeRequested() } else _uiState.value = EditRecipeUiState.Closed
    }

    override fun discard() = applyOutcome { it.discarded() }

    override fun keepEditing() = updateEditor { it.copy(isConfirmingDiscard = false) }

    private suspend fun load() {
        val next = when (val result = showRecipe().first()) {
            is ShowRecipeResult.Shown ->
                EditRecipeUiState.Editing(RecipeEditorUiState.startingWith(RecipeForm.of(result)).copy(offeredLeftover = leftoverEditKeeper.read()))
            ShowRecipeResult.RecipeNoLongerExists -> EditRecipeUiState.NoLongerExists
            is ShowRecipeResult.Unexpected -> EditRecipeUiState.LoadFailed
                .also { logger.e(result.cause) { "編集する Recipe を読み込めなかった" } }
        }
        if (_uiState.value != EditRecipeUiState.Loading) return
        if (next == EditRecipeUiState.NoLongerExists) showNoLongerExists() else _uiState.value = next
    }

    private fun showNoLongerExists() {
        _uiState.value = EditRecipeUiState.NoLongerExists
        leftoverEditKeeper.finish(viewModelScope, leftoverToKeep = null)
    }

    private suspend fun searchItemsWhenQueried() {
        editors.map { it.itemQuery }.distinctUntilChanged().filterNotNull().collectLatest { query ->
            val items = when (val result = searchItems(query.keyword)) {
                is SearchItemsResult.Found -> result.items
                is SearchItemsResult.Unexpected -> persistentListOf<Item>().also { logger.e(result.cause) { "材料の名前で Item を探せなかった" } }
            }
            updateEditor { it.copy(foundItems = FoundItems(query, items)) }
        }
    }

    private fun currentEditor(): RecipeEditorUiState? = (_uiState.value as? EditRecipeUiState.Editing)?.editor

    private fun updateForm(transform: (RecipeForm) -> RecipeForm) = updateEditor { it.withFormChanged(transform) }

    private fun updateEditor(transform: (RecipeEditorUiState) -> RecipeEditorUiState) {
        val editor = currentEditor() ?: return
        _uiState.value = EditRecipeUiState.Editing(transform(editor))
    }

    private fun applyOutcome(transform: (RecipeEditorUiState) -> RecipeEditorOutcome) {
        val editor = currentEditor() ?: return
        when (val outcome = transform(editor)) {
            is RecipeEditorOutcome.Continue -> _uiState.value = EditRecipeUiState.Editing(outcome.editor)
            is RecipeEditorOutcome.Finish -> {
                _uiState.value = EditRecipeUiState.Closed
                leftoverEditKeeper.finish(viewModelScope, outcome.leftoverToKeep)
            }
        }
    }

    private companion object {
        val logger = Logger.withTag("EditRecipeViewModel")
    }
}

fun interface EditRecipeViewModelFactory {
    fun create(recipeId: RecipeId): EditRecipeViewModel
}

sealed interface EditRecipeUiState {
    data object Loading : EditRecipeUiState
    data object LoadFailed : EditRecipeUiState
    data object NoLongerExists : EditRecipeUiState
    data class Editing(val editor: RecipeEditorUiState) : EditRecipeUiState
    data object Closed : EditRecipeUiState
}
