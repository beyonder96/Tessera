@file:DependsOn("com.squareup.okhttp3:okhttp:4.11.0")

import okhttp3.OkHttpClient
import okhttp3.Request

val token = "s9M1yUatdExguK89eVzkubPv15aZO0hsQoRXjzh01b7g2nUGFPy5qxjmXOqo"
val client = OkHttpClient()

val request1 = Request.Builder()
    .url("https://api.sportmonks.com/v3/football/fixtures?api_token=$token&include=participants;league;state;scores&per_page=5")
    .build()

client.newCall(request1).execute().use { response ->
    println("Latest fixtures response code: ${response.code}")
    println("Body: ${response.body?.string()?.take(500)}")
}
