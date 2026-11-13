package com.college.sweatlock.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.sweatlock.data.preferences.UserSettings
import com.college.sweatlock.data.repository.SweatLockRepository
import com.college.sweatlock.ui.theme.GlassCard
import com.college.sweatlock.ui.theme.VioletAccent
import com.college.sweatlock.workout.model.ExerciseType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(repository: SweatLockRepository) {
    val scope = rememberCoroutineScope()
    val settings by repository.settingsFlow.collectAsState(initial = UserSettings())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Workout Rules", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // Exercise Selector
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Exercise Type", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExerciseType.entries.forEach { type ->
                        val selected = settings.exerciseType == type
                        FilterChip(
                            selected = selected,
                            onClick = { scope.launch { repository.settingsDataStore.updateExerciseType(type) } },
                            label = { Text(type.displayName) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Reps Required Slider
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Reps Required: ${settings.targetReps}", color = Color.White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = settings.targetReps.toFloat(),
                    onValueChange = { scope.launch { repository.settingsDataStore.updateTargetReps(it.roundToInt()) } },
                    valueRange = 5f..50f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VioletAccent)
                )
            }
        }

        // Unlock Duration Slider
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Unlock Duration: ${settings.unlockDurationMinutes} min", color = Color.White, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = settings.unlockDurationMinutes.toFloat(),
                    onValueChange = { scope.launch { repository.settingsDataStore.updateUnlockMinutes(it.roundToInt()) } },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = VioletAccent)
                )
            }
        }
    }
}
