package com.iseeu.app.data.repository

import com.iseeu.app.domain.model.FamilyMember
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    /** Live roster for a family, with each visible member's live location fanned in. */
    fun observeMembers(familyCode: String, selfUid: String): Flow<List<FamilyMember>>

    suspend fun getMemberOnce(familyCode: String, uid: String): FamilyMember?

    suspend fun updateProfile(familyCode: String, uid: String, displayName: String, avatarColor: String)

    suspend fun setVisibility(familyCode: String, uid: String, isVisible: Boolean)

    suspend fun writeLocation(familyCode: String, uid: String, lat: Double, lng: Double)

    /** Asks another member's device to push a fresh fix — see LocationForegroundService, which is what actually listens for this. */
    suspend fun requestRefresh(familyCode: String, targetUid: String)

    /** Emits the epoch millis of each incoming refresh request aimed at this device. */
    fun observeOwnRefreshRequests(familyCode: String, uid: String): Flow<Long>
}
