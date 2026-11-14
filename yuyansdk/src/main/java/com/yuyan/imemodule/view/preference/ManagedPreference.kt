
package com.yuyan.imemodule.view.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import com.yuyan.imemodule.prefs.WeakHashSet
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class ManagedPreference<T : Any>(
    val sharedPreferences: SharedPreferences,
    val key: String,
    val defaultValue: T,
) : ReadWriteProperty<Any?, T> {

    interface StringLikeCodec<T : Any> {
        fun encode(x: T): String = x.toString()
        fun decode(raw: String): T?
    }

    fun interface OnChangeListener<in T : Any> {
        fun onChange(key: String, value: T)
    }

    abstract fun setValue(value: T)

    abstract fun getValue(): T

    abstract fun putValueTo(editor: SharedPreferences.Editor)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getValue()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setValue(value)

    private val listeners = java.util.concurrent.CopyOnWriteArraySet<OnChangeListener<T>>()

    fun registerOnChangeListener(listener: OnChangeListener<T>) {
        listeners.add(listener)
    }

    fun unregisterOnChangeListener(listener: OnChangeListener<T>) {
        listeners.remove(listener)
    }

    fun fireChange() {
        if (listeners.isEmpty()) return
        val newValue = getValue()
        listeners.forEach { it.onChange(key, newValue) }
    }

    class PBool(sharedPreferences: SharedPreferences, key: String, defaultValue: Boolean) :
        ManagedPreference<Boolean>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Boolean) {
            try {
                sharedPreferences.edit { putBoolean(key, value) }
            } catch (e: Exception) {
                sharedPreferences.edit { remove(key).putBoolean(key, value) }
            }
            fireChange()
        }

        override fun getValue(): Boolean {
            return try {
                sharedPreferences.getBoolean(key, defaultValue)
            } catch (e: Exception) {
                setValue(defaultValue)
                defaultValue
            }
        }

        override fun putValueTo(editor: SharedPreferences.Editor) {
            editor.putBoolean(key, getValue())
        }
    }

    class PString(sharedPreferences: SharedPreferences, key: String, defaultValue: String) :
        ManagedPreference<String>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: String) {
            try {
                sharedPreferences.edit { putString(key, value) }
            } catch (e: Exception) {
                sharedPreferences.edit { remove(key).putString(key, value) }
            }
            fireChange()
        }

        override fun getValue(): String {
            return try {
                sharedPreferences.getString(key, defaultValue)!!
            } catch (e: Exception) {
                setValue(defaultValue)
                defaultValue
            }
        }

        override fun putValueTo(editor: SharedPreferences.Editor) {
            editor.putString(key, getValue())
        }
    }

    class PStringLike<T : Any>(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T,
        private val codec: StringLikeCodec<T>
    ) : ManagedPreference<T>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: T) {
            try {
                sharedPreferences.edit { putString(key, codec.encode(value)) }
            } catch (e: Exception) {
                sharedPreferences.edit { remove(key).putString(key, codec.encode(value)) }
            }
            fireChange()
        }

        override fun getValue(): T {
            return try {
                sharedPreferences.getString(key, null)?.let {
                    codec.decode(it)
                } ?: defaultValue
            } catch (e: Exception) {
                setValue(defaultValue)
                defaultValue
            }
        }

        override fun putValueTo(editor: SharedPreferences.Editor) {
            editor.putString(key, codec.encode(getValue()))
        }
    }


    class PInt(sharedPreferences: SharedPreferences, key: String, defaultValue: Int) :
        ManagedPreference<Int>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Int) {
            try {
                sharedPreferences.edit { putInt(key, value) }
            } catch (e: Exception) {
                sharedPreferences.edit { remove(key).putInt(key, value) }
            }
            fireChange()
        }

        override fun getValue(): Int {
            return try {
                sharedPreferences.getInt(key, defaultValue)
            } catch (e: Exception) {
                setValue(defaultValue)
                defaultValue
            }
        }

        override fun putValueTo(editor: SharedPreferences.Editor) {
            editor.putInt(key, getValue())
        }
    }

    class PLong(sharedPreferences: SharedPreferences, key: String, defaultValue: Long) :
        ManagedPreference<Long>(sharedPreferences, key, defaultValue) {

        override fun setValue(value: Long) {
            sharedPreferences.edit { putLong(key, value) }
        }

        override fun getValue(): Long {
            return try {
                sharedPreferences.getLong(key, defaultValue)
            } catch (e: Exception) {
                setValue(defaultValue)
                defaultValue
            }
        }

        override fun putValueTo(editor: SharedPreferences.Editor) {
            editor.putLong(key, getValue())
        }
    }

    class PFloat(sharedPreferences: SharedPreferences, key: String, defaultValue: Float) :
        ManagedPreference<Float>(sharedPreferences, key, defaultValue) {
        override fun setValue(value: Float) {
            sharedPreferences.edit { putFloat(key, value) }
        }

        override fun getValue(): Float {
            return try {
                sharedPreferences.getFloat(key, defaultValue)
            } catch (e: Exception) {
                setValue(defaultValue)
                defaultValue
            }
        }

        override fun putValueTo(editor: SharedPreferences.Editor) {
            editor.putFloat(key, getValue())
        }
    }

}

