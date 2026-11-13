package com.college.sweatlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.college.sweatlock.data.preferences.UserSettings
import com.college.sweatlock.data.repository.SweatLockRepository
import com.college.sweatlock.ui.dashboard.DashboardScreen
import com.college.sweatlock.ui.onboarding.OnboardingScreen
import com.college.sweatlock.ui.settings.SettingsScreen
import com.college.sweatlock.ui.theme.*
import com.college.sweatlock.ui.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: SweatLockRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SweatLockTheme {
                val scope = rememberCoroutineScope()
                val settings by repository.settingsFlow.collectAsState(initial = UserSettings())
                var selectedTab by remember { mutableIntStateOf(0) }
                var testingWorkout by remember { mutableStateOf(false) }

                when {
                    !settings.onboardingCompleted -> {
                        OnboardingScreen(
                            onComplete = {
                                scope.launch {
                                    repository.settingsDataStore.setOnboardingCompleted(true)
                                }
                            }
                        )
                    }
                    testingWorkout -> {
                        WorkoutScreen(
                            blockedAppName = "Practice Mode",
                            targetReps = settings.targetReps,
                            exerciseType = settings.exerciseType,
                            onWorkoutCompleted = { testingWorkout = false }
                        )
                    }
                    else -> {
                        GlassBackdrop {
                            Box(Modifier.fillMaxSize()) {
                                when (selectedTab) {
                                    0 -> DashboardScreen(
                                        repository = repository,
                                        onTestWorkoutClick = { testingWorkout = true }
                                    )
                                    1 -> SettingsScreen(repository = repository)
                                }

                                FloatingGlassDock(
                                    items = listOf(
                                        DockItem("Apps", Icons.Rounded.Home),
                                        DockItem("Rules", Icons.Rounded.Tune)
                                    ),
                                    selectedIndex = selectedTab,
                                    onSelect = { selectedTab = it },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
