package com.example.tagnod.service

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.tagnod.data.local.TagNodDatabase
import com.example.tagnod.data.repository.MacroRepository
import com.example.tagnod.data.repository.PreferencesRepository
import com.example.tagnod.domain.executor.MacroExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NfcHandlerActivity : Activity() {

    private val TAG = "NfcHandlerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentData = intent?.data
        Log.d(TAG, "NfcHandlerActivity intercepted intent: action=${intent?.action}, data=$intentData")

        val tagId = intentData?.getQueryParameter("id")

        if (tagId.isNullOrEmpty()) {
            finish()
            return
        }

        val database = TagNodDatabase.getInstance(applicationContext)
        val repository = MacroRepository(database.macroDao())
        val prefRepository = PreferencesRepository(applicationContext)
        val executor = MacroExecutor(applicationContext, repository)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val macroWithDetails = repository.getMacroByTagId(tagId)
                val showToast = prefRepository.showToastFlow.first()

                withContext(Dispatchers.Main) {
                    if (macroWithDetails != null) {
                        if (showToast) {
                            Toast.makeText(
                                applicationContext,
                                "${macroWithDetails.macro.name} activated",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Tag not registered in TagNod",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                if (macroWithDetails != null) {
                    executor.executeMacro(macroWithDetails)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing macro for tagId $tagId", e)
            } finally {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }
}
