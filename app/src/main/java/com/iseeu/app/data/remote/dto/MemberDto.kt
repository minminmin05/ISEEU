package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}/members/{uid}. Needs a no-arg constructor (default values) for Firestore's reflection-based mapping. */
data class MemberDto(
    val displayName: String = "",
    val avatarColor: String = "#3B5BFE",
    val isVisible: Boolean = true,
    val activityStatus: String? = null, // reserved, unused until Phase 3
    val refreshRequestedAt: Timestamp? = null,
    val profileUpdatedAt: Timestamp? = null,
)
