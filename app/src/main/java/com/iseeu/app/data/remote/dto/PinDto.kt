package com.iseeu.app.data.remote.dto

import com.google.firebase.Timestamp

/** Mirrors families/{familyCode}/pins/{pinId} — a saved place, e.g. "Home" or "School". */
data class PinDto(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusMeters: Double = 150.0,
    val type: String = "other", // "home" | "school" | "work" | "other"
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
)
