package com.yuyan.imemodule.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.database.dao.ClipboardDao
import com.yuyan.imemodule.database.dao.PhraseDao
import com.yuyan.imemodule.database.dao.QuickPhraseDao
import com.yuyan.imemodule.database.dao.SideSymbolDao
import com.yuyan.imemodule.database.dao.SkbFunDao
import com.yuyan.imemodule.database.dao.UsedSymbolDao
import com.yuyan.imemodule.database.dao.UserWordDao
import com.yuyan.imemodule.database.entry.Clipboard
import com.yuyan.imemodule.database.entry.Phrase
import com.yuyan.imemodule.database.entry.QuickPhrase
import com.yuyan.imemodule.database.entry.SideSymbol
import com.yuyan.imemodule.database.entry.SkbFun
import com.yuyan.imemodule.database.entry.UsedSymbol
import com.yuyan.imemodule.database.entry.UserWord
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils

@Database(entities = [SideSymbol::class, Clipboard::class, UsedSymbol::class, Phrase::class, SkbFun::class, UserWord::class, QuickPhrase::class], version = 7, exportSchema = false)
abstract class DataBaseKT : RoomDatabase() {
    abstract fun sideSymbolDao(): SideSymbolDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun usedSymbolDao(): UsedSymbolDao
    abstract fun phraseDao(): PhraseDao
    abstract fun skbFunDao(): SkbFunDao
    abstract fun userWordDao(): UserWordDao
    abstract fun quickPhraseDao(): QuickPhraseDao
    companion object {
        @Volatile private var cachedSideSymbolsPinyin: List<SideSymbol>? = null
        @Volatile private var cachedSideSymbolsNumber: List<SideSymbol>? = null

        fun getSideSymbolsPinyin(): List<SideSymbol> {
            return cachedSideSymbolsPinyin ?: instance.sideSymbolDao().getAllSideSymbolPinyin().also { cachedSideSymbolsPinyin = it }
        }

        fun getSideSymbolsNumber(): List<SideSymbol> {
            return cachedSideSymbolsNumber ?: instance.sideSymbolDao().getAllSideSymbolNumber().also { cachedSideSymbolsNumber = it }
        }

        fun invalidateSideSymbolsCache() {
            cachedSideSymbolsPinyin = null
            cachedSideSymbolsNumber = null
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS phrase (content TEXT PRIMARY KEY NOT NULL, isKeep INTEGER NOT NULL, t9 TEXT NOT NULL, qwerty TEXT NOT NULL, lx17 TEXT NOT NULL, time INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS skbfun (name TEXT KEY NOT NULL, isKeep INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY (name, isKeep))")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO skbfun (name, isKeep, position) VALUES ('TextEdit', 0, 15)")
                db.execSQL("INSERT INTO skbfun (name, isKeep, position) VALUES ('TextEdit', 1, 0)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS user_word")
                db.execSQL("CREATE TABLE IF NOT EXISTS user_word (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, word TEXT NOT NULL, pinyin TEXT NOT NULL, pinyinHead TEXT NOT NULL DEFAULT '', t9 TEXT NOT NULL DEFAULT '', frequency INTEGER NOT NULL DEFAULT 100, time INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 升级保留 user_word 用户词表，防止数据丢失
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS quick_phrase (content TEXT PRIMARY KEY NOT NULL, time INTEGER NOT NULL)")
            }
        }

        val instance = Room.databaseBuilder(Launcher.instance.context, DataBaseKT::class.java, "ime_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addCallback(object :Callback(){
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    ThreadPoolUtils.executeSingleton {
                        initDb()
                    }
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    ThreadPoolUtils.executeSingleton {
                        initDb()
                        initPhrasesDb()
                    }
                }
            })
            .build()
        private fun initDb() {  //初始化数据库数据
            runCatching {
                if (instance.sideSymbolDao().getAllSideSymbolPinyin().isEmpty()) {
                    val symbolPinyin = listOf("，", "。", "？", "！", "～", "：", "；", ".").map { symbolKey ->
                        SideSymbol(symbolKey, symbolKey)
                    }
                    instance.sideSymbolDao().insertAll(symbolPinyin)
                }
                if (instance.sideSymbolDao().getAllSideSymbolNumber().isEmpty()) {
                    val symbolNumber = listOf("%", "/", "-", "+", "*", "#", "@").map { symbolKey ->
                        SideSymbol(symbolKey, symbolKey, "number")
                    }
                    instance.sideSymbolDao().insertAll(symbolNumber)
                }
                invalidateSideSymbolsCache()
            }
        }

        private fun initPhrasesDb() {  // 初始化常用语数据 (QuickPhrase)
            runCatching {
                val presetContents = listOf(
                    "我的电话是__，常联系。",
                    "抱歉，我现在不方便接电话，稍后联系。",
                    "我正在开会，有急事请发短信。",
                    "我很快就到，请稍微等一会儿。",
                    "麻烦放驿站，谢谢。"
                )
                if (instance.quickPhraseDao().getAll().isEmpty()) {
                    val presets = presetContents.map { QuickPhrase(it) }
                    instance.quickPhraseDao().insertAll(presets)
                }
                if(instance.skbFunDao().getAllMenu().isEmpty()) {
                    val skbFuns = listOf(
                        SkbFun(name = SkbMenuMode.SwitchKeyboard.name, isKeep = 1, position = 0),
                        SkbFun(name = SkbMenuMode.Emojicon.name, isKeep = 1, position = 1),
                        SkbFun(name = SkbMenuMode.ClipBoard.name, isKeep = 1, position = 2),
                        SkbFun(name = SkbMenuMode.Phrases.name, isKeep = 0, position = 0),          // 1. 常用语
                        SkbFun(name = SkbMenuMode.Settings.name, isKeep = 0, position = 1),         // 2. 设置
                        SkbFun(name = SkbMenuMode.KeyboardHeight.name, isKeep = 0, position = 2),   // 3. 键盘调节
                        SkbFun(name = SkbMenuMode.Custom.name, isKeep = 0, position = 3),           // 4. 主题皮肤
                        SkbFun(name = SkbMenuMode.OneHanded.name, isKeep = 0, position = 4),         // 5. 单手模式
                        SkbFun(name = SkbMenuMode.Feedback.name, isKeep = 0, position = 5),          // 6. 按键效果
                        SkbFun(name = SkbMenuMode.Mnemonic.name, isKeep = 0, position = 6),          // 7. 双拼提示
                        SkbFun(name = SkbMenuMode.FloatKeyboard.name, isKeep = 0, position = 7),    // 8. 悬浮键盘
                        SkbFun(name = SkbMenuMode.NumberRow.name, isKeep = 0, position = 8),        // 9. 数字行
                        SkbFun(name = SkbMenuMode.JianFan.name, isKeep = 0, position = 9),          // 10. 繁体输入
                        SkbFun(name = SkbMenuMode.Emojicon.name, isKeep = 0, position = 10),
                        SkbFun(name = SkbMenuMode.SwitchKeyboard.name, isKeep = 0, position = 11),
                        SkbFun(name = SkbMenuMode.ClipBoard.name, isKeep = 0, position = 12),
                        SkbFun(name = SkbMenuMode.DarkTheme.name, isKeep = 0, position = 13),
                        SkbFun(name = SkbMenuMode.FlowerTypeface.name, isKeep = 0, position = 14),
                        SkbFun(name = SkbMenuMode.TextEdit.name, isKeep = 0, position = 15),
                    )
                    instance.skbFunDao().insertAll(skbFuns)
                } else {
                    val currentBarMenu = instance.skbFunDao().getALlBarMenu()
                    val oldDefaultNames = listOf(SkbMenuMode.ClipBoard.name, SkbMenuMode.Emojicon.name, SkbMenuMode.SwitchKeyboard.name)
                    val currentNames = currentBarMenu.map { it.name }
                    if (currentNames == oldDefaultNames || currentNames.isEmpty()) {
                        instance.skbFunDao().deleteAllBarMenu()
                        instance.skbFunDao().insert(SkbFun(name = SkbMenuMode.SwitchKeyboard.name, isKeep = 1, position = 0))
                        instance.skbFunDao().insert(SkbFun(name = SkbMenuMode.Emojicon.name, isKeep = 1, position = 1))
                        instance.skbFunDao().insert(SkbFun(name = SkbMenuMode.ClipBoard.name, isKeep = 1, position = 2))
                    }
                }
            }
        }
    }
}
