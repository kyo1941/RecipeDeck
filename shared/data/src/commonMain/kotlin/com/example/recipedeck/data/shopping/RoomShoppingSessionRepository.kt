package com.example.recipedeck.data.shopping

import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import com.example.recipedeck.data.InvalidStoredRecordException
import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.amount.toAmountColumns
import com.example.recipedeck.data.amount.toAmountOrNull
import com.example.recipedeck.data.outcomeOf
import com.example.recipedeck.data.storedNotBlankStringOf
import com.example.recipedeck.domain.Outcome
import com.example.recipedeck.domain.StoredAggregateRestoration
import com.example.recipedeck.domain.amount.Multiplier
import com.example.recipedeck.domain.item.ItemId
import com.example.recipedeck.domain.recipe.RecipeId
import com.example.recipedeck.domain.shopping.ManualAddition
import com.example.recipedeck.domain.shopping.SessionItem
import com.example.recipedeck.domain.shopping.SessionItemId
import com.example.recipedeck.domain.shopping.SessionRecipe
import com.example.recipedeck.domain.shopping.SessionRecipeId
import com.example.recipedeck.domain.shopping.SessionRecipeIngredient
import com.example.recipedeck.domain.shopping.SessionState
import com.example.recipedeck.domain.shopping.ShoppingListItemSourceId
import com.example.recipedeck.domain.shopping.ShoppingSession
import com.example.recipedeck.domain.shopping.ShoppingSessionId
import com.example.recipedeck.domain.shopping.ShoppingSessionRepository
import com.example.recipedeck.domain.shopping.ShoppingSessionRepositoryFailure
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Instant

class RoomShoppingSessionRepository(private val database: RecipeDeckDatabase) : ShoppingSessionRepository {
    private val dao = database.shoppingSessionDao()

    override suspend fun save(session: ShoppingSession): Outcome<Unit, ShoppingSessionRepositoryFailure> =
        outcomeOf(ShoppingSessionRepositoryFailure::Unexpected) {
            val sessionId = session.id.value
            database.withWriteTransaction {
                dao.saveSession(
                    id = sessionId,
                    createdAtEpochMillis = session.createdAt.toEpochMilliseconds(),
                    completedAtEpochMillis = session.state.completedAtOrNull?.toEpochMilliseconds(),
                )
                dao.deleteRecipes(sessionId)
                dao.deleteItems(sessionId)
                dao.insertRecipes(
                    session.recipes.mapIndexed { position, recipe ->
                        SessionRecipeEntity(
                            id = recipe.id.value,
                            sessionId = sessionId,
                            position = position,
                            sourceRecipeId = recipe.sourceRecipeId.value,
                            recipeName = recipe.recipeName.value,
                            multiplierNumerator = recipe.multiplier.numerator,
                            multiplierDenominator = recipe.multiplier.denominator,
                        )
                    },
                )
                dao.insertItems(
                    session.items.mapIndexed { position, item ->
                        SessionItemEntity(
                            id = item.id.value,
                            sessionId = sessionId,
                            position = position,
                            displayName = item.displayName.value,
                            itemId = item.itemId?.value,
                            manualSourceId = item.manualAddition?.id?.value,
                            manualAmount = item.manualAddition?.amount.toAmountColumns(),
                        )
                    },
                )
                dao.insertIngredients(
                    session.recipes.flatMap { recipe ->
                        recipe.ingredients.mapIndexed { position, ingredient ->
                            SessionRecipeIngredientEntity(
                                id = ingredient.id.value,
                                sessionRecipeId = recipe.id.value,
                                position = position,
                                sessionItemId = ingredient.sessionItemId.value,
                                amount = ingredient.amount.toAmountColumns(),
                                isExcluded = ingredient.isExcluded,
                            )
                        }
                    },
                )
            }
        }

    override suspend fun findActive(): Outcome<ShoppingSession?, ShoppingSessionRepositoryFailure> =
        outcomeOf(ShoppingSessionRepositoryFailure::Unexpected) {
            database.withReadTransaction {
                dao.findActive()?.let { session ->
                    session.toShoppingSession(dao.findRecipes(session.id), dao.findItems(session.id), dao.findIngredients(session.id))
                }
            }
        }
}

private val SessionState.completedAtOrNull: Instant?
    get() = when (this) {
        SessionState.Active -> null
        is SessionState.Completed -> completedAt
    }

@OptIn(StoredAggregateRestoration::class)
private fun ShoppingSessionEntity.toShoppingSession(
    recipes: List<SessionRecipeEntity>,
    items: List<SessionItemEntity>,
    ingredients: List<SessionRecipeIngredientEntity>,
): ShoppingSession {
    val ingredientsByRecipeId = ingredients.groupBy { it.sessionRecipeId }
    return ShoppingSession.of(
        id = ShoppingSessionId(id),
        state = completedAtEpochMillis?.let { SessionState.Completed(Instant.fromEpochMilliseconds(it)) } ?: SessionState.Active,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        recipes = recipes.map { it.toSessionRecipe(ingredientsByRecipeId[it.id].orEmpty()) }.toImmutableList(),
        items = items.map { it.toSessionItem() }.toImmutableList(),
    ) ?: throw InvalidStoredRecordException("Session $id が集約の規則を満たさない")
}

private fun SessionRecipeEntity.toSessionRecipe(ingredients: List<SessionRecipeIngredientEntity>): SessionRecipe = SessionRecipe(
    id = SessionRecipeId(id),
    sourceRecipeId = RecipeId(sourceRecipeId),
    recipeName = storedNotBlankStringOf(recipeName) { "Session 内の Recipe $id の名前" },
    multiplier = Multiplier.of(multiplierNumerator, multiplierDenominator)
        ?: throw InvalidStoredRecordException("Session 内の Recipe $id の倍率が正の数になっていない: $multiplierNumerator/$multiplierDenominator"),
    ingredients = ingredients.map { it.toSessionRecipeIngredient() }.toImmutableList(),
)

private fun SessionRecipeIngredientEntity.toSessionRecipeIngredient(): SessionRecipeIngredient = SessionRecipeIngredient(
    id = ShoppingListItemSourceId(id),
    sessionItemId = SessionItemId(sessionItemId),
    amount = amount.toAmountOrNull(),
    isExcluded = isExcluded,
)

private fun SessionItemEntity.toSessionItem(): SessionItem = SessionItem(
    id = SessionItemId(id),
    displayName = storedNotBlankStringOf(displayName) { "品目 $id の表示名" },
    itemId = itemId?.let(::ItemId),
    manualAddition = toManualAddition(),
)

private fun SessionItemEntity.toManualAddition(): ManualAddition? {
    if (manualSourceId == null) {
        if (manualAmount.toAmountOrNull() != null) {
            throw InvalidStoredRecordException("品目 $id は手動追加がないのに、手動追加の分量の列が埋まっている")
        }
        return null
    }
    return ManualAddition(ShoppingListItemSourceId(manualSourceId), manualAmount.toAmountOrNull())
}
