import java.net.URL
import java.net.HttpURLConnection

val token = "fc7a53cdc1cce061cc38365f4791b5f7d1977e4c21001feb964b76761bb0d8cc"
val authUrl = URL("https://api.olhovivo.sptrans.com.br/v2.1/Login/Autenticar?token=$token")
val authConn = authUrl.openConnection() as HttpURLConnection
authConn.requestMethod = "POST"
authConn.setRequestProperty("Content-Length", "0")
authConn.connect()

val cookie = authConn.getHeaderField("Set-Cookie")?.split(";")?.get(0)
println("Auth Cookie: $cookie")

val lineCode = 2506
val prevUrl = URL("https://api.olhovivo.sptrans.com.br/v2.1/Previsao/Linha?codigoLinha=$lineCode")
val prevConn = prevUrl.openConnection() as HttpURLConnection
prevConn.setRequestProperty("Cookie", cookie)
val response = prevConn.inputStream.bufferedReader().use { it.readText() }
println("Previsao Response: ${response.take(200)}...")
