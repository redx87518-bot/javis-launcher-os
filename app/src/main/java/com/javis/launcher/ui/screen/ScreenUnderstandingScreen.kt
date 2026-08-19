package com.javis.launcher.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.ui.components.JavisBottomNav
import com.javis.launcher.ui.theme.*

@Composable
fun ScreenUnderstandingScreen(
    navController: NavController,
    viewModel: ScreenUnderstandingViewModel = hiltViewModel()
) {
    val description by viewModel.description.collectAsState()
    val interpretation by viewModel.interpretation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasService by viewModel.hasService.collectAsState()

    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JavisBgCard)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = JavisRed,
                modifier = Modifier.clickable { navController.popBackStack() }.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text("SCREEN UNDERSTANDING", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp, color = JavisRed))
        }

        if (!hasService) {
            Box(Modifier.fillMaxWidth().padding(16.dp).background(JavisRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                Text("Accessibility is off. Enable JAVIS in Settings → Accessibility to read the screen. Tap Capture to retry.", style = MaterialTheme.typography.bodySmall.copy(color = JavisGold))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
            Button(onClick = { viewModel.capture() }, colors = ButtonDefaults.buttonColors(containerColor = JavisRed), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Capture screen")
            }
            OutlinedButton(onClick = { viewModel.interpret() }, enabled = description.isNotBlank() && !isLoading,
                border = BorderStroke(1.dp, JavisRed.copy(alpha = 0.5f)), modifier = Modifier.weight(1f)) {
                if (isLoading) CircularProgressIndicator(color = JavisRed, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.AutoAwesome, null, tint = JavisRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Interpret", color = JavisRed)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (interpretation.isNotBlank()) {
                item {
                    Text("JARVIS INTERPRETATION", style = sectionStyle())
                    Spacer(Modifier.height(8.dp))
                    Text(interpretation, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
                }
            }
            if (description.isNotBlank()) {
                item {
                    Text("RAW SCREEN TREE", style = sectionStyle())
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(JavisBgCard, RoundedCornerShape(10.dp))
                            .border(1.dp, JavisGlassBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(description, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextSecondary), textAlign = TextAlign.Start)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    JavisBottomNav(navController = navController, currentRoute = currentRoute)
}

@Composable
private fun sectionStyle() = MaterialTheme.typography.labelSmall.copy(color = JavisTextSecondary, letterSpacing = 2.sp)
