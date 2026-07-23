package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TesseraChatScreen(chatViewModel: ChatViewModel = viewModel()) {
    val messages by chatViewModel.messages.collectAsState()
    val downloadState by chatViewModel.downloadState.collectAsState()
    val useLocalGemma by chatViewModel.useLocalGemma.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showGemmaConfig by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(top = 48.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
    ) {
        // Header Minimalista
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tessera AI",
                    color = Color(0xFFF8FAFC),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (useLocalGemma && downloadState.isDownloaded) Color(0x3310B981) else Color(0x3338BDF8))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (useLocalGemma) (if (downloadState.isDownloaded) "Gemma On-Device ⚡" else "Gemma Local ⚠️") else "Gemma 2 Cloud ☁️",
                        color = if (useLocalGemma && downloadState.isDownloaded) Color(0xFF34D399) else Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showGemmaConfig = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Configurar Gemma AI",
                        tint = Color(0xFF38BDF8)
                    )
                }
                if (messages.isNotEmpty()) {
                    IconButton(onClick = { chatViewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Limpar Histórico",
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // Area Central (Lista ou Welcome Screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                WelcomeScreen { prompt ->
                    inputText = prompt
                    chatViewModel.sendMessage(prompt)
                    inputText = ""
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(messages) { message ->
                        AnimatedMessageRow(
                            message = message,
                            onTick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Minimalista
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(32.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        "Pergunte algo sobre Finanças, Apê, Pets...", 
                        color = Color(0xFF64748B),
                        fontSize = 15.sp
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFFF8FAFC),
                    unfocusedTextColor = Color(0xFFF8FAFC),
                    cursorColor = Color(0xFF38BDF8)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            chatViewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                maxLines = 4
            )

            if (inputText.isNotBlank()) {
                IconButton(
                    onClick = {
                        chatViewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp).offset(x = 2.dp)
                    )
                }
            }
        }
    }

    if (showGemmaConfig) {
        GemmaModelConfigBottomSheet(
            chatViewModel = chatViewModel,
            onDismiss = { showGemmaConfig = false }
        )
    }
}

@Composable
fun WelcomeScreen(onPromptSelect: (String) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { 50 }, animationSpec = tween(500)) + fadeIn(tween(500))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val prompts = listOf<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>(
                Triple("O que eu posso gerenciar no meu apê?", "Visão geral da sua casa", Icons.Outlined.Home),
                Triple("Como posso organizar minhas economias este mês?", "Dicas de controle financeiro", Icons.Outlined.Star),
                Triple("Quais cuidados com a saúde dos pets?", "Dicas para a rotina do pet", Icons.Outlined.Build)
            )

            prompts.forEach { (title, subtitle, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onPromptSelect(title) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            color = Color(0xFFF8FAFC),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMessageRow(message: ChatMessage, onTick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(message) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { if (message.isUser) 200 else -200 },
            animationSpec = tween(400)
        ) + fadeIn(tween(400))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(vertical = 8.dp)
            ) {
                if (message.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFF8FAFC),
                        strokeWidth = 2.dp
                    )
                } else if (!message.isUser) {
                    Column {
                        val widgetType = extractWidgetType(message.text)
                        if (widgetType != null) {
                            ChatWidgetCard(type = widgetType)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        TypewriterText(
                            fullText = removeWidgetTags(message.text),
                            onTick = onTick
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        color = Color(0xFF94A3B8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TypewriterText(fullText: String, onTick: () -> Unit) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        if (displayedText.isEmpty()) {
            for (i in fullText.indices) {
                displayedText += fullText[i]
                if (i % 3 == 0) onTick()
                delay(15)
            }
            onTick()
        } else {
            displayedText = fullText
        }
    }

    Text(
        text = parseMarkdown(displayedText),
        color = Color(0xFFF8FAFC),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp
    )
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val boldIndex = text.indexOf("**", currentIndex)
            val codeIndex = text.indexOf("`", currentIndex)
            val nextMark = listOf(boldIndex, codeIndex).filter { it != -1 }.minOrNull()

            if (nextMark == null) {
                append(text.substring(currentIndex))
                break
            }

            if (nextMark > currentIndex) {
                append(text.substring(currentIndex, nextMark))
            }

            when (nextMark) {
                boldIndex -> {
                    val endBold = text.indexOf("**", boldIndex + 2)
                    if (endBold != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)) {
                            append(text.substring(boldIndex + 2, endBold))
                        }
                        currentIndex = endBold + 2
                    } else {
                        append("**")
                        currentIndex = boldIndex + 2
                    }
                }
                codeIndex -> {
                    val endCode = text.indexOf("`", codeIndex + 1)
                    if (endCode != -1) {
                        withStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFF0F172A),
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp
                            )
                        ) {
                            append(" " + text.substring(codeIndex + 1, endCode) + " ")
                        }
                        currentIndex = endCode + 1
                    } else {
                        append("`")
                        currentIndex = codeIndex + 1
                    }
                }
            }
        }
    }
}

fun extractWidgetType(text: String): String? {
    val regex = "\\[WIDGET:([A-Z]+)\\]".toRegex()
    return regex.find(text)?.groupValues?.get(1)
}

fun removeWidgetTags(text: String): String {
    val regex = "\\[WIDGET:[A-Z]+\\]".toRegex()
    return text.replace(regex, "").trim()
}

@Composable
fun ChatWidgetCard(type: String) {
    when (type) {
        "FINANCE" -> FinanceChatWidget()
        "PETS" -> PetsChatWidget()
        "APARTMENT" -> ApartmentChatWidget()
    }
}

@Composable
fun FinanceChatWidget() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Star, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Resumo Financeiro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Gasto Mensal: R$ 2.450,00", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PetsChatWidget() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Home, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Carteira Pet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Próxima Vacina: Raiva em 10 dias", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ApartmentChatWidget() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Build, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Progresso da Obra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pintura: 80% Concluído", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemmaModelConfigBottomSheet(
    chatViewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val downloadState by chatViewModel.downloadState.collectAsState()
    val useLocalGemma by chatViewModel.useLocalGemma.collectAsState()
    val modelUrl by chatViewModel.gemmaModelUrl.collectAsState()
    val systemPromptState by chatViewModel.systemPrompt.collectAsState()
    val tempState by chatViewModel.temperature.collectAsState()
    val topKState by chatViewModel.topK.collectAsState()
    val maxTokensState by chatViewModel.maxTokens.collectAsState()

    var customUrl by remember { mutableStateOf(modelUrl) }
    var promptInput by remember { mutableStateOf(systemPromptState) }
    var tempVal by remember { mutableFloatStateOf(tempState) }
    var topKVal by remember { mutableFloatStateOf(topKState.toFloat()) }
    var maxTokensVal by remember { mutableFloatStateOf(maxTokensState.toFloat()) }

    val presets = listOf(
        "Gemma 2B GPU" to "https://huggingface.co/google/gemma-2b-it-gpu-int4/resolve/main/gemma-2b-it-gpu-int4.bin",
        "Gemma 2B CPU" to "https://huggingface.co/google/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin",
        "Gemma 2 2B" to "https://huggingface.co/google/gemma-2-2b-it-gpu-int4/resolve/main/gemma-2-2b-it-gpu-int4.bin"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Configurações do Gemma AI", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

            // Switch Modo Local vs Cloud
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Executar Gemma On-Device (Local)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Roda o modelo Gemma direto na GPU/processador do celular", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Switch(
                    checked = useLocalGemma,
                    onCheckedChange = { chatViewModel.setUseLocalGemma(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF38BDF8)
                    )
                )
            }

            // Card de Download do Modelo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DOWNLOAD DO MODELO GEMMA", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Text("Selecione a versão do modelo:", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { (name, url) ->
                            val isSelected = customUrl == url
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0F172A))
                                    .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(10.dp))
                                    .clickable {
                                        customUrl = url
                                        chatViewModel.updateModelUrl(url)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name, color = if (isSelected) Color.White else Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = {
                            customUrl = it
                            chatViewModel.updateModelUrl(it)
                        },
                        label = { Text("URL Personalizada de Download (.bin)", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Status e Progresso de Download
                    if (downloadState.isDownloading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF0F172A)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Baixando: ${downloadState.progressPercent}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    String.format(Locale("pt", "BR"), "%.1f MB / %.1f MB", downloadState.bytesDownloadedMB, downloadState.totalBytesMB),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (downloadState.isDownloaded) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("✅ Modelo Gemma baixado (${String.format(Locale("pt", "BR"), "%.1f MB", downloadState.bytesDownloadedMB)})", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { chatViewModel.deleteGemmaModel() }) {
                                Text("Excluir", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { chatViewModel.downloadGemmaModel() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Baixar Modelo Gemma", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }

                    downloadState.errorMessage?.let { err ->
                        Text(err, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            // Seção de Parâmetros do Sistema
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("PARÂMETROS DO SISTEMA", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Instrução do Sistema (Prompt)", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )

                    // Sliders
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Temperatura: ${String.format(Locale("pt", "BR"), "%.2f", tempVal)}", color = Color.White, fontSize = 12.sp)
                            Text("Criatividade da resposta", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Slider(
                            value = tempVal,
                            onValueChange = { tempVal = it },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                        )
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Top-K: ${topKVal.toInt()}", color = Color.White, fontSize = 12.sp)
                            Text("Diversidade de vocabulário", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Slider(
                            value = topKVal,
                            onValueChange = { topKVal = it },
                            valueRange = 1.0f..100.0f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                        )
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max Tokens: ${maxTokensVal.toInt()}", color = Color.White, fontSize = 12.sp)
                            Text("Tamanho máx. da resposta", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                        Slider(
                            value = maxTokensVal,
                            onValueChange = { maxTokensVal = it },
                            valueRange = 128.0f..2048.0f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                        )
                    }

                    Button(
                        onClick = {
                            chatViewModel.updateSystemParameters(promptInput, tempVal, topKVal.toInt(), maxTokensVal.toInt())
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("Salvar Parâmetros", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
