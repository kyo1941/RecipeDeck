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

sealed interface RecipeEditorOutcome {
    data class Continue(val editor: RecipeEditorUiState) : RecipeEditorOutcome

    data class Finish(val leftoverToKeep: RecipeForm?) : RecipeEditorOutcome
}

data class RecipeEditorUiState(
    val form: RecipeForm,
    private val initialForm: RecipeForm,
    private val isSaveRequested: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val isConfirmingDiscard: Boolean = false,
    val itemQuery: ItemQuery? = null,
    private val foundItems: FoundItems? = null,
    val offeredLeftover: RecipeForm? = null,
) {
    val hasUnsavedChanges: Boolean get() = !form.hasSameContentAs(initialForm)

    val isNameRequired: Boolean get() = isSaveRequested && NotBlankString.of(form.name) == null

    val ingredientKeysRequiringName: Set<Long>
        get() = if (isSaveRequested) form.ingredients.rows.filter { it.ingredient.toInput().isInvalid }.map { it.key }.toSet() else emptySet()

    val shownFoundItems: FoundItems?
        get() = foundItems?.takeIf { found ->
            found.query == itemQuery &&
                saveStatus != SaveStatus.Saving &&
                form.ingredients.rows.any { it.key == found.query.ingredientKey && it.ingredient.name == found.query.keyword }
        }

    val leftoverToKeep: RecipeForm? get() = if (hasUnsavedChanges) form else offeredLeftover

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
        return if (leftover == null || saveStatus == SaveStatus.Saving) this else copy(form = leftover, offeredLeftover = null, itemQuery = null)
    }

    fun saveRequested(): RecipeEditorUiState? =
        if (saveStatus == SaveStatus.Saving) null else copy(isSaveRequested = true, saveStatus = SaveStatus.Saving)

    fun saveRejected(): RecipeEditorUiState = afterSaving { copy(saveStatus = SaveStatus.Idle) }

    fun saveFailed(): RecipeEditorUiState = afterSaving { copy(saveStatus = SaveStatus.Failed) }

    fun closeRequested(): RecipeEditorOutcome = when {
        saveStatus == SaveStatus.Saving -> RecipeEditorOutcome.Continue(this)
        hasUnsavedChanges -> RecipeEditorOutcome.Continue(copy(isConfirmingDiscard = true))
        else -> RecipeEditorOutcome.Finish(leftoverToKeep = offeredLeftover)
    }

    fun discarded(): RecipeEditorOutcome = RecipeEditorOutcome.Finish(leftoverToKeep = null)

    fun saved(): RecipeEditorOutcome =
        if (saveStatus == SaveStatus.Saving) RecipeEditorOutcome.Finish(leftoverToKeep = null) else RecipeEditorOutcome.Continue(this)

    // 保存の結果は、保存を始めた後に遅れて届く。その間に状態が変わっていたら（保存中でなければ）当てない。
    private fun afterSaving(transform: RecipeEditorUiState.() -> RecipeEditorUiState): RecipeEditorUiState =
        if (saveStatus == SaveStatus.Saving) transform() else this

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
