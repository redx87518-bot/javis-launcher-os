package com.javis.launcher.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.javis.launcher.ui.home.components.AiCore
import com.javis.launcher.data.model.CoreState
import com.javis.launcher.ui.theme.*

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(0) }
    var userName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("Sir") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisBg),
        contentAlignment = Alignment.Center
    ) {
        // Background grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = Color(0x0A4488FF)
            val cellSize = 60.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 0.5f)
                x += cellSize
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 0.5f)
                y += cellSize
            }
        }

        AnimatedContent(targetState = step, transitionSpec = {
            fadeIn(tween(400)) + slideInHorizontally { it } togetherWith
            fadeOut(tween(400)) + slideOutHorizontally { -it }
        }, label = "step") { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep(onNext = { step = 1 })
                1 -> NameStep(
                    name = userName,
                    nickname = nickname,
                    onNameChange = { userName = it },
                    onNicknameChange = { nickname = it },
                    onNext = {
                        viewModel.saveProfile(userName, nickname)
                        step = 2
                    }
                )
                2 -> PermissionsStep(onNext = { step = 3 })
                3 -> ReadyStep(onFinish = {
                    viewModel.completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        AiCore(state = CoreState.IDLE)

        Text(
            "JAVIS",
            style = MaterialTheme.typography.displayLarge.copy(
                color = JavisRed,
                letterSpacing = 12.sp
            )
        )
        Text(
            "LAUNCHER OS",
            style = MaterialTheme.typography.titleLarge.copy(
                color = JavisTextSecondary,
                letterSpacing = 6.sp
            )
        )
        Text(
            "Your AI Companion. Your Mission Control.\nAlways ready, Sir.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = JavisTextSecondary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = JavisRed),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("INITIALIZE JAVIS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, null)
        }
    }
}

@Composable
fun NameStep(
    name: String,
    nickname: String,
    onNameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(Icons.Default.Person, null, tint = JavisRed, modifier = Modifier.size(48.dp))
        Text("IDENTIFY YOURSELF", style = MaterialTheme.typography.titleLarge.copy(color = JavisTextPrimary, letterSpacing = 3.sp))
        Text("JAVIS needs to know who you are.", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary, textAlign = TextAlign.Center))

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Your Name") },
            placeholder = { Text("e.g. Tony Stark") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JavisRed, unfocusedBorderColor = JavisGlassBorder,
                focusedLabelColor = JavisRed, unfocusedLabelColor = JavisTextDim,
                focusedTextColor = JavisTextPrimary, unfocusedTextColor = JavisTextPrimary, cursorColor = JavisRed
            )
        )

        OutlinedTextField(
            value = nickname, onValueChange = onNicknameChange,
            label = { Text("JAVIS calls you") },
            placeholder = { Text("e.g. Sir, Boss, Commander") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JavisRed, unfocusedBorderColor = JavisGlassBorder,
                focusedLabelColor = JavisRed, unfocusedLabelColor = JavisTextDim,
                focusedTextColor = JavisTextPrimary, unfocusedTextColor = JavisTextPrimary, cursorColor = JavisRed
            )
        )

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = JavisRed),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("CONTINUE", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp))
        }
    }
}

@Composable
fun PermissionsStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(Icons.Default.Security, null, tint = JavisRed, modifier = Modifier.size(48.dp))
        Text("SYSTEM ACCESS", style = MaterialTheme.typography.titleLarge.copy(color = JavisTextPrimary, letterSpacing = 3.sp))
        Text("JAVIS requires certain permissions to operate at full capacity.", style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextSecondary, textAlign = TextAlign.Center))

        Spacer(Modifier.height(8.dp))

        val permissions = listOf(
            Pair(Icons.Default.Mic, "Microphone — Voice activation"),
            Pair(Icons.Default.Contacts, "Contacts — Call & message assistance"),
            Pair(Icons.Default.Notifications, "Notifications — Smart summaries"),
            Pair(Icons.Default.Phone, "Phone — Calling assistance"),
            Pair(Icons.Default.Accessibility, "Accessibility — App automation")
        )

        permissions.forEach { (icon, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JavisBgCard, RoundedCornerShape(10.dp))
                    .border(1.dp, JavisGlassBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = JavisRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium.copy(color = JavisTextPrimary))
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = JavisRed),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GRANT ACCESS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp))
        }
    }
}

@Composable
fun ReadyStep(onFinish: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ready")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        AiCore(state = CoreState.COMPLETED, modifier = Modifier.scale(scale))

        Text("JAVIS ONLINE", style = MaterialTheme.typography.headlineLarge.copy(color = JavisGreen, letterSpacing = 4.sp))
        Text(
            "All systems operational.\nReady to assist, Sir.",
            style = MaterialTheme.typography.bodyLarge.copy(color = JavisTextSecondary, textAlign = TextAlign.Center)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = JavisRed),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.RocketLaunch, null)
            Spacer(Modifier.width(8.dp))
            Text("LAUNCH JAVIS", style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp))
        }
    }
}
