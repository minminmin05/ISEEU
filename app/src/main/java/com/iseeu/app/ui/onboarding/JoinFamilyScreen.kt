package com.iseeu.app.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun JoinFamilyScreen(
    // Same Activity-scoped instance as WelcomeScreen — see the note there.
    viewModel: OnboardingViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    onJoined: () -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is OnboardingUiState.Joined) onJoined()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Enter your family code", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = codeInput,
            onValueChange = { codeInput = it },
            label = { Text("XXX-XXX") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val state = viewModel.uiState
        if (state is OnboardingUiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        if (state is OnboardingUiState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { viewModel.joinFamily(codeInput) },
                enabled = codeInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Join family") }
        }
    }
}
