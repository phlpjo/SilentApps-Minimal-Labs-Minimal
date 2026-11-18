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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
            when (val s = screen.value) {
                Screen.Home -> HomeScreen(
                    prefs, apps.value, tick.intValue,
                    onOpen = ::open,
                    onApps = { screen.value = Screen.Apps },
                    onSettings = { screen.value = Screen.Settings },
                )
                Screen.Apps -> AppsScreen(prefs, apps.value, onOpen = ::open, onBack = { screen.value = Screen.Home })
                Screen.Settings -> SettingsScreen(prefs, apps.value, tick.intValue, onBack = { screen.value = Screen.Home })
                is Screen.Pause -> PauseScreen(
                    s.app, prefs.pauseSeconds,
                    onCancel = { screen.value = Screen.Home },
                    onOpen = { screen.value = Screen.Home; Apps.launch(this, s.app) },
                )
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

    private fun reload() {
        scope.launch { apps.value = withContext(Dispatchers.IO) { Apps.load(this@MainActivity) } }
    }

    // Home-Taste, während der Launcher offen ist → zurück zum Startbildschirm
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) screen.value = Screen.Home
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }

    override fun onStop() {
        super.onStop()
        // Nach dem Öffnen einer App startet der Launcher wieder auf dem Startbildschirm (nicht in der Pause)
        if (screen.value is Screen.Pause) screen.value = Screen.Home
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(packages) } catch (_: Exception) { }
        scope.cancel()
    }
}
