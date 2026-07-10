package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class RssArticle(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String,
    val sourceName: String
)

class RssRepository(context: Context) {
    private val rssParser = RssParserBuilder().build()
    private val prefs: SharedPreferences = context.getSharedPreferences("tessera_rss_prefs", Context.MODE_PRIVATE)

    // Example popular Brazilian tech & news RSS feeds
    private val defaultFeeds = mapOf(
        "G1 Tecnologia" to "https://g1.globo.com/rss/g1/tecnologia/",
        "G1 Economia" to "https://g1.globo.com/rss/g1/economia/",
        "BBC Brasil" to "https://feeds.bbci.co.uk/portuguese/rss.xml"
    )

    fun getFeeds(): Map<String, String> {
        val jsonStr = prefs.getString("custom_feeds", null)
        if (jsonStr != null) {
            try {
                val json = JSONObject(jsonStr)
                val map = mutableMapOf<String, String>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.getString(key)
                }
                return map
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return defaultFeeds
    }

    fun addFeed(name: String, url: String) {
        val currentFeeds = getFeeds().toMutableMap()
        currentFeeds[name] = url
        saveFeeds(currentFeeds)
    }

    fun removeFeed(name: String) {
        val currentFeeds = getFeeds().toMutableMap()
        currentFeeds.remove(name)
        saveFeeds(currentFeeds)
    }

    private fun saveFeeds(feeds: Map<String, String>) {
        val json = JSONObject()
        feeds.forEach { (name, url) -> json.put(name, url) }
        prefs.edit().putString("custom_feeds", json.toString()).apply()
    }

    suspend fun getLatestNews(): List<RssArticle> = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<RssArticle>()
        val feedsToFetch = getFeeds()

        for ((sourceName, url) in feedsToFetch) {
            try {
                val channel: RssChannel = rssParser.getRssChannel(url)
                
                val articles = channel.items.mapNotNull { item ->
                    if (item.title == null || item.link == null) return@mapNotNull null
                    
                    RssArticle(
                        title = item.title!!,
                        description = item.description?.replace(Regex("<.*?>"), "")?.take(150)?.plus("..."),
                        url = item.link!!,
                        imageUrl = item.image,
                        publishedAt = formatPubDate(item.pubDate),
                        sourceName = sourceName
                    )
                }
                allArticles.addAll(articles)
            } catch (e: Exception) {
                e.printStackTrace()
                // Skip if this feed fails
            }
        }

        // Shuffle so the feed is dynamic, and take top 15
        allArticles.shuffled().take(15)
    }

    private fun formatPubDate(pubDate: String?): String {
        if (pubDate.isNullOrBlank()) return ""
        return pubDate.take(22) // Quick fallback format
    }
}
