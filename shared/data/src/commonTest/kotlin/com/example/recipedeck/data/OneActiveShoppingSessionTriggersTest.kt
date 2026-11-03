package com.example.recipedeck.data

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import kotlin.test.Test
import kotlin.test.assertTrue

class OneActiveShoppingSessionTriggersTest {
    private suspend fun RecipeDeckDatabase.write(sql: String): Result<Unit> =
        runCatching { useWriterConnection { it.executeSQL(sql) } }

    private suspend fun RecipeDeckDatabase.insertSession(id: String, completedAtEpochMillis: Long?): Result<Unit> =
        write("INSERT INTO shopping_session (id, created_at_epoch_millis, completed_at_epoch_millis) VALUES ('$id', 0, ${completedAtEpochMillis ?: "NULL"})")

    @Test
    fun 進行中のSessionがほかにあると別の進行中のSessionは入れられない() = runTestWithRecipeDeckDatabase { database ->
        database.insertSession("session-1", completedAtEpochMillis = null).getOrThrow()

        assertTrue(database.insertSession("session-2", completedAtEpochMillis = null).isFailure)
    }

    @Test
    fun 完了したSessionを進行中に戻す更新はほかに進行中のSessionがあると拒まれる() = runTestWithRecipeDeckDatabase { database ->
        database.insertSession("session-completed", completedAtEpochMillis = 1).getOrThrow()
        database.insertSession("session-active", completedAtEpochMillis = null).getOrThrow()

        assertTrue(database.write("UPDATE shopping_session SET completed_at_epoch_millis = NULL WHERE id = 'session-completed'").isFailure)
    }

    @Test
    fun 完了したSessionは進行中のSessionがあっても何件でも入れられる() = runTestWithRecipeDeckDatabase { database ->
        database.insertSession("session-active", completedAtEpochMillis = null).getOrThrow()

        assertTrue(database.insertSession("session-completed-1", completedAtEpochMillis = 1).isSuccess)
        assertTrue(database.insertSession("session-completed-2", completedAtEpochMillis = 2).isSuccess)
    }

    @Test
    fun 進行中のSessionを完了にすると新しい進行中のSessionを入れられる() = runTestWithRecipeDeckDatabase { database ->
        database.insertSession("session-1", completedAtEpochMillis = null).getOrThrow()
        database.write("UPDATE shopping_session SET completed_at_epoch_millis = 1 WHERE id = 'session-1'").getOrThrow()

        assertTrue(database.insertSession("session-2", completedAtEpochMillis = null).isSuccess)
    }
}
