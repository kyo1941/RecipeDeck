package com.example.recipedeck.app

import androidx.room3.RoomDatabase
import com.example.recipedeck.data.RecipeDeckDatabase
import com.example.recipedeck.data.RoomTransactionRunner
import com.example.recipedeck.data.buildRecipeDeckDatabase
import com.example.recipedeck.data.item.RoomItemRepository
import com.example.recipedeck.data.recipe.RoomRecipeRepository
import com.example.recipedeck.domain.TransactionRunner
import com.example.recipedeck.domain.item.ItemRepository
import com.example.recipedeck.domain.recipe.RecipeRepository
import com.example.recipedeck.usecase.item.CreateItemUseCase
import com.example.recipedeck.usecase.item.DuplicateCandidateFinder
import com.example.recipedeck.usecase.item.FindDuplicateCandidatesUseCase
import com.example.recipedeck.usecase.item.RenameItemUseCase
import com.example.recipedeck.usecase.item.SearchItemsUseCase
import com.example.recipedeck.usecase.recipe.CreateRecipeUseCase
import com.example.recipedeck.usecase.recipe.EditRecipeUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(AppScope::class)
interface AppGraph {
    val findDuplicateCandidatesUseCase: FindDuplicateCandidatesUseCase
    val searchItemsUseCase: SearchItemsUseCase
    val createItemUseCase: CreateItemUseCase
    val renameItemUseCase: RenameItemUseCase
    val createRecipeUseCase: CreateRecipeUseCase
    val editRecipeUseCase: EditRecipeUseCase

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides databaseBuilder: RoomDatabase.Builder<RecipeDeckDatabase>): AppGraph
    }

    @SingleIn(AppScope::class)
    @Provides
    fun provideRecipeDeckDatabase(databaseBuilder: RoomDatabase.Builder<RecipeDeckDatabase>): RecipeDeckDatabase =
        buildRecipeDeckDatabase(databaseBuilder)

    @Provides
    fun provideTransactionRunner(database: RecipeDeckDatabase): TransactionRunner = RoomTransactionRunner(database)

    @Provides
    fun provideItemRepository(database: RecipeDeckDatabase): ItemRepository = RoomItemRepository(database)

    @Provides
    fun provideRecipeRepository(database: RecipeDeckDatabase): RecipeRepository = RoomRecipeRepository(database)

    /**
     * TODO: AI の具象ができるまでは、候補を出さない Finder を渡している。
     *  AI の具象を作るときに、ここで機能フラグを見て、AI の具象とこれを切り替える。
     */
    @Provides
    fun provideDuplicateCandidateFinder(): DuplicateCandidateFinder = NoDuplicateCandidateFinder

    @Provides
    fun provideFindDuplicateCandidatesUseCase(
        duplicateCandidateFinder: DuplicateCandidateFinder,
    ): FindDuplicateCandidatesUseCase = FindDuplicateCandidatesUseCase(duplicateCandidateFinder)

    @Provides
    fun provideSearchItemsUseCase(itemRepository: ItemRepository): SearchItemsUseCase =
        SearchItemsUseCase(itemRepository)

    @Provides
    fun provideCreateItemUseCase(itemRepository: ItemRepository): CreateItemUseCase =
        CreateItemUseCase(itemRepository)

    @Provides
    fun provideRenameItemUseCase(itemRepository: ItemRepository): RenameItemUseCase =
        RenameItemUseCase(itemRepository)

    @Provides
    fun provideCreateRecipeUseCase(
        recipeRepository: RecipeRepository,
        itemRepository: ItemRepository,
        transactionRunner: TransactionRunner,
    ): CreateRecipeUseCase = CreateRecipeUseCase(recipeRepository, itemRepository, transactionRunner)

    @Provides
    fun provideEditRecipeUseCase(
        recipeRepository: RecipeRepository,
        itemRepository: ItemRepository,
        transactionRunner: TransactionRunner,
    ): EditRecipeUseCase = EditRecipeUseCase(recipeRepository, itemRepository, transactionRunner)
}
