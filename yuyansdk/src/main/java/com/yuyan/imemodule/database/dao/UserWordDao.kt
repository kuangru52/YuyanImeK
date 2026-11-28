package com.yuyan.imemodule.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.yuyan.imemodule.database.BaseDao
import com.yuyan.imemodule.database.entry.UserWord

@Dao
interface UserWordDao : BaseDao<UserWord> {

    @Query("SELECT * FROM user_word WHERE isEnabled = 1 ORDER BY frequency DESC, time DESC")
    fun getAll(): List<UserWord>

    @Query("SELECT * FROM user_word ORDER BY frequency DESC, time DESC")
    fun getAllRaw(): List<UserWord>

    @Query("SELECT * FROM user_word WHERE isEnabled = 1 AND (schema = '全部方案' OR schema LIKE '%' || :layoutName || '%') AND (LOWER(REPLACE(pinyin, ' ', '')) = LOWER(:py) OR LOWER(REPLACE(pinyin, ' ', '')) LIKE LOWER(:py) || '%' OR LOWER(pinyinHead) = LOWER(:py) OR LOWER(pinyinHead) LIKE LOWER(:py) || '%' OR t9 = :py OR word = :py) ORDER BY frequency DESC, time DESC")
    fun queryByLayout(py: String, layoutName: String): List<UserWord>

    @Query("SELECT * FROM user_word WHERE isEnabled = 1 AND (schema = '全部方案' OR schema LIKE '%' || :layoutName || '%') AND (LOWER(REPLACE(pinyin, ' ', '')) = LOWER(:py) OR LOWER(pinyinHead) = LOWER(:py) OR t9 = :py OR word = :py) ORDER BY frequency DESC, time DESC")
    fun queryExactByLayout(py: String, layoutName: String): List<UserWord>

    @Query("SELECT * FROM user_word WHERE isEnabled = 1 AND (LOWER(REPLACE(pinyin, ' ', '')) = LOWER(:py) OR LOWER(REPLACE(pinyin, ' ', '')) LIKE LOWER(:py) || '%' OR LOWER(pinyinHead) = LOWER(:py) OR LOWER(pinyinHead) LIKE LOWER(:py) || '%' OR t9 = :py OR word = :py) ORDER BY frequency DESC, time DESC")
    fun query(py: String): List<UserWord>

    @Query("SELECT * FROM user_word WHERE isEnabled = 1 AND (LOWER(REPLACE(pinyin, ' ', '')) = LOWER(:py) OR LOWER(pinyinHead) = LOWER(:py) OR t9 = :py OR word = :py) ORDER BY frequency DESC, time DESC")
    fun queryExact(py: String): List<UserWord>

    @Query("DELETE FROM user_word WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM user_word WHERE word = :word")
    fun deleteByWord(word: String)

    @Query("SELECT * FROM user_word WHERE word = :word LIMIT 1")
    fun queryByWord(word: String): UserWord?

    @Query("DELETE FROM user_word")
    fun deleteAll()

    @Query("SELECT fileName FROM user_word GROUP BY fileName")
    fun getAllFileNames(): List<String>

    @Query("SELECT COUNT(*) FROM user_word WHERE fileName = :fileName")
    fun getWordCountByFileName(fileName: String): Int

    @Query("SELECT word FROM user_word WHERE fileName = :fileName LIMIT 15")
    fun getSampleWordsByFileName(fileName: String): List<String>

    @Query("DELETE FROM user_word WHERE fileName = :fileName")
    fun deleteByFileName(fileName: String)

    @Query("UPDATE user_word SET isEnabled = :enabled WHERE fileName = :fileName")
    fun setDictEnabled(fileName: String, enabled: Boolean)

    @Query("UPDATE user_word SET schema = :schema WHERE fileName = :fileName")
    fun setDictSchema(fileName: String, schema: String)

    @Query("SELECT isEnabled FROM user_word WHERE fileName = :fileName LIMIT 1")
    fun isDictEnabled(fileName: String): Boolean?

    @Query("SELECT schema FROM user_word WHERE fileName = :fileName LIMIT 1")
    fun getDictSchema(fileName: String): String?
}
