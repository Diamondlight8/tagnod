package com.example.tagnod.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tagnod.data.local.entity.ActionEntity
import com.example.tagnod.data.local.entity.MacroEntity
import com.example.tagnod.data.local.entity.MacroWithDetails
import com.example.tagnod.data.local.entity.NfcTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {

    @Transaction
    @Query("SELECT * FROM macros ORDER BY createdAt DESC")
    fun getAllMacrosWithDetails(): Flow<List<MacroWithDetails>>

    @Transaction
    @Query("SELECT * FROM macros WHERE id = :macroId")
    suspend fun getMacroWithDetailsById(macroId: Long): MacroWithDetails?

    @Transaction
    @Query("SELECT m.* FROM macros m INNER JOIN nfc_tags t ON m.id = t.macroId WHERE t.tagId = :tagId")
    suspend fun getMacroWithDetailsByTagId(tagId: String): MacroWithDetails?

    @Transaction
    @Query("SELECT * FROM macros WHERE triggerType = 'APP_LAUNCH' AND targetPackageName = :packageName AND isEnabled = 1")
    suspend fun getMacrosByAppLaunchPackage(packageName: String): List<MacroWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity): Long

    @Update
    suspend fun updateMacro(macro: MacroEntity)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :macroId")
    suspend fun deleteMacroById(macroId: Long)

    @Query("UPDATE macros SET lastTriggeredAt = :timestamp WHERE id = :macroId")
    suspend fun updateLastTriggered(macroId: Long, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<NfcTagEntity>)

    @Query("DELETE FROM nfc_tags WHERE macroId = :macroId")
    suspend fun deleteTagsForMacro(macroId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<ActionEntity>)

    @Query("DELETE FROM actions WHERE macroId = :macroId")
    suspend fun deleteActionsForMacro(macroId: Long)

    @Transaction
    suspend fun upsertMacroWithDetails(
        macro: MacroEntity,
        tags: List<NfcTagEntity>,
        actions: List<ActionEntity>
    ): Long {
        val macroId = if (macro.id == 0L) {
            insertMacro(macro)
        } else {
            updateMacro(macro)
            macro.id
        }

        // Replace tags
        deleteTagsForMacro(macroId)
        val updatedTags = tags.map { it.copy(macroId = macroId) }
        insertTags(updatedTags)

        // Replace actions
        deleteActionsForMacro(macroId)
        val updatedActions = actions.mapIndexed { index, action ->
            action.copy(macroId = macroId, actionOrder = index)
        }
        insertActions(updatedActions)

        return macroId
    }
}
