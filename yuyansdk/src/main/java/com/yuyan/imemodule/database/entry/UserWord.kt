package com.yuyan.imemodule.database.entry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自造词实体
 */
@Entity(tableName = "user_word")
data class UserWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "word")
    var word: String,
    @ColumnInfo(name = "pinyin")
    var pinyin: String,
    @ColumnInfo(name = "pinyinHead")
    var pinyinHead: String = "",
    @ColumnInfo(name = "t9")
    var t9: String = "",
    @ColumnInfo(name = "frequency")
    var frequency: Int = 100,
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis()
)
