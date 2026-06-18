package com.javis.launcher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.javis.launcher.service.JavisService
import com.javis.launcher.ui.navigation.JavisNavHost
import com.javis.launcher.ui.theme.JavisTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        JavisService.start(this)
        setContent {
            JavisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JavisTheme.colors.background
                ) {
                    JavisNavHost()
                }
            }
        }
    }

    override fun onBackPressed() {
        // Launchers should not go "back" — stay on home
    }
}
