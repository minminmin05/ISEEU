package com.iseeu.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.MemberRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Only ever triggered by our own PendingIntent from GeofencingClient. Just updates this device's
 * own currentPinId on Firestore — every other family member's device is the one that notices the
 * change (via LocationForegroundService's roster listener) and shows the "arrived/left" notification,
 * the same FCM-free "other device reacts to a Firestore change" pattern used for refresh requests.
 */
@AndroidEntryPoint
class PinGeofenceReceiver : BroadcastReceiver() {

    @Inject lateinit var memberRepository: MemberRepository
    @Inject lateinit var prefsDataStore: PrefsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val pinId = event.triggeringGeofences?.firstOrNull()?.requestId ?: return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val familyCode = prefsDataStore.familyCode.first() ?: return@launch
                val uid = prefsDataStore.selfUid.first() ?: return@launch

                // Respect the privacy toggle: a hidden member's comings and goings shouldn't leak
                // to the family any more than their live location does.
                val isVisible = memberRepository.getMemberOnce(familyCode, uid)?.isVisible ?: return@launch
                if (!isVisible) return@launch

                when (event.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> memberRepository.updateCurrentPin(familyCode, uid, pinId)
                    Geofence.GEOFENCE_TRANSITION_EXIT -> memberRepository.updateCurrentPin(familyCode, uid, null)
                }
            } catch (e: com.google.firebase.FirebaseException) {
                // best-effort signal — a missed update just means presence looks stale until the next transition
            } finally {
                pendingResult.finish()
            }
        }
    }
}
