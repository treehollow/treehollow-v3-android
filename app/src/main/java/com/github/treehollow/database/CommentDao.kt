package com.github.treehollow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CommentDao {
    @Query("SELECT MAX(pid) FROM LocalComment LIMIT 1")
    suspend fun getMaxCachedPoseId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLocalComment(item: LocalComment)

    @Query("SELECT * FROM LocalComment WHERE pid = :pid")
    suspend fun getCommentCache(pid: Int): LocalComment?
}