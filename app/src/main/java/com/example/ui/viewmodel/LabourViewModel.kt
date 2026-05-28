package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.LabourDatabase
import com.example.data.firebase.FirebaseAuthService
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.data.models.UserProfile
import com.example.data.repository.LabourRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LabourViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LabourRepository
    private val authService: FirebaseAuthService

    // Application language mode: "English" or "Hindi"
    var isHindi by mutableStateOf(false)
        private set

    fun toggleLanguage() {
        isHindi = !isHindi
    }

    init {
        val database = LabourDatabase.getDatabase(application, viewModelScope)
        repository = LabourRepository(database.labourDao())
        authService = FirebaseAuthService.getInstance(application, database)

        viewModelScope.launch {
            try {
                authService.currentUser.collect { user ->
                    if (user != null) {
                        repository.preseedFirestoreDataIfEmpty(user.uid)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Failed to check or seed workspace in Firestore", e)
            }
        }
    }

    // AUTH STATE
    val currentUser: StateFlow<UserProfile?> = authService.currentUser

    var authEmail by mutableStateOf("")
    var authPassword by mutableStateOf("")
    var authDisplayName by mutableStateOf("")
    var isSignUpMode by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)
    var isAuthLoading by mutableStateOf(false)

    fun toggleAuthMode() {
        isSignUpMode = !isSignUpMode
        authError = null
    }

    fun handleAuthAction() {
        authError = null
        if (authEmail.isBlank() || authPassword.isBlank()) {
            authError = if (isHindi) "कृपया सभी फ़ील्ड भरें" else "Please fill all fields"
            return
        }
        if (isSignUpMode && authDisplayName.isBlank()) {
            authError = if (isHindi) "कृपया नाम दर्ज करें" else "Please enter your name"
            return
        }

        isAuthLoading = true
        viewModelScope.launch {
            if (isSignUpMode) {
                authService.signUp(authDisplayName, authEmail, authPassword)
                    .onSuccess {
                        showToast(if (isHindi) "पंजीकरण सफल" else "Registration Successful")
                        resetAuthForm()
                    }
                    .onFailure {
                        authError = it.message ?: "Authentication Failed"
                    }
            } else {
                authService.login(authEmail, authPassword)
                    .onSuccess {
                        showToast(if (isHindi) "लॉगिन सफल" else "Log In Successful")
                        resetAuthForm()
                    }
                    .onFailure {
                        authError = it.message ?: "Authentication Failed"
                    }
            }
            isAuthLoading = false
        }
    }

    fun logout() {
        authService.logout()
        showToast(if (isHindi) "सफलतापूर्वक लॉगआउट किया गया" else "Logged out successfully")
    }

    private fun resetAuthForm() {
        authEmail = ""
        authPassword = ""
        authDisplayName = ""
        authError = null
        isSignUpMode = false
    }

    // Observed collections - dynamically query partitioned by logged-in user!
    @OptIn(ExperimentalCoroutinesApi::class)
    val sites: StateFlow<List<Site>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getAllSites(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val labours: StateFlow<List<Labour>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getAllLabour(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val payments: StateFlow<List<Payment>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getAllPayments(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter states
    private val _selectedSiteFilter = MutableStateFlow<Int?>(null)
    val selectedSiteFilter = _selectedSiteFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setSiteFilter(siteId: Int?) {
        _selectedSiteFilter.value = siteId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered lists logic
    val filteredLabours: StateFlow<List<Labour>> = combine(
        labours, _selectedSiteFilter, _searchQuery
    ) { list, siteId, query ->
        list.filter { labour ->
            val isActive = labour.status == "Active"
            val matchesSite = siteId == null || labour.siteId == siteId
            val matchesQuery = query.isEmpty() || 
                labour.name.contains(query, ignoreCase = true) ||
                labour.skillType.contains(query, ignoreCase = true) ||
                labour.phoneNumber.contains(query)
            isActive && matchesSite && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary Statistics
    val totalLabourCount = labours.combine(filteredLabours) { all, _ ->
        all.count { it.status == "Active" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeSitesCount = sites.combine(filteredLabours) { list, _ ->
        list.count { it.status == "Active" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPaymentsAmount = payments.combine(filteredLabours) { list, _ ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageWage = labours.combine(filteredLabours) { list, _ ->
        val active = list.filter { it.status == "Active" }
        if (active.isEmpty()) 0.0 else active.map { it.dailyWage }.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // UI Menu and Dialog states
    var isFabMenuExpanded by mutableStateOf(false)
        private set

    var showAddLabourDialog by mutableStateOf(false)
        private set

    var showAddPaymentDialog by mutableStateOf(false)
        private set

    var showAddSiteDialog by mutableStateOf(false)
        private set

    // Edit state holders
    var editingLabour by mutableStateOf<Labour?>(null)
        private set

    var editingSite by mutableStateOf<Site?>(null)
        private set

    // Delete state holders
    var showDeleteLabourDialog by mutableStateOf(false)
        private set

    var deletingLabour by mutableStateOf<Labour?>(null)
        private set

    var showDeleteSiteDialog by mutableStateOf(false)
        private set

    var deletingSite by mutableStateOf<Site?>(null)
        private set

    // Selected navigation tab (0 = Dashboard/Labour, 1 = Sites, 2 = Disbursements)
    var selectedTab by mutableStateOf(0)
        private set

    fun selectTab(index: Int) {
        selectedTab = index
    }

    fun toggleFabMenu() {
        isFabMenuExpanded = !isFabMenuExpanded
    }

    fun closeFabMenu() {
        isFabMenuExpanded = false
    }

    fun openAddLabourDialog() {
        resetLabourForm()
        editingLabour = null
        showAddLabourDialog = true
        closeFabMenu()
    }

    fun openEditLabourDialog(labour: Labour) {
        editingLabour = labour
        labourFormName = labour.name
        labourFormPhone = labour.phoneNumber
        labourFormSkillType = labour.skillType
        labourFormDailyWage = labour.dailyWage.toInt().toString()
        labourFormSiteId = labour.siteId
        labourFormRepeatAutomatically = labour.repeatAutomatically
        showAddLabourDialog = true
        closeFabMenu()
    }

    fun closeAddLabourDialog() {
        showAddLabourDialog = false
        editingLabour = null
    }

    fun openAddPaymentDialog() {
        resetPaymentForm()
        showAddPaymentDialog = true
        closeFabMenu()
    }

    fun closeAddPaymentDialog() {
        showAddPaymentDialog = false
    }

    fun openAddSiteDialog() {
        resetSiteForm()
        editingSite = null
        showAddSiteDialog = true
        closeFabMenu()
    }

    fun openEditSiteDialog(site: Site) {
        editingSite = site
        siteFormName = site.name
        siteFormLocation = site.location
        siteFormManagerName = site.managerName
        showAddSiteDialog = true
        closeFabMenu()
    }

    fun closeAddSiteDialog() {
        showAddSiteDialog = false
        editingSite = null
    }

    fun openDeleteLabourDialog(labour: Labour) {
        deletingLabour = labour
        showDeleteLabourDialog = true
    }

    fun closeDeleteLabourDialog() {
        showDeleteLabourDialog = false
        deletingLabour = null
    }

    fun openDeleteSiteDialog(site: Site) {
        deletingSite = site
        showDeleteSiteDialog = true
    }

    fun closeDeleteSiteDialog() {
        showDeleteSiteDialog = false
        deletingSite = null
    }

    // Deletion states & routines
    fun softDeleteLabour(labour: Labour) {
        viewModelScope.launch {
            try {
                val updated = labour.copy(status = "Inactive")
                repository.updateLabour(updated)
                showToast(if (isHindi) "मजदूर सफलतापूर्वक हटाया गया" else "Labour removed successfully")
                closeDeleteLabourDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun permanentDeleteLabour(labour: Labour) {
        viewModelScope.launch {
            try {
                val uid = currentUser.value?.uid ?: "default_contractor_uid"
                repository.deleteLabourById(labour.id, uid)
                showToast(if (isHindi) "मजदूर सफलतापूर्वक हटाया गया" else "Labour removed successfully")
                closeDeleteLabourDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun archiveSite(site: Site) {
        viewModelScope.launch {
            try {
                val updated = site.copy(status = "Archived")
                repository.updateSite(updated)
                showToast(if (isHindi) "साइट सफलतापूर्वक संग्रहित की गई" else "Site archived successfully")
                closeDeleteSiteDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    fun permanentDeleteSite(site: Site) {
        viewModelScope.launch {
            try {
                val uid = currentUser.value?.uid ?: "default_contractor_uid"
                repository.deleteSiteById(site.id, uid)
                showToast(if (isHindi) "साइट सफलतापूर्वक हटाई गई" else "Site removed successfully")
                closeDeleteSiteDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    // Shared toast notifications flow: transmits message string
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(message)
        }
    }

    // ==========================================
    // FORM STATE: LABOUR FORM
    // ==========================================
    var labourFormName by mutableStateOf("")
    var labourFormPhone by mutableStateOf("")
    
    // Skill selection
    val skillOptionsEnglish = listOf("Mason", "Helper", "Carpenter", "Welder", "Electrician", "Plumber", "Supervisor")
    val skillOptionsHindi = listOf("राजमिस्त्री (Mason)", "मददगार (Helper)", "बढ़ई (Carpenter)", "वेल्डर (Welder)", "बिजली मिस्त्री (Electrician)", "प्लम्बर (Plumber)", "सुपरवाइजर (Supervisor)")
    var labourFormSkillType by mutableStateOf("Mason")

    var labourFormDailyWage by mutableStateOf("")
    var labourFormSiteId by mutableStateOf<Int?>(null)
    var labourFormRepeatAutomatically by mutableStateOf(false)

    // Error states
    var labourFormNameError by mutableStateOf<String?>(null)
    var labourFormPhoneError by mutableStateOf<String?>(null)
    var labourFormWageError by mutableStateOf<String?>(null)
    var isLabourSubmitting by mutableStateOf(false)

    private fun resetLabourForm() {
        labourFormName = ""
        labourFormPhone = ""
        labourFormSkillType = if (isHindi) "राजमिस्त्री (Mason)" else "Mason"
        labourFormDailyWage = ""
        labourFormSiteId = sites.value.firstOrNull()?.id
        labourFormRepeatAutomatically = false
        
        labourFormNameError = null
        labourFormPhoneError = null
        labourFormWageError = null
        isLabourSubmitting = false
    }

    fun updateSiteSelection(siteId: Int) {
        labourFormSiteId = siteId
    }

    fun submitLabour() {
        // Simple valid checks
        var hasError = false
        if (labourFormName.trim().isEmpty()) {
            labourFormNameError = if (isHindi) "नाम खाली नहीं हो सकता" else "Name cannot be empty"
            hasError = true
        } else {
            labourFormNameError = null
        }

        val phoneTrim = labourFormPhone.trim()
        if (phoneTrim.isNotEmpty() && (phoneTrim.length != 10 || phoneTrim.any { !it.isDigit() })) {
            labourFormPhoneError = if (isHindi) "वैध 10-अंकीय फ़ोन नंबर दर्ज करें" else "Enter a valid 10-digit phone number"
            hasError = true
        } else {
            labourFormPhoneError = null
        }

        val wageVal = labourFormDailyWage.toDoubleOrNull()
        if (wageVal == null || wageVal <= 0) {
            labourFormWageError = if (isHindi) "वैध दैनिक वेतन दर्ज करें" else "Enter a valid daily wage"
            hasError = true
        } else {
            labourFormWageError = null
        }

        if (labourFormSiteId == null) {
            showToast(if (isHindi) "कृपया एक साइट चुनें" else "Please select a site")
            hasError = true
        }

        if (hasError) return

        isLabourSubmitting = true
        viewModelScope.launch {
            try {
                val currentUid = currentUser.value?.uid ?: "default_contractor_uid"
                val currentEditingLabour = editingLabour
                if (currentEditingLabour != null) {
                    val updatedLabour = currentEditingLabour.copy(
                        name = labourFormName.trim(),
                        phoneNumber = phoneTrim,
                        skillType = labourFormSkillType,
                        dailyWage = wageVal ?: 0.0,
                        siteId = labourFormSiteId,
                        repeatAutomatically = labourFormRepeatAutomatically,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateLabour(updatedLabour)
                    showToast(
                        if (isHindi) "मजदूर विवरण सफलतापूर्वक अपडेट किया गया" else "Labour details updated successfully"
                    )
                } else {
                    val newLabour = Labour(
                        userId = currentUid,
                        name = labourFormName.trim(),
                        phoneNumber = phoneTrim,
                        skillType = labourFormSkillType,
                        dailyWage = wageVal ?: 0.0,
                        siteId = labourFormSiteId,
                        status = "Active",
                        repeatAutomatically = labourFormRepeatAutomatically,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.insertLabour(newLabour)
                    
                    showToast(
                        if (isHindi) "मजदूर सफलतापूर्वक जोड़ा गया" else "Labour added successfully"
                    )
                }
                closeAddLabourDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isLabourSubmitting = false
            }
        }
    }

    // ==========================================
    // FORM STATE: PAYMENT FORM
    // ==========================================
    var paymentFormLabourId by mutableStateOf<Int?>(null)
    var paymentFormAmount by mutableStateOf("")
    var paymentFormMode by mutableStateOf("UPI")
    var paymentFormType by mutableStateOf("Daily Wage (Kharchi)") // Color coded styles: e.g. Daily Wage (Kharchi), Extra Expense, Weekly Advance, Bonus, Deduction
    var paymentFormRemarks by mutableStateOf("")
    var paymentFormDate by mutableStateOf(getCurrentDateString())
    
    var paymentFormAmountError by mutableStateOf<String?>(null)
    var paymentFormLabourError by mutableStateOf<String?>(null)
    var isPaymentSubmitting by mutableStateOf(false)

    private fun resetPaymentForm() {
        paymentFormLabourId = labours.value.firstOrNull()?.id
        paymentFormAmount = ""
        paymentFormMode = "UPI"
        paymentFormType = "Daily Wage (Kharchi)"
        paymentFormRemarks = ""
        paymentFormDate = getCurrentDateString()
        paymentFormAmountError = null
        paymentFormLabourError = null
        isPaymentSubmitting = false
    }

    fun submitPayment() {
        var hasError = false
        if (paymentFormLabourId == null) {
            paymentFormLabourError = if (isHindi) "कृपया मजदूर का चयन करें" else "Please select a labour"
            hasError = true
        } else {
            paymentFormLabourError = null
        }

        val amt = paymentFormAmount.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            paymentFormAmountError = if (isHindi) "वैध राशि दर्ज करें" else "Enter a valid amount"
            hasError = true
        } else {
            paymentFormAmountError = null
        }

        if (hasError) return

        val labourObj = labours.value.find { it.id == paymentFormLabourId }
        val name = labourObj?.name ?: "Unknown"
        val assignedSiteId = labourObj?.siteId ?: 0

        isPaymentSubmitting = true
        viewModelScope.launch {
            try {
                val currentUid = currentUser.value?.uid ?: "default_contractor_uid"
                val payment = Payment(
                    userId = currentUid,
                    labourId = paymentFormLabourId ?: 0,
                    labourName = name,
                    siteId = assignedSiteId,
                    amount = amt ?: 0.0,
                    date = paymentFormDate,
                    paymentMode = paymentFormMode,
                    paymentType = paymentFormType,
                    remarks = paymentFormRemarks.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertPayment(payment)
                showToast(
                    if (isHindi) "भुगतान सफलतापूर्वक जोड़ा गया" else "Payment added successfully"
                )
                closeAddPaymentDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isPaymentSubmitting = false
            }
        }
    }

    // ==========================================
    // FORM STATE: SITE FORM
    // ==========================================
    var siteFormName by mutableStateOf("")
    var siteFormLocation by mutableStateOf("")
    var siteFormManagerName by mutableStateOf("")
    
    var siteFormNameError by mutableStateOf<String?>(null)
    var siteFormLocationError by mutableStateOf<String?>(null)
    var siteFormManagerError by mutableStateOf<String?>(null)
    var isSiteSubmitting by mutableStateOf(false)

    private fun resetSiteForm() {
        siteFormName = ""
        siteFormLocation = ""
        siteFormManagerName = ""
        siteFormNameError = null
        siteFormLocationError = null
        siteFormManagerError = null
        isSiteSubmitting = false
    }

    fun submitSite() {
        var hasError = false
        if (siteFormName.trim().isEmpty()) {
            siteFormNameError = if (isHindi) "साइट का नाम खाली नहीं हो सकता" else "Site name cannot be empty"
            hasError = true
        } else {
            siteFormNameError = null
        }

        if (siteFormLocation.trim().isEmpty()) {
            siteFormLocationError = if (isHindi) "स्थान खाली नहीं हो सकता" else "Location cannot be empty"
            hasError = true
        } else {
            siteFormLocationError = null
        }

        if (siteFormManagerName.trim().isEmpty()) {
            siteFormManagerError = if (isHindi) "मैनेजर का नाम खाली नहीं हो सकता" else "Manager name cannot be empty"
            hasError = true
        } else {
            siteFormManagerError = null
        }

        if (hasError) return

        isSiteSubmitting = true
        viewModelScope.launch {
            try {
                val currentUid = currentUser.value?.uid ?: "default_contractor_uid"
                val currentEditingSite = editingSite
                if (currentEditingSite != null) {
                    val updatedSite = currentEditingSite.copy(
                        name = siteFormName.trim(),
                        location = siteFormLocation.trim(),
                        managerName = siteFormManagerName.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateSite(updatedSite)
                    showToast(
                        if (isHindi) "साइट विवरण सफलतापूर्वक अपडेट किया गया" else "Site details updated successfully"
                    )
                } else {
                    val site = Site(
                        userId = currentUid,
                        name = siteFormName.trim(),
                        location = siteFormLocation.trim(),
                        managerName = siteFormManagerName.trim(),
                        status = "Active",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.insertSite(site)
                    showToast(
                        if (isHindi) "नई साइट सफलतापूर्वक जोड़ी गई" else "New site added successfully"
                    )
                }
                closeAddSiteDialog()
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                isSiteSubmitting = false
            }
        }
    }

    // EXPORT METHOD EXCLUSIVELY FOR ACTIVE CONTRACTOR
    fun exportContractorData(context: Context) {
        val user = currentUser.value ?: return
        val currentSites = sites.value
        val currentLabour = labours.value
        val currentPayments = payments.value

        val csvBuilder = StringBuilder()
        csvBuilder.append("LABOUR TRACKER - MULTI-TENANT BACKUP EXPORT\n")
        csvBuilder.append("Contractor/Manager: ${user.displayName}\n")
        csvBuilder.append("Contractor UID: ${user.uid}\n")
        csvBuilder.append("Email Account: ${user.email}\n")
        csvBuilder.append("Export Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

        csvBuilder.append("=== REGISTERED SITES ===\n")
        csvBuilder.append("Site ID, Name, Location, Manager, Status\n")
        currentSites.forEach {
            csvBuilder.append("${it.id}, \"${it.name}\", \"${it.location}\", \"${it.managerName}\", ${it.status}\n")
        }

        csvBuilder.append("\n=== LABOUR LOGS ===\n")
        csvBuilder.append("Labour ID, Name, Phone Number, Skill Type, Daily Wage, Site ID, Status\n")
        currentLabour.forEach {
            csvBuilder.append("${it.id}, \"${it.name}\", \"${it.phoneNumber}\", \"${it.skillType}\", ${it.dailyWage}, ${it.siteId}, ${it.status}\n")
        }

        csvBuilder.append("\n=== RECOGNIZED PAYMENTS ===\n")
        csvBuilder.append("Payment ID, Labour Name, Amount, Date, Mode, Remarks\n")
        currentPayments.forEach {
            csvBuilder.append("${it.id}, \"${it.labourName}\", ${it.amount}, ${it.date}, ${it.paymentMode}, \"${it.remarks}\"\n")
        }

        try {
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, csvBuilder.toString())
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Labour Tracker Multi-Tenant Export - ${user.displayName}")
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Contractor Data")
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            showToast("Export failed: ${e.message}")
        }
    }

    // EXPORTS HIGHLY FORMATTED EXCEL WORKBOOKS
    var isExportingExcel by mutableStateOf(false)

    fun exportToExcel(
        context: Context,
        filterSiteId: Int? = null,
        filterLabourId: Int? = null,
        startDateStr: String? = null,
        endDateStr: String? = null
    ) {
        val user = currentUser.value ?: return
        if (isExportingExcel) return // Disable duplicate clicks
        
        isExportingExcel = true
        viewModelScope.launch {
            try {
                // Short organic suspend delay for rich UX feedback progress matching state
                kotlinx.coroutines.delay(600)
                
                val currentSites = sites.value
                val currentLabour = labours.value
                val currentPayments = payments.value
                
                val exportedFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.util.ExcelExporter.generateLabourExcelReport(
                        context = context,
                        user = user,
                        sites = currentSites,
                        labours = currentLabour,
                        payments = currentPayments,
                        attendances = attendance.value,
                        filterSiteId = filterSiteId,
                        filterLabourId = filterLabourId,
                        startDateStr = startDateStr,
                        endDateStr = endDateStr,
                        isHindi = isHindi
                    )
                }
                
                com.example.util.ExcelExporter.shareGeneratedExcel(
                    context = context,
                    excelFile = exportedFile,
                    title = if (isHindi) "मजदूर वित्त रिपोर्ट" else "Labour Finance Report"
                )
                
                showToast(
                    if (isHindi) "एक्सेल रिपोर्ट सफलतापूर्वक निर्यात की गई" else "Excel report exported successfully"
                )
            } catch (e: Exception) {
                showToast("Excel Export Error: ${e.message}")
            } finally {
                isExportingExcel = false
            }
        }
    }

    // ==========================================
    // DAILY ATTENDANCE & AUTO-PAYMENT HANDLERS
    // ==========================================
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendance: StateFlow<List<com.example.data.models.Attendance>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getAllAttendance(user.uid)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var showAttendanceDialog by mutableStateOf(false)
    var isGeneratingWages by mutableStateOf(false)
    var attendanceListState by mutableStateOf<List<WorkingAttendanceState>>(emptyList())
    var attendanceSelectedDate by mutableStateOf(getCurrentDateString())
    
    // Auto finalization configurations
    var isAutoFinalizeEnabled by mutableStateOf(true)
    var hasAutoShownAttendanceToday = false

    fun toggleAttendance(labourId: Int) {
        attendanceListState = attendanceListState.map {
            if (it.labourId == labourId) {
                if (it.isFinalized) return@map it
                val nextStatus = when (it.status) {
                    "Present" -> "Half Day"
                    "Half Day" -> "Absent"
                    else -> "Present"
                }
                saveSingleDraftToDb(labourId, nextStatus)
                it.copy(status = nextStatus)
            } else it
        }
    }

    fun setAttendanceStatus(labourId: Int, nextStatus: String) {
        attendanceListState = attendanceListState.map {
            if (it.labourId == labourId) {
                if (it.isFinalized) return@map it
                saveSingleDraftToDb(labourId, nextStatus)
                it.copy(status = nextStatus)
            } else it
        }
    }

    private fun saveSingleDraftToDb(labourId: Int, status: String) {
        val date = attendanceSelectedDate
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        viewModelScope.launch {
            val drafts = repository.getAttendanceDraftsByDate(uid, date)
            val match = drafts.find { it.labourId == labourId }
            val item = labours.value.find { it.id == labourId }
            if (match != null) {
                repository.updateAttendanceDraft(match.copy(status = status, updatedAt = System.currentTimeMillis()))
            } else {
                repository.insertAttendanceDraft(
                    com.example.data.models.AttendanceDraft(
                        userId = uid,
                        labourId = labourId,
                        siteId = item?.siteId ?: 0,
                        date = date,
                        status = status,
                        finalized = false,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun markAllPresent() {
        attendanceListState = attendanceListState.map {
            if (it.isFinalized) it else {
                saveSingleDraftToDb(it.labourId, "Present")
                it.copy(status = "Present")
            }
        }
    }

    fun markAllAbsent() {
        attendanceListState = attendanceListState.map {
            if (it.isFinalized) it else {
                saveSingleDraftToDb(it.labourId, "Absent")
                it.copy(status = "Absent")
            }
        }
    }

    fun loadAttendanceForSelectedDate() {
        val date = attendanceSelectedDate
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        viewModelScope.launch {
            val finalizedList = repository.getAttendanceByDate(uid, date)
            val eligibleLabours = labours.value.filter { it.status == "Active" && it.repeatAutomatically }
            val currentSites = sites.value

            if (finalizedList.isNotEmpty()) {
                // Already finalized! Lock statuses
                attendanceListState = eligibleLabours.map { labour ->
                    val siteName = currentSites.find { it.id == labour.siteId }?.name ?: "No Site"
                    val finalStatus = finalizedList.find { it.labourId == labour.id }?.status ?: "Absent"
                    WorkingAttendanceState(
                        labourId = labour.id,
                        labourName = labour.name,
                        siteId = labour.siteId ?: 0,
                        siteName = siteName,
                        dailyWage = labour.dailyWage,
                        skillType = labour.skillType,
                        status = finalStatus,
                        isFinalized = true
                    )
                }
            } else {
                // Load drafts
                val drafts = repository.getAttendanceDraftsByDate(uid, date)
                if (drafts.isNotEmpty()) {
                    attendanceListState = eligibleLabours.map { labour ->
                        val siteName = currentSites.find { it.id == labour.siteId }?.name ?: "No Site"
                        val draftStatus = drafts.find { it.labourId == labour.id }?.status ?: "Present"
                        WorkingAttendanceState(
                            labourId = labour.id,
                            labourName = labour.name,
                            siteId = labour.siteId ?: 0,
                            siteName = siteName,
                            dailyWage = labour.dailyWage,
                            skillType = labour.skillType,
                            status = draftStatus,
                            isFinalized = false
                        )
                    }
                } else {
                    // No drafts and no finalized yet! Auto-draft with default "Present"
                    val newDrafts = eligibleLabours.map { labour ->
                        com.example.data.models.AttendanceDraft(
                            userId = uid,
                            labourId = labour.id,
                            siteId = labour.siteId ?: 0,
                            date = date,
                            status = "Present",
                            finalized = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    if (newDrafts.isNotEmpty()) {
                        repository.insertAttendanceDrafts(newDrafts)
                    }

                    attendanceListState = eligibleLabours.map { labour ->
                        val siteName = currentSites.find { it.id == labour.siteId }?.name ?: "No Site"
                        WorkingAttendanceState(
                            labourId = labour.id,
                            labourName = labour.name,
                            siteId = labour.siteId ?: 0,
                            siteName = siteName,
                            dailyWage = labour.dailyWage,
                            skillType = labour.skillType,
                            status = "Present",
                            isFinalized = false
                        )
                    }
                }
            }
        }
    }

    fun checkIfAttendanceNeededOnOpen() {
        if (hasAutoShownAttendanceToday) return
        val today = getCurrentDateString()
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        viewModelScope.launch {
            val uid = currentUser.value?.uid ?: "default_contractor_uid"
            val todayAttendance = repository.getAttendanceByDate(uid, today)

            // Auto finalize at end of day logic (8:00 PM = hour >= 20)
            if (isAutoFinalizeEnabled && hour >= 20 && todayAttendance.isEmpty()) {
                val eligibleLabours = labours.value.filter { it.status == "Active" && it.repeatAutomatically }
                if (eligibleLabours.isNotEmpty()) {
                    attendanceSelectedDate = today
                    loadAttendanceForSelectedDate()
                    // Delay a moment to ensure state is populated
                    kotlinx.coroutines.delay(200)
                    generateDailyWagesAndAttendance()
                    showToast(if (isHindi) "8:00 पीएम बीतने के कारण आज की मजदूरी स्वचालित रूप से सहेजी गई!" else "Auto-finalized today's wages as daily work hours concluded!")
                    hasAutoShownAttendanceToday = true
                    return@launch
                }
            }

            if (todayAttendance.isEmpty()) {
                val eligibleLabours = labours.value.filter { it.status == "Active" && it.repeatAutomatically }
                if (eligibleLabours.isNotEmpty()) {
                    attendanceSelectedDate = today
                    loadAttendanceForSelectedDate()
                    showAttendanceDialog = true
                    hasAutoShownAttendanceToday = true
                }
            }
        }
    }

    fun openManualAttendanceDialog() {
        attendanceSelectedDate = getCurrentDateString()
        loadAttendanceForSelectedDate()
        showAttendanceDialog = true
    }

    fun generateDailyWagesAndAttendance() {
        if (isGeneratingWages) return
        isGeneratingWages = true
        
        viewModelScope.launch {
            try {
                val date = attendanceSelectedDate
                val uid = currentUser.value?.uid ?: "default_contractor_uid"
                
                val todayAttendance = repository.getAttendanceByDate(uid, date)
                if (todayAttendance.isNotEmpty()) {
                    showToast(if (isHindi) "आज की मजदूरी पहले ही बनाई जा चुकी है" else "Today’s wages already generated")
                    showAttendanceDialog = false
                    isGeneratingWages = false
                    return@launch
                }
                
                for (item in attendanceListState) {
                    val status = item.status
                    var generatedPaymentId: Int? = null
                    
                    if (status == "Present" || status == "Half Day") {
                        val multiple = if (status == "Half Day") 0.5 else 1.0
                        val wageAmount = item.dailyWage * multiple
                        
                        val payment = Payment(
                            userId = uid,
                            labourId = item.labourId,
                            labourName = item.labourName,
                            siteId = item.siteId,
                            amount = wageAmount,
                            date = date,
                            paymentMode = "Cash",
                            paymentType = "Daily Wage (Kharchi)",
                            remarks = if (isHindi) {
                                if (status == "Half Day") "स्वचालित दैनिक वेतन (हाफ डे)" else "स्वचालित दैनिक वेतन (प्रेजेंट)"
                            } else {
                                if (status == "Half Day") "Auto-generated daily wage (Half Day)" else "Auto-generated daily wage (Present)"
                            },
                            status = status,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val paymentId = repository.insertPayment(payment)
                        generatedPaymentId = paymentId.toInt()
                    }
                    
                    val attendanceRecord = com.example.data.models.Attendance(
                        userId = uid,
                        labourId = item.labourId,
                        siteId = item.siteId,
                        date = date,
                        status = status,
                        generatedPaymentId = generatedPaymentId,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertAttendance(attendanceRecord)
                    
                    // Update/save finalized drafts to prevent duplication
                    val drafts = repository.getAttendanceDraftsByDate(uid, date)
                    val currentDraft = drafts.find { it.labourId == item.labourId }
                    if (currentDraft != null) {
                        repository.updateAttendanceDraft(currentDraft.copy(finalized = true, status = status, updatedAt = System.currentTimeMillis()))
                    } else {
                        repository.insertAttendanceDraft(
                            com.example.data.models.AttendanceDraft(
                                userId = uid,
                                labourId = item.labourId,
                                siteId = item.siteId,
                                date = date,
                                status = status,
                                finalized = true,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                
                showToast(if (isHindi) "आज की मजदूरी और उपस्थिति सफलतापूर्वक दर्ज की गई!" else "Today's wages and attendance recorded successfully!")
                showAttendanceDialog = false
                loadAttendanceForSelectedDate()
            } catch (e: Exception) {
                showToast("Error generating wages: ${e.message}")
            } finally {
                isGeneratingWages = false
            }
        }
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

data class WorkingAttendanceState(
    val labourId: Int,
    val labourName: String,
    val siteId: Int,
    val siteName: String,
    val dailyWage: Double,
    val skillType: String,
    val status: String = "Present", // "Present", "Absent", "Half Day"
    val isFinalized: Boolean = false
) {
    val isPresent: Boolean get() = status == "Present" || status == "Half Day"
}
