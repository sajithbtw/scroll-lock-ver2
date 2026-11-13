package com.college.sweatlock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val isBlocked: Boolean = true,
    val unlockUntilMs: Long = 0L
)
