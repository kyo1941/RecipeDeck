package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.Outcome
import kotlinx.coroutines.flow.Flow

interface ShoppingSessionRepository {
    suspend fun save(session: ShoppingSession): Outcome<Unit, ShoppingSessionRepositoryFailure>
    suspend fun findActive(): Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure>
    fun observeActive(): Flow<Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure>>
}
