package com.intentphone.launcher.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intentphone.launcher.data.model.*
import com.intentphone.launcher.data.repository.InstalledAppsRepository
import com.intentphone.launcher.data.repository.IntentPreferencesRepository
import com.intentphone.launcher.data.repository.MindfulStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LauncherScreenMode {
    LOCK_INTENT_PROMPT,
    ACTIVE_SESSION,
    FRICTION_GATE,
    SESSION_REFLECTION,
    ALL_APPS_DRAWER,
    SETTINGS,
    STATS
}

data class LauncherUiState(
    val currentMode: LauncherScreenMode = LauncherScreenMode.LOCK_INTENT_PROMPT,
    val activeSession: IntentSession? = null,
    val remainingSeconds: Int = 0,
    val installedApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: AppCategory? = null,
    val frictionTargetApp: AppInfo? = null,
    val frictionCountdown: Int = 5,
    val stats: MindfulStats = MindfulStats(0, 0, 0, 0),
    val settings: LauncherSettings = LauncherSettings()
)

class IntentViewModel(
    private val preferencesRepository: IntentPreferencesRepository,
    private val appsRepository: InstalledAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var frictionJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        viewModelScope.launch {
            preferencesRepository.statsFlow.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }

        viewModelScope.launch {
            preferencesRepository.activeSessionFlow.collect { session ->
                _uiState.update { current ->
                    if (session != null) {
                        current.copy(
                            activeSession = session,
                            currentMode = if (current.currentMode == LauncherScreenMode.LOCK_INTENT_PROMPT) 
                                LauncherScreenMode.ACTIVE_SESSION else current.currentMode
                        )
                    } else {
                        current.copy(activeSession = null)
                    }
                }
                if (session != null) {
                    startSessionTimer(session)
                } else {
                    timerJob?.cancel()
                }
            }
        }

        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            val apps = appsRepository.getInstalledApps()
            _uiState.update { 
                it.copy(
                    installedApps = apps,
                    filteredApps = filterAppsList(apps, it.searchQuery, it.selectedCategoryFilter)
                ) 
            }
        }
    }

    fun onScreenUnlocked() {
        if (_uiState.value.settings.promptEveryUnlock && _uiState.value.activeSession == null) {
            _uiState.update { it.copy(currentMode = LauncherScreenMode.LOCK_INTENT_PROMPT) }
        }
    }

    fun commitIntention(
        why: String,
        goal: String,
        appName: String,
        appPackage: String,
        expectedTime: String,
        completionAction: String,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val session = preferencesRepository.startSession(
                why = why,
                goal = goal,
                appName = appName,
                appPackage = appPackage,
                expectedTime = expectedTime,
                completionAction = completionAction,
                durationMinutes = durationMinutes
            )
            _uiState.update { 
                it.copy(
                    activeSession = session,
                    currentMode = LauncherScreenMode.ACTIVE_SESSION
                ) 
            }
            startSessionTimer(session)
        }
    }

    private fun startSessionTimer(session: IntentSession) {
        timerJob?.cancel()
        val totalSeconds = session.durationMinutes * 60
        val elapsedSeconds = ((System.currentTimeMillis() - session.startedAtTimestamp) / 1000).toInt()
        val initialRemaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0)

        timerJob = viewModelScope.launch {
            var rem = initialRemaining
            while (rem > 0) {
                _uiState.update { it.copy(remainingSeconds = rem) }
                delay(1000)
                rem--
            }
            _uiState.update { 
                it.copy(
                    remainingSeconds = 0,
                    currentMode = LauncherScreenMode.SESSION_REFLECTION 
                ) 
            }
        }
    }

    fun onAppClicked(app: AppInfo, context: Context) {
        val activeSession = _uiState.value.activeSession
        if (app.isDistractionRisk && activeSession != null) {
            triggerFrictionGate(app)
        } else {
            appsRepository.launchApp(app)
        }
    }

    private fun triggerFrictionGate(app: AppInfo) {
        _uiState.update { 
            it.copy(
                frictionTargetApp = app,
                frictionCountdown = it.settings.frictionSeconds,
                currentMode = LauncherScreenMode.FRICTION_GATE
            ) 
        }

        frictionJob?.cancel()
        frictionJob = viewModelScope.launch {
            var count = _uiState.value.settings.frictionSeconds
            while (count > 0) {
                delay(1000)
                count--
                _uiState.update { it.copy(frictionCountdown = count) }
            }
        }
    }

    fun cancelFriction() {
        frictionJob?.cancel()
        viewModelScope.launch {
            preferencesRepository.recordDistractionAvoided()
        }
        _uiState.update { 
            it.copy(
                frictionTargetApp = null,
                currentMode = LauncherScreenMode.ACTIVE_SESSION
            ) 
        }
    }

    fun proceedPastFriction() {
        val app = _uiState.value.frictionTargetApp ?: return
        appsRepository.launchApp(app)
        _uiState.update { 
            it.copy(
                frictionTargetApp = null,
                currentMode = LauncherScreenMode.ACTIVE_SESSION
            ) 
        }
    }

    fun completeSession(wasAccomplished: Boolean) {
        viewModelScope.launch {
            preferencesRepository.clearSession(accomplished = wasAccomplished)
            _uiState.update { 
                it.copy(
                    activeSession = null,
                    currentMode = LauncherScreenMode.LOCK_INTENT_PROMPT
                ) 
            }
        }
    }

    fun setScreenMode(mode: LauncherScreenMode) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredApps = filterAppsList(current.installedApps, query, current.selectedCategoryFilter)
            )
        }
    }

    fun onCategoryFilterChanged(category: AppCategory?) {
        _uiState.update { current ->
            current.copy(
                selectedCategoryFilter = category,
                filteredApps = filterAppsList(current.installedApps, current.searchQuery, category)
            )
        }
    }

    private fun filterAppsList(apps: List<AppInfo>, query: String, category: AppCategory?): List<AppInfo> {
        return apps.filter { app ->
            val matchesQuery = query.isBlank() || app.label.contains(query, ignoreCase = true)
            val matchesCategory = category == null || app.category == category
            matchesQuery && matchesCategory
        }
    }

    fun updateSettings(newSettings: LauncherSettings) {
        viewModelScope.launch {
            preferencesRepository.updateSettings(newSettings)
        }
    }

    fun openDefaultLauncherSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}