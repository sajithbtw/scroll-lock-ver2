package com.college.sweatlock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.college.sweatlock.data.local.BlockedAppEntity
import com.college.sweatlock.data.local.SweatLockDao
import com.college.sweatlock.data.local.WorkoutLogEntity
import com.college.sweatlock.data.preferences.SettingsDataStore
import com.college.sweatlock.workout.model.ExerciseType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isBlocked: Boolean,
    val unlockUntilMs: Long
)

@Singleton
class SweatLockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SweatLockDao,
    val settingsDataStore: SettingsDataStore
) {
    val settingsFlow = settingsDataStore.settingsFlow
    val blockedAppsFlow = dao.observeBlockedApps()

    fun observeTodayReps(): Flow<Int> = dao.observeRepsForDay(LocalDate.now().toEpochDay())

    fun observeStreakDays(): Flow<Int> = dao.observeActiveDays().map { sortedDaysDesc ->
        if (sortedDaysDesc.isEmpty()) return@map 0
        val today = LocalDate.now().toEpochDay()
        var expectedDay = if (sortedDaysDesc.first() == today) today else today - 1
        var streak = 0
        for (day in sortedDaysDesc) {
            if (day == expectedDay) {
                streak++
                expectedDay--
            } else if (day < expectedDay) {
                break
            }
        }
        streak
    }

    suspend fun getLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        resolveInfos
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it != context.packageName }
            .map { pkg ->
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val dbEntry = dao.getBlockedApp(pkg)
                InstalledAppInfo(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    isBlocked = dbEntry?.isBlocked ?: false,
                    unlockUntilMs = dbEntry?.unlockUntilMs ?: 0L
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun toggleAppBlocked(packageName: String, shouldBlock: Boolean) {
        dao.upsertBlockedApp(
            BlockedAppEntity(
                packageName = packageName,
                isBlocked = shouldBlock,
                unlockUntilMs = 0L // Lock immediately upon enabling
            )
        )
    }

    suspend fun completeWorkoutAndUnlock(
        packageName: String,
        repsCompleted: Int,
        durationMinutes: Int,
        exerciseType: ExerciseType
    ) {
        val unlockUntil = System.currentTimeMillis() + (durationMinutes * 60_000L)
        dao.setUnlockTimestamp(packageName, unlockUntil)
        dao.insertWorkoutLog(
            WorkoutLogEntity(
                epochDay = LocalDate.now().toEpochDay(),
                repsCompleted = repsCompleted,
                exerciseType = exerciseType.name
            )
        )
    }

    fun getAppLabel(packageName: String): String {
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}
