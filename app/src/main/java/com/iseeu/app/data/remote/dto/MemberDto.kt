package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}/members/{uid}. Needs a no-arg constructor (default values) for Firestore's reflection-based mapping. */
data class MemberDto(
    val displayName: String = "",
    val avatarColor: String = "#3B5BFE",
    val avatarPhotoBase64: String? = null, // small (256x256) JPEG, Base64-encoded; null until the user uploads a photo
    val isVisible: Boolean = true,
    val activityStatus: String? = null, // "driving" | "walking" | "still" | null (unknown yet)
    val currentPinId: String? = null, // set while inside a saved place's geofence, null otherwise
    val currentPinEnteredAt: Timestamp? = null,
    val refreshRequestedAt: Timestamp? = null,
    val profileUpdatedAt: Timestamp? = null,
)
