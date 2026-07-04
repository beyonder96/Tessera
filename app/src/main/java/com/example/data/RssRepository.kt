package com.example.data

import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RssArticle(
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String,
    val sourceName: String
)

class RssRepository {
    private val rssParser = RssParserBuilder().build()

    // Example popular Brazilian tech & news RSS feeds
    private val defaultFeeds = mapOf(
        "G1 Tecnologia" to "https://g1.globo.com/rss/g1/tecnologia/",
        "G1 Economia" to "https://g1.globo.com/rss/g1/economia/",
        "BBC Brasil" to "https://feeds.bbci.co.uk/portuguese/rss.xml"
    )

    suspend fun getLatestNews(): List<RssArticle> = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<RssArticle>()

        for ((sourceName, url) in defaultFeeds) {
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
