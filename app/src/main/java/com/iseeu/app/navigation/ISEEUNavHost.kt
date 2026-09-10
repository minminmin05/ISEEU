package com.iseeu.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iseeu.app.ui.map.MapScreen
import com.iseeu.app.ui.onboarding.CreateFamilyScreen
import com.iseeu.app.ui.onboarding.CreateOrJoinScreen
import com.iseeu.app.ui.onboarding.JoinFamilyScreen
import com.iseeu.app.ui.onboarding.WelcomeScreen
import com.iseeu.app.ui.permissions.PermissionRationaleScreen
import com.iseeu.app.ui.profile.ProfileScreen

@Composable
fun ISEEUNavHost(
    navController: NavHostController = rememberNavController(),
    startupViewModel: StartupViewModel = hiltViewModel(),
) {
    val startDestination by startupViewModel.startDestination.collectAsStateWithLifecycle()
    val destination = startDestination

    if (destination == null) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }

    NavHost(navController = navController, startDestination = destination) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(onContinue = { navController.navigate(Screen.CreateOrJoin.route) })
        }
        composable(Screen.CreateOrJoin.route) {
            CreateOrJoinScreen(
                onCreate = { navController.navigate(Screen.CreateFamily.route) },
                onJoin = { navController.navigate(Screen.JoinFamily.route) },
            )
        }
        composable(Screen.CreateFamily.route) {
            CreateFamilyScreen(onContinue = { navController.goToPermissions() })
        }
        composable(Screen.JoinFamily.route) {
            JoinFamilyScreen(onJoined = { navController.goToPermissions() })
        }
        composable(Screen.Permissions.route) {
            PermissionRationaleScreen(onAllGranted = { navController.goToMap() })
        }
        composable(Screen.Map.route) {
            MapScreen(onOpenProfile = { navController.navigate(Screen.Profile.route) })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.goToPermissions() {
    navigate(Screen.Permissions.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }
}

private fun NavHostController.goToMap() {
    navigate(Screen.Map.route) { popUpTo(0) }
}
