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
import com.example.data.models.Attendance
import com.example.data.models.AttendanceAuditLog
import com.example.data.models.SiteExpense
import com.example.data.repository.LabourRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

        // Start a lightweight coroutine check loop for 8:00 PM auto-finalization
        viewModelScope.launch {
            while (true) {
                try {
                    checkAutoFinalizeTick()
                } catch (e: Exception) {
                    android.util.Log.e("LabourViewModel", "Error in auto-finalize tick", e)
                }
                kotlinx.coroutines.delay(15000) // check every 15 seconds
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
                .catch { emit(emptyList()) }
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
                .catch { emit(emptyList()) }
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
                .catch { emit(emptyList()) }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val siteExpenses: StateFlow<List<SiteExpense>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getAllSiteExpenses(user.uid)
                .catch { emit(emptyList()) }
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

    private val _selectedTradeFilter = MutableStateFlow<String?>(null)
    val selectedTradeFilter = _selectedTradeFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String?>(null)
    val selectedStatusFilter = _selectedStatusFilter.asStateFlow()

    private val _selectedWageTypeFilter = MutableStateFlow<String?>(null)
    val selectedWageTypeFilter = _selectedWageTypeFilter.asStateFlow()

    private val _selectedAttendanceStatusFilter = MutableStateFlow<String?>(null)
    val selectedAttendanceStatusFilter = _selectedAttendanceStatusFilter.asStateFlow()

    fun setSiteFilter(siteId: Int?) {
        _selectedSiteFilter.value = siteId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTradeFilter(trade: String?) {
        _selectedTradeFilter.value = trade
    }

    fun setStatusFilter(status: String?) {
        _selectedStatusFilter.value = status
    }

    fun setWageTypeFilter(wageType: String?) {
        _selectedWageTypeFilter.value = wageType
    }

    fun setAttendanceStatusFilter(status: String?) {
        _selectedAttendanceStatusFilter.value = status
    }

    fun resetDashboardFilters() {
        _selectedSiteFilter.value = null
        _searchQuery.value = ""
        _selectedTradeFilter.value = null
        _selectedStatusFilter.value = null
        _selectedWageTypeFilter.value = null
        _selectedAttendanceStatusFilter.value = null
    }

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

    var selectedLabourForDetail by mutableStateOf<Labour?>(null)
    var selectedSiteForDetail by mutableStateOf<Site?>(null)

    fun openLabourDetail(labour: Labour) {
        selectedLabourForDetail = labour
    }

    fun closeLabourDetail() {
        selectedLabourForDetail = null
    }

    fun openSiteDetail(site: Site) {
        selectedSiteForDetail = site
    }

    fun closeSiteDetail() {
        selectedSiteForDetail = null
    }

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

    fun openAddPaymentForWorker(workerId: Int) {
        resetPaymentForm()
        paymentFormLabourId = workerId
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
                kotlinx.coroutines.withTimeout(8000) {
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
                }
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Firebase/Firestore save failed for Labour", e)
                showToast("Error saving Labour: ${e.localizedMessage ?: e.message}")
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
                kotlinx.coroutines.withTimeout(8000) {
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
                }
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Firebase/Firestore save failed for Payment", e)
                showToast("Error adding Payment: ${e.localizedMessage ?: e.message}")
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
                kotlinx.coroutines.withTimeout(8000) {
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
                }
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Firebase/Firestore save failed for Site", e)
                showToast("Error saving Site: ${e.localizedMessage ?: e.message}")
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
                
                // Get bounds for date range
                val bounds = getDateRangeBounds()
                val finalStartDate = startDateStr ?: bounds.first
                val finalEndDate = endDateStr ?: bounds.second
                val finalSiteId = filterSiteId ?: filterSiteIdState
                
                // Filter records with full precision using helper functions
                val finalPayments = getFilteredPayments(filterLabourId, payments.value)
                val finalAttendances = getFilteredAttendances(filterLabourId, attendance.value)
                val finalSiteExpenses = getFilteredSiteExpenses(siteExpenses.value)
                
                val exportedFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.util.ExcelExporter.generateLabourExcelReport(
                        context = context,
                        user = user,
                        sites = currentSites,
                        labours = currentLabour,
                        payments = finalPayments,
                        attendances = finalAttendances,
                        siteExpenses = finalSiteExpenses,
                        filterSiteId = finalSiteId,
                        filterLabourId = filterLabourId,
                        startDateStr = finalStartDate,
                        endDateStr = finalEndDate,
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
                .catch { emit(emptyList()) }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Secondary filter state data class for optimized compile
    data class AdvancedFilterState(
        val siteId: Int?,
        val query: String,
        val trade: String?,
        val status: String?,
        val wageType: String?,
        val attStatus: String?
    )

    private val filterPart1: kotlinx.coroutines.flow.Flow<Pair<Int?, String>> = combine(
        _selectedSiteFilter,
        _searchQuery
    ) { siteFilter, search ->
        Pair(siteFilter, search)
    }

    private val filterPart2: kotlinx.coroutines.flow.Flow<Triple<String?, String?, String?>> = combine(
        _selectedTradeFilter,
        _selectedStatusFilter,
        _selectedWageTypeFilter
    ) { trade, status, wageType ->
        Triple(trade, status, wageType)
    }

    // Now combine the parts along with the attendance status filter!
    private val filterStateFlow: kotlinx.coroutines.flow.Flow<AdvancedFilterState> = combine(
        filterPart1,
        filterPart2,
        _selectedAttendanceStatusFilter
    ) { part1, part2, attStatus ->
        AdvancedFilterState(
            siteId = part1.first,
            query = part1.second,
            trade = part2.first,
            status = part2.second,
            wageType = part2.third,
            attStatus = attStatus
        )
    }

    // Filtered lists logic
    val filteredLabours: StateFlow<List<Labour>> = combine(
        labours,
        filterStateFlow,
        attendance
    ) { list, filter, attList ->
        val todayStr = getCurrentDateString()
        val todaysAtt = attList.filter { it.date == todayStr }

        list.filter { labour ->
            // Site filter
            val matchesSite = filter.siteId == null || labour.siteId == filter.siteId

            // Query filter
            val matchesQuery = filter.query.isEmpty() || 
                labour.name.contains(filter.query, ignoreCase = true) ||
                labour.skillType.contains(filter.query, ignoreCase = true) ||
                labour.phoneNumber.contains(filter.query)

            // Trade filter
            val matchesTrade = filter.trade == null || labour.skillType.equals(filter.trade, ignoreCase = true)

            // Status filter (Active/Inactive)
            // Default to only Active unless explicitly filtering
            val matchesStatus = if (filter.status == null) {
                labour.status.equals("Active", ignoreCase = true)
            } else {
                labour.status.equals(filter.status, ignoreCase = true)
            }

            // Wage type filter (Auto/Manual)
            val matchesWageType = filter.wageType == null || (
                if (filter.wageType == "Auto Wage") labour.repeatAutomatically else !labour.repeatAutomatically
            )

            // Attendance status filter (Present/Absent/Pending)
            val actualAttToday = todaysAtt.find { it.labourId == labour.id }
            val matchesAttStatus = filter.attStatus == null || when (filter.attStatus) {
                "Present" -> actualAttToday?.status == "Present" || actualAttToday?.status == "Half Day"
                "Absent" -> actualAttToday?.status == "Absent"
                "Pending" -> actualAttToday == null
                else -> true
            }

            matchesSite && matchesQuery && matchesTrade && matchesStatus && matchesWageType && matchesAttStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Summary Statistics
    val totalLabourCount: StateFlow<Int> = labours.combine(filteredLabours) { all, _ ->
        all.count { it.status == "Active" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeSitesCount: StateFlow<Int> = sites.combine(filteredLabours) { list, _ ->
        list.count { it.status == "Active" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPaymentsAmount: StateFlow<Double> = payments.combine(filteredLabours) { list, _ ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averageWage: StateFlow<Double> = labours.combine(filteredLabours) { list, _ ->
        val active = list.filter { it.status == "Active" }
        if (active.isEmpty()) 0.0 else active.map { it.dailyWage }.average()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    var showAttendanceDialog by mutableStateOf(false)
    var isGeneratingWages by mutableStateOf(false)
    var attendanceListState by mutableStateOf<List<WorkingAttendanceState>>(emptyList())
    var attendanceSelectedDate by mutableStateOf(getCurrentDateString())
    
    // Auto finalization configurations
    var isAutoFinalizeEnabled by mutableStateOf(true)
    var hasAutoShownAttendanceToday = false

    // Manual lock override dates (Admin/Owner only)
    var overrideUnlockedDates by mutableStateOf<Set<String>>(emptySet())

    // Finalized Attendance Editing States
    var editingAttendanceState by mutableStateOf<WorkingAttendanceState?>(null)
    var selectedEditStatus by mutableStateOf("Present")
    var editedWageAdjustment by mutableStateOf("")
    var editReasonWord by mutableStateOf("")

    fun isDateLocked(date: String): Boolean {
        if (overrideUnlockedDates.contains(date)) {
            return false
        }
        val today = getCurrentDateString()
        if (date < today) {
            return true
        }
        if (date == today) {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            return hour >= 20
        }
        return false
    }

    fun unlockSelectedDateAttendance() {
        val date = attendanceSelectedDate
        overrideUnlockedDates = overrideUnlockedDates + date
        loadAttendanceForSelectedDate()
    }

    fun lockSelectedDateAttendance() {
        val date = attendanceSelectedDate
        overrideUnlockedDates = overrideUnlockedDates - date
        loadAttendanceForSelectedDate()
    }

    fun toggleAttendance(labourId: Int) {
        val date = attendanceSelectedDate
        if (isDateLocked(date)) return
        attendanceListState = attendanceListState.map {
            if (it.labourId == labourId) {
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
        val date = attendanceSelectedDate
        if (isDateLocked(date)) return
        attendanceListState = attendanceListState.map {
            if (it.labourId == labourId) {
                saveSingleDraftToDb(labourId, nextStatus)
                it.copy(status = nextStatus)
            } else it
        }
    }

    fun toggleAttendanceStatusForToday(labourId: Int) {
        val today = getCurrentDateString()
        attendanceSelectedDate = today
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        viewModelScope.launch {
            try {
                val currentAttendance = repository.getAttendanceByDate(uid, today)
                val existingRecord = currentAttendance.find { it.labourId == labourId }
                val nextStatus = when (existingRecord?.status) {
                    "Present" -> "Half Day"
                    "Half Day" -> "Absent"
                    else -> "Present"
                }
                saveSingleDraftToDb(labourId, nextStatus)
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Quick attendance toggle failed", e)
            }
        }
    }

    fun saveSingleDraftToDb(labourId: Int, status: String) {
        val date = attendanceSelectedDate
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        viewModelScope.launch {
            try {
                val currentAttendance = repository.getAttendanceByDate(uid, date)
                val existingRecord = currentAttendance.find { it.labourId == labourId }
                val labour = labours.value.find { it.id == labourId } ?: return@launch
                
                val earnedAmount = when (status) {
                    "Present" -> labour.dailyWage
                    "Half Day" -> labour.dailyWage * 0.5
                    else -> 0.0
                }
                
                val isLocked = isDateLocked(date)
                
                // Keep track of generated payment id
                var generatedPaymentId = existingRecord?.generatedPaymentId

                // Check payments
                val userPayments = payments.value
                val existingPayment = userPayments.find { 
                    it.labourId == labourId && 
                    it.date == date && 
                    it.paymentType == "Daily Wage (Kharchi)" 
                }
                
                if (status == "Present" || status == "Half Day") {
                    if (existingPayment != null) {
                        // Update existing payment
                        val updatedPayment = existingPayment.copy(
                            amount = earnedAmount,
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.insertPayment(updatedPayment)
                        generatedPaymentId = updatedPayment.id
                    } else {
                        // Create new payment
                        val payment = Payment(
                            userId = uid,
                            labourId = labourId,
                            labourName = labour.name,
                            siteId = labour.siteId ?: 0,
                            amount = earnedAmount,
                            date = date,
                            paymentMode = "Cash",
                            paymentType = "Daily Wage (Kharchi)",
                            status = status,
                            remarks = if (isHindi) {
                                if (status == "Half Day") "स्वचालित दैनिक वेतन (हाफ डे)" else "स्वचालित दैनिक वेतन (प्रेजेंट)"
                            } else {
                                if (status == "Half Day") "Auto-generated daily wage (Half Day)" else "Auto-generated daily wage (Present)"
                            },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val paymentId = repository.insertPayment(payment)
                        generatedPaymentId = paymentId.toInt()
                    }
                } else {
                    // Status is Absent (no earnings). Delete existing payment if there is one!
                    if (existingPayment != null) {
                        repository.deletePayment(existingPayment)
                        generatedPaymentId = null
                    }
                }
                
                // Now create/update Attendance record
                val attendanceRecord = com.example.data.models.Attendance(
                    id = existingRecord?.id ?: 0,
                    userId = uid,
                    labourId = labourId,
                    siteId = labour.siteId ?: 0,
                    date = date,
                    status = status,
                    attendanceType = when (status) {
                        "Present" -> "Full Day"
                        "Half Day" -> "Half Day"
                        else -> "Absent"
                    },
                    dailyRate = labour.dailyWage,
                    earnedAmount = earnedAmount,
                    finalized = !overrideUnlockedDates.contains(date) && isLocked,
                    finalizedAt = existingRecord?.finalizedAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    generatedPaymentId = generatedPaymentId,
                    createdAt = existingRecord?.createdAt ?: System.currentTimeMillis()
                )
                
                repository.insertAttendance(attendanceRecord)
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Error saving single attendance", e)
            }
        }
    }

    suspend fun saveBulkAttendanceToDb(items: List<WorkingAttendanceState>) {
        val date = attendanceSelectedDate
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        try {
            val currentAttendance = repository.getAttendanceByDate(uid, date)
            val userPayments = payments.value
            val laboursList = labours.value

            items.forEach { item ->
                val status = item.status
                val labourId = item.labourId
                val existingRecord = currentAttendance.find { it.labourId == labourId }
                val labour = laboursList.find { it.id == labourId } ?: return@forEach

                val earnedAmount = when (status) {
                    "Present" -> labour.dailyWage
                    "Half Day" -> labour.dailyWage * 0.5
                    else -> 0.0
                }

                val isLocked = isDateLocked(date)
                var generatedPaymentId = existingRecord?.generatedPaymentId

                val existingPayment = userPayments.find {
                    it.labourId == labourId &&
                    it.date == date &&
                    it.paymentType == "Daily Wage (Kharchi)"
                }

                if (status == "Present" || status == "Half Day") {
                    if (existingPayment != null) {
                        val updatedPayment = existingPayment.copy(
                            amount = earnedAmount,
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.insertPayment(updatedPayment)
                        generatedPaymentId = updatedPayment.id
                    } else {
                        val payment = Payment(
                            userId = uid,
                            labourId = labourId,
                            labourName = labour.name,
                            siteId = labour.siteId ?: 0,
                            amount = earnedAmount,
                            date = date,
                            paymentMode = "Cash",
                            paymentType = "Daily Wage (Kharchi)",
                            status = status,
                            remarks = if (isHindi) {
                                if (status == "Half Day") "स्वचालित दैनिक वेतन (हाफ डे)" else "स्वचालित दैनिक वेतन (प्रेजेंट)"
                            } else {
                                if (status == "Half Day") "Auto-generated daily wage (Half Day)" else "Auto-generated daily wage (Present)"
                            },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val paymentId = repository.insertPayment(payment)
                        generatedPaymentId = paymentId.toInt()
                    }
                } else {
                    if (existingPayment != null) {
                        repository.deletePayment(existingPayment)
                        generatedPaymentId = null
                    }
                }

                val attendanceRecord = com.example.data.models.Attendance(
                    id = existingRecord?.id ?: 0,
                    userId = uid,
                    labourId = labourId,
                    siteId = labour.siteId ?: 0,
                    date = date,
                    status = status,
                    attendanceType = when (status) {
                        "Present" -> "Full Day"
                        "Half Day" -> "Half Day"
                        else -> "Absent"
                    },
                    dailyRate = labour.dailyWage,
                    earnedAmount = earnedAmount,
                    finalized = !overrideUnlockedDates.contains(date) && isLocked,
                    finalizedAt = existingRecord?.finalizedAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    generatedPaymentId = generatedPaymentId,
                    createdAt = existingRecord?.createdAt ?: System.currentTimeMillis()
                )

                repository.insertAttendance(attendanceRecord)
            }
        } catch (e: Exception) {
            android.util.Log.e("LabourViewModel", "Error in bulk attendance save", e)
        }
    }

    fun updateFinalizedAttendance(labourId: Int, newStatus: String, customWage: Double, reason: String) {
        val date = attendanceSelectedDate
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        viewModelScope.launch {
            try {
                val currentAttendance = repository.getAttendanceByDate(uid, date)
                val existingRecord = currentAttendance.find { it.labourId == labourId }
                val oldStatus = existingRecord?.status ?: "Absent"
                
                val earnedAmount = when (newStatus) {
                    "Present" -> customWage
                    "Half Day" -> customWage * 0.5
                    else -> 0.0
                }
                
                // 1. Insert Audit Log
                val auditLog = AttendanceAuditLog(
                    userId = uid,
                    labourId = labourId,
                    date = date,
                    oldValue = oldStatus,
                    newValue = newStatus,
                    editedBy = currentUser.value?.displayName ?: "Pratik",
                    editedAt = System.currentTimeMillis(),
                    reason = if (reason.isBlank()) "Manual Correction" else reason
                )
                repository.insertAttendanceAuditLog(auditLog)
                
                // 2. Manage payments to match updated attendance
                var generatedPaymentId = existingRecord?.generatedPaymentId
                val userPayments = payments.value
                val existingPayment = userPayments.find { 
                    it.labourId == labourId && 
                    it.date == date && 
                    it.paymentType == "Daily Wage (Kharchi)" 
                }
                
                if (newStatus == "Present" || newStatus == "Half Day") {
                    if (existingPayment != null) {
                        val updatedPayment = existingPayment.copy(
                            amount = earnedAmount,
                            status = newStatus,
                            updatedAt = System.currentTimeMillis(),
                            remarks = if (isHindi) {
                                "संशोधित दैनिक वेतन (${if (newStatus == "Half Day") "हाफ डे" else "पूरा दिन"})"
                            } else {
                                "Corrected daily wage (${if (newStatus == "Half Day") "Half Day" else "Full Day"})"
                            }
                        )
                        repository.insertPayment(updatedPayment)
                        generatedPaymentId = updatedPayment.id
                    } else {
                        val payment = Payment(
                            userId = uid,
                            labourId = labourId,
                            labourName = editingAttendanceState?.labourName ?: "Labour",
                            siteId = editingAttendanceState?.siteId ?: 0,
                            amount = earnedAmount,
                            date = date,
                            paymentMode = "Cash",
                            paymentType = "Daily Wage (Kharchi)",
                            status = newStatus,
                            remarks = if (isHindi) {
                                if (newStatus == "Half Day") "संशोधित दैनिक वेतन (हाफ डे)" else "संशोधित दैनिक वेतन (प्रेजेंट)"
                            } else {
                                if (newStatus == "Half Day") "Corrected wage (Half Day)" else "Corrected wage (Present)"
                            },
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val paymentId = repository.insertPayment(payment)
                        generatedPaymentId = paymentId.toInt()
                    }
                } else {
                    if (existingPayment != null) {
                        repository.deletePayment(existingPayment)
                        generatedPaymentId = null
                    }
                }
                
                // 3. Update Attendance Record
                val attendanceRecord = com.example.data.models.Attendance(
                    id = existingRecord?.id ?: 0,
                    userId = uid,
                    labourId = labourId,
                    siteId = editingAttendanceState?.siteId ?: existingRecord?.siteId ?: 0,
                    date = date,
                    status = newStatus,
                    attendanceType = when (newStatus) {
                        "Present" -> "Full Day"
                        "Half Day" -> "Half Day"
                        else -> "Absent"
                    },
                    dailyRate = customWage,
                    earnedAmount = earnedAmount,
                    finalized = true,
                    finalizedAt = existingRecord?.finalizedAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    generatedPaymentId = generatedPaymentId,
                    createdAt = existingRecord?.createdAt ?: System.currentTimeMillis(),
                    lastEditedAt = System.currentTimeMillis(),
                    lastEditedBy = currentUser.value?.displayName ?: "Pratik",
                    autoGenerated = existingRecord?.autoGenerated ?: false,
                    version = (existingRecord?.version ?: 1) + 1
                )
                
                repository.insertAttendance(attendanceRecord)
                
                showToast(
                    if (isHindi) "मजदूरी भुगतान और वित्तीय विवरण सफलतापूर्वक अपडेट किए गए!" 
                    else "Attendance corrected & wage ledgers recalculated successfully!"
                )
                
                loadAttendanceForSelectedDate()
                editingAttendanceState = null
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Error updating finalized attendance", e)
                showToast("Correction Error: ${e.message}")
            }
        }
    }

    fun saveBulkBackdatedAttendance(labourId: Int, dates: List<String>, status: String, customWage: Double) {
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        val labour = labours.value.find { it.id == labourId } ?: return
        viewModelScope.launch {
            try {
                dates.forEach { date ->
                    val currentAttendance = repository.getAttendanceByDate(uid, date)
                    val existingRecord = currentAttendance.find { it.labourId == labourId }
                    val oldStatus = existingRecord?.status ?: "Absent"
                    
                    val earnedAmount = when (status) {
                        "Present" -> customWage
                        "Half Day" -> customWage * 0.5
                        else -> 0.0
                    }
                    
                    // 1. Audit Log if status changes
                    if (oldStatus != status || (existingRecord != null && existingRecord.dailyRate != customWage)) {
                        val auditLog = AttendanceAuditLog(
                            userId = uid,
                            labourId = labourId,
                            date = date,
                            oldValue = oldStatus,
                            newValue = status,
                            editedBy = currentUser.value?.displayName ?: "Pratik",
                            editedAt = System.currentTimeMillis(),
                            reason = "Bulk Backdated Log"
                        )
                        repository.insertAttendanceAuditLog(auditLog)
                    }
                    
                    // 2. Payments handling
                    val userPayments = payments.value
                    val existingPayment = userPayments.find { 
                        it.labourId == labourId && 
                        it.date == date && 
                        it.paymentType == "Daily Wage (Kharchi)" 
                    }
                    var generatedPaymentId = existingRecord?.generatedPaymentId
                    
                    if (status == "Present" || status == "Half Day") {
                        if (existingPayment != null) {
                            val updatedPayment = existingPayment.copy(
                                amount = earnedAmount,
                                status = status,
                                updatedAt = System.currentTimeMillis(),
                                remarks = if (isHindi) {
                                    "स्वचालित दैनिक वेतन (${if (status == "Half Day") "हाफ डे" else "पूरा दिन"})"
                                } else {
                                    "Daily wage (${if (status == "Half Day") "Half Day" else "Full Day"})"
                                }
                            )
                            repository.insertPayment(updatedPayment)
                            generatedPaymentId = updatedPayment.id
                        } else {
                            val payment = Payment(
                                userId = uid,
                                labourId = labourId,
                                labourName = labour.name,
                                siteId = labour.siteId ?: 0,
                                amount = earnedAmount,
                                date = date,
                                paymentMode = "Cash",
                                paymentType = "Daily Wage (Kharchi)",
                                status = status,
                                remarks = if (isHindi) {
                                    if (status == "Half Day") "दैनिक वेतन (हाफ डे)" else "दैनिक वेतन (प्रेजेंट)"
                                } else {
                                    if (status == "Half Day") "Daily wage (Half Day)" else "Daily wage (Present)"
                                },
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            val paymentId = repository.insertPayment(payment)
                            generatedPaymentId = paymentId.toInt()
                        }
                    } else {
                        if (existingPayment != null) {
                            repository.deletePayment(existingPayment)
                            generatedPaymentId = null
                        }
                    }
                    
                    // 3. Insert / Update Attendance
                    val attendanceRecord = com.example.data.models.Attendance(
                        id = existingRecord?.id ?: 0,
                        userId = uid,
                        labourId = labourId,
                        siteId = labour.siteId ?: 0,
                        date = date,
                        status = status,
                        attendanceType = when (status) {
                            "Present" -> "Full Day"
                            "Half Day" -> "Half Day"
                            else -> "Absent"
                        },
                        dailyRate = customWage,
                        earnedAmount = earnedAmount,
                        finalized = true,
                        finalizedAt = existingRecord?.finalizedAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        generatedPaymentId = generatedPaymentId,
                        createdAt = existingRecord?.createdAt ?: System.currentTimeMillis(),
                        lastEditedAt = System.currentTimeMillis(),
                        lastEditedBy = currentUser.value?.displayName ?: "Pratik",
                        autoGenerated = existingRecord?.autoGenerated ?: false,
                        version = (existingRecord?.version ?: 1) + 1
                    )
                    
                    repository.insertAttendance(attendanceRecord)
                }
                
                showToast(
                    if (isHindi) "${dates.size} दिनों की हाजिरी सफलतापूर्वक अपडेट की गई!" 
                    else "Successfully logged attendance for ${dates.size} days!"
                )
                
                loadAttendanceForSelectedDate()
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Error saving bulk backdated attendance", e)
                showToast("Bulk Sync Error: ${e.message}")
            }
        }
    }

    fun markAllPresent() {
        val date = attendanceSelectedDate
        if (isDateLocked(date)) return
        val updatedList = attendanceListState.map { it.copy(status = "Present") }
        attendanceListState = updatedList
        viewModelScope.launch {
            saveBulkAttendanceToDb(updatedList)
        }
    }

    fun markAllAbsent() {
        val date = attendanceSelectedDate
        if (isDateLocked(date)) return
        val updatedList = attendanceListState.map { it.copy(status = "Absent") }
        attendanceListState = updatedList
        viewModelScope.launch {
            saveBulkAttendanceToDb(updatedList)
        }
    }

    fun loadAttendanceForSelectedDate() {
        val date = attendanceSelectedDate
        val uid = currentUser.value?.uid ?: "default_contractor_uid"
        viewModelScope.launch {
            try {
                val finalizedList = repository.getAttendanceByDate(uid, date)
                val eligibleLabours = labours.value.filter { it.status == "Active" && it.repeatAutomatically }
                val currentSites = sites.value

                val isLocked = isDateLocked(date)

                if (finalizedList.isNotEmpty()) {
                    attendanceListState = eligibleLabours.map { labour ->
                        val siteName = currentSites.find { it.id == labour.siteId }?.name ?: "No Site"
                        val finalRecord = finalizedList.find { it.labourId == labour.id }
                        val finalStatus = finalRecord?.status ?: "Absent"
                        WorkingAttendanceState(
                            labourId = labour.id,
                            labourName = labour.name,
                            siteId = labour.siteId ?: 0,
                            siteName = siteName,
                            dailyWage = finalRecord?.dailyRate ?: labour.dailyWage,
                            skillType = labour.skillType,
                            status = finalStatus,
                            isFinalized = finalRecord?.finalized ?: isLocked,
                            lastEditedAt = finalRecord?.lastEditedAt,
                            lastEditedBy = finalRecord?.lastEditedBy,
                            version = finalRecord?.version ?: 1,
                            isEditedAfterFinalization = finalRecord?.lastEditedAt != null,
                            finalizedAt = finalRecord?.finalizedAt ?: finalRecord?.createdAt
                        )
                    }
                } else {
                    if (isLocked) {
                        attendanceListState = eligibleLabours.map { labour ->
                            val siteName = currentSites.find { it.id == labour.siteId }?.name ?: "No Site"
                            WorkingAttendanceState(
                                labourId = labour.id,
                                labourName = labour.name,
                                siteId = labour.siteId ?: 0,
                                siteName = siteName,
                                dailyWage = labour.dailyWage,
                                skillType = labour.skillType,
                                status = "Absent",
                                isFinalized = true
                            )
                        }
                    } else {
                        val updatedList = eligibleLabours.map { labour ->
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
                        attendanceListState = updatedList
                        saveBulkAttendanceToDb(updatedList)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Error loading attendance: ", e)
            }
        }
    }

    private fun checkAutoFinalizeTick() {
        val today = getCurrentDateString()
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        if (hour >= 20) {
            val uid = currentUser.value?.uid ?: return
            viewModelScope.launch {
                val todayAttendance = repository.getAttendanceByDate(uid, today)
                if (todayAttendance.isNotEmpty()) {
                    var updatedAny = false
                    todayAttendance.forEach { record ->
                        if (!record.finalized && !overrideUnlockedDates.contains(today)) {
                            val updatedRecord = record.copy(
                                finalized = true,
                                finalizedAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.insertAttendance(updatedRecord)
                            updatedAny = true
                        }
                    }
                    if (updatedAny) {
                        if (attendanceSelectedDate == today) {
                            loadAttendanceForSelectedDate()
                        }
                        showToast(if (isHindi) "⏰ शाम 8:00 बज चुके हैं। आज की हाजिरी अंतिम रूप से सहेज दी गई और लॉक कर दी गई है।" else "⏰ 8:00 PM cutoff reached. Today's attendance has been locked & finalized.")
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
            try {
                val uid = currentUser.value?.uid ?: "default_contractor_uid"
                val todayAttendance = repository.getAttendanceByDate(uid, today)

                if (isAutoFinalizeEnabled && hour >= 20 && todayAttendance.isEmpty()) {
                    val eligibleLabours = labours.value.filter { it.status == "Active" && it.repeatAutomatically }
                    if (eligibleLabours.isNotEmpty()) {
                        attendanceSelectedDate = today
                        loadAttendanceForSelectedDate()
                        kotlinx.coroutines.delay(1000)
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
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Error checking attendance on open: ", e)
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
                kotlinx.coroutines.withTimeout(30000) {
                    saveBulkAttendanceToDb(attendanceListState)
                    
                    showToast(if (isHindi) "आज की मजदूरी और उपस्थिति रिकॉर्ड लॉक और सुरक्षित की गई!" else "Today's wages and attendance finalized and locked successfully!")
                    showAttendanceDialog = false
                    loadAttendanceForSelectedDate()
                }
            } catch (e: Exception) {
                android.util.Log.e("LabourViewModel", "Firebase/Firestore save failed for daily wages/attendance", e)
                showToast("Error generating wages: ${e.localizedMessage ?: e.message}")
            } finally {
                isGeneratingWages = false
            }
        }
    }

    fun insertPaymentDirectly(payment: Payment, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertPayment(payment)
                showToast(if (isHindi) "विवरण सफलतापूर्वक सहेजा गया!" else "Record saved successfully!")
                onSuccess()
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deletePaymentDirectly(payment: Payment, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deletePayment(payment)
                showToast(if (isHindi) "विवरण निकाल दिया गया!" else "Record removed successfully!")
                onSuccess()
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            }
        }
    }

    fun insertSiteExpenseDirectly(expense: SiteExpense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertSiteExpense(expense)
                showToast(if (isHindi) "खर्च सफलतापूर्वक सहेजा गया!" else "Expense saved successfully!")
                onSuccess()
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            }
        }
    }

    fun updateSiteExpenseDirectly(expense: SiteExpense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateSiteExpense(expense)
                showToast(if (isHindi) "खर्च सफलतापूर्वक अपडेट किया गया!" else "Expense updated successfully!")
                onSuccess()
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteSiteExpenseDirectly(expense: SiteExpense, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteSiteExpense(expense)
                showToast(if (isHindi) "खर्च हटा दिया गया!" else "Expense deleted successfully!")
                onSuccess()
            } catch (e: Exception) {
                showToast("Error: ${e.localizedMessage}")
            }
        }
    }

    // ========================================================
    // ADVANCED GLOBAL FILTER SYSTEM STATE & LOGIC
    // ========================================================
    var filterDateMode by mutableStateOf("All") // "All", "Today", "Yesterday", "This Week", "Last Week", "This Month", "Last Month", "Last 3 Months", "Last 6 Months", "This Financial Year", "Previous Financial Year", "Custom", "MonthNavigator"
    var filterStartDate by mutableStateOf("") // "YYYY-MM-DD"
    var filterEndDate by mutableStateOf("") // "YYYY-MM-DD"

    // Month Navigation (defaults to local time May 2026)
    var filterNavMonth by mutableStateOf(5) // May (1-index)
    var filterNavYear by mutableStateOf(2026)

    // Filter properties
    var filterAttendanceStatus by mutableStateOf("All") // "All", "Full Day", "Half Day", "Absent", "Present", "Missing Attendance", "Auto-Generated Attendance", "Manually Added Attendance"
    var filterPaymentStatus by mutableStateOf("All") // "All", "Paid", "Pending", "Partially Paid", "Settled", "Unsettled"
    var filterAdvanceType by mutableStateOf("All") // "All", "Weekly Advance", "Emergency Advance", "Food Advance", "Petrol Advance", "Tool Advance", "Manual Advance"
    var filterExtraExpenseCategory by mutableStateOf("All") // "All", "Petrol", "Diesel", "Food", "Travel", "Accommodation", "Materials", "Equipment", "Miscellaneous"
    var filterSiteIdState by mutableStateOf<Int?>(null) // null = All Sites, Int = Specific Site
    var filterAmountMin by mutableStateOf<Double?>(null)
    var filterAmountMax by mutableStateOf<Double?>(null)
    var filterAmountRange by mutableStateOf("All") // "All", "0-500", "500-1000", "1000-5000", "5000+", "Custom"
    var filterLabourStatus by mutableStateOf("All") // "All", "Active", "Inactive", "Left Site", "On Leave"

    // Search and Sort properties
    var filterSearchQuery by mutableStateOf("")
    var filterSortingOption by mutableStateOf("Date (Newest First)") // "Date (Newest First)", "Date (Oldest First)", "Highest Amount", "Lowest Amount", "Highest Wage", "Most Advances", "Most Expenses"

    fun resetAllFilters() {
        filterDateMode = "All"
        filterStartDate = ""
        filterEndDate = ""
        filterNavMonth = 5
        filterNavYear = 2026
        filterAttendanceStatus = "All"
        filterPaymentStatus = "All"
        filterAdvanceType = "All"
        filterExtraExpenseCategory = "All"
        filterSiteIdState = null
        filterAmountMin = null
        filterAmountMax = null
        filterAmountRange = "All"
        filterLabourStatus = "All"
        filterSearchQuery = ""
        filterSortingOption = "Date (Newest First)"
    }

    fun getDateRangeBounds(): Pair<String?, String?> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(2026, java.util.Calendar.MAY, 29, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        when (filterDateMode) {
            "Today" -> {
                val dateStr = formatter.format(cal.time)
                return Pair(dateStr, dateStr)
            }
            "Yesterday" -> {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val dateStr = formatter.format(cal.time)
                return Pair(dateStr, dateStr)
            }
            "This Week" -> {
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                val start = formatter.format(cal.time)
                cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "Last Week" -> {
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                cal.add(java.util.Calendar.WEEK_OF_YEAR, -1)
                val start = formatter.format(cal.time)
                cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "This Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = formatter.format(cal.time)
                val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, maxDay)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "Last Month" -> {
                cal.add(java.util.Calendar.MONTH, -1)
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = formatter.format(cal.time)
                val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, maxDay)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "Last 3 Months" -> {
                cal.add(java.util.Calendar.MONTH, -2)
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = formatter.format(cal.time)
                cal.add(java.util.Calendar.MONTH, 2)
                val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, maxDay)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "Last 6 Months" -> {
                cal.add(java.util.Calendar.MONTH, -5)
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = formatter.format(cal.time)
                cal.add(java.util.Calendar.MONTH, 5)
                val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, maxDay)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "This Financial Year" -> {
                val currentMonth = cal.get(java.util.Calendar.MONTH)
                val fyStartYear = if (currentMonth >= java.util.Calendar.APRIL) cal.get(java.util.Calendar.YEAR) else cal.get(java.util.Calendar.YEAR) - 1
                cal.set(fyStartYear, java.util.Calendar.APRIL, 1)
                val start = formatter.format(cal.time)
                cal.set(fyStartYear + 1, java.util.Calendar.MARCH, 31)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "Previous Financial Year" -> {
                val currentMonth = cal.get(java.util.Calendar.MONTH)
                val fyStartYear = (if (currentMonth >= java.util.Calendar.APRIL) cal.get(java.util.Calendar.YEAR) else cal.get(java.util.Calendar.YEAR) - 1) - 1
                cal.set(fyStartYear, java.util.Calendar.APRIL, 1)
                val start = formatter.format(cal.time)
                cal.set(fyStartYear + 1, java.util.Calendar.MARCH, 31)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            "Custom" -> {
                val start = if (filterStartDate.isNotBlank()) filterStartDate else null
                val end = if (filterEndDate.isNotBlank()) filterEndDate else null
                return Pair(start, end)
            }
            "MonthNavigator" -> {
                cal.set(java.util.Calendar.YEAR, filterNavYear)
                cal.set(java.util.Calendar.MONTH, filterNavMonth - 1)
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = formatter.format(cal.time)
                val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, maxDay)
                val end = formatter.format(cal.time)
                return Pair(start, end)
            }
            else -> return Pair(null, null)
        }
    }

    fun isDateInRange(dateStr: String, start: String?, end: String?): Boolean {
        // Support both "YYYY-MM-DD" and "dd/MM/yyyy" safely or sub-matching
        val standardDate = if (dateStr.contains("/")) {
            try {
                val parts = dateStr.split("/")
                val d = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                val y = parts[2]
                "$y-$m-$d"
            } catch (e: Exception) {
                dateStr
            }
        } else {
            dateStr
        }

        if (start != null && standardDate < start) return false
        if (end != null && standardDate > end) return false
        return true
    }

    fun getFilteredAttendances(labourId: Int?, rawList: List<Attendance>): List<Attendance> {
        val bounds = getDateRangeBounds()
        return rawList.filter { record ->
            val matchesLabour = labourId == null || record.labourId == labourId
            val matchesSite = filterSiteIdState == null || record.siteId == filterSiteIdState
            val matchesDate = isDateInRange(record.date, bounds.first, bounds.second)
            
            val matchesStatus = when (filterAttendanceStatus) {
                "All" -> true
                "Full Day" -> record.status == "Present" && record.attendanceType == "Full Day"
                "Half Day" -> record.status == "Half Day"
                "Absent" -> record.status == "Absent"
                "Present" -> record.status == "Present" || record.status == "Half Day"
                "Auto-Generated Attendance" -> record.autoGenerated
                "Manually Added Attendance" -> !record.autoGenerated
                else -> true
            }

            // Global search query matching
            val matchesSearch = if (filterSearchQuery.isBlank()) true else {
                val q = filterSearchQuery.lowercase().trim()
                record.date.contains(q) || 
                record.status.lowercase().contains(q) ||
                record.attendanceType.lowercase().contains(q) ||
                record.dailyRate.toString().contains(q)
            }

            matchesLabour && matchesSite && matchesDate && matchesStatus && matchesSearch
        }.let { list ->
            when (filterSortingOption) {
                "Date (Newest First)" -> list.sortedByDescending { it.date }
                "Date (Oldest First)" -> list.sortedBy { it.date }
                "Highest Wage" -> list.sortedByDescending { it.earnedAmount }
                else -> list.sortedByDescending { it.date }
            }
        }
    }

    fun getFilteredPayments(labourId: Int?, rawList: List<Payment>): List<Payment> {
        val bounds = getDateRangeBounds()
        return rawList.filter { record ->
            val matchesLabour = labourId == null || record.labourId == labourId
            val matchesSite = filterSiteIdState == null || record.siteId == filterSiteIdState
            val matchesDate = isDateInRange(record.date, bounds.first, bounds.second)
            
            // Payment type / status filter
            val matchesPaymentType = when (filterPaymentStatus) {
                "All" -> true
                "Paid" -> record.paymentType == "Daily Wage (Kharchi)" || record.paymentType.contains("Paid", ignoreCase = true)
                "Pending" -> record.status.contains("Pending", ignoreCase = true)
                "Partially Paid" -> record.status.contains("Partially", ignoreCase = true)
                "Settled" -> record.status.contains("Settled", ignoreCase = true) || record.paymentType.contains("Settle", ignoreCase = true)
                "Unsettled" -> record.status.contains("Unsettled", ignoreCase = true)
                else -> true
            }

            val matchesAdvance = when (filterAdvanceType) {
                "All" -> true
                "Weekly Advance" -> record.paymentType == "Weekly Advance"
                "Emergency Advance" -> record.paymentType == "Emergency Advance"
                "Food Advance" -> record.paymentType == "Food Advance"
                "Petrol Advance" -> record.paymentType == "Petrol Advance" || (record.paymentType.contains("Petrol", ignoreCase = true) && record.paymentType.contains("Advance", ignoreCase = true))
                "Tool Advance" -> record.paymentType == "Tool Advance"
                "Manual Advance" -> record.paymentType == "Manual Advance" || record.paymentType == "Advance"
                else -> true
            }

            // Extra Expense Category Filter
            val matchesExtraExpense = when (filterExtraExpenseCategory) {
                "All" -> true
                "Petrol" -> record.remarks.lowercase().contains("petrol") || record.remarks.lowercase().contains("fuel") || record.paymentType.contains("Petrol", ignoreCase = true)
                "Diesel" -> record.remarks.lowercase().contains("diesel") || record.paymentType.contains("Diesel", ignoreCase = true)
                "Food" -> record.remarks.lowercase().contains("food") || record.remarks.lowercase().contains("meal") || record.remarks.lowercase().contains("tea") || record.paymentType.contains("Food", ignoreCase = true)
                "Travel" -> record.remarks.lowercase().contains("travel") || record.remarks.lowercase().contains("fare") || record.paymentType.contains("Travel", ignoreCase = true)
                "Accommodation" -> record.remarks.lowercase().contains("stay") || record.remarks.lowercase().contains("room") || record.paymentType.contains("Accommodation", ignoreCase = true)
                "Materials" -> record.remarks.lowercase().contains("material") || record.paymentType.contains("Material", ignoreCase = true)
                "Equipment" -> record.remarks.lowercase().contains("tool") || record.remarks.lowercase().contains("equipment") || record.paymentType.contains("Equipment", ignoreCase = true)
                "Miscellaneous" -> true
                else -> true
            }

            // Amount Range Filter
            val matchesAmountRange = when (filterAmountRange) {
                "All" -> true
                "0-500" -> record.amount in 0.0..500.0
                "500-1000" -> record.amount in 500.0..1000.0
                "1000-5000" -> record.amount in 1000.0..5000.0
                "5000+" -> record.amount >= 5000.0
                "Custom" -> {
                    val min = filterAmountMin ?: 0.0
                    val max = filterAmountMax ?: Double.MAX_VALUE
                    record.amount >= min && record.amount <= max
                }
                else -> true
            }

            // Global search query matching Remarks, Amount, PaymentType, Date, Transaction ID
            val matchesSearch = if (filterSearchQuery.isBlank()) true else {
                val q = filterSearchQuery.lowercase().trim()
                record.remarks.lowercase().contains(q) ||
                record.amount.toString().contains(q) ||
                record.paymentType.lowercase().contains(q) ||
                record.paymentMode.lowercase().contains(q) ||
                record.date.contains(q) ||
                record.id.toString().contains(q) ||
                record.labourName.lowercase().contains(q)
            }

            matchesSite && matchesDate && matchesPaymentType && matchesAdvance && matchesExtraExpense && matchesAmountRange && matchesSearch
        }.let { list ->
            when (filterSortingOption) {
                "Date (Newest First)" -> list.sortedByDescending { it.date }
                "Date (Oldest First)" -> list.sortedBy { it.date }
                "Highest Amount" -> list.sortedByDescending { it.amount }
                "Lowest Amount" -> list.sortedBy { it.amount }
                "Most Advances" -> list.filter { it.paymentType.contains("Advance", ignoreCase = true) }.sortedByDescending { it.amount }
                "Most Expenses" -> list.filter { it.paymentType.contains("Expense", ignoreCase = true) }.sortedByDescending { it.amount }
                else -> list.sortedByDescending { it.date }
            }
        }
    }

    fun getFilteredSiteExpenses(rawList: List<SiteExpense>): List<SiteExpense> {
        val bounds = getDateRangeBounds()
        return rawList.filter { record ->
            val matchesSite = filterSiteIdState == null || record.siteId == filterSiteIdState
            val matchesDate = isDateInRange(record.expenseDate, bounds.first, bounds.second)
            
            // Extra Expense Category Filter
            val matchesCategory = when (filterExtraExpenseCategory) {
                "All" -> true
                "Petrol" -> record.category.lowercase().contains("fuel") || record.category.lowercase().contains("petrol") || record.expenseName.lowercase().contains("petrol")
                "Diesel" -> record.category.lowercase().contains("diesel") || record.category.lowercase().contains("fuel") || record.expenseName.lowercase().contains("diesel")
                "Food" -> record.category.lowercase().contains("food") || record.expenseName.lowercase().contains("food")
                "Travel" -> record.category.lowercase().contains("travel") || record.expenseName.lowercase().contains("travel")
                "Accommodation" -> record.category.lowercase().contains("stay") || record.category.lowercase().contains("accommodation")
                "Materials" -> record.category.lowercase().contains("material") || record.expenseName.lowercase().contains("material")
                "Equipment" -> record.category.lowercase().contains("tool") || record.category.lowercase().contains("equipment")
                "Miscellaneous" -> true
                else -> true
            }

            val matchesAmountRange = when (filterAmountRange) {
                "All" -> true
                "0-500" -> record.amount in 0.0..500.0
                "500-1000" -> record.amount in 500.0..1000.0
                "1000-5000" -> record.amount in 1000.0..5000.0
                "5000+" -> record.amount >= 5000.0
                "Custom" -> {
                    val min = filterAmountMin ?: 0.0
                    val max = filterAmountMax ?: Double.MAX_VALUE
                    record.amount >= min && record.amount <= max
                }
                else -> true
            }

            val matchesSearch = if (filterSearchQuery.isBlank()) true else {
                val q = filterSearchQuery.lowercase().trim()
                record.expenseName.lowercase().contains(q) ||
                record.paidTo.lowercase().contains(q) ||
                record.amount.toString().contains(q) ||
                record.category.lowercase().contains(q) ||
                record.description.lowercase().contains(q) ||
                record.expenseDate.contains(q)
            }

            matchesSite && matchesDate && matchesCategory && matchesAmountRange && matchesSearch
        }.let { list ->
            when (filterSortingOption) {
                "Date (Newest First)" -> list.sortedByDescending { it.expenseDate }
                "Date (Oldest First)" -> list.sortedBy { it.expenseDate }
                "Highest Amount" -> list.sortedByDescending { it.amount }
                "Lowest Amount" -> list.sortedBy { it.amount }
                else -> list.sortedByDescending { it.expenseDate }
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
    val isFinalized: Boolean = false,
    val lastEditedAt: Long? = null,
    val lastEditedBy: String? = null,
    val version: Int = 1,
    val isEditedAfterFinalization: Boolean = false,
    val finalizedAt: Long? = null
) {
    val isPresent: Boolean get() = status == "Present" || status == "Half Day"
}
