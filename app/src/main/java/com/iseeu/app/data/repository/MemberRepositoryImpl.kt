package com.iseeu.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.iseeu.app.data.remote.FirestorePaths
import com.iseeu.app.data.remote.dto.LocationDto
import com.iseeu.app.data.remote.dto.MemberDto
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.domain.model.MemberLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : MemberRepository {

    override fun observeMembers(familyCode: String, selfUid: String): Flow<List<FamilyMember>> = callbackFlow {
        // Firestore has no joins: the roster listener gives us name/avatar/visibility, and each
        // visible member additionally gets its own listener on its location subdocument, fanned
        // back into one combined list on every change from either source.
        val members = mutableMapOf<String, MemberDto>()
        val locations = mutableMapOf<String, MemberLocation?>()
        val locationListeners = mutableMapOf<String, ListenerRegistration>()

        fun emitCurrentState() {
            val result = members.map { (uid, dto) ->
                FamilyMember(
                    uid = uid,
                    displayName = dto.displayName,
                    avatarColor = dto.avatarColor,
                    isVisible = dto.isVisible,
                    isSelf = uid == selfUid,
                    location = locations[uid],
                )
            }
            trySend(result)
        }

        val membersListener = FirestorePaths.membersCollection(firestore, familyCode)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val currentUids = snapshot.documents.map { it.id }.toSet()

                locationListeners.keys.filter { it !in currentUids }.forEach { uid ->
                    locationListeners.remove(uid)?.remove()
                    locations.remove(uid)
                }
                members.keys.filter { it !in currentUids }.forEach { members.remove(it) }

                for (doc in snapshot.documents) {
                    val uid = doc.id
                    val dto = doc.toObject(MemberDto::class.java) ?: continue
                    members[uid] = dto

                    // Rules deny reading location/current for a hidden member who isn't you —
                    // don't even attempt that listener, it would just fail with permission-denied.
                    if (dto.isVisible || uid == selfUid) {
                        if (uid !in locationListeners) {
                            locationListeners[uid] = FirestorePaths.memberLocationDoc(firestore, familyCode, uid)
                                .addSnapshotListener { locSnapshot, _ ->
                                    val locDto = locSnapshot?.toObject(LocationDto::class.java)
                                    locations[uid] = locDto?.timestamp?.let { ts ->
                                        MemberLocation(lat = locDto.lat, lng = locDto.lng, timestampMillis = ts.toDate().time)
                                    }
                                    emitCurrentState()
                                }
                        }
                    } else {
                        locationListeners.remove(uid)?.remove()
                        locations.remove(uid)
                    }
                }
                emitCurrentState()
            }

        awaitClose {
            membersListener.remove()
            locationListeners.values.forEach { it.remove() }
        }
    }

    override suspend fun getMemberOnce(familyCode: String, uid: String): FamilyMember? {
        val doc = FirestorePaths.memberDoc(firestore, familyCode, uid).get().await()
        val dto = doc.toObject(MemberDto::class.java) ?: return null
        return FamilyMember(
            uid = uid,
            displayName = dto.displayName,
            avatarColor = dto.avatarColor,
            isVisible = dto.isVisible,
            isSelf = false,
            location = null,
        )
    }

    override suspend fun updateProfile(familyCode: String, uid: String, displayName: String, avatarColor: String) {
        FirestorePaths.memberDoc(firestore, familyCode, uid).set(
            mapOf(
                "displayName" to displayName,
                "avatarColor" to avatarColor,
                "profileUpdatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    override suspend fun setVisibility(familyCode: String, uid: String, isVisible: Boolean) {
        FirestorePaths.memberDoc(firestore, familyCode, uid).set(
            mapOf(
                "isVisible" to isVisible,
                "profileUpdatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    override suspend fun writeLocation(familyCode: String, uid: String, lat: Double, lng: Double) {
        FirestorePaths.memberLocationDoc(firestore, familyCode, uid).set(
            mapOf(
                "lat" to lat,
                "lng" to lng,
                "timestamp" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun requestRefresh(familyCode: String, targetUid: String) {
        FirestorePaths.memberDoc(firestore, familyCode, targetUid).set(
            mapOf("refreshRequestedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
    }

    override fun observeOwnRefreshRequests(familyCode: String, uid: String): Flow<Long> = callbackFlow {
        val listener = FirestorePaths.memberDoc(firestore, familyCode, uid)
            .addSnapshotListener { snapshot, _ ->
                val dto = snapshot?.toObject(MemberDto::class.java)
                dto?.refreshRequestedAt?.let { trySend(it.toDate().time) }
            }
        awaitClose { listener.remove() }
    }
}
