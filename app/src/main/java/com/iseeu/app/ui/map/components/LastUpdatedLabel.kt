package com.iseeu.app.ui.map.components

import android.text.format.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.iseeu.app.R

@Composable
fun LastUpdatedLabel(timestampMillis: Long?, modifier: Modifier = Modifier) {
    val text = if (timestampMillis == null) {
        stringResource(R.string.no_location_yet)
    } else {
        DateUtils.getRelativeTimeSpanString(
            timestampMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
    Text(text = text, style = MaterialTheme.typography.bodySmall, modifier = modifier)
}
