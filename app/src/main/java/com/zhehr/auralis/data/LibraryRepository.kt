package com.zhehr.auralis.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepository(db: AuralisDatabase, settings: SettingsStore) {
    private val songs = db.songDao()
    private val fav = db.favoriteDao()
    private val pl = db.playlistDao()
    private val hist = db.historyDao()
    private val hidden = db.hiddenDao()

    /** Which kinds of audio appear in normal music views. Hidden songs are always excluded by SQL. */
    private val kinds: Flow<List<String>> =
        combine(settings.includeRecordings, settings.includeCall) { rec, call ->
            buildList {
                add(Kind.MUSIC)
                if (rec) { add(Kind.VOICE); add(Kind.SCREEN) }
                if (call) add(Kind.CALL)
            }
        }.distinctUntilChanged()

    val allSongs: Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeAll(it) }
    val count: Flow<Int> = kinds.flatMapLatest { songs.observeCount(it) }
    val albums: Flow<List<AlbumRow>> = kinds.flatMapLatest { songs.observeAlbums(it) }
    val artists: Flow<List<ArtistRow>> = kinds.flatMapLatest { songs.observeArtists(it) }
    val folders: Flow<List<FolderRow>> = kinds.flatMapLatest { songs.observeFolders(it) }
    val recentlyAdded: Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeRecentlyAdded(20, it) }
    val recentlyPlayed: Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeRecentlyPlayed(20, it) }
    val mostPlayed: Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeMostPlayed(20, it) }
    val favorites: Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeFavorites(it) }
    val favoriteIds: Flow<Set<Long>> = fav.observeIds().map { it.toSet() }
    val playlists: Flow<List<PlaylistRow>> = pl.observeAll()
    val hiddenSongs: Flow<List<SongEntity>> = songs.observeHidden()
    val recordingFolders: Flow<List<FolderRow>> = songs.observeRecordingFolders()

    fun byAlbum(id: Long): Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeByAlbum(id, it) }
    fun byArtist(name: String): Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeByArtist(name, it) }
    fun byFolder(path: String): Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeByFolder(path, it) }
    fun byPlaylist(id: Long): Flow<List<SongEntity>> = kinds.flatMapLatest { songs.observeByPlaylist(id, it) }
    fun recordings(of: List<String>): Flow<List<SongEntity>> = songs.observeRecordings(of)

    suspend fun search(q: String): List<SongEntity> = songs.search(q, kinds.first())

    suspend fun hideSong(id: Long) = hidden.hide(HiddenSongEntity(id, System.currentTimeMillis()))
    suspend fun unhideSong(id: Long) = hidden.unhide(id)

    suspend fun setFavorite(id: Long, on: Boolean) {
        if (on) fav.add(FavoriteEntity(id, System.currentTimeMillis())) else fav.remove(id)
    }

    suspend fun createPlaylist(name: String): Long =
        pl.insert(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    /** Returns false if the song was already in the playlist. */
    suspend fun addToPlaylist(playlistId: Long, songId: Long): Boolean {
        val pos = pl.nextPosition(playlistId)
        return pl.addSong(PlaylistSongEntity(playlistId, songId, pos, System.currentTimeMillis())) != -1L
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: Long) = pl.removeSong(playlistId, songId)

    suspend fun deletePlaylist(id: Long) {
        pl.clear(id)
        pl.delete(id)
    }

    suspend fun playlistName(id: Long): String? = pl.name(id)

    suspend fun createPlaylistWithSongs(name: String, songIds: List<Long>): Long {
        val id = createPlaylist(name)
        songIds.distinct().forEach { addToPlaylist(id, it) }
        return id
    }

    suspend fun recordPlay(songId: Long) = hist.add(PlayHistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))

    /** Chunked lookup (SQLite variable limit), in the order of [ids], skipping hidden songs. */
    suspend fun songsByIdsOrdered(ids: List<Long>): List<SongEntity> {
        val hid = hidden.hiddenIds().toSet()
        val map = ids.chunked(500).flatMap { songs.byIds(it) }.associateBy { it.id }
        return ids.mapNotNull { id -> map[id]?.takeIf { it.id !in hid } }
    }
}
