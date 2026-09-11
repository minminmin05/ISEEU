package com.iseeu.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iseeu.app.ISEEUApplication
import com.iseeu.app.MainActivity
import com.iseeu.app.R
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.data.repository.PinRepository
import com.iseeu.app.location.ActivityTransitionClient
import com.iseeu.app.location.AdaptiveLocationStrategy
import com.iseeu.app.location.LocationClient
import com.iseeu.app.location.PinGeofenceClient
import com.iseeu.app.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    @Inject lateinit var activityTransitionClient: ActivityTransitionClient
    @Inject lateinit var pinRepository: PinRepository
    @Inject lateinit var pinGeofenceClient: PinGeofenceClient

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var lastServicedRefreshAt = 0L

    @Volatile private var pinNamesById: Map<String, String> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        // Must happen before subscribing to anything else, or Android 31+ can kill the service
        // with ForegroundServiceDidNotStartInTimeException.
        startForeground(NOTIFICATION_ID, buildNotification())
        serviceScope.launch { runTracking() }

        // Unlike the other permissions, ACTIVITY_RECOGNITION may genuinely be missing here: it
        // was added after the rest of the permission flow, so an already-onboarded install won't
        // have been asked for it yet. Skip quietly rather than crash — activity status just stays
        // unknown until the user grants it from a later app update's permission screen.
        if (PermissionUtils.hasActivityRecognitionPermission(this)) {
            serviceScope.launch { activityTransitionClient.start() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Deliberately NOT serviceScope here: it's cancelled two lines down, which would race
        // this cleanup call and likely cancel it before the unregister request actually goes out.
        if (PermissionUtils.hasActivityRecognitionPermission(this)) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { activityTransitionClient.stop() }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { pinGeofenceClient.stop() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runTracking() {
        val familyCode = prefsDataStore.familyCode.first()
        val uid = prefsDataStore.selfUid.first()
        Log.d("ISEEU_DIAG", "runTracking: familyCode=$familyCode uid=$uid")
        if (familyCode == null || uid == null) {
            Log.d("ISEEU_DIAG", "runTracking: stopping self, missing familyCode/uid")
            stopSelf()
            return
        }

        serviceScope.launch {
            Log.d("ISEEU_DIAG", "runTracking: subscribing to continuousUpdates")
            locationClient.continuousUpdates().collect { location ->
                Log.d("ISEEU_DIAG", "runTracking: got location $location")
                val now = System.currentTimeMillis()
                if (adaptiveStrategy.shouldWrite(location, now)) {
                    memberRepository.writeLocation(familyCode, uid, location.latitude, location.longitude)
                    // Riding on the same write-gate as the current-location write is deliberate —
                    // it's already tuned to "meaningfully moved or enough time passed", which is
                    // exactly the cadence a history trail wants too.
                    memberRepository.appendHistoryEntry(familyCode, uid, location.latitude, location.longitude)
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

        serviceScope.launch {
            pinRepository.observePins(familyCode).collect { pins ->
                pinNamesById = pins.associate { it.id to it.name }
                pinGeofenceClient.sync(pins)
            }
        }

        serviceScope.launch {
            // Seed from the first emission without notifying — otherwise every service (re)start
            // would re-announce everyone's already-current place as a fresh "arrival".
            val previousPinIds = mutableMapOf<String, String?>()
            var isFirstEmission = true
            memberRepository.observeMembers(familyCode, uid).collect { members ->
                for (member in members) {
                    if (member.isSelf) continue
                    val previous = previousPinIds[member.uid]
                    val current = member.currentPinId
                    if (!isFirstEmission && previous != current) {
                        val pinId = current ?: previous
                        val pinName = pinNamesById[pinId]
                        if (pinName != null) {
                            notifyPlaceTransition(member.displayName, pinName, arrived = current != null)
                        }
                    }
                    previousPinIds[member.uid] = current
                }
                isFirstEmission = false
            }
        }
    }

    private fun notifyPlaceTransition(memberName: String, pinName: String, arrived: Boolean) {
        val body = if (arrived) {
            getString(R.string.place_arrived_body, pinName)
        } else {
            getString(R.string.place_left_body, pinName)
        }
        val notification = NotificationCompat.Builder(this, ISEEUApplication.PLACE_ALERTS_CHANNEL_ID)
            .setContentTitle(memberName)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (PermissionUtils.hasNotificationPermission(this)) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_PLACE_BASE + memberName.hashCode(), notification)
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
        private const val NOTIFICATION_ID_PLACE_BASE = 2_000_000

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationForegroundService::class.java))
        }
    }
}
