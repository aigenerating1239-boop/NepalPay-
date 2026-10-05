package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.NepalPayDatabase
import com.example.data.repository.NepalPayRepository
import com.example.data.security.SecurityManager
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.SandboxComplianceBanner
import com.example.ui.i18n.tr
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KycScreen
import com.example.ui.screens.LinkBankAccountScreen
import com.example.ui.screens.NepalBankDirectoryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PaymentsHubScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QrScanPayScreen
import com.example.ui.screens.ReceiveMoneyScreen
import com.example.ui.screens.SecurityAuditScreen
import com.example.ui.screens.SendMoneyScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NepalCrimson
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.NepalPayViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NepalPayRootApp()
        }
    }
}

@Composable
fun NepalPayRootApp() {
    val context = LocalContext.current
    val repository = remember {
        val db = NepalPayDatabase.getDatabase(context)
        NepalPayRepository(db.nepalPayDao(), appContext = context.applicationContext)
    }
    val viewModel: NepalPayViewModel = viewModel(
        factory = NepalPayViewModel.Factory(repository)
    )

    val lang by viewModel.language.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allBanks by viewModel.allBanks.collectAsStateWithLifecycle()
    val linkedBankAccounts by viewModel.linkedBankAccounts.collectAsStateWithLifecycle()
    val userTransactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val moneyRequests by viewModel.moneyRequests.collectAsStateWithLifecycle()
    val userKyc by viewModel.userKycSubmission.collectAsStateWithLifecycle()
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val balanceVisible by viewModel.balanceVisible.collectAsStateWithLifecycle()
    val selectedSendMode by viewModel.selectedSendMode.collectAsStateWithLifecycle()
    val selectedBankForAction by viewModel.selectedBankForAction.collectAsStateWithLifecycle()
    val selectedWalletTab by viewModel.selectedWalletTab.collectAsStateWithLifecycle()
    val activeReceiptTx by viewModel.activeReceiptTx.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val lastActivity by viewModel.lastActivityTimestamp.collectAsStateWithLifecycle()

    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allKycSubmissions by viewModel.allKycSubmissions.collectAsStateWithLifecycle()
    val reconciliationLogs by viewModel.reconciliationLogs.collectAsStateWithLifecycle()
    val apiErrorLogs by viewModel.apiErrorLogs.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isAuthenticated, lastActivity) {
        if (isAuthenticated) {
            delay(SecurityManager.SESSION_TIMEOUT_MS)
            val elapsed = System.currentTimeMillis() - viewModel.lastActivityTimestamp.value
            if (elapsed >= SecurityManager.SESSION_TIMEOUT_MS) {
                viewModel.lockSession()
                viewModel.showToast("Session timed out for security. Please unlock with MPIN or Biometric.")
            }
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    MyApplicationTheme(darkTheme = darkTheme) {
        if (!isAuthenticated || currentScreen == AppScreen.AUTH) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AuthScreen(viewModel = viewModel, lang = lang)
                }
            }
        } else {
            val unreadCount = userNotifications.count { !it.isRead }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { viewModel.navigateTo(AppScreen.QR_SCAN_PAY) },
                        containerColor = NepalCrimson,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                        modifier = Modifier.testTag("fab_qr_scan_pay")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan & Pay QR"
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HOME,
                            onClick = { viewModel.navigateTo(AppScreen.HOME) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.HOME) Icons.Filled.Home
                                    else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text(tr(lang, "Home", "होम")) },
                            modifier = Modifier.testTag("nav_home")
                        )

                        val isBanksActive = currentScreen in listOf(
                            AppScreen.BANKS_DIRECTORY,
                            AppScreen.LINK_BANK_ACCOUNT
                        )
                        NavigationBarItem(
                            selected = isBanksActive,
                            onClick = { viewModel.navigateTo(AppScreen.BANKS_DIRECTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (isBanksActive) Icons.Filled.AccountBalance
                                    else Icons.Outlined.AccountBalance,
                                    contentDescription = "Nepal Banks"
                                )
                            },
                            label = { Text(tr(lang, "Banks", "बैंक")) },
                            modifier = Modifier.testTag("nav_banks")
                        )

                        val isPaymentsActive = currentScreen in listOf(
                            AppScreen.PAYMENTS_HUB,
                            AppScreen.SEND_MONEY,
                            AppScreen.RECEIVE_MONEY,
                            AppScreen.QR_SCAN_PAY,
                            AppScreen.WALLET
                        )
                        NavigationBarItem(
                            selected = isPaymentsActive,
                            onClick = { viewModel.navigateTo(AppScreen.PAYMENTS_HUB) },
                            icon = {
                                Icon(
                                    imageVector = if (isPaymentsActive) Icons.Filled.AccountBalanceWallet
                                    else Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "Payments"
                                )
                            },
                            label = { Text(tr(lang, "Payments", "भुक्तानी")) },
                            modifier = Modifier.testTag("nav_payments")
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.TRANSACTIONS,
                            onClick = { viewModel.navigateTo(AppScreen.TRANSACTIONS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.TRANSACTIONS) Icons.Filled.ReceiptLong
                                    else Icons.Outlined.ReceiptLong,
                                    contentDescription = "Transactions"
                                )
                            },
                            label = { Text(tr(lang, "History", "कारोबार")) },
                            modifier = Modifier.testTag("nav_transactions")
                        )

                        val isProfileActive = currentScreen in listOf(
                            AppScreen.PROFILE,
                            AppScreen.KYC_VERIFICATION,
                            AppScreen.ADMIN_DASHBOARD,
                            AppScreen.SECURITY_AUDIT
                        )
                        NavigationBarItem(
                            selected = isProfileActive,
                            onClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                            icon = {
                                Icon(
                                    imageVector = if (isProfileActive) Icons.Filled.Person
                                    else Icons.Outlined.Person,
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text(tr(lang, "Profile", "प्रोफाइल")) },
                            modifier = Modifier.testTag("nav_profile")
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SandboxComplianceBanner(lang = lang)

                    Crossfade(
                        targetState = currentScreen,
                        label = "nepalpay_screen_transition",
                        modifier = Modifier.weight(1f)
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> HomeScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                banksCount = allBanks.size,
                                linkedAccounts = linkedBankAccounts,
                                transactions = userTransactions,
                                moneyRequests = moneyRequests,
                                unreadNotificationCount = unreadCount,
                                balanceVisible = balanceVisible,
                                lang = lang
                            )

                            AppScreen.BANKS_DIRECTORY -> NepalBankDirectoryScreen(
                                viewModel = viewModel,
                                banks = allBanks,
                                linkedAccounts = linkedBankAccounts,
                                lang = lang
                            )

                            AppScreen.LINK_BANK_ACCOUNT -> LinkBankAccountScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                banks = allBanks,
                                linkedAccounts = linkedBankAccounts,
                                preselectedBank = selectedBankForAction,
                                lang = lang
                            )

                            AppScreen.PAYMENTS_HUB -> PaymentsHubScreen(
                                viewModel = viewModel,
                                banksCount = allBanks.size,
                                lang = lang
                            )

                            AppScreen.SEND_MONEY -> SendMoneyScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                banks = allBanks,
                                preselectedBank = selectedBankForAction,
                                initialMode = selectedSendMode,
                                lang = lang
                            )

                            AppScreen.QR_SCAN_PAY -> QrScanPayScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                lang = lang
                            )

                            AppScreen.RECEIVE_MONEY -> ReceiveMoneyScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                lang = lang
                            )

                            AppScreen.WALLET -> WalletScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                banks = allBanks,
                                transactions = userTransactions,
                                initialTab = selectedWalletTab,
                                lang = lang
                            )

                            AppScreen.TRANSACTIONS -> TransactionsScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                transactions = userTransactions,
                                lang = lang
                            )

                            AppScreen.NOTIFICATIONS -> NotificationsScreen(
                                viewModel = viewModel,
                                notifications = userNotifications,
                                lang = lang
                            )

                            AppScreen.PROFILE -> ProfileScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                linkedAccounts = linkedBankAccounts,
                                darkTheme = darkTheme,
                                notificationsEnabled = notificationsEnabled,
                                lang = lang
                            )

                            AppScreen.KYC_VERIFICATION -> KycScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                existingKyc = userKyc,
                                lang = lang
                            )

                            AppScreen.ADMIN_DASHBOARD -> AdminDashboardScreen(
                                viewModel = viewModel,
                                allBanks = allBanks,
                                allUsers = allUsers,
                                allTransactions = allTransactions,
                                allKycSubmissions = allKycSubmissions,
                                reconciliationLogs = reconciliationLogs,
                                apiErrorLogs = apiErrorLogs,
                                lang = lang
                            )

                            AppScreen.SECURITY_AUDIT -> SecurityAuditScreen(
                                viewModel = viewModel,
                                user = currentUser,
                                auditLogs = auditLogs,
                                lang = lang
                            )

                            AppScreen.AUTH -> AuthScreen(viewModel = viewModel, lang = lang)
                        }
                    }
                }
            }
        }

        if (activeReceiptTx != null) {
            DigitalReceiptDialog(
                tx = activeReceiptTx!!,
                lang = lang,
                onDismiss = { viewModel.showReceipt(null) },
                onDownloadClick = {
                    viewModel.showToast(
                        tr(
                            lang,
                            "Receipt ${activeReceiptTx!!.txId}.pdf saved to Downloads (Sandbox)",
                            "डिजिटल रसिद ${activeReceiptTx!!.txId} डाउनलोड गरियो"
                        )
                    )
                }
            )
        }
    }
}
