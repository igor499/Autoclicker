package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoClickService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AutoClickService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event listening required for auto click gestures
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoClickService interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        Log.d(TAG, "AutoClickService unbound")
        return super.onUnbind(intent)
    }

    /**
     * Dispatches a single tap gesture at the specified screen coordinates (X, Y).
     */
    fun tapAt(x: Float, y: Float, durationMs: Long = 50L): Boolean {
        if (x < 0f || y < 0f) return false
        return try {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(10L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch gesture", e)
            false
        }
    }

    companion object {
        private const val TAG = "AutoClickService"

        var instance: AutoClickService? = null
            private set

        fun isServiceRunning(): Boolean {
            return instance != null
        }

        fun isServiceEnabledInSettings(context: Context): Boolean {
            val expectedService = "${context.packageName}/${AutoClickService::class.java.canonicalName}"
            val accessibilityEnabled = try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Settings.SettingNotFoundException) {
                0
            }

            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (settingValue != null) {
                    val splitter = TextUtils.SimpleStringSplitter(':')
                    splitter.setString(settingValue)
                    while (splitter.hasNext()) {
                        val service = splitter.next()
                        if (service.equals(expectedService, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }
            return false
        }
    }
}
