package com.example.recipedeck.ui.navigation

import androidx.navigation3.runtime.NavEntry

internal actual fun entriesBeneathNonStartDestination(startDestinationEntries: List<NavEntry<Route>>): List<NavEntry<Route>> =
    startDestinationEntries
