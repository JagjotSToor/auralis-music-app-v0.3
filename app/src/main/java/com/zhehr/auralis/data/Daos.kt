package com.zhehr.auralis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class SyncInfo(val id: Long, val dateModified: Long, val sizeBytes: Long)
data class AlbumRow(val songId: Long, val albumId: Long, val album: String, val artist: String, val songCount: Int)
data class ArtistRow(val songId: Long, val albumId: Long, val artist: String, val songCount: Int, val albumCount: Int)
data class FolderRow(val folder: String, val songCount: Int)
data class PlaylistRow(val id: Long, val name: String, val songCount: Int)

/** Visible = not hidden and of an allowed kind. Two spellings: with and without the "s." alias. */
const val VIS = "id NOT IN (SELECT songId FROM hidden_song) AND kind IN (:kinds)"
const val VIS_S = "s.id NOT IN (SELECT songId FROM hidden_song) AND s.kind IN (:kinds)"

@Dao
interface SongDao {
    @Query("SELECT id, dateModified, sizeBytes FROM song")
    suspend fun syncInfo(): List<SyncInfo>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM song WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM song WHERE $VIS ORDER BY title COLLATE NOCASE")
    fun observeAll(kinds: List<String>): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM song WHERE $VIS")
    fun observeCount(kinds: List<String>): Flow<Int>

    @Query("SELECT * FROM song WHERE $VIS ORDER BY dateAdded DESC LIMIT :n")
    fun observeRecentlyAdded(n: Int, kinds: List<String>): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM song s INNER JOIN favorite f ON f.songId = s.id WHERE $VIS_S ORDER BY f.addedAt DESC")
    fun observeFavorites(kinds: List<String>): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM song s INNER JOIN " +
            "(SELECT songId, MAX(playedAt) AS m FROM play_history GROUP BY songId) h ON h.songId = s.id " +
            "WHERE $VIS_S ORDER BY h.m DESC LIMIT :n"
    )
    fun observeRecentlyPlayed(n: Int, kinds: List<String>): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM song s INNER JOIN " +
            "(SELECT songId, COUNT(*) AS c, MAX(playedAt) AS m FROM play_history GROUP BY songId) h ON h.songId = s.id " +
            "WHERE $VIS_S ORDER BY h.c DESC, h.m DESC LIMIT :n"
    )
    fun observeMostPlayed(n: Int, kinds: List<String>): Flow<List<SongEntity>>

    @Query(
        "SELECT MIN(id) AS songId, albumId, album, MIN(artist) AS artist, COUNT(*) AS songCount " +
            "FROM song WHERE $VIS GROUP BY albumId ORDER BY album COLLATE NOCASE"
    )
    fun observeAlbums(kinds: List<String>): Flow<List<AlbumRow>>

    @Query(
        "SELECT MIN(id) AS songId, MIN(albumId) AS albumId, artist, COUNT(*) AS songCount, " +
            "COUNT(DISTINCT albumId) AS albumCount FROM song WHERE $VIS GROUP BY artist ORDER BY artist COLLATE NOCASE"
    )
    fun observeArtists(kinds: List<String>): Flow<List<ArtistRow>>

    @Query("SELECT folder, COUNT(*) AS songCount FROM song WHERE $VIS GROUP BY folder ORDER BY folder COLLATE NOCASE")
    fun observeFolders(kinds: List<String>): Flow<List<FolderRow>>

    @Query("SELECT * FROM song WHERE albumId = :albumId AND $VIS ORDER BY trackNo, title COLLATE NOCASE")
    fun observeByAlbum(albumId: Long, kinds: List<String>): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE artist = :artist AND $VIS ORDER BY album COLLATE NOCASE, trackNo, title COLLATE NOCASE")
    fun observeByArtist(artist: String, kinds: List<String>): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE folder = :folder AND $VIS ORDER BY title COLLATE NOCASE")
    fun observeByFolder(folder: String, kinds: List<String>): Flow<List<SongEntity>>

    @Query(
        "SELECT s.* FROM song s INNER JOIN playlist_song p ON p.songId = s.id " +
            "WHERE p.playlistId = :playlistId AND $VIS_S ORDER BY p.position"
    )
    fun observeByPlaylist(playlistId: Long, kinds: List<String>): Flow<List<SongEntity>>

    @Query(
        "SELECT * FROM song WHERE $VIS AND (title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY title COLLATE NOCASE LIMIT 200"
    )
    suspend fun search(q: String, kinds: List<String>): List<SongEntity>

    @Query("SELECT * FROM song WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<SongEntity>

    /** Recordings section: ignores the "include in library" settings but still respects hidden songs. */
    @Query("SELECT * FROM song WHERE $VIS ORDER BY dateAdded DESC")
    fun observeRecordings(kinds: List<String>): Flow<List<SongEntity>>

    @Query("SELECT folder, COUNT(*) AS songCount FROM song WHERE kind != 'MUSIC' GROUP BY folder ORDER BY folder COLLATE NOCASE")
    fun observeRecordingFolders(): Flow<List<FolderRow>>

    @Query("SELECT s.* FROM song s INNER JOIN hidden_song h ON h.songId = s.id ORDER BY h.hiddenAt DESC")
    fun observeHidden(): Flow<List<SongEntity>>
}

@Dao
interface HiddenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hide(entry: HiddenSongEntity)

    @Query("DELETE FROM hidden_song WHERE songId = :songId")
    suspend fun unhide(songId: Long)

    @Query("SELECT songId FROM hidden_song")
    suspend fun hiddenIds(): List<Long>
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE songId = :songId")
    suspend fun remove(songId: Long)

    @Query("SELECT songId FROM favorite")
    fun observeIds(): Flow<List<Long>>
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSong(entry: PlaylistSongEntity): Long

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun clear(playlistId: Long)

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    @Query("SELECT name FROM playlist WHERE id = :playlistId")
    suspend fun name(playlistId: Long): String?

    @Query(
        "SELECT p.id AS id, p.name AS name, COUNT(ps.songId) AS songCount FROM playlist p " +
            "LEFT JOIN playlist_song ps ON ps.playlistId = p.id GROUP BY p.id ORDER BY p.createdAt DESC"
    )
    fun observeAll(): Flow<List<PlaylistRow>>
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun add(entry: PlayHistoryEntity)
}
