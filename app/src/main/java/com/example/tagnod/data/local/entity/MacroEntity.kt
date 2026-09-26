package com.example.tagnod.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val triggerType: String = "NFC", // "NFC" or "APP_LAUNCH"
    val targetPackageName: String? = null,
    val targetAppName: String? = null,
    val lastTriggeredAt: Long? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
