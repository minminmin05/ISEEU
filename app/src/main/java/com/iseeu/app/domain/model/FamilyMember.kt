package com.iseeu.app.domain.model

data class FamilyMember(
    val uid: String,
    val displayName: String,
    val avatarColor: String,
    val isVisible: Boolean,
    val isSelf: Boolean,
    val location: MemberLocation?,
)

data class MemberLocation(
    val lat: Double,
    val lng: Double,
    val timestampMillis: Long,
)
