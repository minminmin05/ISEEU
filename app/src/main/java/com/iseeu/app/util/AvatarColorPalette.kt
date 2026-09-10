package com.iseeu.app.util

object AvatarColorPalette {
    val colors = listOf(
        "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
        "#5C6BC0", "#42A5F5", "#29B6F6", "#26A69A",
        "#66BB6A", "#9CCC65", "#FFA726", "#FF7043",
    )

    /** Deterministic so a member's default color doesn't change across sessions before they ever pick one. */
    fun defaultFor(seed: String): String {
        val index = kotlin.math.abs(seed.hashCode()) % colors.size
        return colors[index]
    }
}
