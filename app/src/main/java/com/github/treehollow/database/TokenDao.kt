package com.github.treehollow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.treehollow.data.model.TOKEN

@Dao
interface TokenDao {
    @Query("SELECT * FROM TOKEN LIMIT 1")
    suspend fun loadToken(): TOKEN

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveToken(item: TOKEN)

    @Query("DELETE FROM TOKEN")
    suspend fun deleteToken()
}