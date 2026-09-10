package com.iseeu.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iseeu.app.R
import com.iseeu.app.ui.map.components.MemberAvatar
import com.iseeu.app.util.AvatarColorPalette
import com.iseeu.app.util.FamilyCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val photoErrorRes = viewModel.photoErrorRes
    LaunchedEffect(photoErrorRes) {
        if (photoErrorRes != null) {
            Toast.makeText(context, photoErrorRes, Toast.LENGTH_SHORT).show()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onPhotoPicked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MemberAvatar(
                        displayName = viewModel.displayName,
                        colorHex = viewModel.avatarColor,
                        avatarUrl = viewModel.avatarUrl,
                        size = 96.dp,
                    )
                    if (viewModel.isUploadingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape),
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.profile_change_photo),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label = { Text(stringResource(R.string.profile_display_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.profile_avatar_color_label), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(AvatarColorPalette.colors) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Surface(
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            color = color,
                            onClick = { viewModel.onAvatarColorChanged(hex) },
                        ) {
                            if (hex == viewModel.avatarColor) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.selected_content_description),
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { viewModel.save() }) { Text(stringResource(R.string.action_save)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.profile_share_location_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.profile_share_location_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = viewModel.isSharingEnabled,
                    onCheckedChange = { viewModel.setSharingEnabled(it, context) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.profile_family_code_label), style = MaterialTheme.typography.labelLarge)
                Text(FamilyCodeGenerator.format(viewModel.familyCode), style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
