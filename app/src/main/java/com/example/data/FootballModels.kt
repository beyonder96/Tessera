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
