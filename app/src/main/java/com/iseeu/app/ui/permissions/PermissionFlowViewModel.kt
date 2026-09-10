package com.iseeu.app.ui.permissions

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.iseeu.app.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class PermissionStep { FOREGROUND_LOCATION, BACKGROUND_LOCATION, NOTIFICATIONS, DONE }

@HiltViewModel
class PermissionFlowViewModel @Inject constructor() : ViewModel() {

    var step by mutableStateOf(PermissionStep.FOREGROUND_LOCATION)
        private set

    fun refresh(context: Context) {
        step = when {
            !PermissionUtils.hasForegroundLocationPermission(context) -> PermissionStep.FOREGROUND_LOCATION
            !PermissionUtils.hasBackgroundLocationPermission(context) -> PermissionStep.BACKGROUND_LOCATION
            !PermissionUtils.hasNotificationPermission(context) -> PermissionStep.NOTIFICATIONS
            else -> PermissionStep.DONE
        }
    }
}
