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

/** Ordner: Name und enthaltene Apps (Schlüssel „paket/aktivität“) */
data class Folder(val id: String, val name: String, val apps: List<String>)

/** Was eine Wischgeste auf dem Startbildschirm tut: "" = nichts, "notif" = Benachrichtigungen, "app:<key>" = App öffnen */
typealias SwipeAction = String

/** Einstellungen – als Compose-Zustand, damit sich die Oberfläche sofort aktualisiert */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("minimal", Context.MODE_PRIVATE)

    var favorites by mutableStateOf(list("favorites"))
        private set
    var distracting by mutableStateOf(list("distracting").toSet())
        private set
    var hidden by mutableStateOf(list("hidden").toSet())
        private set
    var renames by mutableStateOf(readMap("renames"))
        private set
    /** Symbol je App („paket/aktivität“) oder Ordner („folder:<id>“) */
    var icons by mutableStateOf(readMap("icons"))
        private set
    var showIcons by mutableStateOf(sp.getBoolean("showIcons", true))
        private set
    var pauseSeconds by mutableIntStateOf(sp.getInt("pauseSeconds", 10))
        private set
    var showBattery by mutableStateOf(sp.getBoolean("showBattery", true))
        private set
    var showScreenTime by mutableStateOf(sp.getBoolean("showScreenTime", true))
        private set
    var keyboardOnOpen by mutableStateOf(sp.getBoolean("keyboardOnOpen", false))
        private set
    /** Ausgeblendete Apps trotzdem in der App-Liste zeigen (abgedunkelt) */
    var showHidden by mutableStateOf(sp.getBoolean("showHidden", false))
        private set
    /** Wischgesten: unten/links/rechts (nach oben = alle Apps) */
    var swipeDown by mutableStateOf(sp.getString("swipeDown", "notif") ?: "notif")
        private set
    var swipeLeft by mutableStateOf(sp.getString("swipeLeft", "") ?: "")
        private set
    var swipeRight by mutableStateOf(sp.getString("swipeRight", "") ?: "")
        private set
    /** Schrift: "space" | "system" | "mono"; Größe 0 = klein, 1 = normal, 2 = groß */
    var font by mutableStateOf(sp.getString("font", "space") ?: "space")
        private set
    var fontSize by mutableIntStateOf(sp.getInt("fontSize", 1))
        private set
    /** Sternenhimmel: "move" | "still" | "off" */
    var stars by mutableStateOf(sp.getString("stars", "move") ?: "move")
        private set
    /** Wie stark die Sterne beim Scrollen/Wischen mitziehen: 0 aus, 1 sehr langsam, 2 langsam, 3 normal */
    var warpLevel by mutableIntStateOf(sp.getInt("warpLevel", 1))
        private set
    /** Ordner auf dem Startbildschirm (nur als Text); auf dem Startbildschirm stehen sie als "folder:<id>" */
    var folders by mutableStateOf(readFolders())
        private set

    private fun list(name: String) = (sp.getString(name, "") ?: "").split('|').filter { it.isNotBlank() }
    private fun readMap(name: String): Map<String, String> = try {
        val o = org.json.JSONObject(sp.getString(name, "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    } catch (_: Exception) { emptyMap() }

    fun saveFavorites(v: List<String>) { favorites = v; sp.edit().putString("favorites", v.joinToString("|")).apply() }
    fun toggleFavorite(key: String) =
        saveFavorites(if (key in favorites) favorites - key else (favorites + key).take(MAX_FAVORITES))
    fun moveFavorite(key: String, by: Int) {
        val i = favorites.indexOf(key); val j = i + by
        if (i < 0 || j !in favorites.indices) return
        saveFavorites(favorites.toMutableList().apply { add(j, removeAt(i)) })
    }

    fun toggleDistracting(pkg: String) {
        distracting = if (pkg in distracting) distracting - pkg else distracting + pkg
        sp.edit().putString("distracting", distracting.joinToString("|")).apply()
    }

    fun toggleHidden(key: String) {
        hidden = if (key in hidden) hidden - key else hidden + key
        sp.edit().putString("hidden", hidden.joinToString("|")).apply()
    }

    /** Neuer Name; leer = Originalname */
    fun rename(key: String, name: String) {
        val n = name.trim()
        renames = if (n.isEmpty()) renames - key else renames + (key to n)
        sp.edit().putString("renames", org.json.JSONObject(renames).toString()).apply()
    }

    fun setIcon(key: String, name: String?) {
        icons = if (name == null) icons - key else icons + (key to name)
        sp.edit().putString("icons", org.json.JSONObject(icons).toString()).apply()
    }
    fun iconFor(key: String): String? = if (showIcons) icons[key] else null
    fun toggleShowIcons() { showIcons = !showIcons; sp.edit().putBoolean("showIcons", showIcons).apply() }

    fun cyclePause() {
        val steps = listOf(5, 10, 20, 30)
        pauseSeconds = steps[(steps.indexOf(pauseSeconds) + 1) % steps.size]
        sp.edit().putInt("pauseSeconds", pauseSeconds).apply()
    }

    fun setSwipe(dir: String, action: SwipeAction) {
        when (dir) { "down" -> swipeDown = action; "left" -> swipeLeft = action; "right" -> swipeRight = action }
        sp.edit().putString("swipe" + dir.replaceFirstChar { it.uppercase() }, action).apply()
    }

    fun cycleFont() {
        val all = listOf("space", "system", "mono")
        font = all[(all.indexOf(font) + 1) % all.size]
        sp.edit().putString("font", font).apply()
    }
    fun cycleFontSize() { fontSize = (fontSize + 1) % 3; sp.edit().putInt("fontSize", fontSize).apply() }
    fun cycleStars() {
        val all = listOf("move", "still", "off")
        stars = all[(all.indexOf(stars) + 1) % all.size]
        sp.edit().putString("stars", stars).apply()
    }
    fun cycleWarp() { warpLevel = (warpLevel + 1) % 4; sp.edit().putInt("warpLevel", warpLevel).apply() }

    // ── Ordner ──
    private fun readFolders(): List<Folder> = try {
        val a = org.json.JSONArray(sp.getString("folders", "[]") ?: "[]")
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val apps = o.getJSONArray("apps")
            Folder(o.getString("id"), o.getString("name"), List(apps.length()) { apps.getString(it) })
        }
    } catch (_: Exception) { emptyList() }

    private fun saveFolders(v: List<Folder>) {
        folders = v
        val a = org.json.JSONArray()
        v.forEach { f -> a.put(org.json.JSONObject().put("id", f.id).put("name", f.name).put("apps", org.json.JSONArray(f.apps))) }
        sp.edit().putString("folders", a.toString()).apply()
    }

    fun folder(id: String) = folders.firstOrNull { it.id == id }

    /** Neuer Ordner – landet auf dem Startbildschirm, wenn dort Platz ist. Gibt die ID zurück. */
    fun createFolder(name: String, firstApp: String? = null): String {
        val id = System.currentTimeMillis().toString(36)
        saveFolders(folders + Folder(id, name.trim().ifEmpty { "Ordner" }, listOfNotNull(firstApp)))
        if (favorites.size < MAX_FAVORITES) saveFavorites(favorites + "folder:$id")
        return id
    }
    fun renameFolder(id: String, name: String) =
        saveFolders(folders.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    fun deleteFolder(id: String) {
        saveFolders(folders.filter { it.id != id })
        saveFavorites(favorites - "folder:$id")
    }
    fun moveInFolder(id: String, appKey: String, by: Int) = saveFolders(folders.map { f ->
        if (f.id != id) return@map f
        val i = f.apps.indexOf(appKey); val j = i + by
        if (i < 0 || j !in f.apps.indices) f else f.copy(apps = f.apps.toMutableList().apply { add(j, removeAt(i)) })
    })
    fun toggleInFolder(id: String, appKey: String) =
        saveFolders(folders.map { if (it.id == id) it.copy(apps = if (appKey in it.apps) it.apps - appKey else it.apps + appKey) else it })

    fun toggleBattery() { showBattery = !showBattery; sp.edit().putBoolean("showBattery", showBattery).apply() }
    fun toggleScreenTime() { showScreenTime = !showScreenTime; sp.edit().putBoolean("showScreenTime", showScreenTime).apply() }
    fun toggleShowHidden() { showHidden = !showHidden; sp.edit().putBoolean("showHidden", showHidden).apply() }
    fun toggleKeyboard() { keyboardOnOpen = !keyboardOnOpen; sp.edit().putBoolean("keyboardOnOpen", keyboardOnOpen).apply() }

    /** Zuletzt auf GitHub gesehene Build-Nummer (Hinweis bleibt nach Neustart erhalten) */
    var latestKnown: Int
        get() = sp.getInt("latestKnown", 0)
        set(v) { sp.edit().putInt("latestKnown", v).apply() }

    var lastUpdateCheck: Long
        get() = sp.getLong("lastUpdateCheck", 0L)
        set(v) { sp.edit().putLong("lastUpdateCheck", v).apply() }

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

/** Kurzwege und Gesten */
object Shortcuts {
    fun alarms(ctx: Context) = start(ctx, Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))
        || start(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, "android.intent.category.APP_CLOCK"))

    fun calendar(ctx: Context) {
        val uri = android.provider.CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(System.currentTimeMillis().toString()).build()
        if (!start(ctx, Intent(Intent.ACTION_VIEW, uri))) start(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
    }

    /** Benachrichtigungen herunterziehen (verstecktes System-API, auf fast allen Geräten verfügbar) */
    @android.annotation.SuppressLint("WrongConstant")
    fun notifications(ctx: Context) {
        try {
            val sb = ctx.getSystemService("statusbar") ?: return
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (_: Exception) { }
    }

    private fun start(ctx: Context, i: Intent): Boolean = try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: Exception) { false }
}

/** Neue Version auf GitHub? Gibt die Build-Nummer der neuesten Version zurück (oder null) */
object Updates {
    const val REPO = "phlpjo/Minimalist-Phone-APK"
    const val DOWNLOAD = "https://github.com/$REPO/releases/latest/download/Minimal.apk"

    fun latestBuild(): Int? = try {
        val c = java.net.URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as java.net.HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "Minimal-Launcher")
        val body = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        org.json.JSONObject(body).getString("tag_name").substringAfterLast('.').toIntOrNull()
    } catch (_: Exception) { null }

    fun currentBuild(ctx: Context): Int = try {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else @Suppress("DEPRECATION") pi.versionCode
    } catch (_: Exception) { 0 }
}
