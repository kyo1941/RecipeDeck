package com.example.recipedeck.app

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import co.touchlab.kermit.Logger
import com.example.recipedeck.ui.recipe.LeftoverRecipeEdit
import com.example.recipedeck.ui.recipe.LeftoverRecipeEditKey
import com.example.recipedeck.ui.recipe.LeftoverRecipeEditStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath

const val LEFTOVER_RECIPE_EDITS_FILE_NAME = "leftover_recipe_edits.json"

class DataStoreLeftoverRecipeEditStore(filePath: String) : LeftoverRecipeEditStore {
    private val dataStore: DataStore<LeftoverRecipeEdits> = DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, LeftoverRecipeEditsSerializer) { filePath.toPath() },
        corruptionHandler = ReplaceFileCorruptionHandler { cause ->
            logger.e(cause) { "残った編集のファイルが壊れていたので空にした" }
            LeftoverRecipeEdits()
        },
    )

    override fun leftoverEdit(key: LeftoverRecipeEditKey): Flow<LeftoverRecipeEdit?> = dataStore.data
        .catch { cause ->
            if (cause !is IOException) throw cause
            logger.e(cause) { "残った編集を読めなかった" }
            emit(LeftoverRecipeEdits())
        }
        .map { it.edits[key.storedName] }
        .distinctUntilChanged()

    override suspend fun save(key: LeftoverRecipeEditKey, edit: LeftoverRecipeEdit) =
        update { it.copy(edits = it.edits + (key.storedName to edit)) }

    override suspend fun delete(key: LeftoverRecipeEditKey) =
        update { it.copy(edits = it.edits - key.storedName) }

    private suspend fun update(transform: (LeftoverRecipeEdits) -> LeftoverRecipeEdits) {
        try {
            dataStore.updateData(transform)
        } catch (cause: IOException) {
            logger.e(cause) { "残った編集を書けなかった" }
        }
    }

    private companion object {
        val logger = Logger.withTag("DataStoreLeftoverRecipeEditStore")
    }
}

@Serializable
private data class LeftoverRecipeEdits(val edits: Map<String, LeftoverRecipeEdit> = emptyMap())

private val LeftoverRecipeEditKey.storedName: String
    get() = when (this) {
        LeftoverRecipeEditKey.NewRecipe -> "new-recipe"
        is LeftoverRecipeEditKey.OfRecipe -> "recipe/${recipeId.value}"
    }

private object LeftoverRecipeEditsSerializer : OkioSerializer<LeftoverRecipeEdits> {
    override val defaultValue = LeftoverRecipeEdits()

    override suspend fun readFrom(source: BufferedSource): LeftoverRecipeEdits = try {
        Json.decodeFromString(LeftoverRecipeEdits.serializer(), source.readUtf8())
    } catch (cause: SerializationException) {
        throw CorruptionException("残った編集のファイルを読み解けない", cause)
    }

    override suspend fun writeTo(t: LeftoverRecipeEdits, sink: BufferedSink) {
        sink.writeUtf8(Json.encodeToString(LeftoverRecipeEdits.serializer(), t))
    }
}
