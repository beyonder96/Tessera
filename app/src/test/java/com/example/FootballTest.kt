package com.example

import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FootballTest {

    @Test
    fun testJsonParsing() {
        val json = """
            {
              "response": [
                {
                  "fixture": {
                    "id": 12345,
                    "date": "2026-06-20T16:00:00+00:00",
                    "status": {
                      "long": "Match Finished",
                      "short": "FT",
                      "elapsed": 90
                    }
                  },
                  "league": {
                    "id": 71,
                    "name": "Brasileirão Série A",
                    "logo": "https://media.api-sports.io/football/leagues/71.png"
                  },
                  "teams": {
                    "home": {
                      "id": 127,
                      "name": "Flamengo",
                      "logo": "https://media.api-sports.io/football/teams/127.png"
                    },
                    "away": {
                      "id": 133,
                      "name": "Vasco",
                      "logo": "https://media.api-sports.io/football/teams/133.png"
                    }
                  },
                  "goals": {
                    "home": 2,
                    "away": 0
                  }
                }
              ]
            }
        """.trimIndent()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(FootballFixturesResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertNotNull(response?.response)
        assertEquals(1, response?.response?.size)

        val element = response!!.response!!.first()
        assertEquals(12345, element.fixture?.id)
        assertEquals("FT", element.fixture?.status?.short)
        assertEquals("Brasileirão Série A", element.league?.name)
        assertEquals("Flamengo", element.teams?.home?.name)
        assertEquals(127, element.teams?.home?.id)
        assertEquals("Vasco", element.teams?.away?.name)
        assertEquals(2, element.goals?.home)
        assertEquals(0, element.goals?.away)
    }

    @Test
    fun testModelMapping() {
        val detail = MatchDetail(
            homeTeamName = "Flamengo",
            homeTeamLogo = "https://media.api-sports.io/football/teams/127.png",
            awayTeamName = "Vasco",
            awayTeamLogo = "https://media.api-sports.io/football/teams/133.png",
            homeGoals = 2,
            awayGoals = 0,
            statusShort = "FT",
            dateFormatted = "20/06 13:00",
            leagueName = "Série A"
        )

        val info = FootballMatchInfo(
            teamName = "Flamengo",
            lastMatch = detail,
            nextMatch = null
        )

        assertEquals("Flamengo", info.teamName)
        assertNotNull(info.lastMatch)
        assertEquals("Vasco", info.lastMatch?.awayTeamName)
        assertEquals(2, info.lastMatch?.homeGoals)
        assertEquals(0, info.lastMatch?.awayGoals)
        assertEquals("FT", info.lastMatch?.statusShort)
        assertEquals("20/06 13:00", info.lastMatch?.dateFormatted)
        assertEquals("Série A", info.lastMatch?.leagueName)
    }

    @Test
    fun testDateFormattingLogic() {
        val rawDate = "2026-06-20T16:00:00+00:00"
        val formatted = try {
            val instant = Instant.parse(rawDate)
            val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                .withZone(ZoneId.of("UTC"))
            formatter.format(instant)
        } catch (e: Exception) {
            rawDate.take(16).replace("T", " ")
        }

        assertEquals("20/06 16:00", formatted)
    }
}
