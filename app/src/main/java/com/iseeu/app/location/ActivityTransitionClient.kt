package com.iseeu.app.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.iseeu.app.service.ActivityTransitionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps ActivityRecognitionClient's transition API — this is the same building block most
 * location-sharing apps use for a lightweight walking/driving/still signal (a dedicated
 * classifier is overkill for a 10-person family app).
 */
@Singleton
class ActivityTransitionClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ACTION_ACTIVITY_TRANSITION
        }
        // FLAG_MUTABLE is required here — the system fills this PendingIntent's Intent with the
        // ActivityTransitionResult extra when delivering it; FLAG_IMMUTABLE would silently drop it.
        PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    suspend fun start() {
        val transitions = listOf(DetectedActivity.STILL, DetectedActivity.WALKING, DetectedActivity.IN_VEHICLE)
            .flatMap { type ->
                listOf(
                    ActivityTransition.Builder()
                        .setActivityType(type)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build(),
                )
            }
        val request = ActivityTransitionRequest(transitions)
        ActivityRecognition.getClient(context).requestActivityTransitionUpdates(request, pendingIntent).await()
    }

    suspend fun stop() {
        ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent).await()
    }

    companion object {
        const val ACTION_ACTIVITY_TRANSITION = "com.iseeu.app.ACTION_ACTIVITY_TRANSITION"
        private const val REQUEST_CODE = 2001
    }
}
