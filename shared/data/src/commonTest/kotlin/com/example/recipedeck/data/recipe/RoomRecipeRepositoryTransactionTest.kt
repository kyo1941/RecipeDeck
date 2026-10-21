package com.example.recipedeck.data.recipe

import com.example.recipedeck.data.RoomTransactionRunner
import com.example.recipedeck.data.item.RoomItemRepository
import com.example.recipedeck.data.runTestWithRecipeDeckFileDatabase
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomRecipeRepositoryTransactionTest {
    @Test
    fun 同じトランザクションで作ったItemを指すRecipeを保存し読み戻せる() = runTestWithRecipeDeckFileDatabase { database ->
        val itemRepository = RoomItemRepository(database)
        val recipeRepository = RoomRecipeRepository(database)
        val saved = recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(onion.id, null)))

        val result = RoomTransactionRunner(database).run {
            itemRepository.save(onion)
            recipeRepository.save(saved)
            recipeRepository.findById(saved.id)
        }

        assertEquals(Outcome.Success(Outcome.Success(saved)), result)
        assertEquals(Outcome.Success(saved), recipeRepository.findById(saved.id))
    }

    @Test
    fun 取り消すとItemもRecipeも材料も残らない() = runTestWithRecipeDeckFileDatabase { database ->
        val itemRepository = RoomItemRepository(database)
        val recipeRepository = RoomRecipeRepository(database)

        RoomTransactionRunner(database).run {
            itemRepository.save(onion)
            recipeRepository.save(recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(onion.id, null))))
            rollback(Unit)
        }

        assertEquals(Outcome.Success(null), itemRepository.findById(onion.id))
        assertEquals(Outcome.Success(null), recipeRepository.findById(RecipeId("recipe-1")))
        assertEquals(emptyList(), database.recipeDao().findIngredients("recipe-1"))
    }
}
