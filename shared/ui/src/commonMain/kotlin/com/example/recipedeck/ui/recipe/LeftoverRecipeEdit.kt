package com.example.recipedeck.ui.recipe

import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.recipe.RecipeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class LeftoverRecipeEdit(
    val name: String,
    val isDraft: Boolean,
    val memo: String,
    val ingredients: List<Ingredient>,
) {
    @Serializable
    data class Ingredient(
        val name: String,
        val selectedItemId: String?,
        val quantity: String,
        val unit: AmountUnit?,
    )
}

sealed interface LeftoverRecipeEditKey {
    data object NewRecipe : LeftoverRecipeEditKey
    data class OfRecipe(val recipeId: RecipeId) : LeftoverRecipeEditKey
}

/**
 * 読み書きに失敗しても、編集と保存は続けられる。失敗はログに残し、残った編集がないものとして扱う。
 */
interface LeftoverRecipeEditStore {
    fun leftoverEdit(key: LeftoverRecipeEditKey): Flow<LeftoverRecipeEdit?>
    suspend fun save(key: LeftoverRecipeEditKey, edit: LeftoverRecipeEdit)
    suspend fun delete(key: LeftoverRecipeEditKey)
}

class LeftoverRecipeEditKeeper(
    private val store: LeftoverRecipeEditStore,
    private val key: LeftoverRecipeEditKey,
) {
    private var saveJob: Job? = null

    suspend fun read(): RecipeForm? = store.leftoverEdit(key).first()?.let(RecipeForm::of)

    @OptIn(FlowPreview::class)
    fun keepSaving(scope: CoroutineScope, editors: Flow<RecipeEditorUiState>) {
        if (saveJob != null) return
        saveJob = scope.launch {
            editors
                .distinctUntilChanged { old, new -> old.form.hasSameContentAs(new.form) }
                .drop(1)
                .debounce(WRITE_DELAY)
                .collect { editor ->
                    if (editor.hasUnsavedChanges) store.save(key, editor.form.toLeftoverEdit()) else store.delete(key)
                }
        }
    }

    fun stopSaving() {
        saveJob?.cancel()
    }

    // 閉じるとすぐ ViewModel が片付けられ、scope が取り消される。
    // 消し損ねると、捨てたはずの入力が次に開いたときに出るので、取り消されても消し終える形にする。
    fun delete(scope: CoroutineScope) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { store.delete(key) }
        }
    }

    private companion object {
        val WRITE_DELAY = 1.seconds
    }
}
