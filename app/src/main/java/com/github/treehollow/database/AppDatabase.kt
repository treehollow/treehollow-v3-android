package com.github.treehollow.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.github.treehollow.data.model.ConfigWithUrl
import com.github.treehollow.data.model.TOKEN
import com.github.treehollow.network.ApiResponse

@Database(
    entities = [ConfigWithUrl::class, TOKEN::class, ApiResponse.PushMessage::class, LocalComment::class],
    version = 7
)
@TypeConverters(value = [ConfigResponseConverter::class, LocalCommentConverter::class])
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun tokenDao(): TokenDao
    abstract fun pushMessagesDao(): PushMessageDao
    abstract fun commentDao(): CommentDao
}