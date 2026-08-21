package com.example.nyayalegalai.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.nyayalegalai.R
import com.example.nyayalegalai.ui.navigation.Route
import com.example.nyayalegalai.viewmodel.SignupState
import com.example.nyayalegalai.viewmodel.SignupViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController, signupViewModel: SignupViewModel) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Role selection: Client or Lawyer
    var selectedRole by remember { mutableStateOf("Client") } // "Client" or "Lawyer"

    // Lawyer specific fields
    var barId by remember { mutableStateOf("") }
    var stateBarCouncil by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var additionalSpecializations by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var languages by remember { mutableStateOf("English, Hindi") }
    var bio by remember { mutableStateOf("") }
    var consultationFee by remember { mutableStateOf("500") }
    var onlineAvailable by remember { mutableStateOf(true) }
    var inPersonAvailable by remember { mutableStateOf(true) }
    var emergencyAvailable by remember { mutableStateOf(false) }
    var officeAddress by remember { mutableStateOf("") }
    var availableDays by remember { mutableStateOf("Mon - Sat") }
    var availableTime by remember { mutableStateOf("09:00 AM - 06:00 PM") }

    val signupState by signupViewModel.signupState.collectAsState()

    // Inline field validation states
    val isFullNameValid = remember(fullName) { fullName.trim().length >= 3 }
    val isEmailValid = remember(email) { android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }
    val isPhoneValid = remember(phone) { phone.trim().length == 10 && phone.all { it.isDigit() } }
    val isConfirmPasswordValid = remember(password, confirmPassword) { confirmPassword.isNotEmpty() && confirmPassword == password }

    // Handle SignupState changes
    LaunchedEffect(signupState) {
        Log.d("SIGNUP_DEBUG", "SignupScreen Received State Change: $signupState")
        if (signupState is SignupState.Success) {
            val role = (signupState as SignupState.Success).role
            try {
                if (role.equals("LAWYER", ignoreCase = true)) {
                    navController.navigate(Route.LawyerDashboard.route) {
                        popUpTo(Route.Signup.route) { inclusive = true }
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Route.AppTutorial.route) {
                        popUpTo(Route.Signup.route) { inclusive = true }
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                }
                signupViewModel.resetState()
            } catch (e: Exception) {
                Log.e("SIGNUP_DEBUG", "Navigation Failed!", e)
            }
        }
    }

    // Form Entrance Slide Up Animation Setup
    val cardTransitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val entranceTransition = rememberTransition(cardTransitionState, label = "signupCardEntrance")
    val cardOffsetY by entranceTransition.animateDp(
        transitionSpec = { spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow) },
        label = "offset"
    ) { state -> if (state) 0.dp else 100.dp }
    val cardAlpha by entranceTransition.animateFloat(
        transitionSpec = { tween(600, easing = LinearOutSlowInEasing) },
        label = "alpha"
    ) { state -> if (state) 1f else 0f }

    // Pulsing branding logo
    val logoPulse = rememberInfiniteTransition(label = "pulse")
    val logoScale by logoPulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep Slate Navy
                        Color(0xFF1E1B4B), // Midnight Indigo
                        Color(0xFF1A237E)  // Dark Blue Accent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(logoScale),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.create_account),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = stringResource(id = R.string.join_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // Main Signup Form Card with Slide-up Animation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = cardOffsetY.toPx()
                    }
                    .alpha(cardAlpha)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Full Name Input
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(stringResource(id = R.string.full_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            if (fullName.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isFullNameValid) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = { fullName = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            }
                        },
                        isError = fullName.isNotEmpty() && !isFullNameValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    if (fullName.isNotEmpty() && !isFullNameValid) {
                        Text(
                            text = "Name must be at least 3 characters",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(id = R.string.email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            if (email.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isEmailValid) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = { email = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            }
                        },
                        isError = email.isNotEmpty() && !isEmailValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    if (email.isNotEmpty() && !isEmailValid) {
                        Text(
                            text = "Enter a valid email address",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number Input
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(id = R.string.phone_number)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            if (phone.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPhoneValid) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = { phone = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            }
                        },
                        isError = phone.isNotEmpty() && !isPhoneValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    if (phone.isNotEmpty() && !isPhoneValid) {
                        Text(
                            text = "Phone number must be exactly 10 digits",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Selection (Client vs Lawyer)
                    Text(
                        text = "I am registering as:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { selectedRole = "Client" },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedRole == "Client") primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (selectedRole == "Client") primaryColor else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (selectedRole == "Client") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Client",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedRole == "Client") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { selectedRole = "Lawyer" },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedRole == "Lawyer") primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (selectedRole == "Lawyer") primaryColor else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (selectedRole == "Lawyer") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lawyer",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedRole == "Lawyer") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Lawyer Registration Professional Form
                    val isLawyerSelected = selectedRole.equals("Lawyer", ignoreCase = true)
                    AnimatedVisibility(
                        visible = isLawyerSelected,
                        enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + expandVertically() + fadeIn(),
                        exit = slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Professional Lawyer Details",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = primaryColor
                                    )

                                    // Bar Council Number
                                    OutlinedTextField(
                                        value = barId,
                                        onValueChange = { barId = it },
                                        label = { Text("Bar Council / Enrollment No. *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        isError = isLawyerSelected && barId.isBlank()
                                    )

                                    // State Bar Council
                                    OutlinedTextField(
                                        value = stateBarCouncil,
                                        onValueChange = { stateBarCouncil = it },
                                        label = { Text("State Bar Council (e.g., Bar Council of Delhi)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Specialization & Additional
                                    OutlinedTextField(
                                        value = specialization,
                                        onValueChange = { specialization = it },
                                        label = { Text("Primary Specialization * (e.g., Criminal, Civil)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        isError = isLawyerSelected && specialization.isBlank()
                                    )

                                    OutlinedTextField(
                                        value = additionalSpecializations,
                                        onValueChange = { additionalSpecializations = it },
                                        label = { Text("Additional Specializations (Comma separated)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Years of Experience & Qualification
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = experience,
                                            onValueChange = { experience = it },
                                            label = { Text("Experience (Yrs) *") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            isError = isLawyerSelected && experience.isBlank()
                                        )
                                        OutlinedTextField(
                                            value = qualification,
                                            onValueChange = { qualification = it },
                                            label = { Text("Qualification (e.g. LL.B)") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    // University
                                    OutlinedTextField(
                                        value = university,
                                        onValueChange = { university = it },
                                        label = { Text("University / Institution") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Current City & Languages
                                    OutlinedTextField(
                                        value = location,
                                        onValueChange = { location = it },
                                        label = { Text("Current City *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        isError = isLawyerSelected && location.isBlank()
                                    )

                                    OutlinedTextField(
                                        value = languages,
                                        onValueChange = { languages = it },
                                        label = { Text("Languages Known") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Office Address
                                    OutlinedTextField(
                                        value = officeAddress,
                                        onValueChange = { officeAddress = it },
                                        label = { Text("Office Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 2
                                    )

                                    // Consultation Fee
                                    OutlinedTextField(
                                        value = consultationFee,
                                        onValueChange = { consultationFee = it },
                                        label = { Text("Consultation Fee (₹)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )

                                    // Available Days & Time
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = availableDays,
                                            onValueChange = { availableDays = it },
                                            label = { Text("Available Days") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = availableTime,
                                            onValueChange = { availableTime = it },
                                            label = { Text("Available Time") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    // Consultation Type Checkboxes / Switches
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Online Consultation", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = onlineAvailable, onCheckedChange = { onlineAvailable = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("In-Person Consultation", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = inPersonAvailable, onCheckedChange = { inPersonAvailable = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Emergency Consultation Available", style = MaterialTheme.typography.bodyMedium)
                                        Switch(checked = emergencyAvailable, onCheckedChange = { emergencyAvailable = it })
                                    }

                                    // Bio
                                    OutlinedTextField(
                                        value = bio,
                                        onValueChange = { bio = it },
                                        label = { Text("Professional Bio") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 3
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(id = R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password Input
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(id = R.string.confirm_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            if (confirmPassword.isNotEmpty()) {
                                val tickColor = if (isConfirmPasswordValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                Icon(
                                    imageVector = if (isConfirmPasswordValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = tickColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        isError = confirmPassword.isNotEmpty() && !isConfirmPasswordValid,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )
                    if (confirmPassword.isNotEmpty() && !isConfirmPasswordValid) {
                        Text(
                            text = "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    // Error Box
                    AnimatedVisibility(
                        visible = signupState is SignupState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val msg = (signupState as? SignupState.Error)?.message ?: ""
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic form validation flags
                    val isFormValid = isFullNameValid && isEmailValid && isPhoneValid && 
                            password.isNotEmpty() && isConfirmPasswordValid && 
                            (!isLawyerSelected || (barId.isNotBlank() && specialization.isNotBlank() && experience.isNotBlank() && location.isNotBlank()))

                    // Signup Submit Button with premium horizontal gradient
                    val buttonBrush = if (isFormValid && signupState !is SignupState.Loading) {
                        Brush.horizontalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(2.dp, RoundedCornerShape(26.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(enabled = isFormValid && signupState !is SignupState.Loading) {
                                val isLawyer = selectedRole.equals("Lawyer", ignoreCase = true)
                                signupViewModel.signupUser(
                                    name = fullName,
                                    email = email,
                                    phone = phone,
                                    pass = password,
                                    confirmPass = confirmPassword,
                                    isLawyer = isLawyer,
                                    barId = barId,
                                    stateBarCouncil = stateBarCouncil,
                                    specialization = specialization,
                                    additionalSpecializations = additionalSpecializations,
                                    experience = experience,
                                    qualification = qualification,
                                    university = university,
                                    location = location,
                                    languages = languages,
                                    bio = bio,
                                    consultationFee = consultationFee.toDoubleOrNull() ?: 500.0,
                                    onlineAvailable = onlineAvailable,
                                    inPersonAvailable = inPersonAvailable,
                                    emergencyAvailable = emergencyAvailable,
                                    officeAddress = officeAddress,
                                    availableDays = availableDays,
                                    availableTime = availableTime
                                )
                            },
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .background(buttonBrush)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (signupState is SignupState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    stringResource(id = R.string.signup),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFormValid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Already have account action link
            TextButton(
                onClick = { navController.navigate(Route.Login.route) }
            ) {
                Text(
                    text = stringResource(id = R.string.already_have_account),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PasswordCheckItem(label: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
