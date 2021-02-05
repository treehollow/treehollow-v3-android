package com.github.treehollow.database

import androidx.room.TypeConverter
import com.github.treehollow.data.model.TreeHollowConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

class ConfigResponseConverter {
    @TypeConverter
    fun fromString(value: String?): TreeHollowConfig? {
        return value?.let {
            val adapter: JsonAdapter<TreeHollowConfig> =
                Moshi.Builder().build().adapter(TreeHollowConfig::class.java)
            adapter.fromJson(it)
        }
    }

    @TypeConverter
    fun fromConfig(conf: TreeHollowConfig?): String? {
        return conf?.let {
            val adapter: JsonAdapter<TreeHollowConfig> =
                Moshi.Builder().build().adapter(TreeHollowConfig::class.java)
            adapter.toJson(it)
        }
    }
}