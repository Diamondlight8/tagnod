package com.example.tagnod.data.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.tagnod.data.local.entity.ActionEntity
import com.example.tagnod.data.local.entity.MacroEntity
import com.example.tagnod.data.local.entity.NfcTagEntity
import com.example.tagnod.data.repository.MacroRepository
import com.example.tagnod.data.repository.PreferencesRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupTagData(
    val tagId: String,
    val tagName: String
)

data class BackupActionData(
    val actionOrder: Int,
    val actionType: String,
    val paramsJson: String
)

data class BackupMacroData(
    val name: String,
    val isEnabled: Boolean = true,
    val triggerType: String = "NFC",
    val targetPackageName: String? = null,
    val targetAppName: String? = null,
    val tags: List<BackupTagData> = emptyList(),
    val actions: List<BackupActionData> = emptyList()
)

data class BackupSettingsData(
    val themeMode: String = "DARK",
    val showToast: Boolean = true
)

data class BackupData(
    val app: String = "TagNod",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: BackupSettingsData = BackupSettingsData(),
    val macros: List<BackupMacroData> = emptyList()
)

enum class ImportMode {
    REPLACE_ALL,
    MERGE
}

object BackupManager {

    private const val TAG = "BackupManager"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportFullBackup(
        context: Context,
        repository: MacroRepository,
        preferencesRepository: PreferencesRepository
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val allMacros = repository.getAllMacros().first()
            val themeMode = preferencesRepository.themeModeFlow.first()
            val showToast = preferencesRepository.showToastFlow.first()

            val backupMacros = allMacros.map { mwd ->
                BackupMacroData(
                    name = mwd.macro.name,
                    isEnabled = mwd.macro.isEnabled,
                    triggerType = mwd.macro.triggerType,
                    targetPackageName = mwd.macro.targetPackageName,
                    targetAppName = mwd.macro.targetAppName,
                    tags = mwd.tags.map { BackupTagData(it.tagId, it.tagName) },
                    actions = mwd.actions.map { BackupActionData(it.actionOrder, it.actionType, it.paramsJson) }
                )
            }

            val backupData = BackupData(
                app = "TagNod",
                version = 1,
                exportedAt = System.currentTimeMillis(),
                settings = BackupSettingsData(themeMode = themeMode, showToast = showToast),
                macros = backupMacros
            )

            val jsonString = gson.toJson(backupData)

            val timeStamp = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date())
            val fileName = "TagNod_backup_$timeStamp.json"

            // Target Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outFile = if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                File(downloadsDir, fileName)
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            }

            outFile.writeText(jsonString)
            Log.d(TAG, "Successfully exported backup to ${outFile.absolutePath}")
            Result.success(outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export backup", e)
            Result.failure(e)
        }
    }

    suspend fun parseBackupUri(
        context: Context,
        uri: Uri
    ): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val jsonString = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Could not read content from backup file."))

            val rootElement = JsonParser.parseString(jsonString)
            if (!rootElement.isJsonObject) {
                return@withContext Result.failure(Exception("Invalid backup file: Root element is not a JSON object."))
            }

            val rootObj = rootElement.asJsonObject
            val appField = rootObj.get("app")?.asString
            val macrosArray = rootObj.getAsJsonArray("macros")

            if (appField != "TagNod" && macrosArray == null) {
                return@withContext Result.failure(Exception("File is not a valid TagNod backup format."))
            }

            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            if (backupData == null) {
                return@withContext Result.failure(Exception("Corrupted or unparseable backup data."))
            }

            Result.success(backupData)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing backup file", e)
            Result.failure(Exception("Corrupted backup file: ${e.localizedMessage ?: "Invalid JSON"}"))
        }
    }

    suspend fun restoreBackup(
        context: Context,
        repository: MacroRepository,
        preferencesRepository: PreferencesRepository,
        backupData: BackupData,
        importMode: ImportMode
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (importMode == ImportMode.REPLACE_ALL) {
                // Delete all current macros
                val current = repository.getAllMacros().first()
                current.forEach { repository.deleteMacro(it.macro) }

                // Restore settings
                preferencesRepository.setThemeMode(backupData.settings.themeMode)
                preferencesRepository.setShowToast(backupData.settings.showToast)
            }

            val existingMacros = if (importMode == ImportMode.MERGE) {
                repository.getAllMacros().first()
            } else {
                emptyList()
            }

            var importedCount = 0

            backupData.macros.forEach { backupMacro ->
                if (importMode == ImportMode.MERGE) {
                    val alreadyExists = existingMacros.any { it.macro.name.equals(backupMacro.name, ignoreCase = true) }
                    if (alreadyExists) return@forEach // Skip duplicate macro names
                }

                val macroEntity = MacroEntity(
                    id = 0L,
                    name = backupMacro.name,
                    isEnabled = backupMacro.isEnabled,
                    triggerType = backupMacro.triggerType,
                    targetPackageName = backupMacro.targetPackageName,
                    targetAppName = backupMacro.targetAppName
                )

                val tagEntities = backupMacro.tags.map {
                    NfcTagEntity(id = 0L, macroId = 0L, tagId = it.tagId, tagName = it.tagName)
                }

                val actionEntities = backupMacro.actions.mapIndexed { idx, act ->
                    ActionEntity(id = 0L, macroId = 0L, actionOrder = idx, actionType = act.actionType, paramsJson = act.paramsJson)
                }

                repository.saveMacro(macroEntity, tagEntities, actionEntities)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            Result.failure(e)
        }
    }

    fun serializeSingleMacroToJson(macroName: String, triggerType: String, targetPackageName: String?, targetAppName: String?, actions: List<BackupActionData>): String {
        val root = JsonObject().apply {
            addProperty("type", "TAGNOD_MACRO")
            addProperty("version", 1)
            addProperty("name", macroName)
            addProperty("triggerType", triggerType)
            if (targetPackageName != null) addProperty("targetPackageName", targetPackageName)
            if (targetAppName != null) addProperty("targetAppName", targetAppName)

            val actionsArr = JsonArray()
            actions.forEach { act ->
                val actObj = JsonObject().apply {
                    addProperty("actionType", act.actionType)
                    addProperty("paramsJson", act.paramsJson)
                }
                actionsArr.add(actObj)
            }
            add("actions", actionsArr)
        }
        return gson.toJson(root)
    }

    fun parseSingleMacroJson(jsonString: String): Result<BackupMacroData> {
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val type = root.get("type")?.asString
            val name = root.get("name")?.asString ?: "Imported Macro"
            val triggerType = root.get("triggerType")?.asString ?: "NFC"
            val targetPackageName = root.get("targetPackageName")?.asString
            val targetAppName = root.get("targetAppName")?.asString
            val actionsArr = root.getAsJsonArray("actions")

            if (type != "TAGNOD_MACRO" && actionsArr == null) {
                return Result.failure(Exception("Not a valid TagNod macro string"))
            }

            val actions = mutableListOf<BackupActionData>()
            actionsArr?.forEachIndexed { index, elem ->
                val obj = elem.asJsonObject
                val actionType = obj.get("actionType")?.asString ?: "WAIT"
                val paramsJson = obj.get("paramsJson")?.asString ?: "{}"
                actions.add(BackupActionData(actionOrder = index, actionType = actionType, paramsJson = paramsJson))
            }

            val backupMacro = BackupMacroData(
                name = name,
                isEnabled = true,
                triggerType = triggerType,
                targetPackageName = targetPackageName,
                targetAppName = targetAppName,
                tags = emptyList(),
                actions = actions
            )

            return Result.success(backupMacro)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
