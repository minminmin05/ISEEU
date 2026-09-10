package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}/members/{uid}/history/{entryId}. */
data class HistoryEntryDto(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Timestamp? = null,
)
