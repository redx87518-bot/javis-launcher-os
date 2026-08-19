package com.javis.launcher.ui.appdrawer

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.ui.components.JavisBottomNav
import com.javis.launcher.ui.theme.*

@Composable
fun AppIntelligenceScreen(
    navController: NavController,
    packageName: String,
    viewModel: AppIntelligenceViewModel = hiltViewModel()
) {
    val app by viewModel.app.collectAsState()
    val explanation by viewModel.explanation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(packageName) { viewModel.load(packageName) }

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
            Text("APP INTELLIGENCE", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp))
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (app == null) {
                item { Text("Loading app information...", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim)) }
            } else {
                val a = app!!
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).background(JavisBgElevated, RoundedCornerShape(14.dp))
                                .border(1.dp, JavisGlassBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(a.appName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge.copy(color = JavisRed))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(a.appName, style = MaterialTheme.typography.titleMedium.copy(color = JavisTextPrimary))
                            Text(a.category.ifBlank { "Other" }, style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim))
                        }
                    }
                }

                item {
                    Text("WHAT IT DOES", style = sectionStyle())
                    Spacer(Modifier.height(8.dp))
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = JavisRed, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        }
                    } else if (explanation.isBlank()) {
                        Text("Tap \"Analyze with JAVIS\" to generate an AI explanation.", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextDim))
                    } else {
                        Text(explanation, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.explain() }, colors = ButtonDefaults.buttonColors(containerColor = JavisRed), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze with JAVIS")
                    }
                }

                item {
                    Text("JARVIS CAPABILITIES", style = sectionStyle())
                    Spacer(Modifier.height(8.dp))
                    listOf("Open the app", "Read permitted UI", "Navigate within it", "Draft replies (if supported)").forEach { cap ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = JavisGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(cap, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
                        }
                    }
                    Text(
                        "Security note: JARVIS uses reliable metadata and will not invent permissions or label an app malicious without evidence.",
                        style = MaterialTheme.typography.bodySmall.copy(color = JavisTextDim),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { viewModel.launchApp(a.packageName) }, colors = ButtonDefaults.buttonColors(containerColor = JavisRed), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("OPEN APP")
                        }
                        OutlinedButton(onClick = { navController.navigate("conversation") }, modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, JavisRed.copy(alpha = 0.5f))) {
                            Icon(Icons.Default.Chat, null, tint = JavisRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ASK JARVIS", color = JavisRed)
                        }
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
