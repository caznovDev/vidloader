package com.example.data.repository

import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.Playlist
import com.example.data.model.QualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MediaRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    private val curatedPlaylists = listOf(
        Playlist(
            id = "pl_summer_hits",
            title = "Summer Release & Dance Hits",
            creator = "Ruby Von Rails",
            thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            description = "Hot summer party releases, EDM dance beats and festival vibes.",
            trackCount = 5,
            tracks = listOf(
                MediaItem(
                    id = "sm_01",
                    title = "2021 Summer Release - Crazy Angels",
                    channel = "Happy Sunday Records",
                    duration = "3:42",
                    durationSeconds = 222,
                    thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                    views = "2.8M views",
                    publishedTime = "1 year ago",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "sm_02",
                    title = "Natalya Undergrowth - Passionate Dance",
                    channel = "Art Music Collective",
                    duration = "4:15",
                    durationSeconds = 255,
                    thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                    views = "1.5M views",
                    publishedTime = "8 months ago",
                    mediaType = MediaType.VIDEO
                ),
                MediaItem(
                    id = "sm_03",
                    title = "Free Birds - Hello Autumn",
                    channel = "Acoustic Records",
                    duration = "3:18",
                    durationSeconds = 198,
                    thumbnailUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
                    views = "920K views",
                    publishedTime = "10 months ago",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "sm_04",
                    title = "Woman Rocker - Crazy City",
                    channel = "Wild Echoes Studio",
                    duration = "4:32",
                    durationSeconds = 272,
                    thumbnailUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
                    views = "3.1M views",
                    publishedTime = "2 years ago",
                    mediaType = MediaType.VIDEO
                ),
                MediaItem(
                    id = "sm_05",
                    title = "Coy Love - Handsome Mate",
                    channel = "Inverness Music",
                    duration = "2:54",
                    durationSeconds = 174,
                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                    views = "640K views",
                    publishedTime = "3 weeks ago",
                    mediaType = MediaType.AUDIO
                )
            )
        ),
        Playlist(
            id = "pl_drum_jazz",
            title = "Rodney Artichoke - Jazz & Drum Show",
            creator = "Inverness Music",
            thumbnailUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=600&auto=format&fit=crop&q=80",
            description = "Intense acoustic drums, jazz improvisation, and live stage solos.",
            trackCount = 4,
            tracks = listOf(
                MediaItem(
                    id = "dj_01",
                    title = "Archibald Northbottom - Voice of the Goddess",
                    channel = "Inverness Music",
                    duration = "6:20",
                    durationSeconds = 380,
                    thumbnailUrl = "https://images.unsplash.com/photo-1519671482749-fd09be7ccebf?w=600&auto=format&fit=crop&q=80",
                    views = "4.2M views",
                    publishedTime = "7 days ago",
                    mediaType = MediaType.VIDEO
                ),
                MediaItem(
                    id = "dj_02",
                    title = "Lurch Schpelichek - Perfect Coexistence & Eternal Love",
                    channel = "Inverness Music",
                    duration = "7:12",
                    durationSeconds = 432,
                    thumbnailUrl = "https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=600&auto=format&fit=crop&q=80",
                    views = "830K views",
                    publishedTime = "3 days ago",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "dj_03",
                    title = "Richard Tea - Blue Scream",
                    channel = "Moving Sound Lab",
                    duration = "3:15",
                    durationSeconds = 195,
                    thumbnailUrl = "https://images.unsplash.com/photo-1445985543470-41fdd6ce388d?w=600&auto=format&fit=crop&q=80",
                    views = "1.8M views",
                    publishedTime = "4 months ago",
                    mediaType = MediaType.VIDEO
                ),
                MediaItem(
                    id = "dj_04",
                    title = "Valentino Morose - Miss Universe",
                    channel = "Desmond Eagle",
                    duration = "4:48",
                    durationSeconds = 288,
                    thumbnailUrl = "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=600&auto=format&fit=crop&q=80",
                    views = "5.5M views",
                    publishedTime = "6 months ago",
                    mediaType = MediaType.AUDIO
                )
            )
        ),
        Playlist(
            id = "pl_lofi_rain",
            title = "Lo-Fi Beats & Rainy Nights",
            creator = "Chillhop Collective",
            thumbnailUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            description = "Soft aesthetic lo-fi beats, gentle vinyl crackles and mellow piano.",
            trackCount = 4,
            tracks = listOf(
                MediaItem(
                    id = "lf_01",
                    title = "Star Music - Rain of Me",
                    channel = "Desmond Eagle",
                    duration = "3:24",
                    durationSeconds = 204,
                    thumbnailUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
                    views = "3.8M views",
                    publishedTime = "1 month ago",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "lf_02",
                    title = "Lance Bogrol - Masked Ball",
                    channel = "Art Music",
                    duration = "5:10",
                    durationSeconds = 310,
                    thumbnailUrl = "https://images.unsplash.com/photo-1520523839898-50712825e3a7?w=600&auto=format&fit=crop&q=80",
                    views = "1.2M views",
                    publishedTime = "2 months ago",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "lf_03",
                    title = "Nigel Nigel - Long River",
                    channel = "River Records",
                    duration = "4:02",
                    durationSeconds = 242,
                    thumbnailUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=600&auto=format&fit=crop&q=80",
                    views = "760K views",
                    publishedTime = "5 months ago",
                    mediaType = MediaType.VIDEO
                ),
                MediaItem(
                    id = "lf_04",
                    title = "Midnight Horizon - Neon Drive",
                    channel = "Synthwave Underground",
                    duration = "4:20",
                    durationSeconds = 260,
                    thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                    views = "2.1M views",
                    publishedTime = "3 months ago",
                    mediaType = MediaType.VIDEO
                )
            )
        )
    )

    fun getTrendingItems(): List<MediaItem> {
        return curatedPlaylists.flatMap { it.tracks }.distinctBy { it.id }
    }

    fun getPlaylists(): List<Playlist> {
        return curatedPlaylists
    }

    fun getPlaylistById(id: String): Playlist? {
        return curatedPlaylists.firstOrNull { it.id == id }
    }

    suspend fun search(query: String, filterType: String = "ALL"): List<MediaItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext getTrendingItems()

        val allItems = curatedPlaylists.flatMap { it.tracks }.distinctBy { it.id }
        val matched = allItems.filter { item ->
            item.title.lowercase().contains(q) ||
            item.channel.lowercase().contains(q)
        }

        val filtered = when (filterType) {
            "MUSIC" -> matched.filter { it.mediaType == MediaType.AUDIO }
            "VIDEOS" -> matched.filter { it.mediaType == MediaType.VIDEO }
            else -> matched
        }

        if (filtered.isNotEmpty()) {
            filtered
        } else {
            // Dynamic generation for any query string so user can search anything!
            listOf(
                MediaItem(
                    id = "search_${q.hashCode()}_1",
                    title = "$query - Official Audio Remastered",
                    channel = "Top Hits Music",
                    duration = "3:30",
                    durationSeconds = 210,
                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                    views = "1.9M views",
                    publishedTime = "Recently uploaded",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "search_${q.hashCode()}_2",
                    title = "$query - Live Concert in 4K",
                    channel = "Live Stage TV",
                    duration = "4:45",
                    durationSeconds = 285,
                    thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                    views = "3.4M views",
                    publishedTime = "1 week ago",
                    mediaType = MediaType.VIDEO
                ),
                MediaItem(
                    id = "search_${q.hashCode()}_3",
                    title = "$query (Acoustic Version)",
                    channel = "Acoustic Covers Lounge",
                    duration = "3:12",
                    durationSeconds = 192,
                    thumbnailUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
                    views = "890K views",
                    publishedTime = "2 weeks ago",
                    mediaType = MediaType.AUDIO
                ),
                MediaItem(
                    id = "search_${q.hashCode()}_4",
                    title = "$query - Music Video [HD]",
                    channel = "Universal Vevo",
                    duration = "3:58",
                    durationSeconds = 238,
                    thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                    views = "12M views",
                    publishedTime = "3 months ago",
                    mediaType = MediaType.VIDEO
                )
            )
        }
    }

    fun parseUrl(url: String): MediaItem {
        val cleanUrl = url.trim()
        val isAudio = cleanUrl.contains("music") || cleanUrl.contains("audio") || cleanUrl.endsWith(".mp3")
        val id = cleanUrl.substringAfterLast("/").substringAfter("v=").take(11).ifEmpty { "custom_url" }
        val title = if (cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")) {
            "YouTube Video ($id)"
        } else {
            "Downloaded Stream: ${cleanUrl.take(32)}..."
        }

        return MediaItem(
            id = id,
            title = title,
            channel = "Web Extractor",
            duration = "3:45",
            durationSeconds = 225,
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            views = "Direct Link",
            publishedTime = "Just now",
            mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
            sourceUrl = cleanUrl
        )
    }
}
