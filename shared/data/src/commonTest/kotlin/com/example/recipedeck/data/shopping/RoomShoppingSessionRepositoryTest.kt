package com.example.recipedeck.data.shopping

import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.amount.AmountColumns
import com.example.recipedeck.data.notBlankStringOrFail
import com.example.recipedeck.data.recipe.amountOrFail
import com.example.recipedeck.data.runTestWithRecipeDeckDatabase
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.StoredAggregateRestoration
import com.example.recipedeck.domain.amount.Amount
import com.example.recipedeck.domain.amount.AmountUnit
import com.example.recipedeck.domain.amount.Multiplier
import com.example.recipedeck.domain.amount.QuantityNotation
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.shopping.AddedRecipeIngredient
import com.example.recipedeck.domain.shopping.ManualAddition
import com.example.recipedeck.domain.shopping.SessionItemId
import com.example.recipedeck.domain.shopping.SessionState
import com.example.recipedeck.domain.shopping.ShoppingListItemSourceId
import com.example.recipedeck.domain.shopping.ShoppingSession
import com.example.recipedeck.domain.shopping.ShoppingSessionId
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Instant

@OptIn(StoredAggregateRestoration::class)
class RoomShoppingSessionRepositoryTest {
    private val createdAt = Instant.parse("2026-10-29T10:00:00.123Z")
    private val greenOnionId = ItemId("item-green-onion")
    private val noAmount = AmountColumns(null, null, null, null)
    private val storedSession = ShoppingSessionEntity("session-1", 0, null)
    private val storedRecipe = SessionRecipeEntity("recipe-1", "session-1", 0, "recipe-mapo-tofu", "麻婆豆腐", 1, 1)
    private val storedItem = SessionItemEntity("item-1", "session-1", 0, "長ネギ", null, null, noAmount)
    private val storedIngredient = SessionRecipeIngredientEntity("ingredient-1", "recipe-1", 0, "item-1", noAmount, isExcluded = false)

    private fun added(displayName: String, itemId: ItemId?, amount: Amount?, isExcluded: Boolean = false) =
        AddedRecipeIngredient(notBlankStringOrFail(displayName), itemId, amount, isExcluded)

    private fun ShoppingSession.withAddedMapoTofu() = withAddedRecipe(
        RecipeId("recipe-mapo-tofu"),
        notBlankStringOrFail("麻婆豆腐"),
        assertNotNull(Multiplier.of(2)),
        listOf(
            added("長ネギ", greenOnionId, amountOrFail(1, 2, QuantityNotation.FRACTION, AmountUnit.PIECE)),
            added("豆板醤", itemId = null, amount = null),
            added("長ネギ", greenOnionId, amount = null, isExcluded = true),
        ),
    )

    private fun ShoppingSession.withAddedFriedRice() = withAddedRecipe(
        RecipeId("recipe-fried-rice"),
        notBlankStringOrFail("炒飯"),
        assertNotNull(Multiplier.of(1, 2)),
        listOf(added("長ネギ", greenOnionId, amountOrFail(10, 1, QuantityNotation.DECIMAL, AmountUnit.GRAM))),
    )

    private fun ShoppingSession.completedCopy(id: String) =
        assertNotNull(ShoppingSession.of(ShoppingSessionId(id), SessionState.Completed(createdAt), createdAt, recipes, items))

    @Test
    fun 保存した進行中のSessionを読める() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu().withAddedFriedRice()

        assertEquals(Outcome.Success(Unit), repository.save(session))
        assertEquals(Outcome.Success(session), repository.findActive())
    }

    @Test
    fun 進行中のSessionがなければnullを返す() = runTestWithRecipeDeckDatabase { database ->
        assertEquals(Outcome.Success(null), RoomShoppingSessionRepository(database).findActive())
    }

    @Test
    fun 完了したSessionは進行中のSessionとして読まない() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)

        repository.save(ShoppingSession.create(createdAt).withAddedMapoTofu().completedCopy("session-completed"))

        assertEquals(Outcome.Success(null), repository.findActive())
    }

    @Test
    fun 手動追加のある品目を読み戻せる() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)
        val withRecipe = ShoppingSession.create(createdAt).withAddedFriedRice()
        val greenOnion = withRecipe.items.single()
        val manualOnly = greenOnion.copy(
            id = SessionItemId("item-milk"),
            displayName = notBlankStringOrFail("牛乳"),
            itemId = null,
            manualAddition = ManualAddition(ShoppingListItemSourceId("source-milk"), amount = null),
        )
        val session = assertNotNull(
            ShoppingSession.of(
                withRecipe.id,
                withRecipe.state,
                withRecipe.createdAt,
                withRecipe.recipes,
                listOf(
                    greenOnion.copy(manualAddition = ManualAddition(ShoppingListItemSourceId("source-green-onion"), amountOrFail(1, 1, QuantityNotation.DECIMAL, null))),
                    manualOnly,
                ).toImmutableList(),
            ),
        )

        repository.save(session)

        assertEquals(Outcome.Success(session), repository.findActive())
    }

    @Test
    fun 同じIDで保存すると中身を上書きする() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu()
        repository.save(session)
        val updated = session.withAddedFriedRice()

        assertEquals(Outcome.Success(Unit), repository.save(updated))
        assertEquals(Outcome.Success(updated), repository.findActive())
    }

    @Test
    fun 購入の記録があるSessionを保存し直しても購入の記録は残る() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)
        val purchaseRecordRepository = RoomPurchaseRecordRepository(database)
        val session = ShoppingSession.create(createdAt).withAddedMapoTofu()
        repository.save(session)
        val purchased = session.recipes.single().ingredients.map { it.id }.toSet()
        purchaseRecordRepository.record(session.id, purchased, isPurchased = true)

        assertEquals(Outcome.Success(Unit), repository.save(session.withAddedFriedRice()))
        assertEquals(Outcome.Success(purchased), purchaseRecordRepository.findPurchasedSourceIds(session.id))
    }

    @Test
    fun 保存しても別のSessionの中身は変わらない() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)
        val dao = database.shoppingSessionDao()
        val completed = ShoppingSession.create(createdAt).withAddedMapoTofu().completedCopy("session-completed")
        repository.save(completed)
        val recipesBefore = dao.findRecipes("session-completed")
        val itemsBefore = dao.findItems("session-completed")
        val ingredientsBefore = dao.findIngredients("session-completed")
        val active = ShoppingSession.create(createdAt).withAddedFriedRice()
        repository.save(active)

        repository.save(active.withAddedMapoTofu())

        assertEquals(recipesBefore, dao.findRecipes("session-completed"))
        assertEquals(itemsBefore, dao.findItems("session-completed"))
        assertEquals(ingredientsBefore, dao.findIngredients("session-completed"))
    }

    @Test
    fun 進行中のSessionがほかにあると保存に失敗し何も書かない() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomShoppingSessionRepository(database)
        val first = ShoppingSession.create(createdAt).withAddedMapoTofu()
        repository.save(first)
        val second = ShoppingSession.create(createdAt).withAddedFriedRice()

        assertIs<Outcome.Failure<ShoppingSessionRepositoryFailure.Unexpected>>(repository.save(second))
        assertEquals(Outcome.Success(first), repository.findActive())
        assertEquals(emptyList(), database.shoppingSessionDao().findRecipes(second.id.value))
    }

    @Test
    fun 正しい行を直接入れたSessionは読める() = runTestWithRecipeDeckDatabase { database ->
        saveStoredSession(database)

        assertIs<Outcome.Success<ShoppingSession?>>(RoomShoppingSessionRepository(database).findActive())
    }

    @Test
    fun 入れた順ではなく並び順の列の順に読む() = runTestWithRecipeDeckDatabase { database ->
        val dao = database.shoppingSessionDao()
        dao.saveSession("session-1", 0, null)
        dao.insertRecipes(listOf(storedRecipe.copy(id = "recipe-2", position = 1, recipeName = "炒飯"), storedRecipe))
        dao.insertItems(listOf(storedItem.copy(id = "item-2", position = 1, displayName = "豆板醤"), storedItem))
        dao.insertIngredients(
            listOf(
                storedIngredient.copy(id = "ingredient-2", position = 1, sessionItemId = "item-2"),
                storedIngredient,
                storedIngredient.copy(id = "ingredient-3", sessionRecipeId = "recipe-2"),
            ),
        )

        val session = assertNotNull(assertIs<Outcome.Success<ShoppingSession?>>(RoomShoppingSessionRepository(database).findActive()).value)

        assertEquals(listOf("麻婆豆腐", "炒飯"), session.recipes.map { it.recipeName.value })
        assertEquals(listOf("長ネギ", "豆板醤"), session.items.map { it.displayName.value })
        assertEquals(listOf("ingredient-1", "ingredient-2"), session.recipes.first().ingredients.map { it.id.value })
    }

    @Test
    fun 読み戻した中身が集約の規則を満たさなければ失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredSession(database, ingredient = null)

        assertIs<Outcome.Failure<ShoppingSessionRepositoryFailure.Unexpected>>(RoomShoppingSessionRepository(database).findActive())
    }

    @Test
    fun 読み戻した品目に手動追加がないのに手動追加の分量があれば失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredSession(database, item = storedItem.copy(manualAmount = AmountColumns(1, 1, "decimal", "gram")))

        assertIs<Outcome.Failure<ShoppingSessionRepositoryFailure.Unexpected>>(RoomShoppingSessionRepository(database).findActive())
    }

    @Test
    fun 読み戻した倍率が正の数でなければ失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredSession(database, recipe = storedRecipe.copy(multiplierNumerator = 0))

        assertIs<Outcome.Failure<ShoppingSessionRepositoryFailure.Unexpected>>(RoomShoppingSessionRepository(database).findActive())
    }

    @Test
    fun 読み戻したRecipe名や表示名の前後に空白があれば失敗を返す() = runTestWithRecipeDeckDatabase { database ->
        saveStoredSession(database, recipe = storedRecipe.copy(recipeName = "麻婆豆腐 "))
        val repository = RoomShoppingSessionRepository(database)
        assertIs<Outcome.Failure<ShoppingSessionRepositoryFailure.Unexpected>>(repository.findActive())

        saveStoredSession(database, item = storedItem.copy(displayName = " 長ネギ"))
        assertIs<Outcome.Failure<ShoppingSessionRepositoryFailure.Unexpected>>(repository.findActive())
    }

    private suspend fun saveStoredSession(
        database: RecipeDeckDatabase,
        session: ShoppingSessionEntity = storedSession,
        recipe: SessionRecipeEntity = storedRecipe,
        item: SessionItemEntity = storedItem,
        ingredient: SessionRecipeIngredientEntity? = storedIngredient,
    ) {
        val dao = database.shoppingSessionDao()
        dao.saveSession(session.id, session.createdAtEpochMillis, session.completedAtEpochMillis)
        dao.deleteRecipes(session.id)
        dao.deleteItems(session.id)
        dao.insertRecipes(listOf(recipe))
        dao.insertItems(listOf(item))
        dao.insertIngredients(listOfNotNull(ingredient))
    }
}
