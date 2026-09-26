package com.example.tagnod.domain.executor

import android.content.Context
import android.util.Log
import com.example.tagnod.data.local.entity.MacroWithDetails
import com.example.tagnod.data.repository.MacroRepository
import com.example.tagnod.domain.model.ActionModel
import com.example.tagnod.domain.model.ActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MacroExecutor(
    private val context: Context,
    private val repository: MacroRepository
) {

    private val TAG = "MacroExecutor"
    private val actionHandlers = ActionHandlers(context)

    suspend fun executeMacro(macroWithDetails: MacroWithDetails): Boolean = withContext(Dispatchers.IO) {
        val macro = macroWithDetails.macro
        if (!macro.isEnabled) {
            Log.d(TAG, "Macro '${macro.name}' is disabled. Skipping execution.")
            return@withContext false
        }

        Log.d(TAG, "Starting execution of macro '${macro.name}' with ${macroWithDetails.actions.size} actions.")

        // Sort actions by order
        val sortedActions = macroWithDetails.actions
            .sortedBy { it.actionOrder }
            .map { ActionModel.fromEntity(it) }

        var skipNext = false

        for (action in sortedActions) {
            if (skipNext) {
                Log.d(TAG, "Skipping action ${action.type.name} due to IF_ELSE condition result")
                skipNext = false
                continue
            }

            try {
                if (action.type == ActionType.IF_ELSE) {
                    val conditionMet = actionHandlers.executeAction(action)
                    if (!conditionMet) {
                        skipNext = true
                    }
                } else {
                    val success = actionHandlers.executeAction(action)
                    if (!success) {
                        Log.w(TAG, "Action ${action.type.name} failed mid-chain. Continuing next actions.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception running action ${action.type.name} in macro '${macro.name}'", e)
            }
        }

        // Update last triggered timestamp
        repository.updateLastTriggered(macro.id, System.currentTimeMillis())
        Log.d(TAG, "Finished macro '${macro.name}' execution.")
        true
    }
}
