package com.example.glyphmuseum.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.glyphmuseum.data.AppDatabase
import com.example.glyphmuseum.glyph.GlyphPlayer
import kotlinx.coroutines.*
import java.io.File

class NotificationListener : NotificationListenerService() {
    private lateinit var database: AppDatabase
    private lateinit var glyphPlayer: GlyphPlayer
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        glyphPlayer = GlyphPlayer(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val senderName = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        serviceScope.launch {
            val activeRules = database.ruleDao().getActiveRulesSync()
            
            for (rule in activeRules) {
                // Check if we have app filter
                if (!rule.appPackages.isNullOrEmpty()) {
                    val packages = rule.appPackages.split(",")
                    if (!packages.contains(packageName)) {
                        continue
                    }
                }
                
                if (!rule.senderName.isNullOrEmpty() && !senderName.contains(rule.senderName, ignoreCase = true)) continue
                if (!rule.messageContains.isNullOrEmpty() && !text.contains(rule.messageContains, ignoreCase = true)) continue

                Log.d("NotificationListener", "Rule matched: \${rule.id}")
                val gif = database.gifDao().getGifById(rule.gifId)
                if (gif != null) {
                    val file = File(gif.filePath)
                    if (file.exists()) {
                        glyphPlayer.playGif(file)
                    }
                }
                break // Stop evaluating since we found the highest priority match
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        glyphPlayer.destroy()
    }
}
