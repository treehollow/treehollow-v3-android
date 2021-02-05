package com.github.treehollow.repository

import android.content.Context
import com.github.treehollow.data.model.TreeHollowPreferences
import com.github.treehollow.data.model.defaultBooleanPrefs
import com.github.treehollow.utils.Const

object PreferencesRepository {

    fun getPreferences(context: Context): TreeHollowPreferences {
        val sharedPref = context.getSharedPreferences(Const.PreferenceKey, Context.MODE_PRIVATE)
        val prefs = TreeHollowPreferences()
        for ((name, defVal) in prefs.booleanPrefs) {
            val tmp = sharedPref.getBoolean(name, defVal)
            tmp.let {
                prefs.booleanPrefs.put(name, it)
            }
        }
        sharedPref.getString("blockWords", prefs.blockWords).let {
            if (it != null) {
                prefs.blockWords = it
            }
        }
        return prefs
    }

    fun getBooleanPreference(context: Context, name: String, defaultValue: Boolean): Boolean {
        val sharedPref = context.getSharedPreferences(Const.PreferenceKey, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(name, defaultBooleanPrefs[name] ?: defaultValue)
    }

    fun setBooleanPreference(context: Context, name: String, value: Boolean) {
        val sharedPref = context.getSharedPreferences(Const.PreferenceKey, Context.MODE_PRIVATE)
        val edit = sharedPref.edit()
        edit.putBoolean(name, value)
        edit.apply()
    }

    fun setStringPreference(context: Context, name: String, value: String) {
        val sharedPref = context.getSharedPreferences(Const.PreferenceKey, Context.MODE_PRIVATE)
        val edit = sharedPref.edit()
        edit.putString(name, value)
        edit.apply()
    }
}