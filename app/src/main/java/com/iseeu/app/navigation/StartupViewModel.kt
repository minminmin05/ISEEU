package com.iseeu.app.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Decides where a (re)launch should land: onboarding, permissions, or straight to the map. */
@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsDataStore: PrefsDataStore,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    init {
        viewModelScope.launch {
            val onboarded = prefsDataStore.hasCompletedOnboarding.first()
            val hasPermissions = PermissionUtils.hasForegroundLocationPermission(context) &&
                PermissionUtils.hasBackgroundLocationPermission(context) &&
                PermissionUtils.hasNotificationPermission(context)

            _startDestination.value = when {
                !onboarded -> Screen.Welcome.route
                !hasPermissions -> Screen.Permissions.route
                else -> Screen.Map.route
            }
        }
    }
}
