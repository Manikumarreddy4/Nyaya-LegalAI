package com.example.nyayalegalai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.R
import com.example.nyayalegalai.models.FirestoreChatMessage
import com.example.nyayalegalai.ui.components.MarkdownText
import com.example.nyayalegalai.utils.SessionManager
import com.example.nyayalegalai.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatbotScreen(
    navController: NavController,
    viewModel: ChatViewModel,
    sessionId: String? = null
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val sessionManager = remember { SessionManager(context) }
    val user = remember { sessionManager.getUser() }
    val userInitials = remember(user) {
        val name = user?.name ?: "User"
        name.take(1).uppercase()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val backgroundColor = MaterialTheme.colorScheme.background

    // TextToSpeech State
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var currentlySpeakingMessageId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // In-memory feedback states for thumbs up/down
    val likedMessages = remember { mutableStateMapOf<String, Boolean>() }
    val dislikedMessages = remember { mutableStateMapOf<String, Boolean>() }

    // Speech Recognition Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    viewModel.sendMessage(spokenText)
                }
            }
        }
    )

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your legal situation...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition not supported on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    // Local input text state
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(sessionId) {
        if (sessionId != null && sessionId != "-1" && sessionId != "null") {
            viewModel.setSession(sessionId)
        } else {
            viewModel.startNewChat()
        }
    }

    // Auto-scroll to bottom on messages change or typing state change
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1 + (if (isSending) 1 else 0))
        }
    }

    // Error Dialogs
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("AI Assistant Error") },
            text = { Text(errorMessage ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentId by viewModel.currentSessionId.collectAsState()
                    if (currentId != null) {
                        IconButton(onClick = {
                            viewModel.deleteChat(currentId!!)
                            Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            // Main Chat Feed
            if (messages.isEmpty() && !isSending) {
                // ChatGPT Premium Welcome Screen for AI assistant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pulsing / Glowing Logo
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 0.35f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp * scale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = glowAlpha),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Surface(
                                modifier = Modifier.size(76.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(2.dp, primaryColor.copy(alpha = 0.3f)),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Nyaya AI Assistant Logo",
                                        tint = primaryColor,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Describe your legal situation below",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                brush = Brush.linearGradient(colors = listOf(primaryColor, tertiaryColor))
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Get direct explanations, rights, remedies, and actionable steps.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
                        )

                        // Suggested Prompt Cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val suggestions = listOf(
                                AssistantSuggestionData(Icons.Default.WorkOutline, "Unpaid salary for 3 months", "Learn your rights and remedies regarding labor disputes."),
                                AssistantSuggestionData(Icons.Default.Home, "Landlord won't return deposit", "Understand rental agreements and tenant protection laws."),
                                AssistantSuggestionData(Icons.Default.Lock, "Online financial fraud case", "How to report cybercrime and seek compensation.")
                            )

                            suggestions.forEach { suggestion ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputText = suggestion.query
                                            viewModel.sendMessage(suggestion.query)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            color = primaryColor.copy(alpha = 0.1f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = suggestion.icon,
                                                    contentDescription = null,
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = suggestion.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = suggestion.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(messages, key = { _, it -> it.messageId.ifEmpty { it.timestamp.toString() } }) { index, message ->
                        val isLatestBotMessage = index == messages.size - 1 && message.sender == "Bot"
                        ChatBubblePremium(
                            message = message,
                            userInitials = userInitials,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            isLatestBotMessage = isLatestBotMessage,
                            tts = tts,
                            currentlySpeakingMessageId = currentlySpeakingMessageId,
                            onTtsToggle = { isSpeaking ->
                                currentlySpeakingMessageId = if (isSpeaking) message.messageId else null
                            },
                            isLiked = likedMessages[message.messageId] ?: false,
                            isDisliked = dislikedMessages[message.messageId] ?: false,
                            onLike = {
                                likedMessages[message.messageId] = !likedMessages.getOrDefault(message.messageId, false)
                                dislikedMessages[message.messageId] = false
                            },
                            onDislike = {
                                dislikedMessages[message.messageId] = !dislikedMessages.getOrDefault(message.messageId, false)
                                likedMessages[message.messageId] = false
                            }
                        )
                    }

                    if (isSending) {
                        item {
                            PremiumTypingIndicator(primaryColor = primaryColor)
                        }
                    }
                }
            }

            // Bottom Input Pill Capsule
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Capsule Box
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(1.dp, RoundedCornerShape(26.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = { startVoiceInput() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Speech to Text",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.ask_question_hint),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 5
                                )
                            }

                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputText = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear input",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    val hasText = inputText.isNotBlank()
                    val sendButtonColor = if (hasText && !isSending) {
                        primaryColor
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val sendIconColor = if (hasText && !isSending) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(sendButtonColor)
                            .clickable(enabled = hasText && !isSending) {
                                val text = inputText
                                inputText = ""
                                viewModel.sendMessage(text)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = sendIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubblePremium(
    message: FirestoreChatMessage,
    userInitials: String,
    primaryColor: Color,
    secondaryColor: Color,
    isLatestBotMessage: Boolean,
    tts: TextToSpeech?,
    currentlySpeakingMessageId: String?,
    onTtsToggle: (Boolean) -> Unit,
    isLiked: Boolean,
    isDisliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    val isUser = message.sender == "User"
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val time = remember(message.timestamp) {
        try {
            val date = message.timestamp.toDate()
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }

    if (isUser) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp),
                    color = primaryColor,
                    border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        if (time.isNotEmpty()) {
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userInitials,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = primaryColor
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "AI Legal Assistant",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (time.isNotEmpty()) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (isLatestBotMessage) {
                            TypewriterText(
                                text = message.message,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            MarkdownText(
                                text = message.message,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    val isSpeaking = currentlySpeakingMessageId == message.messageId
                    IconButton(
                        onClick = {
                            if (tts != null) {
                                if (isSpeaking) {
                                    tts.stop()
                                    onTtsToggle(false)
                                } else {
                                    tts.stop()
                                    tts.speak(message.message, TextToSpeech.QUEUE_FLUSH, null, null)
                                    onTtsToggle(true)
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Mute" else "Read Aloud",
                            tint = if (isSpeaking) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.message))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, message.message)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share advice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Like",
                            tint = if (isLiked) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDislike,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = "Dislike",
                            tint = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    var textLengthToShow by remember { mutableStateOf(0) }
    LaunchedEffect(text) {
        textLengthToShow = 0
        val targetLength = text.length
        val duration = (targetLength * 6).coerceIn(200, 1200)
        val delayTime = (duration / targetLength.coerceAtLeast(1)).toLong().coerceIn(1, 12)
        for (i in 1..targetLength) {
            textLengthToShow = i
            kotlinx.coroutines.delay(delayTime)
        }
    }
    val visibleText = remember(text, textLengthToShow) {
        text.substring(0, textLengthToShow)
    }
    MarkdownText(text = visibleText, color = color, modifier = modifier)
}

@Composable
fun PremiumTypingIndicator(primaryColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 40.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = primaryColor.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = "AI Legal Assistant",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    val transition = rememberInfiniteTransition(label = "typing")
                    val alpha1 by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.2f at 0
                                1f at 150
                                0.2f at 300
                            },
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dot1"
                    )
                    val alpha2 by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.2f at 100
                                1f at 250
                                0.4f at 400
                            },
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dot2"
                    )
                    val alpha3 by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.2f at 200
                                1f at 350
                                0.6f at 500
                            },
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dot3"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(primaryColor.copy(alpha = alpha1)))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(primaryColor.copy(alpha = alpha2)))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(primaryColor.copy(alpha = alpha3)))
                    }
                }
            }
        }
    }
}

data class AssistantSuggestionData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val query: String = title
)
