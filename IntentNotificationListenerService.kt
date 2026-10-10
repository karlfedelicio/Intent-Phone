package com.intentphone.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal, Privacy-Respecting Notification Service.
 * Does NOT intercept or store private notification text, message bodies, or user credentials.
 */
class IntentNotificationListenerService : NotificationListenerService() {
    data class NotificationMeta(
        val key: String,
        val packageName: String,
        val postTime: Long,
        val isOngoing: Boolean
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
    }

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    }
}