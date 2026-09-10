package com.iseeu.app.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.ui.map.components.LastUpdatedLabel
import com.iseeu.app.ui.map.components.MemberAvatar
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onOpenProfile: () -> Unit,
) {
    val members by viewModel.members.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenProfile) {
                Icon(Icons.Filled.Person, contentDescription = "Profile")
            }
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                MemberRosterRow(members = members, onMemberTap = viewModel::requestRefresh)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            OsmMapView(members = members, onMarkerTap = viewModel::requestRefresh)
        }
    }
}

/**
 * Always-visible roster strip: this is where "last updated: X ago" and hidden members actually
 * surface — a hidden member never gets a map marker (no location to place one at), so without this
 * they'd disappear from the app entirely instead of showing as present-but-hidden.
 */
@Composable
private fun MemberRosterRow(
    members: List<FamilyMember>,
    onMemberTap: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(members, key = { it.uid }) { member ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                onClick = { onMemberTap(member.uid) },
            ) {
                Column(
                    modifier = Modifier.padding(10.dp).width(84.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MemberAvatar(displayName = member.displayName, colorHex = member.avatarColor, size = 40.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = member.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (!member.isVisible && !member.isSelf) {
                        Text(
                            text = "Location hidden",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    } else {
                        LastUpdatedLabel(timestampMillis = member.location?.timestampMillis)
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMapView(
    members: List<FamilyMember>,
    onMarkerTap: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCentered by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
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
            view.overlays.removeAll(view.overlays.filterIsInstance<Marker>())

            var firstPoint: GeoPoint? = null
            for (member in members) {
                val location = member.location ?: continue
                val point = GeoPoint(location.lat, location.lng)
                if (firstPoint == null) firstPoint = point

                val marker = Marker(view).apply {
                    position = point
                    title = member.displayName
                    setAnchor(Marker.ANCHOR_CENTER, MemberMarkerFactory.ANCHOR_Y_FRACTION)
                    icon = MemberMarkerFactory.build(context, member)
                    setOnMarkerClickListener { _, _ ->
                        onMarkerTap(member.uid)
                        true
                    }
                }
                view.overlays.add(marker)
            }
            view.invalidate()

            if (!hasCentered && firstPoint != null) {
                view.controller.animateTo(firstPoint)
                hasCentered = true
            }
        },
    )
}
