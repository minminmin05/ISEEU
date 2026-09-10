package com.iseeu.app.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.service.LocationForegroundService
import com.iseeu.app.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val prefsDataStore: PrefsDataStore,
) : ViewModel() {

    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

    private val lastRefreshRequestAt = mutableMapOf<String, Long>()

    init {
        viewModelScope.launch {
            val familyCode = prefsDataStore.familyCode.first() ?: return@launch
            // A transient auth/network failure here shouldn't crash the whole app — worst case,
            // the map just stays empty until the next successful collection.
            try {
                val uid = authRepository.ensureSignedIn()
                memberRepository.observeMembers(familyCode, uid).collect { list ->
                    _members.value = list.sortedByDescending { it.isSelf }
                }
            } catch (e: com.google.firebase.FirebaseException) {
                // leave _members as-is; nothing else to do without a retry UI in Phase 1
            }
        }
    }

    /**
     * The only two places the service is started are here (reaching the map — covers "just
     * finished onboarding" and "cold app relaunch") and Profile's own toggle. Safe to call
     * every time the map appears: starting an already-running service is a no-op.
     */
    fun ensureTrackingStarted(context: Context) {
        viewModelScope.launch {
            val sharingEnabled = prefsDataStore.isSharingEnabled.first()
            val hasPermissions = PermissionUtils.hasForegroundLocationPermission(context) &&
                PermissionUtils.hasBackgroundLocationPermission(context)
            if (sharingEnabled && hasPermissions) {
                LocationForegroundService.start(context)
            }
        }
    }

    fun requestRefresh(targetUid: String) {
        val now = System.currentTimeMillis()
        val last = lastRefreshRequestAt[targetUid] ?: 0L
        if (now - last < REFRESH_COOLDOWN_MS) return
        lastRefreshRequestAt[targetUid] = now

        viewModelScope.launch {
            val familyCode = prefsDataStore.familyCode.first() ?: return@launch
            memberRepository.requestRefresh(familyCode, targetUid)
        }
    }

    companion object {
        private const val REFRESH_COOLDOWN_MS = 60_000L
    }
}
