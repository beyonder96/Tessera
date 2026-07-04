package com.example.data

data class FootballMatchInfo(
    val teamName: String, // "Flamengo" ou "Brasil"
    val lastMatch: MatchDetail?,
    val nextMatch: MatchDetail?
)

data class MatchDetail(
    val homeTeamName: String,
    val homeTeamLogo: String,
    val awayTeamName: String,
    val awayTeamLogo: String,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val statusShort: String, // "FT" (Finished), "NS" (Not Started), "1H" (First Half)
    val dateFormatted: String, // "20/06 16:00" ou similar
    val leagueName: String
)

data class DetailedFixture(
    val matchDetail: MatchDetail,
    val venueName: String?,
    val events: List<MatchEvent>,
    val homeLineup: List<MatchLineup>,
    val awayLineup: List<MatchLineup>
)

data class MatchEvent(
    val id: Long,
    val minute: Int,
    val typeName: String,
    val typeCode: String?,
    val playerName: String,
    val isHomeTeam: Boolean
)

data class MatchLineup(
    val playerId: Long,
    val playerName: String,
    val playerImage: String?,
    val position: Int?
)
