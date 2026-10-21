package com.example.recipedeck.data.recipe

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
internal interface RecipeDao {
    @Query(
        """
        INSERT INTO recipe (id, name, status, photo_ref, memo) VALUES (:id, :name, :status, :photoRef, :memo)
        ON CONFLICT(id) DO UPDATE SET
            name = excluded.name,
            status = excluded.status,
            photo_ref = excluded.photo_ref,
            memo = excluded.memo
        """,
    )
    suspend fun saveRecipe(id: String, name: String, status: String, photoRef: String?, memo: String?)

    @Query("DELETE FROM recipe_ingredient WHERE recipe_id = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Query("SELECT * FROM recipe WHERE id = :id")
    suspend fun findById(id: String): RecipeEntity?

    @Query("SELECT * FROM recipe_ingredient WHERE recipe_id = :recipeId ORDER BY position")
    suspend fun findIngredients(recipeId: String): List<RecipeIngredientEntity>
}
