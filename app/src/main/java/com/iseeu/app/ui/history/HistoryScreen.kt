package com.iseeu.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iseeu.app.R
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.domain.model.HistoryPoint
import com.iseeu.app.ui.map.components.MemberAvatar
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val selectedUid by viewModel.selectedUid.collectAsStateWithLifecycle()
    val points by viewModel.historyPoints.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MemberPickerRow(members = members, selectedUid = selectedUid, onSelect = viewModel::selectMember)
            if (points.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty))
                }
            } else {
                HistoryMap(points = points)
            }
        }
    }
}

@Composable
private fun MemberPickerRow(
    members: List<FamilyMember>,
    selectedUid: String?,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(members, key = { it.uid }) { member ->
            val isSelected = member.uid == selectedUid
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onSelect(member.uid) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MemberAvatar(displayName = member.displayName, colorHex = member.avatarColor, size = 28.dp)
                    Text(member.displayName, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun HistoryMap(points: List<HistoryPoint>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val startLabel = stringResource(R.string.history_marker_start)
    val latestLabel = stringResource(R.string.history_marker_latest)

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlays.add(CopyrightOverlay(context))
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            view.overlays.removeAll(view.overlays.filterIsInstance<Polyline>() + view.overlays.filterIsInstance<Marker>())

            // History points arrive newest-first (see observeHistory's ORDER BY DESC); a path
            // line wants chronological order, oldest to newest.
            val chronological = points.sortedBy { it.timestampMillis }
            val geoPoints = chronological.map { GeoPoint(it.lat, it.lng) }

            if (geoPoints.isNotEmpty()) {
                view.overlays.add(
                    Polyline(view).apply {
                        setPoints(geoPoints)
                        outlinePaint.color = android.graphics.Color.parseColor("#3B5BFE")
                        outlinePaint.strokeWidth = 8f
                    },
                )
                view.overlays.add(Marker(view).apply { position = geoPoints.first(); title = startLabel })
                view.overlays.add(Marker(view).apply { position = geoPoints.last(); title = latestLabel })
                view.controller.setZoom(15.0)
                view.controller.setCenter(geoPoints.last())
            }
            view.invalidate()
        },
    )
}
