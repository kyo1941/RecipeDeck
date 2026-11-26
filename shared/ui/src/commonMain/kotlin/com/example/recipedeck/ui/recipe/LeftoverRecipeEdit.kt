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
    private var stored: Stored? = null

    suspend fun read(): RecipeForm? = store.leftoverEdit(key).first()?.let(RecipeForm::of).also { stored = Stored(it) }

    @OptIn(FlowPreview::class)
    fun keepSaving(scope: CoroutineScope, leftovers: Flow<RecipeForm?>) {
        if (saveJob != null) return
        saveJob = scope.launch {
            leftovers
                .distinctUntilChanged { old, new -> old.hasSameContentAs(new) }
                .debounce(WRITE_DELAY)
                .collect { write(it) }
        }
    }

    fun finish(scope: CoroutineScope, leftoverToKeep: RecipeForm?) {
        saveJob?.cancel()
        // 終わるとすぐ ViewModel が片付けられ、scope が取り消される。
        // 書き損ねると、捨てたはずの入力が次に開いたときに出るので、取り消されても書き終える形にする。
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { write(leftoverToKeep) }
        }
    }

    private suspend fun write(leftover: RecipeForm?) {
        if (stored?.let { leftover.hasSameContentAs(it.leftover) } == true) return
        if (leftover == null) store.delete(key) else store.save(key, leftover.toLeftoverEdit())
        stored = Stored(leftover)
    }

    private fun RecipeForm?.hasSameContentAs(other: RecipeForm?): Boolean =
        if (this == null || other == null) this == other else hasSameContentAs(other)

    // 保存先にあると分かっている残った編集。まだ読んでも書いてもいなければ、分からないので null。
    private data class Stored(val leftover: RecipeForm?)

    private companion object {
        val WRITE_DELAY = 1.seconds
    }
}
