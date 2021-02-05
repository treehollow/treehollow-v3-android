package com.github.treehollow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.treehollow.network.ApiResponse

@Dao
interface PushMessageDao {
    @Query("SELECT MAX(id) FROM PushMessage LIMIT 1")
    suspend fun getMaxPushId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePushMessage(item: List<ApiResponse.PushMessage>)
}