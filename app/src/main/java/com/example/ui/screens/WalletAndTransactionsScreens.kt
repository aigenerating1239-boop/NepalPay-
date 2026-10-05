package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.security.SecurityManager
import com.example.ui.components.MpinAuthorizationDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import com.example.ui.i18n.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.RubyError
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.NepalPayViewModel
import com.example.ui.viewmodel.SendMoneyMode
import com.example.ui.viewmodel.WalletActionTab

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    banks: List<BankDirectoryEntity>,
    transactions: List<TransactionEntity>,
    initialTab: WalletActionTab,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    val enabledBanks = banks.filter { it.integrationEnabled }
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedBank by remember(enabledBanks) { mutableStateOf(enabledBanks.firstOrNull()) }
    var accountNumber by remember { mutableStateOf("00109283746512") }
    var amountInput by remember { mutableStateOf("5000") }
    var noteInput by remember { mutableStateOf("Nepal Bank Sandbox Settlement") }
    var simulatedOutcome by remember { mutableStateOf("SUCCESSFUL") }
    var showMpinModal by remember { mutableStateOf(false) }

    val amountNpr = amountInput.toDoubleOrNull() ?: 0.0
    val successfulTxs = transactions.filter { it.status == "SUCCESSFUL" }
    val pendingTxs = transactions.filter { it.status == "PENDING" }
    val failedTxs = transactions.filter { it.status == "FAILED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("wallet_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = tr(lang, "NepalPay Wallet & Bank Link", "वालेट र बैंक कारोबार"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(NepalCrimson, HimalayanNavy)))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = tr(lang, "Current NPR Wallet Balance", "हालको वालेट मौज्दात (NPR)"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = NprFormatter.format(user?.walletBalanceNpr ?: 48750.0, lang),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusCounterPill(
                            label = tr(lang, "Successful", "सफल"),
                            count = successfulTxs.size,
                            color = EmeraldSuccess
                        )
                        StatusCounterPill(
                            label = tr(lang, "Pending", "पेन्डिङ"),
                            count = pendingTxs.size,
                            color = AmberWarning
                        )
                        StatusCounterPill(
                            label = tr(lang, "Failed", "असफल"),
                            count = failedTxs.size,
                            color = RubyError
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ScrollableTabRow(selectedTabIndex = activeTab.ordinal, edgePadding = 0.dp) {
            Tab(
                selected = activeTab == WalletActionTab.OVERVIEW,
                onClick = { activeTab = WalletActionTab.OVERVIEW },
                text = { Text(tr(lang, "Status Breakdown", "कारोबार स्थिति")) }
            )
            Tab(
                selected = activeTab == WalletActionTab.ADD_MONEY,
                onClick = { activeTab = WalletActionTab.ADD_MONEY },
                text = { Text(tr(lang, "Add Money", "रकम थप्नुहोस्")) },
                modifier = Modifier.testTag("wallet_tab_add_money")
            )
            Tab(
                selected = activeTab == WalletActionTab.WITHDRAW_MONEY,
                onClick = { activeTab = WalletActionTab.WITHDRAW_MONEY },
                text = { Text(tr(lang, "Withdraw", "रकम झिक्नुहोस्")) },
                modifier = Modifier.testTag("wallet_tab_withdraw")
            )
            Tab(
                selected = activeTab == WalletActionTab.BANK_TRANSFER,
                onClick = { activeTab = WalletActionTab.BANK_TRANSFER },
                text = { Text(tr(lang, "Bank Transfer", "बैंक ट्रान्सफर")) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeTab == WalletActionTab.OVERVIEW) {
            WalletStatusSection(
                title = tr(lang, "Pending Payments (${pendingTxs.size})", "पेन्डिङ कारोबारहरू (${pendingTxs.size})"),
                items = pendingTxs,
                userMobile = user?.mobileNumber ?: "",
                lang = lang,
                onTxClick = { viewModel.showReceipt(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            WalletStatusSection(
                title = tr(lang, "Failed Payments (${failedTxs.size})", "असफल कारोबारहरू (${failedTxs.size})"),
                items = failedTxs,
                userMobile = user?.mobileNumber ?: "",
                lang = lang,
                onTxClick = { viewModel.showReceipt(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            WalletStatusSection(
                title = tr(lang, "Successful Payments (${successfulTxs.size})", "सफल कारोबारहरू (${successfulTxs.size})"),
                items = successfulTxs.take(5),
                userMobile = user?.mobileNumber ?: "",
                lang = lang,
                onTxClick = { viewModel.showReceipt(it) }
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val headerText = when (activeTab) {
                        WalletActionTab.ADD_MONEY -> tr(
                            lang,
                            "Load Wallet via Licensed Nepal Bank Connector",
                            "बैंक खाताबाट वालेटमा रकम जम्मा गर्नुहोस्"
                        )
                        WalletActionTab.WITHDRAW_MONEY -> tr(
                            lang,
                            "Withdraw NPR from Wallet to Bank Account",
                            "वालेटबाट बैंक खातामा रकम झिक्नुहोस्"
                        )
                        else -> tr(
                            lang,
                            "Direct connectIPS / Bank Connector Transfer",
                            "connectIPS / बैंक खातामा सिधा ट्रान्सफर"
                        )
                    }
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(lang, "Choose Nepal Partner Bank:", "साझेदार बैंक छान्नुहोस्:"),
                            style = MaterialTheme.typography.labelLarge
                        )
                        TextButton(onClick = { viewModel.navigateTo(AppScreen.BANKS_DIRECTORY) }) {
                            Text(tr(lang, "All ${banks.size} Banks →", "सबै ${banks.size} बैंक →"))
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        enabledBanks.take(5).forEach { bank ->
                            FilterChip(
                                selected = selectedBank?.bankId == bank.bankId,
                                onClick = { selectedBank = bank },
                                label = { Text(bank.shortName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.filter { c -> c.isDigit() }.take(18) },
                        label = {
                            Text(
                                tr(
                                    lang,
                                    "${selectedBank?.officialNameEn ?: "Bank"} Account Number",
                                    "${selectedBank?.officialNameNe ?: "बैंक"} खाता नम्बर"
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                        label = { Text(tr(lang, "Amount in NPR (रु.)", "रकम NPR (रु.)")) },
                        prefix = { Text("रु. ", fontWeight = FontWeight.Bold, color = NepalCrimson) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wallet_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text(tr(lang, "Remarks / Reference", "कैफियत")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tr(
                            lang,
                            "Sandbox Gateway Outcome Simulator (Test Success/Pending/Failed):",
                            "स्यान्डबक्स नतिजा सिमुलेटर:"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SUCCESSFUL", "PENDING", "FAILED").forEach { statusOption ->
                            FilterChip(
                                selected = simulatedOutcome == statusOption,
                                onClick = { simulatedOutcome = statusOption },
                                label = { Text(statusOption) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (amountNpr > 0) showMpinModal = true
                            else viewModel.showToast("Enter valid NPR amount")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("wallet_submit_action_button")
                    ) {
                        Text(
                            when (activeTab) {
                                WalletActionTab.ADD_MONEY -> tr(lang, "Add NPR to Wallet (Sandbox)", "वालेटमा रकम थप्नुहोस्")
                                WalletActionTab.WITHDRAW_MONEY -> tr(lang, "Withdraw NPR to Bank", "बैंकमा रकम झिक्नुहोस्")
                                else -> tr(lang, "Execute Bank Transfer", "बैंक ट्रान्सफर गर्नुहोस्")
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showMpinModal) {
        val fee = if (activeTab == WalletActionTab.ADD_MONEY) 0.0 else (selectedBank?.transferFeeNpr ?: 10.0)
        val bName = selectedBank?.officialNameEn ?: "Nabil Bank Limited"
        val bCode = selectedBank?.bankCode ?: "NARBNPKA"
        MpinAuthorizationDialog(
            lang = lang,
            title = tr(lang, "Authorize Bank / Wallet Action", "वालेट/बैंक कारोबार प्रमाणीकरण"),
            subtitle = "$bName (${SecurityManager.maskCardOrAccountNumber(accountNumber)})",
            amountFormatted = NprFormatter.format(amountNpr + fee, lang),
            biometricEnabled = user?.biometricEnabled ?: true,
            expectedLength = user?.mpinLength ?: 4,
            onVerifyMpin = { pin -> viewModel.verifyMpinForCurrentUser(pin) },
            onAuthorized = {
                showMpinModal = false
                val txType = when (activeTab) {
                    WalletActionTab.ADD_MONEY -> "ADD_MONEY"
                    WalletActionTab.WITHDRAW_MONEY -> "WITHDRAW_BANK"
                    else -> "BANK_TRANSFER"
                }
                viewModel.processTransaction(
                    receiverIdentifier = "$bCode • ${SecurityManager.maskCardOrAccountNumber(accountNumber)}",
                    receiverName = bName,
                    amountNpr = amountNpr,
                    feeNpr = fee,
                    note = noteInput,
                    txType = txType,
                    paymentMethod = "CONNECT_IPS_SANDBOX",
                    bankName = bName,
                    bankId = selectedBank?.bankId ?: "",
                    simulateStatus = simulatedOutcome
                ) {}
            },
            onDismiss = { showMpinModal = false }
        )
    }
}

@Composable
private fun StatusCounterPill(label: String, count: Int, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WalletStatusSection(
    title: String,
    items: List<TransactionEntity>,
    userMobile: String,
    lang: AppLanguage,
    onTxClick: (TransactionEntity) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    if (items.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = tr(lang, "No transactions in this category.", "यस श्रेणीमा कुनै कारोबार छैन।"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
            )
        }
    } else {
        items.forEach { tx ->
            TransactionRowCard(
                tx = tx,
                currentUserMobile = userMobile,
                lang = lang,
                onClick = { onTxClick(tx) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun TransactionsScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    transactions: List<TransactionEntity>,
    lang: AppLanguage
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    val filtered = transactions.filter { tx ->
        val matchesStatus = statusFilter == "ALL" || tx.status.equals(statusFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
            tx.txId.contains(searchQuery, ignoreCase = true) ||
            tx.receiverName.contains(searchQuery, ignoreCase = true) ||
            tx.senderName.contains(searchQuery, ignoreCase = true) ||
            tx.note.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("transactions_history_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tr(lang, "Transaction History & Digital Receipts", "कारोबार इतिहास र डिजिटल रसिद"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = tr(
                    lang,
                    "Tap any transaction to view, download, or share its NRB Sandbox Digital Receipt",
                    "कुनै पनि कारोबारमा ट्याप गरी डिजिटल रसिद डाउनलोड वा शेयर गर्नुहोस्"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(tr(lang, "Search by Tx ID, Name, or Note", "कारोबार आईडी वा नाम खोज्नुहोस्")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ALL" to tr(lang, "All (${transactions.size})", "सबै (${transactions.size})"),
                    "SUCCESSFUL" to tr(lang, "Successful", "सफल"),
                    "PENDING" to tr(lang, "Pending", "पेन्डिङ"),
                    "FAILED" to tr(lang, "Failed", "असफल")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("tx_filter_$key")
                    )
                }
            }
        }

        items(filtered, key = { it.txId }) { tx ->
            TransactionRowCard(
                tx = tx,
                currentUserMobile = user?.mobileNumber ?: "+9779841234567",
                lang = lang,
                onClick = { viewModel.showReceipt(tx) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
fun PaymentsHubScreen(
    viewModel: NepalPayViewModel,
    banksCount: Int,
    lang: AppLanguage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("payments_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = tr(lang, "Payments, Banks & Transfers Hub", "भुक्तानी, बैंक र ट्रान्सफर सेवा"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = tr(
                lang,
                "Dynamic Nepal Licensed Banks Directory, Account Linking, QR Pay & Wallet Services",
                "नेपालका बैंकहरूको डाइरेक्टरी, खाता लिंक, नेपाल क्यूआर र वालेट सेवाहरू"
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HubOptionCard(
            title = tr(lang, "Nepal Licensed Banks Directory ($banksCount Banks)", "नेपाल बैंक डाइरेक्टरी ($banksCount बैंकहरू)"),
            subtitle = tr(
                lang,
                "Official Websites, Bank Codes, Per-Bank API Connectors & Merger Info",
                "आधिकारिक वेबसाइट, बैंक कोड, एपीआई कनेक्टर र मर्जर विवरण"
            ),
            icon = Icons.Default.AccountBalance,
            tint = HimalayanNavy
        ) {
            viewModel.navigateTo(AppScreen.BANKS_DIRECTORY)
        }

        HubOptionCard(
            title = tr(lang, "Link Bank Account (6-Step Flow)", "बैंक खाता लिंक गर्नुहोस्"),
            subtitle = tr(
                lang,
                "Verify account holder via bank connector, complete OTP/MPIN & link securely",
                "बैंक कनेक्टरबाट खाता जाँच र OTP/MPIN प्रमाणीकरण गरी लिंक गर्नुहोस्"
            ),
            icon = Icons.Default.AddLink,
            tint = Color(0xFF7C3AED)
        ) {
            viewModel.openLinkBankAccount(null)
        }

        HubOptionCard(
            title = tr(lang, "Send Money (+977 / Payment ID / Bank)", "पैसा पठाउनुहोस् (+977 / आईडी / बैंक)"),
            subtitle = tr(
                lang,
                "Instant wallet transfer or connectIPS bank settlement with MPIN/Biometric auth",
                "मोबाइल नम्बर, पेमेन्ट आईडी वा बैंक खातामा तुरुन्त रकम पठाउनुहोस्"
            ),
            icon = Icons.AutoMirrored.Filled.Send,
            tint = NepalCrimson
        ) {
            viewModel.openSendMoney(SendMoneyMode.MOBILE)
        }

        HubOptionCard(
            title = tr(lang, "NepalQR Scan & Pay", "स्क्यान एण्ड पे (NepalQR)"),
            subtitle = tr(
                lang,
                "Scan merchant or personal QR codes and generate digital receipts",
                "मर्चेन्ट वा व्यक्तिगत क्यूआर कोड स्क्यान गरी भुक्तानी गर्नुहोस्"
            ),
            icon = Icons.Default.QrCodeScanner,
            tint = Color(0xFF0284C7)
        ) {
            viewModel.navigateTo(AppScreen.QR_SCAN_PAY)
        }

        HubOptionCard(
            title = tr(lang, "Receive Money & Personal QR", "पैसा पाउनुहोस् र व्यक्तिगत QR"),
            subtitle = tr(
                lang,
                "Share your unique @nepalpay Payment ID, download QR, or send a Money Request",
                "आफ्नो पेमेन्ट आईडी र QR शेयर गर्नुहोस् वा रकम अनुरोध पठाउनुहोस्"
            ),
            icon = Icons.Default.QrCode2,
            tint = EmeraldSuccess
        ) {
            viewModel.navigateTo(AppScreen.RECEIVE_MONEY)
        }

        HubOptionCard(
            title = tr(lang, "Add Money / Withdraw / Wallet Status", "वालेटमा रकम जम्मा वा झिक्नुहोस्"),
            subtitle = tr(
                lang,
                "Load NPR via linked Nepal banks, withdraw to bank, or inspect status breakdown",
                "नेपालका बैंकहरूबाट वालेटमा रकम लोड वा बैंकमा ट्रान्सफर गर्नुहोस्"
            ),
            icon = Icons.Default.AccountBalanceWallet,
            tint = Color(0xFFD97706)
        ) {
            viewModel.openWalletWithTab(WalletActionTab.OVERVIEW)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun HubOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
