package com.github.treehollow.database

import androidx.room.TypeConverter
import com.github.treehollow.network.ApiResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class LocalCommentConverter {
    @TypeConverter
    fun fromString(value: String?): List<ApiResponse.Comment>? {
        return value?.let {
            val listType =
                Types.newParameterizedType(List::class.java, ApiResponse.Comment::class.java)
            val adapter: JsonAdapter<List<ApiResponse.Comment>> =
                Moshi.Builder().build().adapter(listType)
            adapter.fromJson(it)
        }
    }

    @TypeConverter
    fun fromConfig(conf: List<ApiResponse.Comment>?): String? {
        return conf?.let {
            val listType =
                Types.newParameterizedType(List::class.java, ApiResponse.Comment::class.java)
            val adapter: JsonAdapter<List<ApiResponse.Comment>> =
                Moshi.Builder().build().adapter(listType)
            adapter.toJson(it)
        }
    }
}