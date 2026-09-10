package com.iseeu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.iseeu.app.navigation.ISEEUNavHost
import com.iseeu.app.ui.common.theme.ISEEUTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ISEEUTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ISEEUNavHost()
                }
            }
        }
    }
}
