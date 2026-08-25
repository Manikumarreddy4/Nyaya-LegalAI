package com.example.nyayalegalai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.repository.FirestoreRepository
import com.example.nyayalegalai.ui.navigation.Route
import com.example.nyayalegalai.utils.SessionManager
import com.example.nyayalegalai.viewmodel.ConsultationViewModel
import com.example.nyayalegalai.viewmodel.UploadState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(navController: NavController, viewModel: ConsultationViewModel, lawyerId: String) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val user = sessionManager.getUser()

    var lawyerProfile by remember { mutableStateOf<LawyerProfile?>(null) }
    LaunchedEffect(lawyerId) {
        viewModel.resetUploadState()
        val repo = FirestoreRepository()
        val fetched = repo.getLawyerProfile(lawyerId)
        if (fetched != null) {
            lawyerProfile = fetched
        } else {
            lawyerProfile = viewModel.allLawyers.value.find { it.lawyerId == lawyerId || it.userId == lawyerId }
        }
    }

    var clientName by remember { mutableStateOf(user?.name ?: "") }
    var caseTitle by remember { mutableStateOf("") }
    var caseDescription by remember { mutableStateOf("") }
    var consultationType by remember { mutableStateOf("Online") } // Online or In-Person
    var date by remember { mutableStateOf("") } // Empty initially
    var selectedTimeSlot by remember { mutableStateOf("") } // Empty initially
    var preferredLanguage by remember { mutableStateOf("English") }
    var contactNumber by remember { mutableStateOf(user?.phone ?: "") }
    var additionalNotes by remember { mutableStateOf("") }

    val languages = listOf("English", "Hindi", "Tamil", "Telugu", "Marathi", "Bengali", "Kannada", "Malayalam")
    val categories = listOf("Criminal Law", "Civil Law", "Family Law", "Property Law", "Corporate Law", "Cyber Law", "Other")
    
    var selectedCategory by remember { mutableStateOf("Civil Law") }
    var expandedCat by remember { mutableStateOf(false) }
    var expandedLang by remember { mutableStateOf(false) }

    val fee = lawyerProfile?.consultationFee ?: 500.0
    val lawyerName = lawyerProfile?.name ?: "Advocate"

    val uploadState by viewModel.uploadState.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    val inPersonAvailable = lawyerProfile?.isInPersonAvailable ?: true
    LaunchedEffect(inPersonAvailable) {
        if (!inPersonAvailable && consultationType == "In-Person") {
            consultationType = "Online"
        }
    }

    val calendar = java.util.Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selCal = java.util.Calendar.getInstance()
            selCal.set(year, month, dayOfMonth)
            val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            date = format.format(selCal.time)
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val minStr = String.format("%02d", minute)
            selectedTimeSlot = "$hour:$minStr $amPm"
        },
        calendar.get(java.util.Calendar.HOUR_OF_DAY),
        calendar.get(java.util.Calendar.MINUTE),
        false
    )

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            Toast.makeText(context, "✓ Consultation request sent successfully.", Toast.LENGTH_LONG).show()
            navController.navigate(Route.LawyerHistory.route) {
                popUpTo(Route.LawyerConsultation.route) { inclusive = false }
            }
        } else if (uploadState is UploadState.Error) {
            Toast.makeText(context, (uploadState as UploadState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Pre-submission Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Consultation Request?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lawyer: $lawyerName")
                    Text("Type: $consultationType Consultation")
                    Text("Preferred Date: $date")
                    Text("Preferred Time: $selectedTimeSlot")
                    Text("Consultation Fee: ₹${fee.toInt()}")
                    Text("Legal Category: $selectedCategory")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.bookConsultation(
                            lawyerUid = lawyerId,
                            lawyerName = lawyerName,
                            userName = clientName,
                            issueType = selectedCategory,
                            description = caseDescription,
                            dateTime = "$date, $selectedTimeSlot",
                            contact = contactNumber,
                            caseTitle = caseTitle,
                            consultationType = consultationType,
                            date = date,
                            time = selectedTimeSlot,
                            preferredLanguage = preferredLanguage,
                            notes = additionalNotes,
                            fee = fee
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Send Request", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Consultation", fontWeight = FontWeight.Bold) },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lawyer Overview Mini Card
            if (lawyerProfile != null) {
                val prof = lawyerProfile!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            color = primaryColor.copy(alpha = 0.1f)
                        ) {
                            if (prof.displayPhoto.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = prof.displayPhoto,
                                    contentDescription = prof.name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(prof.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(prof.specialization.ifEmpty { "General Practice" }, style = MaterialTheme.typography.bodyMedium, color = primaryColor, fontWeight = FontWeight.SemiBold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("★ ${prof.rating}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                                Text("•", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text("${prof.experience} Exp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("•", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                Text(prof.displayLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            Text("Fee: ₹${prof.consultationFee.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = primaryColor)
                        }
                    }
                }
            }

            // Case & Request Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("1. Case & Contact Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = primaryColor)

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Your Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = caseTitle,
                        onValueChange = { caseTitle = it },
                        label = { Text("Legal Issue / Case Title * (e.g. Property Dispute)") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Legal Category Dropdown Selection
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = !expandedCat },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Legal Category *") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = primaryColor) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = caseDescription,
                        onValueChange = { caseDescription = it },
                        label = { Text("Description of Case *") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("Contact Phone Number *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text("2. Consultation Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = primaryColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable { consultationType = "Online" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (consultationType == "Online") primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (consultationType == "Online") primaryColor else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, tint = if (consultationType == "Online") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Online", fontWeight = FontWeight.Bold, color = if (consultationType == "Online") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clickable(enabled = inPersonAvailable) { consultationType = "In-Person" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!inPersonAvailable) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            } else if (consultationType == "In-Person") {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = BorderStroke(
                                1.dp, 
                                if (!inPersonAvailable) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                } else if (consultationType == "In-Person") {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                        ) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Business, 
                                    contentDescription = null, 
                                    tint = if (!inPersonAvailable) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    } else if (consultationType == "In-Person") {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (inPersonAvailable) "In-Person" else "In-Person (Unavailable)", 
                                    fontWeight = FontWeight.Bold, 
                                    color = if (!inPersonAvailable) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    } else if (consultationType == "In-Person") {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    Text("3. Date & Time Slot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = primaryColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val todayStr = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()) }
                        val tomorrowStr = remember {
                            val cal = java.util.Calendar.getInstance()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
                        }

                        val isTodaySelected = date == todayStr
                        val isTomorrowSelected = date == tomorrowStr
                        val isCustomSelected = date.isNotEmpty() && !isTodaySelected && !isTomorrowSelected

                        FilterChip(
                            selected = isTodaySelected,
                            onClick = { date = todayStr },
                            label = { Text("Today") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = isTomorrowSelected,
                            onClick = { date = tomorrowStr },
                            label = { Text("Tomorrow") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = isCustomSelected,
                            onClick = { datePickerDialog.show() },
                            label = { Text(if (isCustomSelected) date else "Select Date") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Date Picker
                    Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                        OutlinedTextField(
                            value = if (date.isEmpty()) "Select Preferred Date *" else date,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Preferred Date *") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = primaryColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (date.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = primaryColor,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Time Picker
                    Box(modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() }) {
                        OutlinedTextField(
                            value = if (selectedTimeSlot.isEmpty()) "Select Preferred Time *" else selectedTimeSlot,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Preferred Time *") },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = primaryColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (selectedTimeSlot.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLeadingIconColor = primaryColor,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Preferred Language Picker
                    ExposedDropdownMenuBox(
                        expanded = expandedLang,
                        onExpandedChange = { expandedLang = !expandedLang },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = preferredLanguage,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Preferred Language") },
                            leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = primaryColor) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLang) },
                            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLang,
                            onDismissRequest = { expandedLang = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        preferredLanguage = lang
                                        expandedLang = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = { Text("Additional Notes (Optional)") },
                        leadingIcon = { Icon(Icons.Default.NoteAlt, contentDescription = null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )
                }
            }

            // Booking & Fee Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Booking Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primaryColor)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lawyer:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lawyerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date & Time:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (date.isEmpty() || selectedTimeSlot.isEmpty()) "Not specified" else "$date ($selectedTimeSlot)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Consultation Type:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(consultationType, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Consultation Fee:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${fee.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("₹${fee.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = primaryColor)
                    }
                }
            }

            // Confirm Booking Button
            val isConsultationTypeValid = consultationType == "Online" || (consultationType == "In-Person" && inPersonAvailable)
            val isFormValid = clientName.isNotBlank() && caseTitle.isNotBlank() && caseDescription.isNotBlank() && contactNumber.isNotBlank() && date.isNotBlank() && selectedTimeSlot.isNotBlank() && isConsultationTypeValid

            Button(
                onClick = {
                    if (isFormValid) {
                        showConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isFormValid && uploadState !is UploadState.Loading
            ) {
                if (uploadState is UploadState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm Booking Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

