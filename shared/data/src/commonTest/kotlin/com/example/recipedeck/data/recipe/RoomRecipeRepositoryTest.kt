package com.example.recipedeck.data.recipe

import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.amount.AmountColumns
import com.example.recipedeck.data.item.RoomItemRepository
import com.example.recipedeck.data.observedValues
import com.example.recipedeck.data.runTestWithRecipeDeckDatabase
import com.example.recipedeck.data.writeSql
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.Recipe
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.recipe.RecipeIngredient
import com.example.recipedeck.domain.recipe.RecipeRepositoryFailure
import com.example.recipedeck.domain.recipe.RecipeStatus
import kotlinx.collections.immutable.ImmutableSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RoomRecipeRepositoryTest {
    private suspend fun saveItems(database: RecipeDeckDatabase) {
        val itemRepository = RoomItemRepository(database)
        listOf(onion, pork, salt).forEach { itemRepository.save(it) }
    }

    private fun Outcome<ImmutableSet<Recipe>, RecipeRepositoryFailure>.recipesOrFail(): Set<Recipe> =
        assertIs<Outcome.Success<ImmutableSet<Recipe>>>(this).value

    @Test
    fun 一覧の監視は今のRecipeを材料も含めて全件流す() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        val gingerPork = recipe(
            id = "recipe-1",
            name = "生姜焼き",
            ingredients = listOf(
                RecipeIngredient(pork.id, amountOrFail(200, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM)),
                RecipeIngredient(onion.id, null),
            ),
        )
        val salad = recipe(id = "recipe-2", name = "サラダ", status = RecipeStatus.DRAFT, ingredients = listOf(RecipeIngredient(onion.id, null)))
        val withoutIngredients = recipe(id = "recipe-3", name = "白ごはん")
        listOf(gingerPork, salad, withoutIngredients).forEach { repository.save(it) }

        assertEquals(setOf(gingerPork, salad, withoutIngredients), observedValues(repository.observeAll()).receive().recipesOrFail())
    }

    @Test
    fun 一覧の監視は材料を並び順で組み立てる() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val dao = database.recipeDao()
        dao.saveRecipe("recipe-1", "生姜焼き", "ready", null, null)
        val noAmount = AmountColumns(null, null, null, null)
        dao.insertIngredients(
            listOf(
                RecipeIngredientEntity("recipe-1", 1, onion.id.value, noAmount),
                RecipeIngredientEntity("recipe-1", 0, pork.id.value, noAmount),
            ),
        )

        val expected = recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(pork.id, null), RecipeIngredient(onion.id, null)))
        assertEquals(setOf(expected), observedValues(RoomRecipeRepository(database).observeAll()).receive().recipesOrFail())
    }

    @Test
    fun 一覧の監視は材料の表だけが変わっても流し直す() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        repository.save(recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(pork.id, amountOrFail(200, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM)))))
        val values = observedValues(repository.observeAll())
        values.receive()

        database.writeSql("UPDATE recipe_ingredient SET quantity_numerator = 300 WHERE recipe_id = 'recipe-1'")

        val expected = recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(pork.id, amountOrFail(300, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM))))
        assertEquals(setOf(expected), values.receive().recipesOrFail())
    }

    @Test
    fun 一覧の監視は読み戻しに失敗しても閉じず次の変更で流し直す() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomRecipeRepository(database)
        val saved = recipe(id = "recipe-1", name = "生姜焼き")
        repository.save(saved)
        val values = observedValues(repository.observeAll())
        values.receive()

        database.writeSql("UPDATE recipe SET name = ' ' WHERE id = 'recipe-1'")
        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(values.receive())

        database.writeSql("UPDATE recipe SET name = '生姜焼き' WHERE id = 'recipe-1'")
        assertEquals(setOf(saved), values.receive().recipesOrFail())
    }

    @Test
    fun データベースが閉じていると一覧の監視は失敗を流して終わる() = runTestWithRecipeDeckDatabase { database ->
        database.close()

        val values = observedValues(RoomRecipeRepository(database).observeAll())

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(values.receive())
        assertTrue(values.receiveCatching().isClosed)
    }

    @Test
    fun 保存したRecipeをIDで読める() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        val saved = recipe(
            id = "recipe-1",
            name = "生姜焼き",
            status = RecipeStatus.DRAFT,
            photoRef = "photo-1",
            memo = "強火で焼く",
            ingredients = listOf(
                RecipeIngredient(pork.id, amountOrFail(200, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM)),
                RecipeIngredient(onion.id, amountOrFail(1, 2, QuantityNotation.FRACTION, AmountUnit.PIECE)),
                RecipeIngredient(salt.id, null),
            ),
        )

        assertEquals(Outcome.Success(Unit), repository.save(saved))
        assertEquals(Outcome.Success(saved), repository.findById(RecipeId("recipe-1")))
    }

    @Test
    fun 無いIDを読むとnullを返す() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomRecipeRepository(database)

        assertEquals(Outcome.Success(null), repository.findById(RecipeId("recipe-1")))
    }

    @Test
    fun 単位のない分量と材料のないRecipeを読み戻せる() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        val withoutUnit = recipe(
            id = "recipe-1",
            ingredients = listOf(RecipeIngredient(onion.id, amountOrFail(3, 1, QuantityNotation.DECIMAL, null))),
        )
        val withoutIngredients = recipe(id = "recipe-2")

        repository.save(withoutUnit)
        repository.save(withoutIngredients)

        assertEquals(Outcome.Success(withoutUnit), repository.findById(RecipeId("recipe-1")))
        assertEquals(Outcome.Success(withoutIngredients), repository.findById(RecipeId("recipe-2")))
    }

    @Test
    fun 同じItemの材料を複数行持てる() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        val saved = recipe(
            id = "recipe-1",
            ingredients = listOf(
                RecipeIngredient(salt.id, amountOrFail(1, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM)),
                RecipeIngredient(pork.id, null),
                RecipeIngredient(salt.id, amountOrFail(2, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM)),
            ),
        )

        repository.save(saved)

        assertEquals(Outcome.Success(saved), repository.findById(RecipeId("recipe-1")))
    }

    @Test
    fun 同じIDで保存すると材料も含めて上書きする() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        repository.save(
            recipe(
                id = "recipe-1",
                name = "生姜焼き",
                memo = "強火で焼く",
                ingredients = listOf(RecipeIngredient(pork.id, null), RecipeIngredient(onion.id, null), RecipeIngredient(salt.id, null)),
            ),
        )
        val edited = recipe(
            id = "recipe-1",
            name = "豚の生姜焼き",
            memo = null,
            ingredients = listOf(RecipeIngredient(onion.id, null), RecipeIngredient(pork.id, null)),
        )

        assertEquals(Outcome.Success(Unit), repository.save(edited))
        assertEquals(Outcome.Success(edited), repository.findById(RecipeId("recipe-1")))
    }

    @Test
    fun 保存しても別のRecipeの材料は変わらない() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        val other = recipe(id = "recipe-2", ingredients = listOf(RecipeIngredient(salt.id, null)))
        repository.save(other)

        repository.save(recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(pork.id, null))))

        assertEquals(Outcome.Success(other), repository.findById(RecipeId("recipe-2")))
    }

    @Test
    fun 無いItemを指す材料があると失敗しRecipeも材料も残さない() = runTestWithRecipeDeckDatabase { database ->
        saveItems(database)
        val repository = RoomRecipeRepository(database)
        val before = recipe(id = "recipe-1", ingredients = listOf(RecipeIngredient(pork.id, null)))
        repository.save(before)

        val result = repository.save(
            recipe(id = "recipe-1", name = "豚の生姜焼き", ingredients = listOf(RecipeIngredient(ItemId("item-missing"), null))),
        )

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(result)
        assertEquals(Outcome.Success(before), repository.findById(RecipeId("recipe-1")))
    }

    @Test
    fun 読み戻した名前が空白だけなら失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        database.recipeDao().saveRecipe("recipe-1", " ", "ready", null, null)

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(RoomRecipeRepository(database).findById(RecipeId("recipe-1")))
    }

    @Test
    fun 読み戻した名前やメモの前後に空白があれば失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        val dao = database.recipeDao()
        dao.saveRecipe("recipe-1", "生姜焼き ", "ready", null, null)
        dao.saveRecipe("recipe-2", "生姜焼き", "ready", null, " 強火で焼く")
        val repository = RoomRecipeRepository(database)

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(repository.findById(RecipeId("recipe-1")))
        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(repository.findById(RecipeId("recipe-2")))
    }

    @Test
    fun 読み戻した状態が知らない値なら失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        database.recipeDao().saveRecipe("recipe-1", "生姜焼き", "archived", null, null)

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(RoomRecipeRepository(database).findById(RecipeId("recipe-1")))
    }

    @Test
    fun 読み戻した数量が正の数でなければ失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredIngredient(database, AmountColumns(0, 1, "decimal", "gram"))

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(RoomRecipeRepository(database).findById(RecipeId("recipe-1")))
    }

    @Test
    fun 読み戻した分量の列が一部だけ空なら失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredIngredient(database, AmountColumns(null, null, null, "gram"))

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(RoomRecipeRepository(database).findById(RecipeId("recipe-1")))
    }

    @Test
    fun 読み戻した単位が知らない値なら失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredIngredient(database, AmountColumns(1, 1, "decimal", "pack"))

        assertIs<Outcome.Failure<RecipeRepositoryFailure.Unexpected>>(RoomRecipeRepository(database).findById(RecipeId("recipe-1")))
    }

    private suspend fun saveStoredIngredient(database: RecipeDeckDatabase, amount: AmountColumns) {
        saveItems(database)
        val dao = database.recipeDao()
        dao.saveRecipe("recipe-1", "生姜焼き", "ready", null, null)
        dao.insertIngredients(listOf(RecipeIngredientEntity("recipe-1", 0, salt.id.value, amount)))
    }
}
