package com.github.treehollow.database

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Db {
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE `LocalComment` (`pid` INTEGER NOT NULL, `comments` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`pid`))"
            )
        }
    }
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `PushMessage` ADD COLUMN `timestamp` INTEGER"
            )
        }
    }
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "DROP TABLE `PushMessage`"
            )
            database.execSQL("CREATE TABLE IF NOT EXISTS `PushMessage` (`id` INTEGER NOT NULL, `title` TEXT, `body` TEXT, `pid` INTEGER, `cid` INTEGER, `type` INTEGER, `timestamp` INTEGER, PRIMARY KEY(`id`))")
        }
    }

    fun getDatabase(application: Application): AppDatabase {
        return Room.databaseBuilder(application, AppDatabase::class.java, "tree_hollow.db")
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
    }
}