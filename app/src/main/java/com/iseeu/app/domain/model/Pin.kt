package com.iseeu.app.domain.model

data class Pin(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float,
    val type: PinType,
)

enum class PinType {
    HOME, SCHOOL, WORK, OTHER;

    companion object {
        fun fromFirestoreValue(value: String?): PinType = when (value) {
            "home" -> HOME
            "school" -> SCHOOL
            "work" -> WORK
            else -> OTHER
        }
    }
}

fun PinType.toFirestoreValue(): String = when (this) {
    PinType.HOME -> "home"
    PinType.SCHOOL -> "school"
    PinType.WORK -> "work"
    PinType.OTHER -> "other"
}
