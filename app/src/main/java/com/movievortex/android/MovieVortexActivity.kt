package com.movievortex.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.movievortex.android.controller.SettingsController
import com.movievortex.android.ui.navigation.MovieVortexNavGraph
import com.movievortex.android.ui.theme.MovieVortexTheme

class MovieVortexActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set status and navigation bars to dark transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize CloudStream Core infrastructure
        CommonActivity.setActivityInstance(this)

        // Asynchronously load online & local providers in background
        ioSafe {
            try {
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllOnlinePlugins(this@MovieVortexActivity)
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(this@MovieVortexActivity, false)
            } catch (_: Exception) {}
        }

        val settingsController = SettingsController(applicationContext)

        setContent {
            MovieVortexTheme {
                MovieVortexNavGraph(
                    settingsController = settingsController,
                    isFirstLaunch = false
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CommonActivity.setActivityInstance(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        CommonActivity.setActivityInstance(null)
    }
}
