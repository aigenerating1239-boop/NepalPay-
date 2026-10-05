package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApiErrorLogEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.KycSubmissionEntity
import com.example.data.local.LinkedBankAccountEntity
import com.example.data.local.MoneyRequestEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.ReconciliationLogEntity
import com.example.data.local.SandboxMerchantQr
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.NepalPayRepository
import com.example.data.security.SecurityManager
import com.example.data.service.AccountVerificationResult
import com.example.data.service.ParsedQrResult
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH,
    HOME,
    PAYMENTS_HUB,
    BANKS_DIRECTORY,
    LINK_BANK_ACCOUNT,
    SEND_MONEY,
    RECEIVE_MONEY,
    QR_SCAN_PAY,
    WALLET,
    TRANSACTIONS,
    NOTIFICATIONS,
    PROFILE,
    KYC_VERIFICATION,
    ADMIN_DASHBOARD,
    SECURITY_AUDIT
}

enum class SendMoneyMode {
    MOBILE,
    PAYMENT_ID,
    QR_CODE,
    BANK_ACCOUNT
}

enum class WalletActionTab {
    OVERVIEW,
    ADD_MONEY,
    WITHDRAW_MONEY,
    BANK_TRANSFER
}

@OptIn(ExperimentalCoroutinesApi::class)
class NepalPayViewModel(
    private val repository: NepalPayRepository
) : ViewModel() {

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _navigationBackStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.HOME))

    private val _activeUserMobile = MutableStateFlow("+9779841234567")
    val activeUserMobile: StateFlow<String> = _activeUserMobile.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _balanceVisible = MutableStateFlow(true)
    val balanceVisible: StateFlow<Boolean> = _balanceVisible.asStateFlow()

    private val _selectedSendMode = MutableStateFlow(SendMoneyMode.MOBILE)
    val selectedSendMode: StateFlow<SendMoneyMode> = _selectedSendMode.asStateFlow()

    private val _selectedBankForAction = MutableStateFlow<BankDirectoryEntity?>(null)
    val selectedBankForAction: StateFlow<BankDirectoryEntity?> = _selectedBankForAction.asStateFlow()

    private val _selectedWalletTab = MutableStateFlow(WalletActionTab.OVERVIEW)
    val selectedWalletTab: StateFlow<WalletActionTab> = _selectedWalletTab.asStateFlow()

    private val _activeReceiptTx = MutableStateFlow<TransactionEntity?>(null)
    val activeReceiptTx: StateFlow<TransactionEntity?> = _activeReceiptTx.asStateFlow()

    private val _prefilledQrTarget = MutableStateFlow<ParsedQrResult?>(null)
    val prefilledQrTarget: StateFlow<ParsedQrResult?> = _prefilledQrTarget.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _lastActivityTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastActivityTimestamp: StateFlow<Long> = _lastActivityTimestamp.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _activeUserMobile
        .flatMapLatest { mobile ->
            if (mobile.isBlank()) flowOf(null) else repository.observeUser(mobile)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allBanks: StateFlow<List<BankDirectoryEntity>> = repository.allBanksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val linkedBankAccounts: StateFlow<List<LinkedBankAccountEntity>> = _activeUserMobile
        .flatMapLatest { mobile ->
            if (mobile.isBlank()) flowOf(emptyList()) else repository.observeLinkedBankAccounts(mobile)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userTransactions: StateFlow<List<TransactionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.observeUserTransactions(user.mobileNumber, user.paymentId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moneyRequests: StateFlow<List<MoneyRequestEntity>> = _activeUserMobile
        .flatMapLatest { mobile ->
            if (mobile.isBlank()) flowOf(emptyList()) else repository.observeMoneyRequests(mobile)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userKycSubmission: StateFlow<KycSubmissionEntity?> = _activeUserMobile
        .flatMapLatest { mobile ->
            if (mobile.isBlank()) flowOf(null) else repository.observeKyc(mobile)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userNotifications: StateFlow<List<NotificationEntity>> = _activeUserMobile
        .flatMapLatest { mobile ->
            if (mobile.isBlank()) flowOf(emptyList()) else repository.observeNotifications(mobile)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Flows
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKycSubmissions: StateFlow<List<KycSubmissionEntity>> = repository.allKycSubmissionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reconciliationLogs: StateFlow<List<ReconciliationLogEntity>> = repository.reconciliationLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiErrorLogs: StateFlow<List<ApiErrorLogEntity>> = repository.apiErrorLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sandboxMerchants: List<SandboxMerchantQr> get() = repository.sandboxMerchants

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    fun recordUserInteraction() {
        _lastActivityTimestamp.value = System.currentTimeMillis()
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.ENGLISH) {
            AppLanguage.NEPALI
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun toggleDarkTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun toggleNotificationsEnabled() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }

    fun toggleBalanceVisibility() {
        _balanceVisible.value = !_balanceVisible.value
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun showReceipt(tx: TransactionEntity?) {
        _activeReceiptTx.value = tx
    }

    fun navigateTo(screen: AppScreen) {
        recordUserInteraction()
        if (_currentScreen.value != screen) {
            _navigationBackStack.value = (_navigationBackStack.value + screen).takeLast(12)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        val stack = _navigationBackStack.value
        if (stack.size > 1) {
            val updated = stack.dropLast(1)
            _navigationBackStack.value = updated
            _currentScreen.value = updated.last()
            return true
        }
        if (_currentScreen.value != AppScreen.HOME && _isAuthenticated.value) {
            _currentScreen.value = AppScreen.HOME
            _navigationBackStack.value = listOf(AppScreen.HOME)
            return true
        }
        return false
    }

    fun openSendMoney(mode: SendMoneyMode, preselectedBank: BankDirectoryEntity? = null) {
        _selectedSendMode.value = mode
        if (preselectedBank != null) {
            _selectedBankForAction.value = preselectedBank
        }
        navigateTo(AppScreen.SEND_MONEY)
    }

    fun openLinkBankAccount(preselectedBank: BankDirectoryEntity? = null) {
        _selectedBankForAction.value = preselectedBank
        navigateTo(AppScreen.LINK_BANK_ACCOUNT)
    }

    fun openWalletWithTab(tab: WalletActionTab) {
        _selectedWalletTab.value = tab
        navigateTo(AppScreen.WALLET)
    }

    fun lockSession() {
        _isAuthenticated.value = false
        _currentScreen.value = AppScreen.AUTH
        _navigationBackStack.value = listOf(AppScreen.AUTH)
    }

    fun logout() {
        lockSession()
        showToast(
            if (_language.value == AppLanguage.NEPALI) "सुरक्षित रूपमा लगआउट भयो"
            else "Logged out securely from NepalPay Sandbox"
        )
    }

    fun switchDemoAccount(mobile: String) {
        _activeUserMobile.value = SecurityManager.normalizeNepalMobile(mobile)
        _isAuthenticated.value = true
        _currentScreen.value = AppScreen.HOME
        _navigationBackStack.value = listOf(AppScreen.HOME)
        showToast("Switched Sandbox Account: $mobile")
    }

    suspend fun requestOtp(mobile: String): String {
        return repository.requestSandboxOtp(SecurityManager.normalizeNepalMobile(mobile))
    }

    suspend fun verifyBankAccountViaConnector(
        bank: BankDirectoryEntity,
        accountNumber: String,
        holderName: String
    ): AccountVerificationResult {
        return repository.verifyBankAccountHolder(bank, accountNumber, holderName)
    }

    fun completeBankAccountLinking(
        bank: BankDirectoryEntity,
        accountNumberPlain: String,
        verifiedHolderName: String,
        isPrimary: Boolean,
        onComplete: (LinkedBankAccountEntity) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val linked = repository.linkBankAccount(
                user = user,
                bank = bank,
                accountNumberPlain = accountNumberPlain,
                verifiedHolderName = verifiedHolderName,
                isPrimary = isPrimary
            )
            showToast("Linked ${bank.officialNameEn} (${linked.accountNumberMasked})")
            onComplete(linked)
        }
    }

    fun removeLinkedBankAccount(linkId: String) {
        val mobile = _activeUserMobile.value
        if (mobile.isBlank()) return
        viewModelScope.launch {
            repository.unlinkBankAccount(mobile, linkId)
            showToast("Bank account unlinked")
        }
    }

    fun verifyMpinForCurrentUser(inputMpin: String): Pair<Boolean, String> {
        val user = currentUser.value ?: return false to "User session not found"
        val (allowed, waitSec) = SecurityManager.checkRateLimit(user.mobileNumber)
        if (!allowed) {
            return false to "Too many failed attempts. Please wait ${waitSec}s (Rate limit protection)."
        }
        val valid = SecurityManager.verifyMpin(inputMpin, user.mpinHash, user.mpinSalt)
        val attempts = SecurityManager.recordAuthResult(user.mobileNumber, valid)
        if (!valid) {
            viewModelScope.launch {
                repository.logSecurityEvent(
                    user.mobileNumber,
                    "MPIN_FAILED",
                    "Invalid MPIN attempt #$attempts",
                    if (attempts >= 3) "CRITICAL" else "WARNING"
                )
            }
            return false to "Invalid MPIN (Attempt $attempts/4). Sandbox default MPIN is 1234."
        }
        recordUserInteraction()
        return true to "Verified"
    }

    fun loginWithMpinOrBiometric(
        mobileRaw: String,
        mpinInput: String,
        useBiometric: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val normalized = SecurityManager.normalizeNepalMobile(mobileRaw)
            val (allowed, waitSec) = SecurityManager.checkRateLimit(normalized)
            if (!allowed) {
                onResult(false, "Account temporarily locked for ${waitSec}s due to rate-limiting.")
                return@launch
            }
            val user = repository.getUserByMobile(normalized)
            if (user == null) {
                onResult(false, "Account not found for $normalized. Please Register first.")
                return@launch
            }
            if (user.isSuspended) {
                onResult(false, "Account $normalized is suspended by Compliance Admin.")
                return@launch
            }
            val verified = if (useBiometric && user.biometricEnabled) {
                true
            } else {
                SecurityManager.verifyMpin(mpinInput, user.mpinHash, user.mpinSalt)
            }
            val attempts = SecurityManager.recordAuthResult(normalized, verified)
            if (verified) {
                _activeUserMobile.value = normalized
                _isAuthenticated.value = true
                _currentScreen.value = AppScreen.HOME
                _navigationBackStack.value = listOf(AppScreen.HOME)
                repository.logSecurityEvent(
                    normalized,
                    if (useBiometric) "BIOMETRIC_LOGIN" else "MPIN_LOGIN",
                    "Authenticated session started",
                    "INFO"
                )
                onResult(true, "Welcome back, ${user.fullName}")
            } else {
                repository.logSecurityEvent(
                    normalized,
                    "AUTH_FAILED",
                    "Failed login attempt #$attempts",
                    "WARNING"
                )
                onResult(false, "Incorrect MPIN (Attempt $attempts/4). Sandbox MPIN: 1234")
            }
        }
    }

    fun registerOrRecoverAccount(
        mobileRaw: String,
        fullName: String,
        email: String,
        newMpin: String,
        biometricEnabled: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = repository.registerOrResetUser(
                mobileRaw = mobileRaw,
                fullName = fullName,
                email = email,
                plainMpin = newMpin,
                biometricEnabled = biometricEnabled
            )
            _activeUserMobile.value = user.mobileNumber
            _isAuthenticated.value = true
            _currentScreen.value = AppScreen.HOME
            _navigationBackStack.value = listOf(AppScreen.HOME)
            showToast("Account ready for ${user.fullName} (${user.mobileNumber})")
            onSuccess()
        }
    }

    fun resolveReceiverPreview(identifier: String, onResolved: (String, String) -> Unit) {
        viewModelScope.launch {
            val (id, name) = repository.findReceiver(identifier)
            onResolved(id, name)
        }
    }

    fun parseQrCode(payload: String): ParsedQrResult = repository.parseQrCode(payload)

    fun setPrefilledQrTarget(target: ParsedQrResult?) {
        _prefilledQrTarget.value = target
    }

    fun buildPersonalQr(user: UserEntity): String = repository.buildPersonalQrPayload(user)

    fun processTransaction(
        receiverIdentifier: String,
        receiverName: String,
        amountNpr: Double,
        feeNpr: Double,
        note: String,
        txType: String,
        paymentMethod: String,
        bankName: String = "",
        bankId: String = "",
        simulateStatus: String = "SUCCESSFUL",
        onCompleted: (Result<TransactionEntity>) -> Unit
    ) {
        val sender = currentUser.value
        if (sender == null) {
            onCompleted(Result.failure(IllegalStateException("Not logged in")))
            return
        }
        viewModelScope.launch {
            val result = repository.executePayment(
                sender = sender,
                receiverIdentifier = receiverIdentifier,
                receiverName = receiverName,
                amountNpr = amountNpr,
                feeNpr = feeNpr,
                note = note,
                txType = txType,
                paymentMethod = paymentMethod,
                bankName = bankName,
                bankId = bankId,
                simulateStatus = simulateStatus
            )
            result.onSuccess { tx ->
                _activeReceiptTx.value = tx
            }.onFailure { err ->
                showToast(err.message ?: "Transaction failed")
            }
            onCompleted(result)
        }
    }

    fun createMoneyRequest(
        payerMobile: String,
        amountNpr: Double,
        note: String,
        onDone: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.createMoneyRequest(user, payerMobile, amountNpr, note)
            showToast(
                if (_language.value == AppLanguage.NEPALI) "रकम अनुरोध सफलतापूर्वक पठाइयो"
                else "Money request sent in Sandbox"
            )
            onDone()
        }
    }

    fun respondToMoneyRequest(request: MoneyRequestEntity, approve: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.respondToMoneyRequest(user, request, approve)
            result.onSuccess { tx ->
                if (tx != null) {
                    _activeReceiptTx.value = tx
                } else {
                    showToast("Money request declined")
                }
            }.onFailure {
                showToast(it.message ?: "Unable to process request")
            }
        }
    }

    fun submitKycApplication(
        fullName: String,
        dob: String,
        province: String,
        district: String,
        municipality: String,
        wardNo: String,
        docType: String,
        docNumber: String,
        docFileName: String,
        selfieVerified: Boolean
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitKyc(
                user = user,
                fullName = fullName,
                dob = dob,
                province = province,
                district = district,
                municipality = municipality,
                wardNo = wardNo,
                docType = docType,
                docNumberPlain = docNumber,
                docFileName = docFileName,
                selfieVerified = selfieVerified
            )
            showToast(
                if (_language.value == AppLanguage.NEPALI) "KYC विवरण सुरक्षित रूपमा पेश गरियो"
                else "KYC submitted with AES-256 encryption for NRB review"
            )
        }
    }

    fun updateMpin(currentMpin: String, newMpin: String, onDone: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        val (valid, msg) = verifyMpinForCurrentUser(currentMpin)
        if (!valid) {
            onDone(false, msg)
            return
        }
        viewModelScope.launch {
            repository.changeMpin(user, newMpin)
            onDone(true, "MPIN updated successfully")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleBiometric(user, enabled)
            showToast(if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled")
        }
    }

    fun markAllNotificationsRead() {
        val mobile = _activeUserMobile.value
        if (mobile.isBlank()) return
        viewModelScope.launch {
            repository.markNotificationsRead(mobile)
        }
    }

    // Admin actions for Dynamic Banks Directory, Reconciliation, and Error Logs
    fun adminSaveOrUpdateBank(bank: BankDirectoryEntity) {
        viewModelScope.launch {
            repository.adminSaveOrUpdateBank(bank)
            showToast("Saved ${bank.officialNameEn} to editable Bank Directory DB")
        }
    }

    fun adminToggleBankIntegration(bank: BankDirectoryEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.adminToggleBankIntegration(bank, enabled)
            showToast(
                if (enabled) "Enabled ${bank.shortName} API connector"
                else "Disabled ${bank.shortName} API connector"
            )
        }
    }

    fun adminVerifyWithNrbRegistry() {
        viewModelScope.launch {
            repository.adminVerifyAllBanksWithNrb()
            showToast("Verified all licensed banks with Nepal Rastra Bank (NRB) registry")
        }
    }

    fun adminRunBankReconciliation(bank: BankDirectoryEntity) {
        viewModelScope.launch {
            repository.adminRunReconciliation(bank)
            showToast("Reconciliation completed for ${bank.officialNameEn}")
        }
    }

    fun adminResolveErrorLog(errorId: Long) {
        viewModelScope.launch {
            repository.adminResolveApiError(errorId)
            showToast("Marked API Error #$errorId as resolved")
        }
    }

    fun adminSetUserSuspended(mobile: String, suspended: Boolean) {
        viewModelScope.launch {
            repository.adminToggleUserSuspension(mobile, suspended)
            showToast(if (suspended) "Suspended $mobile" else "Reactivated $mobile")
        }
    }

    fun adminUpdateKyc(mobile: String, status: String, note: String) {
        viewModelScope.launch {
            repository.adminReviewKyc(mobile, status, note)
            showToast("Updated KYC for $mobile to $status")
        }
    }

    fun adminUpdateTxStatus(txId: String, status: String) {
        viewModelScope.launch {
            repository.adminUpdateTransactionStatus(txId, status)
            showToast("Transaction $txId marked $status")
        }
    }

    class Factory(private val repository: NepalPayRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NepalPayViewModel(repository) as T
        }
    }
}
