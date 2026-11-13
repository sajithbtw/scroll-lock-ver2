package com.college.sweatlock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BlockedAppEntity::class, WorkoutLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SweatLockDatabase : RoomDatabase() {
    abstract fun dao(): SweatLockDao

    companion object {
        @Volatile
        private var INSTANCE: SweatLockDatabase? = null

        fun getInstance(context: Context): SweatLockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SweatLockDatabase::class.java,
                    "sweatlock.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
