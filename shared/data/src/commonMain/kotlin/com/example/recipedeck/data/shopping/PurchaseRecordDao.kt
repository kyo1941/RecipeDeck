package com.example.recipedeck.data.shopping

import androidx.room3.Dao
import androidx.room3.Query

@Dao
internal interface PurchaseRecordDao {
    @Query("SELECT source_id FROM purchase_record WHERE session_id = :sessionId")
    suspend fun findPurchasedSourceIds(sessionId: String): List<String>

    @Query("INSERT INTO purchase_record (session_id, source_id) VALUES (:sessionId, :sourceId) ON CONFLICT(session_id, source_id) DO NOTHING")
    suspend fun markPurchased(sessionId: String, sourceId: String)

    @Query("DELETE FROM purchase_record WHERE session_id = :sessionId AND source_id IN (:sourceIds)")
    suspend fun markUnpurchased(sessionId: String, sourceIds: List<String>)
}
