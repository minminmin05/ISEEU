package com.iseeu.app.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object CreateOrJoin : Screen("create_or_join")
    data object CreateFamily : Screen("create_family")
    data object JoinFamily : Screen("join_family")
    data object Permissions : Screen("permissions")
    data object Map : Screen("map")
    data object Profile : Screen("profile")
    data object History : Screen("history")
}
