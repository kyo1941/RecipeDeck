package com.example.recipedeck.usecase.shopping

import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.shopping.SessionState
import com.example.recipedeck.domain.shopping.ShoppingSession
import com.example.recipedeck.domain.shopping.ShoppingSessionRepository
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeShoppingSessionRepository : ShoppingSessionRepository {
    val savedSessions = mutableListOf<ShoppingSession>()
    var saveFailure: ShoppingSessionRepositoryFailure? = null
    var findFailure: ShoppingSessionRepositoryFailure? = null
    private val saveCount = MutableStateFlow(0)

    override suspend fun save(session: ShoppingSession): Outcome<Unit, ShoppingSessionRepositoryFailure> {
        saveFailure?.let { return Outcome.Failure(it) }
        savedSessions += session
        saveCount.update { it + 1 }
        return Outcome.Success(Unit)
    }

    override suspend fun findActive(): Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure> {
        findFailure?.let { return Outcome.Failure(it) }
        return Outcome.Success(savedSessions.lastOrNull { it.state == SessionState.Active })
    }

    override fun observeActive(): Flow<Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure>> =
        saveCount.map { findActive() }
}
