package com.intentphone.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.intentphone.launcher.ui.screens.*
import com.intentphone.launcher.ui.theme.IntentPhoneTheme
import com.intentphone.launcher.ui.viewmodel.IntentViewModel
import com.intentphone.launcher.ui.viewmodel.LauncherScreenMode

/**
 * Core Launcher Activity. Intercepts phone unlock events and Home key presses.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: IntentViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as IntentPhoneApp
                return IntentViewModel(app.preferencesRepository, app.appsRepository) as T
            }
        }
    }

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                viewModel.onScreenUnlocked()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Listen for screen unlock (User Present)
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            IntentPhoneTheme(eInkMode = uiState.settings.eInkPaperMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState.currentMode) {
                        LauncherScreenMode.LOCK_INTENT_PROMPT -> {
                            LockIntentScreen(
                                installedApps = uiState.installedApps,
                                onCommitIntention = { why, goal, appName, appPackage, expectedTime, completionAction, durationMinutes ->
                                    viewModel.commitIntention(
                                        why = why,
                                        goal = goal,
                                        appName = appName,
                                        appPackage = appPackage,
                                        expectedTime = expectedTime,
                                        completionAction = completionAction,
                                        durationMinutes = durationMinutes
                                    )
                                },
                                onOpenEmergency = { openEmergencyDialer() },
                                onOpenSettings = { viewModel.setScreenMode(LauncherScreenMode.SETTINGS) },
                                onOpenStats = { viewModel.setScreenMode(LauncherScreenMode.STATS) }
                            )
                        }

                        LauncherScreenMode.ACTIVE_SESSION, LauncherScreenMode.ALL_APPS_DRAWER -> {
                            ActiveIntentScreen(
                                activeSession = uiState.activeSession,
                                remainingSeconds = uiState.remainingSeconds,
                                apps = uiState.filteredApps,
                                searchQuery = uiState.searchQuery,
                                selectedCategory = uiState.selectedCategoryFilter,
                                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                                onCategoryChange = { viewModel.onCategoryFilterChanged(it) },
                                onAppClick = { app -> viewModel.onAppClicked(app, this@MainActivity) },
                                onFinishSession = {
                                    if (uiState.settings.showReflectionAfterSession) {
                                        viewModel.setScreenMode(LauncherScreenMode.SESSION_REFLECTION)
                                    } else {
                                        viewModel.completeSession(true)
                                    }
                                },
                                onChangeIntention = {
                                    viewModel.setScreenMode(LauncherScreenMode.LOCK_INTENT_PROMPT)
                                },
                                onOpenSettings = { viewModel.setScreenMode(LauncherScreenMode.SETTINGS) }
                            )
                        }

                        LauncherScreenMode.FRICTION_GATE -> {
                            val targetApp = uiState.frictionTargetApp
                            if (targetApp != null) {
                                FrictionDialog(
                                    targetApp = targetApp,
                                    activeSession = uiState.activeSession,
                                    countdownSeconds = uiState.frictionCountdown,
                                    onCancel = { viewModel.cancelFriction() },
                                    onProceed = { viewModel.proceedPastFriction() }
                                )
                            }
                        }

                        LauncherScreenMode.SESSION_REFLECTION -> {
                            ReflectionDialog(
                                session = uiState.activeSession,
                                onAnswer = { accomplished -> viewModel.completeSession(accomplished) }
                            )
                        }

                        LauncherScreenMode.SETTINGS -> {
                            SettingsScreen(
                                settings = uiState.settings,
                                onUpdateSettings = { viewModel.updateSettings(it) },
                                onSetDefaultLauncher = { viewModel.openDefaultLauncherSettings(this@MainActivity) },
                                onBack = {
                                    val returnMode = if (uiState.activeSession != null) 
                                        LauncherScreenMode.ACTIVE_SESSION else LauncherScreenMode.LOCK_INTENT_PROMPT
                                    viewModel.setScreenMode(returnMode)
                                }
                            )
                        }

                        LauncherScreenMode.STATS -> {
                            StatsScreen(
                                stats = uiState.stats,
                                onBack = {
                                    val returnMode = if (uiState.activeSession != null) 
                                        LauncherScreenMode.ACTIVE_SESSION else LauncherScreenMode.LOCK_INTENT_PROMPT
                                    viewModel.setScreenMode(returnMode)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.categories?.contains(Intent.CATEGORY_HOME) == true) {
            val state = viewModel.uiState.value
            if (state.activeSession == null && state.settings.promptEveryUnlock) {
                viewModel.setScreenMode(LauncherScreenMode.LOCK_INTENT_PROMPT)
            }
        }
    }

    private fun openEmergencyDialer() {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(dialIntent)
        } catch (e: Exception) {
            // Ignored
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {}
    }
}