package com.iseeu.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.iseeu.app.ISEEUApplication
import com.iseeu.app.MainActivity
import com.iseeu.app.R
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.location.AdaptiveLocationStrategy
import com.iseeu.app.location.LocationClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Runs continuous adaptive location tracking and listens for on-demand refresh requests aimed at
 * this device (the FCM-free "wake me up" flow — see the plan's deviation #2). Only ever started
 * after the permission flow has completed, so no defensive permission checks here.
 */
@AndroidEntryPoint
class LocationForegroundService : Service() {

    @Inject lateinit var locationClient: LocationClient
    @Inject lateinit var adaptiveStrategy: AdaptiveLocationStrategy
    @Inject lateinit var memberRepository: MemberRepository
    @Inject lateinit var prefsDataStore: PrefsDataStore

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var lastServicedRefreshAt = 0L

    override fun onCreate() {
        super.onCreate()
        // Must happen before subscribing to anything else, or Android 31+ can kill the service
        // with ForegroundServiceDidNotStartInTimeException.
        startForeground(NOTIFICATION_ID, buildNotification())
        serviceScope.launch { runTracking() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runTracking() {
        val familyCode = prefsDataStore.familyCode.first()
        val uid = prefsDataStore.selfUid.first()
        if (familyCode == null || uid == null) {
            stopSelf()
            return
        }

        serviceScope.launch {
            locationClient.continuousUpdates().collect { location ->
                val now = System.currentTimeMillis()
                if (adaptiveStrategy.shouldWrite(location, now)) {
                    memberRepository.writeLocation(familyCode, uid, location.latitude, location.longitude)
                    adaptiveStrategy.markWritten(location, now)
                }
            }
        }

        serviceScope.launch {
            memberRepository.observeOwnRefreshRequests(familyCode, uid).collect { requestedAtMillis ->
                if (requestedAtMillis > lastServicedRefreshAt) {
                    lastServicedRefreshAt = requestedAtMillis
                    locationClient.oneShotHighAccuracy()?.let { fix ->
                        memberRepository.writeLocation(familyCode, uid, fix.latitude, fix.longitude)
                        adaptiveStrategy.markWritten(fix, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ISEEUApplication.LOCATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.location_service_notification_title))
            .setContentText(getString(R.string.location_service_notification_body))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }
}
