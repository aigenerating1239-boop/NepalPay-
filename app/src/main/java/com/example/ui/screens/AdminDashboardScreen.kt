package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ApiErrorLogEntity
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.KycSubmissionEntity
import com.example.data.local.ReconciliationLogEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import com.example.ui.i18n.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.RubyError
import com.example.ui.viewmodel.NepalPayViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: NepalPayViewModel,
    allBanks: List<BankDirectoryEntity>,
    allUsers: List<UserEntity>,
    allTransactions: List<TransactionEntity>,
    allKycSubmissions: List<KycSubmissionEntity>,
    reconciliationLogs: List<ReconciliationLogEntity>,
    apiErrorLogs: List<ApiErrorLogEntity>,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    // 0 = Banks Manager, 1 = Tx Monitoring, 2 = Reconciliation, 3 = Error Logs, 4 = Users, 5 = KYC Queue, 6 = Analytics
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingBank by remember { mutableStateOf<BankDirectoryEntity?>(null) }
    var showAddNewBankDialog by remember { mutableStateOf(false) }

    val totalUsers = allUsers.size
    val activeUsers = allUsers.count { !it.isSuspended }
    val successfulTxs = allTransactions.filter { it.status == "SUCCESSFUL" }
    val failedTxs = allTransactions.filter { it.status == "FAILED" }
    val pendingTxs = allTransactions.filter { it.status == "PENDING" }
    val flaggedTxs = allTransactions.filter { it.isFlaggedSuspicious }
    val totalVolumeNpr = successfulTxs.sumOf { it.totalAmountNpr }
    val enabledBanksCount = allBanks.count { it.integrationEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = tr(lang, "NepalPay Admin & Bank Directory Console", "नेपालपे एडमिन र बैंक व्यवस्थापन ड्यासबोर्ड"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = tr(
                            lang,
                            "Editable Bank DB • Per-Bank Connectors • Reconciliation • Error Logs • Server Vault",
                            "डाइनामिक बैंक डाटाबेस • एपीआई कनेक्टर • रिकन्सिलिएसन र एरर लग"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // KPI Metric Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    title = tr(lang, "Licensed Banks DB", "इजाजतपत्र प्राप्त बैंक"),
                    value = "${allBanks.size} ($enabledBanksCount Active)",
                    subtitle = "Server Vault Secured",
                    accent = HimalayanNavy,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = tr(lang, "Total Volume (NPR)", "कुल कारोबार रकम"),
                    value = NprFormatter.format(totalVolumeNpr, lang, showCode = false),
                    subtitle = "${successfulTxs.size} Settled Txs",
                    accent = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    title = tr(lang, "Users / Active", "प्रयोगकर्ता"),
                    value = "$totalUsers / $activeUsers",
                    subtitle = "${pendingTxs.size} Pending Clearing",
                    accent = NepalCrimson,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = tr(lang, "AML / Error Alerts", "शंकास्पद / एरर लग"),
                    value = "${flaggedTxs.size} / ${apiErrorLogs.count { !it.isResolved }}",
                    subtitle = "${reconciliationLogs.size} Recon Batches",
                    accent = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(tr(lang, "All Banks (${allBanks.size})", "सबै बैंक (${allBanks.size})")) },
                    modifier = Modifier.testTag("admin_tab_banks")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(tr(lang, "Tx Monitoring (${allTransactions.size})", "कारोबार अनुगमन")) },
                    modifier = Modifier.testTag("admin_tab_transactions")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(tr(lang, "Reconciliation (${reconciliationLogs.size})", "रिकन्सिलिएसन")) },
                    modifier = Modifier.testTag("admin_tab_reconciliation")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text(tr(lang, "Error Logs (${apiErrorLogs.size})", "एरर लगहरू")) },
                    modifier = Modifier.testTag("admin_tab_error_logs")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text(tr(lang, "Users ($totalUsers)", "प्रयोगकर्ता")) },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text(tr(lang, "KYC Queue (${allKycSubmissions.size})", "KYC प्रमाणीकरण")) },
                    modifier = Modifier.testTag("admin_tab_kyc")
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    text = { Text(tr(lang, "Analytics", "एनालिटिक्स")) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // ALL BANKS & ADD/EDIT BANK DIRECTORY MANAGER
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddNewBankDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_bank_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Add Licensed Bank", "नयाँ बैंक थप्नुहोस्"))
                        }

                        OutlinedButton(
                            onClick = { viewModel.adminVerifyWithNrbRegistry() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_verify_nrb_btn")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Verify NRB Registry", "NRB बाट भेरिफाई"))
                        }
                    }
                }

                items(allBanks, key = { it.bankId }) { bank ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_bank_row_${bank.bankId}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    BankLogoBadge(
                                        monogram = bank.logoMonogram,
                                        primaryHex = bank.logoPrimaryHex,
                                        secondaryHex = bank.logoSecondaryHex,
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${bank.displayOrder}. ${bank.officialNameEn}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${bank.shortName} • Code: ${bank.bankCode} • ${bank.environmentMode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = bank.integrationEnabled,
                                        onCheckedChange = { enabled ->
                                            viewModel.adminToggleBankIntegration(bank, enabled)
                                        }
                                    )
                                    IconButton(
                                        onClick = { editingBank = bank },
                                        modifier = Modifier.testTag("admin_edit_bank_${bank.bankId}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Bank", tint = HimalayanNavy)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Website: ${bank.officialWebsiteUrl}\nEndpoint: ${bank.apiEndpointUrl}\nConnector: ${bank.apiAdapterKey} (Secrets in Encrypted Server Vault)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            1 -> {
                items(allTransactions, key = { it.txId }) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.txId} • ${NprFormatter.format(tx.totalAmountNpr, lang)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${tx.senderName} → ${tx.receiverName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${tx.txType} • ${tx.paymentMethod}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = tx.status, lang = lang)
                            }

                            if (tx.isFlaggedSuspicious) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = AmberWarning
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AML Flag: ${tx.fraudReason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RubyError,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (tx.status == "PENDING") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.adminUpdateTxStatus(tx.txId, "SUCCESSFUL") },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr(lang, "Approve Settlement", "सफल बनाउनुहोस्"))
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.adminUpdateTxStatus(tx.txId, "FAILED") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(tr(lang, "Reject / Fail", "अस्वीकार गर्नुहोस्"))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // RECONCILIATION TAB
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        )
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
                                    text = tr(
                                        lang,
                                        "Automated Bank Switch Reconciliation",
                                        "स्वचालित बैंक रिकन्सिलिएसन"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "Compare NepalPay ledger vs NCHL connectIPS / Bank connector EOD totals",
                                        "नेपालपे लेजर र बैंक स्विचको कारोबार हिसाब मिलाउनुहोस्"
                                    ),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Button(
                                onClick = {
                                    allBanks.firstOrNull()?.let { viewModel.adminRunBankReconciliation(it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HimalayanNavy),
                                modifier = Modifier.testTag("admin_run_recon_btn")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr(lang, "Run Recon", "रिकन चलाउनुहोस्"))
                            }
                        }
                    }
                }

                items(reconciliationLogs, key = { it.reconId }) { recon ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${recon.reconId} • ${recon.bankName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = recon.status,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (recon.status == "BALANCED") EmeraldSuccess else AmberWarning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Matched: ${recon.matchedTxCount}/${recon.totalTxCount} • Discrepancies: ${recon.discrepancyCount} • Volume: ${NprFormatter.format(recon.totalVolumeNpr, lang)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = recon.notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            3 -> {
                // API ERROR LOGS TAB
                items(apiErrorLogs, key = { it.id }) { err ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[HTTP ${err.httpStatusCode}] ${err.errorCode} • ${err.bankName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (err.isResolved) EmeraldSuccess else RubyError
                                )
                                if (!err.isResolved) {
                                    TextButton(onClick = { viewModel.adminResolveErrorLog(err.id) }) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(tr(lang, "Resolve", "समाधान"))
                                    }
                                } else {
                                    Text(
                                        text = "RESOLVED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = err.errorMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Endpoint: ${err.endpoint} (${err.environmentMode})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            4 -> {
                items(allUsers, key = { it.mobileNumber }) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${u.fullName} (${u.fullNameNe})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${u.mobileNumber} • ${u.paymentId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Balance: ${NprFormatter.format(u.walletBalanceNpr, lang)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        status = if (u.isSuspended) "SUSPENDED" else "ACTIVE",
                                        lang = lang
                                    )
                                    StatusBadge(status = u.kycStatus, lang = lang)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.adminSetUserSuspended(u.mobileNumber, !u.isSuspended) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_suspend_${u.mobileNumber}")
                                ) {
                                    Text(
                                        if (u.isSuspended) tr(lang, "Reactivate Account", "खाता सुचारु गर्नुहोस्")
                                        else tr(lang, "Suspend Account", "खाता रोक्का गर्नुहोस्")
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.switchDemoAccount(u.mobileNumber) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(tr(lang, "Switch to User", "यस खातामा जानुहोस्"))
                                }
                            }
                        }
                    }
                }
            }

            5 -> {
                items(allKycSubmissions, key = { it.mobileNumber }) { kyc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${kyc.fullName} (${kyc.mobileNumber})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "DOB: ${kyc.dateOfBirth} • ${kyc.district}, ${kyc.province}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Doc: ${kyc.documentType} (${kyc.documentNumberMasked}) • Selfie: ${if (kyc.selfieVerified) "Verified" else "No"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = kyc.status, lang = lang)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.adminUpdateKyc(
                                            kyc.mobileNumber,
                                            "VERIFIED",
                                            "Approved by NRB Compliance Admin"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_kyc_approve_${kyc.mobileNumber}")
                                ) {
                                    Text(tr(lang, "Verify KYC", "KYC प्रमाणित गर्नुहोस्"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.adminUpdateKyc(
                                            kyc.mobileNumber,
                                            "REJECTED",
                                            "Document clarity / mismatch requires resubmission"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_kyc_reject_${kyc.mobileNumber}")
                                ) {
                                    Text(tr(lang, "Reject KYC", "अस्वीकार गर्नुहोस्"))
                                }
                            }
                        }
                    }
                }
            }

            6 -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = tr(lang, "Payment Channel Distribution (NPR)", "भुक्तानी माध्यम अनुसार विश्लेषण"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val totalCount = allTransactions.size.coerceAtLeast(1)
                            val walletCount = allTransactions.count { it.paymentMethod == "NEPALPAY_WALLET" }
                            val qrCount = allTransactions.count { it.paymentMethod == "NEPALQR_STANDARD" }
                            val bankCount = allTransactions.count {
                                it.paymentMethod == "CONNECT_IPS_SANDBOX" || it.paymentMethod == "BANK_DIRECT"
                            }

                            AnalyticsBarRow(
                                label = "NepalPay P2P Wallet ($walletCount)",
                                progress = walletCount.toFloat() / totalCount,
                                color = NepalCrimson
                            )
                            AnalyticsBarRow(
                                label = "NepalQR Merchant Scan ($qrCount)",
                                progress = qrCount.toFloat() / totalCount,
                                color = HimalayanNavy
                            )
                            AnalyticsBarRow(
                                label = "connectIPS / Licensed Bank ($bankCount)",
                                progress = bankCount.toFloat() / totalCount,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }

    if (editingBank != null || showAddNewBankDialog) {
        val initial = editingBank ?: BankDirectoryEntity(
            bankId = "NEW_BANK_${allBanks.size + 1}",
            displayOrder = allBanks.size + 1,
            officialNameEn = "",
            officialNameNe = "",
            shortName = "",
            bankCode = "",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.",
            logoMonogram = "BNK",
            logoPrimaryHex = "#003893",
            logoSecondaryHex = "#C8102E",
            apiAdapterKey = "CONNECTOR_CUSTOM_OPENAPI",
            apiEndpointUrl = "https://sandbox.api.bank.com.np/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true
        )
        BankEditorDialog(
            initialBank = initial,
            lang = lang,
            onDismiss = {
                editingBank = null
                showAddNewBankDialog = false
            },
            onSave = { updated ->
                viewModel.adminSaveOrUpdateBank(updated)
                editingBank = null
                showAddNewBankDialog = false
            }
        )
    }
}

@Composable
private fun BankEditorDialog(
    initialBank: BankDirectoryEntity,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (BankDirectoryEntity) -> Unit
) {
    var officialNameEn by remember { mutableStateOf(initialBank.officialNameEn) }
    var officialNameNe by remember { mutableStateOf(initialBank.officialNameNe) }
    var shortName by remember { mutableStateOf(initialBank.shortName) }
    var bankCode by remember { mutableStateOf(initialBank.bankCode) }
    var websiteUrl by remember { mutableStateOf(initialBank.officialWebsiteUrl) }
    var logoMonogram by remember { mutableStateOf(initialBank.logoMonogram) }
    var apiEndpoint by remember { mutableStateOf(initialBank.apiEndpointUrl) }
    var apiStatus by remember { mutableStateOf(initialBank.apiStatus) }
    var envMode by remember { mutableStateOf(initialBank.environmentMode) }
    var mergerNote by remember { mutableStateOf(initialBank.mergerNoteEn) }
    var enabled by remember { mutableStateOf(initialBank.integrationEnabled) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr(
                        lang,
                        "Edit Dynamic Bank Directory Record (${initialBank.bankId})",
                        "बैंक डाइरेक्टरी विवरण सम्पादन (${initialBank.bankId})"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = officialNameEn,
                    onValueChange = { officialNameEn = it },
                    label = { Text("Official Bank Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = officialNameNe,
                    onValueChange = { officialNameNe = it },
                    label = { Text("Official Bank Name (नेपाली)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text("Short Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bankCode,
                        onValueChange = { bankCode = it.uppercase() },
                        label = { Text("Bank / SWIFT Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = logoMonogram,
                        onValueChange = { logoMonogram = it.take(5).uppercase() },
                        label = { Text("Logo Monogram") },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f)
                    )
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Official HTTPS Website") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = apiEndpoint,
                    onValueChange = { apiEndpoint = it },
                    label = { Text("API Endpoint URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mergerNote,
                    onValueChange = { mergerNote = it },
                    label = { Text("Merger / Successor Institution Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("API Status:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CONNECTED_SANDBOX", "AUTHORIZED_READY", "DISABLED").forEach { st ->
                        FilterChip(
                            selected = apiStatus == st,
                            onClick = { apiStatus = st },
                            label = { Text(st) }
                        )
                    }
                }

                Text("Environment Mode:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SANDBOX", "PRODUCTION_READY").forEach { mode ->
                        FilterChip(
                            selected = envMode == mode,
                            onClick = { envMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Bank API Integration")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(tr(lang, "Cancel", "रद्द गर्नुहोस्"))
                    }
                    Button(
                        onClick = {
                            onSave(
                                initialBank.copy(
                                    officialNameEn = officialNameEn.ifBlank { "Licensed Nepal Bank" },
                                    officialNameNe = officialNameNe.ifBlank { officialNameEn },
                                    shortName = shortName.ifBlank { "Bank" },
                                    bankCode = bankCode.ifBlank { "NPBKNPKA" },
                                    officialWebsiteUrl = websiteUrl,
                                    logoMonogram = logoMonogram.ifBlank { "BNK" },
                                    apiEndpointUrl = apiEndpoint,
                                    apiStatus = apiStatus,
                                    environmentMode = envMode,
                                    mergerNoteEn = mergerNote,
                                    integrationEnabled = enabled
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_bank_directory_btn")
                    ) {
                        Text(tr(lang, "Save to DB", "डाटाबेसमा सेभ"))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsBarRow(
    label: String,
    progress: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0.05f, 1f) },
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}
