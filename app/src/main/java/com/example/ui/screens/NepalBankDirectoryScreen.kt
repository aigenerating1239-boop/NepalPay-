package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.LinkedBankAccountEntity
import com.example.data.local.UserEntity
import com.example.data.security.SecurityManager
import com.example.data.service.AccountVerificationResult
import com.example.ui.components.MpinAuthorizationDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.RubyError
import com.example.ui.theme.RubyErrorContainer
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.NepalPayViewModel
import com.example.ui.viewmodel.SendMoneyMode
import kotlinx.coroutines.launch

fun parseHexColorSafe(hex: String, fallback: Color = HimalayanNavy): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun BankLogoBadge(
    monogram: String,
    primaryHex: String,
    secondaryHex: String,
    size: Dp = 54.dp
) {
    val c1 = remember(primaryHex) { parseHexColorSafe(primaryHex, HimalayanNavy) }
    val c2 = remember(secondaryHex) { parseHexColorSafe(secondaryHex, NepalCrimson) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(c1, c2)))
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.34f)
            )
            Text(
                text = monogram.take(5).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NepalBankDirectoryScreen(
    viewModel: NepalPayViewModel,
    banks: List<BankDirectoryEntity>,
    linkedAccounts: List<LinkedBankAccountEntity>,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showArchitectureInfo by remember { mutableStateOf(false) }

    val filteredBanks = banks.filter { bank ->
        val matchesCategory = when (selectedCategory) {
            "STATE" -> bank.category == "COMMERCIAL_STATE"
            "PRIVATE" -> bank.category == "COMMERCIAL_PRIVATE"
            "MERGED" -> bank.category == "MERGED_SUCCESSOR" || bank.mergerNoteEn.isNotBlank()
            "AUTHORIZED" -> bank.apiStatus == "AUTHORIZED_READY"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            bank.officialNameEn.contains(searchQuery, ignoreCase = true) ||
            bank.officialNameNe.contains(searchQuery, ignoreCase = true) ||
            bank.shortName.contains(searchQuery, ignoreCase = true) ||
            bank.bankCode.contains(searchQuery, ignoreCase = true) ||
            bank.bankId.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("nepal_banks_directory_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = tr(
                                lang,
                                "Nepal Licensed Banks Directory (${banks.size})",
                                "नेपालका इजाजतपत्र प्राप्त बैंकहरू (${banks.size})"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = tr(
                                lang,
                                "Dynamic Editable DB • Official HTTPS Links • Per-Bank API Connectors",
                                "डाइनामिक डाटाबेस • आधिकारिक वेबसाइट र एपीआई कनेक्टर"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                    modifier = Modifier.testTag("directory_open_admin_btn")
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "Edit Banks in Admin Panel",
                        tint = NepalCrimson
                    )
                }
            }
        }

        // Connected Payment Architecture & NRB Regulatory Verification Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = HimalayanNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "Connected Bank API & Adapter Architecture",
                                    "कनेक्टेड बैंक एपीआई र एडप्टर आर्किटेक्चर"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { showArchitectureInfo = !showArchitectureInfo }) {
                            Text(if (showArchitectureInfo) tr(lang, "Hide", "लुकाउनुहोस्") else tr(lang, "Diagram", "आर्किटेक्चर"))
                        }
                    }

                    if (showArchitectureInfo) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ArchitectureStepPill("1. NepalPay Mobile App (TLS 1.3 + MPIN/Biometric)")
                                Text("↓", fontWeight = FontWeight.ExtraBold, color = NepalCrimson)
                                ArchitectureStepPill("2. Secure Backend Gateway (Server Vault Secrets + Rate Limiter)")
                                Text("↓", fontWeight = FontWeight.ExtraBold, color = NepalCrimson)
                                ArchitectureStepPill("3. Payment / Bank Integration Layer (HMAC Webhook Verifier)")
                                Text("↓", fontWeight = FontWeight.ExtraBold, color = NepalCrimson)
                                ArchitectureStepPill("4. Per-Bank API Connector (ISO20022 / connectIPS / OpenBanking)")
                                Text("↓", fontWeight = FontWeight.ExtraBold, color = NepalCrimson)
                                ArchitectureStepPill("5. Licensed Nepal Commercial Bank Account")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nrb.org.np"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_nrb_official_source_btn")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                tr(lang, "Verify on NRB (nrb.org.np)", "NRB आधिकारिक सूची हेर्नुहोस्"),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = { viewModel.openLinkBankAccount(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("directory_link_bank_btn")
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                tr(lang, "Link Bank A/C (${linkedAccounts.size})", "बैंक खाता लिंक (${linkedAccounts.size})"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Search & Category Filters
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = {
                    Text(
                        tr(
                            lang,
                            "Search Bank Name, Short Name, or Code (e.g. Nabil, NICA, GLBBNPKA)",
                            "बैंकको नाम, छोटो नाम वा कोड खोज्नुहोस्"
                        )
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_directory_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "ALL" to tr(lang, "All (${banks.size})", "सबै (${banks.size})"),
                    "STATE" to tr(lang, "State-Owned", "सरकारी/सार्वजनिक"),
                    "PRIVATE" to tr(lang, "Private Commercial", "निजी वाणिज्य"),
                    "MERGED" to tr(lang, "Merged / Successor", "मर्जर / उत्तराधिकारी"),
                    "AUTHORIZED" to tr(lang, "Authorized Ready", "एपीआई तयार")
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        items(filteredBanks, key = { it.bankId }) { bank ->
            NepalBankDirectoryCard(
                bank = bank,
                lang = lang,
                onVisitWebsite = {
                    if (SecurityManager.isSafeOfficialBankUrl(bank.officialWebsiteUrl)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bank.officialWebsiteUrl))
                        context.startActivity(intent)
                    } else {
                        viewModel.showToast("Blocked non-HTTPS or unverified URL")
                    }
                },
                onAddBankAccount = {
                    viewModel.openLinkBankAccount(bank)
                },
                onBankTransfer = {
                    viewModel.openSendMoney(SendMoneyMode.BANK_ACCOUNT, preselectedBank = bank)
                }
            )
        }

        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

@Composable
private fun ArchitectureStepPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun NepalBankDirectoryCard(
    bank: BankDirectoryEntity,
    lang: AppLanguage,
    onVisitWebsite: () -> Unit,
    onAddBankAccount: () -> Unit,
    onBankTransfer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_card_${bank.bankId}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                BankLogoBadge(
                    monogram = bank.logoMonogram,
                    primaryHex = bank.logoPrimaryHex,
                    secondaryHex = bank.logoSecondaryHex
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (lang == AppLanguage.NEPALI) bank.officialNameNe else bank.officialNameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (lang == AppLanguage.NEPALI) bank.officialNameEn else bank.officialNameNe,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Short: ${bank.shortName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Code: ${bank.bankCode}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (bank.mergerNoteEn.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AmberWarningContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️ " + (if (lang == AppLanguage.NEPALI && bank.mergerNoteNe.isNotBlank()) bank.mergerNoteNe else bank.mergerNoteEn),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF78350F),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // API Integration Status & Sandbox/Production Mode Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (apiBg, apiFg) = when {
                    !bank.integrationEnabled || bank.apiStatus == "DISABLED" -> RubyErrorContainer to RubyError
                    bank.apiStatus == "AUTHORIZED_READY" -> EmeraldSuccessContainer to EmeraldSuccess
                    else -> AmberWarningContainer to AmberWarning
                }
                Surface(color = apiBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "API: ${if (bank.integrationEnabled) bank.apiStatus else "DISABLED"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = apiFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = if (bank.environmentMode == "PRODUCTION_READY") EmeraldSuccessContainer
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Env: ${bank.environmentMode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bank.environmentMode == "PRODUCTION_READY") EmeraldSuccess else NepalCrimson,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connector: ${bank.apiAdapterKey} • Official: ${bank.officialWebsiteUrl}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // 3 Required Action Buttons per Bank Card:
            // 1. Visit Official Website
            // 2. Add Bank Account
            // 3. Bank Transfer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVisitWebsite,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("visit_website_${bank.bankId}")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        tr(lang, "Official Website", "आधिकारिक साइट"),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onAddBankAccount,
                    enabled = bank.integrationEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_bank_account_${bank.bankId}")
                ) {
                    Icon(
                        Icons.Default.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        tr(lang, "Add Account", "खाता जोड्नुहोस्"),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onBankTransfer,
                    enabled = bank.integrationEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bank_transfer_${bank.bankId}")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        tr(lang, "Bank Transfer", "ट्रान्सफर"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 6-Step Secure Bank Account Linking Screen:
 * 1. Select Bank from Dynamic Directory
 * 2. Enter Account Number
 * 3. Verify Account Holder Information
 * 4. Authorized Bank / Payment Provider Connector Verification
 * 5. Complete OTP + MPIN/Biometric Authentication
 * 6. Link Bank Account to NepalPay Profile
 */
@Composable
fun LinkBankAccountScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    banks: List<BankDirectoryEntity>,
    linkedAccounts: List<LinkedBankAccountEntity>,
    preselectedBank: BankDirectoryEntity?,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }
    val scope = rememberCoroutineScope()

    val availableBanks = banks.filter { it.integrationEnabled }
    var selectedBank by remember(preselectedBank, availableBanks) {
        mutableStateOf(preselectedBank ?: availableBanks.firstOrNull())
    }
    var accountNumberInput by remember { mutableStateOf("01200984123456") }
    var accountHolderInput by remember(user) { mutableStateOf(user?.fullName ?: "Aarav Sharma") }
    var setAsPrimary by remember { mutableStateOf(linkedAccounts.isEmpty()) }

    var currentStep by remember { mutableIntStateOf(1) } // 1..6
    var verificationResult by remember { mutableStateOf<AccountVerificationResult?>(null) }
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var showMpinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("link_bank_account_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = tr(lang, "Link Bank Account (6-Step Secure Flow)", "बैंक खाता लिंक गर्नुहोस्"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = tr(
                        lang,
                        "Tokenized AES-256 account linking via per-bank API connector",
                        "बैंक एपीआई कनेक्टर मार्फत सुरक्षित खाता लिंक"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Step Progress Indicator (Steps 1 to 6)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..6).forEach { step ->
                val active = step <= currentStep
                Surface(
                    shape = CircleShape,
                    color = if (active) NepalCrimson else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step.toString(),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 1: Select Bank
                Text(
                    text = tr(lang, "1. Select Licensed Nepal Bank", "१. बैंक छान्नुहोस्"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableBanks.take(4).forEach { b ->
                        FilterChip(
                            selected = selectedBank?.bankId == b.bankId,
                            onClick = {
                                selectedBank = b
                                verificationResult = null
                                currentStep = 1
                            },
                            label = { Text(b.shortName) }
                        )
                    }
                }
                if (selectedBank != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BankLogoBadge(
                            monogram = selectedBank!!.logoMonogram,
                            primaryHex = selectedBank!!.logoPrimaryHex,
                            secondaryHex = selectedBank!!.logoSecondaryHex,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = selectedBank!!.officialNameEn,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Adapter: ${selectedBank!!.apiAdapterKey} (${selectedBank!!.environmentMode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Step 2 & 3: Enter Account Number & Account Holder Info
                Text(
                    text = tr(
                        lang,
                        "2 & 3. Enter Account Number & Holder Information",
                        "२ र ३. खाता नम्बर र खातावालाको विवरण"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = accountNumberInput,
                    onValueChange = {
                        accountNumberInput = it.filter { c -> c.isDigit() }.take(18)
                        currentStep = 2
                    },
                    label = { Text(tr(lang, "Bank Account Number", "बैंक खाता नम्बर")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("link_account_number_input")
                )

                OutlinedTextField(
                    value = accountHolderInput,
                    onValueChange = {
                        accountHolderInput = it
                        currentStep = 3
                    },
                    label = { Text(tr(lang, "Account Holder Name (KYC Match)", "खातावालाको नाम")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr(lang, "Set as Primary Linked Bank Account", "मुख्य बैंक खाता बनाउनुहोस्"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(checked = setAsPrimary, onCheckedChange = { setAsPrimary = it })
                }

                // Step 4: Authorized Bank / Payment Provider Verification
                Button(
                    onClick = {
                        val bank = selectedBank ?: return@Button
                        if (accountNumberInput.length < 8) {
                            viewModel.showToast("Enter a valid bank account number (min 8 digits)")
                            return@Button
                        }
                        scope.launch {
                            val res = viewModel.verifyBankAccountViaConnector(
                                bank = bank,
                                accountNumber = accountNumberInput,
                                holderName = accountHolderInput
                            )
                            verificationResult = res
                            if (res.isVerified) {
                                val otp = viewModel.requestOtp(user?.mobileNumber ?: "+9779841234567")
                                generatedOtp = otp
                                enteredOtp = otp // Pre-fill in sandbox for smooth UX
                                currentStep = 5
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HimalayanNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_bank_connector_btn")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(
                            lang,
                            "4. Verify via Authorized Bank Connector",
                            "४. बैंक एपीआई कनेक्टरबाट जाँच गर्नुहोस्"
                        )
                    )
                }

                if (verificationResult != null) {
                    Surface(
                        color = if (verificationResult!!.isVerified) EmeraldSuccessContainer else RubyErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (lang == AppLanguage.NEPALI) verificationResult!!.messageNe
                                else verificationResult!!.messageEn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (verificationResult!!.isVerified) EmeraldSuccess else RubyError
                            )
                            if (verificationResult!!.isVerified) {
                                Text(
                                    text = "Holder: ${verificationResult!!.verifiedHolderName} • Masked: ${verificationResult!!.maskedAccountNumber}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Step 5 & 6: OTP + MPIN Authentication to Complete Link
                if (verificationResult?.isVerified == true) {
                    HorizontalDivider()
                    Text(
                        text = tr(
                            lang,
                            "5. Complete OTP & MPIN/Biometric Authentication",
                            "५. OTP र MPIN प्रमाणीकरण पूरा गर्नुहोस्"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { enteredOtp = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("6-Digit Bank Link OTP (Sandbox: $generatedOtp)") },
                        leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (enteredOtp == generatedOtp && enteredOtp.isNotBlank()) {
                                showMpinDialog = true
                            } else {
                                viewModel.showToast("Invalid OTP. Use Sandbox OTP: $generatedOtp")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("complete_bank_link_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tr(
                                lang,
                                "6. Authorize & Link Bank Account to NepalPay",
                                "६. प्रमाणीकरण गरी बैंक खाता लिंक गर्नुहोस्"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Currently Linked Bank Accounts List
        Text(
            text = tr(
                lang,
                "Your Linked Bank Accounts (${linkedAccounts.size})",
                "लिंक गरिएका बैंक खाताहरू (${linkedAccounts.size})"
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        linkedAccounts.forEach { acc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = acc.officialBankName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${acc.accountHolderName} • ${acc.accountNumberMasked} (${acc.bankCode})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Verified via: ${acc.verificationProvider}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.removeLinkedBankAccount(acc.linkId) }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Unlink Account",
                            tint = RubyError
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showMpinDialog && selectedBank != null && verificationResult != null) {
        MpinAuthorizationDialog(
            lang = lang,
            title = tr(lang, "Authorize Bank Account Link", "बैंक खाता लिंक प्रमाणीकरण"),
            subtitle = "${selectedBank!!.officialNameEn} (${verificationResult!!.maskedAccountNumber})",
            amountFormatted = "TOKENIZED LINK",
            biometricEnabled = user?.biometricEnabled ?: true,
            expectedLength = user?.mpinLength ?: 4,
            onVerifyMpin = { pin -> viewModel.verifyMpinForCurrentUser(pin) },
            onAuthorized = {
                showMpinDialog = false
                currentStep = 6
                viewModel.completeBankAccountLinking(
                    bank = selectedBank!!,
                    accountNumberPlain = accountNumberInput,
                    verifiedHolderName = verificationResult!!.verifiedHolderName,
                    isPrimary = setAsPrimary
                ) {}
            },
            onDismiss = { showMpinDialog = false }
        )
    }
}
