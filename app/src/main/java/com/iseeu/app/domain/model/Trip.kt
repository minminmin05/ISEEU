package com.iseeu.app.domain.model

data class Trip(
    val points: List<HistoryPoint>,
    val distanceMeters: Double,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isDriving: Boolean,
)
