package de.philipdecker.minimal

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Nur für „Doppeltipp sperrt den Bildschirm“: Android lässt Apps den Bildschirm nur über eine Bedienungshilfe sperren.
 * Der Dienst liest keine Inhalte (keine Ereignistypen abonniert), er führt nur die Sperr-Aktion aus.
 */
class LockService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() { }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }

    companion object {
        @Volatile private var instance: LockService? = null

        val supported: Boolean get() = Build.VERSION.SDK_INT >= 28

        fun enabled(ctx: Context): Boolean {
            val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(ctx, LockService::class.java).flattenToString()
            return list.split(':').any { it.equals(me, ignoreCase = true) }
        }

        /** true = gesperrt; false = Dienst nicht aktiv (dann Einstellungen öffnen) */
        fun lock(): Boolean {
            val s = instance ?: return false
            return if (Build.VERSION.SDK_INT >= 28) s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) else false
        }
    }
}
