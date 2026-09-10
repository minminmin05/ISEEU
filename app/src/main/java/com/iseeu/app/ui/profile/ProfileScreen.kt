package com.iseeu.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iseeu.app.util.AvatarColorPalette
import com.iseeu.app.util.FamilyCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = viewModel::onDisplayNameChanged,
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Avatar color", style = MaterialTheme.typography.labelLarge)
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
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { viewModel.save() }) { Text("Save") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Share my location", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "When off, your family can't see your location at all",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = viewModel.isSharingEnabled,
                    onCheckedChange = { viewModel.setSharingEnabled(it, context) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Family code", style = MaterialTheme.typography.labelLarge)
                Text(FamilyCodeGenerator.format(viewModel.familyCode), style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
