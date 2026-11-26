package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.item.Item
import kotlinx.collections.immutable.ImmutableList

/**
 * 作成と編集の画面が、どちらの ViewModel にも同じ操作で呼びかけるための契約。
 */
interface RecipeEditorActions {
    fun changeName(name: String)
    fun changeDraft(isDraft: Boolean)
    fun changeMemo(memo: String)
    fun changeIngredientName(key: Long, name: String)
    fun selectItem(key: Long, item: Item)
    fun changeQuantity(key: Long, quantity: TextFieldValue)
    fun insertFractionSlash(key: Long)
    fun changeUnit(key: Long, unit: AmountUnit?)
    fun addIngredient()
    fun removeIngredient(key: Long)
    fun reflectLeftover()
    fun discardLeftover()
    fun save()
    fun requestClose()
    fun discard()
    fun keepEditing()
}

sealed interface RecipeEditorStep : CreateRecipeUiState, EditRecipeUiState {
    data class Editing(val editor: RecipeEditorUiState) : RecipeEditorStep

    data class Finished(val leftoverToKeep: RecipeForm?) : RecipeEditorStep

    fun leftoverWrite(): LeftoverWrite = when (this) {
        is Editing -> LeftoverWrite(editor.leftoverToKeep, isImmediate = false)
        is Finished -> LeftoverWrite(leftoverToKeep, isImmediate = true)
    }
}

data class RecipeEditorUiState(
    val form: RecipeForm,
    val initialForm: RecipeForm,
    val isSaveRequested: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val isCloseRequested: Boolean = false,
    val itemQuery: ItemQuery? = null,
    val foundItems: FoundItems? = null,
    val offeredLeftover: RecipeForm? = null,
) {
    val hasUnsavedChanges: Boolean get() = !form.hasSameContentAs(initialForm)

    val isNameRequired: Boolean get() = isSaveRequested && NotBlankString.of(form.name) == null

    val ingredientKeysRequiringName: Set<Long>
        get() = if (isSaveRequested) form.ingredients.rows.filter { it.ingredient.toInput().isInvalid }.map { it.key }.toSet() else emptySet()

    val showsDiscardConfirmation: Boolean get() = isCloseRequested && hasUnsavedChanges

    val shownFoundItems: FoundItems?
        get() = foundItems?.takeIf { found ->
            found.query == itemQuery &&
                saveStatus != SaveStatus.Saving &&
                form.ingredients.rows.any { it.key == found.query.ingredientKey }
        }

    val leftoverToKeep: RecipeForm? get() = if (hasUnsavedChanges) form else offeredLeftover

    val formBeingSaved: RecipeForm? get() = form.takeIf { saveStatus == SaveStatus.Saving }

    fun withFormChanged(transform: (RecipeForm) -> RecipeForm): RecipeEditorUiState =
        if (saveStatus == SaveStatus.Saving) this else copy(form = transform(form))

    fun withIngredientNameChanged(key: Long, name: String): RecipeEditorUiState =
        if (saveStatus == SaveStatus.Saving) {
            this
        } else {
            copy(form = form.withIngredientChanged(key) { it.withName(name) }, itemQuery = ItemQuery(key, name))
        }

    fun withItemSelected(key: Long, item: Item): RecipeEditorUiState =
        if (saveStatus == SaveStatus.Saving) this else copy(form = form.withIngredientChanged(key) { it.withItemSelected(item) }, itemQuery = null)

    fun withLeftoverReflected(): RecipeEditorUiState {
        val leftover = offeredLeftover
        return if (leftover == null || saveStatus == SaveStatus.Saving) this else copy(form = leftover, offeredLeftover = null)
    }

    fun saveRequested(): RecipeEditorUiState =
        if (saveStatus == SaveStatus.Saving) this else copy(isSaveRequested = true, saveStatus = SaveStatus.Saving)

    fun saveRejected(): RecipeEditorStep = afterSaving { copy(saveStatus = SaveStatus.Idle) }

    fun saveFailed(): RecipeEditorStep = afterSaving { copy(saveStatus = SaveStatus.Failed) }

    fun closeRequested(): RecipeEditorStep = when {
        saveStatus == SaveStatus.Saving -> RecipeEditorStep.Editing(this)
        hasUnsavedChanges -> RecipeEditorStep.Editing(copy(isCloseRequested = true))
        else -> RecipeEditorStep.Finished(leftoverToKeep = offeredLeftover)
    }

    fun discarded(): RecipeEditorStep = RecipeEditorStep.Finished(leftoverToKeep = null)

    fun saved(): RecipeEditorStep =
        if (saveStatus == SaveStatus.Saving) RecipeEditorStep.Finished(leftoverToKeep = null) else RecipeEditorStep.Editing(this)

    // 保存の結果は、保存を始めた後に遅れて届く。その間に状態が変わっていたら（保存中でなければ）当てない。
    private fun afterSaving(transform: RecipeEditorUiState.() -> RecipeEditorUiState): RecipeEditorStep =
        RecipeEditorStep.Editing(if (saveStatus == SaveStatus.Saving) transform() else this)

    companion object {
        fun startingWith(form: RecipeForm) = RecipeEditorUiState(form = form, initialForm = form)
    }
}

data class ItemQuery(val ingredientKey: Long, val keyword: String)

data class FoundItems(val query: ItemQuery, val items: ImmutableList<Item>)

sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Saving : SaveStatus
    data object Failed : SaveStatus
}
