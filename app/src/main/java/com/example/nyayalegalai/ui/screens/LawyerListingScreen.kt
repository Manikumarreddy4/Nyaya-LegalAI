package com.example.nyayalegalai.ui.screens

import java.util.Locale
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nyayalegalai.models.LawyerProfile
import com.example.nyayalegalai.ui.components.EmptyState
import com.example.nyayalegalai.viewmodel.ConsultationViewModel
import com.example.nyayalegalai.viewmodel.LawyerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerListingScreen(
    navController: NavController, 
    consultationViewModel: ConsultationViewModel,
    lawyerViewModel: LawyerViewModel
) {
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    var currentUid by remember { mutableStateOf(auth.currentUser?.uid) }
    
    DisposableEffect(auth) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    
    LaunchedEffect(currentUid) {
        lawyerViewModel.loadLawyers(force = true)
    }

    val lawyers by lawyerViewModel.lawyers.collectAsState()
    val isLoading by lawyerViewModel.isLoading.collectAsState()
    val errorMsg by lawyerViewModel.error.collectAsState()
    val searchQuery by lawyerViewModel.searchQuery.collectAsState()
    val selectedSpec by lawyerViewModel.selectedSpecialization.collectAsState()
    val selectedSort by lawyerViewModel.selectedSort.collectAsState()
    val onlineOnly by lawyerViewModel.onlineOnly.collectAsState()
    val inPersonOnly by lawyerViewModel.inPersonOnly.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    val specializations = listOf("All", "Criminal Law", "Civil Law", "Corporate Law", "Family & Corporate Law", "Property Law", "Cyber Law", "Tax Law")
    val sortOptions = listOf("Recommended", "Highest Rated", "Most Experienced", "Lowest Fee", "Highest Fee")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Find Vetted Lawyers", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Search Bar & Filter Controls Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { lawyerViewModel.onSearchQueryChanged(it) },
                    placeholder = { 
                        Text(
                            text = "Search by name, specialization or location...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { lawyerViewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear, 
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Specialization Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(specializations) { spec ->
                        CategoryChip(
                            name = spec,
                            isSelected = selectedSpec == spec,
                            onClick = { lawyerViewModel.onSpecializationSelected(spec) }
                        )
                      }
                }

                // Sorting & Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${lawyers.size} Verified Lawyer${if (lawyers.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Sort Choice Menu
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { sortExpanded = true }
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Sort: $selectedSort",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false }
                        ) {
                            sortOptions.forEach { sort ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = sort,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal
                                            )
                                        ) 
                                    },
                                    onClick = {
                                        lawyerViewModel.onSortSelected(sort)
                                        sortExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Lawyer Cards List
            if (isLoading && lawyers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (errorMsg != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = errorMsg ?: "Unable to load lawyers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { lawyerViewModel.loadLawyers() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (lawyers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val noLawyersInSystem = searchQuery.isBlank() && selectedSpec == "All"
                    EmptyState(
                        icon = Icons.Default.PersonSearch,
                        title = if (noLawyersInSystem) "No Lawyers Available" else "No Lawyers Found",
                        description = if (noLawyersInSystem) "No lawyers are currently available." else "No verified legal professionals match your current search and filter criteria. Try clearing filters."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(lawyers) { lawyer ->
                        ProfessionalLawyerCard(
                            lawyer = lawyer,
                            onViewProfile = { navController.navigate("lawyer_detail/${lawyer.lawyerId.ifBlank { lawyer.userId }}") },
                            onBookConsultation = { navController.navigate("booking_form/${lawyer.lawyerId.ifBlank { lawyer.userId }}") }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet Dialog
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filter Lawyers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Online Consultation Only", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = onlineOnly, onCheckedChange = { lawyerViewModel.onOnlineToggled(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("In-Person Consultation Only", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = inPersonOnly, onCheckedChange = { lawyerViewModel.onInPersonToggled(it) })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp)),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = name.replace(" Law", ""),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
        )
    }
}

@Composable
fun CompactBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
fun AvailabilityBadge(
    text: String,
    isActive: Boolean
) {
    val containerColor = if (isActive) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
    val contentColor = if (isActive) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
    val borderColor = if (isActive) Color(0xFFC8E6C9) else Color(0xFFE0E0E0)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
fun ProfessionalLawyerCard(
    lawyer: LawyerProfile,
    onViewProfile: () -> Unit,
    onBookConsultation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Avatar and basic info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar Container
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    if (lawyer.displayPhoto.isNotBlank()) {
                        AsyncImage(
                            model = lawyer.displayPhoto,
                            contentDescription = lawyer.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = lawyer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        val isVerified = lawyer.verificationStatus.equals("VERIFIED", ignoreCase = true) || lawyer.verificationStatus.isBlank()
                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = lawyer.specialization.ifEmpty { "General Practice" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                              )
                              Spacer(modifier = Modifier.width(2.dp))
                              Text(
                                  text = String.format(Locale.getDefault(), "%.1f", lawyer.rating),
                                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                  color = MaterialTheme.colorScheme.onSurface
                              )
                          }

                          Text(
                              text = "•",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                          )

                          Text(
                              text = "${lawyer.experience.ifEmpty { "0" }} Years Experience",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                      }
                  }
              }

              // Location Row
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                  Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = "Location",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                      modifier = Modifier.size(16.dp)
                  )
                  Text(
                      text = lawyer.displayLocation,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                  )
              }

              // Languages Row
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                  Icon(
                      imageVector = Icons.Default.Translate,
                      contentDescription = "Languages",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                      modifier = Modifier.size(16.dp)
                  )
                  Text(
                      text = "Languages: ${lawyer.languages.ifEmpty { "English, Hindi" }}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                  )
              }

              // Pricing Row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
              ) {
                  Row(
                      verticalAlignment = Alignment.Bottom
                  ) {
                      Text(
                          text = "₹${lawyer.consultationFee.toInt()}",
                          style = MaterialTheme.typography.titleLarge.copy(
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 20.sp
                          ),
                          color = MaterialTheme.colorScheme.primary
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                          text = "/ Consultation",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                  }
              }

              // Availability Badges Row
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  AvailabilityBadge(
                      text = "📱 Video Call: ${if (lawyer.onlineAvailable) "ON" else "OFF"}",
                      isActive = lawyer.onlineAvailable
                  )
                  AvailabilityBadge(
                      text = "🏢 In-Person: ${if (lawyer.inPersonAvailable) "ON" else "OFF"}",
                      isActive = lawyer.inPersonAvailable
                  )
              }

              // Buttons
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                  OutlinedButton(
                      onClick = onViewProfile,
                      modifier = Modifier
                          .weight(1f)
                          .height(40.dp),
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                      contentPadding = PaddingValues(horizontal = 4.dp)
                  ) {
                      Text(
                          text = "View Profile",
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                      )
                  }

                  Button(
                      onClick = onBookConsultation,
                      modifier = Modifier
                          .weight(1f)
                          .height(40.dp),
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      contentPadding = PaddingValues(horizontal = 4.dp),
                      enabled = lawyer.onlineAvailable || lawyer.inPersonAvailable
                  ) {
                      Text(
                          text = if (lawyer.onlineAvailable || lawyer.inPersonAvailable) "Book Consultation" else "Unavailable",
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                      )
                  }
              }
          }
      }
  }


