package com.example.tagnod.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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

class TagNodAccessibilityService : AccessibilityService() {

    private val TAG = "TagNodAccessibility"
    private var lastTriggeredPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore TagNod itself, System UI, and common launchers
            if (packageName == applicationContext.packageName ||
                packageName == "com.android.systemui" ||
                packageName.contains("launcher")
            ) {
                return
            }

            // Debounce: Only fire ONCE per app launch. Reset debounce when switching to a different package.
            if (packageName == lastTriggeredPackage) {
                return
            }

            lastTriggeredPackage = packageName
            Log.d(TAG, "Foreground app changed to: $packageName")

            val database = TagNodDatabase.getInstance(applicationContext)
            val repository = MacroRepository(database.macroDao())
            val prefRepository = PreferencesRepository(applicationContext)
            val executor = MacroExecutor(applicationContext, repository)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val matchingMacros = repository.getMacrosByAppLaunchPackage(packageName)
                    if (matchingMacros.isNotEmpty()) {
                        val showToast = prefRepository.showToastFlow.first()

                        matchingMacros.forEach { macroWithDetails ->
                            withContext(Dispatchers.Main) {
                                if (showToast) {
                                    Toast.makeText(
                                        applicationContext,
                                        "${macroWithDetails.macro.name} activated via App Launch",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            executor.executeMacro(macroWithDetails)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing app launch macro for package $packageName", e)
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        private var instance: TagNodAccessibilityService? = null

        fun isEnabled(): Boolean {
            return instance != null
        }

        fun lockScreen(): Boolean {
            val service = instance ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            return false
        }

        fun takeScreenshot(): Boolean {
            val service = instance ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
            return false
        }

        fun killApp(packageName: String): Boolean {
            val service = instance ?: return false
            service.performGlobalAction(GLOBAL_ACTION_BACK)
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            service.startActivity(intent)
            return true
        }
    }
}
