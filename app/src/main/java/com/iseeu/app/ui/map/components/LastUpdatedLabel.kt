package com.iseeu.app.ui.map.components

import android.text.format.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LastUpdatedLabel(timestampMillis: Long?, modifier: Modifier = Modifier) {
    val text = if (timestampMillis == null) {
        "No location yet"
    } else {
        DateUtils.getRelativeTimeSpanString(
            timestampMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    Text(text = text, style = MaterialTheme.typography.bodySmall, modifier = modifier)
}
