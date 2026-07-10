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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Build
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TesseraChatScreen(chatViewModel: ChatViewModel = viewModel()) {
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
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
            Text(
                text = "AI Assistant",
                color = Color(0xFFF8FAFC),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
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
                                // Auto-scroll durante o typewriter
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
                        "Pergunte algo...", 
                        color = Color(0xFF64748B),
                        fontSize = 16.sp
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
                Triple("Sugira um cardápio para esta semana", "Receitas fáceis e rápidas", Icons.Outlined.Star),
                Triple("Quais os próximos passos da obra?", "Acompanhe seu progresso", Icons.Outlined.Build)
            )

            prompts.forEach { (title, subtitle, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B)) // Cor de fundo do card, semelhante ao print
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
                    TypewriterText(
                        fullText = message.text,
                        onTick = onTick
                    )
                } else {
                    Text(
                        text = message.text,
                        color = Color(0xFF94A3B8), // Cinza discreto estilo iOS
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
                if (i % 3 == 0) onTick() // Otimiza a quantidade de chamadas de scroll
                delay(15) // Velocidade da máquina de escrever
            }
            onTick() // Scroll final
        } else {
            displayedText = fullText
        }
    }

    Text(
        text = parseMarkdown(displayedText),
        color = Color(0xFFF8FAFC),
        fontSize = 20.sp, // Aumentado para estilo Apple
        fontWeight = FontWeight.Bold, // Bold para dar destaque
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    )
}

/**
 * Parser minimalista nativo de Markdown para AnnotatedString.
 * Suporta: **Negrito**, `Código` e * Listas.
 */
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val boldIndex = text.indexOf("**", currentIndex)
            val codeIndex = text.indexOf("`", currentIndex)
            
            // Determina qual marcador vem primeiro
            val nextMark = listOf(boldIndex, codeIndex).filter { it != -1 }.minOrNull()

            if (nextMark == null) {
                // Nenhum marcador encontrado, adiciona o restante e sai
                append(text.substring(currentIndex))
                break
            }

            // Adiciona o texto antes do marcador
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
