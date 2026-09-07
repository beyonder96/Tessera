package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.AiChatMessage
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

private val AccentTeal = Color(0xFF71D7CD)
private val SurfaceCard = Color(0x12FFFFFF)
private val BorderSubtle = Color(0x1FFFFFFF)
private val TextSecondary = Color(0x99FFFFFF)

@Composable
fun TesseraChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ChatTopBar(
            onBack = onBack,
            onClear = { viewModel.clearChat() },
            canClear = state.messages.isNotEmpty()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.messages.isEmpty() && !state.isLoading) {
                ChatEmptyState(
                    onSuggestionClick = { suggestion ->
                        viewModel.sendMessage(suggestion)
                    }
                )
            } else {
                ChatMessageList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    listState = listState
                )
            }
        }

        if (state.error != null) {
            ChatErrorBanner(
                message = state.error ?: "",
                onRetry = { viewModel.retryLastMessage() }
            )
        }

        ChatInputField(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                val toSend = inputText
                inputText = ""
                viewModel.sendMessage(toSend)
                coroutineScope.launch {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size)
                    }
                }
            },
            isEnabled = !state.isLoading
        )
    }
}

@Composable
private fun ChatTopBar(
    onBack: () -> Unit,
    onClear: () -> Unit,
    canClear: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Tessera AI",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Gemini 2.5 Flash",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = AccentTeal
                )
            }
        }

        if (canClear) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Limpar conversa",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    onSuggestionClick: (String) -> Unit
) {
    val suggestions = listOf(
        "Como equilibrar minha rotina de hábitos e sono hoje?",
        "Qual o balanço geral das minhas despesas do mês?",
        "Dê uma sugestão de foco para o meu dia a dia."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Como posso ajudar você hoje?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Conectada ao seu ecossistema pessoal no Tessera.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageList(
    messages: List<AiChatMessage>,
    isLoading: Boolean,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            ChatMessageBubble(message = message)
        }
        if (isLoading) {
            item {
                ChatTypingIndicator()
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiChatMessage) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) AccentTeal.copy(alpha = 0.15f) else SurfaceCard)
                .border(
                    width = 1.dp,
                    color = if (isUser) AccentTeal.copy(alpha = 0.35f) else BorderSubtle,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = if (isUser) Color.White else Color(0xFFE5E7EB)
            )
        }
    }
}

@Composable
private fun ChatTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotsAlpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = (alpha * 0.7f).coerceIn(0.2f, 1f)))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = (alpha * 0.4f).coerceIn(0.2f, 1f)))
        )
    }
}

@Composable
private fun ChatErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x26FF453A))
            .border(1.dp, Color(0x33FF453A), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = Color(0xFFFF453A),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onRetry,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Tentar novamente",
                tint = Color(0xFFFF453A),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ChatInputField(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isEnabled: Boolean
) {
    val canSend = isEnabled && text.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Escreva sua mensagem...",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White
                ),
                cursorBrush = SolidColor(AccentTeal),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (canSend) AccentTeal else SurfaceCard)
                .clickable(enabled = canSend) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Enviar",
                tint = if (canSend) Color(0xFF0F1115) else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
