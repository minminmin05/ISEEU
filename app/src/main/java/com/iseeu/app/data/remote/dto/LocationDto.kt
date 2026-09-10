package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}/members/{uid}/location/current. Split out from MemberDto so Firestore rules can gate it separately (privacy toggle). */
data class LocationDto(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Timestamp? = null,
)
