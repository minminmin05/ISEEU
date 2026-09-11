package com.iseeu.app.ui.map

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.R
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.data.repository.PinRepository
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.domain.model.Pin
import com.iseeu.app.domain.model.PinType
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
    private val pinRepository: PinRepository,
) : ViewModel() {

    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

    private val _pins = MutableStateFlow<List<Pin>>(emptyList())
    val pins: StateFlow<List<Pin>> = _pins.asStateFlow()

    @get:StringRes
    var pinErrorRes by mutableStateOf<Int?>(null)
        private set

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
        viewModelScope.launch {
            val familyCode = prefsDataStore.familyCode.first() ?: return@launch
            pinRepository.observePins(familyCode).collect { _pins.value = it }
        }
    }

    fun addPin(name: String, lat: Double, lng: Double, type: PinType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val familyCode = prefsDataStore.familyCode.first() ?: return@launch
            try {
                val uid = authRepository.ensureSignedIn()
                pinRepository.addPin(familyCode, uid, name.trim(), lat, lng, type)
            } catch (e: com.google.firebase.FirebaseException) {
                android.util.Log.e("MapViewModel", "addPin failed", e)
                pinErrorRes = R.string.pin_save_error
            }
        }
    }

    fun deletePin(pinId: String) {
        viewModelScope.launch {
            val familyCode = prefsDataStore.familyCode.first() ?: return@launch
            try {
                pinRepository.deletePin(familyCode, pinId)
            } catch (e: com.google.firebase.FirebaseException) {
                android.util.Log.e("MapViewModel", "deletePin failed", e)
                pinErrorRes = R.string.pin_save_error
            }
        }
    }

    fun consumePinError() {
        pinErrorRes = null
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
