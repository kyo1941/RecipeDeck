package com.example.recipedeck.domain.shopping

import com.example.recipedeck.domain.Outcome

interface ShoppingSessionRepository {
    suspend fun save(session: ShoppingSession): Outcome<Unit, ShoppingSessionRepositoryFailure>
    suspend fun findActive(): Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure>
}
