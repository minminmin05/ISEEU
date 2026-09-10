package com.iseeu.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.domain.model.FamilyMember
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
