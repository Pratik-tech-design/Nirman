package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.Attendance
import com.example.data.models.Payment
import com.example.ui.viewmodel.LabourViewModel
import java.text.SimpleDateFormat
import java.util.*

data class StatementTransaction(
    val date: String,
    val description: String,
    val debit: Double,
    val credit: Double,
    val type: String,
    val originalRecord: Any,
    val runningBalance: Double = 0.0
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LabourDetailScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val labour = viewModel.selectedLabourForDetail ?: return
    val context = LocalContext.current
    val isHindi = viewModel.isHindi

    // Collect all data reactively
    val attendanceRecords by viewModel.attendance.collectAsState()
    val paymentList by viewModel.payments.collectAsState()
    val siteList by viewModel.sites.collectAsState()

    // Filter relevant records for this specific labourer using the precision-tuned global filter system!
    val relativeAttendance = viewModel.getFilteredAttendances(labour.id, attendanceRecords)
    val relativePayments = viewModel.getFilteredPayments(labour.id, paymentList)

    // Aggregate statistics
    val presentRecordCount = relativeAttendance.count { it.status == "Present" }
    val halfDayRecordCount = relativeAttendance.count { it.status == "Half Day" }
    val absentRecordCount = relativeAttendance.count { it.status == "Absent" }
    val totalWorkingDays = presentRecordCount + (halfDayRecordCount * 0.5)

    // Financial arithmetic
    val totalWagesEarned = relativeAttendance.sumOf { record ->
        when (record.status) {
            "Present" -> record.dailyRate
            "Half Day" -> record.dailyRate * 0.5
            else -> 0.0
        }
    }

    // Advance payment aggregate
    val totalAdvanceTaken = relativePayments
        .filter { it.paymentType in listOf("Weekly Advance", "Emergency Advance", "Food Advance", "Petrol Advance", "Petrol Expense", "Tool Advance") || it.paymentType.contains("Advance", ignoreCase = true) }
        .sumOf { it.amount }

    // Extra Expense aggregate (not part of wages, but reported separately)
    val totalExtraExpense = relativePayments
        .filter { it.paymentType == "Extra Expense" }
        .sumOf { it.amount }

    val totalBonus = relativePayments
        .filter { it.paymentType == "Bonus" }
        .sumOf { it.amount }

    val totalDeduction = relativePayments
        .filter { it.paymentType == "Deduction" }
        .sumOf { it.amount }

    // Net salary calculations
    val pendingPayable = (totalWagesEarned + totalBonus) - (totalAdvanceTaken + totalDeduction)

    // Interactive Dialog Trigger variables
    var showAddAdvanceDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    
    var activeTabIdx by remember { mutableStateOf(0) } // 0 = Attendance, 1 = Payment, 2 = Advance, 3 = Extra Expense, 4 = Statement

    // Compile combined bank passbook ledger statement transactions!
    val statementTransactions = remember(relativeAttendance, relativePayments, viewModel.filterSortingOption) {
        val list = mutableListOf<StatementTransaction>()
        
        // 1. Add all attendance earned wages (Credits)
        relativeAttendance.forEach { att ->
            val earned = when (att.status) {
                "Present" -> att.dailyRate
                "Half Day" -> att.dailyRate * 0.5
                else -> 0.0
            }
            if (earned > 0.0) {
                list.add(
                    StatementTransaction(
                        date = att.date,
                        description = if (isHindi) "हाजिरी: ${if(att.status == "Present") "पूरा दिन" else "आधा दिन"}" else "Attendance: ${if (att.status == "Present") "Full Day" else "Half Day"}",
                        debit = 0.0,
                        credit = earned,
                        type = "Attendance",
                        originalRecord = att
                    )
                )
            }
        }
        
        // 2. Add all payments, advances, extras, bonuses, deductions
        relativePayments.forEach { pay ->
            when (pay.paymentType) {
                "Daily Wage (Kharchi)", "Salary", "Payment" -> {
                    list.add(
                        StatementTransaction(
                            date = pay.date,
                            description = if (pay.remarks.isNotBlank()) pay.remarks else (if (isHindi) "मजदूरी भुगतान (${pay.paymentMode})" else "Wage Payment (${pay.paymentMode})"),
                            debit = pay.amount,
                            credit = 0.0,
                            type = "Payment",
                            originalRecord = pay
                        )
                    )
                }
                "Weekly Advance", "Emergency Advance", "Food Advance", "Petrol Advance", "Tool Advance", "Manual Advance", "Advance" -> {
                    list.add(
                        StatementTransaction(
                            date = pay.date,
                            description = if (pay.remarks.isNotBlank()) pay.remarks else (if (isHindi) "एडवांस (${pay.paymentType})" else "Advance (${pay.paymentType})"),
                            debit = pay.amount,
                            credit = 0.0,
                            type = "Advance",
                            originalRecord = pay
                        )
                    )
                }
                "Extra Expense" -> {
                    list.add(
                        StatementTransaction(
                            date = pay.date,
                            description = if (pay.remarks.isNotBlank()) pay.remarks else (if (isHindi) "अतिरिक्त खर्च" else "Extra Expense"),
                            debit = pay.amount,
                            credit = 0.0,
                            type = "Extra Expense",
                            originalRecord = pay
                        )
                    )
                }
                "Bonus" -> {
                    list.add(
                        StatementTransaction(
                            date = pay.date,
                            description = if (pay.remarks.isNotBlank()) pay.remarks else (if (isHindi) "बोनस" else "Bonus"),
                            debit = 0.0,
                            credit = pay.amount,
                            type = "Bonus",
                            originalRecord = pay
                        )
                    )
                }
                "Deduction" -> {
                    list.add(
                        StatementTransaction(
                            date = pay.date,
                            description = if (pay.remarks.isNotBlank()) pay.remarks else (if (isHindi) "कटौती/जुर्माना" else "Deduction"),
                            debit = pay.amount,
                            credit = 0.0,
                            type = "Deduction",
                            originalRecord = pay
                        )
                    )
                }
                else -> {
                    list.add(
                        StatementTransaction(
                            date = pay.date,
                            description = if (pay.remarks.isNotBlank()) pay.remarks else pay.paymentType,
                            debit = pay.amount,
                            credit = 0.0,
                            type = "Payment",
                            originalRecord = pay
                        )
                    )
                }
            }
        }
        
        // 3. Sort chronologically (Oldest First) to calculate correct sequential running balance!
        val chronList = list.sortedBy { it.date }
        var runningBal = 0.0
        val finalizedList = chronList.map { tx ->
            runningBal += (tx.credit - tx.debit)
            tx.copy(runningBalance = runningBal)
        }
        
        // 4. Return list sorted according to active sorting option!
        if (viewModel.filterSortingOption == "Date (Oldest First)") {
            finalizedList
        } else {
            finalizedList.sortedByDescending { it.date }
        }
    }

    // Workforce Pro Design System Color Tokens - Ultra Premium Clean Light Theme
    val darkSurface = Color(0xFFF8F9FC) // Background #F8F9FC
    val cardBg = Color(0xFFFFFFFF) // Surface #FFFFFF
    val accentPurple = Color(0xFF0841E2) // Primary Blue #0841E2
    val borderSlate = Color(0xFFE8E8E8) // Elegant subtle border #E8E8E8
    
    // Status colors
    val colorKharchi = Color(0xFF0841E2) // Blue
    val colorAdvance = Color(0xFFF59E0B) // Orange
    val colorExpense = Color(0xFF22C55E) // Green
    val colorBonus = Color(0xFFA855F7) // Purple
    val colorDeduction = Color(0xFFEF4444) // Red
    val colorAbsent = Color(0xFF616161) // Slate Gray

    Surface(
        modifier = modifier.fillMaxSize().testTag("labour_detail_screen"),
        color = darkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ==========================================
            // TOP TOOLBAR & HEADER BLOCK
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.closeLabourDetail() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(1.dp, Color(0xFFE8E8E8), CircleShape)
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1A1A1A)
                    )
                }

                Text(
                    text = if (isHindi) "कार्यालय खाताबुक" else "Worker Ledger Register",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                IconButton(
                    onClick = { viewModel.exportToExcel(context, filterLabourId = labour.id) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorExpense.copy(alpha = 0.1f))
                        .border(1.dp, colorExpense.copy(alpha = 0.5f), CircleShape)
                        .testTag("detail_export_excel")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Export Excel",
                        tint = colorExpense
                    )
                }
            }

            // ==========================================
            // WORKER HEADER CARD
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderSlate),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Icon placeholder with initials
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(accentPurple.copy(alpha = 0.15f))
                            .border(2.dp, accentPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labour.name.take(2).uppercase(),
                            color = accentPurple,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = labour.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Construction,
                                contentDescription = "Skill",
                                tint = accentPurple.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${labour.skillType} • ",
                                color = Color(0xFF616161),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = "Site",
                                tint = colorKharchi,
                                modifier = Modifier.size(14.dp)
                            )
                            val workersSite = siteList.find { it.id == labour.siteId }
                            Text(
                                text = workersSite?.name ?: (if (isHindi) "अनिर्धारित" else "No Site"),
                                color = Color(0xFF616161),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status & phone
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = "₹${labour.dailyWage.toInt()}/${if (isHindi) "दिन" else "day"}",
                                color = accentPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (labour.status == "Active") colorExpense.copy(alpha = 0.15f) else colorAbsent.copy(alpha = 0.15f))
                                    .border(0.5.dp, if (labour.status == "Active") colorExpense else colorAbsent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                             ) {
                                Text(
                                    text = labour.status.uppercase(),
                                    color = if (labour.status == "Active") colorExpense else colorAbsent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4 SUMMARY CARDS GRID (Workforce Pro Design System)
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = if (isHindi) "हाजिरी (दिन)" else "Present Days",
                        value = "${totalWorkingDays.toString().replace(".0", "")} / ${relativeAttendance.size}",
                        subtitle = if (isHindi) "${presentRecordCount} पूरा | ${halfDayRecordCount} आधा " else "${presentRecordCount} Full | ${halfDayRecordCount} Half",
                        color = Color(0xFF22C55E), // Green
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isHindi) "कुल अर्जित मजदूरी" else "Wages Earned",
                        value = "₹${totalWagesEarned.toInt()}",
                        subtitle = if (isHindi) "दैनिक हाजिरी गणना" else "Gross wages sum",
                        color = Color(0xFF0841E2), // Primary Blue
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = if (isHindi) "एडवांस / खर्च" else "Advances",
                        value = "₹${totalAdvanceTaken.toInt()}",
                        subtitle = if (isHindi) "अग्रिम भुगतान" else "Salary advances",
                        color = Color(0xFFF59E0B), // Orange
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isHindi) "शेष देय वेतन" else "Pending Payable",
                        value = "₹${pendingPayable.toInt()}",
                        subtitle = if (isHindi) "कैलकुलेटर आधार" else "Net wage balance",
                        color = if (pendingPayable >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ==========================================
            // UNIVERSAL FILTER CONTROLS PANEL
            // ==========================================
            var showAdvancedFilters by remember { mutableStateOf(false) }
            val activeFilterCount = remember(
                viewModel.filterDateMode, viewModel.filterAttendanceStatus,
                viewModel.filterPaymentStatus, viewModel.filterAdvanceType,
                viewModel.filterExtraExpenseCategory, viewModel.filterSiteIdState,
                viewModel.filterAmountRange, viewModel.filterSearchQuery
            ) {
                var count = 0
                if (viewModel.filterDateMode != "All") count++
                if (viewModel.filterAttendanceStatus != "All") count++
                if (viewModel.filterPaymentStatus != "All") count++
                if (viewModel.filterAdvanceType != "All") count++
                if (viewModel.filterExtraExpenseCategory != "All") count++
                if (viewModel.filterSiteIdState != null) count++
                if (viewModel.filterAmountRange != "All") count++
                if (viewModel.filterSearchQuery.isNotBlank()) count++
                count
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filters",
                        tint = if (activeFilterCount > 0) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isHindi) "वित्तीय फिल्टर" else "Financial Filter Utility",
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeFilterCount > 0) {
                        Text(
                            text = if (isHindi) "रीसेट" else "Reset",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.resetAllFilters() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { showAdvancedFilters = !showAdvancedFilters },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showAdvancedFilters) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Toggle Filters",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAdvancedFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, borderSlate),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Search Box
                        OutlinedTextField(
                            value = viewModel.filterSearchQuery,
                            onValueChange = { viewModel.filterSearchQuery = it },
                            placeholder = { Text(text = if (isHindi) "विवरण, राशि या प्रकार खोजें..." else "Search ledger by remarks, amount, type...", color = Color(0xFF64748B), fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 13.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("filter_search_input"),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (viewModel.filterSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.filterSearchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentPurple,
                                unfocusedBorderColor = borderSlate,
                                focusedContainerColor = darkSurface.copy(alpha = 0.5f),
                                unfocusedContainerColor = darkSurface.copy(alpha = 0.3f)
                            )
                        )

                        // 2. Reporting Period / Quick Dates
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isHindi) "समयावधि (Reporting Period):" else "Select Reporting Period:",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val dateOptions = listOf(
                                    "All" to "सभी (All)",
                                    "Today" to "आज (Today)",
                                    "Yesterday" to "कल (Yest.)",
                                    "This Week" to "इस सप्ताह",
                                    "Last Week" to "पिछले सप्ताह",
                                    "This Month" to "इस माह (Month)",
                                    "Last Month" to "पिछला माह",
                                    "Last 3 Months" to "3 माह (3M)",
                                    "Last 6 Months" to "6 माह (6M)",
                                    "This Financial Year" to "वित्तीय वर्ष (FY)",
                                    "MonthNavigator" to "माह चयकर्ता (Nav)",
                                    "Custom" to "कस्टम सीमा"
                                )
                                dateOptions.forEach { (mode, label) ->
                                    val isSel = viewModel.filterDateMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) accentPurple.copy(alpha = 0.25f) else darkSurface)
                                            .border(1.dp, if (isSel) accentPurple else borderSlate, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.filterDateMode = mode }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = if (isHindi) label else mode, color = if (isSel) accentPurple else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Month Navigator details
                        if (viewModel.filterDateMode == "MonthNavigator") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(darkSurface, RoundedCornerShape(8.dp))
                                    .border(1.dp, borderSlate, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        if (viewModel.filterNavMonth == 1) {
                                            viewModel.filterNavMonth = 12
                                            viewModel.filterNavYear--
                                        } else {
                                            viewModel.filterNavMonth--
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Prev Month", tint = Color(0xFF0F172A))
                                }

                                val monthNamesEn = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                                val monthNamesHi = listOf("", "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
                                val labelMonth = if (isHindi) "${monthNamesHi[viewModel.filterNavMonth]} ${viewModel.filterNavYear}" else "${monthNamesEn[viewModel.filterNavMonth]} ${viewModel.filterNavYear}"
                                
                                Text(
                                    text = labelMonth,
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        if (viewModel.filterNavMonth == 12) {
                                            viewModel.filterNavMonth = 1
                                            viewModel.filterNavYear++
                                        } else {
                                            viewModel.filterNavMonth++
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next Month", tint = Color(0xFF0F172A))
                                }
                            }
                        }

                        // Custom Date pickers entry
                        if (viewModel.filterDateMode == "Custom") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = viewModel.filterStartDate,
                                    onValueChange = { viewModel.filterStartDate = it },
                                    label = { Text(text = if (isHindi) "शुरुआत (YYYY-MM-DD)" else "From (YYYY-MM-DD)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentPurple,
                                        unfocusedBorderColor = borderSlate
                                    )
                                )
                                OutlinedTextField(
                                    value = viewModel.filterEndDate,
                                    onValueChange = { viewModel.filterEndDate = it },
                                    label = { Text(text = if (isHindi) "अंत (YYYY-MM-DD)" else "To (YYYY-MM-DD)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentPurple,
                                        unfocusedBorderColor = borderSlate
                                    )
                                )
                            }
                        }

                        // 3. Attendance Status selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isHindi) "उपस्थिति फ़िल्टर (Attendance status)" else "Attendance Category Filter:",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val attFilters = listOf(
                                    "All" to "सभी (All)",
                                    "Full Day" to "पूरा दिन (Full Day)",
                                    "Half Day" to "आधा दिन (Half Day)",
                                    "Absent" to "अनुपस्थित (Absent)",
                                    "Present" to "उपस्थित (Present)",
                                    "Auto-Generated Attendance" to "स्वचालित (Auto)",
                                    "Manually Added Attendance" to "मैनुअल (Manual)"
                                )
                                attFilters.forEach { (mode, label) ->
                                    val isSel = viewModel.filterAttendanceStatus == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) accentPurple.copy(alpha = 0.25f) else darkSurface)
                                            .border(1.dp, if (isSel) accentPurple else borderSlate, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.filterAttendanceStatus = mode }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = if (isHindi) label else mode, color = if (isSel) accentPurple else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // 4. Payment Categorizations
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isHindi) "भुगतान फ़िल्टर (Wages Paid detail)" else "Payment Status Filter:",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val payFilters = listOf(
                                    "All" to "सभी (All)",
                                    "Paid" to "भुगतान किया (Paid)",
                                    "Pending" to "लंबित (Pending)",
                                    "Partially Paid" to "आंशिक भुगतान",
                                    "Settled" to "तय हुआ (Settled)",
                                    "Unsettled" to "बाकी (Unsettled)"
                                )
                                payFilters.forEach { (mode, label) ->
                                    val isSel = viewModel.filterPaymentStatus == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) colorKharchi.copy(alpha = 0.25f) else darkSurface)
                                            .border(1.dp, if (isSel) colorKharchi else borderSlate, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.filterPaymentStatus = mode }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = if (isHindi) label else mode, color = if (isSel) colorKharchi else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // 5. Transaction values ranges
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isHindi) "राशि फ़िल्टर (Amount limits)" else "Transaction Value Range:",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val amtFilters = listOf(
                                    "All" to "सभी रकम (All)",
                                    "0-500" to "₹0 – ₹500",
                                    "500-1000" to "₹500 – ₹1000",
                                    "1000-5000" to "₹1000 – ₹5000",
                                    "5000+" to "₹5000+",
                                    "Custom" to "कस्टम सीमा"
                                )
                                amtFilters.forEach { (mode, label) ->
                                    val isSel = viewModel.filterAmountRange == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) colorAdvance.copy(alpha = 0.25f) else darkSurface)
                                            .border(1.dp, if (isSel) colorAdvance else borderSlate, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.filterAmountRange = mode }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = label, color = if (isSel) colorAdvance else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        if (viewModel.filterAmountRange == "Custom") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = viewModel.filterAmountMin?.toString() ?: "",
                                    onValueChange = { viewModel.filterAmountMin = it.toDoubleOrNull() },
                                    label = { Text(text = if (isHindi) "न्यूनतम ₹" else "Min ₹", fontSize = 10.sp, color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorAdvance,
                                        unfocusedBorderColor = borderSlate
                                    )
                                )
                                OutlinedTextField(
                                    value = viewModel.filterAmountMax?.toString() ?: "",
                                    onValueChange = { viewModel.filterAmountMax = it.toDoubleOrNull() },
                                    label = { Text(text = if (isHindi) "अधिकतम ₹" else "Max ₹", fontSize = 10.sp, color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorAdvance,
                                        unfocusedBorderColor = borderSlate
                                    )
                                )
                            }
                        }

                        // 6. Sorter Choices
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isHindi) "क्रमबद्ध करें (Sort order)" else "Ledger Sorting Option:",
                                color = Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sortOptions = listOf(
                                    "Date (Newest First)" to "तारीख (नई पहले)",
                                    "Date (Oldest First)" to "तारीख (पुरानी पहले)",
                                    "Highest Amount" to "अधिकतम राशि (Max ₹)",
                                    "Lowest Amount" to "न्यूनतम राशि (Min ₹)"
                                )
                                sortOptions.forEach { (mode, label) ->
                                    val isSel = viewModel.filterSortingOption == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF14B8A6).copy(alpha = 0.25f) else darkSurface)
                                            .border(1.dp, if (isSel) Color(0xFF14B8A6) else borderSlate, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.filterSortingOption = mode }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = if (isHindi) label else mode, color = if (isSel) Color(0xFF0F766E) else Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SEGMENTED TAB BUTTONS (BOTTOM NAVIGATION TABS)
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE8E8E8))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                ) {
                    val tabsList = listOf(
                        Triple(0, if (isHindi) "हाजिरी" else "Attendance", Icons.Rounded.EventAvailable),
                        Triple(1, if (isHindi) "भुगतान" else "Payment", Icons.Rounded.Calculate),
                        Triple(2, if (isHindi) "एडवांस" else "Advance", Icons.Rounded.Payments),
                        Triple(3, if (isHindi) "अतिरिक्त" else "Expense", Icons.Rounded.Category),
                        Triple(4, if (isHindi) "लेजर" else "Ledger", Icons.Rounded.ReceiptLong)
                    )

                    tabsList.forEach { (idx, label, icon) ->
                        val isSelected = activeTabIdx == idx

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF0841E2) else Color.White)
                                .clickable { activeTabIdx = idx }
                                .padding(vertical = 4.dp)
                                .testTag("detail_tab_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else Color(0xFF616161),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFF616161),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // SCROLLABLE SUBSECTIONS (ACTIVE TAB CONTAINER)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                when (activeTabIdx) {
                    0 -> AttendanceSubSection(
                        viewModel = viewModel,
                        labourId = labour.id,
                        records = relativeAttendance,
                        isHindi = isHindi,
                        cardBg = cardBg,
                        borderSlate = borderSlate,
                        colorPresent = colorExpense,
                        colorHalf = colorAdvance,
                        colorAbsent = colorAbsent
                    )
                    1 -> PaymentSubSection(
                        viewModel = viewModel,
                        payments = relativePayments,
                        totalEarned = totalWagesEarned,
                        totalAdvance = totalAdvanceTaken,
                        totalBonus = totalBonus,
                        totalDeduction = totalDeduction,
                        totalExtraExpense = totalExtraExpense,
                        pendingPayable = pendingPayable,
                        isHindi = isHindi,
                        cardBg = cardBg,
                        borderSlate = borderSlate,
                        colorExpense = colorExpense,
                        colorAdvance = colorAdvance,
                        accentPurple = accentPurple,
                        onAddPayment = { showAddPaymentDialog = true }
                    )
                    2 -> AdvanceSubSection(
                        payments = relativePayments,
                        totalAdvance = totalAdvanceTaken,
                        isHindi = isHindi,
                        cardBg = cardBg,
                        borderSlate = borderSlate,
                        colorAdvance = colorAdvance,
                        colorAbsent = colorAbsent,
                        onAddAdvance = { showAddAdvanceDialog = true },
                        onDeletePayment = { pay -> viewModel.deletePaymentDirectly(pay) }
                    )
                    3 -> ExtraExpenseSubSection(
                        payments = relativePayments,
                        totalExpense = totalExtraExpense,
                        isHindi = isHindi,
                        cardBg = cardBg,
                        borderSlate = borderSlate,
                        colorExpense = colorExpense,
                        colorAbsent = colorAbsent,
                        onAddExpense = { showAddExpenseDialog = true },
                        onDeletePayment = { pay -> viewModel.deletePaymentDirectly(pay) }
                    )
                    4 -> StatementSubSection(
                        statementList = statementTransactions,
                        isHindi = isHindi,
                        cardBg = cardBg,
                        borderSlate = borderSlate,
                        colorDebit = colorDeduction,
                        colorCredit = colorExpense
                    )
                }
            }
        }
    }
}

    // ==========================================
    // DIALOGS FOR LEDGER ADDITIONS
    // ==========================================

    if (showAddAdvanceDialog) {
        AddAdvanceLedgerDialog(
            labourId = labour.id,
            labourName = labour.name,
            siteId = labour.siteId ?: 0,
            userId = viewModel.currentUser.value?.uid ?: "default_contractor_uid",
            isHindi = isHindi,
            onDismiss = { showAddAdvanceDialog = false },
            onSubmit = { p ->
                viewModel.insertPaymentDirectly(p) {
                    showAddAdvanceDialog = false
                }
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseLedgerDialog(
            labourId = labour.id,
            labourName = labour.name,
            siteId = labour.siteId ?: 0,
            userId = viewModel.currentUser.value?.uid ?: "default_contractor_uid",
            isHindi = isHindi,
            onDismiss = { showAddExpenseDialog = false },
            onSubmit = { p ->
                viewModel.insertPaymentDirectly(p) {
                    showAddExpenseDialog = false
                }
            }
        )
    }

    if (showAddPaymentDialog) {
        AddDirectPaymentDialog(
            labourId = labour.id,
            labourName = labour.name,
            siteId = labour.siteId ?: 0,
            userId = viewModel.currentUser.value?.uid ?: "default_contractor_uid",
            isHindi = isHindi,
            onDismiss = { showAddPaymentDialog = false },
            onSubmit = { p ->
                viewModel.insertPaymentDirectly(p) {
                    showAddPaymentDialog = false
                }
            }
        )
    }
}

// ==========================================
// REUSABLE HELPER SUB-COMPONENTS
// ==========================================

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(105.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color(0xFF475569),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = Color(0xFF0F172A),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// 5. STATEMENT SUBSECTION
// ==========================================
@Composable
fun StatementSubSection(
    statementList: List<StatementTransaction>,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorDebit: Color, // Red (-)
    colorCredit: Color // Green (+)
) {
    if (statementList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = "No Transactions",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = if (isHindi) "इस अवधि के लिए कोई लेन-देन नहीं मिला" else "No transactions found for this period",
                    color = Color(0xFF1E293B),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isHindi) "सलाह: सक्रिय फ़िल्टर बदलें या रीसेट करें" else "Tip: Modify or reset your active filters above",
                    color = Color(0xFF475569),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(1.dp, borderSlate, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "दिनांक" else "Date",
                    color = Color(0xFF334155),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1.3f)
                )
                Text(
                    text = if (isHindi) "विवरण (Remarks)" else "Description",
                    color = Color(0xFF334155),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(2.5f)
                )
                Text(
                    text = if (isHindi) "डेबिट (-)" else "Debit (-)",
                    color = Color(0xFF334155),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = if (isHindi) "क्रेडिट (+)" else "Credit (+)",
                    color = Color(0xFF334155),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = if (isHindi) "शेष राशि" else "Balance",
                    color = Color(0xFF334155),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.8f)
                )
            }

            // Column statement register entries
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderSlate, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(cardBg),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                statementList.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date column
                        Text(
                            text = tx.date,
                            color = Color(0xFF334155),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.3f)
                        )

                        // Description column
                        Column(modifier = Modifier.weight(2.5f)) {
                            Text(
                                text = tx.description,
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Tag
                            Text(
                                text = tx.type.uppercase(),
                                color = when (tx.type) {
                                    "Attendance" -> Color(0xFF0369A1)
                                    "Advance" -> colorDebit
                                    "Payment" -> colorDebit
                                    "Bonus" -> Color(0xFF7E22CE)
                                    "Deduction" -> Color.Red
                                    else -> Color(0xFF57606A)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Debit column
                        Text(
                            text = if (tx.debit > 0.0) "-₹${tx.debit.toInt()}" else "—",
                            color = if (tx.debit > 0.0) colorDebit else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.5f)
                        )

                        // Credit column
                        Text(
                            text = if (tx.credit > 0.0) "+₹${tx.credit.toInt()}" else "—",
                            color = if (tx.credit > 0.0) colorCredit else Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.5f)
                        )

                        // Running Balance column
                        val sign = if (tx.runningBalance >= 0) "+" else "-"
                        val balColor = if (tx.runningBalance >= 0) colorCredit else colorDebit
                        Text(
                            text = "₹${Math.abs(tx.runningBalance).toInt()}",
                            color = balColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.8f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. ATTENDANCE SUBSECTION
// ==========================================
@Composable
fun AttendanceSubSection(
    viewModel: LabourViewModel,
    labourId: Int,
    records: List<Attendance>,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorPresent: Color,
    colorHalf: Color,
    colorAbsent: Color
) {
    val context = LocalContext.current
    val sites by viewModel.sites.collectAsState()
    val labour = viewModel.labours.value.find { it.id == labourId }

    // Navigation and Calendar State
    var calendarYear by remember { mutableStateOf(2026) }
    var calendarMonth by remember { mutableStateOf(5) } // Default May 2026

    // Bulk Entry states
    var isBulkModeEnabled by remember { mutableStateOf(false) }
    var bulkSelectedDates by remember { mutableStateOf(setOf<String>()) }

    // Attendance specific filters for history list below
    var currentFilter by remember { mutableStateOf("All") } // All, Weekly, Monthly
    val filteredRecords = remember(records, currentFilter) {
        val now = System.currentTimeMillis()
        when (currentFilter) {
            "Weekly" -> records.filter {
                val recordTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)?.time ?: 0L
                (now - recordTime) <= (7 * 24 * 60 * 60 * 1000L)
            }
            "Monthly" -> records.filter {
                val recordTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)?.time ?: 0L
                (now - recordTime) <= (30 * 24 * 60 * 60 * 1000L)
            }
            else -> records
        }
    }

    // Dynamic month labels
    val monthNamesEng = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val monthNamesHin = listOf("", "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
    val monthLabel = if (isHindi) "${monthNamesHin[calendarMonth]} $calendarYear" else "${monthNamesEng[calendarMonth]} $calendarYear"

    // Days allocation calculations for a clean grid
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, calendarYear)
        set(Calendar.MONTH, calendarMonth - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ..., 7=Saturday
    val paddingSlots = firstDayOfWeek - 1
    val gridElements = List(paddingSlots) { null } + (1..maxDays).toList()
    val daysInRows = gridElements.chunked(7)

    // Baseline stats calculations for this month
    val monthPrefix = String.format(Locale.US, "%04d-%02d", calendarYear, calendarMonth)
    val monthRecords = records.filter { it.date.startsWith(monthPrefix) }
    val pDays = monthRecords.count { it.status == "Present" }
    val hDays = monthRecords.count { it.status == "Half Day" }
    val aDays = monthRecords.count { it.status == "Absent" }

    // Detect missed attendance registers up to "Today"
    val todayFormated = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val missingDates = (1..maxDays).map { d ->
        String.format(Locale.US, "%04d-%02d-%02d", calendarYear, calendarMonth, d)
    }.filter { fDate ->
        fDate <= todayFormated && records.none { it.date == fDate }
    }
    val mDays = missingDates.size

    // Overall attendance percentage calculation
    val pr = filteredRecords.count { it.status == "Present" }
    val hd = filteredRecords.count { it.status == "Half Day" }
    val ab = filteredRecords.count { it.status == "Absent" }
    val percent = if (filteredRecords.isNotEmpty()) {
        ((pr + hd * 0.5) / filteredRecords.size * 100).toInt()
    } else 0

    SafeScrollColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Entry Option & CTA Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "तात्कालिक हाजिरी विकल्प" else "Quick Entry Options",
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        isBulkModeEnabled = !isBulkModeEnabled
                        if (!isBulkModeEnabled) {
                            bulkSelectedDates = emptySet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBulkModeEnabled) Color(0xFFEF4444) else Color(0xFF8B5CF6)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_attendance_button")
                ) {
                    Icon(
                        imageVector = if (isBulkModeEnabled) Icons.Rounded.Close else Icons.Rounded.Add,
                        contentDescription = "Add Attendance",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBulkModeEnabled) {
                            if (isHindi) "रद्द करें" else "Cancel Bulk"
                        } else {
                            if (isHindi) "हाजिरी जोड़ें" else "Add Attendance"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Stats grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderSlate)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isHindi) "उपस्थिति प्रतिशत (Attendance)" else "Attendance Rate",
                            color = Color(0xFF475569),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "$percent%",
                                color = if (percent >= 75) colorPresent else if (percent >= 50) colorHalf else colorAbsent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = if (percent >= 75) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = if (percent >= 75) colorPresent else colorAbsent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusIndicatorBadge("P", "$pr", colorPresent)
                        StatusIndicatorBadge("H", "$hd", colorHalf)
                        StatusIndicatorBadge("A", "$ab", colorAbsent)
                    }
                }
            }
        }

        // Missing Attendance Warning Banner
        if (mDays > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "$mDays दिनों की हाजिरी दर्ज नहीं की गई" else "$mDays attendance records missing",
                                color = Color(0xFF991B1B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHindi) "कृपया कैलेंडर पर क्लिक करके हाजिरी जोड़ें।" else "Please tap missing dates on the calendar to catch up.",
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Calendar Heatmap Card with Month Navigation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderSlate)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with Navigate arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (calendarMonth == 1) {
                                    calendarMonth = 12
                                    calendarYear -= 1
                                } else {
                                    calendarMonth -= 1
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = Color(0xFF0F172A)
                            )
                        }

                        Text(
                            text = monthLabel,
                            color = Color(0xFF0F172A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )

                        IconButton(
                            onClick = {
                                if (calendarMonth == 12) {
                                    calendarMonth = 1
                                    calendarYear += 1
                                } else {
                                    calendarMonth += 1
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Next Month",
                                tint = Color(0xFF0F172A)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    // Days of week short labels
                    val dayOfWeekLabels = if (isHindi) {
                        listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
                    } else {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayOfWeekLabels.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF334155),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Calendar Grid Rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        daysInRows.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                week.forEach { day ->
                                    if (day == null) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val fDate = String.format(Locale.US, "%04d-%02d-%02d", calendarYear, calendarMonth, day)
                                        val matchedDayRecord = records.find { it.date == fDate }
                                        val isSelectedInBulk = bulkSelectedDates.contains(fDate)

                                        val hasNoRecord = matchedDayRecord == null
                                        val isMissingInPast = hasNoRecord && fDate <= todayFormated

                                        val itemBg = when (matchedDayRecord?.status) {
                                            "Present" -> colorPresent
                                            "Half Day" -> colorHalf
                                            "Absent" -> colorAbsent
                                            else -> {
                                                if (isMissingInPast) Color(0xFFEF4444).copy(alpha = 0.08f)
                                                else Color(0xFFF8FAFC)
                                            }
                                        }

                                        val textColor = if (matchedDayRecord != null) {
                                            Color.White
                                        } else if (isMissingInPast) {
                                            Color(0xFF991B1B)
                                        } else {
                                            Color(0xFF475569)
                                        }

                                        val cellBorderColor = if (isSelectedInBulk) {
                                            Color(0xFFC084FC)
                                        } else if (fDate == todayFormated) {
                                            Color(0xFF3B82F6)
                                        } else if (isMissingInPast) {
                                            Color(0xFFEF4444).copy(alpha = 0.5f)
                                        } else {
                                            borderSlate
                                        }

                                        val cellBorderWidth = if (isSelectedInBulk) {
                                            2.dp
                                        } else if (fDate == todayFormated) {
                                            1.5.dp
                                        } else if (isMissingInPast) {
                                            1.dp
                                        } else {
                                            0.5.dp
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .background(if (isSelectedInBulk) Color(0xFF8B5CF6).copy(alpha = 0.35f) else itemBg)
                                                .border(cellBorderWidth, cellBorderColor, CircleShape)
                                                .clickable {
                                                    if (isBulkModeEnabled) {
                                                        bulkSelectedDates = if (bulkSelectedDates.contains(fDate)) {
                                                            bulkSelectedDates - fDate
                                                        } else {
                                                            bulkSelectedDates + fDate
                                                        }
                                                    } else {
                                                        viewModel.attendanceSelectedDate = fDate
                                                        if (matchedDayRecord != null) {
                                                            viewModel.editingAttendanceState = com.example.ui.viewmodel.WorkingAttendanceState(
                                                                labourId = matchedDayRecord.labourId,
                                                                labourName = labour?.name ?: "Labour",
                                                                siteId = matchedDayRecord.siteId,
                                                                siteName = sites.find { it.id == matchedDayRecord.siteId }?.name ?: "",
                                                                dailyWage = matchedDayRecord.dailyRate,
                                                                skillType = labour?.skillType ?: "",
                                                                status = matchedDayRecord.status,
                                                                isFinalized = matchedDayRecord.finalized,
                                                                lastEditedAt = matchedDayRecord.lastEditedAt,
                                                                lastEditedBy = matchedDayRecord.lastEditedBy,
                                                                version = matchedDayRecord.version,
                                                                isEditedAfterFinalization = matchedDayRecord.lastEditedAt != null,
                                                                finalizedAt = matchedDayRecord.finalizedAt ?: matchedDayRecord.createdAt
                                                            )
                                                            viewModel.selectedEditStatus = matchedDayRecord.status
                                                            viewModel.editedWageAdjustment = matchedDayRecord.dailyRate.toInt().toString()
                                                            viewModel.editReasonWord = ""
                                                        } else {
                                                            viewModel.editingAttendanceState = com.example.ui.viewmodel.WorkingAttendanceState(
                                                                labourId = labourId,
                                                                labourName = labour?.name ?: "Labour",
                                                                siteId = labour?.siteId ?: 0,
                                                                siteName = sites.find { it.id == (labour?.siteId ?: 0) }?.name ?: "",
                                                                dailyWage = labour?.dailyWage ?: 650.0,
                                                                skillType = labour?.skillType ?: "",
                                                                status = "Present",
                                                                isFinalized = false
                                                            )
                                                            viewModel.selectedEditStatus = "Present"
                                                            viewModel.editedWageAdjustment = (labour?.dailyWage ?: 650.0).toInt().toString()
                                                            viewModel.editReasonWord = ""
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$day",
                                                    color = textColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isMissingInPast && !isSelectedInBulk) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFEF4444))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = borderSlate.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Monthly Summary Panel
                    Text(
                        text = if (isHindi) "मासिक सारांश ($monthLabel)" else "Monthly Summary ($monthLabel)",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryChip(if (isHindi) "पूर्ण दिन" else "Full Day", "$pDays", Color(0xFF10B981))
                        SummaryChip(if (isHindi) "आधा दिन" else "Half Day", "$hDays", Color(0xFFF59E0B))
                        SummaryChip(if (isHindi) "अनुपस्थित" else "Absent", "$aDays", Color(0xFF94A3B8))
                        SummaryChip(if (isHindi) "बाकी दिन" else "Missing", "$mDays", Color(0xFFEF4444), isMissing = mDays > 0)
                    }
                }
            }
        }

        // Bulk Entry floating action panel
        if (isBulkModeEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E38)),
                    border = BorderStroke(1.5.dp, Color(0xFF8B5CF6))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "एक साथ कई हाजिरी दर्ज करें (${bulkSelectedDates.size} दिन चयनित)" else "Bulk Attendance Entry (${bulkSelectedDates.size} days selected)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            TextButton(
                                onClick = {
                                    bulkSelectedDates = missingDates.toSet()
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "सभी बाकी दिन चुनें" else "Select All Missing",
                                    color = Color(0xFFC084FC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = if (isHindi) {
                                "चेतावनी: हाजिरी सहेजने पर मजदूरी और विवरण स्वतः ही सीधे लेखा बही में अद्यतन हो जाएंगे।"
                            } else {
                                "Updating attendance will recalculate payment totals and wage records."
                            },
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (bulkSelectedDates.isEmpty()) {
                                        Toast.makeText(context, if (isHindi) "कृपया कम से कम एक तारीख चुनें" else "Please select at least one date", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.saveBulkBackdatedAttendance(
                                        labourId = labourId,
                                        dates = bulkSelectedDates.toList(),
                                        status = "Present",
                                        customWage = labour?.dailyWage ?: 650.0
                                    )
                                    bulkSelectedDates = emptySet()
                                    isBulkModeEnabled = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isHindi) "पूरा (Full)" else "Full Day", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            
                            Button(
                                onClick = {
                                    if (bulkSelectedDates.isEmpty()) {
                                        Toast.makeText(context, if (isHindi) "कृपया कम से कम एक तारीख चुनें" else "Please select at least one date", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.saveBulkBackdatedAttendance(
                                        labourId = labourId,
                                        dates = bulkSelectedDates.toList(),
                                        status = "Half Day",
                                        customWage = labour?.dailyWage ?: 650.0
                                    )
                                    bulkSelectedDates = emptySet()
                                    isBulkModeEnabled = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isHindi) "आधा (Half)" else "Half Day", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            
                            Button(
                                onClick = {
                                    if (bulkSelectedDates.isEmpty()) {
                                        Toast.makeText(context, if (isHindi) "कृपया कम से कम एक तारीख चुनें" else "Please select at least one date", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.saveBulkBackdatedAttendance(
                                        labourId = labourId,
                                        dates = bulkSelectedDates.toList(),
                                        status = "Absent",
                                        customWage = labour?.dailyWage ?: 650.0
                                    )
                                    bulkSelectedDates = emptySet()
                                    isBulkModeEnabled = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isHindi) "अनुपस्थित" else "Absent", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // Filters group
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Weekly", "Monthly").forEach { filter ->
                    val isCurr = currentFilter == filter
                    val lbl = when (filter) {
                        "Weekly" -> if (isHindi) "इस सप्ताह" else "Weekly"
                        "Monthly" -> if (isHindi) "इस महीने" else "Monthly"
                        else -> if (isHindi) "सभी रिकॉर्ड" else "All Logs"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isCurr) Color.White else cardBg)
                            .border(1.dp, if (isCurr) Color.White else borderSlate, RoundedCornerShape(30.dp))
                            .clickable { currentFilter = filter }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lbl,
                            color = if (isCurr) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Attendance list table header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "दैनिक हाजिरी खाता बही" else "Daily Attendance Sheets",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredRecords.size} ${if (isHindi) "दिन दर्ज" else "entries"}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderSlate)
                ) {
                    Text(
                        text = if (isHindi) "इस अवधि में कोई हाजिरी इतिहास उपलब्ध नहीं है।" else "No attendance logs found in this period.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        } else {
            items(filteredRecords) { record ->
                AttendanceRowItem(
                    record = record,
                    isHindi = isHindi,
                    cardBg = cardBg,
                    borderSlate = borderSlate,
                    colorPresent = colorPresent,
                    colorHalf = colorHalf,
                    colorAbsent = colorAbsent,
                    onEditClick = {
                        viewModel.attendanceSelectedDate = record.date
                        viewModel.editingAttendanceState = com.example.ui.viewmodel.WorkingAttendanceState(
                            labourId = record.labourId,
                            labourName = labour?.name ?: "Labour",
                            siteId = record.siteId,
                            siteName = "",
                            dailyWage = record.dailyRate,
                            skillType = labour?.skillType ?: "",
                            status = record.status,
                            isFinalized = record.finalized,
                            lastEditedAt = record.lastEditedAt,
                            lastEditedBy = record.lastEditedBy,
                            version = record.version,
                            isEditedAfterFinalization = record.lastEditedAt != null,
                            finalizedAt = record.finalizedAt ?: record.createdAt
                        )
                        viewModel.selectedEditStatus = record.status
                        viewModel.editedWageAdjustment = record.dailyRate.toInt().toString()
                        viewModel.editReasonWord = ""
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RowScope.SummaryChip(label: String, count: String, color: Color, isMissing: Boolean = false) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(
                width = 2.dp,
                color = if (isMissing) color else color.copy(alpha = 0.40f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                color = Color(0xFF374151),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatusIndicatorBadge(letter: String, count: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Text(count, color = color, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun AttendanceRowItem(
    record: Attendance,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorPresent: Color,
    colorHalf: Color,
    colorAbsent: Color,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderSlate)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Formatting Date
                val cleanDate = try {
                    val orig = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.date)
                    SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault()).format(orig ?: Date())
                } catch(e: Exception) {
                    record.date
                }

                Text(
                    text = cleanDate,
                    color = Color(0xFF111827),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rate: ₹${record.dailyRate.toInt()}",
                        color = Color(0xFF4B5563),
                        fontSize = 11.sp
                    )

                    if (record.lastEditedAt != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF3E8FF).copy(alpha = 0.15f))
                                .border(0.5.dp, Color(0xFFC084FC), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isHindi) "संशोधित" else "Edited ✍️",
                                color = Color(0xFFC084FC),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status indicator
                val col = when (record.status) {
                    "Present" -> colorPresent
                    "Half Day" -> colorHalf
                    else -> colorAbsent
                }
                val label = when (record.status) {
                    "Present" -> if (isHindi) "प्रेजेंट" else "Full Day"
                    "Half Day" -> if (isHindi) "हाफ डे" else "Half Day"
                    else -> if (isHindi) "एब्सेंट" else "Absent"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(col.copy(alpha = 0.15f))
                        .border(1.dp, col, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = col,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Attendance",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. PAYMENT SUBSECTION
// ==========================================
@Composable
fun PaymentSubSection(
    viewModel: LabourViewModel,
    payments: List<Payment>,
    totalEarned: Double,
    totalAdvance: Double,
    totalBonus: Double,
    totalDeduction: Double,
    totalExtraExpense: Double,
    pendingPayable: Double,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorExpense: Color,
    colorAdvance: Color,
    accentPurple: Color,
    onAddPayment: () -> Unit
) {
    val darkSurface = Color(0xFFF8F9FC)
    // Payments section filtering
    var currentPayFilter by remember { mutableStateOf("All") } // All, Paid, Pending/Kharch
    val filteredPayments = remember(payments, currentPayFilter) {
        when (currentPayFilter) {
            "Paid" -> payments.filter { it.paymentType == "Daily Wage (Kharchi)" }
            "Advances" -> payments.filter { it.paymentType in listOf("Weekly Advance", "Emergency Advance", "Petrol Expense", "Food Advance") || it.paymentType.contains("Advance", ignoreCase = true) }
            else -> payments
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Calculations and Ledger breakdown board
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "लेखा संगणना (Wage & Calculations)" else "Recalculation Ledger Formula",
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                FormulaRow(if (isHindi) "मजदूरी कुल अर्जित (Wages Earned)" else "Salary Wages Earned", "₹${totalEarned.toInt()}", Color(0xFF1E293B), isBold = true)
                FormulaRow(if (isHindi) "अतिरिक्त बोनस" else "Bonus Credits (+)", "₹${totalBonus.toInt()}", accentPurple)
                FormulaRow(if (isHindi) "अग्रिम कटौती (-)" else "Advance Deductions (-)", "₹${totalAdvance.toInt()}", colorAdvance)
                FormulaRow(if (isHindi) "अन्य कटौतियां (-)" else "Other Deductions (-)", "₹${totalDeduction.toInt()}", Color(0xFFEF4444))

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = borderSlate,
                    thickness = 1.dp
                )

                val netColor = if (pendingPayable >= 0) colorExpense else Color(0xFFEF4444)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "शुद्ध देय राशि (Net Balance)" else "Calculated Net Balance",
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isHindi) "नकद / यूपीआई भुगतान हेतु" else "Awaiting settlement",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = "₹${pendingPayable.toInt()}",
                        color = netColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Action controls (Add Ledger Settle Entry)
        Button(
            onClick = onAddPayment,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_payment_dialog_trigger"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add Calculation Credit/Debit",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isHindi) "भुगतान/बोनस/कटौती रिकॉर्ड करें" else "Record Settle Payment & Bonus",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // List Header Tab Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Paid", "Advances").forEach { payFilter ->
                val isCurr = currentPayFilter == payFilter
                val label = when (payFilter) {
                    "Paid" -> if (isHindi) "केवल भुगतान" else "Salaries Paid"
                    "Advances" -> if (isHindi) "केवल एडवांस" else "Advances Only"
                    else -> if (isHindi) "सभी विवरण देखें" else "All Payments"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isCurr) accentPurple else darkSurface)
                        .border(1.dp, if (isCurr) accentPurple else borderSlate, RoundedCornerShape(30.dp))
                        .clickable { currentPayFilter = payFilter }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isCurr) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Ledger list entries
        if (filteredPayments.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderSlate)
            ) {
                Text(
                    text = if (isHindi) "कोई भुगतान ट्रांसैक्शन रिकॉर्ड नहीं मिला।" else "No payment or credit transactions found.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
        } else {
            filteredPayments.forEach { pay ->
                PaymentRowItem(
                    payment = pay,
                    isHindi = isHindi,
                    cardBg = cardBg,
                    borderSlate = borderSlate,
                    colorExpense = colorExpense,
                    colorAdvance = colorAdvance,
                    accentPurple = accentPurple,
                    onDelete = { viewModel.deletePaymentDirectly(pay) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FormulaRow(label: String, amount: String, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF1E293B),
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.Medium
        )
        Text(
            text = amount,
            color = if (color == Color.White) Color(0xFF1E293B) else color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PaymentRowItem(
    payment: Payment,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorExpense: Color,
    colorAdvance: Color,
    accentPurple: Color,
    onDelete: () -> Unit
) {
    val indicatorColor = when (payment.paymentType) {
        "Daily Wage (Kharchi)", "Wages Paid", "Paid" -> colorExpense
        "Weekly Advance", "Emergency Advance", "Petrol Expense", "Food Advance" -> colorAdvance
        "Extra Expense" -> Color(0xFF3B82F6)
        "Bonus" -> accentPurple
        else -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderSlate)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circular leading category check
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(indicatorColor.copy(alpha = 0.15f))
                        .border(1.dp, indicatorColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (payment.paymentType) {
                            "Extra Expense" -> Icons.Rounded.LocalGasStation
                            "Bonus" -> Icons.Rounded.Star
                            "Deduction" -> Icons.Rounded.RemoveCircle
                            else -> Icons.Rounded.Payments
                        },
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val cleanType = when (payment.paymentType) {
                        "Daily Wage (Kharchi)" -> if (isHindi) "मजदूरी सिस्ट (Paid)" else "Salary Wage Paid"
                        "Weekly Advance" -> if (isHindi) "साप्ताहिक अग्रिम (Advance)" else "Weekly Advance"
                        "Emergency Advance" -> if (isHindi) "इमरजेंसी एडवांस" else "Emergency Advance"
                        "Petrol Expense" -> if (isHindi) "पेट्रोल एडवांस" else "Petrol Advance"
                        "Food Advance" -> if (isHindi) "खाद्य एडवांस" else "Food Advance"
                        "Extra Expense" -> if (isHindi) "अतिरिक्त व्यय" else "Extra Expense"
                        "Bonus" -> if (isHindi) "बोनस" else "Bonus Added"
                        else -> payment.paymentType
                    }

                    Text(
                        text = cleanType,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${payment.date} • ${payment.paymentMode} ${if (payment.remarks.isNotBlank()) "• ${payment.remarks}" else ""}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "₹${payment.amount.toInt()}",
                    color = indicatorColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. ADVANCE SUBSECTION
// ==========================================
@Composable
fun AdvanceSubSection(
    payments: List<Payment>,
    totalAdvance: Double,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorAdvance: Color,
    colorAbsent: Color,
    onAddAdvance: () -> Unit,
    onDeletePayment: (Payment) -> Unit
) {
    val advancePayments = remember(payments) {
        payments.filter { it.paymentType in listOf("Weekly Advance", "Emergency Advance", "Petrol Expense", "Food Advance") || it.paymentType.contains("Advance", ignoreCase = true) }
    }

    SafeScrollColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderSlate)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isHindi) "कुल ली गई अग्रिम राशि" else "Aggregate Advance Balance",
                            color = Color(0xFF475569),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${totalAdvance.toInt()}",
                            color = colorAdvance,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isHindi) "हाजिरी चक्र से देय कटौती योग्य" else "Settle deductions directly from earned salary amount.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(colorAdvance.copy(alpha = 0.15f))
                            .border(1.dp, colorAdvance, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = colorAdvance,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onAddAdvance,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_advance_dialog_trigger"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorAdvance)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = "Give Advance",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "अग्रिम भुगतान दर्ज करें (Give Advance)" else "Disburse Advance & Kharchi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Text(
                text = if (isHindi) "अन्तरिम भुगतान का बही खाता" else "Salary Advance Audit Ledger",
                color = Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (advancePayments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderSlate)
                ) {
                    Text(
                        text = if (isHindi) "कोई एडवांस / खर्च भुगतान इतिहास नहीं है।" else "No outstanding advances recorded for this worker.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        } else {
            items(advancePayments) { pay ->
                CommonLedgerItem(
                    payment = pay,
                    isHindi = isHindi,
                    cardBg = cardBg,
                    borderSlate = borderSlate,
                    colorAccent = colorAdvance,
                    onDelete = { onDeletePayment(pay) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// 4. EXTRA EXPENSE SUBSECTION
// ==========================================
@Composable
fun ExtraExpenseSubSection(
    payments: List<Payment>,
    totalExpense: Double,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorExpense: Color,
    colorAbsent: Color,
    onAddExpense: () -> Unit,
    onDeletePayment: (Payment) -> Unit
) {
    val expensePayments = remember(payments) {
        payments.filter { it.paymentType == "Extra Expense" }
    }

    SafeScrollColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderSlate)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isHindi) "कुल अतिरिक्त व्यय" else "Separate Extra Expenses Sum",
                            color = Color(0xFF475569),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${totalExpense.toInt()}",
                            color = colorExpense,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isHindi) "यह राशि वेतन गणना से बाहर रखी जाती है" else "These costs stand separately from wages & salary books.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(colorExpense.copy(alpha = 0.15f))
                            .border(1.dp, colorExpense, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalGasStation,
                            contentDescription = null,
                            tint = colorExpense,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onAddExpense,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_expense_dialog_trigger"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorExpense)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalGasStation,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "नया अतिरिक्त व्यय जोड़ें" else "Record New Extra Business Expense",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Text(
                text = if (isHindi) "अतिरिक्त व्यय बही" else "Business Expenses & Assets Ledger",
                color = Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (expensePayments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderSlate)
                ) {
                    Text(
                        text = if (isHindi) "कोई अतिरिक्त कार्यालय व्यय नहीं है।" else "No standalone expenses recorded for this worker profile.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        } else {
            items(expensePayments) { pay ->
                CommonLedgerItem(
                    payment = pay,
                    isHindi = isHindi,
                    cardBg = cardBg,
                    borderSlate = borderSlate,
                    colorAccent = colorExpense,
                    onDelete = { onDeletePayment(pay) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CommonLedgerItem(
    payment: Payment,
    isHindi: Boolean,
    cardBg: Color,
    borderSlate: Color,
    colorAccent: Color,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderSlate)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(colorAccent)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = payment.remarks.ifBlank { payment.paymentType },
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${payment.date} • Mode: ${payment.paymentMode}",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "₹${payment.amount.toInt()}",
                    color = colorAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete record",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// FORM DIALOGS - IMPLEMENTATIONS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAdvanceLedgerDialog(
    labourId: Int,
    labourName: String,
    siteId: Int,
    userId: String,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Payment) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Weekly Advance") }
    val types = listOf("Weekly Advance", "Emergency Advance", "Petrol Expense", "Food Advance")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = if (isHindi) "अग्रिम भुगतान दर्ज करें" else "Record Money Advance (Kharchi)",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Worker: $labourName",
                    color = Color.White.copy(alpha = 0.7f),
                    size = 12.sp
                )

                // Advance type selector
                Text(
                    text = if (isHindi) "अग्रिम प्रकार" else "Advance Categorization",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) Color(0xFFF97316) else Color(0xFF334155))
                                .clickable { selectedType = t }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (isHindi) "राशि (₹)" else "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("advance_amt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "विवरण / टिप्पणी (Notes)" else "Remarks / Reason") },
                    modifier = Modifier.fillMaxWidth().testTag("advance_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val p = Payment(
                            userId = userId,
                            labourId = labourId,
                            labourName = labourName,
                            siteId = siteId,
                            amount = amt,
                            date = sdf.format(Date()),
                            paymentMode = "Cash",
                            paymentType = selectedType,
                            remarks = notes.ifBlank { selectedType },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSubmit(p)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                modifier = Modifier.testTag("confirm_advance_submit")
            ) {
                Text(if (isHindi) "सुरक्षित करें" else "Disburse Advance")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseLedgerDialog(
    labourId: Int,
    labourName: String,
    siteId: Int,
    userId: String,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Payment) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Petrol") }
    val types = listOf("Petrol", "Tools", "Travel", "Material Help", "Food", "Bonus")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = if (isHindi) "अतिरिक्त व्यय दर्ज करें" else "Add Extra Standalone Expense",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Worker: $labourName",
                    color = Color.White.copy(alpha = 0.7f),
                    size = 12.sp
                )

                // Expense type selector
                Text(
                    text = if (isHindi) "व्यय का प्रकार" else "Expense Category",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) Color(0xFF10B981) else Color(0xFF334155))
                                .clickable { selectedType = t }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (isHindi) "राशि (₹)" else "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("expense_amt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "टिप्पणी / कारण" else "Remarks / Material details") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val p = Payment(
                            userId = userId,
                            labourId = labourId,
                            labourName = labourName,
                            siteId = siteId,
                            amount = amt,
                            date = sdf.format(Date()),
                            paymentMode = "Cash",
                            paymentType = "Extra Expense",
                            remarks = "$selectedType - ${notes.ifBlank { "N/A" }}",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSubmit(p)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.testTag("confirm_expense_submit")
            ) {
                Text(if (isHindi) "रिकॉर्ड सहेजें" else "Save Expense")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDirectPaymentDialog(
    labourId: Int,
    labourName: String,
    siteId: Int,
    userId: String,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Payment) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Daily Wage (Kharchi)") }
    var selectedMode by remember { mutableStateOf("UPI") }

    val types = listOf("Daily Wage (Kharchi)", "Bonus", "Deduction")
    val modes = listOf("UPI", "Cash", "Bank Transfer")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = if (isHindi) "पक्की खाता प्रविष्टि" else "Settle Financial Ledger Transaction",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Worker: $labourName",
                    color = Color.White.copy(alpha = 0.7f),
                    size = 12.sp
                )

                // Settle Type selector
                Text(
                    text = if (isHindi) "ट्रांसैक्शन प्रकार" else "Ledger Transaction Type",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val isSel = selectedType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) Color(0xFF8B5CF6) else Color(0xFF334155))
                                .clickable { selectedType = t }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Payment Mode Selector
                Text(
                    text = if (isHindi) "भुगतान माध्यम" else "Payment Settlement Mode",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { m ->
                        val isSel = selectedMode == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) Color(0xFF8B5CF6).copy(alpha = 0.7f) else Color(0xFF334155))
                                .clickable { selectedMode = m }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = m,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (isHindi) "राशि (₹)" else "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "टिप्पणी / टिप्पणी" else "Remarks / Reference No.") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_notes_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val p = Payment(
                            userId = userId,
                            labourId = labourId,
                            labourName = labourName,
                            siteId = siteId,
                            amount = amt,
                            date = sdf.format(Date()),
                            paymentMode = selectedMode,
                            paymentType = selectedType,
                            remarks = notes.ifBlank { selectedType },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSubmit(p)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                modifier = Modifier.testTag("confirm_payment_submit")
            ) {
                Text(if (isHindi) "सहेजें" else "Record Transaction")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

// Inline helper extension for Text size configuration in Dialogs to bypass deprecation wrapper errors
@Composable
private fun Text(text: String, color: Color, size: androidx.compose.ui.unit.TextUnit) {
    Text(text = text, color = color, fontSize = size)
}

@Composable
fun SafeScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: SafeScrollScope.() -> Unit
) {
    val scope = SafeScrollScope()
    scope.content()
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        scope.render()
    }
}

class SafeScrollScope {
    private val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }

    fun <T> items(
        itemsList: List<T>,
        itemContent: @Composable (item: T) -> Unit
    ) {
        itemsList.forEach { item ->
            items.add { itemContent(item) }
        }
    }

    @Composable
    fun render() {
        items.forEach { comp ->
            comp()
        }
    }
}
