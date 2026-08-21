package com.example.nyayalegalai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.models.Consultation
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.ui.navigation.Route
import com.example.nyayalegalai.utils.SessionManager
import com.example.nyayalegalai.viewmodel.ConsultationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerDashboardScreen(navController: NavController, viewModel: ConsultationViewModel) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val user = sessionManager.getUser()
    val scope = rememberCoroutineScope()
    val repo = remember { FirestoreRepository() }

    // 1. Redirection Logic (Users/Clients cannot access the Lawyer Dashboard)
    LaunchedEffect(user) {
        if (user == null || user.role.uppercase() != "LAWYER") {
            navController.navigate(Route.Dashboard.route) {
                popUpTo(Route.LawyerDashboard.route) { inclusive = true }
            }
        }
    }

    var isOnline by remember { mutableStateOf(true) }
    var lawyerProfile by remember { mutableStateOf<LawyerProfile?>(null) }
    var selectedTab by remember { mutableStateOf("Pending") }

    LaunchedEffect(user?.uid) {
        if (user != null && user.uid.isNotBlank()) {
            repo.getLawyerProfileFlow(user.uid).collect { profile ->
                if (profile != null) {
                    lawyerProfile = profile
                    isOnline = profile.onlineAvailable
                }
            }
        }
    }

    val allRequests by viewModel.getLawyerRequests().collectAsState(initial = emptyList())

    // 2. Classify Consultations
    val pendingRequests = remember(allRequests) { allRequests.filter { it.status.equals("PENDING", ignoreCase = true) } }
    val acceptedRequests = remember(allRequests) { allRequests.filter { it.status.equals("ACCEPTED", ignoreCase = true) } }
    val rejectedRequests = remember(allRequests) { allRequests.filter { it.status.equals("REJECTED", ignoreCase = true) } }
    val completedRequests = remember(allRequests) { allRequests.filter { it.status.equals("COMPLETED", ignoreCase = true) } }
    
    val totalEarnings = remember(allRequests) {
        allRequests.filter { it.status.equals("ACCEPTED", ignoreCase = true) || it.status.equals("COMPLETED", ignoreCase = true) }
            .sumOf { it.fee }
    }

    // 3. Appointments Classification (Today's vs Upcoming)
    var currentDate by remember { mutableStateOf(Date()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentDate = Date()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val todayMidnight = remember(currentDate) {
        val cal = java.util.Calendar.getInstance()
        cal.time = currentDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.time
    }

    val todayAppointments = remember(allRequests, todayMidnight) {
        allRequests.filter { it.status.equals("ACCEPTED", ignoreCase = true) }
            .filter {
                val apptDate = it.parseDateOnly()
                apptDate != null && apptDate.time == todayMidnight.time
            }
            .sortedWith(
                compareBy<Consultation, Date?>(nullsLast()) { it.parseTimeOnly() }
            )
    }

    val upcomingAppointments = remember(allRequests, todayMidnight) {
        allRequests.filter { it.status.equals("ACCEPTED", ignoreCase = true) }
            .filter {
                val apptDate = it.parseDateOnly()
                apptDate != null && apptDate.after(todayMidnight)
            }
            .sortedWith(
                compareBy<Consultation, Date?>(nullsLast()) { it.parseDateOnly() }
                    .thenBy(nullsLast()) { it.parseTimeOnly() }
            )
    }

    val displayRequests = when (selectedTab) {
        "Pending" -> pendingRequests
        "Accepted" -> acceptedRequests
        "Rejected" -> rejectedRequests
        "Completed" -> completedRequests
        else -> allRequests
    }

    val isVerified = lawyerProfile?.verificationStatus.equals("VERIFIED", ignoreCase = true) || lawyerProfile?.verificationStatus.isNullOrBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nyaya Legal AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        sessionManager.logout()
                        navController.navigate(Route.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Advocate Banner
            item {
                val name = lawyerProfile?.name ?: user?.name ?: "Advocate"
                Text(
                    text = "Welcome, Advocate $name",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Verification Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVerified) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    border = BorderStroke(1.dp, if (isVerified) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFEF6C00).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.Verified else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (isVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isVerified) "✓ Verified Advocate" else "Verification Pending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isVerified) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                            )
                            Text(
                                text = if (isVerified) 
                                    "Your profile is active and accepting consultation requests from clients." 
                                else 
                                    "Your bar council enrollment credentials are under review by administration. Displaying in pending mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Advocate Profile Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Advocate Profile Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ProfileDetailRow("Name", lawyerProfile?.name ?: user?.name ?: "")
                        ProfileDetailRow("Email", lawyerProfile?.email ?: user?.email ?: "")
                        ProfileDetailRow("Phone", lawyerProfile?.phone ?: user?.phone ?: "")
                        ProfileDetailRow("Specialization", lawyerProfile?.specialization?.ifBlank { "General Practice" } ?: "General Practice")
                        ProfileDetailRow("Experience", lawyerProfile?.experience ?: "0 Years")
                        ProfileDetailRow("Bar Council No.", lawyerProfile?.barCouncilNumber?.ifBlank { lawyerProfile?.enrollmentNumber } ?: user?.barId ?: "")
                        ProfileDetailRow("Location", lawyerProfile?.displayLocation ?: "Location not specified")
                    }
                }
            }

            // Statistics Row (Pending, Accepted, Earnings)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard("Pending", "${pendingRequests.size}", Icons.Default.PendingActions, Modifier.weight(1f))
                    DashboardStatCard("Accepted", "${acceptedRequests.size}", Icons.Default.CheckCircle, Modifier.weight(1f))
                    DashboardStatCard("Earnings", "₹${totalEarnings.toInt()}", Icons.Default.Payments, Modifier.weight(1f))
                }
            }

            // Availability Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Consultation Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(if (isOnline) "Available (Accepting Client Requests)" else "Offline (Do Not Disturb)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { checked ->
                                isOnline = checked
                                scope.launch {
                                    if (user != null && user.uid.isNotBlank()) {
                                        repo.updateLawyerAvailability(user.uid, checked)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Notifications Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        val hasPending = pendingRequests.isNotEmpty()
                        if (hasPending) {
                            NotificationRow("New consultation request received from ${pendingRequests.first().displayClientName}!")
                        } else {
                            NotificationRow("No new notifications at the moment.")
                        }
                    }
                }
            }

            // Today's Appointments
            item {
                Text("Today's Appointments (${todayAppointments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            if (todayAppointments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No appointments scheduled for today.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(todayAppointments) { request ->
                    LawyerRequestCard(
                        request = request,
                        onAccept = { viewModel.updateStatus(request.consultationId, "ACCEPTED") },
                        onReject = { viewModel.updateStatus(request.consultationId, "REJECTED") },
                        onClick = { navController.navigate("booking_detail/${request.consultationId}") }
                    )
                }
            }

            // Upcoming Appointments
            item {
                Text("Upcoming Appointments (${upcomingAppointments.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            if (upcomingAppointments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No upcoming appointments.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(upcomingAppointments) { request ->
                    LawyerRequestCard(
                        request = request,
                        onAccept = { viewModel.updateStatus(request.consultationId, "ACCEPTED") },
                        onReject = { viewModel.updateStatus(request.consultationId, "REJECTED") },
                        onClick = { navController.navigate("booking_detail/${request.consultationId}") }
                    )
                }
            }

            // Booking Requests Tabs & Category Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Client Requests & Consultation History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf("Pending", "Accepted", "Rejected", "Completed", "All")) { tab ->
                            FilterChip(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                label = { Text(tab) },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }

            if (displayRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No $selectedTab consultation requests.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                items(displayRequests) { request ->
                    LawyerRequestCard(
                        request = request,
                        onAccept = { viewModel.updateStatus(request.consultationId, "ACCEPTED") },
                        onReject = { viewModel.updateStatus(request.consultationId, "REJECTED") },
                        onClick = { navController.navigate("booking_detail/${request.consultationId}") }
                    )
                }
            }

            item {
                Text("Quick Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LawyerFeatureCard("My Profile", Icons.Default.AccountBox, Modifier.weight(1f)) {
                        navController.navigate(Route.Profile.route)
                    }
                    LawyerFeatureCard("Edit Profile", Icons.Default.Edit, Modifier.weight(1f)) {
                        navController.navigate(Route.EditProfile.route)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@Composable
fun NotificationRow(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.secondary, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DashboardStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LawyerRequestCard(
    request: Consultation,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit
) {
    val (statusLabel, statusBg, statusTextColor) = when (request.status.uppercase()) {
        "ACCEPTED" -> Triple("ACCEPTED", Color(0xFFE8F5E9), Color(0xFF2E7D32)) // Soft Green
        "REJECTED" -> Triple("REJECTED", Color(0xFFFFEBEE), Color(0xFFC62828)) // Soft Red
        "COMPLETED" -> Triple("COMPLETED", Color(0xFFE3F2FD), Color(0xFF1565C0)) // Soft Blue
        else -> Triple("PENDING", Color(0xFFFFF3E0), Color(0xFFEF6C00))        // Soft Orange
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.displayClientName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusTextColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Issue: ${request.displayCaseTitle}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Time: ${request.displayDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Type: ${request.consultationType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Description: ${request.displayDescription.ifEmpty { "No details provided." }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )

            if (request.contactNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Contact: ${request.contactNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (request.status.uppercase() == "PENDING") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LawyerFeatureCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
