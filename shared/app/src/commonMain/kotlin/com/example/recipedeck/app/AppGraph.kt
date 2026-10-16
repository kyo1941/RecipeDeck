package com.example.recipedeck.app

import com.example.recipedeck.usecase.item.DuplicateCandidateFinder
import com.example.recipedeck.usecase.item.FindDuplicateCandidatesUseCase
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph
interface AppGraph {
    val findDuplicateCandidatesUseCase: FindDuplicateCandidatesUseCase

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
}
