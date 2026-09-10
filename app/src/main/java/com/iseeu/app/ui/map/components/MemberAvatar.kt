package com.iseeu.app.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MemberAvatar(
    displayName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val color = remember(colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color.Gray)
    }
    val initials = remember(displayName) {
        displayName.trim().split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "?" }
    }

    Box(
        modifier = modifier.size(size).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = Color.White, fontSize = (size.value / 2.5).sp)
    }
}
