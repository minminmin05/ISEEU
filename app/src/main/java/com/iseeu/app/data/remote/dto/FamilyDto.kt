package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}. Needs a no-arg constructor (default values) for Firestore's reflection-based mapping. */
data class FamilyDto(
    val createdAt: Timestamp? = null,
    val createdBy: String = "",
)
