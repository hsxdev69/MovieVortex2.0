package com.movievortex.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.ui.download.DOWNLOAD_NAVIGATE_TO
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.movievortex.android.controller.SettingsController
import com.movievortex.android.ui.components.MainDestination
import com.movievortex.android.ui.navigation.MovieVortexNavGraph
import com.movievortex.android.ui.theme.MovieVortexTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MovieVortexActivity : ComponentActivity() {

    private val targetDestination = MutableStateFlow(MainDestination.MoviesTv)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set status and navigation bars to dark transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize CloudStream Core infrastructure
        CommonActivity.setActivityInstance(this)

        // Process incoming intent navigation (e.g. from notifications)
        handleNavigationIntent(intent)

        // Automatically bootstrap Phisher Repo and install AniVortex, IStreamFlare, MovieBoxProvider
        ioSafe {
            try {
                com.movievortex.android.core.VortexPluginBootstrap.bootstrap(this@MovieVortexActivity)
            } catch (e: Exception) {
                android.util.Log.e("MovieVortexActivity", "Plugin bootstrap error", e)
            }
        }

        val settingsController = SettingsController(applicationContext)

        setContent {
            val destination by targetDestination.collectAsState()
            MovieVortexTheme {
                MovieVortexNavGraph(
                    settingsController = settingsController,
                    isFirstLaunch = false,
                    initialDestination = destination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent != null) {
            val navTo = intent.getStringExtra("NAVIGATE_TO")
            val isDownloadNav = intent.hasExtra(DOWNLOAD_NAVIGATE_TO) || navTo == "downloads"
            if (isDownloadNav) {
                targetDestination.value = MainDestination.Downloads
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
