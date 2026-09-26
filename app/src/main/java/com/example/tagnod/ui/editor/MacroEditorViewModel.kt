package com.example.tagnod.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tagnod.data.backup.BackupActionData
import com.example.tagnod.data.backup.BackupManager
import com.example.tagnod.data.local.entity.ActionEntity
import com.example.tagnod.data.local.entity.MacroEntity
import com.example.tagnod.data.local.entity.NfcTagEntity
import com.example.tagnod.data.repository.MacroRepository
import com.example.tagnod.domain.model.ActionModel
import com.example.tagnod.domain.model.ActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MacroEditorUiState(
    val macroId: Long = 0L,
    val name: String = "",
    val triggerType: String = "NFC", // "NFC" or "APP_LAUNCH"
    val targetPackageName: String? = null,
    val targetAppName: String? = null,
    val tags: List<NfcTagEntity> = emptyList(),
    val actions: List<ActionModel> = emptyList(),
    val isSaved: Boolean = false,
    val isNfcWriteDialogOpen: Boolean = false,
    val pendingTagId: String? = null,
    val showNameTagBottomSheet: Boolean = false
)

class MacroEditorViewModel(
    private val repository: MacroRepository,
    private val initialMacroId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(MacroEditorUiState(macroId = initialMacroId))
    val uiState: StateFlow<MacroEditorUiState> = _uiState.asStateFlow()

    init {
        if (initialMacroId != 0L) {
            loadMacro(initialMacroId)
        }
    }

    private fun loadMacro(macroId: Long) {
        viewModelScope.launch {
            val macroWithDetails = repository.getMacroById(macroId) ?: return@launch
            val models = macroWithDetails.actions.map { ActionModel.fromEntity(it) }

            _uiState.update {
                it.copy(
                    name = macroWithDetails.macro.name,
                    triggerType = macroWithDetails.macro.triggerType,
                    targetPackageName = macroWithDetails.macro.targetPackageName,
                    targetAppName = macroWithDetails.macro.targetAppName,
                    tags = macroWithDetails.tags,
                    actions = models
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun setTriggerType(triggerType: String) {
        _uiState.update { it.copy(triggerType = triggerType) }
    }

    fun setTargetApp(packageName: String, appName: String) {
        _uiState.update { it.copy(targetPackageName = packageName, targetAppName = appName) }
    }

    fun addAction(type: ActionType, params: Map<String, String> = emptyMap()) {
        val newAction = ActionModel(type = type, params = params)
        _uiState.update { it.copy(actions = it.actions + newAction) }
    }

    fun updateAction(index: Int, params: Map<String, String>) {
        _uiState.update { state ->
            if (index in state.actions.indices) {
                val updatedList = state.actions.toMutableList()
                val oldAction = updatedList[index]
                updatedList[index] = oldAction.copy(params = params)
                state.copy(actions = updatedList)
            } else {
                state
            }
        }
    }

    fun removeAction(index: Int) {
        _uiState.update { state ->
            if (index in state.actions.indices) {
                val updatedList = state.actions.toMutableList()
                updatedList.removeAt(index)
                state.copy(actions = updatedList)
            } else {
                state
            }
        }
    }

    fun moveAction(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            if (fromIndex in state.actions.indices && toIndex in state.actions.indices) {
                val updatedList = state.actions.toMutableList()
                val item = updatedList.removeAt(fromIndex)
                updatedList.add(toIndex, item)
                state.copy(actions = updatedList)
            } else {
                state
            }
        }
    }

    fun prepareNewNfcTag() {
        val uniqueTagId = "tag_" + UUID.randomUUID().toString().take(8)
        _uiState.update { it.copy(isNfcWriteDialogOpen = true, pendingTagId = uniqueTagId) }
    }

    fun closeNfcWriteDialog() {
        _uiState.update { it.copy(isNfcWriteDialogOpen = false, pendingTagId = null) }
    }

    fun onNfcTagWrittenSuccessfully() {
        _uiState.update { state ->
            state.copy(
                isNfcWriteDialogOpen = false,
                showNameTagBottomSheet = true
            )
        }
    }

    fun confirmAddTag(tagName: String) {
        val pendingId = _uiState.value.pendingTagId ?: return
        val finalName = if (tagName.isBlank()) "Desk Tag" else tagName.trim()
        _uiState.update { state ->
            val newTag = NfcTagEntity(
                id = 0L,
                macroId = state.macroId,
                tagId = pendingId,
                tagName = finalName
            )
            state.copy(
                tags = state.tags + newTag,
                showNameTagBottomSheet = false,
                pendingTagId = null
            )
        }
    }

    fun cancelAddTag() {
        _uiState.update { state ->
            state.copy(
                showNameTagBottomSheet = false,
                pendingTagId = null
            )
        }
    }

    fun removeNfcTag(tag: NfcTagEntity) {
        _uiState.update { state ->
            state.copy(tags = state.tags.filter { it != tag })
        }
    }

    fun copyMacroToClipboard(context: Context) {
        val currentState = _uiState.value
        val macroName = if (currentState.name.isBlank()) "Untitled Macro" else currentState.name.trim()

        val backupActions = currentState.actions.mapIndexed { idx, act ->
            BackupActionData(
                actionOrder = idx,
                actionType = act.type.name,
                paramsJson = act.toEntity().paramsJson
            )
        }

        val jsonString = BackupManager.serializeSingleMacroToJson(
            macroName,
            currentState.triggerType,
            currentState.targetPackageName,
            currentState.targetAppName,
            backupActions
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TagNod Macro", jsonString)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Macro copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun saveMacro() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val macroName = if (currentState.name.isBlank()) "Untitled Macro" else currentState.name.trim()

            val macroEntity = MacroEntity(
                id = currentState.macroId,
                name = macroName,
                triggerType = currentState.triggerType,
                targetPackageName = if (currentState.triggerType == "APP_LAUNCH") currentState.targetPackageName else null,
                targetAppName = if (currentState.triggerType == "APP_LAUNCH") currentState.targetAppName else null
            )

            val tagEntities = if (currentState.triggerType == "NFC") currentState.tags else emptyList()

            val actionEntities = currentState.actions.mapIndexed { index, actionModel ->
                ActionEntity(
                    id = actionModel.id,
                    macroId = currentState.macroId,
                    actionOrder = index,
                    actionType = actionModel.type.name,
                    paramsJson = actionModel.toEntity().paramsJson
                )
            }

            repository.saveMacro(macroEntity, tagEntities, actionEntities)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
