package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TesseraMessage
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Cores Empáticas (Dark Teal Premium) ---
val ChatBackground = Color(0xFF071013)
val GlassSurface = Color(0x33FFFFFF)
val GlassBorder = Color(0x1AFFFFFF)
val AccentTeal = Color(0xFF00E5FF)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3FFFFFF) // 70% opacity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAIChatSheet(
    onDismiss: () -> Unit,
    viewModel: TesseraViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Core chat state
    val chatHistory = remember {
        androidx.compose.runtime.mutableStateListOf(
            TesseraMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = "Olá! Sou a Tessera AI. Como posso te ajudar a ter um dia mais leve e produtivo hoje?",
                isUser = false
            )
        )
    }
    var prompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll para baixo quando novas mensagens chegarem ou durante o thinking
    LaunchedEffect(chatHistory.size, isThinking) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ChatBackground,
        scrimColor = Color(0x99000000),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background decorativo suave
            AnimatedBackground()

            Column(modifier = Modifier.fillMaxSize()) {
                // Header + Mascot
                ChatHeader(onDismiss = onDismiss, isThinking = isThinking)
                
                // Lista de mensagens estilo Perplexity
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp), // Espaço para input bar flutuante
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    items(chatHistory.size) { index ->
                        val msg = chatHistory[index]
                        ChatMessageItem(msg = msg)
                    }
                    
                    if (isThinking) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                }
            }

            // Glassmorphic Input Bar fixo na base
            GlassInputBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
                prompt = prompt,
                onPromptChange = { prompt = it },
                onSend = {
                    if (prompt.isNotBlank() && !isThinking) {
                        val userText = prompt
                        prompt = ""
                        val msgId = java.util.UUID.randomUUID().toString()
                        chatHistory.add(TesseraMessage(id = msgId, text = userText, isUser = true))
                        isThinking = true
                        
                        coroutineScope.launch {
                            // Chama a resposta do ViewModel (que já usa mutex internamente)
                            val response = viewModel.generateAIResponse(userText)
                            isThinking = false
                            
                            val responseId = java.util.UUID.randomUUID().toString()
                            // Adicionamos a resposta com animação
                            chatHistory.add(TesseraMessage(id = responseId, text = response, isUser = false))
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F2C3D).copy(alpha = alphaAnim),
                        Color.Transparent
                    ),
                    center = Offset(500f, 0f),
                    radius = 1500f
                )
            )
    )
}

@Composable
fun ChatHeader(onDismiss: () -> Unit, isThinking: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GlassSurface)
                .border(1.dp, GlassBorder, CircleShape)
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary)
        }

        // Mascote Animado ao Centro
        Mascot(isThinking = isThinking)

        Spacer(modifier = Modifier.size(40.dp)) // Para balancear
    }
}

@Composable
fun Mascot(isThinking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")
    
    // Float animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotFloat"
    )

    // Pulse animation for thinking
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isThinking) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isThinking) 500 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0077FF))
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    ) {
        // Rosto do Mascote
        Icon(
            imageVector = Icons.Rounded.Face,
            contentDescription = "Mascot Face",
            tint = Color.White,
            modifier = Modifier
                .size(if (isThinking) 28.dp else 24.dp)
        )
    }
}

@Composable
fun ChatMessageItem(msg: TesseraMessage) {
    // Transição de entrada suave
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
        ) + fadeIn(animationSpec = tween(300))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (msg.isUser) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Você",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.Face, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "Tessera AI",
                        color = AccentTeal,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conteúdo fluído (Sem balões)
            Text(
                text = msg.text,
                color = if (msg.isUser) TextPrimary else TextSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(start = 40.dp) // Alinhado com o texto do nome
            )
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse), label = "")
    val dot2Alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "")
    val dot3Alpha by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 40.dp, top = 8.dp)
    ) {
        Text("Pesquisando", color = AccentTeal, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentTeal.copy(alpha = dot1Alpha)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentTeal.copy(alpha = dot2Alpha)))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentTeal.copy(alpha = dot3Alpha)))
        }
    }
}

@Composable
fun GlassInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChange,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            cursorBrush = SolidColor(AccentTeal),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                onSend()
                keyboardController?.hide()
            }),
            decorationBox = { innerTextField ->
                if (prompt.isEmpty()) {
                    Text(
                        text = "Pergunte qualquer coisa...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Botão de envio animado
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (prompt.isNotBlank()) AccentTeal else GlassSurface)
                .clickable(enabled = prompt.isNotBlank()) {
                    onSend()
                    keyboardController?.hide()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Enviar",
                tint = if (prompt.isNotBlank()) Color(0xFF071013) else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
