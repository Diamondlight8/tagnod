package com.example.tagnod.data.repository

import com.example.tagnod.data.local.dao.MacroDao
import com.example.tagnod.data.local.entity.ActionEntity
import com.example.tagnod.data.local.entity.MacroEntity
import com.example.tagnod.data.local.entity.MacroWithDetails
import com.example.tagnod.data.local.entity.NfcTagEntity
import kotlinx.coroutines.flow.Flow

class MacroRepository(private val macroDao: MacroDao) {

    fun getAllMacros(): Flow<List<MacroWithDetails>> {
        return macroDao.getAllMacrosWithDetails()
    }

    suspend fun getMacroById(macroId: Long): MacroWithDetails? {
        return macroDao.getMacroWithDetailsById(macroId)
    }

    suspend fun getMacroByTagId(tagId: String): MacroWithDetails? {
        return macroDao.getMacroWithDetailsByTagId(tagId)
    }

    suspend fun getMacrosByAppLaunchPackage(packageName: String): List<MacroWithDetails> {
        return macroDao.getMacrosByAppLaunchPackage(packageName)
    }

    suspend fun saveMacro(
        macro: MacroEntity,
        tags: List<NfcTagEntity>,
        actions: List<ActionEntity>
    ): Long {
        return macroDao.upsertMacroWithDetails(macro, tags, actions)
    }

    suspend fun deleteMacro(macro: MacroEntity) {
        macroDao.deleteMacro(macro)
    }

    suspend fun deleteMacroById(macroId: Long) {
        macroDao.deleteMacroById(macroId)
    }

    suspend fun updateLastTriggered(macroId: Long, timestamp: Long = System.currentTimeMillis()) {
        macroDao.updateLastTriggered(macroId, timestamp)
    }
}
