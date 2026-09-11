package com.iseeu.app.data.repository

import com.iseeu.app.domain.model.ActivityStatus
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.domain.model.HistoryPoint
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    /** Live roster for a family, with each visible member's live location fanned in. */
    fun observeMembers(familyCode: String, selfUid: String): Flow<List<FamilyMember>>

    suspend fun getMemberOnce(familyCode: String, uid: String): FamilyMember?

    suspend fun updateProfile(familyCode: String, uid: String, displayName: String, avatarColor: String)

    /** Saves an already-compressed, Base64-encoded JPEG (see AvatarImageProcessor) on the member doc. */
    suspend fun updateAvatarPhoto(familyCode: String, uid: String, base64Jpeg: String)

    suspend fun setVisibility(familyCode: String, uid: String, isVisible: Boolean)

    suspend fun writeLocation(familyCode: String, uid: String, lat: Double, lng: Double)

    /** Appends one point to this member's location history — capped, not a full retention policy (see repo notes). */
    suspend fun appendHistoryEntry(familyCode: String, uid: String, lat: Double, lng: Double)

    /** Most recent history points for one member, newest first. */
    fun observeHistory(familyCode: String, uid: String, limit: Long = 500): Flow<List<HistoryPoint>>

    suspend fun updateActivityStatus(familyCode: String, uid: String, status: ActivityStatus)

    /** Sets/clears which saved place (if any) this member is currently inside — see PinGeofenceReceiver. */
    suspend fun updateCurrentPin(familyCode: String, uid: String, pinId: String?)

    /** Asks another member's device to push a fresh fix — see LocationForegroundService, which is what actually listens for this. */
    suspend fun requestRefresh(familyCode: String, targetUid: String)

    /** Emits the epoch millis of each incoming refresh request aimed at this device. */
    fun observeOwnRefreshRequests(familyCode: String, uid: String): Flow<Long>
}
