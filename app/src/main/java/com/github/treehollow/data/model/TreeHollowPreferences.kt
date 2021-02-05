package com.github.treehollow.data.model

//@JsonClass(generateAdapter = true)
data class TreeHollowPreferences(
    var booleanPrefs: MutableMap<String, Boolean> = defaultBooleanPrefs,
    // block words as comma separated strings, e.g. "屏蔽词,另一个屏蔽词"
    var blockWords: String = ""
) {

    fun dark_mode(): Boolean? {
        return booleanPrefs["dark_mode"]
    }

    fun fold_hollows(): Boolean? {
        return booleanPrefs["fold_hollows"]
    }
}

val defaultBooleanPrefs = mutableMapOf(
    "dark_mode" to false,
    "fold_hollows" to true,
)