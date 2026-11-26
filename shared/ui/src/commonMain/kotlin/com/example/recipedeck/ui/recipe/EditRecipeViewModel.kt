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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditRecipeViewModel(
    private val showRecipe: () -> Flow<ShowRecipeResult>,
    private val editRecipe: suspend (name: String, status: RecipeStatus, memo: String, ingredients: List<IngredientInput>) -> EditRecipeResult,
    private val searchItems: suspend (keyword: String) -> SearchItemsResult,
    private val leftoverEditKeeper: LeftoverRecipeEditKeeper,
) : ViewModel(), RecipeEditorActions {
    private val _uiState = MutableStateFlow<EditRecipeUiState>(EditRecipeUiState.Loading)
    val uiState: StateFlow<EditRecipeUiState> = _uiState.asStateFlow()

    private val editors = _uiState.mapNotNull { (it as? RecipeEditorStep.Editing)?.editor }

    init {
        viewModelScope.launch { loadWhenLoading() }
        viewModelScope.launch { searchItemsWhenQueried() }
        viewModelScope.launch { editWhenSaveRequested() }
        leftoverEditKeeper.keepSaving(viewModelScope, _uiState.mapNotNull { it.leftoverWrite() })
    }

    fun retry() {
        _uiState.update { if (it == EditRecipeUiState.LoadFailed) EditRecipeUiState.Loading else it }
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

    override fun save() = updateEditor { it.saveRequested() }

    override fun requestClose() = updateStep { it.closeRequested() }

    override fun discard() = updateStep { it.discarded() }

    override fun keepEditing() = updateEditor { it.copy(isCloseRequested = false) }

    private suspend fun loadWhenLoading() {
        _uiState.map { it == EditRecipeUiState.Loading }.distinctUntilChanged().filter { it }.collect {
            val next = when (val result = showRecipe().first()) {
                is ShowRecipeResult.Shown ->
                    RecipeEditorStep.Editing(RecipeEditorUiState.startingWith(RecipeForm.of(result)).copy(offeredLeftover = leftoverEditKeeper.read()))
                ShowRecipeResult.RecipeNoLongerExists -> EditRecipeUiState.NoLongerExists
                is ShowRecipeResult.Unexpected -> EditRecipeUiState.LoadFailed
                    .also { logger.e(result.cause) { "編集する Recipe を読み込めなかった" } }
            }
            _uiState.update { if (it == EditRecipeUiState.Loading) next else it }
        }
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

    private suspend fun editWhenSaveRequested() {
        editors.map { it.formBeingSaved }.distinctUntilChanged().filterNotNull().collect { form ->
            val result = editRecipe(form.name, form.status, form.memo, form.ingredients.toInputs())
            if (result is EditRecipeResult.Unexpected) logger.e(result.cause) { "Recipe を保存できなかった" }
            _uiState.update { state ->
                val editor = (state as? RecipeEditorStep.Editing)?.editor ?: return@update state
                when (result) {
                    is EditRecipeResult.Saved -> editor.saved()
                    EditRecipeResult.NameRequired, is EditRecipeResult.IngredientDisplayNameRequired -> editor.saveRejected()
                    // TODO: Recipe の削除を作るときに、入力を新しいレシピとして保存するかを尋ねる動線を足す（spec の未確定事項）。今は入力が失われる。
                    EditRecipeResult.RecipeNoLongerExists ->
                        if (editor.saveStatus == SaveStatus.Saving) EditRecipeUiState.NoLongerExists else state
                    is EditRecipeResult.Unexpected -> editor.saveFailed()
                }
            }
        }
    }

    private fun updateForm(transform: (RecipeForm) -> RecipeForm) = updateEditor { it.withFormChanged(transform) }

    private fun updateEditor(transform: (RecipeEditorUiState) -> RecipeEditorUiState) =
        updateStep { RecipeEditorStep.Editing(transform(it)) }

    private fun updateStep(transform: (RecipeEditorUiState) -> RecipeEditorStep) {
        _uiState.update { state -> if (state is RecipeEditorStep.Editing) transform(state.editor) else state }
    }

    private fun EditRecipeUiState.leftoverWrite(): LeftoverWrite? = when (this) {
        EditRecipeUiState.Loading, EditRecipeUiState.LoadFailed -> null
        EditRecipeUiState.NoLongerExists -> LeftoverWrite(leftover = null, isImmediate = true)
        is RecipeEditorStep -> leftoverWrite()
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
}
