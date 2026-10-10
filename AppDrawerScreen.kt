package com.intentphone.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intentphone.launcher.data.model.AppInfo
import com.intentphone.launcher.ui.theme.*

/**
 * Full App Drawer providing access to all installed applications.
 * Alphabetical search, app names, app icons. Does not hide or uninstall apps.
 */
@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    val sortedApps = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    val filteredApps = remember(sortedApps, searchQuery) {
        if (searchQuery.isBlank()) sortedApps
        else sortedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    val groupedApps = remember(filteredApps) {
        filteredApps.groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text("All Applications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            }
            Text("${apps.size} apps", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search all apps...", color = TextMutedDark) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            groupedApps.forEach { (initial, letterApps) ->
                item {
                    Text(initial.toString(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
                }
                items(letterApps) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onAppClick(app) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text(app.label.take(1).uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        }
                    }
                }
            }
        }
    }
}