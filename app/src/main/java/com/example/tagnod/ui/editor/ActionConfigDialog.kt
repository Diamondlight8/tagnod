package com.example.tagnod.ui.editor

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.tagnod.domain.model.ActionCategory
import com.example.tagnod.domain.model.ActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class AppInfoItem(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

@Composable
fun ActionConfigDialog(
    actionType: ActionType,
    initialParams: Map<String, Any> = emptyMap(),
    onDismissRequest: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val params = remember { mutableStateMapOf<String, String>() }

    var installedApps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
    var isAppPickerOpen by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        initialParams.forEach { (k, v) ->
            params[k] = v.toString()
        }

        if (actionType == ActionType.LAUNCH_APP || actionType == ActionType.KILL_APP || actionType == ActionType.LAUNCH_SHORTCUT) {
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolvedAppList = pm.queryIntentActivities(mainIntent, 0).map { resolveInfo ->
                    AppInfoItem(
                        label = resolveInfo.loadLabel(pm).toString(),
                        packageName = resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(pm)
                    )
                }.sortedBy { it.label.lowercase() }
                withContext(Dispatchers.Main) {
                    installedApps = resolvedAppList
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = actionType.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actionType.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (actionType) {
                    ActionType.SET_DISPLAY_COLOR_MODE -> {
                        Text(
                            text = "Grayscale Mode:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var selectedMode by remember {
                            mutableStateOf((initialParams["mode"] as? String) ?: "ENABLE")
                        }

                        val modeOptions = listOf(
                            "ENABLE" to "Enable Grayscale",
                            "DISABLE" to "Disable Grayscale (Normal Colours)"
                        )

                        modeOptions.forEach { (key, label) ->
                            val isSelected = selectedMode == key ||
                                    (key == "ENABLE" && selectedMode == "GREYSCALE") ||
                                    (key == "DISABLE" && selectedMode == "DISABLED")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMode = key
                                        params["mode"] = key
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedMode = key
                                        params["mode"] = key
                                    }
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    ActionType.LAUNCH_APP, ActionType.KILL_APP, ActionType.LAUNCH_SHORTCUT -> {
                        val currentPkg = params["package_name"] ?: ""
                        val currentLabel = params["app_name"] ?: installedApps.find { it.packageName == currentPkg }?.label ?: currentPkg

                        Text("Selected Application:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentPkg.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconDrawable = installedApps.find { it.packageName == currentPkg }?.icon
                                    if (iconDrawable != null) {
                                        Image(
                                            bitmap = iconDrawable.toBitmap(48, 48).asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column {
                                        Text(
                                            text = currentLabel,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = currentPkg,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            onClick = { isAppPickerOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (currentPkg.isBlank()) "Choose Installed App..." else "Change Selected App")
                        }
                    }

                    ActionType.TOGGLE_WIFI,
                    ActionType.TOGGLE_BLUETOOTH,
                    ActionType.TOGGLE_ROTATION_LOCK,
                    ActionType.TOGGLE_FLASHLIGHT -> {
                        Text("State Mode:", style = MaterialTheme.typography.bodyMedium)
                        var currentState by remember { mutableStateOf(params["state"] ?: "TOGGLE") }

                        listOf("TOGGLE" to "Toggle", "ON" to "Turn On", "OFF" to "Turn Off").forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentState = key
                                        params["state"] = key
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (currentState == key),
                                    onClick = {
                                        currentState = key
                                        params["state"] = key
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    ActionType.TURN_SCREEN_ON_OFF -> {
                        Text("Screen Action:", style = MaterialTheme.typography.bodyMedium)
                        var currentState by remember { mutableStateOf(params["state"] ?: "OFF") }

                        listOf("OFF" to "Turn Screen Off (Lock)", "ON" to "Turn Screen On (Wake)").forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentState = key
                                        params["state"] = key
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (currentState == key),
                                    onClick = {
                                        currentState = key
                                        params["state"] = key
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    ActionType.MUTE_UNMUTE_AUDIO -> {
                        Text("Mute Mode:", style = MaterialTheme.typography.bodyMedium)
                        var currentMode by remember { mutableStateOf(params["mode"] ?: "TOGGLE") }

                        listOf("TOGGLE" to "Toggle Mute", "MUTE" to "Mute Audio", "UNMUTE" to "Unmute Audio").forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentMode = key
                                        params["mode"] = key
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (currentMode == key),
                                    onClick = {
                                        currentMode = key
                                        params["mode"] = key
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    ActionType.TOGGLE_DND -> {
                        Text("DND Filter Mode:", style = MaterialTheme.typography.bodyMedium)
                        var currentFilter by remember { mutableStateOf(params["filter"] ?: "PRIORITY") }

                        listOf("OFF" to "Allow All (Off)", "PRIORITY" to "Priority Only", "ALARMS" to "Alarms Only", "NONE" to "Total Silence").forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentFilter = key
                                        params["filter"] = key
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (currentFilter == key),
                                    onClick = {
                                        currentFilter = key
                                        params["filter"] = key
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }

                    ActionType.SET_MEDIA_VOLUME,
                    ActionType.SET_RINGTONE_VOLUME,
                    ActionType.SET_NOTIFICATION_VOLUME,
                    ActionType.SET_ALARM_VOLUME,
                    ActionType.SET_SCREEN_BRIGHTNESS -> {
                        var currentLevel by remember { mutableStateOf((params["level"]?.toFloatOrNull() ?: 50f)) }
                        Text(
                            text = "Level: ${currentLevel.roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Slider(
                            value = currentLevel,
                            onValueChange = {
                                currentLevel = it
                                params["level"] = it.roundToInt().toString()
                            },
                            valueRange = 0f..100f
                        )
                    }

                    ActionType.TEXT_TO_SPEECH -> {
                        OutlinedTextField(
                            value = params["text"] ?: "",
                            onValueChange = { params["text"] = it },
                            label = { Text("Text to speak") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.VIBRATE -> {
                        OutlinedTextField(
                            value = params["duration"] ?: "500",
                            onValueChange = { params["duration"] = it },
                            label = { Text("Duration (milliseconds)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.OPEN_URL -> {
                        OutlinedTextField(
                            value = params["url"] ?: "",
                            onValueChange = { params["url"] = it },
                            label = { Text("URL (e.g. https://google.com)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.SEND_SMS -> {
                        OutlinedTextField(
                            value = params["phone_number"] ?: "",
                            onValueChange = { params["phone_number"] = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = params["message"] ?: "",
                            onValueChange = { params["message"] = it },
                            label = { Text("Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.MAKE_PHONE_CALL -> {
                        OutlinedTextField(
                            value = params["phone_number"] ?: "",
                            onValueChange = { params["phone_number"] = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.SEND_NOTIFICATION -> {
                        OutlinedTextField(
                            value = params["title"] ?: "",
                            onValueChange = { params["title"] = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = params["body"] ?: "",
                            onValueChange = { params["body"] = it },
                            label = { Text("Body") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.COPY_TO_CLIPBOARD -> {
                        OutlinedTextField(
                            value = params["text"] ?: "",
                            onValueChange = { params["text"] = it },
                            label = { Text("Text to Copy") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.WRITE_TO_FILE, ActionType.APPEND_LOG -> {
                        OutlinedTextField(
                            value = params["file_path"] ?: "",
                            onValueChange = { params["file_path"] = it },
                            label = { Text("File Path") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = params["content"] ?: params["message"] ?: "",
                            onValueChange = {
                                params["content"] = it
                                params["message"] = it
                            },
                            label = { Text("Text Content") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.HTTP_REQUEST -> {
                        OutlinedTextField(
                            value = params["url"] ?: "",
                            onValueChange = { params["url"] = it },
                            label = { Text("Webhook URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = params["method"] ?: "POST",
                            onValueChange = { params["method"] = it.uppercase() },
                            label = { Text("HTTP Method (GET/POST)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = params["body"] ?: "",
                            onValueChange = { params["body"] = it },
                            label = { Text("JSON Body (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.RUN_SHELL_COMMAND -> {
                        Text(
                            text = "Disclaimer: Root commands require root access on the device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = params["command"] ?: "",
                            onValueChange = { params["command"] = it },
                            label = { Text("Shell Command") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.WAIT -> {
                        OutlinedTextField(
                            value = params["seconds"] ?: "1",
                            onValueChange = { params["seconds"] = it },
                            label = { Text("Wait Duration (seconds)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.IF_ELSE -> {
                        Text("Condition Type:", style = MaterialTheme.typography.bodyMedium)
                        var condType by remember { mutableStateOf(params["condition_type"] ?: "BATTERY") }

                        listOf("WIFI" to "Wi-Fi State", "BLUETOOTH" to "Bluetooth State", "BATTERY" to "Battery Level", "TIME" to "Time of Day").forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        condType = key
                                        params["condition_type"] = key
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (condType == key),
                                    onClick = {
                                        condType = key
                                        params["condition_type"] = key
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (condType) {
                            "WIFI" -> {
                                var currentWifiState by remember { mutableStateOf(params["wifi_state"] ?: "CONNECTED") }
                                listOf("CONNECTED" to "Wi-Fi Connected", "ENABLED" to "Wi-Fi On").forEach { (key, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentWifiState = key
                                                params["wifi_state"] = key
                                            }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (currentWifiState == key),
                                            onClick = {
                                                currentWifiState = key
                                                params["wifi_state"] = key
                                            }
                                        )
                                        Text(label, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                            "BATTERY" -> {
                                OutlinedTextField(
                                    value = params["min_battery"] ?: "20",
                                    onValueChange = { params["min_battery"] = it },
                                    label = { Text("Minimum Battery %") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "TIME" -> {
                                OutlinedTextField(
                                    value = params["start_hour"] ?: "8",
                                    onValueChange = { params["start_hour"] = it },
                                    label = { Text("Start Hour (0-23)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = params["end_hour"] ?: "22",
                                    onValueChange = { params["end_hour"] = it },
                                    label = { Text("End Hour (0-23)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "No additional parameters required for this action.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Button(
                        onClick = { onSave(params) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Modal App Picker Dialog
    if (isAppPickerOpen) {
        val filteredApps = remember(appSearchQuery, installedApps) {
            if (appSearchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter {
                    it.label.contains(appSearchQuery, ignoreCase = true) ||
                            it.packageName.contains(appSearchQuery, ignoreCase = true)
                }
            }
        }

        Dialog(
            onDismissRequest = { isAppPickerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 600.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Application",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search installed apps...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching apps found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = filteredApps,
                                key = { it.packageName }
                            ) { app ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            params["package_name"] = app.packageName
                                            params["app_name"] = app.label
                                            isAppPickerOpen = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { isAppPickerOpen = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
