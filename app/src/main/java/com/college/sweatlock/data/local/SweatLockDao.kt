package com.college.sweatlock.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SweatLockDao {
    @Query("SELECT * FROM blocked_apps")
    fun observeBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getBlockedApp(pkg: String): BlockedAppEntity?

    @Upsert
    suspend fun upsertBlockedApp(app: BlockedAppEntity)

    @Query("UPDATE blocked_apps SET unlockUntilMs = :unlockUntil WHERE packageName = :pkg")
    suspend fun setUnlockTimestamp(pkg: String, unlockUntil: Long)

    @Insert
    suspend fun insertWorkoutLog(log: WorkoutLogEntity)

    @Query("SELECT COALESCE(SUM(repsCompleted), 0) FROM workout_logs WHERE epochDay = :epochDay")
    fun observeRepsForDay(epochDay: Long): Flow<Int>

    @Query("SELECT DISTINCT epochDay FROM workout_logs ORDER BY epochDay DESC")
    fun observeActiveDays(): Flow<List<Long>>
}
