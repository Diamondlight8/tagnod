package com.example.tagnod.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tagnod.data.local.dao.MacroDao
import com.example.tagnod.data.local.entity.ActionEntity
import com.example.tagnod.data.local.entity.MacroEntity
import com.example.tagnod.data.local.entity.NfcTagEntity

@Database(
    entities = [
        MacroEntity::class,
        NfcTagEntity::class,
        ActionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TagNodDatabase : RoomDatabase() {

    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: TagNodDatabase? = null

        fun getInstance(context: Context): TagNodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TagNodDatabase::class.java,
                    "tagnod_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
