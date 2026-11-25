package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.item.Item
import com.example.recipedeck.domain.recipe.RecipeStatus
import com.example.recipedeck.usecase.ingredient.IngredientInput
import com.example.recipedeck.usecase.item.SearchItemsResult
import com.example.recipedeck.usecase.recipe.CreateRecipeResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateRecipeViewModel(
    private val createRecipe: suspend (name: String, status: RecipeStatus, memo: String, ingredients: List<IngredientInput>) -> CreateRecipeResult,
    private val searchItems: suspend (keyword: String) -> SearchItemsResult,
    private val leftoverEditKeeper: LeftoverRecipeEditKeeper,
    resumesLeftover: Boolean,
) : ViewModel(), RecipeEditorActions {
    private val _uiState = MutableStateFlow<CreateRecipeUiState>(CreateRecipeUiState.Editing(RecipeEditorUiState.startingWith(RecipeForm.empty())))
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

    private var itemSearchJob: Job? = null

    init {
        viewModelScope.launch {
            leftoverEditKeeper.read()?.let { leftover ->
                _uiState.update { state ->
                    if (state !is CreateRecipeUiState.Editing) return@update state
                    val editor = state.editor
                    val resumes = resumesLeftover && editor.form.hasSameContentAs(editor.initialForm)
                    CreateRecipeUiState.Editing(if (resumes) editor.copy(form = leftover) else editor.copy(offeredLeftover = leftover))
                }
            }
            leftoverEditKeeper.keepSaving(viewModelScope, _uiState.mapNotNull { (it as? CreateRecipeUiState.Editing)?.editor })
        }
    }

    override fun changeName(name: String) = updateForm { it.copy(name = name) }

    override fun changeDraft(isDraft: Boolean) = updateForm { it.copy(isDraft = isDraft) }

    override fun changeMemo(memo: String) = updateForm { it.copy(memo = memo) }

    override fun changeIngredientName(key: Long, name: String) {
        updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withName(name) } }
        itemSearchJob?.cancel()
        itemSearchJob = viewModelScope.launch {
            val items = when (val result = searchItems(name)) {
                is SearchItemsResult.Found -> result.items
                is SearchItemsResult.Unexpected -> persistentListOf<Item>().also { logger.e(result.cause) { "材料の名前で Item を探せなかった" } }
            }
            _uiState.update { state ->
                if (state is CreateRecipeUiState.Editing) CreateRecipeUiState.Editing(state.editor.copy(foundItems = FoundItems(key, name, items))) else state
            }
        }
    }

    override fun selectItem(key: Long, item: Item) {
        itemSearchJob?.cancel()
        _uiState.update { state ->
            if (state is CreateRecipeUiState.Editing && state.editor.saveStatus != SaveStatus.Saving) {
                val form = state.editor.form.withIngredientChanged(key) { it.withItemSelected(item) }
                CreateRecipeUiState.Editing(state.editor.copy(form = form, foundItems = null))
            } else {
                state
            }
        }
    }

    override fun changeQuantity(key: Long, quantity: TextFieldValue) =
        updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withQuantity(quantity) } }

    override fun insertFractionSlash(key: Long) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withFractionSlash() } }

    override fun changeUnit(key: Long, unit: AmountUnit?) = updateForm { it.withIngredientChanged(key) { ingredient -> ingredient.withUnit(unit) } }

    override fun addIngredient() = updateForm { it.withIngredientAdded() }

    override fun removeIngredient(key: Long) = updateForm { it.withIngredientRemoved(key) }

    override fun reflectLeftover() {
        _uiState.update { state ->
            val editor = (state as? CreateRecipeUiState.Editing)?.editor
            val leftover = editor?.offeredLeftover
            if (leftover == null || editor.saveStatus == SaveStatus.Saving) state else CreateRecipeUiState.Editing(editor.copy(form = leftover, offeredLeftover = null))
        }
    }

    override fun discardLeftover() {
        val editor = (_uiState.value as? CreateRecipeUiState.Editing)?.editor ?: return
        _uiState.value = CreateRecipeUiState.Editing(editor.copy(offeredLeftover = null))
        if (!editor.hasUnsavedChanges) leftoverEditKeeper.delete(viewModelScope)
    }

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
            if (_uiState.value == CreateRecipeUiState.Closed) {
                leftoverEditKeeper.stopSaving()
                leftoverEditKeeper.delete(viewModelScope)
            }
        }
    }

    override fun requestClose() {
        val editor = (_uiState.value as? CreateRecipeUiState.Editing)?.editor ?: return
        if (editor.saveStatus == SaveStatus.Saving) return
        val requested = editor.copy(isCloseRequested = true)
        if (requested.showsDiscardConfirmation) {
            _uiState.value = CreateRecipeUiState.Editing(requested)
        } else {
            _uiState.value = CreateRecipeUiState.Closed
            leftoverEditKeeper.stopSaving()
            if (editor.offeredLeftover == null) leftoverEditKeeper.delete(viewModelScope)
        }
    }

    override fun discard() {
        _uiState.value = CreateRecipeUiState.Closed
        leftoverEditKeeper.stopSaving()
        leftoverEditKeeper.delete(viewModelScope)
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

fun interface CreateRecipeViewModelFactory {
    fun create(resumesLeftover: Boolean): CreateRecipeViewModel
}

sealed interface CreateRecipeUiState {
    data class Editing(val editor: RecipeEditorUiState) : CreateRecipeUiState
    data object Closed : CreateRecipeUiState
}
