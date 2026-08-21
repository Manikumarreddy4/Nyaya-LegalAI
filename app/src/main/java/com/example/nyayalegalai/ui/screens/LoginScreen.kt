package com.example.nyayalegalai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.nyayalegalai.models.SavedAccount
import com.example.nyayalegalai.ui.navigation.Route
import com.example.nyayalegalai.utils.SessionManager
import com.example.nyayalegalai.viewmodel.LoginState
import com.example.nyayalegalai.viewmodel.LoginViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, loginViewModel: LoginViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val loginState by loginViewModel.loginState.collectAsState()

    var showAccountPicker by remember { mutableStateOf(false) }
    val savedAccounts = remember { mutableStateListOf<SavedAccount>() }

    LaunchedEffect(Unit) {
        savedAccounts.clear()
        savedAccounts.addAll(sessionManager.getSavedAccounts())
        if (savedAccounts.isNotEmpty() && !sessionManager.isLoggedIn()) {
            showAccountPicker = true
        }
    }

    // Redirect if already logged in
    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn()) {
            val user = sessionManager.getUser()
            val dest = if (user?.role?.uppercase() == "LAWYER") Route.LawyerDashboard.route else Route.Dashboard.route
            navController.navigate(dest) {
                popUpTo(Route.Login.route) { inclusive = true }
            }
        }
    }

    // Handle LoginState changes
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val role = (loginState as LoginState.Success).role
            val dest = if (role.uppercase() == "LAWYER") Route.LawyerDashboard.route else Route.Dashboard.route
            navController.navigate(dest) {
                popUpTo(Route.Login.route) { inclusive = true }
            }
            loginViewModel.resetState()
        }
    }

    // Email Pattern Validation
    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    // Form Entrance Slide Up Animation Setup
    val cardTransitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val entranceTransition = rememberTransition(cardTransitionState, label = "cardEntrance")
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
            // Upper Brand Card
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
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = stringResource(id = R.string.secure_legal_assistant),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // Authentication Form Card with Animated Slide Up Entrance
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = cardOffsetY.toPx()
                    }
                    .alpha(cardAlpha),
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
                    Text(
                        text = "Login to your Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Email Input Field with validation
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; loginViewModel.resetState() },
                        label = { Text(stringResource(id = R.string.email_address)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            if (email.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isEmailValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid email format",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = { email = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; loginViewModel.resetState() },
                        label = { Text(stringResource(id = R.string.password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (password.isNotEmpty()) {
                                    IconButton(onClick = { password = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Forgot Password Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                loginViewModel.forgotPassword(email)
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = secondaryColor
                            )
                        }
                    }

                    // Success/Message Alert Box
                    AnimatedVisibility(
                        visible = loginState is LoginState.Message,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val msg = (loginState as? LoginState.Message)?.msg ?: ""
                        Surface(
                            color = primaryColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = primaryColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Error Alert Box
                    AnimatedVisibility(
                        visible = loginState is LoginState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val msg = (loginState as? LoginState.Error)?.message ?: ""
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 12.dp)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Login Action Button with gradient and scaling click logic
                    val canSubmit = isEmailValid && password.isNotBlank() && loginState !is LoginState.Loading
                    val buttonBrush = if (canSubmit) {
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
                            .clickable(enabled = canSubmit) {
                                loginViewModel.login(email, password)
                            },
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .background(buttonBrush)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (loginState is LoginState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    stringResource(id = R.string.login),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canSubmit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Transition Screen Actions
            TextButton(
                onClick = { navController.navigate(Route.Signup.route) }
            ) {
                Text(
                    text = stringResource(id = R.string.dont_have_account),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // Saved Accounts Picker bottom sheet
        if (showAccountPicker) {
            AccountPickerBottomSheet(
                accounts = savedAccounts,
                onAccountSelected = { account ->
                    email = account.email
                    password = account.password ?: ""
                    showAccountPicker = false
                    if (email.isNotBlank() && password.isNotBlank()) {
                        loginViewModel.login(email, password)
                    }
                },
                onRemoveAccount = { acc ->
                    sessionManager.removeAccount(acc.email)
                    savedAccounts.remove(acc)
                    if (savedAccounts.isEmpty()) {
                        showAccountPicker = false
                    }
                },
                onClearAll = {
                    sessionManager.clearAllAccounts()
                    savedAccounts.clear()
                    showAccountPicker = false
                },
                onUseAnotherAccount = {
                    showAccountPicker = false
                },
                onDismiss = { showAccountPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerBottomSheet(
    accounts: List<SavedAccount>,
    onAccountSelected: (SavedAccount) -> Unit,
    onRemoveAccount: (SavedAccount) -> Unit,
    onClearAll: () -> Unit,
    onUseAnotherAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select an Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (accounts.size > 1) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear all", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(accounts) { account ->
                    AccountItem(
                        account = account,
                        onClick = { onAccountSelected(account) },
                        onRemove = { onRemoveAccount(account) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onUseAnotherAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use another account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: SavedAccount,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (account.role.uppercase() == "LAWYER") Icons.Default.Gavel else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = account.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = if (account.role.uppercase() == "LAWYER") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = account.role.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (account.role.uppercase() == "LAWYER") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove account",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
