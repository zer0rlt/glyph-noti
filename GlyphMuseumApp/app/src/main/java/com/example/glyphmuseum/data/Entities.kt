package com.example.glyphmuseum.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "gifs")
data class GifEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var priority: Int, // Lower number = higher priority
    @ColumnInfo(name = "app_package") val appPackage: String?, // null means any app
    @ColumnInfo(name = "sender_name") val senderName: String?,
    @ColumnInfo(name = "message_contains") val messageContains: String?,
    @ColumnInfo(name = "gif_id") val gifId: Long,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true
)
