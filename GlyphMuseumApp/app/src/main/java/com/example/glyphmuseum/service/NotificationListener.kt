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
        val notification = sbn.notification
        val extras = notification.extras
        
        val senderName = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        Log.d("NotificationListener", "Received: pkg=\$packageName, sender=\$senderName, text=\$text")

        serviceScope.launch {
            val activeRules = database.ruleDao().getActiveRulesSync()
            
            for (rule in activeRules) {
                // Check app package
                if (!rule.appPackage.isNullOrEmpty() && rule.appPackage != packageName) {
                    continue
                }
                
                // Check sender name (case-insensitive substring match)
                if (!rule.senderName.isNullOrEmpty() && !senderName.contains(rule.senderName, ignoreCase = true)) {
                    continue
                }

                // Check message text
                if (!rule.messageContains.isNullOrEmpty() && !text.contains(rule.messageContains, ignoreCase = true)) {
                    continue
                }

                // Match found!
                Log.d("NotificationListener", "Rule matched: \${rule.id}")
                val gif = database.gifDao().getGifById(rule.gifId)
                if (gif != null) {
                    val file = File(gif.filePath)
                    if (file.exists()) {
                        glyphPlayer.playGif(file)
                    }
                }
                // Stop evaluating further rules since we found the highest priority match
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        glyphPlayer.destroy()
    }
}
