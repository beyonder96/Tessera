package com.example.ui.components
import androidx.compose.material3.MaterialTheme

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.thermalCard

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XTimelineWidget(
    profileHandle: String = "elonmusk", // Substitua pelo perfil desejado
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    // HTML customizado carregando o script nativo do X e aplicando estilos dark
    val htmlData = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                }
                /* Esconde a scrollbar para manter o design limpo */
                ::-webkit-scrollbar {
                    width: 4px;
                }
                ::-webkit-scrollbar-track {
                    background: transparent;
                }
                ::-webkit-scrollbar-thumb {
                    background: #333;
                    border-radius: 4px;
                }
            </style>
        </head>
        <body>
            <a class="twitter-timeline"
               data-theme="dark"
               data-chrome="noheader nofooter noborder transparent"
               data-tweet-limit="3"
               href="https://twitter.com/$profileHandle?ref_src=twsrc%5Etfw">
               Carregando linha do tempo...
            </a>
            <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .thermalCard(cornerRadius = 16.dp, elevation = 12.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header do Container
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ícone do X (antigo Twitter) desenhado nativamente via Canvas para manter o peso leve
                val sysOnBackground = MaterialTheme.colorScheme.onBackground
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawLine(
                        color = sysOnBackground.copy(alpha = 0.7f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = sysOnBackground.copy(alpha = 0.7f),
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                Text(
                    text = "LINHA DO TEMPO",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp) // Altura fixa para manter no layout com scroll interno
            ) {
                // AndroidView para rodar a WebView embedada
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // Adicionando um pequeno delay para garantir que o script do Twitter processe e renderize o embed
                                    postDelayed({ isLoading = false }, 1500)
                                }
                            }
                            
                            loadDataWithBaseURL("https://twitter.com", htmlData, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Esqueleto de Carregamento Sobreposto
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLoading,
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212)) // Fundo escuro cobrindo a webview
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            repeat(3) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0x1AFFFFFF))
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.4f).background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp)))
                                        Box(modifier = Modifier.height(10.dp).fillMaxWidth(0.9f).background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp)))
                                        Box(modifier = Modifier.height(10.dp).fillMaxWidth(0.7f).background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
