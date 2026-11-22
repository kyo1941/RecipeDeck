package com.example.recipedeck.ui.navigation

import androidx.navigation3.runtime.NavEntry

/**
 * NavDisplay は渡された画面の中でしか戻らず、戻る途中に見せるのもその画面なので、開始のタブへ戻る OS では、開始のタブの画面を下に置く。
 */
internal expect fun entriesBeneathNonStartDestination(startDestinationEntries: List<NavEntry<Route>>): List<NavEntry<Route>>
