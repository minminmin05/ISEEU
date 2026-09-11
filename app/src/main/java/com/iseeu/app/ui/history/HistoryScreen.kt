package com.iseeu.app.ui.history

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.draw.clip
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
import com.iseeu.app.domain.model.Trip
import com.iseeu.app.ui.map.components.MemberAvatar
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val selectedUid by viewModel.selectedUid.collectAsStateWithLifecycle()
    val selectedMember by viewModel.selectedMember.collectAsStateWithLifecycle()
    val pins by viewModel.pins.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()

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

            val currentPinName = selectedMember?.currentPinId?.let { id -> pins.find { it.id == id }?.name }
            val currentPinSince = selectedMember?.currentPinEnteredAtMillis

            if (trips.isEmpty() && currentPinName == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (currentPinName != null && currentPinSince != null) {
                        item(key = "current_status") { CurrentStatusCard(pinName = currentPinName, sinceMillis = currentPinSince) }
                    }
                    items(trips, key = { it.startTimeMillis }) { trip -> TripCard(trip) }
                }
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
                    MemberAvatar(
                        displayName = member.displayName,
                        colorHex = member.avatarColor,
                        avatarPhotoBase64 = member.avatarPhotoBase64,
                        size = 28.dp,
                    )
                    Text(member.displayName, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(pinName: String, sinceMillis: Long) {
    val context = LocalContext.current
    val sinceText = remember(sinceMillis) { DateFormat.getTimeFormat(context).format(Date(sinceMillis)) }
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(stringResource(R.string.history_currently_at, pinName), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.history_since, sinceText), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TripCard(trip: Trip) {
    val context = LocalContext.current
    val timeFormat = remember { DateFormat.getTimeFormat(context) }
    val startText = remember(trip.startTimeMillis) { timeFormat.format(Date(trip.startTimeMillis)) }
    val endText = remember(trip.endTimeMillis) { timeFormat.format(Date(trip.endTimeMillis)) }
    val durationMinutes = remember(trip) { ((trip.endTimeMillis - trip.startTimeMillis) / 60_000L).coerceAtLeast(1) }
    val distanceKm = remember(trip) { trip.distanceMeters / 1000.0 }

    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (trip.isDriving) Icons.Filled.DirectionsCar else Icons.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.trip_distance_km, distanceKm),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                stringResource(R.string.trip_time_range, startText, endText, durationMinutes),
                style = MaterialTheme.typography.bodySmall,
            )
            TripMiniMap(trip)
        }
    }
}

@Composable
private fun TripMiniMap(trip: Trip) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(false)
            // Purely a decorative preview inside a scrolling list — swallow touches so a drag
            // here scrolls the list instead of panning this tiny map.
            setOnTouchListener { _, _ -> true }
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
        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
        factory = { mapView },
        update = { view ->
            view.overlays.removeAll(view.overlays.filterIsInstance<Polyline>() + view.overlays.filterIsInstance<Marker>())

            val geoPoints = trip.points.map { GeoPoint(it.lat, it.lng) }
            view.overlays.add(
                Polyline(view).apply {
                    setPoints(geoPoints)
                    outlinePaint.color = android.graphics.Color.parseColor("#3B5BFE")
                    outlinePaint.strokeWidth = 8f
                },
            )
            view.overlays.add(Marker(view).apply { position = geoPoints.first() })
            view.overlays.add(Marker(view).apply { position = geoPoints.last() })
            view.invalidate()

            // zoomToBoundingBox needs the view to already have a measured size — defer one frame.
            view.post {
                runCatching { view.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), false, 24) }
            }
        },
    )
}
