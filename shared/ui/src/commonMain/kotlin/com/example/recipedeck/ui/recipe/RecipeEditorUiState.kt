package com.example.recipedeck.ui.recipe

import androidx.compose.ui.text.input.TextFieldValue
import com.example.recipedeck.domain.NotBlankString
import com.example.recipedeck.domain.amount.AmountUnit

/**
 * 作成と編集の画面が、どちらの ViewModel にも同じ操作で呼びかけるための契約。
 */
interface RecipeEditorActions {
    fun changeName(name: String)
    fun changeDraft(isDraft: Boolean)
    fun changeMemo(memo: String)
    fun changeIngredientName(key: Long, name: String)
    fun changeQuantity(key: Long, quantity: TextFieldValue)
    fun insertFractionSlash(key: Long)
    fun changeUnit(key: Long, unit: AmountUnit?)
    fun addIngredient()
    fun removeIngredient(key: Long)
    fun save()
    fun requestClose()
    fun discard()
    fun keepEditing()
}

data class RecipeEditorUiState(
    val form: RecipeForm,
    val initialForm: RecipeForm,
    val isSaveRequested: Boolean = false,
    val isCloseRequested: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.Idle,
) {
    val hasUnsavedChanges: Boolean get() = !form.hasSameContentAs(initialForm)

    val isNameRequired: Boolean get() = isSaveRequested && NotBlankString.of(form.name) == null

    val ingredientKeysRequiringName: Set<Long>
        get() = if (isSaveRequested) form.ingredients.rows.filter { it.ingredient.toInput().isInvalid }.map { it.key }.toSet() else emptySet()

    val showsDiscardConfirmation: Boolean get() = isCloseRequested && hasUnsavedChanges

    companion object {
        fun startingWith(form: RecipeForm) = RecipeEditorUiState(form = form, initialForm = form)
    }
}

sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Saving : SaveStatus
    data object Failed : SaveStatus
}
