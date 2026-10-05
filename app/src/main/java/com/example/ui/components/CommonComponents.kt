package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.TransactionEntity
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import com.example.ui.i18n.tr
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.RubyError
import com.example.ui.theme.RubyErrorContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun SandboxComplianceBanner(
    lang: AppLanguage,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AmberWarningContainer,
        contentColor = Color(0xFF78350F),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sandbox_compliance_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "NRB Sandbox Notice",
                tint = AmberWarning,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tr(
                    lang,
                    "NRB SANDBOX / DEMO MODE • Mock transactions only (No real money transferred)",
                    "NRB स्यान्डबक्स / डेमो मोड • परीक्षण कारोबार मात्र (वास्तविक पैसा कारोबार हुँदैन)"
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatusBadge(status: String, lang: AppLanguage) {
    val (bg, fg, label) = when (status.uppercase()) {
        "SUCCESSFUL", "VERIFIED", "PAID", "ACTIVE" -> Triple(
            EmeraldSuccessContainer,
            EmeraldSuccess,
            when (status.uppercase()) {
                "VERIFIED" -> tr(lang, "VERIFIED", "प्रमाणित")
                "PAID" -> tr(lang, "PAID", "भुक्तानी भयो")
                else -> tr(lang, "SUCCESSFUL", "सफल")
            }
        )
        "PENDING" -> Triple(
            AmberWarningContainer,
            AmberWarning,
            tr(lang, "PENDING", "पेन्डिङ")
        )
        else -> Triple(
            RubyErrorContainer,
            RubyError,
            when (status.uppercase()) {
                "REJECTED" -> tr(lang, "REJECTED", "अस्वीकृत")
                "SUSPENDED" -> tr(lang, "SUSPENDED", "निलम्बित")
                else -> tr(lang, "FAILED", "असफल")
            }
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun MpinAuthorizationDialog(
    lang: AppLanguage,
    title: String,
    subtitle: String,
    amountFormatted: String,
    biometricEnabled: Boolean,
    expectedLength: Int = 4,
    onVerifyMpin: (String) -> Pair<Boolean, String>,
    onAuthorized: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("mpin_auth_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure MPIN",
                            tint = NepalCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = amountFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(expectedLength) { index ->
                        val filled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) NepalCrimson
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (filled) NepalCrimson else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tr(
                        lang,
                        "Sandbox Hint: Default MPIN is 1234",
                        "स्यान्डबक्स संकेत: डिफल्ट MPIN 1234 हो"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Numeric Keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { rowDigits ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowDigits.forEach { digit ->
                            PinKeyButton(label = digit) {
                                if (enteredPin.length < expectedLength) {
                                    enteredPin += digit
                                    errorText = null
                                    if (enteredPin.length == expectedLength) {
                                        val (ok, msg) = onVerifyMpin(enteredPin)
                                        if (ok) {
                                            onAuthorized()
                                        } else {
                                            errorText = msg
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (biometricEnabled) {
                        Surface(
                            onClick = onAuthorized,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("biometric_authorize_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Unlock",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(60.dp))
                    }

                    PinKeyButton(label = "0") {
                        if (enteredPin.length < expectedLength) {
                            enteredPin += "0"
                            errorText = null
                            if (enteredPin.length == expectedLength) {
                                val (ok, msg) = onVerifyMpin(enteredPin)
                                if (ok) {
                                    onAuthorized()
                                } else {
                                    errorText = msg
                                    enteredPin = ""
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (biometricEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onAuthorized,
                        modifier = Modifier.testTag("instant_biometric_pay_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            tr(
                                lang,
                                "Tap to Authorize with Fingerprint / Face Unlock",
                                "फिंगरप्रिन्ट / फेस अनलकबाट भुक्तानी पुष्टि गर्नुहोस्"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(60.dp)
            .testTag("pin_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DigitalReceiptDialog(
    tx: TransactionEntity,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
    val formattedDate = remember(tx.timestamp) { dateFormat.format(Date(tx.timestamp)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .verticalScroll(rememberScrollState())
                .testTag("digital_receipt_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sandbox Watermark Header
                Surface(
                    color = AmberWarningContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = tr(
                            lang,
                            "SANDBOX / DEMO DIGITAL RECEIPT",
                            "स्यान्डबक्स / डेमो डिजिटल रसिद"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val statusIcon = when (tx.status) {
                    "SUCCESSFUL" -> Icons.Default.CheckCircle
                    "PENDING" -> Icons.Default.Schedule
                    else -> Icons.Default.Error
                }
                val statusColor = when (tx.status) {
                    "SUCCESSFUL" -> EmeraldSuccess
                    "PENDING" -> AmberWarning
                    else -> RubyError
                }

                Icon(
                    imageVector = statusIcon,
                    contentDescription = tx.status,
                    tint = statusColor,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(status = tx.status, lang = lang)

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = NprFormatter.format(tx.totalAmountNpr, lang),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                ReceiptDetailRow(tr(lang, "Transaction ID", "कारोबार आईडी"), tx.txId, isMono = true)
                ReceiptDetailRow(tr(lang, "Sender", "पठाउने"), "${tx.senderName}\n(${tx.senderMobile})")
                ReceiptDetailRow(tr(lang, "Receiver", "पाउने"), "${tx.receiverName}\n(${tx.receiverIdentifier})")
                ReceiptDetailRow(tr(lang, "Amount", "रकम"), NprFormatter.format(tx.amountNpr, lang))
                ReceiptDetailRow(tr(lang, "Service Fee", "सेवा शुल्क"), NprFormatter.format(tx.feeNpr, lang))
                ReceiptDetailRow(tr(lang, "Total Amount", "कुल रकम"), NprFormatter.format(tx.totalAmountNpr, lang))
                ReceiptDetailRow(tr(lang, "Payment Method", "भुक्तानी माध्यम"), tx.paymentMethod)
                if (tx.bankName.isNotBlank()) {
                    ReceiptDetailRow(tr(lang, "Bank / Switch", "बैंक"), tx.bankName)
                }
                ReceiptDetailRow(tr(lang, "Note / Remarks", "कैफियत"), tx.note)

                if (tx.isFlaggedSuspicious) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = AmberWarningContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tx.fraudReason,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppGood,
                            contentDescription = null,
                            tint = HimalayanNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "NepalPay Sandbox Receipt • Not a real bank settlement receipt.",
                                "नेपालपे स्यान्डबक्स रसिद • यो वास्तविक बैंक कारोबार होइन।"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_receipt_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(lang, "Save Receipt", "डाउनलोड"))
                    }

                    Button(
                        onClick = {
                            val shareText = buildString {
                                appendLine("=== NepalPay Sandbox Digital Receipt ===")
                                appendLine("Status: ${tx.status} [SANDBOX DEMO]")
                                appendLine("Transaction ID: ${tx.txId}")
                                appendLine("Date: $formattedDate")
                                appendLine("Sender: ${tx.senderName} (${tx.senderMobile})")
                                appendLine("Receiver: ${tx.receiverName} (${tx.receiverIdentifier})")
                                appendLine("Amount: NPR ${tx.amountNpr}")
                                appendLine("Fee: NPR ${tx.feeNpr}")
                                appendLine("Total: NPR ${tx.totalAmountNpr}")
                                appendLine("Method: ${tx.paymentMethod}")
                                appendLine("Note: ${tx.note}")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "NepalPay Receipt ${tx.txId}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Receipt"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(lang, "Share", "शेयर गर्नुहोस्"))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(tr(lang, "Done", "बन्द गर्नुहोस्"))
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(
    label: String,
    value: String,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.SansSerif,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
fun NepalPayQrCodeBox(
    payload: String,
    size: Dp = 210.dp,
    modifier: Modifier = Modifier
) {
    val seedHash = remember(payload) { payload.hashCode() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(2.dp, NepalCrimson.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 32.dp)) {
            val grid = 21
            val cellSize = this.size.width / grid

            fun isFinderPattern(r: Int, c: Int): Boolean {
                val topLeft = r in 0..6 && c in 0..6
                val topRight = r in 0..6 && c in (grid - 7) until grid
                val bottomLeft = r in (grid - 7) until grid && c in 0..6
                return topLeft || topRight || bottomLeft
            }

            fun drawFinderSquare(startRow: Int, startCol: Int) {
                for (r in 0..6) {
                    for (c in 0..6) {
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isInner = r in 2..4 && c in 2..4
                        if (isBorder || isInner) {
                            drawRect(
                                color = if (isInner) NepalCrimson else Color(0xFF0F172A),
                                topLeft = Offset((startCol + c) * cellSize, (startRow + r) * cellSize),
                                size = Size(cellSize * 0.95f, cellSize * 0.95f)
                            )
                        }
                    }
                }
            }

            drawFinderSquare(0, 0)
            drawFinderSquare(0, grid - 7)
            drawFinderSquare(grid - 7, 0)

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (isFinderPattern(r, c)) continue
                    if (r in 8..12 && c in 8..12) continue // Reserve center for NepalPay badge
                    val bit = abs((seedHash xor (r * 37 + c * 101) xor payload.length)) % 3 != 0
                    if (bit) {
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                }
            }
        }

        Surface(
            color = NepalCrimson,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "NP",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
