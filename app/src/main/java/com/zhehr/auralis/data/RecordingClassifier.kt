package com.zhehr.auralis.data

/**
 * Heuristic: folder names and file-name patterns only. It can be wrong, which is why the user can
 * mark a folder as "treat as music" in Settings. Nothing here ever deletes or moves a file.
 */
object RecordingClassifier {
    private val callName = Regex("""^(call[ _-]?rec|recorded[ _-]?call|\+?\d{10,15}[ _.-])""")
    private val screenName = Regex("""^(screen[ _-]?rec|screenrecord)""")
    private val voiceName = Regex("""^(voice[ _-]?(\d|note|memo|rec)|recording[ _-]?\d|new[ _-]?recording|rec[ _-]?\d|ptt-|vn[ _-]\d|aud-\d)""")

    private val callFolders = listOf("callrec", "call rec", "call_rec", "call-rec", "recorded call", "phonerec", "phone rec", "/call/", "/calls/")
    private val screenFolders = listOf("screenrec", "screen rec", "screen_rec", "screencast")
    private val voiceFolders = listOf(
        "voicerec", "voice rec", "voice_rec", "soundrec", "sound rec", "sound_rec", "voice memo", "voicememo",
        "voice note", "voicenote", "/recordings/", "/recording/", "/recorder/", "dictaphone",
    )

    fun classify(folder: String, fileName: String): String {
        val f = "/" + folder.lowercase().trim('/') + "/"
        val n = fileName.lowercase()
        return when {
            callFolders.any { f.contains(it) } || callName.containsMatchIn(n) -> Kind.CALL
            screenFolders.any { f.contains(it) } || screenName.containsMatchIn(n) -> Kind.SCREEN
            voiceFolders.any { f.contains(it) } || voiceName.containsMatchIn(n) -> Kind.VOICE
            else -> Kind.MUSIC
        }
    }
}
