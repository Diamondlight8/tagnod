package com.example.tagnod.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tagnod.data.backup.BackupData
import com.example.tagnod.data.backup.BackupManager
import com.example.tagnod.data.backup.ImportMode
import com.example.tagnod.data.repository.MacroRepository
import com.example.tagnod.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val repository: PreferencesRepository,
    private val macroRepository: MacroRepository
) : ViewModel() {

    val themeModeState: StateFlow<String> = repository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "DARK"
        )

    val accentColorState: StateFlow<String> = repository.accentColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "WHITE"
        )

    val showToastState: StateFlow<Boolean> = repository.showToastFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setAccentColor(colorName: String) {
        viewModelScope.launch {
            repository.setAccentColor(colorName)
        }
    }

    fun setShowToast(show: Boolean) {
        viewModelScope.launch {
            repository.setShowToast(show)
        }
    }

    suspend fun exportBackup(context: Context): Result<File> {
        return BackupManager.exportFullBackup(context, macroRepository, repository)
    }

    suspend fun parseBackupUri(context: Context, uri: Uri): Result<BackupData> {
        return BackupManager.parseBackupUri(context, uri)
    }

    suspend fun restoreBackup(context: Context, backupData: BackupData, mode: ImportMode): Result<Int> {
        return BackupManager.restoreBackup(context, macroRepository, repository, backupData, mode)
    }
}
