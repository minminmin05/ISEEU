package com.iseeu.app.ui.onboarding

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iseeu.app.util.FamilyCodeGenerator

@Composable
fun CreateFamilyScreen(
    // Same Activity-scoped instance as WelcomeScreen — see the note there.
    viewModel: OnboardingViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    onContinue: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.uiState is OnboardingUiState.Idle) viewModel.createFamily()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = viewModel.uiState) {
            is OnboardingUiState.FamilyCreated -> {
                Text("Your family is ready", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Share this code with your family so they can join",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Text(FamilyCodeGenerator.format(state.code), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = { shareCode(context, state.code) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Share code")
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }
            is OnboardingUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.createFamily() }) { Text("Try again") }
            }
            else -> CircularProgressIndicator()
        }
    }
}

private fun shareCode(context: Context, code: String) {
    val formatted = FamilyCodeGenerator.format(code)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Join our family on ISEEU! Use this code: $formatted")
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
