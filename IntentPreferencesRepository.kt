package com.intentphone.launcher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.intentphone.launcher.data.model.IntentSession
import com.intentphone.launcher.data.model.LauncherSettings
import com.intentphone.launcher.data.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "intent_phone_prefs")

class IntentPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val ACTIVE_INTENT_ID = stringPreferencesKey("active_intent_id")
        val ACTIVE_INTENT_WHY = stringPreferencesKey("active_intent_why")
        val ACTIVE_INTENT_GOAL = stringPreferencesKey("active_intent_goal")
        val ACTIVE_INTENT_APP_NAME = stringPreferencesKey("active_intent_app_name")
        val ACTIVE_INTENT_APP_PACKAGE = stringPreferencesKey("active_intent_app_package")
        val ACTIVE_INTENT_EXPECTED_TIME = stringPreferencesKey("active_intent_expected_time")
        val ACTIVE_INTENT_ACTION = stringPreferencesKey("active_intent_action")
        val ACTIVE_INTENT_START_TIME = longPreferencesKey("active_intent_start_time")
        val ACTIVE_INTENT_DURATION_MIN = intPreferencesKey("active_intent_duration_min")

        val TOTAL_INTENTIONS = intPreferencesKey("stat_total_intentions")
        val DISTRACTIONS_AVOIDED = intPreferencesKey("stat_distractions_avoided")
        val COMPLETED_INTENTIONS = intPreferencesKey("stat_completed_intentions")
        val MINDFUL_MINUTES = intPreferencesKey("stat_mindful_minutes")

        val SETTING_PROMPT_UNLOCK = booleanPreferencesKey("set_prompt_unlock")
        val SETTING_DEFAULT_INTENTION = stringPreferencesKey("set_default_intention")
        val SETTING_DARK_MODE = booleanPreferencesKey("set_dark_mode")
        val SETTING_SHOW_APP_NAMES = booleanPreferencesKey("set_show_app_names")
        val SETTING_ENABLED_CATEGORIES = stringPreferencesKey("set_enabled_categories")
        val SETTING_APP_ASSIGNMENTS = stringPreferencesKey("set_app_assignments")
        val SETTING_FRICTION_SECONDS = intPreferencesKey("set_friction_seconds")
        val SETTING_AMOLED = booleanPreferencesKey("set_amoled")
        val SETTING_EINK = booleanPreferencesKey("set_eink")
        val SETTING_DEFAULT_MIN = intPreferencesKey("set_default_min")
        val SETTING_REFLECTION = booleanPreferencesKey("set_reflection")
    }

    val activeSessionFlow: Flow<IntentSession?> = context.dataStore.data.map { prefs ->
        val why = prefs[PreferencesKeys.ACTIVE_INTENT_WHY] ?: return@map null
        val id = prefs[PreferencesKeys.ACTIVE_INTENT_ID] ?: "current"
        val goal = prefs[PreferencesKeys.ACTIVE_INTENT_GOAL] ?: ""
        val appName = prefs[PreferencesKeys.ACTIVE_INTENT_APP_NAME] ?: "Selected App"
        val appPackage = prefs[PreferencesKeys.ACTIVE_INTENT_APP_PACKAGE] ?: ""
        val expectedTime = prefs[PreferencesKeys.ACTIVE_INTENT_EXPECTED_TIME] ?: "30 minutes"
        val completionAction = prefs[PreferencesKeys.ACTIVE_INTENT_ACTION] ?: "Lock my phone"
        val startTime = prefs[PreferencesKeys.ACTIVE_INTENT_START_TIME] ?: System.currentTimeMillis()
        val duration = prefs[PreferencesKeys.ACTIVE_INTENT_DURATION_MIN] ?: 30

        IntentSession(
            id = id,
            why = why,
            goal = goal,
            appName = appName,
            appPackage = appPackage,
            expectedTime = expectedTime,
            completionAction = completionAction,
            durationMinutes = duration,
            startedAtTimestamp = startTime,
            status = SessionStatus.IN_PROGRESS
        )
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { prefs ->
        val rawCategories = prefs[PreferencesKeys.SETTING_ENABLED_CATEGORIES]
        val enabledCategories = if (rawCategories != null) {
            rawCategories.split(",").filter { it.isNotBlank() }
        } else {
            listOf("Work", "Communication", "Study", "Health & Mindfulness", "Utility", "Finance", "Entertainment")
        }

        val rawAssignments = prefs[PreferencesKeys.SETTING_APP_ASSIGNMENTS]
        val appAssignments = mutableMapOf<String, String>()
        if (rawAssignments != null) {
            rawAssignments.split(";").forEach { pair ->
                val parts = pair.split("=")
                if (parts.size == 2) {
                    appAssignments[parts[0]] = parts[1]
                }
            }
        }

        LauncherSettings(
            promptEveryUnlock = prefs[PreferencesKeys.SETTING_PROMPT_UNLOCK] ?: true,
            defaultIntention = prefs[PreferencesKeys.SETTING_DEFAULT_INTENTION] ?: "",
            darkMode = prefs[PreferencesKeys.SETTING_DARK_MODE] ?: true,
            showAppNames = prefs[PreferencesKeys.SETTING_SHOW_APP_NAMES] ?: true,
            enabledCategories = enabledCategories,
            appCategoryAssignments = appAssignments,
            frictionSeconds = prefs[PreferencesKeys.SETTING_FRICTION_SECONDS] ?: 5,
            amoledMode = prefs[PreferencesKeys.SETTING_AMOLED] ?: true,
            eInkPaperMode = prefs[PreferencesKeys.SETTING_EINK] ?: false,
            defaultDurationMinutes = prefs[PreferencesKeys.SETTING_DEFAULT_MIN] ?: 15,
            showReflectionAfterSession = prefs[PreferencesKeys.SETTING_REFLECTION] ?: true
        )
    }

    val statsFlow: Flow<MindfulStats> = context.dataStore.data.map { prefs ->
        MindfulStats(
            totalIntentions = prefs[PreferencesKeys.TOTAL_INTENTIONS] ?: 0,
            distractionsResisted = prefs[PreferencesKeys.DISTRACTIONS_AVOIDED] ?: 0,
            completedAccomplished = prefs[PreferencesKeys.COMPLETED_INTENTIONS] ?: 0,
            mindfulMinutes = prefs[PreferencesKeys.MINDFUL_MINUTES] ?: 0
        )
    }

    suspend fun startSession(
        why: String,
        goal: String,
        appName: String,
        appPackage: String,
        expectedTime: String,
        completionAction: String,
        durationMinutes: Int
    ): IntentSession {
        val session = IntentSession(
            why = why,
            goal = goal,
            appName = appName,
            appPackage = appPackage,
            expectedTime = expectedTime,
            completionAction = completionAction,
            durationMinutes = durationMinutes,
            startedAtTimestamp = System.currentTimeMillis()
        )
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACTIVE_INTENT_ID] = session.id
            prefs[PreferencesKeys.ACTIVE_INTENT_WHY] = session.why
            prefs[PreferencesKeys.ACTIVE_INTENT_GOAL] = session.goal
            prefs[PreferencesKeys.ACTIVE_INTENT_APP_NAME] = session.appName
            prefs[PreferencesKeys.ACTIVE_INTENT_APP_PACKAGE] = session.appPackage
            prefs[PreferencesKeys.ACTIVE_INTENT_EXPECTED_TIME] = session.expectedTime
            prefs[PreferencesKeys.ACTIVE_INTENT_ACTION] = session.completionAction
            prefs[PreferencesKeys.ACTIVE_INTENT_START_TIME] = session.startedAtTimestamp
            prefs[PreferencesKeys.ACTIVE_INTENT_DURATION_MIN] = session.durationMinutes

            val currentTotal = prefs[PreferencesKeys.TOTAL_INTENTIONS] ?: 0
            prefs[PreferencesKeys.TOTAL_INTENTIONS] = currentTotal + 1
        }
        return session
    }

    suspend fun clearSession(accomplished: Boolean? = null) {
        context.dataStore.edit { prefs ->
            val duration = prefs[PreferencesKeys.ACTIVE_INTENT_DURATION_MIN] ?: 15
            val currentMinutes = prefs[PreferencesKeys.MINDFUL_MINUTES] ?: 0
            prefs[PreferencesKeys.MINDFUL_MINUTES] = currentMinutes + duration

            if (accomplished == true) {
                val currentComp = prefs[PreferencesKeys.COMPLETED_INTENTIONS] ?: 0
                prefs[PreferencesKeys.COMPLETED_INTENTIONS] = currentComp + 1
            }

            prefs.remove(PreferencesKeys.ACTIVE_INTENT_ID)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_WHY)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_GOAL)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_APP_NAME)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_APP_PACKAGE)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_EXPECTED_TIME)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_ACTION)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_START_TIME)
            prefs.remove(PreferencesKeys.ACTIVE_INTENT_DURATION_MIN)
        }
    }

    suspend fun recordDistractionAvoided() {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.DISTRACTIONS_AVOIDED] ?: 0
            prefs[PreferencesKeys.DISTRACTIONS_AVOIDED] = current + 1
        }
    }

    suspend fun updateSettings(settings: LauncherSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SETTING_PROMPT_UNLOCK] = settings.promptEveryUnlock
            prefs[PreferencesKeys.SETTING_DEFAULT_INTENTION] = settings.defaultIntention
            prefs[PreferencesKeys.SETTING_DARK_MODE] = settings.darkMode
            prefs[PreferencesKeys.SETTING_SHOW_APP_NAMES] = settings.showAppNames
            prefs[PreferencesKeys.SETTING_ENABLED_CATEGORIES] = settings.enabledCategories.joinToString(",")
            prefs[PreferencesKeys.SETTING_APP_ASSIGNMENTS] = settings.appCategoryAssignments.map { "${it.key}=${it.value}" }.joinToString(";")
            prefs[PreferencesKeys.SETTING_FRICTION_SECONDS] = settings.frictionSeconds
            prefs[PreferencesKeys.SETTING_AMOLED] = settings.amoledMode
            prefs[PreferencesKeys.SETTING_EINK] = settings.eInkPaperMode
            prefs[PreferencesKeys.SETTING_DEFAULT_MIN] = settings.defaultDurationMinutes
            prefs[PreferencesKeys.SETTING_REFLECTION] = settings.showReflectionAfterSession
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.SETTING_PROMPT_UNLOCK)
            prefs.remove(PreferencesKeys.SETTING_DEFAULT_INTENTION)
            prefs.remove(PreferencesKeys.SETTING_DARK_MODE)
            prefs.remove(PreferencesKeys.SETTING_SHOW_APP_NAMES)
            prefs.remove(PreferencesKeys.SETTING_ENABLED_CATEGORIES)
            prefs.remove(PreferencesKeys.SETTING_APP_ASSIGNMENTS)
            prefs.remove(PreferencesKeys.SETTING_FRICTION_SECONDS)
            prefs.remove(PreferencesKeys.SETTING_AMOLED)
            prefs.remove(PreferencesKeys.SETTING_EINK)
            prefs.remove(PreferencesKeys.SETTING_DEFAULT_MIN)
            prefs.remove(PreferencesKeys.SETTING_REFLECTION)
        }
    }
}

data class MindfulStats(
    val totalIntentions: Int,
    val distractionsResisted: Int,
    val completedAccomplished: Int,
    val mindfulMinutes: Int
)