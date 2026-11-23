package de.philipdecker.minimal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val screen = mutableStateOf<Screen>(Screen.Home)
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())
    private val tick = mutableIntStateOf(0)
    private val warp = mutableFloatStateOf(0f)
    private val scope = MainScope()
    private lateinit var prefs: Prefs

    // Apps installiert/entfernt → Liste neu laden
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme(resources.configuration)
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        reload()

        ContextCompat.registerReceiver(
            this, packages,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_EXPORTED,
        )

        // Zurück: immer zum Startbildschirm, auf dem Startbildschirm nichts tun
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { screen.value = Screen.Home }
        })

        setContent {
            // Umbenennungen anwenden, alphabetisch; ausgeblendete Apps nur in den Einstellungen
            val all = remember(apps.value, prefs.renames) {
                apps.value.map { a -> prefs.renames[a.key]?.let { a.copy(label = it) } ?: a }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            }
            val visible = remember(all, prefs.hidden, prefs.showHidden) { if (prefs.showHidden) all else all.filter { it.key !in prefs.hidden } }

            CompositionLocalProvider(
                LocalFont provides fontFor(prefs.font),
                LocalScale provides sizeFactor(prefs.fontSize),
                LocalWarp provides warp,
            ) {
                Box(Modifier.fillMaxSize().background(Bg)) {
                    Starfield(prefs.stars, warp, warpFactor(prefs.warpLevel))
                    AnimatedContent(
                        targetState = screen.value,
                        contentKey = { it::class },
                        transitionSpec = {
                            // Tiefer hinein (Home → Apps/Einstellungen/Pause): neues kommt aus der Nähe, altes fliegt nach vorn weg
                            val deeper = depthOf(targetState) > depthOf(initialState)
                            val enter = fadeIn(tween(320, 40)) + scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = if (deeper) 0.9f else 1.08f)
                            val exit = fadeOut(tween(200)) + scaleOut(tween(360, easing = FastOutSlowInEasing), targetScale = if (deeper) 1.08f else 0.92f)
                            enter togetherWith exit
                        },
                        label = "screen",
                    ) { s ->
                        when (s) {
                            Screen.Home -> HomeScreen(
                                prefs, all, tick.intValue,
                                onOpen = ::open,
                                onApps = { screen.value = Screen.Apps },
                                onSettings = { screen.value = Screen.Settings },
                                onSwipe = ::swipe,
                                onFolder = { screen.value = Screen.Folder(it) },
                            )
                            is Screen.Folder -> FolderScreen(prefs, s.id, all, onOpen = ::open, onBack = { screen.value = Screen.Home })
                            Screen.Apps -> AppsScreen(prefs, visible, onOpen = ::open, onBack = { screen.value = Screen.Home })
                            Screen.Settings -> SettingsScreen(
                                prefs, all, tick.intValue,
                                onBack = { screen.value = Screen.Home },
                            )
                            is Screen.Pause -> PauseScreen(
                                s.app, prefs.pauseSeconds,
                                onCancel = { screen.value = Screen.Home },
                                onOpen = { screen.value = Screen.Home; Apps.launch(this@MainActivity, s.app) },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun depthOf(s: Screen) = when (s) { Screen.Home -> 0; Screen.Apps, Screen.Settings, is Screen.Folder -> 1; is Screen.Pause -> 2 }

    /** Ablenkende Apps erst nach der Pause, alle anderen sofort */
    private fun open(app: AppEntry) {
        if (app.pkg in prefs.distracting && prefs.pauseSeconds > 0) {
            screen.value = Screen.Pause(app)
        } else {
            screen.value = Screen.Home
            Apps.launch(this, app)
        }
    }

    private fun swipe(action: SwipeAction) {
        when {
            action == "notif" -> Shortcuts.notifications(this)
            action.startsWith("app:") -> {
                val key = action.removePrefix("app:")
                apps.value.firstOrNull { it.key == key }?.let(::open)
            }
        }
    }

    private fun reload() {
        scope.launch { apps.value = withContext(Dispatchers.IO) { Apps.load(this@MainActivity) } }
    }

    /** Hell/Dunkel vom System übernehmen: Farben und Symbolfarbe der Systemleisten */
    private fun applyTheme(c: Configuration) {
        val dark = (c.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_NO
        Palette.dark = dark
        Lang.de = appLanguage(c) == "de"
        val t = android.graphics.Color.TRANSPARENT
        val bar = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(if (dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE))
    }

    /** Sprache: eigene App-Sprache (Android 13+, falls gewählt), sonst die erste Systemsprache */
    private fun appLanguage(c: Configuration): String {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val app = try { getSystemService(android.app.LocaleManager::class.java)?.applicationLocales } catch (_: Exception) { null }
            if (app != null && !app.isEmpty) return app[0].language
        }
        val sys = android.content.res.Resources.getSystem().configuration.locales
        return if (!sys.isEmpty) sys[0].language else c.locales[0].language
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme(newConfig)
    }

    // Home-Taste, während der Launcher offen ist → zurück zum Startbildschirm
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) screen.value = Screen.Home
    }

    override fun onResume() {
        super.onResume()
        Lang.de = appLanguage(resources.configuration) == "de"
        tick.intValue++
    }

    override fun onStop() {
        super.onStop()
        if (screen.value is Screen.Pause) screen.value = Screen.Home
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(packages) } catch (_: Exception) { }
        scope.cancel()
    }
}
