package com.example.recipedeck.data.shopping

import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.runTestWithRecipeDeckDatabase
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.shopping.PurchaseRecordRepositoryFailure
import com.example.recipedeck.domain.shopping.ShoppingListItemSourceId
import com.example.recipedeck.domain.shopping.ShoppingSessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RoomPurchaseRecordRepositoryTest {
    private val sessionId = ShoppingSessionId("session-1")
    private val greenOnion = ShoppingListItemSourceId("source-green-onion")
    private val doubanjiang = ShoppingListItemSourceId("source-doubanjiang")
    private val tofu = ShoppingListItemSourceId("source-tofu")

    private suspend fun RecipeDeckDatabase.saveSession(id: ShoppingSessionId, completedAtEpochMillis: Long? = null) =
        shoppingSessionDao().saveSession(id.value, createdAtEpochMillis = 0, completedAtEpochMillis = completedAtEpochMillis)

    @Test
    fun 購入済みにした内訳のIDを読める() = runTestWithRecipeDeckDatabase { database ->
        database.saveSession(sessionId)
        val repository = RoomPurchaseRecordRepository(database)

        assertEquals(Outcome.Success(Unit), repository.record(sessionId, setOf(greenOnion, doubanjiang), isPurchased = true))
        assertEquals(Outcome.Success(setOf(greenOnion, doubanjiang)), repository.findPurchasedSourceIds(sessionId))
    }

    @Test
    fun 購入の記録がなければ空の集合を返す() = runTestWithRecipeDeckDatabase { database ->
        database.saveSession(sessionId)

        assertEquals(Outcome.Success(emptySet()), RoomPurchaseRecordRepository(database).findPurchasedSourceIds(sessionId))
    }

    @Test
    fun 未購入にした内訳だけが購入済みから外れる() = runTestWithRecipeDeckDatabase { database ->
        database.saveSession(sessionId)
        val repository = RoomPurchaseRecordRepository(database)
        repository.record(sessionId, setOf(greenOnion, doubanjiang, tofu), isPurchased = true)

        assertEquals(Outcome.Success(Unit), repository.record(sessionId, setOf(greenOnion, doubanjiang), isPurchased = false))
        assertEquals(Outcome.Success(setOf(tofu)), repository.findPurchasedSourceIds(sessionId))
    }

    @Test
    fun 購入済みの内訳をもう一度購入済みにしても失敗せず結果は変わらない() = runTestWithRecipeDeckDatabase { database ->
        database.saveSession(sessionId)
        val repository = RoomPurchaseRecordRepository(database)
        repository.record(sessionId, setOf(greenOnion), isPurchased = true)

        assertEquals(Outcome.Success(Unit), repository.record(sessionId, setOf(greenOnion, doubanjiang), isPurchased = true))
        assertEquals(Outcome.Success(setOf(greenOnion, doubanjiang)), repository.findPurchasedSourceIds(sessionId))
    }

    @Test
    fun 記録のない内訳を未購入にしても失敗しない() = runTestWithRecipeDeckDatabase { database ->
        database.saveSession(sessionId)
        val repository = RoomPurchaseRecordRepository(database)

        assertEquals(Outcome.Success(Unit), repository.record(sessionId, setOf(greenOnion), isPurchased = false))
        assertEquals(Outcome.Success(emptySet()), repository.findPurchasedSourceIds(sessionId))
    }

    @Test
    fun 別のSessionの購入の記録は読まず変えない() = runTestWithRecipeDeckDatabase { database ->
        val otherSessionId = ShoppingSessionId("session-completed")
        database.saveSession(sessionId)
        database.saveSession(otherSessionId, completedAtEpochMillis = 1)
        val repository = RoomPurchaseRecordRepository(database)
        repository.record(otherSessionId, setOf(greenOnion), isPurchased = true)

        repository.record(sessionId, setOf(doubanjiang), isPurchased = true)
        repository.record(sessionId, setOf(greenOnion), isPurchased = false)

        assertEquals(Outcome.Success(setOf(doubanjiang)), repository.findPurchasedSourceIds(sessionId))
        assertEquals(Outcome.Success(setOf(greenOnion)), repository.findPurchasedSourceIds(otherSessionId))
    }

    @Test
    fun 無いSessionに購入済みを記録すると失敗し何も残さない() = runTestWithRecipeDeckDatabase { database ->
        val repository = RoomPurchaseRecordRepository(database)

        assertIs<Outcome.Failure<PurchaseRecordRepositoryFailure.Unexpected>>(repository.record(sessionId, setOf(greenOnion, doubanjiang), isPurchased = true))
        assertEquals(Outcome.Success(emptySet()), repository.findPurchasedSourceIds(sessionId))
    }
}
