package com.college.sweatlock.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.college.sweatlock.blocking.PermissionUtils
import com.college.sweatlock.ui.theme.GlassBackdrop
import com.college.sweatlock.ui.theme.GlassCard
import com.college.sweatlock.ui.theme.VioletAccent

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    var cameraGranted by remember { mutableStateOf(false) }
    var accessibilityGranted by remember { mutableStateOf(false) }
    var usageGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    var batteryUnrestricted by remember { mutableStateOf(false) }

    fun refreshStates() {
        cameraGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        accessibilityGranted = PermissionUtils.isAccessibilityEnabled(context)
        usageGranted = PermissionUtils.isUsageStatsGranted(context)
        overlayGranted = PermissionUtils.canDrawOverlays(context)
        batteryUnrestricted = PermissionUtils.isIgnoringBatteryOptimizations(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshStates() }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshStates() }

    GlassBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Welcome to SweatLock",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Grant these system permissions so SweatLock can detect distracting apps and verify your workouts on-device.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            PermissionTile(
                title = "1. Camera Access",
                description = "Tracks pushups & squats live on-device via ML Kit.",
                granted = cameraGranted,
                onAction = { cameraLauncher.launch(Manifest.permission.CAMERA) }
            )
            PermissionTile(
                title = "2. Accessibility Service",
                description = "Detects when a blocked app opens in the foreground.",
                granted = accessibilityGranted,
                onAction = { PermissionUtils.openAccessibilitySettings(context) }
            )
            PermissionTile(
                title = "3. Usage Access",
                description = "Monitors active app sessions to enforce unlock timers.",
                granted = usageGranted,
                onAction = { PermissionUtils.openUsageAccessSettings(context) }
            )
            PermissionTile(
                title = "4. Display Over Other Apps",
                description = "Launches the full-screen workout lock screen immediately.",
                granted = overlayGranted,
                onAction = { PermissionUtils.openOverlaySettings(context) }
            )
            PermissionTile(
                title = "5. Unrestricted Battery (Recommended)",
                description = "Prevents Android from killing the lock service in the background.",
                granted = batteryUnrestricted,
                onAction = { PermissionUtils.requestIgnoreBatteryOptimizations(context) }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onComplete,
                enabled = cameraGranted && accessibilityGranted && overlayGranted,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Continue to SweatLock", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PermissionTile(
    title: String,
    description: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(description, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
            if (granted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable", fontSize = 12.sp)
                }
            }
        }
    }
}
