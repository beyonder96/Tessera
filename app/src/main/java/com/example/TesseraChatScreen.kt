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
            .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // Header Minimalista
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFE2E8F0),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tessera AI",
                color = Color(0xFFF8FAFC),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Como posso ajudar?",
                color = Color(0xFFF8FAFC),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            val prompts = listOf(
                "O que eu posso gerenciar no meu apê?",
                "Sugira um cardápio para esta semana.",
                "Quais os próximos passos da obra?"
            )

            prompts.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .clickable { onPromptSelect(prompt) }
                        .padding(16.dp)
                ) {
                    Text(
                        text = prompt,
                        color = Color(0xFFCBD5E1),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
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
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = if (message.isUser) 24.dp else 4.dp,
                            bottomEnd = if (message.isUser) 4.dp else 24.dp
                        )
                    )
                    .background(
                        if (message.isUser) Color(0xFF38BDF8)
                        else Color(0xFF1E293B)
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (message.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF38BDF8),
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
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
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
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 26.sp, // Espaçamento luxuoso Apple-style
        letterSpacing = 0.3.sp
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
