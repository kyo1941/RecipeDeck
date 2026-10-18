package com.example.recipedeck.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.recipedeck.domain.Outcome
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RoomTransactionRunnerTest {
    private fun runTestWithDatabase(block: suspend TestScope.(TransactionTestDatabase) -> Unit) = runTest {
        val database = transactionTestDatabaseBuilder()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    @Test
    fun ブロックを抜けるとDAOの書き込みが残りブロックの結果を返す() = runTestWithDatabase { database ->
        val dao = database.testEntryDao()

        val result = RoomTransactionRunner(database).run {
            dao.insert(TestEntry("entry-1"))
            "書いた"
        }

        assertEquals(Outcome.Success("書いた"), result)
        assertEquals(listOf(TestEntry("entry-1")), dao.findAll())
    }

    @Test
    fun 取り消すとDAOの書き込みが消え指定した結果を返す() = runTestWithDatabase { database ->
        val dao = database.testEntryDao()

        val result = RoomTransactionRunner(database).run {
            dao.insert(TestEntry("entry-1"))
            rollback("取り消した")
        }

        assertEquals(Outcome.Success("取り消した"), result)
        assertEquals(emptyList(), dao.findAll())
    }

    @Test
    fun データベースを使えないときは失敗を返す() = runTestWithDatabase { database ->
        database.close()

        val result = RoomTransactionRunner(database).run { "書いた" }

        assertIs<Outcome.Failure<*>>(result)
    }

    @Test
    fun キャンセルは失敗にせずそのまま投げる() = runTestWithDatabase { database ->
        assertFailsWith<CancellationException> {
            RoomTransactionRunner(database).run<Unit> { throw CancellationException("キャンセルした") }
        }
    }
}
