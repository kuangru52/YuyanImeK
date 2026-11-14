package com.yuyan.imemodule.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.yuyan.imemodule.database.BaseDao
import com.yuyan.imemodule.database.entry.QuickPhrase

@Dao
interface QuickPhraseDao : BaseDao<QuickPhrase> {

    @Query("SELECT * FROM quick_phrase ORDER BY time DESC")
    fun getAll(): List<QuickPhrase>

    @Query("DELETE FROM quick_phrase WHERE content = :content")
    fun deleteByContent(content: String)

    @Query("SELECT * FROM quick_phrase WHERE content = :content LIMIT 1")
    fun queryByContent(content: String): QuickPhrase?

    @Query("DELETE FROM quick_phrase")
    fun deleteAll()
}
