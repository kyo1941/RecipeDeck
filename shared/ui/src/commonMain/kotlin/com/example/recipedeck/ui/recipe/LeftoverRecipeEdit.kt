package com.example.recipedeck.ui.recipe

import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.recipe.RecipeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Duration
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

/**
 * 保存先に置くべき残った編集。null なら消す。
 * 閉じたときなど、待たずに書くものは isImmediate にする。
 */
data class LeftoverWrite(val leftover: RecipeForm?, val isImmediate: Boolean)

class LeftoverRecipeEditKeeper(
    private val store: LeftoverRecipeEditStore,
    private val key: LeftoverRecipeEditKey,
) {
    suspend fun read(): RecipeForm? = store.leftoverEdit(key).first()?.let(RecipeForm::of)

    @OptIn(FlowPreview::class)
    fun keepSaving(scope: CoroutineScope, writes: Flow<LeftoverWrite>) {
        scope.launch {
            writes
                .distinctUntilChanged { old, new -> old.isImmediate == new.isImmediate && old.leftover.hasSameContentAs(new.leftover) }
                .debounce { if (it.isImmediate) Duration.ZERO else WRITE_DELAY }
                .collect { write ->
                    // 閉じるとすぐ ViewModel が片付けられ、scope が取り消される。
                    // 書き損ねると、捨てたはずの入力が次に開いたときに出るので、取り消されても書き終える形にする。
                    withContext(NonCancellable) {
                        val leftover = write.leftover
                        if (leftover == null) store.delete(key) else store.save(key, leftover.toLeftoverEdit())
                    }
                }
        }
    }

    private fun RecipeForm?.hasSameContentAs(other: RecipeForm?): Boolean =
        if (this == null || other == null) this == other else hasSameContentAs(other)

    private companion object {
        val WRITE_DELAY = 1.seconds
    }
}
