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
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

class RecipeDeckNavigation(
    currentDestination: MutableState<TopLevelDestination>,
    private val backStacks: Map<TopLevelDestination, NavBackStack<Route>>,
) {
    var currentDestination: TopLevelDestination by currentDestination
        private set

    fun select(destination: TopLevelDestination) {
        currentDestination = destination
    }

    fun goBack() {
        val backStack = backStacks.getValue(currentDestination)
        if (backStack.size > 1) backStack.removeLastOrNull() else select(TopLevelDestination.START)
    }

    /**
     * NavDisplay に decorator を直接渡すと、タブの切り替えが pop として扱われ、decorator が持つ状態が捨てられる。
     * そのため、まとめて全ての BackStack の分を作り、表示する分だけ返す。
     */
    @Composable
    fun displayedEntries(entryProvider: (Route) -> NavEntry<Route>): List<NavEntry<Route>> {
        val entriesByDestination = backStacks.mapValues { (destination, backStack) ->
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    OpaqueBackgroundNavEntryDecorator,
                ),
                entryProvider = { route ->
                    val entry = entryProvider(route)
                    NavEntry(route, entry.contentKey, entry.metadata + (DESTINATION_METADATA_KEY to destination)) { entry.Content() }
                },
            )
        }
        val currentEntries = entriesByDestination.getValue(currentDestination)
        return if (currentDestination == TopLevelDestination.START) {
            currentEntries
        } else {
            entriesBeneathNonStartDestination(entriesByDestination.getValue(TopLevelDestination.START)) + currentEntries
        }
    }
}

private const val DESTINATION_METADATA_KEY = "topLevelDestination"

/**
 * NavDisplay は、切り替えの間、次の画面を前の画面の上に重ねて描くので、画面に背景がないと前の画面が透けて見える。
 */
private val OpaqueBackgroundNavEntryDecorator = NavEntryDecorator<Route> { entry -> Surface { entry.Content() } }

/**
 * NavDisplay はタブの切り替えも積む・外すとして動かすので、一番上の画面のタブが変わる切り替えでは動かさない。
 */
fun withoutAnimationOnDestinationChange(
    transition: AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform,
): AnimatedContentTransitionScope<Scene<Route>>.() -> ContentTransform = {
    if (initialState.destination != targetState.destination) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        transition()
    }
}

private val Scene<Route>.destination: TopLevelDestination?
    get() = entries.last().metadata[DESTINATION_METADATA_KEY] as? TopLevelDestination

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
