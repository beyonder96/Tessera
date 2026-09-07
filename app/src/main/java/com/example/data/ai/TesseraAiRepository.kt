package com.example.data.ai

import com.example.data.supabase.SupabaseClientProvider
import org.json.JSONArray
import org.json.JSONObject

data class DailySummaryContext(
    val userName: String = "Kenned",
    val sleepText: String? = null,
    val sleepEfficiency: Int? = null,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val todaySteps: Long = 0L,
    val expensesToday: Double = 0.0,
    val pendingTasksCount: Int = 0,
    val pendingMedsCount: Int = 0,
    val petRoutinesPending: Int = 0
)

data class AiChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

object TesseraAiRepository {
    private const val FUNCTION_NAME = "tessera-ai"

    suspend fun getDailySummary(context: DailySummaryContext): Result<String> {
        return try {
            val contextJson = JSONObject().apply {
                put("userName", context.userName)
                context.sleepText?.let { put("sleepText", it) }
                context.sleepEfficiency?.let { put("sleepEfficiency", it) }
                put("completedHabits", context.completedHabits)
                put("totalHabits", context.totalHabits)
                put("todaySteps", context.todaySteps)
                put("expensesToday", context.expensesToday)
                put("pendingTasksCount", context.pendingTasksCount)
                put("pendingMedsCount", context.pendingMedsCount)
                put("petRoutinesPending", context.petRoutinesPending)
            }

            val payload = JSONObject().apply {
                put("mode", "summary")
                put("context", contextJson)
            }

            val result = SupabaseClientProvider.invokeFunction(FUNCTION_NAME, payload.toString())
            result.mapCatching { responseJson ->
                val json = JSONObject(responseJson)
                if (json.optBoolean("success", false)) {
                    json.getString("text")
                } else {
                    throw Exception(json.optString("error", "Erro desconhecido na resposta da IA"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(messages: List<AiChatMessage>): Result<String> {
        return try {
            val messagesArray = JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", if (msg.role == "model" || msg.role == "assistant") "model" else "user")
                        put("content", msg.content)
                    })
                }
            }

            val payload = JSONObject().apply {
                put("mode", "chat")
                put("messages", messagesArray)
            }

            val result = SupabaseClientProvider.invokeFunction(FUNCTION_NAME, payload.toString())
            result.mapCatching { responseJson ->
                val json = JSONObject(responseJson)
                if (json.optBoolean("success", false)) {
                    json.getString("text")
                } else {
                    throw Exception(json.optString("error", "Erro desconhecido na resposta da IA"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
