package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.UserEntity
import com.example.data.security.SecurityManager
import com.example.ui.components.MpinAuthorizationDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import com.example.ui.i18n.tr
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.NepalPayViewModel
import com.example.ui.viewmodel.SendMoneyMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SendMoneyScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    banks: List<BankDirectoryEntity>,
    preselectedBank: BankDirectoryEntity?,
    initialMode: SendMoneyMode,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    val enabledBanks = banks.filter { it.integrationEnabled }
    var activeMode by remember(initialMode) { mutableStateOf(initialMode) }
    var mobileReceiver by remember { mutableStateOf("9801122334") }
    var paymentIdReceiver by remember { mutableStateOf("sita@nepalpay") }
    var qrPayloadInput by remember {
        mutableStateOf("NEPALPAY_QR|MERCHANT|himalayan.java@nepalpay|Himalayan Java Coffee|+9779851099887|450.0")
    }
    var selectedBank by remember(preselectedBank, enabledBanks) {
        mutableStateOf(preselectedBank ?: enabledBanks.firstOrNull())
    }
    var bankAccountNumber by remember { mutableStateOf("01200482910019") }
    var bankAccountHolder by remember { mutableStateOf("Sita Gurung") }

    var resolvedReceiverName by remember { mutableStateOf("Sita Gurung") }
    var resolvedIdentifier by remember { mutableStateOf("+9779801122334") }

    var amountInput by remember { mutableStateOf("1500") }
    var noteInput by remember { mutableStateOf("Family support / घरायसी खर्च") }
    var showPrePaymentSummary by remember { mutableStateOf(false) }
    var showMpinDialog by remember { mutableStateOf(false) }

    val amountNpr = amountInput.toDoubleOrNull() ?: 0.0
    val feeNpr = when (activeMode) {
        SendMoneyMode.BANK_ACCOUNT -> selectedBank?.transferFeeNpr ?: 10.0
        else -> if (amountNpr > 25000.0) 10.0 else 0.0
    }
    val totalAmountNpr = amountNpr + feeNpr

    LaunchedEffect(
        activeMode,
        mobileInputKey(
            activeMode,
            mobileReceiver,
            paymentIdReceiver,
            qrPayloadInput,
            bankAccountNumber,
            bankAccountHolder,
            selectedBank?.bankId ?: ""
        )
    ) {
        when (activeMode) {
            SendMoneyMode.MOBILE -> {
                viewModel.resolveReceiverPreview(mobileReceiver) { id, name ->
                    resolvedIdentifier = id
                    resolvedReceiverName = name
                }
            }
            SendMoneyMode.PAYMENT_ID -> {
                viewModel.resolveReceiverPreview(paymentIdReceiver) { id, name ->
                    resolvedIdentifier = id
                    resolvedReceiverName = name
                }
            }
            SendMoneyMode.QR_CODE -> {
                val parsed = viewModel.parseQrCode(qrPayloadInput)
                if (parsed.isValid) {
                    resolvedIdentifier = parsed.paymentId
                    resolvedReceiverName = parsed.recipientName
                }
            }
            SendMoneyMode.BANK_ACCOUNT -> {
                val bCode = selectedBank?.bankCode ?: "NARBNPKA"
                val bName = selectedBank?.officialNameEn ?: "Nabil Bank Limited"
                resolvedIdentifier = "$bCode • ${SecurityManager.maskCardOrAccountNumber(bankAccountNumber)}"
                resolvedReceiverName = "$bankAccountHolder ($bName)"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("send_money_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = tr(lang, "Send Money & Bank Transfer (NPR)", "पैसा पठाउनुहोस् र बैंक ट्रान्सफर"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ScrollableTabRow(
            selectedTabIndex = activeMode.ordinal,
            edgePadding = 0.dp
        ) {
            Tab(
                selected = activeMode == SendMoneyMode.MOBILE,
                onClick = { activeMode = SendMoneyMode.MOBILE },
                text = { Text(tr(lang, "+977 Mobile", "+977 मोबाइल")) },
                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
            )
            Tab(
                selected = activeMode == SendMoneyMode.PAYMENT_ID,
                onClick = { activeMode = SendMoneyMode.PAYMENT_ID },
                text = { Text(tr(lang, "Payment ID", "पेमेन्ट आईडी")) },
                icon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) }
            )
            Tab(
                selected = activeMode == SendMoneyMode.QR_CODE,
                onClick = { activeMode = SendMoneyMode.QR_CODE },
                text = { Text(tr(lang, "QR Code", "क्यूआर कोड")) },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
            )
            Tab(
                selected = activeMode == SendMoneyMode.BANK_ACCOUNT,
                onClick = { activeMode = SendMoneyMode.BANK_ACCOUNT },
                text = { Text(tr(lang, "Bank Transfer", "बैंक खाता")) },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                when (activeMode) {
                    SendMoneyMode.MOBILE -> {
                        OutlinedTextField(
                            value = mobileReceiver,
                            onValueChange = { mobileReceiver = it.filter { c -> c.isDigit() }.take(10) },
                            label = { Text(tr(lang, "Receiver +977 Mobile Number", "पाउनेको +977 मोबाइल नम्बर")) },
                            prefix = { Text("+977 ", fontWeight = FontWeight.Bold, color = NepalCrimson) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_mobile_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = mobileReceiver == "9801122334",
                                onClick = { mobileReceiver = "9801122334" },
                                label = { Text("Sita (9801122334)") }
                            )
                            FilterChip(
                                selected = mobileReceiver == "9812345678",
                                onClick = { mobileReceiver = "9812345678" },
                                label = { Text("Ramesh (9812345678)") }
                            )
                        }
                    }

                    SendMoneyMode.PAYMENT_ID -> {
                        OutlinedTextField(
                            value = paymentIdReceiver,
                            onValueChange = { paymentIdReceiver = it },
                            label = { Text(tr(lang, "NepalPay Payment ID", "नेपालपे पेमेन्ट आईडी")) },
                            placeholder = { Text("username@nepalpay") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_payment_id_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = paymentIdReceiver == "sita@nepalpay",
                                onClick = { paymentIdReceiver = "sita@nepalpay" },
                                label = { Text("sita@nepalpay") }
                            )
                            FilterChip(
                                selected = paymentIdReceiver == "himalayan.java@nepalpay",
                                onClick = { paymentIdReceiver = "himalayan.java@nepalpay" },
                                label = { Text("himalayan.java@nepalpay") }
                            )
                        }
                    }

                    SendMoneyMode.QR_CODE -> {
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(AppScreen.QR_SCAN_PAY) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr(lang, "Open Full Camera QR Scanner", "क्यामेरा क्यूआर स्क्यानर खोल्नुहोस्"))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = qrPayloadInput,
                            onValueChange = { qrPayloadInput = it },
                            label = { Text(tr(lang, "NepalQR Payload / Merchant ID", "क्यूआर कोड डेटा")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SendMoneyMode.BANK_ACCOUNT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(lang, "Select Licensed Nepal Bank:", "इजाजतपत्र प्राप्त बैंक छान्नुहोस्:"),
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
                            enabledBanks.take(6).forEach { bank ->
                                FilterChip(
                                    selected = selectedBank?.bankId == bank.bankId,
                                    onClick = { selectedBank = bank },
                                    label = { Text(bank.shortName) }
                                )
                            }
                        }
                        if (selectedBank != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BankLogoBadge(
                                    monogram = selectedBank!!.logoMonogram,
                                    primaryHex = selectedBank!!.logoPrimaryHex,
                                    secondaryHex = selectedBank!!.logoSecondaryHex,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedBank!!.officialNameEn,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Code: ${selectedBank!!.bankCode} • Adapter: ${selectedBank!!.apiAdapterKey} (${selectedBank!!.environmentMode})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bankAccountNumber,
                            onValueChange = { bankAccountNumber = it.filter { c -> c.isDigit() }.take(18) },
                            label = { Text(tr(lang, "Bank Account Number", "बैंक खाता नम्बर")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bankAccountHolder,
                            onValueChange = { bankAccountHolder = it },
                            label = { Text(tr(lang, "Receiver Account Holder Name", "पाउने खातावालाको नाम")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Verified Receiver Preview Box
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = HimalayanNavy
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tr(lang, "Verified Receiver Name & Routing", "प्राप्तकर्ताको प्रमाणित नाम"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = resolvedReceiverName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = resolvedIdentifier,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' }.take(9) },
                    label = { Text(tr(lang, "Amount in NPR (रु.)", "रकम NPR (रु.) मा")) },
                    prefix = { Text("रु. ", fontWeight = FontWeight.Bold, color = NepalCrimson) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("500", "1500", "5000", "55000").forEach { preset ->
                        FilterChip(
                            selected = amountInput == preset,
                            onClick = { amountInput = preset },
                            label = { Text("रु. $preset") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(tr(lang, "Purpose / Note", "प्रयोजन / कैफियत")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_note_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (amountNpr <= 0.0) {
                            viewModel.showToast("Please enter a valid NPR amount greater than 0")
                        } else {
                            showPrePaymentSummary = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("review_payment_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(lang, "Review Payment Details", "भुक्तानी विवरण जाँच गर्नुहोस्"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showPrePaymentSummary && amountNpr > 0.0) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pre_payment_confirmation_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = tr(
                            lang,
                            "Confirm Payment Details (Before MPIN Auth)",
                            "भुक्तानी पक्का गर्नुअघि विवरण जाँच गर्नुहोस्"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NepalCrimson
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryLine(tr(lang, "Receiver Name", "पाउनेको नाम"), resolvedReceiverName)
                    SummaryLine(tr(lang, "Receiver ID / A/C", "पाउनेको आईडी/खाता"), resolvedIdentifier)
                    if (activeMode == SendMoneyMode.BANK_ACCOUNT && selectedBank != null) {
                        SummaryLine(
                            tr(lang, "Bank Connector", "बैंक एपीआई कनेक्टर"),
                            "${selectedBank!!.apiAdapterKey} (${selectedBank!!.environmentMode})"
                        )
                    }
                    SummaryLine(tr(lang, "Amount in NPR", "रकम (NPR)"), NprFormatter.format(amountNpr, lang))
                    SummaryLine(tr(lang, "Transfer Fee", "सेवा शुल्क"), NprFormatter.format(feeNpr, lang))
                    SummaryLine(tr(lang, "Note / Remarks", "कैफियत"), noteInput)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SummaryLine(
                        tr(lang, "Total Payable Amount", "कुल भुक्तानी रकम"),
                        NprFormatter.format(totalAmountNpr, lang),
                        bold = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showMpinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HimalayanNavy),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("authorize_mpin_pay_button")
                    ) {
                        Text(
                            tr(
                                lang,
                                "Authorize with MPIN / Biometric",
                                "MPIN / बायोमेट्रिकबाट भुक्तानी पुष्टि गर्नुहोस्"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showMpinDialog) {
        MpinAuthorizationDialog(
            lang = lang,
            title = tr(lang, "Authorize NPR Transfer", "भुक्तानी प्रमाणीकरण"),
            subtitle = "${tr(lang, "Paying to", "पाउने:")} $resolvedReceiverName",
            amountFormatted = NprFormatter.format(totalAmountNpr, lang),
            biometricEnabled = user?.biometricEnabled ?: true,
            expectedLength = user?.mpinLength ?: 4,
            onVerifyMpin = { pin -> viewModel.verifyMpinForCurrentUser(pin) },
            onAuthorized = {
                showMpinDialog = false
                showPrePaymentSummary = false
                viewModel.processTransaction(
                    receiverIdentifier = resolvedIdentifier,
                    receiverName = resolvedReceiverName,
                    amountNpr = amountNpr,
                    feeNpr = feeNpr,
                    note = noteInput,
                    txType = if (activeMode == SendMoneyMode.BANK_ACCOUNT) "BANK_TRANSFER"
                    else if (activeMode == SendMoneyMode.QR_CODE) "QR_PAY"
                    else "SEND_MONEY",
                    paymentMethod = when (activeMode) {
                        SendMoneyMode.BANK_ACCOUNT -> "CONNECT_IPS_SANDBOX"
                        SendMoneyMode.QR_CODE -> "NEPALQR_STANDARD"
                        else -> "NEPALPAY_WALLET"
                    },
                    bankName = if (activeMode == SendMoneyMode.BANK_ACCOUNT) (selectedBank?.officialNameEn ?: "") else "",
                    bankId = if (activeMode == SendMoneyMode.BANK_ACCOUNT) (selectedBank?.bankId ?: "") else ""
                ) {}
            },
            onDismiss = { showMpinDialog = false }
        )
    }
}

private fun mobileInputKey(
    mode: SendMoneyMode,
    mobile: String,
    paymentId: String,
    qr: String,
    bankAc: String,
    holder: String,
    bankCode: String
): String = "${mode.name}|$mobile|$paymentId|$qr|$bankAc|$holder|$bankCode"

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}
