package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.recipe.CreateRecipeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateRecipeViewModel(
    private val createRecipe: suspend (name: String, status: RecipeStatus, memo: String, ingredients: List<IngredientInput>) -> CreateRecipeResult,
) : ViewModel(), RecipeEditorActions {
    private val _uiState = MutableStateFlow<CreateRecipeUiState>(CreateRecipeUiState.Editing(RecipeEditorUiState.startingWith(RecipeForm.empty())))
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

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
        val editor = (_uiState.value as? CreateRecipeUiState.Editing)?.editor?.takeUnless { it.saveStatus == SaveStatus.Saving } ?: return
        val saving = editor.copy(isSaveRequested = true, saveStatus = SaveStatus.Saving)
        _uiState.value = CreateRecipeUiState.Editing(saving)
        viewModelScope.launch {
            val form = saving.form
            val next = when (val result = createRecipe(form.name, form.status, form.memo, form.ingredients.toInputs())) {
                is CreateRecipeResult.Created -> CreateRecipeUiState.Closed
                CreateRecipeResult.NameRequired, is CreateRecipeResult.IngredientDisplayNameRequired ->
                    CreateRecipeUiState.Editing(saving.copy(saveStatus = SaveStatus.Idle))
                is CreateRecipeResult.Unexpected -> CreateRecipeUiState.Editing(saving.copy(saveStatus = SaveStatus.Failed))
                    .also { logger.e(result.cause) { "Recipe を作成できなかった" } }
            }
            _uiState.update { current -> if ((current as? CreateRecipeUiState.Editing)?.editor?.saveStatus == SaveStatus.Saving) next else current }
        }
    }

    override fun requestClose() {
        val editor = (_uiState.value as? CreateRecipeUiState.Editing)?.editor ?: return
        when {
            editor.saveStatus == SaveStatus.Saving -> Unit
            editor.hasUnsavedChanges -> _uiState.value = CreateRecipeUiState.Editing(editor.copy(isCloseRequested = true))
            else -> _uiState.value = CreateRecipeUiState.Closed
        }
    }

    override fun discard() {
        _uiState.value = CreateRecipeUiState.Closed
    }

    override fun keepEditing() {
        _uiState.update { state ->
            if (state is CreateRecipeUiState.Editing) CreateRecipeUiState.Editing(state.editor.copy(isCloseRequested = false)) else state
        }
    }

    private fun updateForm(transform: (RecipeForm) -> RecipeForm) {
        _uiState.update { state ->
            if (state is CreateRecipeUiState.Editing && state.editor.saveStatus != SaveStatus.Saving) {
                CreateRecipeUiState.Editing(state.editor.copy(form = transform(state.editor.form)))
            } else {
                state
            }
        }
    }

    private companion object {
        val logger = Logger.withTag("CreateRecipeViewModel")
    }
}

sealed interface CreateRecipeUiState {
    data class Editing(val editor: RecipeEditorUiState) : CreateRecipeUiState
    data object Closed : CreateRecipeUiState
}
