package com.intentphone.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.intentphone.launcher.data.model.AppCategory
import com.intentphone.launcher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class InstalledAppsRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private var cachedApps: List<AppInfo> = emptyList()

    private val distractionPackageSet = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.tiktok.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.pinterest",
        "com.netflix.mediaclient",
        "com.spotify.music"
    )

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        if (cachedApps.isNotEmpty() && !forceRefresh) {
            return@withContext cachedApps
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
        
        val apps = resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null

            val label = resolveInfo.loadLabel(packageManager).toString()
            val activityName = resolveInfo.activityInfo.name

            val category = classifyApp(pkg, label)
            val isDistraction = distractionPackageSet.contains(pkg) || 
                                category == AppCategory.SOCIAL || 
                                category == AppCategory.MEDIA_ENTERTAINMENT

            AppInfo(
                packageName = pkg,
                activityName = activityName,
                label = label,
                category = category,
                isDistractionRisk = isDistraction
            )
        }.sortedBy { it.label.lowercase(Locale.ROOT) }

        cachedApps = apps
        apps
    }

    private fun classifyApp(packageName: String, label: String): AppCategory {
        val lowerPkg = packageName.lowercase(Locale.ROOT)
        val lowerLabel = label.lowercase(Locale.ROOT)

        return when {
            lowerPkg.contains("dialer") || lowerPkg.contains("phone") || 
            lowerPkg.contains("message") || lowerPkg.contains("whatsapp") -> AppCategory.COMMUNICATION

            lowerPkg.contains("calc") || lowerPkg.contains("clock") || 
            lowerPkg.contains("calendar") || lowerPkg.contains("notes") -> AppCategory.PRODUCTIVITY

            lowerPkg.contains("bank") || lowerPkg.contains("wallet") -> AppCategory.FINANCE

            distractionPackageSet.contains(packageName) || 
            lowerLabel.contains("instagram") || lowerLabel.contains("tiktok") || 
            lowerLabel.contains("reddit") -> AppCategory.SOCIAL

            lowerLabel.contains("youtube") || lowerLabel.contains("netflix") -> AppCategory.MEDIA_ENTERTAINMENT

            else -> AppCategory.UTILITY
        }
    }

    fun launchApp(app: AppInfo): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}