package com.iseeu.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iseeu.app.data.local.PrefsDataStore
import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.data.repository.PinRepository
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.domain.model.HistoryPoint
import com.iseeu.app.domain.model.Pin
import com.iseeu.app.domain.model.Trip
import com.iseeu.app.util.TripSegmenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository,
    private val prefsDataStore: PrefsDataStore,
    private val pinRepository: PinRepository,
) : ViewModel() {

    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

    private val _pins = MutableStateFlow<List<Pin>>(emptyList())
    val pins: StateFlow<List<Pin>> = _pins.asStateFlow()

    private val _selectedUid = MutableStateFlow<String?>(null)
    val selectedUid: StateFlow<String?> = _selectedUid.asStateFlow()

    val selectedMember: StateFlow<FamilyMember?> = combine(_members, _selectedUid) { list, uid ->
        list.find { it.uid == uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // The family code never changes for the lifetime of an onboarded install, so resolving it
    // once in init{} and holding it here (rather than re-reading the DataStore Flow on every
    // selection change) is enough — this just needs to be set before selectMember() is ever called.
    private var familyCode: String? = null

    val historyPoints: StateFlow<List<HistoryPoint>> = _selectedUid
        .flatMapLatest { uid ->
            val code = familyCode
            if (uid == null || code == null) flowOf(emptyList()) else memberRepository.observeHistory(code, uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trips: StateFlow<List<Trip>> = historyPoints
        .map { TripSegmenter.segmentTrips(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val code = prefsDataStore.familyCode.first() ?: return@launch
            familyCode = code
            try {
                val uid = authRepository.ensureSignedIn()
                _selectedUid.value = uid // default to "my own" history
                memberRepository.observeMembers(code, uid).collect { list ->
                    _members.value = list.sortedByDescending { it.isSelf }
                }
            } catch (e: com.google.firebase.FirebaseException) {
                // leave members empty; the picker just won't have anyone to choose from yet
            }
        }
        viewModelScope.launch {
            val code = prefsDataStore.familyCode.first() ?: return@launch
            pinRepository.observePins(code).collect { _pins.value = it }
        }
    }

    fun selectMember(uid: String) {
        _selectedUid.value = uid
    }
}
