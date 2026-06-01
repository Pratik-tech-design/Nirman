package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Site
import com.example.data.models.SiteExpense
import com.example.data.models.Payment
import com.example.data.models.UserProfile
import com.example.data.models.Labour
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.GeminiApiHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    viewModel: LabourViewModel,
    site: Site,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labours by viewModel.labours.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val allSiteExpenses by viewModel.siteExpenses.collectAsStateWithLifecycle()
    
    // Filter data for this specific site
    val siteWorkers = remember(labours, site.id) {
        labours.filter { it.siteId == site.id }
    }
    val sitePayments = remember(payments, site.id) {
        payments.filter { it.siteId == site.id }
    }
    val siteExpenses = remember(allSiteExpenses, site.id) {
        allSiteExpenses.filter { it.siteId == site.id }
    }

    // Calculations
    val workerHeadcount = siteWorkers.size
    val totalLabourExpense = remember(sitePayments) {
        sitePayments.sumOf { it.amount }
    }
    val totalSiteExpense = remember(siteExpenses) {
        siteExpenses.sumOf { it.amount }
    }
    val totalProjectCost = totalLabourExpense + totalSiteExpense

    // State for Search
    var searchQuery by remember { mutableStateOf("") }
    
    // Search Filter
    val filteredExpenses = remember(siteExpenses, searchQuery) {
        if (searchQuery.isBlank()) {
            siteExpenses
        } else {
            siteExpenses.filter {
                it.expenseName.contains(searchQuery, ignoreCase = true) ||
                        it.paidTo.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Modal state for Add/Edit Dialog
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<SiteExpense?>(null) }
    
    // Delete Confirmation Dialog state
    var expenseToDelete by remember { mutableStateOf<SiteExpense?>(null) }

    // Chatbot Panel expansion state
    var showChatPanel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = site.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Project Ledger Details / साइट विवरण",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to list"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showChatPanel = !showChatPanel },
                        modifier = Modifier.testTag("ai_helper_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("AI", fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Query Nirmaan AI Assistant",
                                tint = if (showChatPanel) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedExpenseForEdit = null
                    showExpenseDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add site Expense / खर्च जोड़े") },
                modifier = Modifier
                    .testTag("add_site_expense_fab")
                    .padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showChatPanel) {
                // Interactive AI assistant drawer pinned at top or toggled
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    ChatbotComponent(
                        site = site,
                        expenses = siteExpenses,
                        payments = sitePayments,
                        workerCount = workerHeadcount,
                        onClose = { showChatPanel = false }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (showChatPanel) 0.6f else 1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
            ) {
                // Top Cost Summary Dashboard Item
                item {
                    SiteKpiSection(
                        siteName = site.name,
                        location = site.location,
                        workersCount = workerHeadcount,
                        totalLabourExpense = totalLabourExpense,
                        totalSiteExpense = totalSiteExpense,
                        totalProjectCost = totalProjectCost
                    )
                }

                // Site Expense Analytics Analytics Breakdown
                item {
                    SiteExpenseAnalyticsSection(siteExpenses = siteExpenses)
                }

                // Search & Filter Header card
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Site Expense Ledger / व्यय रजिस्टर",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredExpenses.size} Records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search expense, buyer category, paid to..") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_ledger_search_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                // Empty state or Items List
                if (filteredExpenses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (searchQuery.isEmpty()) "No non-labour site expenses registered" else "No matching expense records found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (searchQuery.isEmpty()) "Tap the '+ Add Site Expense' button bellow to log materials, fuel, rent, food payments, etc. for ${site.name}." else "Try adjusting your search keywords to find the ledger transaction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseLedgerItemCard(
                            expense = expense,
                            onEditClick = {
                                selectedExpenseForEdit = expense
                                showExpenseDialog = true
                            },
                            onDeleteClick = {
                                expenseToDelete = expense
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog Form
    if (showExpenseDialog) {
        val currentUserId = currentUser?.uid ?: ""
        SiteExpenseFormDialog(
            siteId = site.id,
            userId = currentUserId,
            expenseToEdit = selectedExpenseForEdit,
            onDismiss = { showExpenseDialog = false },
            onSave = { expense ->
                if (selectedExpenseForEdit == null) {
                    viewModel.insertSiteExpenseDirectly(expense)
                } else {
                    viewModel.updateSiteExpenseDirectly(expense)
                }
                showExpenseDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Site Expense / खर्च हटाएं?") },
            text = {
                Text("Are you sure you want to permanently delete \"${exp.expenseName}\" worth ₹${exp.amount}? This operational ledger entry cannot be reversed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSiteExpenseDirectly(exp)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete / हटाएँ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expenseToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel / निरस्त")
                }
            }
        )
    }
}

@Composable
fun SiteKpiSection(
    siteName: String,
    location: String,
    workersCount: Int,
    totalLabourExpense: Double,
    totalSiteExpense: Double,
    totalProjectCost: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Investment / कुल परियोजना लागत",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "₹${"%,.2f".format(totalProjectCost)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    RowWithSpacing(verticalAlignment = Alignment.CenterVertically, spacing = 4.dp) {
                        Icon(Icons.Default.AccountBox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text("Active Labour / मजदूर", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "$workersCount Workers",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(modifier = Modifier.weight(1.3f)) {
                    RowWithSpacing(verticalAlignment = Alignment.CenterVertically, spacing = 4.dp) {
                        Icon(Icons.Filled.List, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Text("Labour wages / मजदूरी", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "₹${"%,.0f".format(totalLabourExpense)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column(modifier = Modifier.weight(1.3f)) {
                    RowWithSpacing(verticalAlignment = Alignment.CenterVertically, spacing = 4.dp) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                        Text("Site Expenses / साइट खर्च", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "₹${"%,.0f".format(totalSiteExpense)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFBF360C)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowWithSpacing(verticalAlignment: Alignment.Vertical, spacing: androidx.compose.ui.unit.Dp, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = verticalAlignment, modifier = Modifier.padding(bottom = 2.dp)) {
        content()
        Spacer(modifier = Modifier.width(spacing))
    }
}

@Composable
fun SiteExpenseAnalyticsSection(
    siteExpenses: List<SiteExpense>,
    modifier: Modifier = Modifier
) {
    // Computes analytics:
    // 1. Total Site Expense
    // 2. This Month Site Expense (May 2026 / current date format 2026-05)
    // 3. Highest Expense Category
    // 4. Most Frequent Expense Type

    val totalAmt = siteExpenses.sumOf { it.amount }
    
    val currentMonthPrefix = remember {
        val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        format.format(Date()) // Today's month format
    }
    
    val thisMonthAmt = remember(siteExpenses, currentMonthPrefix) {
        siteExpenses.filter { 
            it.expenseDate.startsWith(currentMonthPrefix) || 
            it.expenseDate.contains("/05/") || // Matches "29/05/2026" manual backdate too
            it.expenseDate.endsWith("2026") && it.expenseDate.contains("May")
        }.sumOf { it.amount }
    }

    val highestCat = remember(siteExpenses) {
        if (siteExpenses.isEmpty()) "N/A"
        else {
            siteExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .maxByOrNull { it.value }?.key ?: "N/A"
        }
    }

    val mostFreqType = remember(siteExpenses) {
        if (siteExpenses.isEmpty()) "N/A"
        else {
            siteExpenses.groupBy { it.expenseName.lowercase().trim() }
                .maxByOrNull { it.value.size }?.value?.firstOrNull()?.expenseName ?: "N/A"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Operational Site Analytics / विश्लेषण सारांश",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Monthly Spends
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("This Month Spend", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("₹${"%,.0f".format(thisMonthAmt)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                // Top Category
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Top Category", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(highestCat, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                
                // Frequency
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Frequent Duty", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(mostFreqType, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseLedgerItemCard(
    expense: SiteExpense,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.expenseName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(expense.category, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = null,
                            modifier = Modifier.height(20.dp)
                        )
                        
                        Text(
                            text = expense.expenseDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Text(
                    text = "₹${"%,.2f".format(expense.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paid To: ${expense.paidTo}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expense.description.isNotBlank()) {
                        Text(
                            text = expense.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_expense_button_${expense.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit manual record",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_expense_button_${expense.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete manual record",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SiteExpenseFormDialog(
    siteId: Int,
    userId: String,
    expenseToEdit: SiteExpense?,
    onDismiss: () -> Unit,
    onSave: (SiteExpense) -> Unit
) {
    var expenseName by remember { mutableStateOf(expenseToEdit?.expenseName ?: "") }
    var amountStr by remember { mutableStateOf(expenseToEdit?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Material") }
    var expenseDate by remember {
        mutableStateOf(expenseToEdit?.expenseDate ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }
    var paidTo by remember { mutableStateOf(expenseToEdit?.paidTo ?: "") }
    var description by remember { mutableStateOf(expenseToEdit?.description ?: "") }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Material", "Fuel/Petrol", "Transport", "Food/Catering", "Rent/Lease", "Government/Permits", "Tools/Equipment", "Consumables", "Other")

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var paidToError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("site_expense_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (expenseToEdit == null) "Add Site Expense Details" else "Edit Site Expense Ledger Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Title field
                item {
                    OutlinedTextField(
                        value = expenseName,
                        onValueChange = {
                            expenseName = it
                            nameError = false
                        },
                        label = { Text("Expense Title / खर्च नाम (e.g., Petrol, Cement)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_name_field"),
                        isError = nameError,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Amount field
                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            amountError = false
                        },
                        label = { Text("Amount / राशि (INR)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_amount_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Dropdown Category
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Category / श्रेणी") },
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose Category")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_expense_category_field")
                                .clickable { isDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date Picker field
                item {
                    OutlinedTextField(
                        value = expenseDate,
                        onValueChange = { expenseDate = it },
                        label = { Text("Transaction Date / तिथि (DD/MM/YYYY)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_date_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Paid To field
                item {
                    OutlinedTextField(
                        value = paidTo,
                        onValueChange = {
                            paidTo = it
                            paidToError = false
                        },
                        label = { Text("Paid To / किसे भुगतान किया (Name)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_paidTo_field"),
                        isError = paidToError,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                // Details/Description field
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Remarks Details / विवरण (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_expense_description_field"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dialog_cancel_button")
                        ) {
                            Text("Cancel / रद्द करें")
                        }

                        Button(
                            onClick = {
                                var hasError = false
                                if (expenseName.trim().isEmpty()) {
                                    nameError = true
                                    hasError = true
                                }
                                val amountVal = amountStr.toDoubleOrNull()
                                if (amountVal == null || amountVal <= 0.0) {
                                    amountError = true
                                    hasError = true
                                }
                                if (paidTo.trim().isEmpty()) {
                                    paidToError = true
                                    hasError = true
                                }

                                if (!hasError) {
                                    onSave(
                                        SiteExpense(
                                            id = expenseToEdit?.id ?: 0,
                                            userId = userId,
                                            siteId = siteId,
                                            expenseName = expenseName.trim(),
                                            amount = amountVal!!,
                                            category = category,
                                            expenseDate = expenseDate.trim(),
                                            paidTo = paidTo.trim(),
                                            description = description.trim()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("dialog_save_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Ledger Entry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatbotComponent(
    site: Site,
    expenses: List<SiteExpense>,
    payments: List<Payment>,
    workerCount: Int,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("Hello Contractor, ask me any question about non-labour materials, food catering, fuel costs, or labor payout distributions for ${site.name}!", isUser = false)
            )
        )
    }
    var isAILoading by remember { mutableStateOf(false) }

    val proposedPrompts = listOf(
        "How much was spent on petrol this month?",
        "What is the total expense for ${site.name}?",
        "Who received the highest site expense payment?",
        "How much was spent on food expenses?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Nirmaan AI Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Real-Time Site Financial Specialist", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close chat")
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Message List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(chatMessages) { msg ->
                ChatBubble(message = msg)
            }
            if (isAILoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading Site Financial Registers...", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Suggestions Horizontal List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Let's organize the prompts into 2 quick scrollable suggestion rows, or list them compactly 2 at a time!
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        proposedPrompts.take(2).forEach { prompt ->
                            SuggestionChip(
                                onClick = {
                                    if (!isAILoading) {
                                        inputQuery = prompt
                                        // Auto Send!
                                        val workingQuery = prompt
                                        chatMessages = chatMessages + ChatMessage(workingQuery, isUser = true)
                                        isAILoading = true
                                        inputQuery = ""
                                        coroutineScope.launch {
                                            val reply = GeminiApiHelper.queryGemini(
                                                workingQuery, site, expenses, payments, workerCount
                                            )
                                            chatMessages = chatMessages + ChatMessage(reply, isUser = false)
                                            isAILoading = false
                                        }
                                    }
                                },
                                label = { Text(prompt, fontSize = 9.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        proposedPrompts.drop(2).forEach { prompt ->
                            SuggestionChip(
                                onClick = {
                                    if (!isAILoading) {
                                        inputQuery = prompt
                                        // Auto Send!
                                        val workingQuery = prompt
                                        chatMessages = chatMessages + ChatMessage(workingQuery, isUser = true)
                                        isAILoading = true
                                        inputQuery = ""
                                        coroutineScope.launch {
                                            val reply = GeminiApiHelper.queryGemini(
                                                workingQuery, site, expenses, payments, workerCount
                                            )
                                            chatMessages = chatMessages + ChatMessage(reply, isUser = false)
                                            isAILoading = false
                                        }
                                    }
                                },
                                label = { Text(prompt, fontSize = 9.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // Search inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask Nirmaan AI about expenses...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chatbot_input_query_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                maxLines = 1,
                trailingIcon = {
                    if (inputQuery.isNotBlank() && !isAILoading) {
                        IconButton(
                            onClick = {
                                val workingQuery = inputQuery
                                chatMessages = chatMessages + ChatMessage(workingQuery, isUser = true)
                                isAILoading = true
                                inputQuery = ""
                                coroutineScope.launch {
                                    val reply = GeminiApiHelper.queryGemini(
                                        workingQuery, site, expenses, payments, workerCount
                                    )
                                    chatMessages = chatMessages + ChatMessage(reply, isUser = false)
                                    isAILoading = false
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to AI")
                        }
                    }
                }
            )
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatBubble(message: ChatMessage) {
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 12.sp,
                modifier = Modifier.padding(10.dp),
                lineHeight = 16.sp
            )
        }
    }
}
