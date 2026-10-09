package com.zhehr.auralis.data

enum class HomeSection(val label: String) {
    RECENTLY_PLAYED("Recently played"),
    RECENTLY_ADDED("Recently added"),
    MOST_PLAYED("Most played"),
    FAVORITES("Favorites"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ALL_SONGS("All songs"),
}

enum class HomeDensity(val label: String, val card: Int) {
    COMPACT("Compact", 104),
    COMFORTABLE("Comfortable", 132),
    VISUAL("Visual", 168),
}

data class HomeConfig(val preset: String, val order: List<HomeSection>, val density: HomeDensity)

data class HomePreset(
    val id: String,
    val title: String,
    val blurb: String,
    val order: List<HomeSection>,
    val density: HomeDensity,
) {
    fun toConfig() = HomeConfig(id, order, density)
}

object HomePresets {
    val BALANCED = HomePreset("balanced", "Balanced", "A bit of everything, comfortable cards.",
        listOf(HomeSection.RECENTLY_PLAYED, HomeSection.RECENTLY_ADDED, HomeSection.MOST_PLAYED, HomeSection.FAVORITES, HomeSection.PLAYLISTS), HomeDensity.COMFORTABLE)

    val all = listOf(
        HomePreset("all_songs", "All Songs", "Your whole library first, compact.",
            listOf(HomeSection.ALL_SONGS, HomeSection.RECENTLY_PLAYED, HomeSection.FAVORITES), HomeDensity.COMPACT),
        HomePreset("favorites", "Favorites", "Your favorites on top.",
            listOf(HomeSection.FAVORITES, HomeSection.RECENTLY_PLAYED, HomeSection.MOST_PLAYED), HomeDensity.COMFORTABLE),
        HomePreset("playlists", "Playlists", "Jump straight into your playlists.",
            listOf(HomeSection.PLAYLISTS, HomeSection.FAVORITES, HomeSection.RECENTLY_PLAYED), HomeDensity.COMFORTABLE),
        HomePreset("recent", "Recently Played", "Pick up where you left off.",
            listOf(HomeSection.RECENTLY_PLAYED, HomeSection.MOST_PLAYED, HomeSection.RECENTLY_ADDED), HomeDensity.COMFORTABLE),
        HomePreset("most", "Most Played", "Your heavy rotation first.",
            listOf(HomeSection.MOST_PLAYED, HomeSection.RECENTLY_PLAYED, HomeSection.FAVORITES), HomeDensity.COMFORTABLE),
        HomePreset("immersive", "Immersive", "Large artwork, fewer sections.",
            listOf(HomeSection.RECENTLY_PLAYED, HomeSection.ALBUMS, HomeSection.FAVORITES), HomeDensity.VISUAL),
        BALANCED,
        HomePreset("visual", "Visual", "Albums and artwork everywhere.",
            listOf(HomeSection.ALBUMS, HomeSection.RECENTLY_ADDED, HomeSection.FAVORITES, HomeSection.RECENTLY_PLAYED), HomeDensity.VISUAL),
    )

    val DEFAULT = BALANCED
    fun get(id: String): HomePreset = all.firstOrNull { it.id == id } ?: DEFAULT
    fun titleFor(id: String): String = all.firstOrNull { it.id == id }?.title ?: if (id == "custom") "Custom" else DEFAULT.title
}
