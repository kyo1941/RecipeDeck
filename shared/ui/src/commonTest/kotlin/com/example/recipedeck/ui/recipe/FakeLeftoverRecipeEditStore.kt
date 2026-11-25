package com.example.recipedeck.ui.recipe

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLeftoverRecipeEditStore(vararg initial: Pair<LeftoverRecipeEditKey, LeftoverRecipeEdit>) : LeftoverRecipeEditStore {
    val edits = MutableStateFlow(mapOf(*initial))

    override fun leftoverEdit(key: LeftoverRecipeEditKey): Flow<LeftoverRecipeEdit?> = edits.map { it[key] }

    override suspend fun save(key: LeftoverRecipeEditKey, edit: LeftoverRecipeEdit) = edits.update { it + (key to edit) }

    override suspend fun delete(key: LeftoverRecipeEditKey) = edits.update { it - key }
}
