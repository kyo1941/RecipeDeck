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
 * 読み書きに失敗しても、編集と保存は続けられる。失敗はログに残し、読めなければ残った編集がないものとして扱う。
 * 読めなかったことは使う側に伝わらないので、そのときは「残った編集がない」と分かっているものとして扱われる。
 * その場合、終わったときの削除が飛ぶことがある。
 * 書けたかどうかは返す。書けなかった後は保存先の中身が分からないので、使う側は次の書き込みを飛ばさずに行う。
 */
interface LeftoverRecipeEditStore {
    fun leftoverEdit(key: LeftoverRecipeEditKey): Flow<LeftoverRecipeEdit?>
    suspend fun save(key: LeftoverRecipeEditKey, edit: LeftoverRecipeEdit): Boolean
    suspend fun delete(key: LeftoverRecipeEditKey): Boolean
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
        // 書いている途中で取り消されると、保存先に書けたかどうかが分からない。
        // 終わったときの書き込みを「同じ値」と見て飛ばさないよう、書き終えるまでは分からないものとして扱う。
        stored = null
        val isWritten = if (leftover == null) store.delete(key) else store.save(key, leftover.toLeftoverEdit())
        if (isWritten) stored = Stored(leftover)
    }

    private fun RecipeForm?.hasSameContentAs(other: RecipeForm?): Boolean =
        if (this == null || other == null) this == other else hasSameContentAs(other)

    // 保存先にあると分かっている残った編集。まだ読んでも書いてもいなければ、分からないので null。
    private data class Stored(val leftover: RecipeForm?)

    private companion object {
        val WRITE_DELAY = 1.seconds
    }
}
