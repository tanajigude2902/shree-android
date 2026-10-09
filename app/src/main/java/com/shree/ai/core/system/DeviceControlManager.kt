package com.shree.ai.core.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun setTorch(enable: Boolean): String {
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return "Torch camera not found."
            cameraManager?.setTorchMode(cameraId, enable)
            if (enable) "Torch turned ON." else "Torch turned OFF."
        } catch (e: Exception) { "Torch error: ${e.message}" }
    }

    fun adjustVolume(up: Boolean): String {
        return try {
            val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            if (up) "Volume increased." else "Volume decreased."
        } catch (e: Exception) { "Volume error: ${e.message}" }
    }

    fun setVolumeMute(): String {
        return try {
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            "Volume muted."
        } catch (e: Exception) { "Volume error: ${e.message}" }
    }

    fun openSettings(type: String): String {
        val action = when (type.lowercase().trim()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return try {
            context.startActivity(intent)
            "Opening $type settings."
        } catch (e: Exception) { "Settings error: ${e.message}" }
    }

    fun openAppByName(appName: String): String {
        val pm = context.packageManager
        val query = appName.lowercase().trim()
        val commonPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "instagram" to "com.instagram.android",
            "settings" to "com.android.settings",
            "camera" to "com.google.android.GoogleCamera",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm"
        )
        val targetPkg = commonPackages[query]
        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return "Opening $appName."
            }
        }
        try {
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(query) || query.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        return "Opening ${pm.getApplicationLabel(app)}."
                    }
                }
            }
        } catch (_: Exception) {}
        return "Could not find app '$appName'."
    }

    fun makePhoneCall(number: String): String {
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isNotEmpty()) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(dialIntent)
                "Opening dialer for $cleanNumber."
            } catch (e: Exception) { "Call error: ${e.message}" }
        }
        return "Please specify a valid phone number."
    }

    fun sendWhatsAppMessage(number: String?, messageText: String): String {
        val clean = number?.filter { it.isDigit() }
        val url = if (!clean.isNullOrBlank()) {
            "https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(messageText)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(messageText)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            setPackage("com.whatsapp")
        }
        return try {
            context.startActivity(intent)
            "Opening WhatsApp to send: \"$messageText\"."
        } catch (e: Exception) { "WhatsApp could not be opened." }
    }

    fun performHome(): String {
        val s = ShreeAccessibilityService.instance
        return if (s != null) { s.performGlobal(AccessibilityService.GLOBAL_ACTION_HOME); "Going Home." }
        else "Please enable SHREE in phone Accessibility Settings."
    }

    fun performBack(): String {
        val s = ShreeAccessibilityService.instance
        return if (s != null) { s.performGlobal(AccessibilityService.GLOBAL_ACTION_BACK); "Going Back." }
        else "Please enable SHREE in phone Accessibility Settings."
    }

    fun performRecentApps(): String {
        val s = ShreeAccessibilityService.instance
        return if (s != null) { s.performGlobal(AccessibilityService.GLOBAL_ACTION_RECENTS); "Showing Recent Apps." }
        else "Please enable SHREE in phone Accessibility Settings."
    }

    fun takeScreenshot(): String {
        val s = ShreeAccessibilityService.instance
        return if (s != null) { s.performGlobal(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT); "Taking Screenshot." }
        else "Please enable SHREE in phone Accessibility Settings."
    }

    fun performScroll(forward: Boolean): String {
        val s = ShreeAccessibilityService.instance
        return if (s != null) {
            s.scroll(forward)
            if (forward) "Scrolling down." else "Scrolling up."
        } else "Please enable SHREE in phone Accessibility Settings."
    }
}
