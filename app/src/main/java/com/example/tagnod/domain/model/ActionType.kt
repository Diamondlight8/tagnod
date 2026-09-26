package com.example.tagnod.domain.model

enum class ActionType(
    val title: String,
    val description: String,
    val category: ActionCategory
) {
    // Connectivity
    TOGGLE_WIFI("Wi-Fi", "Turn Wi-Fi on, off, or toggle state", ActionCategory.CONNECTIVITY),
    TOGGLE_BLUETOOTH("Bluetooth", "Turn Bluetooth on, off, or toggle state", ActionCategory.CONNECTIVITY),
    TOGGLE_MOBILE_DATA("Mobile Data", "Toggle mobile data settings", ActionCategory.CONNECTIVITY),
    TOGGLE_AIRPLANE_MODE("Airplane Mode", "Open Airplane mode toggle settings", ActionCategory.CONNECTIVITY),
    TOGGLE_HOTSPOT("Hotspot", "Toggle Wi-Fi Hotspot on or off", ActionCategory.CONNECTIVITY),
    TOGGLE_LOCATION("Location Services", "Toggle or open Location settings", ActionCategory.CONNECTIVITY),

    // Audio & Volume
    SET_MEDIA_VOLUME("Media Volume", "Set media stream volume (0-100%)", ActionCategory.AUDIO),
    SET_RINGTONE_VOLUME("Ringtone Volume", "Set ringtone stream volume (0-100%)", ActionCategory.AUDIO),
    SET_NOTIFICATION_VOLUME("Notification Volume", "Set notification stream volume (0-100%)", ActionCategory.AUDIO),
    SET_ALARM_VOLUME("Alarm Volume", "Set alarm stream volume (0-100%)", ActionCategory.AUDIO),
    MUTE_UNMUTE_AUDIO("Mute / Unmute Audio", "Mute or restore all device audio", ActionCategory.AUDIO),
    TOGGLE_DND("Do Not Disturb", "Set DND mode (Off, Priority, Total Silence)", ActionCategory.AUDIO),
    PLAY_SOUND("Play Sound File", "Play an audio file from storage or URL", ActionCategory.AUDIO),
    TEXT_TO_SPEECH("Text to Speech", "Speak out custom text aloud", ActionCategory.AUDIO),
    VIBRATE("Vibrate", "Vibrate device with custom pattern and duration", ActionCategory.AUDIO),

    // Display & Device
    SET_SCREEN_BRIGHTNESS("Screen Brightness", "Set screen brightness (0-100%)", ActionCategory.DISPLAY),
    TOGGLE_ROTATION_LOCK("Auto-Rotate Screen", "Toggle screen rotation lock on or off", ActionCategory.DISPLAY),
    TURN_SCREEN_ON_OFF("Turn Screen On / Off", "Lock screen or keep screen awake", ActionCategory.DISPLAY),
    SET_SCREEN_TIMEOUT("Screen Timeout", "Set screen timeout duration", ActionCategory.DISPLAY),
    TOGGLE_FLASHLIGHT("Flashlight", "Toggle torch flashlight on or off", ActionCategory.DISPLAY),
    TAKE_SCREENSHOT("Take Screenshot", "Take a screenshot and save to gallery", ActionCategory.DISPLAY),

    // Accessibility Display
    SET_DISPLAY_COLOR_MODE("Toggle Grayscale", "Enable or disable grayscale display mode via Daltonizer", ActionCategory.ACCESSIBILITY_DISPLAY),

    // Apps & Navigation
    LAUNCH_APP("Launch App", "Open any installed application", ActionCategory.APPS),
    OPEN_URL("Open URL", "Open a website link in default browser", ActionCategory.APPS),
    LAUNCH_SHORTCUT("Launch Shortcut", "Trigger a specific app shortcut", ActionCategory.APPS),

    // Communication
    SEND_SMS("Send SMS", "Send a text message to a specific contact", ActionCategory.COMMUNICATION),
    MAKE_PHONE_CALL("Make Phone Call", "Dial or call a phone number", ActionCategory.COMMUNICATION),
    SEND_NOTIFICATION("Send Notification", "Show custom notification with title and body", ActionCategory.COMMUNICATION),

    // Clipboard & Input
    COPY_TO_CLIPBOARD("Copy to Clipboard", "Copy text snippet to system clipboard", ActionCategory.CLIPBOARD),
    PASTE_CLIPBOARD("Paste Clipboard", "Read clipboard content and log or process", ActionCategory.CLIPBOARD),

    // Files & Logging
    WRITE_TO_FILE("Write to File", "Write text line to a specified file", ActionCategory.FILES),
    APPEND_LOG("Append Timestamp Log", "Append timestamped log entry to file", ActionCategory.FILES),

    // System
    HTTP_REQUEST("HTTP Request (Webhook)", "Send GET or POST request to a webhook URL", ActionCategory.SYSTEM),
    RUN_SHELL_COMMAND("Run Shell Command", "Execute terminal command (with root fallback)", ActionCategory.SYSTEM),
    KILL_APP("Kill App", "Force close or stop specified package", ActionCategory.SYSTEM),
    CLEAR_NOTIFICATIONS("Clear Notifications", "Dismiss all active status bar notifications", ActionCategory.SYSTEM),

    // Flow Control
    WAIT("Wait Pause", "Pause macro execution for N seconds", ActionCategory.FLOW_CONTROL),
    IF_ELSE("If / Else Condition", "Conditional execution based on Wi-Fi/BT/Battery/Time", ActionCategory.FLOW_CONTROL);

    companion object {
        fun fromString(value: String): ActionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WAIT
        }
    }
}
