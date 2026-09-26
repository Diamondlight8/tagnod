package com.example.tagnod.domain.executor

import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tagnod.domain.model.ActionModel
import com.example.tagnod.domain.model.ActionType
import com.example.tagnod.service.TagNodAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ActionHandlers(private val context: Context) {

    private val TAG = "ActionHandlers"
    private var tts: TextToSpeech? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS Initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating TTS instance", e)
        }
    }

    suspend fun executeAction(action: ActionModel): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing action: ${action.type.name} with params: ${action.params}")
            when (action.type) {
                ActionType.TOGGLE_WIFI -> handleToggleWifi(action)
                ActionType.TOGGLE_BLUETOOTH -> handleToggleBluetooth(action)
                ActionType.TOGGLE_MOBILE_DATA -> handleToggleMobileData()
                ActionType.TOGGLE_AIRPLANE_MODE -> handleToggleAirplaneMode()
                ActionType.TOGGLE_HOTSPOT -> handleToggleHotspot()
                ActionType.TOGGLE_LOCATION -> handleToggleLocation()

                ActionType.SET_MEDIA_VOLUME -> handleSetVolume(action, AudioManager.STREAM_MUSIC)
                ActionType.SET_RINGTONE_VOLUME -> handleSetVolume(action, AudioManager.STREAM_RING)
                ActionType.SET_NOTIFICATION_VOLUME -> handleSetVolume(action, AudioManager.STREAM_NOTIFICATION)
                ActionType.SET_ALARM_VOLUME -> handleSetVolume(action, AudioManager.STREAM_ALARM)
                ActionType.MUTE_UNMUTE_AUDIO -> handleMuteUnmuteAudio(action)
                ActionType.TOGGLE_DND -> handleToggleDnd(action)
                ActionType.PLAY_SOUND -> handlePlaySound(action)
                ActionType.TEXT_TO_SPEECH -> handleTextToSpeech(action)
                ActionType.VIBRATE -> handleVibrate(action)

                ActionType.SET_SCREEN_BRIGHTNESS -> handleSetBrightness(action)
                ActionType.TOGGLE_ROTATION_LOCK -> handleToggleRotationLock(action)
                ActionType.TURN_SCREEN_ON_OFF -> handleTurnScreenOnOff(action)
                ActionType.SET_SCREEN_TIMEOUT -> handleSetScreenTimeout(action)
                ActionType.TOGGLE_FLASHLIGHT -> handleToggleFlashlight(action)
                ActionType.TAKE_SCREENSHOT -> handleTakeScreenshot()

                ActionType.SET_DISPLAY_COLOR_MODE -> handleSetDisplayColorMode(action)

                ActionType.LAUNCH_APP -> handleLaunchApp(action)
                ActionType.OPEN_URL -> handleOpenUrl(action)
                ActionType.LAUNCH_SHORTCUT -> handleLaunchShortcut(action)

                ActionType.SEND_SMS -> handleSendSms(action)
                ActionType.MAKE_PHONE_CALL -> handleMakePhoneCall(action)
                ActionType.SEND_NOTIFICATION -> handleSendNotification(action)

                ActionType.COPY_TO_CLIPBOARD -> handleCopyToClipboard(action)
                ActionType.PASTE_CLIPBOARD -> handlePasteClipboard()

                ActionType.WRITE_TO_FILE -> handleWriteToFile(action)
                ActionType.APPEND_LOG -> handleAppendLog(action)

                ActionType.HTTP_REQUEST -> handleHttpRequest(action)
                ActionType.RUN_SHELL_COMMAND -> handleRunShellCommand(action)
                ActionType.KILL_APP -> handleKillApp(action)
                ActionType.CLEAR_NOTIFICATIONS -> handleClearNotifications()

                ActionType.WAIT -> handleWait(action)
                ActionType.IF_ELSE -> return@withContext handleIfElseCondition(action)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action ${action.type.name}", e)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun handleToggleWifi(action: ActionModel) {
        val state = action.params["state"] ?: "TOGGLE"
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager != null) {
            val target = when (state) {
                "ON" -> true
                "OFF" -> false
                else -> !wifiManager.isWifiEnabled
            }
            wifiManager.isWifiEnabled = target
        } else {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleToggleBluetooth(action: ActionModel) {
        val state = action.params["state"] ?: "TOGGLE"
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        try {
            when (state) {
                "ON" -> bluetoothAdapter.enable()
                "OFF" -> bluetoothAdapter.disable()
                else -> {
                    if (bluetoothAdapter.isEnabled) {
                        bluetoothAdapter.disable()
                    } else {
                        bluetoothAdapter.enable()
                    }
                }
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun handleToggleMobileData() {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleToggleAirplaneMode() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleToggleHotspot() {
        val intent = Intent("android.settings.TETHER_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleToggleLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleSetVolume(action: ActionModel, streamType: Int) {
        val volumePercent = action.params["level"]?.toIntOrNull() ?: 50
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = (maxVolume * (volumePercent.coerceIn(0, 100) / 100.0)).toInt()
        audioManager.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
    }

    private fun handleMuteUnmuteAudio(action: ActionModel) {
        val mode = action.params["mode"] ?: "TOGGLE"
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (mode == "MUTE") {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } else if (mode == "UNMUTE") {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } else {
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
        }
    }

    private fun handleToggleDnd(action: ActionModel) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }
        val filter = action.params["filter"] ?: "PRIORITY" // OFF, PRIORITY, ALARMS, NONE
        val filterInt = when (filter) {
            "OFF" -> NotificationManager.INTERRUPTION_FILTER_ALL
            "PRIORITY" -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
            "ALARMS" -> NotificationManager.INTERRUPTION_FILTER_ALARMS
            "NONE" -> NotificationManager.INTERRUPTION_FILTER_NONE
            else -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
        notificationManager.setInterruptionFilter(filterInt)
    }

    private fun handlePlaySound(action: ActionModel) {
        try {
            val soundUriString = action.params["uri"]
            val uri = if (!soundUriString.isNullOrEmpty()) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
        }
    }

    private fun handleTextToSpeech(action: ActionModel) {
        val text = action.params["text"] ?: "TagNod Macro Activated"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TagNodTTS")
    }

    @SuppressLint("MissingPermission")
    private fun handleVibrate(action: ActionModel) {
        val durationMs = action.params["duration"]?.toLongOrNull() ?: 500L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun handleSetBrightness(action: ActionModel) {
        val level = action.params["level"]?.toIntOrNull() ?: 50
        val brightness = (255 * (level.coerceIn(0, 100) / 100.0)).toInt()
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun handleToggleRotationLock(action: ActionModel) {
        val mode = action.params["state"] ?: "TOGGLE"
        if (Settings.System.canWrite(context)) {
            val current = try {
                Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION)
            } catch (e: Exception) {
                0
            }
            val newMode = when (mode) {
                "ON" -> 1
                "OFF" -> 0
                else -> if (current == 1) 0 else 1
            }
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, newMode)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun handleTurnScreenOnOff(action: ActionModel) {
        val mode = action.params["state"] ?: "OFF"
        if (mode == "OFF") {
            TagNodAccessibilityService.lockScreen()
        } else {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "TagNod:WakeLock"
            )
            wakeLock.acquire(3000L)
        }
    }

    private fun handleSetScreenTimeout(action: ActionModel) {
        val seconds = action.params["seconds"]?.toIntOrNull() ?: 30
        val timeoutMs = seconds * 1000
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, timeoutMs)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun handleToggleFlashlight(action: ActionModel) {
        val mode = action.params["state"] ?: "TOGGLE"
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
        val enable = mode == "ON"
        try {
            cameraManager.setTorchMode(cameraId, enable)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight", e)
        }
    }

    private fun handleTakeScreenshot() {
        TagNodAccessibilityService.takeScreenshot()
    }

    private fun handleSetDisplayColorMode(action: ActionModel) {
        val mode = action.params["mode"] ?: "ENABLE"
        val isEnable = mode.equals("ENABLE", ignoreCase = true) || mode.equals("GREYSCALE", ignoreCase = true) || mode == "1"

        try {
            if (isEnable) {
                Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer_enabled", 1)
                Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer", 0)
                Log.d(TAG, "Grayscale ENABLED via Daltonizer (enabled=1, daltonizer=0)")
            } else {
                Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer_enabled", 0)
                Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer", -1)
                Log.d(TAG, "Grayscale DISABLED via Daltonizer (enabled=0, daltonizer=-1)")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: WRITE_SECURE_SETTINGS permission required via ADB", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting display color mode", e)
        }
    }

    private fun handleLaunchApp(action: ActionModel) {
        val packageName = action.params["package_name"] ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }

    private fun handleOpenUrl(action: ActionModel) {
        var url = action.params["url"] ?: "https://google.com"
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun handleLaunchShortcut(action: ActionModel) {
        val packageName = action.params["package_name"] ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleSendSms(action: ActionModel) {
        val number = action.params["phone_number"] ?: return
        val message = action.params["message"] ?: "TagNod automated message"
        @Suppress("DEPRECATION")
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(number, null, message, null, null)
    }

    @SuppressLint("MissingPermission")
    private fun handleMakePhoneCall(action: ActionModel) {
        val number = action.params["phone_number"] ?: return
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun handleSendNotification(action: ActionModel) {
        val title = action.params["title"] ?: "TagNod Notification"
        val body = action.params["body"] ?: "Macro executed successfully"
        val channelId = "tagnod_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TagNod Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun handleCopyToClipboard(action: ActionModel) {
        val text = action.params["text"] ?: ""
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TagNod", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun handlePasteClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        Log.d(TAG, "Pasted from clipboard: $clipText")
    }

    private fun handleWriteToFile(action: ActionModel) {
        val path = action.params["file_path"] ?: "${context.filesDir.absolutePath}/tagnod_output.txt"
        val content = action.params["content"] ?: ""
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText("$content\n")
    }

    private fun handleAppendLog(action: ActionModel) {
        val path = action.params["file_path"] ?: "${context.filesDir.absolutePath}/tagnod_log.txt"
        val message = action.params["message"] ?: "Macro executed"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val file = File(path)
        file.parentFile?.mkdirs()
        file.appendText("[$timestamp] $message\n")
    }

    private fun handleHttpRequest(action: ActionModel) {
        val urlString = action.params["url"] ?: return
        val method = action.params["method"] ?: "GET"
        val body = action.params["body"] ?: ""

        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        if (method == "POST" && body.isNotEmpty()) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { os ->
                os.write(body.toByteArray())
            }
        }

        val responseCode = conn.responseCode
        Log.d(TAG, "HTTP Request $method $urlString returned code $responseCode")
        conn.disconnect()
    }

    private fun handleRunShellCommand(action: ActionModel) {
        val command = action.params["command"] ?: return
        try {
            val process = Runtime.getRuntime().exec(command)
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Error running shell command: $command", e)
        }
    }

    private fun handleKillApp(action: ActionModel) {
        val packageName = action.params["package_name"] ?: return
        TagNodAccessibilityService.killApp(packageName)
    }

    private fun handleClearNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    private suspend fun handleWait(action: ActionModel) {
        val seconds = action.params["seconds"]?.toLongOrNull() ?: 1L
        delay(seconds * 1000L)
    }

    @SuppressLint("MissingPermission")
    private fun handleIfElseCondition(action: ActionModel): Boolean {
        val conditionType = action.params["condition_type"] ?: "BATTERY" // WIFI, BLUETOOTH, TIME, BATTERY
        return when (conditionType) {
            "WIFI" -> {
                val expected = action.params["wifi_state"] ?: "CONNECTED" // CONNECTED, ENABLED
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                val isConnectedWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val isWifiEnabled = wifiManager?.isWifiEnabled == true

                if (expected == "CONNECTED") isConnectedWifi else isWifiEnabled
            }
            "BLUETOOTH" -> {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                bluetoothAdapter?.isEnabled == true
            }
            "BATTERY" -> {
                val minLevel = action.params["min_battery"]?.toIntOrNull() ?: 20
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val currentLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                currentLevel >= minLevel
            }
            "TIME" -> {
                val startHour = action.params["start_hour"]?.toIntOrNull() ?: 8
                val endHour = action.params["end_hour"]?.toIntOrNull() ?: 22
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                currentHour in startHour..endHour
            }
            else -> true
        }
    }
}
