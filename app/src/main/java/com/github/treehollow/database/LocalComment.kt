package com.github.treehollow.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.treehollow.network.ApiResponse
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Entity
@JsonClass(generateAdapter = true)
data class LocalComment(
    @PrimaryKey val pid: Int,
    val comments: List<ApiResponse.Comment>,
    val updatedAt: Long,
) : Serializable