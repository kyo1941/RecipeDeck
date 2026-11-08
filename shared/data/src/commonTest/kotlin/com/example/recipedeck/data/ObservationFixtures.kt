package com.example.recipedeck.data

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

fun <T> TestScope.observedValues(flow: Flow<T>): ReceiveChannel<T> {
    val values = Channel<T>(Channel.UNLIMITED)
    backgroundScope.launch {
        flow.collect { values.send(it) }
        values.close()
    }
    return values
}

suspend fun RecipeDeckDatabase.writeSql(sql: String) {
    useWriterConnection { it.executeSQL(sql) }
}
