package com.yuyan.imemodule.database.entry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 常用语实体（用于剪贴板旁的快捷文本粘贴）
 */
@Entity(tableName = "quick_phrase")
data class QuickPhrase(
    @PrimaryKey
    @ColumnInfo(name = "content")
    var content: String,
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis()
)
