package com.iseeu.app.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.iseeu.app.util.PermissionUtils

@Composable
fun PermissionRationaleScreen(
    viewModel: PermissionFlowViewModel = hiltViewModel(),
    onAllGranted: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.refresh(context) }

    // Coming back from the Settings app (background-location step) needs a re-check on resume.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel.step) {
        if (viewModel.step == PermissionStep.DONE) onAllGranted()
    }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh(context) }

    when (viewModel.step) {
        PermissionStep.FOREGROUND_LOCATION -> PermissionStepContent(
            title = "See your family on the map",
            body = "ISEEU needs your location to show you on the map and share it with your family.",
            onAllow = {
                foregroundLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
        )
        PermissionStep.BACKGROUND_LOCATION -> PermissionStepContent(
            title = "Keep sharing in the background",
            body = "To keep your location up to date even when the app isn't open, allow location access \"All the time\" on the next screen.",
            allowLabel = "Open settings",
            onAllow = { context.startActivity(PermissionUtils.appSettingsIntent(context)) },
        )
        PermissionStep.NOTIFICATIONS -> PermissionStepContent(
            title = "Stay informed",
            body = "ISEEU shows a persistent notification while it's sharing your location, so you always know it's active.",
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.refresh(context)
                }
            },
        )
        PermissionStep.DONE -> Unit
    }
}

@Composable
private fun PermissionStepContent(
    title: String,
    body: String,
    onAllow: () -> Unit,
    allowLabel: String = "Allow",
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text(allowLabel) }
    }
}
