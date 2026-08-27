package com.example.nyayalegalai.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.database.LearningHistory
import com.example.nyayalegalai.viewmodel.LegalLearningViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalLearningHistoryScreen(navController: NavController, viewModel: LegalLearningViewModel) {
    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }
    val history by viewModel.recentHistory.collectAsState()
    
    val selectedItems = remember { mutableStateListOf<String>() }
    var isInSelectionMode by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<LearningHistory?>(null) }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                val allSelected = history.isNotEmpty() && selectedItems.size == history.size
                TopAppBar(
                    title = { Text("${selectedItems.size} selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectedItems.clear()
                            isInSelectionMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            if (allSelected) {
                                selectedItems.clear()
                            } else {
                                selectedItems.clear()
                                selectedItems.addAll(history.map { it.id })
                            }
                        }) {
                            Text(
                                text = if (allSelected) "Deselect All" else "Select All",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = {
                            if (selectedItems.isNotEmpty()) {
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
                    title = { Text("Learning History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No learning history found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(history) { item ->
                    val isSelected = selectedItems.contains(item.id)
                    LearningHistoryCard(
                        item = item,
                        isSelected = isSelected,
                        isInSelectionMode = isInSelectionMode,
                        onClick = {
                            if (isInSelectionMode) {
                                if (isSelected) {
                                    selectedItems.remove(item.id)
                                    if (selectedItems.isEmpty()) {
                                        isInSelectionMode = false
                                    }
                                } else {
                                    selectedItems.add(item.id)
                                }
                            }
                        },
                        onLongClick = {
                            if (!isInSelectionMode) {
                                isInSelectionMode = true
                                selectedItems.clear()
                                selectedItems.add(item.id)
                            }
                        },
                        onDeleteClick = {
                            itemToDelete = item
                        }
                    )
                }
            }
        }

        // Dialogs
        if (showDeleteSelectedDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteSelectedDialog = false },
                title = { Text("Delete Selected Items?") },
                text = { Text("Are you sure you want to delete the ${selectedItems.size} selected learning history items?") },
                confirmButton = {
                    TextButton(onClick = {
                        val itemsToDelete = history.filter { it.id in selectedItems }
                        viewModel.deleteLearningHistories(itemsToDelete)
                        selectedItems.clear()
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

        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Learning History?") },
                text = { Text("Are you sure you want to delete this learning item?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteLearningHistory(item)
                        itemToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningHistoryCard(
    item: LearningHistory,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isInSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Text(
                    text = item.question, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                if (!isInSelectionMode) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(text = date, fontSize = 11.sp, color = Color.Gray)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
            
            // Display stylized answer
            val lines = item.answer.split("\n")
            lines.forEach { line ->
                when {
                    line.startsWith("---") -> {}
                    line.endsWith("?") || line.equals("Punishment:", true) || 
                    line.equals("Important Points:", true) || 
                    line.contains("Real-Life Example:") ||
                    line.contains("When is this law commonly used?") -> {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    line.trim().startsWith("-") || line.trim().startsWith("•") -> {
                        Row(modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = line.trim().removePrefix("-").removePrefix("•").trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    line.isNotBlank() -> {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
