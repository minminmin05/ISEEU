package com.iseeu.app.domain.model

data class FamilyMember(
    val uid: String,
    val displayName: String,
    val avatarColor: String,
    val avatarPhotoBase64: String?,
    val isVisible: Boolean,
    val isSelf: Boolean,
    val activityStatus: ActivityStatus,
    val currentPinId: String?,
    val currentPinEnteredAtMillis: Long?,
    val location: MemberLocation?,
)

data class MemberLocation(
    val lat: Double,
    val lng: Double,
    val timestampMillis: Long,
)

data class HistoryPoint(
    val lat: Double,
    val lng: Double,
    val timestampMillis: Long,
)

enum class ActivityStatus {
    UNKNOWN, STILL, WALKING, DRIVING;

    companion object {
        fun fromFirestoreValue(value: String?): ActivityStatus = when (value) {
            "still" -> STILL
            "walking" -> WALKING
            "driving" -> DRIVING
            else -> UNKNOWN
        }
    }
}
