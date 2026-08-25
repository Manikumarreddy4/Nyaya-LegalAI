package com.example.nyayalegalai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.models.Consultation
import com.example.nyayalegalai.ui.components.EmptyState
import com.example.nyayalegalai.viewmodel.ConsultationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingStatusScreen(navController: NavController, viewModel: ConsultationViewModel) {
    val bookings by remember(viewModel) { viewModel.getMyBookingsFlow() }.collectAsState()
    var showReviewDialogFor by remember { mutableStateOf<Consultation?>(null) }

    LaunchedEffect(Unit) {
        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Screen opened")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Consultations & Status", fontWeight = FontWeight.Bold) },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (bookings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.EventNote,
                        title = "No Booking Requests Found",
                        description = "You haven't requested any legal consultation appointments yet. Browse verified lawyers to book."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bookings) { booking ->
                        val safeId = booking.consultationId ?: ""
                        val safeDate = booking.resolvedDate
                        val safeTime = booking.resolvedTime
                        val safeStatus = booking.status ?: "PENDING"
                        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Processing booking ID = $safeId")
                        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Date = $safeDate")
                        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Time = $safeTime")
                        Log.d("MY_BOOKINGS", "MY_BOOKINGS: Status = $safeStatus")
                        
                        BookingCard(
                            booking = booking,
                            onClick = { navController.navigate("booking_detail/${safeId}") },
                            onCancel = { viewModel.updateStatus(safeId, "CANCELLED") },
                            onRateExperience = { showReviewDialogFor = booking }
                        )
                    }
                }
            }
    if (showReviewDialogFor != null) {
        val consultation = showReviewDialogFor!!
        var rating by remember { mutableStateOf(5) }
        var comment by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showReviewDialogFor = null },
            title = { Text("Rate Your Consultation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How was your consultation with ${consultation.lawyerName}?")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (star <= rating) Color(0xFFFFB300) else Color.LightGray,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write a comment about your experience (optional)") },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitReview(
                            consultation = consultation,
                            rating = rating.toDouble(),
                            comment = comment,
                            onSuccess = {
                                isSubmitting = false
                                showReviewDialogFor = null
                            },
                            onError = { err ->
                                isSubmitting = false
                                errorMessage = err.message ?: "Failed to submit review"
                            }
                        )
                    },
                    enabled = !isSubmitting && rating in 1..5
                ) {
                    Text(if (isSubmitting) "Submitting..." else "Submit Review")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialogFor = null },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
        }
    }
}

@Composable
fun BookingCard(booking: Consultation, onClick: () -> Unit, onCancel: () -> Unit, onRateExperience: () -> Unit) {
    val safeStatus = booking.status ?: "PENDING"
    val (statusLabel, statusBg, statusTextColor) = when (safeStatus.uppercase()) {
        "ACCEPTED" -> Triple("ACCEPTED", Color(0xFFE8F5E9), Color(0xFF2E7D32)) // Soft Green
        "REJECTED" -> Triple("REJECTED", Color(0xFFFFEBEE), Color(0xFFC62828)) // Soft Red
        "EXPIRED" -> Triple("EXPIRED", Color(0xFFF5F5F5), Color(0xFF616161)) // Soft Gray
        "COMPLETED" -> Triple("COMPLETED", Color(0xFFE3F2FD), Color(0xFF1565C0)) // Soft Blue
        "CANCELLED" -> Triple("CANCELLED", Color(0xFFF5F5F5), Color(0xFF616161)) // Soft Gray
        else -> Triple("PENDING", Color(0xFFFFF3E0), Color(0xFFEF6C00))        // Soft Orange
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Case Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.displayCaseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (statusLabel == "PENDING") "Booking Request Sent" else statusLabel,
                        color = statusTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if ((booking.lawyerName ?: "").isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lawyer: ${booking.lawyerName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (safeStatus.uppercase() == "ACCEPTED" && booking.bookingId.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Booking ID: ${booking.bookingId}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "📅 Date: ${booking.resolvedDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🕒 Time: ${booking.resolvedTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = booking.consultationType ?: "Online",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "Case Description:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = booking.displayDescription.ifEmpty { "No additional case details provided." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )

            if (safeStatus.uppercase() == "EXPIRED") {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your consultation request expired because the lawyer did not respond before the scheduled appointment time.",
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (booking.fee > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Fee: ₹${booking.fee.toInt()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (safeStatus.uppercase() == "COMPLETED") {
                Spacer(modifier = Modifier.height(12.dp))
                if (booking.hasReviewed) {
                    Text(
                        text = "✓ Review Submitted",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Button(
                        onClick = onRateExperience,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⭐ Rate Your Experience", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (safeStatus.uppercase() == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Request", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
