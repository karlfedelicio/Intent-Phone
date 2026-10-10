package com.intentphone.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intentphone.launcher.data.model.AppInfo
import com.intentphone.launcher.data.model.LauncherSettings
import com.intentphone.launcher.ui.theme.*

/**
 * Privacy-first Settings screen for Intent Phone.
 * Sections: General, Intentions, Apps, and Privacy.
 */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    installedApps: List<AppInfo>,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onResetPreferences: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("General", "Intentions", "Apps", "Privacy")
    var showResetDialog by remember { mutableStateOf(false) }

    var defaultIntentionInput by remember { mutableStateOf(settings.defaultIntention) }
    var isSavedDefault by remember { mutableStateOf(false) }

    val defaultCategories = listOf("Work", "Communication", "Study", "Health & Mindfulness", "Utility", "Finance", "Entertainment")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            }
            TextButton(onClick = { showResetDialog = true }) {
                Text("Reset", style = MaterialTheme.typography.labelSmall, color = IntentDanger)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
            tabTitles.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (selectedTab) {
                0 -> {
                    // General
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ask for intention after unlock", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                Text("Require intention prompt on screen unlock", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                            }
                            Switch(checked = settings.promptEveryUnlock, onCheckedChange = { onUpdateSettings(settings.copy(promptEveryUnlock = it)) })
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Default intention", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Text("Fallback intention for quick focus", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = defaultIntentionInput, onValueChange = { defaultIntentionInput = it }, placeholder = { Text("e.g. Study chapter review") }, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { onUpdateSettings(settings.copy(defaultIntention = defaultIntentionInput.trim())); isSavedDefault = true }) {
                                    Text(if (isSavedDefault) "Saved" else "Save")
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Dark mode", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Switch(checked = settings.darkMode, onCheckedChange = { onUpdateSettings(settings.copy(darkMode = it)) })
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Show app names", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Switch(checked = settings.showAppNames, onCheckedChange = { onUpdateSettings(settings.copy(showAppNames = it)) })
                        }
                    }
                }
                1 -> {
                    // Intentions
                    defaultCategories.forEach { category ->
                        val isEnabled = settings.enabledCategories.contains(category)
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
                            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(category, style = MaterialTheme.typography.bodyLarge)
                                Switch(checked = isEnabled, onCheckedChange = { checked ->
                                    val updated = if (checked) settings.enabledCategories + category else settings.enabledCategories.filter { it != category }
                                    onUpdateSettings(settings.copy(enabledCategories = updated))
                                })
                            }
                        }
                    }
                }
                2 -> {
                    // Apps
                    installedApps.forEach { app ->
                        val assigned = settings.appCategoryAssignments[app.packageName] ?: "General"
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(assigned, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                3 -> {
                    // Privacy
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Privacy Guarantee", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("• 100% Offline: DataStore Preferences storage only.\n• Zero account creation or telemetry tracking.\n• No ad SDKs or background uploads.")
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all preferences?") },
            text = { Text("This will restore all settings and assignments to factory defaults.") },
            confirmButton = { Button(onClick = { onResetPreferences(); showResetDialog = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }
}