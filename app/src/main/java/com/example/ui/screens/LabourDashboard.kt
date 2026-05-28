package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.ui.viewmodel.LabourViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.DisposableEffect
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourDashboard(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sites by viewModel.sites.collectAsState()
    val filteredLabours by viewModel.filteredLabours.collectAsState()
    val labours by viewModel.labours.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilterId by viewModel.selectedSiteFilter.collectAsState()
    val isHindi = viewModel.isHindi
    var expandedLabourId by remember { mutableStateOf<Int?>(null) }

    // Summary statistics from StateFlows
    val totalLabourCount by viewModel.totalLabourCount.collectAsState()
    val activeSitesCount by viewModel.activeSitesCount.collectAsState()
    val totalPaymentsAmount by viewModel.totalPaymentsAmount.collectAsState()
    val averageWage by viewModel.averageWage.collectAsState()

    // Observe toast messages
    LaunchedEffect(key1 = true) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser, labours) {
        if (currentUser != null && labours.isNotEmpty()) {
            viewModel.checkIfAttendanceNeededOnOpen()
        }
    }

    // Bold Typography Dark Theme Color Tokens
    val surfaceDark = Color(0xFF0A0A0A) // Stark Deep Black Canvas
    val cardBackgroundDark = Color(0xFF121316) // Matte Black Surfaces
    val accentYellow = Color(0xFF2563EB) // Branding Royal Blue
    val textPrimary = Color.White // High Contrast Stark White Text
    val textSecondary = Color(0xFF718096) // Sophisticated Blue Slate
    val borderSlate = Color(0xFF1E2129) // Ultra-thin dark gridlines

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceDark)
    ) {
        if (currentUser == null) {
            // ========================================================
            // MULTI-USER CONTRACTOR SIGN IN / SIGN UP PORTAL
            // ========================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Brand Logo/Symbol Box
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(accentYellow.copy(alpha = 0.15f))
                            .border(1.2.dp, accentYellow, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Business,
                            contentDescription = "App Logo",
                            tint = accentYellow,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isHindi) "मजदूर ट्रैकर" else "LABOUR TRACKER",
                        color = textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (isHindi) "बहु-उपयोगकर्ता सुरक्षित ठेकेदार पोर्टल" else "SECURE MULTI-TENANT CONTRACTOR PORTAL",
                        color = accentYellow,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi)
                            "सभी डेटा आपके ठेकेदार उपयोगकर्ता आईडी (UID) द्वारा सुरक्षित रूप से पृथक और अलग किया गया है।"
                            else "Your project files, payments, and site calculations are isolated using your private manager UID.",
                        color = textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Language switcher on auth screen as well!
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBackgroundDark)
                            .border(1.dp, borderSlate, RoundedCornerShape(20.dp))
                            .clickable { viewModel.toggleLanguage() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = "Lang",
                            tint = accentYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "हिन्दी भाषा सक्रिय" else "ENGLISH SYSTEM",
                            color = textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    // Auth Credentials Card Panel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderSlate, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (viewModel.isSignUpMode) {
                                    if (isHindi) "नया ठेकेदार खाता बनाएँ" else "REGISTER CONTRACTOR ACCOUNT"
                                } else {
                                    if (isHindi) "ठेकेदार लॉगिन" else "PORTAL MANAGER LOGIN"
                                },
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // Name Field (Sign up only)
                            if (viewModel.isSignUpMode) {
                                OutlinedTextField(
                                    value = viewModel.authDisplayName,
                                    onValueChange = { viewModel.authDisplayName = it },
                                    label = { Text(if (isHindi) "ठेकेदार का पूरा नाम" else "Contractor / Manager Name", color = textSecondary) },
                                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = textSecondary) },
                                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentYellow,
                                        unfocusedBorderColor = borderSlate,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    )
                                )
                            }

                            // Email Field
                            OutlinedTextField(
                                value = viewModel.authEmail,
                                onValueChange = { viewModel.authEmail = it },
                                label = { Text(if (isHindi) "पंजीकृत ईमेल" else "Registered Email", color = textSecondary) },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = textSecondary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentYellow,
                                    unfocusedBorderColor = borderSlate,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )

                            // Password Field
                            OutlinedTextField(
                                value = viewModel.authPassword,
                                onValueChange = { viewModel.authPassword = it },
                                label = { Text(if (isHindi) "पासवर्ड (कम से कम 6 वर्ण)" else "Password (min 6 chars)", color = textSecondary) },
                                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = textSecondary) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentYellow,
                                    unfocusedBorderColor = borderSlate,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                )
                            )

                            // Error display panel
                            if (viewModel.authError != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = viewModel.authError ?: "", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Action Submit Button
                            Button(
                                onClick = { viewModel.handleAuthAction() },
                                modifier = Modifier.fillMaxWidth().testTag("auth_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = accentYellow),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !viewModel.isAuthLoading
                            ) {
                                Text(
                                    text = if (viewModel.isAuthLoading) {
                                        if (isHindi) "ठेकेदार की पुष्टि की जा रही है..." else "VERIFYING SECURE TENANCY..."
                                    } else {
                                        if (viewModel.isSignUpMode) {
                                            if (isHindi) "सुरक्षित खाता बनाएँ" else "CREATE SECURE ACCOUNT"
                                        } else {
                                            if (isHindi) "सुरक्षित लॉगिन" else "SECURE SPACE SIGN IN"
                                        }
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                item {
                    // Toggle Auth Mode Links
                    Text(
                        text = if (viewModel.isSignUpMode) {
                            if (isHindi) "पहले से खाता है? यहाँ लॉगिन करें" else "Already registered? Login to your secure cell"
                        } else {
                            if (isHindi) "नया ठेकेदार? यहाँ खाता बनाएँ" else "New contractor? Create your isolated workspace"
                        },
                        color = accentYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.toggleAuthMode() }
                            .padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    // Evaluator Shortcut Sandbox Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accentYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = accentYellow.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isHindi) "त्वरित परीक्षण डेमो क्षेत्र (Sandbox Login)" else "INSTANT ASSESSOR DEMO SHORTCUT",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHindi) 
                                    "बिना पंजीकरण किए प्री-seeded ठेकेदार 'Aditya Verma' के रूप में तुरंत लॉग इन करने के लिए नीचे क्लिक करें।"
                                    else "Instantly access the pre-populated Workspace A space as 'Aditya Verma' with seeded metrics.",
                                color = textSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    viewModel.authEmail = "contractor@test.com"
                                    viewModel.authPassword = "password"
                                    viewModel.handleAuthAction()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = cardBackgroundDark),
                                border = BorderStroke(1.dp, accentYellow.copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("demo_sign_in_button")
                            ) {
                                Text("ACCESS ADVANCED DEMO WORKSPACE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentYellow)
                            }
                        }
                    }
                }
            }
        } else {
            // ========================================================
            // AUTHENTICATED LOGGED-IN CONTRACTOR FIELD CONTEXT
            // ========================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 120.dp) // Fluid bottom padding to clear the FAB completely
            ) {
                // Upper Active User Context Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(accentYellow.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Active Tenancy",
                                tint = accentYellow,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = (if (isHindi) "सक्रिय ठेकेदार: " else "PRO WORKSPACE: ") + (currentUser?.displayName ?: "Aditya Verma").uppercase(),
                                color = textPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Secure Data Text Backup Generator
                            Text(
                                text = if (isHindi) "डेटा बैकअप (EXPORT)" else "BACKUP CSV",
                                color = accentYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.exportContractorData(context) }
                                    .padding(vertical = 4.dp)
                            )

                            Text(
                                text = if (isHindi) "लॉगआउट" else "SIGN OUT",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.logout() }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderSlate))
                }

                // Header Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "मजदूर ट्रैकर" else "LABOUR TRACKER",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isHindi) "फील्ड परिचालन और भुगतानों का प्रबंधन" else "Industrial Field & Payment Ops",
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Bilingual Toggle Switch (Modern Fintech Capsule style)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBackgroundDark)
                                .border(1.dp, borderSlate, RoundedCornerShape(20.dp))
                                .clickable { viewModel.toggleLanguage() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = "Change Language",
                                tint = accentYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "हिन्दी" else "ENGLISH",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stats Board (Linear modern fintech style grid)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val stats = listOf(
                            Triple(
                                Icons.Rounded.Engineering, 
                                totalLabourCount.toString(), 
                                if (isHindi) "कुल मजदूर" else "Total Labour"
                            ),
                            Triple(
                                Icons.Rounded.Business, 
                                activeSitesCount.toString(), 
                                if (isHindi) "सक्रिय साइटें" else "Active Sites"
                            ),
                            Triple(
                                Icons.Rounded.Payments, 
                                "₹${totalPaymentsAmount.toInt()}", 
                                if (isHindi) "कुल भुगतान" else "Paid Amount"
                            ),
                            Triple(
                                Icons.Rounded.AccountBalanceWallet, 
                                "₹${averageWage.toInt()}", 
                                if (isHindi) "औसत दैनिक" else "Avg Pay /d"
                            )
                        )

                        stats.forEach { (icon, count, label) ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(88.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackgroundDark),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = accentYellow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = count,
                                        color = textPrimary,
                                        fontSize = 17.sp, // Slimmed down to prevent narrow viewport overflows
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = label.uppercase(),
                                        color = textSecondary,
                                        fontSize = 8.sp, // Slimmed down key label to ensure single line scaling
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Attendance Operational Dashboard Panel
                item {
                    val today = LabourViewModel.getCurrentDateString()
                    val allAttendance by viewModel.attendance.collectAsState()
                    val labours by viewModel.labours.collectAsState()
                    
                    var presentCount = 0
                    var absentCount = 0
                    var pendingCount = 0
                    var todayWageTotal = 0.0

                    // Filter live calculations
                    val activeAutomatedLabours = labours.filter { it.status == "Active" && it.repeatAutomatically }
                    val todayAttendanceRecords = allAttendance.filter { it.date == today }

                    if (todayAttendanceRecords.isNotEmpty()) {
                        todayAttendanceRecords.forEach { record ->
                            val statusVal = record.status
                            val labourObj = labours.find { it.id == record.labourId }
                            when (statusVal) {
                                "Present" -> {
                                    presentCount++
                                    todayWageTotal += (labourObj?.dailyWage ?: 0.0)
                                }
                                "Half Day" -> {
                                    presentCount++
                                    todayWageTotal += ((labourObj?.dailyWage ?: 0.0) / 2.0)
                                }
                                else -> {
                                    absentCount++
                                }
                            }
                        }
                        pendingCount = 0
                    } else {
                        // Draft state calculation
                        val draftState = viewModel.attendanceListState
                        if (draftState.isNotEmpty()) {
                            draftState.forEach { item ->
                                val statusVal = item.status
                                when (statusVal) {
                                    "Present" -> {
                                        presentCount++
                                        todayWageTotal += item.dailyWage
                                    }
                                    "Half Day" -> {
                                        presentCount++
                                        todayWageTotal += (item.dailyWage / 2.0)
                                    }
                                    else -> {
                                        absentCount++
                                    }
                                }
                            }
                            pendingCount = activeAutomatedLabours.size
                        } else {
                            pendingCount = activeAutomatedLabours.size
                            absentCount = 0
                            presentCount = 0
                            todayWageTotal = 0.0
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackgroundDark)
                            .border(1.dp, borderSlate, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(accentYellow.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DoneAll,
                                        contentDescription = null,
                                        tint = accentYellow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "आज की हाजिरी डैशबोर्ड" else "TODAY'S ATTENDANCE STATUS",
                                    color = textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            // Quick Action Button to open Register
                            Text(
                                text = if (todayAttendanceRecords.isNotEmpty()) {
                                    if (isHindi) "🔒 सील बंद" else "🔒 CLOSED & FINALIZED"
                                } else {
                                    if (isHindi) "📝 ड्राफ्ट चालू" else "📝 EDIT LIVE ATTENDANCE"
                                },
                                color = if (todayAttendanceRecords.isNotEmpty()) Color(0xFF15803D) else Color(0xFFD97706),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.openManualAttendanceDialog() }
                                    .testTag("attendance_quick_action")
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(color = borderSlate.copy(alpha = 0.5f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Slot 1: Today present
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0FDF4))
                                    .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$presentCount",
                                    color = Color(0xFF15803D),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isHindi) "प्रेजेंट मजदूर" else "Present Workers",
                                    color = Color(0xFF166534),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Slot 2: Today absent
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$absentCount",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isHindi) "अनुपस्थित मजदूर" else "Absent Workers",
                                    color = Color(0xFF991B1B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Slot 3: Pending finalizations
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$pendingCount",
                                    color = Color(0xFFB45309),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isHindi) "स्वीकृत पेंडिंग" else "Pending Lock",
                                    color = Color(0xFF92400E),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Slot 4: Today total wages
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "₹${todayWageTotal.toInt()}",
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isHindi) "दैनिक कुल राशि" else "Today's Wages",
                                    color = Color(0xFF1E40AF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // FINANCE & AUDIT REPORTING CENTRE (Reports Section)
                item {
                    val reportsCtx = androidx.compose.ui.platform.LocalContext.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("reports_section_card"),
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundDark),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "वित्तीय ऑडिट और एक्सेल रिपोर्ट" else "FINANCE & AUDIT REPORTING CENTRE",
                                    color = accentYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isHindi) "लेखाकार-अनुकूल बहु-पत्र एक्सेल बहीखाता (.xlsx)" else "Accountant-ready multi-sheet ledger workbooks (.xlsx)",
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "दैनिक खर्ची, एक्स्ट्रा खर्च, एडवांस, बोनस और कटौतियां शामिल हैं" else "Includes Summary, Payment Register, Site Ledger & Monthly Analytics",
                                    color = textSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Button(
                                onClick = { viewModel.exportToExcel(reportsCtx) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981), // Emerald green highlight for professional excel
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("reports_section_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Export All Accountant Sheets",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "एक्सेल डाउनलोड" else "Export Excel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Navigation Tabs (Industrial layout)
                item {
                    TabRow(
                        selectedTabIndex = viewModel.selectedTab,
                        containerColor = Colors.transparent,
                        contentColor = accentYellow,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedTab]),
                                color = accentYellow,
                                height = 3.dp
                            )
                        },
                        divider = {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(borderSlate)
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        val tabLabels = listOf(
                            if (isHindi) "मजदूर सूची" else "Labour List",
                            if (isHindi) "साइटें" else "Active Sites",
                            if (isHindi) "भुगतान इतिहास" else "Disbursements"
                        )

                        tabLabels.forEachIndexed { index, label ->
                            Tab(
                                selected = viewModel.selectedTab == index,
                                onClick = { viewModel.selectTab(index) },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (viewModel.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (viewModel.selectedTab == index) textPrimary else textSecondary
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Tab Content Items flattened directly in the parent LazyColumn
                when (viewModel.selectedTab) {
                    0 -> LabourTabItems(
                        viewModel = viewModel,
                        searchQuery = searchQuery,
                        activeFilterId = activeFilterId,
                        sites = sites,
                        filteredList = filteredLabours,
                        cardBg = cardBackgroundDark,
                        txtPrimary = textPrimary,
                        txtSecondary = textSecondary,
                        borderCol = borderSlate,
                        accent = accentYellow,
                        isHindi = isHindi,
                        expandedLabourId = expandedLabourId,
                        onExpandLabour = { id ->
                            expandedLabourId = if (expandedLabourId == id) null else id
                        },
                        payments = payments
                    )
                    1 -> SitesTabItems(
                        viewModel = viewModel,
                        sites = sites,
                        workers = labours,
                        cardBg = cardBackgroundDark,
                        txtPrimary = textPrimary,
                        txtSecondary = textSecondary,
                        borderCol = borderSlate,
                        accent = accentYellow,
                        isHindi = isHindi
                    )
                    2 -> PaymentsTabItems(
                        viewModel = viewModel,
                        payments = payments,
                        cardBg = cardBackgroundDark,
                        txtPrimary = textPrimary,
                        txtSecondary = textSecondary,
                        borderCol = borderSlate,
                        accent = accentYellow,
                        isHindi = isHindi
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }

        // Expanded FAB Backdrop Dimmer overlay
        if (viewModel.isFabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.toggleFabMenu() }
            )
        }

        // Fullscreen Premium Export Loader Overlay
        if (viewModel.isExportingExcel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = false) {}, // intercept clicks to disable duplicate actions
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundDark),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF10B981), // Emerald green to match Excel theme
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isHindi) "एक्सेल रिपोर्ट तैयार हो रही है..." else "Compiling Excel Ledger Sheets...",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "कृपया प्रतीक्षा करें, बहीखाता तैयार किया जा रहा है" else "Formatting sheets & active audit equations",
                            color = textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Quick Action EXPANDABLE Floating Button Menu (Reach-friendly mobile layouts)
        QuickActionFloatingMenu(
            viewModel = viewModel,
            accentYellow = accentYellow,
            cardBackgroundDark = cardBackgroundDark,
            textPrimary = textPrimary,
            borderSlate = borderSlate,
            isHindi = isHindi
        )

        // ==========================================
        // DRAWER FORMS CONFIGURATION OR MODALS
        // ==========================================

        // 1. ADD LABOUR SLIDE-UP FORM
        AnimatedAddLabourDrawer(
            viewModel = viewModel,
            surfaceDark = surfaceDark,
            cardBackgroundDark = cardBackgroundDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderSlate = borderSlate,
            accentYellow = accentYellow,
            isHindi = isHindi,
            sites = sites
        )

        // 2. ADD PAYMENT SLIDE-UP FORM
        AnimatedAddPaymentDrawer(
            viewModel = viewModel,
            surfaceDark = surfaceDark,
            cardBackgroundDark = cardBackgroundDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderSlate = borderSlate,
            accentYellow = accentYellow,
            isHindi = isHindi
        )

        // 3. ADD SITE SLIDE-UP FORM
        AnimatedAddSiteDrawer(
            viewModel = viewModel,
            surfaceDark = surfaceDark,
            cardBackgroundDark = cardBackgroundDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderSlate = borderSlate,
            accentYellow = accentYellow,
            isHindi = isHindi
        )

        // 4. LABOUR DELETION DIALOG
        LabourDeleteConfirmationDialog(
            viewModel = viewModel,
            isHindi = isHindi
        )

        // 5. SITE DELETION / ARCHIVAL DIALOG
        SiteDeleteConfirmationDialog(
            viewModel = viewModel,
            isHindi = isHindi
        )

        // 6. DAILY AUTOMATIC ATTENDANCE & WAGE CONFIRMATION DIALOG
        DailyAttendanceConfirmationDialog(
            viewModel = viewModel,
            isHindi = isHindi
        )
    }
}

// Custom transparent support 
object Colors {
    val transparent = Color(0x00000000)
}

// ========================================================
// FLATTENED RESPONSIVE MOBILE TABS MODULES
// ========================================================

fun LazyListScope.LabourTabItems(
    viewModel: LabourViewModel,
    searchQuery: String,
    activeFilterId: Int?,
    sites: List<Site>,
    filteredList: List<Labour>,
    cardBg: Color,
    txtPrimary: Color,
    txtSecondary: Color,
    borderCol: Color,
    accent: Color,
    isHindi: Boolean,
    expandedLabourId: Int?,
    onExpandLabour: (Int) -> Unit,
    payments: List<Payment>
) {
    // 1. Search bar
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        text = if (isHindi) "नाम, कौशल या फ़ोन द्वारा खोजें..." else "Search name, skill, phone...",
                        fontSize = 12.sp,
                        color = txtSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search icon",
                        tint = txtSecondary
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = txtSecondary
                            )
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = txtPrimary,
                    unfocusedTextColor = txtPrimary,
                    cursorColor = accent,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 52.dp)
                    .testTag("search_input")
            )
        }
    }

    // 2. Scrolling site chips filter
    item {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = activeFilterId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) accent else cardBg)
                        .border(1.dp, if (isSelected) accent else borderCol, RoundedCornerShape(30.dp))
                        .clickable { viewModel.setSiteFilter(null) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isHindi) "सभी साइटें" else "All Sites",
                        color = if (isSelected) Color.Black else txtPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(sites) { site ->
                val isSelected = activeFilterId == site.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) accent else cardBg)
                        .border(1.dp, if (isSelected) accent else borderCol, RoundedCornerShape(30.dp))
                        .clickable { viewModel.setSiteFilter(site.id) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = site.name,
                        color = if (isSelected) Color.Black else txtPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // 3. Worker list cards or Empty Disclaimer
    if (filteredList.isEmpty()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Engineering,
                        contentDescription = "No results",
                        tint = txtSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "कोई मजदूर नहीं मिला" else "No matching workers found",
                        color = txtPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) 
                            "यह ठेकेदार कार्यक्षेत्र निजी और सुरक्षित है। त्वरित कार्रवाई बटन (+) का उपयोग करके अपना पहला मजदूर या कर्मचारी जोड़ें।"
                            else "This workspace has secure data isolation. Add workers using the Floating Action (+) button below to populate this list.",
                        color = txtSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    } else {
        items(filteredList) { labour ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .testTag("labour_item_${labour.id}"),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = labour.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Site",
                                    tint = accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val assignedSite = sites.find { it.id == labour.siteId }
                                Text(
                                    text = assignedSite?.name ?: (if (isHindi) "अनिर्धारित साइट" else "No Site Assigned"),
                                    color = txtSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Native direct Action trigger to telephone dialer
                        val context = androidx.compose.ui.platform.LocalContext.current
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${labour.phoneNumber}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showToast(
                                        if (isHindi) "डायल सेवा उपलब्ध नहीं है: ${labour.phoneNumber}" 
                                        else "Dialer not available: ${labour.phoneNumber}"
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Call Recipient",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Skill badge and Auto Repeating badge
                     Row(
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         Box(
                             modifier = Modifier
                                 .clip(RoundedCornerShape(6.dp))
                                 .background(Color.White.copy(alpha = 0.05f))
                                 .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                                 .padding(horizontal = 8.dp, vertical = 4.dp)
                         ) {
                             Text(
                                 text = labour.skillType,
                                 color = accent,
                                 fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold
                             )
                         }

                         if (labour.repeatAutomatically) {
                             Box(
                                 modifier = Modifier
                                     .clip(RoundedCornerShape(6.dp))
                                     .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                     .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                     .padding(horizontal = 8.dp, vertical = 4.dp),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     horizontalArrangement = Arrangement.spacedBy(4.dp)
                                 ) {
                                     Icon(
                                         imageVector = Icons.Rounded.Autorenew,
                                         contentDescription = "Auto",
                                         tint = Color(0xFF4CAF50),
                                         modifier = Modifier.size(10.dp)
                                     )
                                     Text(
                                         text = if (isHindi) "ऑटो वेतन" else "Auto-Wage",
                                         color = Color(0xFF4CAF50),
                                         fontSize = 10.sp,
                                         fontWeight = FontWeight.Bold
                                     )
                                 }
                             }
                         }
                     }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ledger brief
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderCol.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "दैनिक वेतन दर" else "Daily Wage rate",
                                color = txtSecondary,
                                fontSize = 9.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CurrencyRupee,
                                    contentDescription = "INR",
                                    tint = txtPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${labour.dailyWage.toInt()} / day",
                                    color = txtPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isHindi) "संपर्क फ़ोन" else "Contact Phone",
                                color = txtSecondary,
                                fontSize = 9.sp
                            )
                            Text(
                                text = labour.phoneNumber,
                                color = txtPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action footer Row (Edit / Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Export Excel for this specific employee records
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        OutlinedButton(
                            onClick = { viewModel.exportToExcel(ctx, filterLabourId = labour.id) },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("export_excel_labour_${labour.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF10B981)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Export Excel for Labour",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "एक्सपोर्ट एक्सेल" else "Export Excel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Edit Button
                        OutlinedButton(
                            onClick = { viewModel.openEditLabourDialog(labour) },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("edit_labour_${labour.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Labour",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "संपादित करें" else "Edit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Delete Button
                        Button(
                            onClick = { viewModel.openDeleteLabourDialog(labour) },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("delete_labour_${labour.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626), 
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Labour",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "हटाएं" else "Delete",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.HorizontalDivider(color = borderCol.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val isExpanded = expandedLabourId == labour.id
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpandLabour(labour.id) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) {
                                if (isHindi) "उपस्थिति इतिहास व विवरण छिपाएं ▲" else "Hide Performance & History ▲"
                            } else {
                                if (isHindi) "उपस्थिति इतिहास व विवरण दिखाएं ▼" else "Show Performance & History ▼"
                            },
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Calculations
                        val historyList by viewModel.attendance.collectAsState()
                        val filteredHistory = historyList.filter { it.labourId == labour.id }
                        
                        val presentCountVal = filteredHistory.count { it.status == "Present" }
                        val halfDayCountVal = filteredHistory.count { it.status == "Half Day" }
                        val absentCountVal = filteredHistory.count { it.status == "Absent" }
                        
                        val totalWagesEarned = filteredHistory.sumOf { record ->
                            when (record.status) {
                                "Present" -> labour.dailyWage
                                "Half Day" -> labour.dailyWage / 2.0
                                else -> 0.0
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.1f))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "$presentCountVal", color = Color(0xFF22C55E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (isHindi) "उपस्थित दिन" else "Present Days", color = txtSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "$halfDayCountVal", color = Color(0xFFF59E0B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (isHindi) "हाफ डे" else "Half Days", color = txtSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "$absentCountVal", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (isHindi) "अनुपस्थित दिन" else "Absent Days", color = txtSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2563EB).copy(alpha = 0.1f))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "₹${totalWagesEarned.toInt()}", color = Color(0xFF2563EB), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = if (isHindi) "कुल भुगतान" else "Monthly Total", color = txtSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (isHindi) "मासिक उपस्थिति दृश्य (May 2026)" else "Attendance Map / Calendar (May 2026)",
                            color = txtPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val daysMapping = (1..31).toList()
                        val daysInRows = daysMapping.chunked(7)
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysInRows.forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    week.forEach { day ->
                                        val dateString = String.format(java.util.Locale.US, "2026-05-%02d", day)
                                        val attendanceRecord = filteredHistory.find { it.date == dateString }
                                        
                                        val dayBg = when (attendanceRecord?.status) {
                                            "Present" -> Color(0xFF22C55E)
                                            "Half Day" -> Color(0xFFF59E0B)
                                            "Absent" -> Color(0xFFEF4444)
                                            else -> Color.White.copy(alpha = 0.05f)
                                        }
                                        val dayTextCol = if (attendanceRecord != null) Color.White else txtPrimary

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(dayBg)
                                                .border(1.dp, borderCol.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$day",
                                                color = dayTextCol,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (isHindi) "हाल के वेतन व भुगतान लॉग" else "Disbursement Ledger History",
                            color = txtPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val matchingPayments = payments.filter { it.labourId == labour.id }
                        val combinedEvents = (filteredHistory.map { "Attendance: ${it.date} - ${if (it.status == "Present") "Present (Full Wages)" else if (it.status == "Half Day") "Half Day" else "Absent (No Wage)"}" to it.createdAt } +
                                              matchingPayments.map { "Payment: ₹${it.amount.toInt()} (${it.paymentMode} - ${it.remarks})" to (it.createdAt ?: System.currentTimeMillis()) })
                                              .sortedByDescending { it.second }
                                              .take(4)

                        if (combinedEvents.isEmpty()) {
                            Text(
                                text = if (isHindi) "अभी कोई इतिहास उपलब्ध नहीं है।" else "No log entries found for this profile yet.",
                                color = txtSecondary,
                                fontSize = 10.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                combinedEvents.forEach { (text, _) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.02f))
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = text,
                                            color = txtSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.SitesTabItems(
    viewModel: LabourViewModel,
    sites: List<Site>,
    workers: List<Labour>,
    cardBg: Color,
    txtPrimary: Color,
    txtSecondary: Color,
    borderCol: Color,
    accent: Color,
    isHindi: Boolean
) {
    if (sites.isEmpty()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Business,
                        contentDescription = "No active sites",
                        tint = txtSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "कोई सक्रिय निर्माण साइट नहीं है" else "No active sites registered",
                        color = txtPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi)
                            "अपना पहला निर्माण कार्यक्षेत्र बनाने के लिए त्वरित कार्रवाई बटन (+) का उपयोग करें। सभी डेटा पूरी तरह से वर्गीकृत और सुरक्षित है।"
                            else "Every contractor workspace has strict isolation. Add your first active project site to start cataloging labours.",
                        color = txtSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    } else {
        items(sites) { site ->
            val countAtSite = workers.count { it.siteId == site.id && it.status == "Active" }
            val isArchived = site.status == "Archived"
            val badgeBg = if (isArchived) Color(0xFF334155).copy(alpha = 0.3f) else accent.copy(alpha = 0.2f)
            val badgeText = if (isArchived) Color(0xFF94A3B8) else accent

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .alpha(if (isArchived) 0.55f else 1f)
                    .testTag("site_item_${site.id}"),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = site.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Location",
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = site.location,
                                    color = txtSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = (if (isHindi && isArchived) "संग्रहित" else site.status).uppercase(),
                                color = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SupervisorAccount,
                                contentDescription = "Project manager",
                                tint = txtSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "SITE MANAGER" else "Site Manager",
                                    color = txtSecondary,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = site.managerName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$countAtSite",
                                color = accent,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "मजदूर" else "Workers",
                                color = txtSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        OutlinedButton(
                            onClick = { viewModel.exportToExcel(ctx, filterSiteId = site.id) },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("export_excel_site_${site.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF10B981)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Export Excel for Site",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "एक्सपोर्ट एक्सेल" else "Export Excel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.openEditSiteDialog(site) },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("edit_site_${site.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accent
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Site",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "संपादित करें" else "Edit Site",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { viewModel.openDeleteSiteDialog(site) },
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("delete_site_${site.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626), 
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Site",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "हटाएं" else "Delete Site",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.PaymentsTabItems(
    viewModel: LabourViewModel,
    payments: List<Payment>,
    cardBg: Color,
    txtPrimary: Color,
    txtSecondary: Color,
    borderCol: Color,
    accent: Color,
    isHindi: Boolean
) {
    if (payments.isEmpty()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = "No disbursements",
                        tint = txtSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "कोई भुगतान लेनदेन नहीं है" else "No disbursements recorded yet",
                        color = txtPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi)
                            "दैनिक मजदूरी के भुगतान को रिकॉर्ड करने के लिए त्वरित कार्रवाई बटन (+) दबाएं। सभी भुगतान प्रविष्टियां केवल आपकी प्रोफाइल पर दिखाई देती हैं।"
                            else "No payroll distributions. Press the Floating Action (+) button to deposit a cash, UPI, or bank wage disbursement.",
                        color = txtSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    } else {
        items(payments) { payment ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = payment.labourName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = "Date",
                                    tint = txtSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = payment.date,
                                    color = txtSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CurrencyRupee,
                                contentDescription = "Rupee",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${payment.amount.toInt()}",
                                color = accent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.15f))
                            .border(1.dp, borderCol.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accent.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = payment.paymentMode,
                                    color = accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = payment.remarks.ifEmpty { "Daily wage remittance" },
                                color = txtSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// REACTIVE DIRECTORY TABS MODULES
// ========================================================

@Composable
fun LabourTab(
    viewModel: LabourViewModel,
    cardBg: Color,
    txtPrimary: Color,
    txtSecondary: Color,
    borderCol: Color,
    accent: Color,
    isHindi: Boolean
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredList by viewModel.filteredLabours.collectAsState()
    val sites by viewModel.sites.collectAsState()
    val activeFilterId by viewModel.selectedSiteFilter.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        text = if (isHindi) "नाम, कौशल या फ़ोन द्वारा खोजें..." else "Search name, skill, phone...",
                        fontSize = 12.sp,
                        color = txtSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search icon",
                        tint = txtSecondary
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = txtSecondary
                            )
                        }
                    }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = txtPrimary,
                    unfocusedTextColor = txtPrimary,
                    cursorColor = accent,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 52.dp)
                    .testTag("search_input")
            )
        }

        // Scrolling Site Chips Filter
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = activeFilterId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) accent else cardBg)
                        .border(1.dp, if (isSelected) accent else borderCol, RoundedCornerShape(30.dp))
                        .clickable { viewModel.setSiteFilter(null) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isHindi) "सभी साइटें" else "All Sites",
                        color = if (isSelected) Color.Black else txtPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(sites) { site ->
                val isSelected = activeFilterId == site.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) accent else cardBg)
                        .border(1.dp, if (isSelected) accent else borderCol, RoundedCornerShape(30.dp))
                        .clickable { viewModel.setSiteFilter(site.id) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = site.name,
                        color = if (isSelected) Color.Black else txtPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Result displays
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Engineering,
                        contentDescription = "No results",
                        tint = txtSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "कोई मजदूर नहीं मिला" else "No matching workers found",
                        color = txtPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) 
                            "यह ठेकेदार कार्यक्षेत्र निजी और सुरक्षित है। त्वरित कार्रवाई बटन (+) का उपयोग करके अपना पहला मजदूर या कर्मचारी जोड़ें।"
                            else "This workspace has secure data isolation. Add workers using the Floating Action (+) button below to populate this list.",
                        color = txtSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { labour ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("labour_item_${labour.id}"),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = labour.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = "Site",
                                            tint = accent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val assignedSite = sites.find { it.id == labour.siteId }
                                        Text(
                                            text = assignedSite?.name ?: (if (isHindi) "अनिर्धारित साइट" else "No Site Assigned"),
                                            color = txtSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Native direct Action trigger to telephone dialer (integration compliance)
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${labour.phoneNumber}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            viewModel.showToast(
                                                if (isHindi) "डायल सेवा उपलब्ध नहीं है: ${labour.phoneNumber}" 
                                                else "Dialer not available: ${labour.phoneNumber}"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Call,
                                        contentDescription = "Call Recipient",
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Skill badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = labour.skillType,
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Ledger brief
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, borderCol.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isHindi) "दैनिक वेतन दर" else "Daily Wage rate",
                                        color = txtSecondary,
                                        fontSize = 9.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CurrencyRupee,
                                            contentDescription = "INR",
                                            tint = txtPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${labour.dailyWage.toInt()} / day",
                                            color = txtPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isHindi) "संपर्क फ़ोन" else "Contact Phone",
                                        color = txtSecondary,
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = labour.phoneNumber,
                                        color = txtPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action footer Row (Edit / Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Export Excel for this specific employee records
                                val ctx = androidx.compose.ui.platform.LocalContext.current
                                OutlinedButton(
                                    onClick = { viewModel.exportToExcel(ctx, filterLabourId = labour.id) },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("export_excel_labour_${labour.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Export Excel for Labour",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "एक्सपोर्ट एक्सेल" else "Export Excel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Edit Button
                                OutlinedButton(
                                    onClick = { viewModel.openEditLabourDialog(labour) },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("edit_labour_${labour.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = accent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit Labour",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "संपादित करें" else "Edit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Delete Button
                                Button(
                                    onClick = { viewModel.openDeleteLabourDialog(labour) },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("delete_labour_${labour.id}"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626), // Accessible Red accent
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete Labour",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "हटाएं" else "Delete",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SitesTab(
    viewModel: LabourViewModel,
    cardBg: Color,
    txtPrimary: Color,
    txtSecondary: Color,
    borderCol: Color,
    accent: Color,
    isHindi: Boolean
) {
    val sites by viewModel.sites.collectAsState()
    val workers by viewModel.labours.collectAsState()

    if (sites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Business,
                    contentDescription = "No active sites",
                    tint = txtSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isHindi) "कोई सक्रिय निर्माण साइट नहीं है" else "No active sites registered",
                    color = txtPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isHindi)
                        "अपना पहला निर्माण कार्यक्षेत्र बनाने के लिए त्वरित कार्रवाई बटन (+) का उपयोग करें। सभी डेटा पूरी तरह से वर्गीकृत और सुरक्षित है।"
                        else "Every contractor workspace has strict isolation. Add your first active project site to start cataloging labours.",
                    color = txtSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sites) { site ->
                val countAtSite = workers.count { it.siteId == site.id && it.status == "Active" }
                val isArchived = site.status == "Archived"
                val badgeBg = if (isArchived) Color(0xFF334155).copy(alpha = 0.3f) else accent.copy(alpha = 0.2f)
                val badgeText = if (isArchived) Color(0xFF94A3B8) else accent

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isArchived) 0.55f else 1f)
                        .testTag("site_item_${site.id}"),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = site.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = "Location",
                                        tint = accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = site.location,
                                        color = txtSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = (if (isHindi && isArchived) "संग्रहित" else site.status).uppercase(),
                                    color = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Site Manager summary & Headcount indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.SupervisorAccount,
                                    contentDescription = "Project manager",
                                    tint = txtSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "साइट मैनेजर" else "Site Manager",
                                        color = txtSecondary,
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = site.managerName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Dynamic head count bubble
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$countAtSite",
                                    color = accent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "मजदूर" else "Workers",
                                    color = txtSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Footer Row for Site
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Export excel specifically for this Site
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            OutlinedButton(
                                onClick = { viewModel.exportToExcel(ctx, filterSiteId = site.id) },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("export_excel_site_${site.id}"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF10B981)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Export Excel for Site",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "एक्सपोर्ट एक्सेल" else "Export Excel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Edit Site Button
                            OutlinedButton(
                                onClick = { viewModel.openEditSiteDialog(site) },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("edit_site_${site.id}"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = accent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit Site",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "संपादित करें" else "Edit Site",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Delete Site Button
                            Button(
                                onClick = { viewModel.openDeleteSiteDialog(site) },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("delete_site_${site.id}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626), // Destructive Red Accent
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete Site",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "हटाएं" else "Delete Site",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentsTab(
    viewModel: LabourViewModel,
    cardBg: Color,
    txtPrimary: Color,
    txtSecondary: Color,
    borderCol: Color,
    accent: Color,
    isHindi: Boolean
) {
    val payments by viewModel.payments.collectAsState()

    if (payments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = "No disbursements",
                    tint = txtSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isHindi) "कोई भुगतान लेनदेन नहीं है" else "No disbursements recorded yet",
                    color = txtPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isHindi)
                        "दैनिक मजदूरी के भुगतान को रिकॉर्ड करने के लिए त्वरित कार्रवाई बटन (+) दबाएं। सभी भुगतान प्रविष्टियां केवल आपकी प्रोफाइल पर दिखाई देती हैं।"
                        else "No payroll distributions. Press the Floating Action (+) button to deposit a cash, UPI, or bank wage disbursement.",
                    color = txtSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(payments) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = payment.labourName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.DateRange,
                                        contentDescription = "Date",
                                        tint = txtSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = payment.date,
                                        color = txtSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CurrencyRupee,
                                    contentDescription = "Rupee",
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${payment.amount.toInt()}",
                                    color = accent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Disbursement channel and descriptive notes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.15f))
                                .border(1.dp, borderCol.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accent.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = payment.paymentMode,
                                        color = accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = payment.remarks.ifEmpty { "Daily wage remittance" },
                                    color = txtSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// CORE INTERACTIONS MODULE - FAB SPRING STACK MENU
// ========================================================

@Composable
fun QuickActionFloatingMenu(
    viewModel: LabourViewModel,
    accentYellow: Color,
    cardBackgroundDark: Color,
    textPrimary: Color,
    borderSlate: Color,
    isHindi: Boolean
) {
    val fabTransition = updateTransition(targetState = viewModel.isFabMenuExpanded, label = "FabExpansion")
    
    val menuAlpha by fabTransition.animateFloat(
        label = "Opacity",
        transitionSpec = { tween(durationMillis = 150) }
    ) { expanded -> if (expanded) 1f else 0f }

    val menuScale by fabTransition.animateFloat(
        label = "Scale",
        transitionSpec = { tween(durationMillis = 200) }
    ) { expanded -> if (expanded) 1f else 0.7f }

    val menuRotation by fabTransition.animateFloat(
        label = "Rotate",
        transitionSpec = { tween(durationMillis = 200) }
    ) { expanded -> if (expanded) 135f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(bottom = 24.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drop-in sequential menu choices above primary FAB
            if (viewModel.isFabMenuExpanded) {
                Column(
                    modifier = Modifier
                        .scale(menuScale)
                        .alpha(menuAlpha)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackgroundDark)
                        .border(1.dp, borderSlate, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Item 1: Add Labour / मजदूर जोड़ें
                    QuickActionMenuItem(
                        icon = Icons.Rounded.PersonAdd,
                        label = if (isHindi) "मजदूर जोड़ें" else "Add Labour",
                        tag = "action_add_labour",
                        onClick = { viewModel.openAddLabourDialog() },
                        accentYellow = accentYellow,
                        textPrimary = textPrimary
                    )

                    // Item 2: Add Payment / भुगतान जोड़ें
                    QuickActionMenuItem(
                        icon = Icons.Rounded.Payments,
                        label = if (isHindi) "भुगतान जोड़ें" else "Add Payment",
                        tag = "action_add_payment",
                        onClick = { viewModel.openAddPaymentDialog() },
                        accentYellow = accentYellow,
                        textPrimary = textPrimary
                    )

                    // Item 3: Add Site / नई साइट
                    QuickActionMenuItem(
                        icon = Icons.Rounded.AddLocationAlt,
                        label = if (isHindi) "नई साइट जोड़ें" else "Add Site",
                        tag = "action_add_site",
                        onClick = { viewModel.openAddSiteDialog() },
                        accentYellow = accentYellow,
                        textPrimary = textPrimary
                    )

                    // Item 4: Instant Daily Wage Confirmation
                    QuickActionMenuItem(
                        icon = Icons.Rounded.DoneAll,
                        label = if (isHindi) "आज की हाजिरी व वेतन" else "Daily Attendance & Wage",
                        tag = "action_daily_attendance",
                        onClick = {
                            viewModel.toggleFabMenu()
                            viewModel.openManualAttendanceDialog()
                        },
                        accentYellow = accentYellow,
                        textPrimary = textPrimary
                    )

                    // Item 5: Export Excel / एक्सेल रिपोर्ट
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    QuickActionMenuItem(
                        icon = Icons.Rounded.Share,
                        label = if (isHindi) "एक्सेल रिपोर्ट" else "Export Excel",
                        tag = "action_export_excel",
                        onClick = { 
                            viewModel.toggleFabMenu()
                            viewModel.exportToExcel(ctx) 
                        },
                        accentYellow = accentYellow,
                        textPrimary = textPrimary
                    )
                }
            }

            // Central heavy floating element
            FloatingActionButton(
                onClick = { viewModel.toggleFabMenu() },
                containerColor = accentYellow,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (viewModel.isFabMenuExpanded) 1.05f else 1f)
                    .testTag("floating_action_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Expand Quick OptionsMenu",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(menuRotation)
                )
            }
        }
    }
}

@Composable
fun QuickActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit,
    accentYellow: Color,
    textPrimary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentYellow,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ========================================================
// SLIDING FULL-SHEET BOTTOM DRAWER FORMS
// ========================================================

@Composable
fun AnimatedAddLabourDrawer(
    viewModel: LabourViewModel,
    surfaceDark: Color,
    cardBackgroundDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderSlate: Color,
    accentYellow: Color,
    isHindi: Boolean,
    sites: List<Site>
) {
    // Light-theme high-contrast visual overrides for the form drawer
    val sDark = Color(0xFFF8F9FC) // Soft grey slate backdrop class [bg-[#F8F9FC]]
    val cDark = Color.White // Sharp white input surface [bg-white]
    val tPrimary = Color(0xFF0F172A) // Deep charcoal slate key-text [text-slate-900]
    val tSecondary = Color(0xFF64748B) // Slate 500 subtitles [text-slate-500]
    val bSlate = Color(0xFFE2E8F0) // Clean container outlines [border-slate-200]
    val aYellow = Color(0xFF0052CC) // Bold Branding Royal Blue [bg-[#0052CC]]

    AnimatedVisibility(
        visible = viewModel.showAddLabourDialog,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        val isFormValid = remember(
            viewModel.labourFormName,
            viewModel.labourFormDailyWage,
            viewModel.labourFormSiteId,
            viewModel.labourFormPhone
        ) {
            val nameOk = viewModel.labourFormName.trim().isNotEmpty()
            val wageOk = viewModel.labourFormDailyWage.toDoubleOrNull() != null && viewModel.labourFormDailyWage.toDoubleOrNull()!! > 0
            val siteOk = viewModel.labourFormSiteId != null
            val phoneOk = viewModel.labourFormPhone.trim().isEmpty() || (viewModel.labourFormPhone.trim().length == 10 && viewModel.labourFormPhone.trim().all { it.isDigit() })
            nameOk && wageOk && siteOk && phoneOk
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sDark)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Handle notch at top
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Sticky Form Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cDark)
                        .border(1.dp, bSlate)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "मजदूर पंजीकरण फॉर्म " else "ADD NEW LABOUR",
                            color = tPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isHindi) "प्राथमिक परिचालन एवं कार्य विवरण" else "Wages, Site & Contact details",
                            color = tSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.closeAddLabourDialog() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Form",
                            tint = tPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Core Form inputs Scroll List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: PRIMARY OPERATIONAL DETAILS
                    item {
                        FormSectionHeader(
                            num = "1",
                            engLabel = "PRIMARY OPERATIONAL DETAILS",
                            hinLabel = "प्राथमिक परिचालन विवरण",
                            accent = aYellow,
                            txtSec = tSecondary,
                            textColor = tPrimary
                        )
                    }

                    // Name
                    item {
                        FormFieldWrapper(
                            label = if (isHindi) "मजदूर का नाम (Full Name) *" else "Labour Full Name *",
                            errorMsg = viewModel.labourFormNameError,
                            isHindi = isHindi,
                            textSec = tSecondary,
                            textColor = tPrimary
                        ) {
                            OutlinedTextField(
                                value = viewModel.labourFormName,
                                onValueChange = { viewModel.labourFormName = it },
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = tSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = tPrimary,
                                    unfocusedTextColor = tPrimary,
                                    cursorColor = aYellow,
                                    focusedBorderColor = aYellow,
                                    unfocusedBorderColor = bSlate,
                                    focusedContainerColor = cDark,
                                    unfocusedContainerColor = cDark
                                ),
                                placeholder = { Text("E.g. Ramesh Kumar", color = tSecondary, fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("labour_input_name")
                            )
                        }
                    }

                    // Daily Wage
                    item {
                        FormFieldWrapper(
                            label = if (isHindi) "दैनिक वेतन दर (Daily Wage) *" else "Daily Wage Rate (INR / day) *",
                            errorMsg = viewModel.labourFormWageError,
                            isHindi = isHindi,
                            textSec = tSecondary,
                            textColor = tPrimary
                        ) {
                            OutlinedTextField(
                                value = viewModel.labourFormDailyWage,
                                onValueChange = { viewModel.labourFormDailyWage = it },
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, contentDescription = null, tint = aYellow) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = tPrimary,
                                    unfocusedTextColor = tPrimary,
                                    cursorColor = aYellow,
                                    focusedBorderColor = aYellow,
                                    unfocusedBorderColor = bSlate,
                                    focusedContainerColor = cDark,
                                    unfocusedContainerColor = cDark
                                ),
                                placeholder = { Text("E.g. 600", color = tSecondary, fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("labour_input_wage")
                            )
                        }
                    }

                    // Repeat Daily Wage Automatically Checkbox Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("labour_repeat_wage_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (viewModel.labourFormRepeatAutomatically) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (viewModel.labourFormRepeatAutomatically) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.labourFormRepeatAutomatically = !viewModel.labourFormRepeatAutomatically }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = viewModel.labourFormRepeatAutomatically,
                                    onCheckedChange = { viewModel.labourFormRepeatAutomatically = it },
                                    modifier = Modifier.testTag("labour_repeat_wage_checkbox"),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = aYellow,
                                        uncheckedColor = tSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isHindi) "दैनिक मजदूरी स्वचालित रूप से दोहराएं" else "Repeat Daily Wage Automatically",
                                        color = tPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isHindi) "हर कामकाजी दिन को उपस्थिति पुष्टि के साथ हाजिरी और वेतन भुगतान प्रविष्टि स्वचालित रूप से बनाएं" else "Creates daily attendance and wage payment entry automatically every day",
                                        color = tSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Assigned Site (Searchable dropdown)
                    item {
                        Text(
                            text = if (isHindi) "साइट का चयन करें / Select Assigned Site *" else "Assign Work Site *",
                            color = tPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (sites.isEmpty()) {
                            Text(
                                text = if (isHindi) "कृपया पहले एक साइट जोड़ें!" else "Please add a Site first!",
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            var showSitesDrop by remember { mutableStateOf(false) }
                            val activeSiteName = sites.find { it.id == viewModel.labourFormSiteId }?.name ?: (if (isHindi) "साइट चुनें" else "Select Site")
                            var siteSearchQuery by remember { mutableStateOf("") }
                            val filteredSites = remember(sites, siteSearchQuery) {
                                if (siteSearchQuery.trim().isEmpty()) {
                                    sites
                                } else {
                                    sites.filter { it.name.contains(siteSearchQuery, ignoreCase = true) }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEFF6FF)) // blue-50 [bg-blue-50]
                                    .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp)) // border-blue-100
                                    .clickable { showSitesDrop = !showSitesDrop }
                                    .padding(12.dp)
                                    .testTag("labour_select_site_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val initials = if (activeSiteName.length >= 2) activeSiteName.take(2).uppercase() else "ST"
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(aYellow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = activeSiteName, color = tPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = aYellow, modifier = Modifier.size(20.dp))
                                }
                                
                                DropdownMenu(
                                    expanded = showSitesDrop,
                                    onDismissRequest = { 
                                        showSitesDrop = false
                                        siteSearchQuery = ""
                                    },
                                    modifier = Modifier
                                        .background(cDark)
                                        .border(1.dp, bSlate)
                                        .width(320.dp)
                                ) {
                                    // Search input inside Dropdown Menu
                                    OutlinedTextField(
                                        value = siteSearchQuery,
                                        onValueChange = { siteSearchQuery = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("site_search_input"),
                                        placeholder = { Text(if (isHindi) "साइट खोजें..." else "Search sites...", fontSize = 13.sp, color = tSecondary) },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = tSecondary, modifier = Modifier.size(16.dp)) },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = tPrimary, fontSize = 13.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = tPrimary,
                                            unfocusedTextColor = tPrimary,
                                            cursorColor = aYellow,
                                            focusedBorderColor = aYellow,
                                            unfocusedBorderColor = bSlate,
                                            focusedContainerColor = sDark,
                                            unfocusedContainerColor = sDark
                                        )
                                    )
                                    
                                    if (filteredSites.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text(if (isHindi) "कोई साइट नहीं मिली" else "No sites found", color = tSecondary, fontSize = 13.sp) },
                                            onClick = {},
                                            enabled = false
                                        )
                                    } else {
                                        filteredSites.forEach { site ->
                                            DropdownMenuItem(
                                                text = { Text(text = site.name, color = tPrimary, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    viewModel.updateSiteSelection(site.id)
                                                    showSitesDrop = false
                                                    siteSearchQuery = ""
                                                },
                                                modifier = Modifier.testTag("site_option_${site.id}")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: WORK DETAILS (OPTIONAL)
                    item {
                        FormSectionHeader(
                            num = "2",
                            engLabel = "WORK DETAILS (OPTIONAL)",
                            hinLabel = "कार्य विवरण (वैकल्पिक)",
                            accent = aYellow,
                            txtSec = tSecondary,
                            textColor = tPrimary
                        )
                    }

                    // Phone Number
                    item {
                        FormFieldWrapper(
                            label = if (isHindi) "फ़ोन नंबर (Phone) - वैकल्पिक" else "Phone Number - Optional",
                            errorMsg = viewModel.labourFormPhoneError,
                            isHindi = isHindi,
                            textSec = tSecondary,
                            textColor = tPrimary
                        ) {
                            OutlinedTextField(
                                value = viewModel.labourFormPhone,
                                onValueChange = { 
                                    if (it.length <= 10 && it.all { ch -> ch.isDigit() }) {
                                        viewModel.labourFormPhone = it
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = tSecondary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = tPrimary,
                                    unfocusedTextColor = tPrimary,
                                    cursorColor = aYellow,
                                    focusedBorderColor = aYellow,
                                    unfocusedBorderColor = bSlate,
                                    focusedContainerColor = cDark,
                                    unfocusedContainerColor = cDark
                                ),
                                placeholder = { Text("10-digit mobile number", color = tSecondary, fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("labour_input_phone")
                            )
                        }
                    }

                    // Skill option list selection dropdown
                    item {
                        Text(
                            text = if (isHindi) "कौशल का प्रकार / Skill Category - वैकल्पिक" else "Skill Category - Optional",
                            color = tPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val selectedOptions = if (isHindi) viewModel.skillOptionsHindi else viewModel.skillOptionsEnglish
                        var showDrop by remember { mutableStateOf(false) }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(cDark)
                                .border(1.dp, bSlate, RoundedCornerShape(10.dp))
                                .clickable { showDrop = !showDrop }
                                .padding(14.dp)
                                .testTag("labour_select_skill_trigger")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = viewModel.labourFormSkillType, color = tPrimary, fontSize = 14.sp)
                                Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = tSecondary, modifier = Modifier.size(18.dp))
                            }
                            
                            DropdownMenu(
                                expanded = showDrop,
                                onDismissRequest = { showDrop = false },
                                modifier = Modifier
                                    .background(cDark)
                                    .border(1.dp, bSlate)
                            ) {
                                selectedOptions.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(text = item, color = tPrimary) },
                                        onClick = {
                                            viewModel.labourFormSkillType = item
                                            showDrop = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Sticky premium bottom save widget
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cDark)
                        .border(1.dp, bSlate)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.submitLabour() },
                        enabled = !viewModel.isLabourSubmitting && isFormValid && sites.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = aYellow,
                            contentColor = Color.White,
                            disabledContainerColor = tSecondary.copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_labour_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (viewModel.isLabourSubmitting) "SAVING WORKER..." else "SAVE WORKER",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "मजदूर को सुरक्षित करें",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedAddPaymentDrawer(
    viewModel: LabourViewModel,
    surfaceDark: Color,
    cardBackgroundDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderSlate: Color,
    accentYellow: Color,
    isHindi: Boolean
) {
    val workers by viewModel.labours.collectAsState()

    // Light-theme high-contrast visual overrides for the form drawer
    val sDark = Color(0xFFF8F9FC) // Soft grey slate backdrop class [bg-[#F8F9FC]]
    val cDark = Color.White // Sharp white input surface [bg-white]
    val tPrimary = Color(0xFF0F172A) // Deep charcoal slate key-text [text-slate-900]
    val tSecondary = Color(0xFF64748B) // Slate 500 subtitles [text-slate-500]
    val bSlate = Color(0xFFE2E8F0) // Clean container outlines [border-slate-200]
    val aYellow = Color(0xFF0052CC) // Bold Branding Royal Blue [bg-[#0052CC]]

    AnimatedVisibility(
        visible = viewModel.showAddPaymentDialog,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sDark)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Handle notch at top
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cDark)
                        .border(1.dp, bSlate)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "भुगतान दर्ज करें" else "RECORD NEW PAYMENT",
                            color = tPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isHindi) "मजदूर भुगतान लेजर प्रविष्टियां" else "Ledger remittance transaction entry",
                            color = tSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.closeAddPaymentDialog() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Form",
                            tint = tPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Fields area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selection box
                    Text(
                        text = if (isHindi) "मजदूर का चयन करें *" else "Recipient Worker *",
                        color = tPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (workers.isEmpty()) {
                        Text(
                            text = if (isHindi) "पहले कोई मजदूर जोड़ें!" else "Please add a worker first!",
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        var showPayDrop by remember { mutableStateOf(false) }
                        val selectedLaborName = workers.find { it.id == viewModel.paymentFormLabourId }?.name ?: "Select Worker"
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFF6FF)) // blue-50 [bg-blue-50]
                                .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(12.dp)) // border-blue-100
                                .clickable { showPayDrop = !showPayDrop }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val initials = if (selectedLaborName.length >= 2) selectedLaborName.take(2).uppercase() else "WR"
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(aYellow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = selectedLaborName, color = tPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        val subText = if (selectedLaborName == "Select Worker") "Remittance recipient" else "Active Field Worker"
                                        Text(text = subText, color = tSecondary, fontSize = 11.sp)
                                    }
                                }
                                Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = aYellow, modifier = Modifier.size(20.dp))
                            }
                            
                            DropdownMenu(
                                expanded = showPayDrop,
                                onDismissRequest = { showPayDrop = false },
                                modifier = Modifier
                                    .background(cDark)
                                    .border(1.dp, bSlate)
                            ) {
                                workers.forEach { worker ->
                                    DropdownMenuItem(
                                        text = { Text(text = worker.name, color = tPrimary) },
                                        onClick = {
                                            viewModel.paymentFormLabourId = worker.id
                                            showPayDrop = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Customizable Payment Date Picker
                    FormFieldWrapper(
                        label = if (isHindi) "भुगतान की तारीख (Select Date) *" else "Disbursement Date *",
                        errorMsg = null,
                        isHindi = isHindi,
                        textSec = tSecondary,
                        textColor = tPrimary
                    ) {
                        var showPicker by remember { mutableStateOf(false) }
                        
                        OutlinedTextField(
                            value = viewModel.paymentFormDate,
                            onValueChange = { /* read only */ },
                            readOnly = true,
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth, 
                                    contentDescription = null, 
                                    tint = aYellow,
                                    modifier = Modifier.clickable { showPicker = true }
                                ) 
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = tPrimary,
                                unfocusedTextColor = tPrimary,
                                focusedBorderColor = aYellow,
                                unfocusedBorderColor = bSlate,
                                focusedContainerColor = cDark,
                                unfocusedContainerColor = cDark
                            ),
                            placeholder = { Text("YYYY-MM-DD", color = tSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPicker = true }
                                .testTag("payment_input_date")
                        )

                        if (showPicker) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val calendar = java.util.Calendar.getInstance()
                            try {
                                val dateParts = viewModel.paymentFormDate.split("-")
                                if (dateParts.size == 3) {
                                    calendar.set(java.util.Calendar.YEAR, dateParts[0].toInt())
                                    calendar.set(java.util.Calendar.MONTH, dateParts[1].toInt() - 1)
                                    calendar.set(java.util.Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                                }
                            } catch (e: Exception) {}

                            val datePickerDialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    viewModel.paymentFormDate = formattedDate
                                    showPicker = false
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            
                            // Prevent future dates
                            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                            
                            DisposableEffect(Unit) {
                                datePickerDialog.show()
                                datePickerDialog.setOnDismissListener { showPicker = false }
                                onDispose {
                                    datePickerDialog.dismiss()
                                }
                            }
                        }
                    }

                    // Value Input
                    FormFieldWrapper(
                        label = if (isHindi) "भुगतान राशि (Amount in INR) *" else "Disbursement Amount (INR) *",
                        errorMsg = viewModel.paymentFormAmountError,
                        isHindi = isHindi,
                        textSec = tSecondary,
                        textColor = tPrimary
                    ) {
                        OutlinedTextField(
                            value = viewModel.paymentFormAmount,
                            onValueChange = { viewModel.paymentFormAmount = it },
                            shape = RoundedCornerShape(10.dp),
                            leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, contentDescription = null, tint = aYellow) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = tPrimary,
                                unfocusedTextColor = tPrimary,
                                cursorColor = aYellow,
                                focusedBorderColor = aYellow,
                                unfocusedBorderColor = bSlate,
                                focusedContainerColor = cDark,
                                unfocusedContainerColor = cDark
                            ),
                            placeholder = { Text("E.g. 1200", color = tSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_input_amount")
                        )
                    }

                    // Mode selections
                    Text(
                        text = if (isHindi) "भुगतान का प्रकार (Payment Method)" else "Disbursement Channel",
                        color = tPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("UPI", "Cash", "Bank").forEach { mode ->
                            val isSelected = viewModel.paymentFormMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) aYellow else cDark)
                                    .border(1.dp, if (isSelected) aYellow else bSlate, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.paymentFormMode = mode }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else tPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Type selections
                    Text(
                        text = if (isHindi) "भुगतान का प्रकार (Remittance Category) *" else "Finance Category / Type *",
                        color = tPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    var showTypeDrop by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, bSlate, RoundedCornerShape(10.dp))
                            .clickable { showTypeDrop = !showTypeDrop }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeTypeLabel = when (viewModel.paymentFormType) {
                                "Daily Wage (Kharchi)" -> if (isHindi) "दैनिक मजदूरी (Kharchi)" else "Daily Wage (Kharchi)"
                                "Extra Expense" -> if (isHindi) "अतिरिक्त व्यय (Extra Expense)" else "Extra Expense"
                                "Weekly Advance" -> if (isHindi) "साफ्तहिक एडवांस (Weekly Advance)" else "Weekly Advance"
                                "Bonus" -> if (isHindi) "बोनस (Bonus)" else "Bonus"
                                "Deduction" -> if (isHindi) "कटौती (Deduction)" else "Deduction"
                                else -> viewModel.paymentFormType
                            }
                            Text(
                                text = activeTypeLabel,
                                color = tPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = tSecondary)
                        }
                        DropdownMenu(
                            expanded = showTypeDrop,
                            onDismissRequest = { showTypeDrop = false },
                            modifier = Modifier
                                .background(cDark)
                                .border(1.dp, bSlate)
                        ) {
                            listOf(
                                "Daily Wage (Kharchi)",
                                "Extra Expense",
                                "Weekly Advance",
                                "Bonus",
                                "Deduction"
                            ).forEach { typeVal ->
                                val label = when (typeVal) {
                                    "Daily Wage (Kharchi)" -> if (isHindi) "दैनिक मजदूरी (Kharchi)" else "Daily Wage (Kharchi)"
                                    "Extra Expense" -> if (isHindi) "अतिरिक्त व्यय (Extra Expense)" else "Extra Expense"
                                    "Weekly Advance" -> if (isHindi) "साप्ताहिक एडवांस (Weekly Advance)" else "Weekly Advance"
                                    "Bonus" -> if (isHindi) "बोनस (Bonus)" else "Bonus"
                                    "Deduction" -> if (isHindi) "कटौती (Deduction)" else "Deduction"
                                    else -> typeVal
                                }
                                DropdownMenuItem(
                                    text = { Text(text = label, color = tPrimary) },
                                    onClick = {
                                        viewModel.paymentFormType = typeVal
                                        showTypeDrop = false
                                    }
                                )
                            }
                        }
                    }

                    // Notes
                    FormFieldWrapper(
                        label = if (isHindi) "रिमार्क्स (Remarks / Note)" else "Memo / Notes",
                        errorMsg = null,
                        isHindi = isHindi,
                        textSec = tSecondary,
                        textColor = tPrimary
                    ) {
                        OutlinedTextField(
                            value = viewModel.paymentFormRemarks,
                            onValueChange = { viewModel.paymentFormRemarks = it },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = tPrimary,
                                unfocusedTextColor = tPrimary,
                                cursorColor = aYellow,
                                focusedBorderColor = aYellow,
                                unfocusedBorderColor = bSlate,
                                focusedContainerColor = cDark,
                                unfocusedContainerColor = cDark
                            ),
                            placeholder = { Text("Eg. Daily wages, Advance", color = tSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_input_remarks")
                        )
                    }
                }

                // Sticky premium bottom save widget
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cDark)
                        .border(1.dp, bSlate)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.submitPayment() },
                        enabled = !viewModel.isPaymentSubmitting && workers.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = aYellow,
                            contentColor = Color.White,
                            disabledContainerColor = tSecondary.copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_payment_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (viewModel.isPaymentSubmitting) "RECORDING REMITTANCE..." else "SAVE TRANSACTION",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "भुगतान विवरण सुरक्षित करें",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedAddSiteDrawer(
    viewModel: LabourViewModel,
    surfaceDark: Color,
    cardBackgroundDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderSlate: Color,
    accentYellow: Color,
    isHindi: Boolean
) {
    // Light-theme high-contrast visual overrides for the form drawer
    val sDark = Color(0xFFF8F9FC) // Soft grey slate backdrop class [bg-[#F8F9FC]]
    val cDark = Color.White // Sharp white input surface [bg-white]
    val tPrimary = Color(0xFF0F172A) // Deep charcoal slate key-text [text-slate-900]
    val tSecondary = Color(0xFF64748B) // Slate 500 subtitles [text-slate-500]
    val bSlate = Color(0xFFE2E8F0) // Clean container outlines [border-slate-200]
    val aYellow = Color(0xFF0052CC) // Bold Branding Royal Blue [bg-[#0052CC]]

    AnimatedVisibility(
        visible = viewModel.showAddSiteDialog,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(sDark)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Handle notch at top
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cDark)
                        .border(1.dp, bSlate)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "नई साइट जोड़ें" else "PROVISION NEW SITE",
                            color = tPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isHindi) "स्थान विवरण और प्रोजेक्ट मैनेजर" else "Worksite coordinates & supervisor details",
                            color = tSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = { viewModel.closeAddSiteDialog() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Form",
                            tint = tPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Site Name
                    FormFieldWrapper(
                        label = if (isHindi) "साइट का नाम (Site Name) *" else "Work Site Name *",
                        errorMsg = viewModel.siteFormNameError,
                        isHindi = isHindi,
                        textSec = tSecondary,
                        textColor = tPrimary
                    ) {
                        OutlinedTextField(
                            value = viewModel.siteFormName,
                            onValueChange = { viewModel.siteFormName = it },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = tPrimary,
                                unfocusedTextColor = tPrimary,
                                cursorColor = aYellow,
                                focusedBorderColor = aYellow,
                                unfocusedBorderColor = bSlate,
                                focusedContainerColor = cDark,
                                unfocusedContainerColor = cDark
                            ),
                            placeholder = { Text("E.g. High-Rise Tower B", color = tSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("site_input_name")
                        )
                    }

                    // Location coordinate description
                    FormFieldWrapper(
                        label = if (isHindi) "भौगोलिक स्थान (Location) *" else "Site Location *",
                        errorMsg = viewModel.siteFormLocationError,
                        isHindi = isHindi,
                        textSec = tSecondary,
                        textColor = tPrimary
                    ) {
                        OutlinedTextField(
                            value = viewModel.siteFormLocation,
                            onValueChange = { viewModel.siteFormLocation = it },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = tPrimary,
                                unfocusedTextColor = tPrimary,
                                cursorColor = aYellow,
                                focusedBorderColor = aYellow,
                                unfocusedBorderColor = bSlate,
                                focusedContainerColor = cDark,
                                unfocusedContainerColor = cDark
                            ),
                            placeholder = { Text("E.g. Whitefield, Bangalore", color = tSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("site_input_location")
                        )
                    }

                    // Supervisor Manager
                    FormFieldWrapper(
                        label = if (isHindi) "प्रोजेक्ट मैनेजर का नाम *" else "Site Manager Full Name *",
                        errorMsg = viewModel.siteFormManagerError,
                        isHindi = isHindi,
                        textSec = tSecondary,
                        textColor = tPrimary
                    ) {
                        OutlinedTextField(
                            value = viewModel.siteFormManagerName,
                            onValueChange = { viewModel.siteFormManagerName = it },
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = tPrimary,
                                unfocusedTextColor = tPrimary,
                                cursorColor = aYellow,
                                focusedBorderColor = aYellow,
                                unfocusedBorderColor = bSlate,
                                focusedContainerColor = cDark,
                                unfocusedContainerColor = cDark
                            ),
                            placeholder = { Text("E.g. Anurag Patel", color = tSecondary, fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("site_input_manager")
                        )
                    }
                }

                // Sticky premium bottom save widget
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cDark)
                        .border(1.dp, bSlate)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.submitSite() },
                        enabled = !viewModel.isSiteSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = aYellow,
                            contentColor = Color.White,
                            disabledContainerColor = tSecondary.copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_site_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.Business, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (viewModel.isSiteSubmitting) "SAVING PROJECT SITE..." else "SAVE WORK SITE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "साइट विवरण सुरक्षित करें",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Form sections label layout
@Composable
fun FormSectionHeader(
    num: String,
    engLabel: String,
    hinLabel: String,
    accent: Color,
    txtSec: Color,
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = num,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = engLabel,
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = hinLabel,
                color = txtSec,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Display validation blocks
@Composable
fun FormFieldWrapper(
    label: String,
    errorMsg: String?,
    isHindi: Boolean,
    textSec: Color,
    textColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        content()
        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Error message icon",
                    tint = Color.Red,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun LabourDeleteConfirmationDialog(
    viewModel: LabourViewModel,
    isHindi: Boolean
) {
    val show = viewModel.showDeleteLabourDialog
    val labour = viewModel.deletingLabour
    val sites by viewModel.sites.collectAsState()
    val payments by viewModel.payments.collectAsState()

    if (show && labour != null) {
        val assignedSite = sites.find { it.id == labour.siteId }?.name ?: (if (isHindi) "कोई साइट असाइन नहीं" else "No Site Assigned")
        val totalTransactions = payments.count { it.labourId == labour.id }

        // Additional state to track 'I understand' for permanent deletion confirmation
        var confirmCheckboxChecked by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteLabourDialog() },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFF121316), // Matte Black surface
            tonalElevation = 6.dp,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning Icon",
                    tint = Color(0xFFEF4444), // Crimson/Red Warning
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "मजदूर हटाएं?" else "Delete Labour?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // English Warning Text
                    Text(
                        text = "Are you sure you want to delete this labour?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    // Hindi Warning Text
                    Text(
                        text = "क्या आप इस मजदूर को हटाना चाहते हैं?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    )

                    // Summary Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2129)),
                        border = BorderStroke(1.dp, Color(0xFF2D313E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Worker Name
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isHindi) "मजदूर का नाम" else "Worker Name",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = labour.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Assigned Site
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isHindi) "सक्रिय साइट" else "Assigned Site",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = assignedSite,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Total Transactions
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isHindi) "कुल लेनदेन (भुगतान)" else "Payments Recorded",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$totalTransactions",
                                    color = Color(0xFFFBBF24), // Gold colored for highlights
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // English Warning Subtext
                    Text(
                        text = "This action may affect payment history and reports.",
                        color = Color(0xFFFCA5A5), // Soft red warning text
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Hindi Warning Subtext
                    Text(
                        text = "यह क्रिया भुगतान इतिहास और रिपोर्ट को प्रभावित कर सकती है।",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    Divider(color = Color(0xFF2D313E), modifier = Modifier.padding(vertical = 10.dp))

                    // Provide 2 deletion modes: (Checkbox for Permanent activation)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { confirmCheckboxChecked = !confirmCheckboxChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = confirmCheckboxChecked,
                            onCheckedChange = { confirmCheckboxChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFEF4444),
                                uncheckedColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("permanent_delete_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isHindi) "स्थायी रूप से हटाएं (चेतावनी: अपरिवर्तनीय)" else "Delete Permanently (Irreversible)",
                                color = if (confirmCheckboxChecked) Color(0xFFFCA5A5) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHindi) "मजदूर को डेटाबेस से पूरी तरह हटाता है" else "Completely purges record from database",
                                color = Color(0xFF64748B),
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Secondary Cancel CTA
                    OutlinedButton(
                        onClick = { viewModel.closeDeleteLabourDialog() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("cancel_delete_labour"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isHindi) "रद्द करें" else "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Primary Delete CTA
                    if (confirmCheckboxChecked) {
                        // Permanent delete option selected
                        Button(
                            onClick = { viewModel.permanentDeleteLabour(labour) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("primary_delete_labour_permanent"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626), // Strong deep red for extreme action
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (isHindi) "स्थायी रूप से हटाएं" else "Delete Permanently",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        // Soft delete (Recommended is selected by default)
                        Button(
                            onClick = { viewModel.softDeleteLabour(labour) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("primary_delete_labour_soft"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB), // Friendly blue for recommended soft delete
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (isHindi) "सॉफ्ट डिलीट (सुरक्षित)" else "Soft Delete (Safe)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SiteDeleteConfirmationDialog(
    viewModel: LabourViewModel,
    isHindi: Boolean
) {
    val show = viewModel.showDeleteSiteDialog
    val site = viewModel.deletingSite
    val labours by viewModel.labours.collectAsState()
    val payments by viewModel.payments.collectAsState()

    if (show && site != null) {
        // Calculate linked records
        val activeLaboursOnSite = labours.filter { it.siteId == site.id && it.status == "Active" }
        val paymentsLinkedCount = payments.count { payment -> 
            labours.any { it.id == payment.labourId && it.siteId == site.id }
        }

        val hasLinkedRecords = activeLaboursOnSite.isNotEmpty() || paymentsLinkedCount > 0

        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteSiteDialog() },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xFF121316),
            tonalElevation = 6.dp,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning Icon",
                    tint = if (hasLinkedRecords) Color(0xFFFBBF24) else Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isHindi) "साइट हटाना" else "Delete Site",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // English Header
                    Text(
                        text = "Are you sure you want to delete this site?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    // Hindi Header
                    Text(
                        text = "क्या आप इस साइट को हटाना चाहते हैं?",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // Summary Block
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2129)),
                        border = BorderStroke(1.dp, Color(0xFF2D313E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Site Name
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isHindi) "साइट का नाम" else "Site Name",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = site.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Assigned active workers
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isHindi) "सक्रिय मजदूर" else "Active Workers",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeLaboursOnSite.size}",
                                    color = if (activeLaboursOnSite.isNotEmpty()) Color(0xFFEF4444) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Linked payments
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isHindi) "लिंक्ड भुगतान रिकॉर्ड" else "Linked Payments",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$paymentsLinkedCount",
                                    color = if (paymentsLinkedCount > 0) Color(0xFFEF4444) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hasLinkedRecords) {
                        // Linked Records found! Prevent permanent delete.
                        Text(
                            text = "Linked records exist (workers or payment logs). Permanent delete is disabled to guard operational history.",
                            color = Color(0xFFFBBF24), // Gold warning color
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "इस साइट के पास सक्रिय मजदूर या वित्तीय रिकॉर्ड हैं। डेटा सुरक्षा की दृष्टि से स्थायी डिलीट प्रतिबंधित है, कृपया इसे संग्रहित (Archive) करें।",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )
                    } else {
                        // No linked records. Friendly warning.
                        Text(
                            text = "No linked records. You may permanently delete this site, or safe archive it instead.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action 1: Secondary Cancel
                    OutlinedButton(
                        onClick = { viewModel.closeDeleteSiteDialog() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("cancel_delete_site"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isHindi) "रद्द करें" else "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (hasLinkedRecords) {
                        // Linked records exist: ONLY archive option is available.
                        Button(
                            onClick = { viewModel.archiveSite(site) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                                .testTag("primary_archive_site"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB), // Friendly Blue for Recommended Archive
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (isHindi) "संग्रहित करें" else "Archive Site",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // No linked records: give choice to permanent delete or archive!
                        var showSecondaryChoice by remember { mutableStateOf(false) }

                        if (!showSecondaryChoice) {
                            // Offer clean choice buttons
                            Button(
                                onClick = { showSecondaryChoice = true },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(44.dp)
                                    .testTag("permanent_delete_site_trigger"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626), // Destructive Red
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isHindi) "स्थायी हटाएं" else "Delete Site",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Second step double confirmation check! To block accidental click
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.5f)) {
                                Button(
                                    onClick = { viewModel.permanentDeleteSite(site) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("primary_delete_site_permanent"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB91C1C), // Deep crimson
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (isHindi) "हाँ, स्थायी हटाएँ" else "Purge Site",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}@Composable
fun DailyAttendanceConfirmationDialog(
    viewModel: LabourViewModel,
    isHindi: Boolean
) {
    if (viewModel.showAttendanceDialog) {
        val list = viewModel.attendanceListState
        val accent = Color(0xFF2563EB) // Branding Royal Blue
        val textPrimary = Color(0xFF0F172A) // Slate-900
        val textSecondary = Color(0xFF475569) // Slate-600
        val borderSlate = Color(0xFFE2E8F0) // Slate-200
        val context = androidx.compose.ui.platform.LocalContext.current
        
        AlertDialog(
            onDismissRequest = { viewModel.showAttendanceDialog = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("daily_attendance_confirmation_dialog"),
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            ),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Attendance",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isHindi) "मजदूर हाजिरी व वेतन रजिस्टर" else "Attendance & Daily Wage Register",
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            val isAnyFinalized = list.any { it.isFinalized }
                            Text(
                                text = if (isAnyFinalized) {
                                    if (isHindi) "🔒 आज की मजदूरी लॉक्ड है (locked)" else "🔒 Finalized & Locked (Closed for Date)"
                                } else {
                                    if (isHindi) "📝 ड्राफ्ट (स्वचालित रूप से सहेजा गया)" else "📝 Live Draft (Changes Autosaved)"
                                },
                                color = if (isAnyFinalized) Color(0xFF1E3A8A) else Color(0xFFB45309),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interactive Date Browser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, borderSlate, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Day Arrow
                        IconButton(
                            onClick = {
                                try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val dateObj = sdf.parse(viewModel.attendanceSelectedDate) ?: Date()
                                    val cal = java.util.Calendar.getInstance()
                                    cal.time = dateObj
                                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                                    viewModel.attendanceSelectedDate = sdf.format(cal.time)
                                    viewModel.loadAttendanceForSelectedDate()
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous Day",
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Selected Date string (Interactive click)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val cal = java.util.Calendar.getInstance()
                                    try {
                                        val parts = viewModel.attendanceSelectedDate.split("-")
                                        if (parts.size == 3) {
                                            cal.set(java.util.Calendar.YEAR, parts[0].toInt())
                                            cal.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                                            cal.set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                                        }
                                    } catch (e: Exception) {}

                                    val dpd = android.app.DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
                                            viewModel.attendanceSelectedDate = formatted
                                            viewModel.loadAttendanceForSelectedDate()
                                        },
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH),
                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    )
                                    // Lock future dates to represent real history logic
                                    dpd.datePicker.maxDate = System.currentTimeMillis()
                                    dpd.show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val displayDate = try {
                                val originalDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(viewModel.attendanceSelectedDate)
                                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(originalDate ?: Date())
                            } catch (e: Exception) {
                                viewModel.attendanceSelectedDate
                            }
                            Text(
                                text = displayDate,
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Next Day Arrow
                        val isTodaySelected = viewModel.attendanceSelectedDate == LabourViewModel.getCurrentDateString()
                        IconButton(
                            onClick = {
                                if (!isTodaySelected) {
                                    try {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val dateObj = sdf.parse(viewModel.attendanceSelectedDate) ?: Date()
                                        val cal = java.util.Calendar.getInstance()
                                        cal.time = dateObj
                                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                        viewModel.attendanceSelectedDate = sdf.format(cal.time)
                                        viewModel.loadAttendanceForSelectedDate()
                                    } catch (e: Exception) {}
                                }
                            },
                            enabled = !isTodaySelected,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = "Next Day",
                                tint = if (isTodaySelected) Color.LightGray else accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isLocked = list.any { it.isFinalized }
                    
                    if (!isLocked) {
                        // Quick confirmation tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.markAllPresent() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("attendance_mark_all_present"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF15803D)
                                )
                            ) {
                                Text(
                                    text = if (isHindi) "सभी उपस्थित" else "All Present",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.markAllAbsent() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("attendance_mark_all_absent"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFB91C1C)
                                )
                            ) {
                                Text(
                                    text = if (isHindi) "सभी अनुपस्थित" else "All Absent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Divider(color = borderSlate, thickness = 1.dp)
                    }

                    // Scroll list of auto-wage active workers
                    Box(modifier = Modifier.heightIn(max = 280.dp)) {
                        if (list.isEmpty()) {
                            Text(
                                text = if (isHindi) "कोई भी मजदूर सक्रिय नहीं है जो स्वचालित वेतन दोहराने के योग्य हो।" else "No active workers have auto repeating daily wage enabled currently.",
                                color = textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(list) { item ->
                                    val status = item.status
                                    val isFinalized = item.isFinalized
                                    
                                    val containerColor = when (status) {
                                        "Present" -> Color(0xFFF0FDF4) // Green
                                        "Half Day" -> Color(0xFFFFFBEB) // Amber/Yellow
                                        else -> Color(0xFFFEF2F2) // Red
                                    }
                                    val borderColor = when (status) {
                                        "Present" -> Color(0xFFBBF7D0)
                                        "Half Day" -> Color(0xFFFDE68A)
                                        else -> Color(0xFFFCA5A5)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("attendance_item_${item.labourId}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = containerColor
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.labourName,
                                                        color = textPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.LocationOn,
                                                            contentDescription = null,
                                                            tint = accent,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "${item.siteName} • ₹${item.dailyWage.toInt()}",
                                                            color = textSecondary,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                // State display badge
                                                val badgeText = when (status) {
                                                    "Present" -> if (isHindi) "प्रेजेंट" else "Present"
                                                    "Half Day" -> if (isHindi) "हाफ डे" else "Half Day"
                                                    else -> if (isHindi) "अनुपस्थित" else "Absent"
                                                }
                                                val badgeColor = when (status) {
                                                    "Present" -> Color(0xFF15803D)
                                                    "Half Day" -> Color(0xFFB45309)
                                                    else -> Color(0xFFB91C1C)
                                                }
                                                val badgeBg = when (status) {
                                                    "Present" -> Color(0xFFDCFCE7)
                                                    "Half Day" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFFEE2E2)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(badgeBg)
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        color = badgeColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            if (!isFinalized) {
                                                // Segmented style fast toggle options for Present / Half Day / Absent
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    listOf("Present", "Half Day", "Absent").forEach { opt ->
                                                        val isSelected = status == opt
                                                        val optBg = when (opt) {
                                                            "Present" -> if (isSelected) Color(0xFF22C55E) else Color.White
                                                            "Half Day" -> if (isSelected) Color(0xFFF59E0B) else Color.White
                                                            else -> if (isSelected) Color(0xFFEF4444) else Color.White
                                                        }
                                                        val optTxt = if (isSelected) Color.White else textSecondary
                                                        val label = when (opt) {
                                                            "Present" -> if (isHindi) "प्रेजेंट" else "Present"
                                                            "Half Day" -> if (isHindi) "हाफ डे" else "Half Day"
                                                            else -> if (isHindi) "एब्सेंट" else "Absent"
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(optBg)
                                                                .border(1.dp, if (isSelected) optBg else borderSlate, RoundedCornerShape(6.dp))
                                                                .clickable { viewModel.setAttendanceStatus(item.labourId, opt) }
                                                                .padding(vertical = 5.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                color = optTxt,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = borderSlate, thickness = 1.dp)

                    // Auto-Finalize evening runner toggle switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, borderSlate, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "शाम 8:00 बजे स्वचालित वेतन (Auto-Finalize)" else "Auto-Finalize wages at 8:00 PM",
                                    color = textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "काम समाप्त होने के बाद स्वतः हाजिरी दर्ज करें" else "Auto checks in draft states when shift ends",
                                    color = textSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.isAutoFinalizeEnabled,
                            onCheckedChange = { viewModel.isAutoFinalizeEnabled = it },
                            modifier = Modifier.testTag("attendance_auto_finalize_switch")
                        )
                    }
                }
            },
            confirmButton = {
                val isLocked = list.any { it.isFinalized }
                
                Button(
                    onClick = { viewModel.generateDailyWagesAndAttendance() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("attendance_generate_wage_trigger"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocked) Color.Gray else Color(0xFF16803D),
                        contentColor = Color.White
                    ),
                    enabled = !viewModel.isGeneratingWages && !isLocked && list.isNotEmpty()
                ) {
                    if (viewModel.isGeneratingWages) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        if (isLocked) {
                            Text(
                                text = if (isHindi) "🔒 आज की उपस्थिति दर्ज हो चुकी है" else "🔒 Wages Already Finalized",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val presentCount = list.count { it.isPresent }
                            Text(
                                text = if (isHindi) "आज की मजदूरी दर्ज करें ($presentCount उपस्थित)" else "Finalize & Pay ($presentCount Present)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.showAttendanceDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("attendance_cancel_trigger"),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderSlate)
                ) {
                    Text(
                        text = if (isHindi) "बंद करें" else "Close / Dismiss Window",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
