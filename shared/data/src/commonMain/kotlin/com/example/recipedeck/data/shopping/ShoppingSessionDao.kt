package com.example.recipedeck.data.shopping

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
internal interface ShoppingSessionDao {
    @Query(
        """
        INSERT INTO shopping_session (id, created_at_epoch_millis, completed_at_epoch_millis) VALUES (:id, :createdAtEpochMillis, :completedAtEpochMillis)
        ON CONFLICT(id) DO UPDATE SET
            created_at_epoch_millis = excluded.created_at_epoch_millis,
            completed_at_epoch_millis = excluded.completed_at_epoch_millis
        """,
    )
    suspend fun saveSession(id: String, createdAtEpochMillis: Long, completedAtEpochMillis: Long?)

    @Query("DELETE FROM session_recipe WHERE session_id = :sessionId")
    suspend fun deleteRecipes(sessionId: String)

    @Query("DELETE FROM session_item WHERE session_id = :sessionId")
    suspend fun deleteItems(sessionId: String)

    @Insert
    suspend fun insertRecipes(recipes: List<SessionRecipeEntity>)

    @Insert
    suspend fun insertItems(items: List<SessionItemEntity>)

    @Insert
    suspend fun insertIngredients(ingredients: List<SessionRecipeIngredientEntity>)

    @Query("SELECT * FROM shopping_session WHERE completed_at_epoch_millis IS NULL")
    suspend fun findActive(): ShoppingSessionEntity?

    @Query("SELECT * FROM session_recipe WHERE session_id = :sessionId ORDER BY position")
    suspend fun findRecipes(sessionId: String): List<SessionRecipeEntity>

    @Query("SELECT * FROM session_item WHERE session_id = :sessionId ORDER BY position")
    suspend fun findItems(sessionId: String): List<SessionItemEntity>

    @Query(
        """
        SELECT session_recipe_ingredient.* FROM session_recipe_ingredient
        JOIN session_recipe ON session_recipe.id = session_recipe_ingredient.session_recipe_id
        WHERE session_recipe.session_id = :sessionId
        ORDER BY session_recipe_ingredient.position
        """,
    )
    suspend fun findIngredients(sessionId: String): List<SessionRecipeIngredientEntity>
}
