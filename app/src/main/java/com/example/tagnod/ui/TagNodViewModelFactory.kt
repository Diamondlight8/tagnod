package com.example.tagnod.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tagnod.data.local.TagNodDatabase
import com.example.tagnod.data.repository.MacroRepository
import com.example.tagnod.data.repository.PreferencesRepository
import com.example.tagnod.ui.editor.MacroEditorViewModel
import com.example.tagnod.ui.home.HomeViewModel
import com.example.tagnod.ui.settings.SettingsViewModel

class TagNodViewModelFactory(
    private val context: Context,
    private val macroId: Long = 0L
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = TagNodDatabase.getInstance(context)
        val repository = MacroRepository(database.macroDao())
        val preferencesRepository = PreferencesRepository(context)

        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(MacroEditorViewModel::class.java) -> {
                MacroEditorViewModel(repository, macroId) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(preferencesRepository, repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
        }
    }
}
