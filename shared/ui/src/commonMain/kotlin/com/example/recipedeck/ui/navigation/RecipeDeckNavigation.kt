package com.example.recipedeck.ui.navigation

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
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
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
     * 積み重ねを NavDisplay に渡して任せると、タブを切り替えて NavDisplay が外れたときに、画面の状態と ViewModel も捨てられるので、全部のタブの分をここで作る。
     */
    @Composable
    fun entriesByDestination(entryProvider: (Route) -> NavEntry<Route>): Map<TopLevelDestination, List<NavEntry<Route>>> =
        backStacks.mapValues { (_, backStack) ->
            rememberDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
}

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
