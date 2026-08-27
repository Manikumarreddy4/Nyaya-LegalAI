package com.example.nyayalegalai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.models.Consultation
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.repository.toSafeConsultation
import com.example.nyayalegalai.viewmodel.ConsultationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    navController: NavController,
    viewModel: ConsultationViewModel,
    consultationId: String
) {
    var consultation by remember { mutableStateOf<Consultation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(consultationId) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docSnap = db.collection("consultations").document(consultationId).get().await()
            consultation = docSnap.toSafeConsultation()
        } catch (e: Exception) {
            // Fallback: check viewmodel flow
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultation Details", fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (consultation == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Consultation details not found.")
            }
        } else {
            val booking = consultation!!
            val safeStatus = if ((booking.status ?: "PENDING").uppercase() == "PENDING" && booking.parsedAppointmentDate()?.before(java.util.Date()) == true) "EXPIRED" else booking.status ?: "PENDING"
            val (statusLabel, statusBg, statusTextColor) = when (safeStatus.uppercase()) {
                "ACCEPTED" -> Triple("ACCEPTED", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                "REJECTED" -> Triple("REJECTED", Color(0xFFFFEBEE), Color(0xFFC62828))
                "EXPIRED" -> Triple("EXPIRED", Color(0xFFF5F5F5), Color(0xFF616161))
                "COMPLETED" -> Triple("COMPLETED", Color(0xFFE3F2FD), Color(0xFF1565C0))
                "CANCELLED" -> Triple("CANCELLED", Color(0xFFF5F5F5), Color(0xFF616161))
                else -> Triple("PENDING", Color(0xFFFFF3E0), Color(0xFFEF6C00))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Section Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = statusBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    color = statusTextColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (safeStatus.uppercase() == "EXPIRED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "This consultation request automatically expired because the scheduled appointment time was reached before the lawyer responded.",
                                    color = Color(0xFFC62828),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Case Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Case Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        
                        BookingDetailRow("Client Name:", booking.clientName.ifBlank { booking.userName.ifBlank { "Client" } })
                        BookingDetailRow("Issue/Title:", booking.displayCaseTitle)
                        BookingDetailRow("Category/Type:", booking.issueType.ifBlank { "Legal Consultation" })
                        BookingDetailRow("Description:", booking.displayDescription.ifBlank { "No description provided." })
                    }
                }

                // Lawyer Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Lawyer Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        
                        BookingDetailRow("Name:", booking.lawyerName.ifBlank { "Advocate" })
                    }
                }

                // Booking Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Booking Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        
                        if (safeStatus.uppercase() == "ACCEPTED" && booking.bookingId.isNotBlank()) {
                            BookingDetailRow("Booking ID:", booking.bookingId)
                        }
                        BookingDetailRow("Type:", booking.consultationType)
                        BookingDetailRow("Date:", booking.resolvedDate)
                        BookingDetailRow("Time:", booking.resolvedTime)
                        if (safeStatus.uppercase() == "ACCEPTED") {
                            val phone = booking.userPhone.ifBlank { booking.contactNumber }
                            if (phone.isNotBlank()) {
                                BookingDetailRow("Callback Contact:", phone)
                            }
                            if (booking.userEmail.isNotBlank()) {
                                BookingDetailRow("Client Email:", booking.userEmail)
                            }
                        }
                        BookingDetailRow("Preferred Language:", booking.preferredLanguage)
                        BookingDetailRow("Consultation Fee:", "₹${booking.fee.toInt()}")
                        if (booking.notes.isNotBlank()) {
                            BookingDetailRow("Notes:", booking.notes)
                        }
                    }
                }

                // Action Buttons
                if (booking.status.uppercase() == "PENDING") {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.updateStatus(booking.consultationId, "CANCELLED")
                                navController.popBackStack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Cancel Request", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
