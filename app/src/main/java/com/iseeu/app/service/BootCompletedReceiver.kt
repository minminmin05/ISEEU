package com.iseeu.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restarts tracking after a device reboot — without this, a phone that restarts (OS update, dead
 * battery) silently stops sharing until someone manually reopens the app. Not in the original
 * brief, added deliberately: this failure mode matters most for exactly who this app is likely to
 * track (kids, elderly parents).
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var prefsDataStore: PrefsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val onboarded = prefsDataStore.hasCompletedOnboarding.first()
                val sharing = prefsDataStore.isSharingEnabled.first()
                val hasPermissions = PermissionUtils.hasForegroundLocationPermission(context) &&
                    PermissionUtils.hasBackgroundLocationPermission(context)

                if (onboarded && sharing && hasPermissions) {
                    LocationForegroundService.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
