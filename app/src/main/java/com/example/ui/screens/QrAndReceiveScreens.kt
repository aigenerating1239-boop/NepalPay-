package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SandboxMerchantQr
import com.example.data.local.UserEntity
import com.example.data.service.ParsedQrResult
import com.example.ui.components.MpinAuthorizationDialog
import com.example.ui.components.NepalPayQrCodeBox
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import com.example.ui.i18n.tr
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.viewmodel.NepalPayViewModel

@Composable
fun QrScanPayScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var scannedTarget by remember {
        mutableStateOf<ParsedQrResult?>(
            viewModel.parseQrCode(viewModel.sandboxMerchants.first().qrPayload)
        )
    }
    var amountInput by remember { mutableStateOf("450") }
    var noteInput by remember { mutableStateOf("NepalQR Merchant Payment") }
    var showConfirmStep by remember { mutableStateOf(false) }
    var showMpinAuth by remember { mutableStateOf(false) }

    val amountNpr = amountInput.toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("qr_scan_pay_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = tr(lang, "NepalQR Scan & Pay", "स्क्यान एण्ड पे (NepalQR)"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Camera Viewfinder Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, NepalCrimson, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraPreviewBox(modifier = Modifier.fillMaxSize())
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tr(
                            lang,
                            "Camera QR Scanner Ready (or select a Sandbox NepalQR below)",
                            "क्यामेरा स्क्यानर वा तलको डेमो मर्चेन्ट क्यूआर छान्नुहोस्"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(tr(lang, "Enable Camera Viewfinder", "क्यामेरा अनुमति दिनुहोस्"))
                    }
                }
            }

            // Corner Scan Frame Overlay
            Box(
                modifier = Modifier
                    .size(145.dp)
                    .border(2.5.dp, Color(0xFF34D399), RoundedCornerShape(18.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tr(
                lang,
                "Simulate Scanning Standard NepalQR Merchants:",
                "नेपालमा प्रचलित स्ट्यान्डर्ड QR मर्चेन्ट स्क्यान गर्नुहोस्:"
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        viewModel.sandboxMerchants.forEach { merchant ->
            MerchantQrOptionCard(
                merchant = merchant,
                isSelected = scannedTarget?.paymentId == merchant.paymentId,
                lang = lang
            ) {
                scannedTarget = viewModel.parseQrCode(merchant.qrPayload)
                if (merchant.suggestedAmount > 0) {
                    amountInput = merchant.suggestedAmount.toInt().toString()
                }
                noteInput = "QR Pay at ${merchant.merchantNameEn}"
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (scannedTarget != null && scannedTarget!!.isValid) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = NepalCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = scannedTarget!!.recipientName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${scannedTarget!!.paymentId} • ${scannedTarget!!.mobileNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                        label = { Text(tr(lang, "Enter Amount in NPR (रु.)", "भुक्तानी रकम (रु.)")) },
                        prefix = { Text("रु. ", fontWeight = FontWeight.Bold, color = NepalCrimson) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_amount_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text(tr(lang, "Bill / Counter Remarks", "बिल कैफियत")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!showConfirmStep) {
                        Button(
                            onClick = {
                                if (amountNpr > 0) showConfirmStep = true
                                else viewModel.showToast("Enter valid NPR amount")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("qr_proceed_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr(lang, "Proceed to QR Payment Confirmation", "भुक्तानी विवरण पक्का गर्नुहोस्"))
                        }
                    } else {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = tr(lang, "QR Payment Confirmation", "QR भुक्तानी पुष्टिकरण"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HimalayanNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${tr(lang, "Merchant:", "मर्चेन्ट:")} ${scannedTarget!!.recipientName}\n" +
                                "${tr(lang, "Total Payable:", "कुल रकम:")} ${NprFormatter.format(amountNpr, lang)} (Zero QR Fee)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showMpinAuth = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("qr_authorize_pay_button")
                        ) {
                            Text(
                                tr(
                                    lang,
                                    "Authorize QR Payment (MPIN / Biometric)",
                                    "MPIN / बायोमेट्रिकबाट QR भुक्तानी गर्नुहोस्"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showMpinAuth && scannedTarget != null) {
        MpinAuthorizationDialog(
            lang = lang,
            title = tr(lang, "Authorize NepalQR Payment", "NepalQR भुक्तानी प्रमाणीकरण"),
            subtitle = scannedTarget!!.recipientName,
            amountFormatted = NprFormatter.format(amountNpr, lang),
            biometricEnabled = user?.biometricEnabled ?: true,
            expectedLength = user?.mpinLength ?: 4,
            onVerifyMpin = { pin -> viewModel.verifyMpinForCurrentUser(pin) },
            onAuthorized = {
                showMpinAuth = false
                showConfirmStep = false
                viewModel.processTransaction(
                    receiverIdentifier = scannedTarget!!.paymentId,
                    receiverName = scannedTarget!!.recipientName,
                    amountNpr = amountNpr,
                    feeNpr = 0.0,
                    note = noteInput,
                    txType = "QR_PAY",
                    paymentMethod = "NEPALQR_STANDARD"
                ) {}
            },
            onDismiss = { showMpinAuth = false }
        )
    }
}

@Composable
private fun MerchantQrOptionCard(
    merchant: SandboxMerchantQr,
    isSelected: Boolean,
    lang: AppLanguage,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) NepalCrimson else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lang == AppLanguage.NEPALI) merchant.merchantNameNe else merchant.merchantNameEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lang == AppLanguage.NEPALI) merchant.categoryNe else merchant.categoryEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "रु. ${merchant.suggestedAmount.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = NepalCrimson,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CameraPreviewBox(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
fun ReceiveMoneyScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var activeTab by remember { mutableIntStateOf(0) } // 0 = My QR & Payment ID, 1 = Request Money

    var payerMobileInput by remember { mutableStateOf("9801122334") }
    var requestAmountInput by remember { mutableStateOf("1200") }
    var requestNoteInput by remember { mutableStateOf("Lunch bill split / खाजा खर्च") }

    val qrPayload = remember(user) {
        if (user != null) viewModel.buildPersonalQr(user)
        else "NEPALPAY_QR|USER|aarav@nepalpay|Aarav Sharma|+9779841234567|0.0"
    }
    val moneyRequests by viewModel.moneyRequests.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("receive_money_screen")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = tr(lang, "Receive Money & NepalQR", "पैसा पाउनुहोस् र मेरो QR"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text(tr(lang, "My QR & Payment ID", "मेरो QR र आईडी")) },
                icon = { Icon(Icons.Default.QrCode2, contentDescription = null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text(tr(lang, "Request Money", "रकम अनुरोध")) },
                icon = { Icon(Icons.Default.RequestPage, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeTab == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = user?.fullName ?: "Aarav Sharma",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = user?.mobileNumber ?: "+9779841234567",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        onClick = {
                            val id = user?.paymentId ?: "aarav@nepalpay"
                            clipboard.setText(AnnotatedString(id))
                            viewModel.showToast("Copied Payment ID: $id")
                        },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("copy_payment_id_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment ID: ${user?.paymentId ?: "aarav@nepalpay"}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    NepalPayQrCodeBox(
                        payload = qrPayload,
                        size = 220.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tr(
                            lang,
                            "Interoperable NepalPay Sandbox QR • Scan from any NepalPay app",
                            "नेपालपे स्ट्यान्डर्ड QR • पैसा प्राप्त गर्न स्क्यान गर्नुहोस्"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.showToast(
                                    tr(
                                        lang,
                                        "Personal QR saved to device gallery (Sandbox)",
                                        "तपाईंको QR कोड ग्यालरीमा सुरक्षित गरियो"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_qr_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Download QR", "QR डाउनलोड"))
                        }

                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Pay ${user?.fullName ?: "Aarav Sharma"} on NepalPay!\nPayment ID: ${user?.paymentId}\nMobile: ${user?.mobileNumber}\nNepalQR Payload: $qrPayload"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share NepalPay QR"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_qr_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Share QR", "QR शेयर"))
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = tr(
                            lang,
                            "Request Money from NepalPay User",
                            "अर्को प्रयोगकर्तासँग रकम अनुरोध गर्नुहोस्"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = payerMobileInput,
                        onValueChange = { payerMobileInput = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text(tr(lang, "Payer +977 Mobile Number", "भुक्तानी गर्नेको +977 नम्बर")) },
                        prefix = { Text("+977 ", fontWeight = FontWeight.Bold, color = NepalCrimson) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_payer_mobile_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = requestAmountInput,
                        onValueChange = { requestAmountInput = it.filter { c -> c.isDigit() || c == '.' }.take(8) },
                        label = { Text(tr(lang, "Requested Amount in NPR (रु.)", "अनुरोध रकम (रु.)")) },
                        prefix = { Text("रु. ", fontWeight = FontWeight.Bold, color = NepalCrimson) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_amount_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = requestNoteInput,
                        onValueChange = { requestNoteInput = it },
                        label = { Text(tr(lang, "Reason / Note", "कैफियत")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val amt = requestAmountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0 && payerMobileInput.length == 10) {
                                viewModel.createMoneyRequest(payerMobileInput, amt, requestNoteInput) {}
                            } else {
                                viewModel.showToast("Enter valid 10-digit mobile and NPR amount")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_money_request_button")
                    ) {
                        Text(
                            tr(lang, "Send Money Request & Push Alert", "रकम अनुरोध र नोटिफिकेसन पठाउनुहोस्"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tr(lang, "Recent Money Requests", "हालैका रकम अनुरोधहरू"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            moneyRequests.forEach { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp)
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
                                text = "${req.requesterName} → ${req.payerName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${req.requestId} • ${req.note}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = NprFormatter.format(req.amountNpr, lang, showCode = false),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(status = req.status, lang = lang)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
