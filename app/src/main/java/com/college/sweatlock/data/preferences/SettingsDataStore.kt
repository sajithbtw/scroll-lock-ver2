package com.college.sweatlock.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.college.sweatlock.workout.model.ExerciseType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sweatlock_settings")

data class UserSettings(
    val targetReps: Int = 10,
    val unlockDurationMinutes: Int = 15,
    val exerciseType: ExerciseType = ExerciseType.PUSHUPS,
    val onboardingCompleted: Boolean = false
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val TARGET_REPS = intPreferencesKey("target_reps")
        val UNLOCK_MINUTES = intPreferencesKey("unlock_minutes")
        val EXERCISE_TYPE = stringPreferencesKey("exercise_type")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            targetReps = prefs[Keys.TARGET_REPS] ?: 10,
            unlockDurationMinutes = prefs[Keys.UNLOCK_MINUTES] ?: 15,
            exerciseType = prefs[Keys.EXERCISE_TYPE]?.let {
                runCatching { ExerciseType.valueOf(it) }.getOrNull()
            } ?: ExerciseType.PUSHUPS,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun updateTargetReps(reps: Int) {
        context.dataStore.edit { it[Keys.TARGET_REPS] = reps }
    }

    suspend fun updateUnlockMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.UNLOCK_MINUTES] = minutes }
    }

    suspend fun updateExerciseType(type: ExerciseType) {
        context.dataStore.edit { it[Keys.EXERCISE_TYPE] = type.name }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }
}
