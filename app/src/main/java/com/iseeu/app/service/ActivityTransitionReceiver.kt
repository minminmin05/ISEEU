package com.iseeu.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.domain.model.ActivityStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTransitionReceiver : BroadcastReceiver() {

    @Inject lateinit var memberRepository: MemberRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var prefsDataStore: PrefsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        // Multiple ENTER events can arrive in one batch; the last one is the current state.
        val latest = result.transitionEvents.lastOrNull() ?: return
        val status = latest.toActivityStatus() ?: return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val familyCode = prefsDataStore.familyCode.first() ?: return@launch
                val uid = prefsDataStore.selfUid.first() ?: return@launch
                memberRepository.updateActivityStatus(familyCode, uid, status)
            } catch (e: com.google.firebase.FirebaseException) {
                // best-effort signal — a missed update just means the roster shows a stale status
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun com.google.android.gms.location.ActivityTransitionEvent.toActivityStatus(): ActivityStatus? = when (activityType) {
        DetectedActivity.STILL -> ActivityStatus.STILL
        DetectedActivity.WALKING, DetectedActivity.ON_FOOT, DetectedActivity.RUNNING -> ActivityStatus.WALKING
        DetectedActivity.IN_VEHICLE -> ActivityStatus.DRIVING
        else -> null
    }
}
