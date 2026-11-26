package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ViralReelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RefReelsDatabase : RoomDatabase() {
    abstract fun viralReelDao(): ViralReelDao

    companion object {
        @Volatile
        private var INSTANCE: RefReelsDatabase? = null

        fun getInstance(context: Context): RefReelsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RefReelsDatabase::class.java,
                    "refreels_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
