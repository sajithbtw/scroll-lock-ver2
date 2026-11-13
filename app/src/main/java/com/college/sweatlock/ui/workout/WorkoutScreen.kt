package com.college.sweatlock.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.college.sweatlock.blocking.PermissionUtils
import com.college.sweatlock.ui.theme.GlassCard
import com.college.sweatlock.ui.theme.VioletAccent
import com.college.sweatlock.workout.counter.RepCounter
import com.college.sweatlock.workout.counter.RepState
import com.college.sweatlock.workout.model.ExerciseType
import com.college.sweatlock.workout.vision.PoseAnalyzer
import com.college.sweatlock.workout.vision.PoseFrameResult
import java.util.concurrent.Executors

@Composable
fun WorkoutScreen(
    blockedAppName: String,
    targetReps: Int,
    exerciseType: ExerciseType,
    onWorkoutCompleted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Edge case: Camera permission denied fallback
    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090F))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera Required to Unlock",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "SweatLock counts your ${exerciseType.displayName.lowercase()} on-device using your camera. No video is ever recorded or uploaded.",
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { PermissionUtils.openAppDetailsSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Open App Settings")
                    }
                }
            }
        }
        return
    }

    val repCounter = remember(exerciseType) { RepCounter(exerciseType) }
    var repState by remember { mutableStateOf(RepState()) }
    var latestFrame by remember { mutableStateOf<PoseFrameResult?>(null) }
    var completedTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(repState.count) {
        if (repState.count >= targetReps && !completedTriggered) {
            completedTriggered = true
            onWorkoutCompleted()
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val poseAnalyzer = remember {
        PoseAnalyzer { frameResult ->
            latestFrame = frameResult
            repState = repCounter.processFrame(frameResult.landmarks, System.currentTimeMillis())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            poseAnalyzer.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Live CameraX Feed
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, poseAnalyzer)
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Visual Skeleton Overlay
        SkeletonOverlay(
            landmarks = latestFrame?.landmarks,
            sourceWidth = latestFrame?.sourceWidth ?: 0,
            sourceHeight = latestFrame?.sourceHeight ?: 0,
            isFrontCamera = true,
            isValidBody = repState.isFullBodyVisible
        )

        // Top Glassmorphic Prompt Banner
        GlassCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$blockedAppName is Locked",
                    color = Color(0xFFB794F4),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Do $targetReps ${exerciseType.displayName} to Unlock",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = repState.feedbackMessage,
                    color = if (repState.isFullBodyVisible) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                    fontSize = 14.sp
                )
            }
        }

        // Bottom Rep Counter Pill
        GlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            cornerRadius = 32.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            if (repState.phase.name == "DOWN") Color(0xFF4ADE80) else VioletAccent,
                            CircleShape
                        )
                )
                Text(
                    text = "${repState.count} / $targetReps",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${repState.currentAngle.toInt()}°",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }
    }
}
