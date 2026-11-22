package com.example.recipedeck.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

class RecipeDeckNavigation(
    currentDestination: MutableState<TopLevelDestination>,
    private val backStacks: Map<TopLevelDestination, NavBackStack<Route>>,
) {
    var currentDestination: TopLevelDestination by currentDestination
        private set

    val isAtRootOfNonStartDestination: Boolean
        get() = currentDestination != TopLevelDestination.START && backStacks.getValue(currentDestination).size == 1

    fun select(destination: TopLevelDestination) {
        currentDestination = destination
    }

    fun goBack() {
        backStacks.getValue(currentDestination).removeLastOrNull()
    }

    /**
     * NavDisplay に decorator を直接渡すと、タブの切り替えが pop として扱われ、decorator が持つ状態が捨てられる。
     * そのため、まとめて全ての BackStack の分を作り、今のタブの分だけ返す。
     */
    @Composable
    fun currentEntries(entryProvider: (Route) -> NavEntry<Route>): List<NavEntry<Route>> =
        backStacks.mapValues { (_, backStack) ->
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    OpaqueBackgroundNavEntryDecorator,
                ),
                entryProvider = entryProvider,
            )
        }.getValue(currentDestination)
}

/**
 * NavDisplay は、切り替えの間、次の画面を前の画面の上に重ねて描くので、画面に背景がないと前の画面が透けて見える。
 */
private val OpaqueBackgroundNavEntryDecorator = NavEntryDecorator<Route> { entry -> Surface { entry.Content() } }

/**
 * NavDisplay は、積み重ねの先頭が変わると進む操作として動かすので、先頭が変わるタブの切り替えでは動かさない。
 */
fun forwardTransitionExceptDestinationChange(): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform = {
    if (initialState.root != targetState.root) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        defaultTransitionSpec<Route>()()
    }
}

private val Scene<Route>.root: Any
    get() = (previousEntries + entries).first().contentKey

@Composable
fun rememberRecipeDeckNavigation(): RecipeDeckNavigation {
    val currentDestination = rememberSerializable(serializer = MutableStateSerializer(TopLevelDestination.serializer())) {
        mutableStateOf(TopLevelDestination.START)
    }
    val backStacks = TopLevelDestination.entries.associateWith { destination ->
        rememberSerializable(destination, serializer = NavBackStackSerializer(Route.serializer())) {
            NavBackStack<Route>(destination.root)
        }
    }
    return remember(currentDestination, backStacks) { RecipeDeckNavigation(currentDestination, backStacks) }
}
