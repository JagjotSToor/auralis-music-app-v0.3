package com.zhehr.auralis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zhehr.auralis.theme.MotionLevel
import com.zhehr.auralis.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auralis_settings")

data class SavedSession(val ids: List<Long>, val index: Int, val positionMs: Long)

class SettingsStore(private val context: Context) {
    private object K {
        val THEME = stringPreferencesKey("theme_mode")
        val REDUCE = booleanPreferencesKey("reduce_motion") // legacy, read only for migration
        val MOTION = stringPreferencesKey("motion_level")
        val ACCENT = intPreferencesKey("accent_argb")
        val TEXTURE = booleanPreferencesKey("texture")
        val QUEUE = stringPreferencesKey("queue_ids")
        val QUEUE_INDEX = intPreferencesKey("queue_index")
        val QUEUE_POS = longPreferencesKey("queue_pos")
        val SPEED = floatPreferencesKey("speed")
        val HOME_CHOSEN = booleanPreferencesKey("home_chosen")
        val HOME_PRESET = stringPreferencesKey("home_preset")
        val HOME_ORDER = stringPreferencesKey("home_order")
        val HOME_DENSITY = stringPreferencesKey("home_density")
        val INC_REC = booleanPreferencesKey("include_recordings")
        val INC_CALL = booleanPreferencesKey("include_call_recordings")
        val MUSIC_FOLDERS = stringSetPreferencesKey("music_folder_overrides")
        val CLASSIFIER = intPreferencesKey("classifier_version")
    }

    private val data get() = context.dataStore.data

    // ---- appearance ----
    val themeMode: Flow<ThemeMode> = data.map { p ->
        ThemeMode.entries.firstOrNull { it.name == p[K.THEME] } ?: ThemeMode.SYSTEM
    }
    val accent: Flow<Int?> = data.map { it[K.ACCENT] }
    val motion: Flow<MotionLevel> = data.map { p ->
        MotionLevel.entries.firstOrNull { it.name == p[K.MOTION] }
            ?: if (p[K.REDUCE] == true) MotionLevel.MINIMAL else MotionLevel.BALANCED
    }
    val texture: Flow<Boolean> = data.map { it[K.TEXTURE] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[K.THEME] = mode.name } }
    suspend fun setAccent(argb: Int?) {
        context.dataStore.edit { if (argb == null) it.remove(K.ACCENT) else it[K.ACCENT] = argb }
    }
    suspend fun setMotion(level: MotionLevel) { context.dataStore.edit { it[K.MOTION] = level.name } }
    suspend fun setTexture(on: Boolean) { context.dataStore.edit { it[K.TEXTURE] = on } }

    // ---- home ----
    /** null = still loading, false = never chosen, true = chosen. */
    val homeChosen: Flow<Boolean?> = data.map<Preferences, Boolean?> { it[K.HOME_CHOSEN] ?: false }

    val homeConfig: Flow<HomeConfig> = data.map { p ->
        val base = HomePresets.get(p[K.HOME_PRESET] ?: HomePresets.DEFAULT.id)
        val order = p[K.HOME_ORDER]?.split(',')?.mapNotNull { n -> HomeSection.entries.firstOrNull { it.name == n } }
        val density = HomeDensity.entries.firstOrNull { it.name == p[K.HOME_DENSITY] }
        HomeConfig(p[K.HOME_PRESET] ?: base.id, order ?: base.order, density ?: base.density)
    }

    suspend fun setHomeConfig(cfg: HomeConfig) {
        context.dataStore.edit {
            it[K.HOME_CHOSEN] = true
            it[K.HOME_PRESET] = cfg.preset
            it[K.HOME_ORDER] = cfg.order.joinToString(",") { s -> s.name }
            it[K.HOME_DENSITY] = cfg.density.name
        }
    }

    // ---- library ----
    val includeRecordings: Flow<Boolean> = data.map { it[K.INC_REC] ?: false }
    val includeCall: Flow<Boolean> = data.map { it[K.INC_CALL] ?: false }
    val musicFolders: Flow<Set<String>> = data.map { it[K.MUSIC_FOLDERS] ?: emptySet() }

    suspend fun setIncludeRecordings(on: Boolean) { context.dataStore.edit { it[K.INC_REC] = on } }
    suspend fun setIncludeCall(on: Boolean) { context.dataStore.edit { it[K.INC_CALL] = on } }
    suspend fun setMusicFolder(folder: String, treatAsMusic: Boolean) {
        context.dataStore.edit {
            val cur = it[K.MUSIC_FOLDERS] ?: emptySet()
            it[K.MUSIC_FOLDERS] = if (treatAsMusic) cur + folder else cur - folder
        }
    }
    suspend fun classifierVersion(): Int = data.first()[K.CLASSIFIER] ?: 0
    suspend fun setClassifierVersion(v: Int) { context.dataStore.edit { it[K.CLASSIFIER] = v } }

    // ---- playback ----
    suspend fun getSpeed(): Float = data.first()[K.SPEED] ?: 1f
    suspend fun setSpeed(speed: Float) { context.dataStore.edit { it[K.SPEED] = speed } }

    suspend fun saveSession(ids: List<Long>, index: Int, positionMs: Long) {
        context.dataStore.edit {
            it[K.QUEUE] = ids.joinToString(",")
            it[K.QUEUE_INDEX] = index
            it[K.QUEUE_POS] = positionMs
        }
    }

    suspend fun loadSession(): SavedSession? {
        val p = data.first()
        val ids = (p[K.QUEUE] ?: return null).split(',').mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        return SavedSession(ids, p[K.QUEUE_INDEX] ?: 0, p[K.QUEUE_POS] ?: 0L)
    }
}
