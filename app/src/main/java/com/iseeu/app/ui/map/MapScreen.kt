package com.iseeu.app.ui.map

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.iseeu.app.domain.model.ActivityStatus
import com.iseeu.app.domain.model.FamilyMember
import com.iseeu.app.domain.model.Pin
import com.iseeu.app.domain.model.PinType
import com.iseeu.app.ui.map.components.LastUpdatedLabel
import com.iseeu.app.ui.map.components.MemberAvatar
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val pins by viewModel.pins.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showFamilySheet by remember { mutableStateOf(false) }
    var focusedMemberUid by remember { mutableStateOf<String?>(null) }
    var showLocationChoice by remember { mutableStateOf(false) }
    var pendingPinLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedPin by remember { mutableStateOf<Pin?>(null) }

    val cameraTarget = remember(focusedMemberUid, members) {
        members.find { it.uid == focusedMemberUid }?.location?.let { GeoPoint(it.lat, it.lng) }
    }

    LaunchedEffect(Unit) { viewModel.ensureTrackingStarted(context) }

    val pinErrorRes = viewModel.pinErrorRes
    LaunchedEffect(pinErrorRes) {
        if (pinErrorRes != null) {
            Toast.makeText(context, pinErrorRes, Toast.LENGTH_SHORT).show()
            viewModel.consumePinError()
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { showFamilySheet = true }) {
                        Icon(Icons.Filled.Groups, contentDescription = stringResource(R.string.family_tab_content_description))
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history_content_description))
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_content_description))
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showLocationChoice = true }) {
                        Icon(Icons.Filled.AddLocation, contentDescription = stringResource(R.string.add_pin_content_description))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            OsmMapView(
                members = members,
                pins = pins,
                onMarkerTap = viewModel::requestRefresh,
                onMapLongPress = { pendingPinLocation = it },
                onPinTap = { selectedPin = it },
                cameraTarget = cameraTarget,
                onCameraTargetHandled = { focusedMemberUid = null },
            )
        }
    }

    if (showFamilySheet) {
        FamilyBottomSheet(
            members = members,
            onSelect = { uid ->
                focusedMemberUid = uid
                showFamilySheet = false
            },
            onDismiss = { showFamilySheet = false },
        )
    }

    if (showLocationChoice) {
        LocationChoiceDialog(
            onUseCurrentLocation = {
                showLocationChoice = false
                val selfLocation = members.find { it.isSelf }?.location
                if (selfLocation != null) {
                    pendingPinLocation = GeoPoint(selfLocation.lat, selfLocation.lng)
                } else {
                    Toast.makeText(context, R.string.add_pin_no_location_yet, Toast.LENGTH_SHORT).show()
                }
            },
            onChooseManually = {
                showLocationChoice = false
                Toast.makeText(context, R.string.add_pin_choose_manually_hint, Toast.LENGTH_LONG).show()
            },
            onDismiss = { showLocationChoice = false },
        )
    }

    pendingPinLocation?.let { point ->
        AddPinDialog(
            onConfirm = { name, type ->
                viewModel.addPin(name, point.latitude, point.longitude, type)
                pendingPinLocation = null
            },
            onDismiss = { pendingPinLocation = null },
        )
    }

    selectedPin?.let { pin ->
        PinDetailDialog(
            pin = pin,
            onDelete = {
                viewModel.deletePin(pin.id)
                selectedPin = null
            },
            onDismiss = { selectedPin = null },
        )
    }
}

@Composable
private fun LocationChoiceDialog(
    onUseCurrentLocation: () -> Unit,
    onChooseManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_pin_choose_location_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUseCurrentLocation, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_pin_use_current_location))
                }
                TextButton(onClick = onChooseManually, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_pin_choose_manually))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Tap a member here to pan the map to their current location — the map itself never scrolls on its own after that. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyBottomSheet(
    members: List<FamilyMember>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            items(members, key = { it.uid }) { member ->
                ListItem(
                    modifier = Modifier.clickable(enabled = member.location != null) { onSelect(member.uid) },
                    leadingContent = {
                        Box {
                            MemberAvatar(
                                displayName = member.displayName,
                                colorHex = member.avatarColor,
                                avatarPhotoBase64 = member.avatarPhotoBase64,
                                size = 40.dp,
                            )
                            ActivityBadge(status = member.activityStatus, modifier = Modifier.align(Alignment.BottomEnd))
                        }
                    },
                    headlineContent = { Text(member.displayName) },
                    supportingContent = {
                        if (!member.isVisible && !member.isSelf) {
                            Text(stringResource(R.string.location_hidden))
                        } else {
                            LastUpdatedLabel(timestampMillis = member.location?.timestampMillis)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AddPinDialog(onConfirm: (String, PinType) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PinType.HOME) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_pin_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.add_pin_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PinType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(pinTypeLabel(type)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, selectedType) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun pinTypeLabel(type: PinType): String = when (type) {
    PinType.HOME -> stringResource(R.string.pin_type_home)
    PinType.SCHOOL -> stringResource(R.string.pin_type_school)
    PinType.WORK -> stringResource(R.string.pin_type_work)
    PinType.OTHER -> stringResource(R.string.pin_type_other)
}

@Composable
private fun PinDetailDialog(pin: Pin, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pin.name) },
        text = { Text(stringResource(R.string.pin_detail_body)) },
        confirmButton = {
            TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ActivityBadge(status: ActivityStatus, modifier: Modifier = Modifier) {
    val icon = when (status) {
        ActivityStatus.DRIVING -> Icons.Filled.DirectionsCar
        ActivityStatus.WALKING -> Icons.Filled.DirectionsWalk
        ActivityStatus.STILL, ActivityStatus.UNKNOWN -> null
    } ?: return

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.size(18.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(3.dp),
        )
    }
}

@Composable
private fun OsmMapView(
    members: List<FamilyMember>,
    pins: List<Pin>,
    onMarkerTap: (String) -> Unit,
    onMapLongPress: (GeoPoint) -> Unit,
    onPinTap: (Pin) -> Unit,
    cameraTarget: GeoPoint?,
    onCameraTargetHandled: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCentered by remember { mutableStateOf(false) }

    // Mutable holder so the long-press callback (captured once, in factory{}) always sees the
    // latest lambda from the current composition instead of a stale one from the first render.
    val onMapLongPressState = remember { mutableStateOf(onMapLongPress) }
    onMapLongPressState.value = onMapLongPress

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
            overlays.add(CopyrightOverlay(context))
            overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            p?.let { onMapLongPressState.value(it) }
                            return true
                        }
                    },
                ),
            )
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

            for (pin in pins) {
                val marker = Marker(view).apply {
                    position = GeoPoint(pin.lat, pin.lng)
                    title = pin.name
                    setAnchor(Marker.ANCHOR_CENTER, PinMarkerFactory.ANCHOR_Y_FRACTION)
                    icon = PinMarkerFactory.build(context, pin)
                    setOnMarkerClickListener { _, _ ->
                        onPinTap(pin)
                        true
                    }
                }
                view.overlays.add(marker)
            }
            view.invalidate()

            if (cameraTarget != null) {
                view.controller.animateTo(cameraTarget)
                view.controller.setZoom(17.0)
                hasCentered = true
                onCameraTargetHandled()
            } else if (!hasCentered && firstPoint != null) {
                view.controller.animateTo(firstPoint)
                hasCentered = true
            }
        },
    )
}
