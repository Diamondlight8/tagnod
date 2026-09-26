package com.example.tagnod.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tagnod.data.backup.BackupManager
import com.example.tagnod.data.local.entity.ActionEntity
import com.example.tagnod.data.local.entity.MacroEntity
import com.example.tagnod.data.local.entity.MacroWithDetails
import com.example.tagnod.data.repository.MacroRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val repository: MacroRepository) : ViewModel() {

    val macrosState: StateFlow<List<MacroWithDetails>> = repository.getAllMacros()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var recentlyDeletedMacro: MacroWithDetails? = null

    fun deleteMacro(macroWithDetails: MacroWithDetails) {
        recentlyDeletedMacro = macroWithDetails
        viewModelScope.launch {
            repository.deleteMacro(macroWithDetails.macro)
        }
    }

    fun undoDelete() {
        val macroDetails = recentlyDeletedMacro ?: return
        viewModelScope.launch {
            repository.saveMacro(
                macroDetails.macro,
                macroDetails.tags,
                macroDetails.actions
            )
            recentlyDeletedMacro = null
        }
    }

    suspend fun duplicateMacro(macroWithDetails: MacroWithDetails): Long = withContext(Dispatchers.IO) {
        val originalMacro = macroWithDetails.macro
        val newMacro = MacroEntity(
            id = 0L,
            name = "${originalMacro.name} (copy)",
            isEnabled = originalMacro.isEnabled,
            triggerType = originalMacro.triggerType,
            targetPackageName = originalMacro.targetPackageName,
            targetAppName = originalMacro.targetAppName,
            createdAt = System.currentTimeMillis()
        )

        val newActions = macroWithDetails.actions.mapIndexed { idx, act ->
            ActionEntity(
                id = 0L,
                macroId = 0L,
                actionOrder = idx,
                actionType = act.actionType,
                paramsJson = act.paramsJson
            )
        }

        repository.saveMacro(newMacro, emptyList(), newActions)
    }

    suspend fun importMacroFromClipboard(context: Context): Long? = withContext(Dispatchers.IO) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (clipText.isNullOrBlank()) return@withContext null

            val parseResult = BackupManager.parseSingleMacroJson(clipText)
            if (parseResult.isFailure) return@withContext null

            val backupMacro = parseResult.getOrNull() ?: return@withContext null

            val newMacro = MacroEntity(
                id = 0L,
                name = backupMacro.name,
                isEnabled = true,
                triggerType = backupMacro.triggerType,
                targetPackageName = backupMacro.targetPackageName,
                targetAppName = backupMacro.targetAppName,
                createdAt = System.currentTimeMillis()
            )

            val actionEntities = backupMacro.actions.mapIndexed { idx, act ->
                ActionEntity(
                    id = 0L,
                    macroId = 0L,
                    actionOrder = idx,
                    actionType = act.actionType,
                    paramsJson = act.paramsJson
                )
            }

            repository.saveMacro(newMacro, emptyList(), actionEntities)
        } catch (_: Exception) {
            null
        }
    }
}
