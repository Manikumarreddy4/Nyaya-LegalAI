package com.example.nyayalegalai.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.nyayalegalai.database.ChatSession
import com.example.nyayalegalai.ui.components.EmptyState
import com.example.nyayalegalai.ui.navigation.navigateToChat
import com.example.nyayalegalai.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatHistoryScreen(navController: NavController, viewModel: HistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val sessionManager = remember { com.example.nyayalegalai.utils.SessionManager(context) }
    val user = remember { sessionManager.getUser() }
    LaunchedEffect(user) {
        if (user != null && user.uid.isNotEmpty()) {
            viewModel.refreshHistory(user.uid)
        }
    }
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val selectedSessions = remember { mutableStateListOf<Long>() }
    var isInSelectionMode by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("AI Problem Assistant", "Legal Learning")
    val types = listOf("AI_ASSISTANT", "LEGAL_LEARNING")

    val rawSessions by (if (searchQuery.isEmpty()) {
        viewModel.getSessionsByType(types[selectedTabIndex])
    } else {
        viewModel.allSessions
    }).collectAsState(initial = emptyList())

    val sessions = remember(rawSessions, selectedTabIndex) {
        rawSessions.filter { it.chatbotType == types[selectedTabIndex] }
    }

    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                val allSelected = sessions.isNotEmpty() && selectedSessions.size == sessions.size
                TopAppBar(
                    title = { Text("${selectedSessions.size} selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectedSessions.clear()
                            isInSelectionMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            if (allSelected) {
                                selectedSessions.clear()
                            } else {
                                selectedSessions.clear()
                                selectedSessions.addAll(sessions.map { it.sessionId })
                            }
                        }) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = {
                            if (selectedSessions.isNotEmpty()) {
                                showDeleteSelectedDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Chat History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Delete All")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Rounded Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search history...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Dynamic tab selector
            if (searchQuery.isEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { 
                                selectedTabIndex = index
                                selectedSessions.clear()
                                isInSelectionMode = false
                            },
                            text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No History Found",
                        description = "There are no previous conversations recorded in this category yet."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = sessions, key = { it.sessionId }) { session ->
                        val isSelected = selectedSessions.contains(session.sessionId)
                        HistoryItem(
                            session = session,
                            viewModel = viewModel,
                            isSelected = isSelected,
                            isInSelectionMode = isInSelectionMode,
                            onChatClick = {
                                if (isInSelectionMode) {
                                    if (isSelected) {
                                        selectedSessions.remove(session.sessionId)
                                        if (selectedSessions.isEmpty()) {
                                            isInSelectionMode = false
                                        }
                                    } else {
                                        selectedSessions.add(session.sessionId)
                                    }
                                } else {
                                    navigateToChat(navController, session)
                                }
                            },
                            onLongClick = {
                                if (!isInSelectionMode) {
                                    isInSelectionMode = true
                                    selectedSessions.clear()
                                    selectedSessions.add(session.sessionId)
                                } else {
                                    if (isSelected) {
                                        selectedSessions.remove(session.sessionId)
                                        if (selectedSessions.isEmpty()) {
                                            isInSelectionMode = false
                                        }
                                    } else {
                                        selectedSessions.add(session.sessionId)
                                    }
                                }
                            },
                            onPinClick = { viewModel.togglePin(session.sessionId, !session.isPinned) },
                            onDeleteClick = { sessionToDelete = session },
                            onRenameClick = { sessionToRename = session },
                            onShareClick = { shareChat(context, session, viewModel) }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Clear ${tabs[selectedTabIndex]} History?") },
                text = { Text("This will permanently delete all history in ${tabs[selectedTabIndex]}. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSessions(sessions.map { it.sessionId })
                        showDeleteAllDialog = false
                    }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text("Delete Chat?") },
                text = { Text("Are you sure you want to delete '${session.title}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSession(session.sessionId)
                        sessionToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteSelectedDialog) {
            val count = selectedSessions.size
            val titleText = if (count == 1) "Delete this conversation?" else "Delete $count selected conversations?"
            val messageText = if (count == 1) {
                "This conversation will be deleted permanently. This action cannot be undone."
            } else {
                "These conversations will be deleted permanently. This action cannot be undone."
            }
            
            AlertDialog(
                onDismissRequest = { showDeleteSelectedDialog = false },
                title = { Text(titleText) },
                text = { Text(messageText) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSessions(selectedSessions.toList())
                        selectedSessions.clear()
                        isInSelectionMode = false
                        showDeleteSelectedDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSelectedDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        sessionToRename?.let { session ->
            var newTitle by remember { mutableStateOf(session.title) }
            AlertDialog(
                onDismissRequest = { sessionToRename = null },
                title = { Text("Rename Chat") },
                text = {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("New Title") },
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.renameSession(session.sessionId, newTitle)
                        }
                        sessionToRename = null
                    }) {
                        Text("Rename", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    session: ChatSession,
    viewModel: HistoryViewModel,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onChatClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val lastMessage by viewModel.getLastMessageForSession(session.sessionId).collectAsState(initial = null)
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onChatClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (session.isPinned) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isPinned) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (session.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection Indicator / Category Icon
                if (isInSelectionMode) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = if (isSelected) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (session.chatbotType) {
                                    "AI_ASSISTANT" -> Icons.AutoMirrored.Filled.Chat
                                    "LEGAL_LEARNING" -> Icons.Default.School
                                    "CONSULTATION" -> Icons.Default.PersonSearch
                                    "ENCYCLOPEDIA" -> Icons.AutoMirrored.Filled.MenuBook
                                    else -> Icons.Default.ChatBubbleOutline
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (session.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (session.chatbotType == "AI_ASSISTANT") "AI PROBLEM ASSISTANT" else session.chatbotType.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (session.isPinned) "Unpin" else "Pin") },
                            onClick = { onPinClick(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { onRenameClick(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { onShareClick(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { onDeleteClick(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = lastMessage?.message ?: "No messages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun shareChat(context: Context, session: ChatSession, viewModel: HistoryViewModel) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Nyaya Legal AI Chat History: ${session.title}\n\nType: ${session.chatbotType}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
