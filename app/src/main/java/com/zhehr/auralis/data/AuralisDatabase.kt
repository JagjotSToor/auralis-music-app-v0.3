package com.zhehr.auralis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        FavoriteEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayHistoryEntity::class,
        HiddenSongEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun hiddenDao(): HiddenDao

    companion object {
        fun create(context: Context): AuralisDatabase =
            Room.databaseBuilder(context.applicationContext, AuralisDatabase::class.java, "auralis.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** v0.2 -> v0.3: recording classification + hidden songs. Keeps favorites, playlists and history. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE song ADD COLUMN kind TEXT NOT NULL DEFAULT 'MUSIC'")
        db.execSQL("CREATE TABLE IF NOT EXISTS hidden_song (songId INTEGER NOT NULL, hiddenAt INTEGER NOT NULL, PRIMARY KEY(songId))")
    }
}
