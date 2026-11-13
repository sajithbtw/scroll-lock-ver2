package com.college.sweatlock.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.college.sweatlock.data.repository.SweatLockRepository
import com.college.sweatlock.ui.theme.SweatLockTheme
import com.college.sweatlock.ui.workout.WorkoutScreen
import com.college.sweatlock.workout.model.ExerciseType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }

    @Inject
    lateinit var repository: SweatLockRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: run {
            finish()
            return
        }

        // Pressing Back exits to Home Launcher instead of bypassing the lock
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })

        val appLabel = repository.getAppLabel(blockedPackage)

        setContent {
            SweatLockTheme {
                var targetReps by remember { mutableIntStateOf(10) }
                var unlockMinutes by remember { mutableIntStateOf(15) }
                var exerciseType by remember { mutableStateOf(ExerciseType.PUSHUPS) }

                LaunchedEffect(Unit) {
                    val settings = repository.settingsFlow.first()
                    targetReps = settings.targetReps
                    unlockMinutes = settings.unlockDurationMinutes
                    exerciseType = settings.exerciseType
                }

                WorkoutScreen(
                    blockedAppName = appLabel,
                    targetReps = targetReps,
                    exerciseType = exerciseType,
                    onWorkoutCompleted = {
                        lifecycleScope.launch {
                            repository.completeWorkoutAndUnlock(
                                packageName = blockedPackage,
                                repsCompleted = targetReps,
                                durationMinutes = unlockMinutes,
                                exerciseType = exerciseType
                            )
                            finish() // Returns user directly into the now-unlocked app
                        }
                    }
                )
            }
        }
    }
}
