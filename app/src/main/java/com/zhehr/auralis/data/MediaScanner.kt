package com.zhehr.auralis.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media as M
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class ScanState(val running: Boolean = false, val message: String = "")

/**
 * Incremental scanner. MediaStore already holds the metadata, so we read it in one query and only
 * write rows whose (id, dateModified, size) differ from what the database has. A full re-evaluation
 * only happens when asked (force) or when the recording-detection rules change (CLASSIFIER_VERSION).
 */
class MediaScanner(
    private val context: Context,
    private val db: AuralisDatabase,
    private val settings: SettingsStore,
) {
    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()
    private val mutex = Mutex()

    @Suppress("DEPRECATION")
    private val folderColumn: String =
        if (Build.VERSION.SDK_INT >= 29) M.RELATIVE_PATH else M.DATA

    suspend fun scan(force: Boolean = false): Unit = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext
        try {
            _state.value = ScanState(true, "Scanning your music…")
            val dao = db.songDao()
            val full = force || settings.classifierVersion() != CLASSIFIER_VERSION
            val overrides = settings.musicFolders.first()
            val existing = dao.syncInfo().associateBy { it.id }
            val seen = HashSet<Long>()
            val batch = ArrayList<SongEntity>()
            var added = 0
            var changed = 0

            val projection = arrayOf(
                M._ID, M.TITLE, M.ARTIST, M.ALBUM, M.ALBUM_ID, M.YEAR, M.TRACK,
                M.DURATION, M.SIZE, M.DATE_ADDED, M.DATE_MODIFIED, M.IS_MUSIC, M.DISPLAY_NAME, folderColumn,
            )
            // Recordings are not flagged as music by Android, so read all real audio and classify it ourselves.
            val selection = "${M.DURATION} >= 1000 AND ${M.IS_RINGTONE} = 0 AND ${M.IS_NOTIFICATION} = 0 AND ${M.IS_ALARM} = 0"

            context.contentResolver.query(M.EXTERNAL_CONTENT_URI, projection, selection, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(M._ID)
                val titleCol = c.getColumnIndexOrThrow(M.TITLE)
                val artistCol = c.getColumnIndexOrThrow(M.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(M.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(M.ALBUM_ID)
                val yearCol = c.getColumnIndexOrThrow(M.YEAR)
                val trackCol = c.getColumnIndexOrThrow(M.TRACK)
                val durCol = c.getColumnIndexOrThrow(M.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(M.SIZE)
                val addedCol = c.getColumnIndexOrThrow(M.DATE_ADDED)
                val modCol = c.getColumnIndexOrThrow(M.DATE_MODIFIED)
                val isMusicCol = c.getColumnIndexOrThrow(M.IS_MUSIC)
                val nameCol = c.getColumnIndexOrThrow(M.DISPLAY_NAME)
                val folderCol = c.getColumnIndexOrThrow(folderColumn)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val mod = c.getLong(modCol)
                    val size = c.getLong(sizeCol)
                    val old = existing[id]
                    if (!full && old != null && old.dateModified == mod && old.sizeBytes == size) {
                        seen.add(id)
                        continue
                    }

                    val dur = c.getLong(durCol)
                    val rawFolder = c.getString(folderCol).orEmpty()
                    val folder = (if (Build.VERSION.SDK_INT >= 29) rawFolder.trimEnd('/') else rawFolder.substringBeforeLast('/', ""))
                        .ifBlank { "Unknown folder" }
                    val overridden = folder in overrides
                    val kind = if (overridden) Kind.MUSIC else RecordingClassifier.classify(folder, c.getString(nameCol).orEmpty())
                    val isMusic = c.getInt(isMusicCol) != 0
                    val accept = when {
                        kind != Kind.MUSIC -> dur >= 1000
                        overridden -> dur >= 1000
                        else -> isMusic && dur >= 10000
                    }
                    if (!accept) continue
                    seen.add(id)

                    batch.add(
                        SongEntity(
                            id = id,
                            title = c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown title",
                            artist = clean(c.getString(artistCol), "Unknown artist"),
                            album = clean(c.getString(albumCol), "Unknown album"),
                            albumId = c.getLong(albumIdCol),
                            year = c.getInt(yearCol),
                            trackNo = c.getInt(trackCol) % 1000,
                            durationMs = dur,
                            sizeBytes = size,
                            dateAdded = c.getLong(addedCol),
                            dateModified = mod,
                            folder = folder,
                            kind = kind,
                        )
                    )
                    if (old == null) added++ else changed++
                    if (batch.size >= 300) {
                        dao.upsertAll(batch.toList())
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) dao.upsertAll(batch.toList())

            val gone = existing.keys.filter { it !in seen }
            gone.chunked(500).forEach { dao.deleteByIds(it) }
            settings.setClassifierVersion(CLASSIFIER_VERSION)

            _state.value = ScanState(false, "${seen.size} audio files · $added new, $changed updated, ${gone.size} removed")
        } catch (e: SecurityException) {
            _state.value = ScanState(false, "Music permission is missing.")
        } catch (e: Exception) {
            _state.value = ScanState(false, "Scan failed: ${e.message}")
        } finally {
            mutex.unlock()
        }
    }

    private fun clean(value: String?, fallback: String): String =
        if (value.isNullOrBlank() || value == "<unknown>") fallback else value

    companion object {
        /** Bump when RecordingClassifier rules change so existing libraries get re-evaluated once. */
        const val CLASSIFIER_VERSION = 1
    }
}
