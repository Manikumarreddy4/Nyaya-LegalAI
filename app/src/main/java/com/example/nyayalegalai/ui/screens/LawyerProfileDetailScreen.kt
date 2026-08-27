package com.example.nyayalegalai.ui.screens

import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.viewmodel.ConsultationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerProfileDetailScreen(
    navController: NavController,
    viewModel: ConsultationViewModel,
    lawyerId: String
) {
    var lawyer by remember { mutableStateOf<LawyerProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(lawyerId) {
        val firestoreRepo = FirestoreRepository()
        val fetched = firestoreRepo.getLawyerProfile(lawyerId)
        if (fetched != null) {
            lawyer = fetched
        } else {
            // Check fallback from viewModel lawyers list
            lawyer = viewModel.allLawyers.value.find { it.lawyerId == lawyerId || it.userId == lawyerId }
        }
        isLoading = false
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lawyer Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (lawyer != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Consultation Fee", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "₹${lawyer!!.consultationFee.toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = primaryColor
                            )
                        }

                        Button(
                            onClick = { navController.navigate("booking_form/${lawyer!!.lawyerId.ifBlank { lawyer!!.userId }}") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .padding(start = 16.dp),
                            enabled = lawyer!!.isOnlineAvailable || lawyer!!.isInPersonOnlineAvailable
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lawyer!!.isOnlineAvailable || lawyer!!.isInPersonOnlineAvailable) "Book Consultation" else "Unavailable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (lawyer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Lawyer profile not found.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            val prof = lawyer!!
            val isVerified = prof.verificationStatus.equals("VERIFIED", ignoreCase = true) || prof.verificationStatus.isBlank()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape),
                            color = primaryColor.copy(alpha = 0.1f)
                        ) {
                            if (prof.displayPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = prof.displayPhoto,
                                    contentDescription = prof.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(48.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = prof.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isVerified) {
                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Verified", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = prof.specialization.ifEmpty { "General Practice" },
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Row (Rating, Experience, Consultations)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStatItem("Rating", "★ ${prof.rating}")
                            HorizontalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileStatItem("Experience", prof.experience.ifEmpty { "0 Yrs" })
                            HorizontalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileStatItem("Consultations", "${prof.consultationCount}+")
                        }
                    }
                }

                // About Section
                if (prof.bio.isNotBlank()) {
                    DetailSectionCard("About the Lawyer", Icons.Default.Info) {
                        Text(
                            text = prof.bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Credentials & Bar Council Section
                DetailSectionCard("Bar Council & Credentials", Icons.Default.Badge) {
                    DetailRow(label = "Enrollment Number", value = prof.displayBarNumber.ifEmpty { "Verified Advocate" })
                    DetailRow(label = "State Bar Council", value = prof.displayBarCouncil.ifEmpty { "State Bar Association" })
                    DetailRow(label = "Qualification", value = prof.qualification.ifEmpty { "LL.B Legal Practitioner" })
                    if (prof.university.isNotBlank()) {
                        DetailRow(label = "University", value = prof.university)
                    }
                }

                // Location & Contact Section
                DetailSectionCard("Office Location & Languages", Icons.Default.LocationOn) {
                    DetailRow(label = "Current City", value = prof.displayLocation)
                    if (prof.officeAddress.isNotBlank()) {
                        DetailRow(label = "Office Address", value = prof.officeAddress)
                    }
                    DetailRow(label = "Languages Known", value = prof.languages.ifEmpty { "English, Hindi" })
                }

                // Availability & Consultation Options
                DetailSectionCard("Consultation & Availability", Icons.Default.AccessTime) {
                    DetailRow(label = "Available Days", value = prof.availableDays)
                    DetailRow(label = "Available Time", value = prof.availableTime)
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Online Consultation Card
                        val isOnlineAvailable = prof.isOnlineAvailable
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isOnlineAvailable) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOnlineAvailable) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoCall,
                                        contentDescription = null,
                                        tint = if (isOnlineAvailable) Color(0xFF2E7D32) else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Online Consultation",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isOnlineAvailable) Color(0xFF2E7D32) else Color.Gray
                                        )
                                        Text(
                                            text = if (isOnlineAvailable) "Available for video call" else "Currently Unavailable",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isOnlineAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isOnlineAvailable) Color(0xFF2E7D32) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isOnlineAvailable) "ON" else "OFF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isOnlineAvailable) Color(0xFF2E7D32) else Color.Gray
                                    )
                                }
                            }
                        }

                        // In-Person Consultation Card
                        val isInPersonAvailable = prof.isInPersonOnlineAvailable
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isInPersonAvailable) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isInPersonAvailable) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = if (isInPersonAvailable) Color(0xFF2E7D32) else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "In-Person Consultation",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isInPersonAvailable) Color(0xFF2E7D32) else Color.Gray
                                        )
                                        Text(
                                            text = if (isInPersonAvailable) "Available for Offline Meetings" else "Not Available for Offline Meetings",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isInPersonAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isInPersonAvailable) Color(0xFF2E7D32) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isInPersonAvailable) "ON" else "OFF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isInPersonAvailable) Color(0xFF2E7D32) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Customer Reviews Section
                val reviews by remember(viewModel, prof.lawyerId.ifBlank { prof.userId }) {
                    viewModel.getLawyerReviewsFlow(prof.lawyerId.ifBlank { prof.userId })
                }.collectAsState(initial = emptyList())

                DetailSectionCard("Client Reviews", Icons.Default.Star) {
                    if (reviews.isEmpty()) {
                        Text(
                            text = "No client reviews yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            reviews.forEach { r ->
                                SampleReviewCard(
                                    clientName = r.userName.ifBlank { r.clientName.ifBlank { "Client" } },
                                    rating = r.rating,
                                    comment = r.comment
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DetailSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SampleReviewCard(clientName: String, rating: Double, comment: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(clientName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("$rating", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
