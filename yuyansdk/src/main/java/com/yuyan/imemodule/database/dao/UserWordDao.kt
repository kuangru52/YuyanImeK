package com.yuyan.imemodule.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.yuyan.imemodule.database.BaseDao
import com.yuyan.imemodule.database.entry.UserWord

@Dao
interface UserWordDao : BaseDao<UserWord> {

    @Query("SELECT * FROM user_word ORDER BY frequency DESC, time DESC")
    fun getAll(): List<UserWord>

    @Query("SELECT * FROM user_word WHERE pinyin = :py OR pinyin LIKE :py || '%' OR pinyinHead = :py OR t9 = :py OR word = :py ORDER BY frequency DESC, time DESC")
    fun query(py: String): List<UserWord>

    @Query("DELETE FROM user_word WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM user_word WHERE word = :word")
    fun deleteByWord(word: String)

    @Query("SELECT * FROM user_word WHERE word = :word LIMIT 1")
    fun queryByWord(word: String): UserWord?

    @Query("DELETE FROM user_word")
    fun deleteAll()
}
