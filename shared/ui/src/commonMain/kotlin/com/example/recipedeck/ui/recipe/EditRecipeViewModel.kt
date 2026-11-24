package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.recipe.EditRecipeResult
import com.example.recipedeck.usecase.recipe.ShowRecipeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditRecipeViewModel(
    private val showRecipe: () -> Flow<ShowRecipeResult>,
    private val editRecipe: suspend (name: String, status: RecipeStatus, memo: String, ingredients: List<IngredientInput>) -> EditRecipeResult,
) : ViewModel(), RecipeEditorActions {
    private val _uiState = MutableStateFlow<EditRecipeUiState>(EditRecipeUiState.Loading)
    val uiState: StateFlow<EditRecipeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    override fun changeName(name: String) = updateForm { it.copy(name = name) }

    override fun changeDraft(isDraft: Boolean) = updateForm { it.copy(isDraft = isDraft) }

    override fun changeMemo(memo: String) = updateForm { it.copy(memo = memo) }

    override fun changeIngredientName(key: Long, name: String) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withName(name) } }

    override fun changeQuantity(key: Long, quantity: TextFieldValue) =
        updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withQuantity(quantity) } }

    override fun insertFractionSlash(key: Long) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withFractionSlash() } }

    override fun changeUnit(key: Long, unit: AmountUnit?) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withUnit(unit) } }

    override fun addIngredient() = updateForm { it.withIngredientAdded() }

    override fun removeIngredient(key: Long) = updateForm { it.withIngredientRemoved(key) }

    override fun save() {
        val editor = (_uiState.value as? EditRecipeUiState.Editing)?.editor?.takeUnless { it.saveStatus == SaveStatus.Saving } ?: return
        val saving = editor.copy(isSaveRequested = true, saveStatus = SaveStatus.Saving)
        _uiState.value = EditRecipeUiState.Editing(saving)
        viewModelScope.launch {
            val form = saving.form
            val next = when (val result = editRecipe(form.name, form.status, form.memo, form.ingredients.toInputs())) {
                is EditRecipeResult.Saved -> EditRecipeUiState.Closed
                EditRecipeResult.NameRequired, is EditRecipeResult.IngredientDisplayNameRequired ->
                    EditRecipeUiState.Editing(saving.copy(saveStatus = SaveStatus.Idle))
                // TODO: Recipe の削除を作るときに、入力を新しいレシピとして保存するかを尋ねる動線を足す（spec の未確定事項）。今は入力が失われる。
                EditRecipeResult.RecipeNoLongerExists -> EditRecipeUiState.NoLongerExists
                is EditRecipeResult.Unexpected -> EditRecipeUiState.Editing(saving.copy(saveStatus = SaveStatus.Failed))
                    .also { logger.e(result.cause) { "Recipe を保存できなかった" } }
            }
            _uiState.update { current -> if ((current as? EditRecipeUiState.Editing)?.editor?.saveStatus == SaveStatus.Saving) next else current }
        }
    }

    override fun requestClose() {
        val editor = (_uiState.value as? EditRecipeUiState.Editing)?.editor
        when {
            editor == null -> _uiState.value = EditRecipeUiState.Closed
            editor.saveStatus == SaveStatus.Saving -> Unit
            editor.hasUnsavedChanges -> _uiState.value = EditRecipeUiState.Editing(editor.copy(isCloseRequested = true))
            else -> _uiState.value = EditRecipeUiState.Closed
        }
    }

    override fun discard() {
        _uiState.value = EditRecipeUiState.Closed
    }

    override fun keepEditing() {
        _uiState.update { state ->
            if (state is EditRecipeUiState.Editing) EditRecipeUiState.Editing(state.editor.copy(isCloseRequested = false)) else state
        }
    }

    private fun load() {
        _uiState.value = EditRecipeUiState.Loading
        viewModelScope.launch {
            val next = when (val result = showRecipe().first()) {
                is ShowRecipeResult.Shown -> EditRecipeUiState.Editing(RecipeEditorUiState.startingWith(RecipeForm.of(result)))
                ShowRecipeResult.RecipeNoLongerExists -> EditRecipeUiState.NoLongerExists
                is ShowRecipeResult.Unexpected -> EditRecipeUiState.LoadFailed
                    .also { logger.e(result.cause) { "編集する Recipe を読み込めなかった" } }
            }
            _uiState.update { current -> if (current == EditRecipeUiState.Loading) next else current }
        }
    }

    private fun updateForm(transform: (RecipeForm) -> RecipeForm) {
        _uiState.update { state ->
            if (state is EditRecipeUiState.Editing && state.editor.saveStatus != SaveStatus.Saving) {
                EditRecipeUiState.Editing(state.editor.copy(form = transform(state.editor.form)))
            } else {
                state
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
