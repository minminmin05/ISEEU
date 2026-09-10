package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}/members/{uid}. Needs a no-arg constructor (default values) for Firestore's reflection-based mapping. */
data class MemberDto(
    val displayName: String = "",
    val avatarColor: String = "#3B5BFE",
    val avatarUrl: String? = null, // Firebase Storage download URL, null until the user uploads a photo
    val isVisible: Boolean = true,
    val activityStatus: String? = null, // "driving" | "walking" | "still" | null (unknown yet)
    val refreshRequestedAt: Timestamp? = null,
    val profileUpdatedAt: Timestamp? = null,
)
