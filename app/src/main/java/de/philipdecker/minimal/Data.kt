package de.philipdecker.minimal

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.Normalizer
import java.util.Calendar

/** Eine startbare App. key = „paket/aktivität“ (eindeutig, auch bei mehreren Einstiegen einer App) */
data class AppEntry(val label: String, val pkg: String, val cls: String) {
    val key: String get() = "$pkg/$cls"
    /** Für die Suche: klein, ohne Akzente („Ö“ findet auch „o“) */
    val search: String = fold(label)
}

fun fold(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

object Apps {
    fun load(ctx: Context): List<AppEntry> {
        val la = ctx.getSystemService(LauncherApps::class.java) ?: return emptyList()
        return la.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != ctx.packageName }
            .map { AppEntry(it.label.toString().trim(), it.componentName.packageName, it.componentName.className) }
            .distinctBy { it.key }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    fun launch(ctx: Context, app: AppEntry) {
        try {
            ctx.getSystemService(LauncherApps::class.java)!!
                .startMainActivity(ComponentName(app.pkg, app.cls), Process.myUserHandle(), null, null)
        } catch (e: Exception) {
            Toast.makeText(ctx, "${app.label} lässt sich nicht öffnen", Toast.LENGTH_SHORT).show()
        }
    }

    fun info(ctx: Context, app: AppEntry) {
        try {
            ctx.getSystemService(LauncherApps::class.java)
                ?.startAppDetailsActivity(ComponentName(app.pkg, app.cls), Process.myUserHandle(), null, null)
        } catch (_: Exception) { }
    }

    fun uninstall(ctx: Context, app: AppEntry) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}

/** Einstellungen – als Compose-Zustand, damit sich die Oberfläche sofort aktualisiert */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("minimal", Context.MODE_PRIVATE)

    var favorites by mutableStateOf(list("favorites"))
        private set
    var distracting by mutableStateOf(list("distracting").toSet())
        private set
    var pauseSeconds by mutableIntStateOf(sp.getInt("pauseSeconds", 10))
        private set
    var showBattery by mutableStateOf(sp.getBoolean("showBattery", true))
        private set
    var showScreenTime by mutableStateOf(sp.getBoolean("showScreenTime", true))
        private set
    var keyboardOnOpen by mutableStateOf(sp.getBoolean("keyboardOnOpen", true))
        private set

    private fun list(name: String) = (sp.getString(name, "") ?: "").split('|').filter { it.isNotBlank() }

    fun setFavorites(v: List<String>) { favorites = v; sp.edit().putString("favorites", v.joinToString("|")).apply() }
    fun toggleFavorite(key: String) =
        setFavorites(if (key in favorites) favorites - key else (favorites + key).take(MAX_FAVORITES))
    fun moveFavorite(key: String, by: Int) {
        val i = favorites.indexOf(key); val j = i + by
        if (i < 0 || j !in favorites.indices) return
        setFavorites(favorites.toMutableList().apply { add(j, removeAt(i)) })
    }

    fun toggleDistracting(pkg: String) {
        distracting = if (pkg in distracting) distracting - pkg else distracting + pkg
        sp.edit().putString("distracting", distracting.joinToString("|")).apply()
    }

    fun cyclePause() {
        val steps = listOf(5, 10, 20, 30)
        pauseSeconds = steps[(steps.indexOf(pauseSeconds) + 1) % steps.size]
        sp.edit().putInt("pauseSeconds", pauseSeconds).apply()
    }

    fun toggleBattery() { showBattery = !showBattery; sp.edit().putBoolean("showBattery", showBattery).apply() }
    fun toggleScreenTime() { showScreenTime = !showScreenTime; sp.edit().putBoolean("showScreenTime", showScreenTime).apply() }
    fun toggleKeyboard() { keyboardOnOpen = !keyboardOnOpen; sp.edit().putBoolean("keyboardOnOpen", keyboardOnOpen).apply() }

    companion object { const val MAX_FAVORITES = 8 }
}

/** Bildschirmzeit heute (ab Mitternacht), je App – braucht den „Nutzungszugriff“ */
object Usage {
    fun granted(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Millisekunden im Vordergrund je Paket, heute. Leer ohne Berechtigung. */
    @Suppress("DEPRECATION")
    fun today(ctx: Context): Map<String, Long> {
        if (!granted(ctx)) return emptyMap()
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val now = System.currentTimeMillis()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val events = usm.queryEvents(start, now) ?: return emptyMap()
        val e = UsageEvents.Event()
        val since = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val p = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> if (p !in since) since[p] = e.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> since.remove(p)?.let { total[p] = (total[p] ?: 0L) + (e.timeStamp - it) }
            }
        }
        for ((p, t) in since) total[p] = (total[p] ?: 0L) + (now - t)
        total.remove(ctx.packageName)
        return total
    }
}

fun battery(ctx: Context): Int? =
    ctx.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }

/** „1 Std. 12 Min.“ / „12 Min.“ / „unter 1 Min.“ */
fun duration(ms: Long): String {
    val min = ms / 60_000
    return when {
        min < 1 -> "unter 1 Min."
        min < 60 -> "$min Min."
        else -> "${min / 60} Std. ${min % 60} Min."
    }
}
