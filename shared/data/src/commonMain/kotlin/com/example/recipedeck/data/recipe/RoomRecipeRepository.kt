package com.example.recipedeck.data.recipe

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.example.recipedeck.data.InvalidStoredRecordException
import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.outcomeOf
import com.example.recipedeck.data.storedNotBlankStringOf
import com.example.recipedeck.data.amount.toAmountColumns
import com.example.recipedeck.data.amount.toAmountOrNull
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeIngredient
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.toImmutableList

class RoomRecipeRepository(private val database: RecipeDeckDatabase) : RecipeRepository {
    private val dao = database.recipeDao()

    override suspend fun save(recipe: Recipe): Outcome<Unit, RecipeRepositoryFailure> =
        outcomeOf(RecipeRepositoryFailure::Unexpected) {
            database.withWriteTransaction {
                dao.saveRecipe(
                    id = recipe.id.value,
                    name = recipe.name.value,
                    status = recipe.status.storedValue,
                    photoRef = recipe.photoRef,
                    memo = recipe.memo?.value,
                )
                dao.deleteIngredients(recipe.id.value)
                dao.insertIngredients(
                    recipe.ingredients.mapIndexed { position, ingredient ->
                        RecipeIngredientEntity(
                            recipeId = recipe.id.value,
                            position = position,
                            itemId = ingredient.itemId.value,
                            amount = ingredient.amount.toAmountColumns(),
                        )
                    },
                )
            }
        }

    override suspend fun findById(id: RecipeId): Outcome<Recipe?, RecipeRepositoryFailure> =
        outcomeOf(RecipeRepositoryFailure::Unexpected) {
            database.withReadTransaction {
                dao.findById(id.value)?.toRecipe(dao.findIngredients(id.value))
            }
        }
}

private val RecipeStatus.storedValue: String
    get() = when (this) {
        RecipeStatus.DRAFT -> "draft"
        RecipeStatus.READY -> "ready"
    }

private fun RecipeEntity.toRecipe(ingredients: List<RecipeIngredientEntity>): Recipe = Recipe(
    id = RecipeId(id),
    name = storedNotBlankStringOf(name) { "Recipe $id の名前" },
    status = RecipeStatus.entries.firstOrNull { it.storedValue == status }
        ?: throw InvalidStoredRecordException("Recipe $id の状態が知らない値になっている: $status"),
    photoRef = photoRef,
    memo = memo?.let { storedNotBlankStringOf(it) { "Recipe $id のメモ" } },
    ingredients = ingredients.map { it.toRecipeIngredient() }.toImmutableList(),
)

private fun RecipeIngredientEntity.toRecipeIngredient(): RecipeIngredient = RecipeIngredient(
    itemId = ItemId(itemId),
    amount = amount.toAmountOrNull(),
)
