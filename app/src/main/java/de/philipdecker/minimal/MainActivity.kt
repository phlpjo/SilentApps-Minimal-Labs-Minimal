package de.philipdecker.minimal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
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
    /** Neueste Build-Nummer auf GitHub, falls neuer als diese App */
    private val update = mutableStateOf<Int?>(null)
    private val checking = mutableStateOf(false)
    private val scope = MainScope()
    private lateinit var prefs: Prefs

    // Apps installiert/entfernt → Liste neu laden
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        prefs.latestKnown.takeIf { it > Updates.currentBuild(this) }?.let { update.value = it }
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
            val visible = remember(all, prefs.hidden) { all.filter { it.key !in prefs.hidden } }

            CompositionLocalProvider(
                LocalFont provides fontFor(prefs.font),
                LocalScale provides sizeFactor(prefs.fontSize),
            ) {
                Box(Modifier.fillMaxSize().background(Bg)) {
                    AnimatedContent(
                        targetState = screen.value,
                        contentKey = { it::class },
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "screen",
                    ) { s ->
                        when (s) {
                            Screen.Home -> HomeScreen(
                                prefs, all, tick.intValue, update.value,
                                onOpen = ::open,
                                onApps = { screen.value = Screen.Apps },
                                onSettings = { screen.value = Screen.Settings },
                                onSwipe = ::swipe,
                            )
                            Screen.Apps -> AppsScreen(prefs, visible, onOpen = ::open, onBack = { screen.value = Screen.Home })
                            Screen.Settings -> SettingsScreen(
                                prefs, all, tick.intValue, update.value, checking.value,
                                onCheckUpdate = { checkUpdate(force = true) },
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

    /** Höchstens alle 6 Stunden bei GitHub nach einer neuen Version fragen (oder sofort, wenn gewünscht) */
    private fun checkUpdate(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (checking.value || (!force && now - prefs.lastUpdateCheck < 6 * 3600_000L)) return
        checking.value = true
        scope.launch {
            val latest = withContext(Dispatchers.IO) { Updates.latestBuild() }
            val current = Updates.currentBuild(this@MainActivity)
            if (latest != null) {
                prefs.lastUpdateCheck = now
                prefs.latestKnown = latest
                update.value = latest.takeIf { it > current }
            }
            checking.value = false
        }
    }

    // Home-Taste, während der Launcher offen ist → zurück zum Startbildschirm
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) screen.value = Screen.Home
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
        checkUpdate()
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
