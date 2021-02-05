package com.github.treehollow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.treehollow.data.model.ConfigWithUrl

@Dao
interface ConfigDao {
    @Query("SELECT * FROM ConfigWithUrl LIMIT 1")
    suspend fun loadConfig(): ConfigWithUrl

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: ConfigWithUrl)

    @Query("DELETE FROM ConfigWithUrl")
    suspend fun deleteConfig()
}