package com.example.data

import android.util.Log
import com.example.data.apifootball.LeagueStandingsInfo
import com.example.data.apifootball.StandingTeamRank
import com.example.data.apifootball.StandingsData
import com.example.data.apifootball.TeamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BrasileiraoRepository {
    private const val TAG = "BrasileiraoRepo"
    private const val GE_URL = "https://ge.globo.com/futebol/brasileirao-serie-a/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getLiveStandings(): StandingsData? = withContext(Dispatchers.IO) {
        // 1. Tenta buscar tabela 100% atualizada em tempo real via feed oficial GE
        try {
            val geStandings = fetchFromGloboEsporte()
            if (geStandings != null && (geStandings.league.standings.firstOrNull()?.isNotEmpty() == true)) {
                return@withContext geStandings
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao buscar tabela no feed GE: ${e.message}", e)
        }

        // 2. Fallback: TheSportsDB
        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val seasonsToTry = listOf(currentYear.toString(), (currentYear - 1).toString())
            for (seasonStr in seasonsToTry) {
                val tableResponse = TheSportsDbApi.service.getLeagueTable("4351", seasonStr)
                val items = tableResponse.table ?: emptyList()
                if (items.isNotEmpty()) {
                    val ranks = items.mapIndexed { index, item ->
                        StandingTeamRank(
                            rank = item.intRank?.toIntOrNull() ?: (index + 1),
                            team = TeamInfo(
                                id = item.idTeam?.toLongOrNull() ?: index.toLong(),
                                name = item.strTeam ?: "Time",
                                logo = item.strBadge ?: item.strLogo
                            ),
                            points = item.intPoints?.toIntOrNull() ?: 0,
                            goalsDiff = item.intGoalDifference?.toIntOrNull() ?: 0,
                            group = "Brasileirão Série A",
                            form = item.strForm,
                            status = null,
                            description = item.strDescription
                        )
                    }
                    return@withContext StandingsData(
                        league = LeagueStandingsInfo(
                            id = 4351L,
                            name = "Brasileirão Série A",
                            country = "Brasil",
                            logo = "https://www.thesportsdb.com/images/media/league/badge/2d3b5b1535384163.png",
                            season = seasonStr.toIntOrNull() ?: currentYear,
                            standings = listOf(ranks)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha no fallback TheSportsDB: ${e.message}", e)
        }

        null
    }

    private fun fetchFromGloboEsporte(): StandingsData? {
        val request = Request.Builder()
            .url(GE_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "GE HTTP Error: ${response.code}")
            return null
        }

        val html = response.body?.string() ?: return null
        val marker = "const classificacao = "
        val startIndex = html.indexOf(marker)
        if (startIndex == -1) {
            Log.w(TAG, "Marcador 'const classificacao' não encontrado no HTML do GE")
            return null
        }

        val jsonStart = startIndex + marker.length
        // Procura o fechamento da instrução JavaScript
        var jsonEnd = html.indexOf(";\n", jsonStart)
        if (jsonEnd == -1) {
            jsonEnd = html.indexOf(";const ", jsonStart)
        }
        if (jsonEnd == -1) {
            jsonEnd = html.indexOf(";</script>", jsonStart)
        }
        if (jsonEnd == -1) {
            jsonEnd = html.indexOf(";", jsonStart)
        }
        if (jsonEnd == -1 || jsonEnd <= jsonStart) {
            Log.w(TAG, "Fim do JSON não encontrado no HTML do GE")
            return null
        }

        val jsonStr = html.substring(jsonStart, jsonEnd).trim()
        val rootObj = JSONObject(jsonStr)
        val classifArray = rootObj.optJSONArray("classificacao") ?: return null

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var editionYear = currentYear
        val edicaoObj = rootObj.optJSONObject("edicao")
        if (edicaoObj != null) {
            val edicaoNome = edicaoObj.optString("nome", "")
            val yearFromTitle = Regex("\\d{4}").find(edicaoNome)?.value?.toIntOrNull()
            if (yearFromTitle != null) {
                editionYear = yearFromTitle
            }
        }

        val ranksList = mutableListOf<StandingTeamRank>()
        for (i in 0 until classifArray.length()) {
            val item = classifArray.getJSONObject(i)
            val rankPos = item.optInt("ordem", i + 1)
            val teamId = item.optLong("equipe_id", i.toLong())
            val teamName = item.optString("nome_popular", item.optString("nome", "Time"))
            val badgeUrl = item.optString("escudo", "").replace("http://", "https://")
            val points = item.optInt("pontos", 0)
            val goalsDiff = item.optInt("saldo_gols", 0)
            val played = item.optInt("jogos", 0)
            val wins = item.optInt("vitorias", 0)
            val draws = item.optInt("empates", 0)
            val losses = item.optInt("derrotas", 0)

            // Formatação dos últimos jogos (ex: "V-E-D-V-V")
            val ultimosJogosArray = item.optJSONArray("ultimos_jogos")
            val formStr = if (ultimosJogosArray != null && ultimosJogosArray.length() > 0) {
                (0 until ultimosJogosArray.length()).map { idx ->
                    ultimosJogosArray.optString(idx).uppercase()
                }.joinToString("")
            } else null

            // Classificação por zona
            val zoneDesc = when {
                rankPos <= 4 -> "Fase de Grupos da Copa Libertadores"
                rankPos in 5..6 -> "Qualificação da Copa Libertadores"
                rankPos in 7..12 -> "Fase de Grupos da Copa Sul-Americana"
                rankPos >= 17 -> "Rebaixamento para a Série B"
                else -> null
            }

            ranksList.add(
                StandingTeamRank(
                    rank = rankPos,
                    team = TeamInfo(
                        id = teamId,
                        name = teamName,
                        logo = badgeUrl.ifEmpty { null }
                    ),
                    points = points,
                    goalsDiff = goalsDiff,
                    group = "Brasileirão Série A",
                    form = formStr,
                    status = "$played J ($wins V, $draws E, $losses D)",
                    description = zoneDesc
                )
            )
        }

        return StandingsData(
            league = LeagueStandingsInfo(
                id = 4351L,
                name = "Brasileirão Série A",
                country = "Brasil",
                logo = "https://s.sde.globo.com/media/organizations/2019/07/06/Palmeiras.svg",
                season = editionYear,
                standings = listOf(ranksList)
            )
        )
    }
}
