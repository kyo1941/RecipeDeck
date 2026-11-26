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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateRecipeViewModel(
    private val createRecipe: suspend (name: String, status: RecipeStatus, memo: String, ingredients: List<IngredientInput>) -> CreateRecipeResult,
    private val searchItems: suspend (keyword: String) -> SearchItemsResult,
    private val leftoverEditKeeper: LeftoverRecipeEditKeeper,
    private val resumesLeftover: Boolean,
) : ViewModel(), RecipeEditorActions {
    private val _uiState = MutableStateFlow<CreateRecipeUiState>(CreateRecipeUiState.Loading)
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

    private val editors = _uiState.mapNotNull { (it as? RecipeEditorStep.Editing)?.editor }

    init {
        viewModelScope.launch { start() }
        viewModelScope.launch { searchItemsWhenQueried() }
        viewModelScope.launch { createWhenSaveRequested() }
        leftoverEditKeeper.keepSaving(viewModelScope, _uiState.mapNotNull { it.leftoverWrite() })
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

    private suspend fun start() {
        val leftover = leftoverEditKeeper.read()
        val editor = RecipeEditorUiState.startingWith(RecipeForm.empty()).let {
            when {
                leftover == null -> it
                resumesLeftover -> it.copy(form = leftover)
                else -> it.copy(offeredLeftover = leftover)
            }
        }
        _uiState.update { if (it == CreateRecipeUiState.Loading) RecipeEditorStep.Editing(editor) else it }
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

    private suspend fun createWhenSaveRequested() {
        editors.map { it.formBeingSaved }.distinctUntilChanged().filterNotNull().collect { form ->
            val result = createRecipe(form.name, form.status, form.memo, form.ingredients.toInputs())
            if (result is CreateRecipeResult.Unexpected) logger.e(result.cause) { "Recipe を作成できなかった" }
            updateStep { editor ->
                when (result) {
                    is CreateRecipeResult.Created -> editor.saved()
                    CreateRecipeResult.NameRequired, is CreateRecipeResult.IngredientDisplayNameRequired -> editor.saveRejected()
                    is CreateRecipeResult.Unexpected -> editor.saveFailed()
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

    private fun CreateRecipeUiState.leftoverWrite(): LeftoverWrite? = when (this) {
        CreateRecipeUiState.Loading -> null
        is RecipeEditorStep -> leftoverWrite()
    }

    private companion object {
        val logger = Logger.withTag("CreateRecipeViewModel")
    }
}

fun interface CreateRecipeViewModelFactory {
    fun create(resumesLeftover: Boolean): CreateRecipeViewModel
}

sealed interface CreateRecipeUiState {
    data object Loading : CreateRecipeUiState
}
