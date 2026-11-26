package com.example.recipedeck.ui.recipe

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLeftoverRecipeEditStore(vararg initial: Pair<LeftoverRecipeEditKey, LeftoverRecipeEdit>) : LeftoverRecipeEditStore {
    val edits = MutableStateFlow(mapOf(*initial))
    var writes = 0
        private set

    var afterWrite: CompletableDeferred<Unit>? = null
    var failsWrites = false

    override fun leftoverEdit(key: LeftoverRecipeEditKey): Flow<LeftoverRecipeEdit?> = edits.map { it[key] }

    override suspend fun save(key: LeftoverRecipeEditKey, edit: LeftoverRecipeEdit): Boolean {
        writes++
        if (failsWrites) return false
        edits.update { it + (key to edit) }
        afterWrite?.await()
        return true
    }

    override suspend fun delete(key: LeftoverRecipeEditKey): Boolean {
        writes++
        if (failsWrites) return false
        edits.update { it - key }
        return true
    }
}
