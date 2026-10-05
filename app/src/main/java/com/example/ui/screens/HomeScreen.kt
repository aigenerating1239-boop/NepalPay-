package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.LinkedBankAccountEntity
import com.example.data.local.MoneyRequestEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import com.example.ui.i18n.tr
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.NepalPayViewModel
import com.example.ui.viewmodel.SendMoneyMode
import com.example.ui.viewmodel.WalletActionTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    banksCount: Int,
    linkedAccounts: List<LinkedBankAccountEntity>,
    transactions: List<TransactionEntity>,
    moneyRequests: List<MoneyRequestEntity>,
    unreadNotificationCount: Int,
    balanceVisible: Boolean,
    lang: AppLanguage
) {
    val pendingIncomingRequests = moneyRequests.filter {
        it.status == "PENDING" && it.payerMobile == user?.mobileNumber
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("home_dashboard_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.navigateTo(AppScreen.PROFILE) }
                        .padding(4.dp)
                        .testTag("home_profile_header")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_avatar_default),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr(lang, "Namaste, 👋", "नमस्ते, 👋"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (lang == AppLanguage.NEPALI) (user?.fullNameNe ?: "आरव शर्मा")
                            else (user?.fullName ?: "Aarav Sharma"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user?.mobileNumber ?: "+9779841234567",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(status = user?.kycStatus ?: "VERIFIED", lang = lang)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { viewModel.toggleLanguage() },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.testTag("home_lang_switch")
                    ) {
                        Text(
                            text = if (lang == AppLanguage.ENGLISH) "नेपाली" else "EN",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.NOTIFICATIONS) },
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text(unreadNotificationCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.lockSession() },
                        modifier = Modifier.testTag("home_lock_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Session",
                            tint = NepalCrimson
                        )
                    }
                }
            }
        }

        // Hero Wallet Balance Card in NPR (रु.)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        NepalCrimson.copy(alpha = 0.92f),
                                        HimalayanNavy.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = tr(
                                        lang,
                                        "Available Wallet Balance",
                                        "उपलब्ध वालेट मौज्दात (NPR)"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (balanceVisible) {
                                            NprFormatter.format(user?.walletBalanceNpr ?: 48750.0, lang)
                                        } else {
                                            "NPR रु. ••••••"
                                        },
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.testTag("wallet_balance_text")
                                    )
                                    IconButton(
                                        onClick = { viewModel.toggleBalanceVisibility() },
                                        modifier = Modifier.testTag("toggle_balance_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (balanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Balance Visibility",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = user?.paymentId ?: "aarav@nepalpay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.openWalletWithTab(WalletActionTab.ADD_MONEY) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = NepalCrimson
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_add_money_btn")
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr(lang, "Add Money", "रकम थप्नुहोस्"),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.openWalletWithTab(WalletActionTab.WITHDRAW_MONEY) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_withdraw_btn")
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tr(lang, "Withdraw", "रकम झिक्नुहोस्"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Core Quick Services Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr(lang, "Quick Payment & Banking Services", "भुक्तानी र बैंकिङ सेवाहरू"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.AccountBalance,
                            label = tr(lang, "Nepal Banks", "नेपाल बैंकहरू"),
                            tint = HimalayanNavy,
                            tag = "action_nepal_banks_directory"
                        ) {
                            viewModel.navigateTo(AppScreen.BANKS_DIRECTORY)
                        }
                        QuickActionItem(
                            icon = Icons.AutoMirrored.Filled.Send,
                            label = tr(lang, "Send Money", "पैसा पठाउनुहोस्"),
                            tint = NepalCrimson,
                            tag = "action_send_money"
                        ) {
                            viewModel.openSendMoney(SendMoneyMode.MOBILE)
                        }
                        QuickActionItem(
                            icon = Icons.Default.QrCode2,
                            label = tr(lang, "Receive Money", "पैसा पाउनुहोस्"),
                            tint = EmeraldSuccess,
                            tag = "action_receive_money"
                        ) {
                            viewModel.navigateTo(AppScreen.RECEIVE_MONEY)
                        }
                        QuickActionItem(
                            icon = Icons.Default.QrCodeScanner,
                            label = tr(lang, "Scan & Pay", "स्क्यान र पे"),
                            tint = Color(0xFF0284C7),
                            tag = "action_scan_pay"
                        ) {
                            viewModel.navigateTo(AppScreen.QR_SCAN_PAY)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.AddLink,
                            label = tr(lang, "Link Bank A/C", "बैंक लिंक"),
                            tint = Color(0xFF7C3AED),
                            tag = "action_link_bank"
                        ) {
                            viewModel.openLinkBankAccount(null)
                        }
                        QuickActionItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = tr(lang, "My Wallet", "मेरो वालेट"),
                            tint = Color(0xFFD97706),
                            tag = "action_wallet"
                        ) {
                            viewModel.openWalletWithTab(WalletActionTab.OVERVIEW)
                        }
                        QuickActionItem(
                            icon = Icons.Default.VerifiedUser,
                            label = tr(lang, "NRB KYC", "KYC विवरण"),
                            tint = EmeraldSuccess,
                            tag = "action_kyc"
                        ) {
                            viewModel.navigateTo(AppScreen.KYC_VERIFICATION)
                        }
                        QuickActionItem(
                            icon = Icons.Default.AdminPanelSettings,
                            label = tr(lang, "Admin Panel", "एडमिन प्यानल"),
                            tint = NepalCrimson,
                            tag = "action_admin_dashboard"
                        ) {
                            viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD)
                        }
                    }
                }
            }
        }

        // Featured Dynamic Nepal Banks Directory Banner + Linked Bank Accounts
        item {
            Card(
                onClick = { viewModel.navigateTo(AppScreen.BANKS_DIRECTORY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_banks_directory_banner"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    lang,
                                    "Nepal Licensed Banks Directory ($banksCount Banks)",
                                    "नेपालका इजाजतपत्र प्राप्त बैंकहरूको डाइरेक्टरी ($banksCount)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = tr(
                                    lang,
                                    "Official Websites • Per-Bank API Connectors • Account Linking • Transfers",
                                    "आधिकारिक वेबसाइट • एपीआई कनेक्टर • खाता लिंक र बैंक ट्रान्सफर"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.BANKS_DIRECTORY) },
                            colors = ButtonDefaults.buttonColors(containerColor = HimalayanNavy)
                        ) {
                            Text(tr(lang, "Explore", "हेर्नुहोस्"))
                        }
                    }

                    if (linkedAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        linkedAccounts.take(2).forEach { acc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${acc.officialBankName} (${acc.accountNumberMasked})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = acc.environmentMode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NepalCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pending Incoming Money Requests Section
        if (pendingIncomingRequests.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr(
                                lang,
                                "Pending Money Requests (${pendingIncomingRequests.size})",
                                "भुक्तानी अनुरोधहरू (${pendingIncomingRequests.size})"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        pendingIncomingRequests.forEach { req ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${req.requesterName} • ${NprFormatter.format(req.amountNpr, lang)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = req.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = { viewModel.respondToMoneyRequest(req, approve = false) }
                                    ) {
                                        Text(tr(lang, "Decline", "अस्वीकार"))
                                    }
                                    Button(
                                        onClick = { viewModel.respondToMoneyRequest(req, approve = true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                                        modifier = Modifier.testTag("approve_request_${req.requestId}")
                                    ) {
                                        Text(tr(lang, "Pay Now", "तिर्नुहोस्"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(lang, "Recent Transactions", "हालैका कारोबारहरू"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.navigateTo(AppScreen.TRANSACTIONS) },
                    modifier = Modifier.testTag("view_all_transactions_btn")
                ) {
                    Text(tr(lang, "See All", "सबै हेर्नुहोस्"))
                }
            }
        }

        items(transactions.take(5), key = { it.txId }) { tx ->
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
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(tag)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionEntity,
    currentUserMobile: String,
    lang: AppLanguage,
    onClick: () -> Unit
) {
    val isIncoming = tx.receiverIdentifier == currentUserMobile || tx.txType == "ADD_MONEY"
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_card_${tx.txId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isIncoming) EmeraldSuccess.copy(alpha = 0.14f)
                    else NepalCrimson.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isIncoming) Icons.AutoMirrored.Filled.CallReceived
                            else Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = if (isIncoming) EmeraldSuccess else NepalCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isIncoming) tx.senderName else tx.receiverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tx.txId} • ${dateFormat.format(Date(tx.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncoming) "+ " else "- ") +
                        NprFormatter.format(tx.totalAmountNpr, lang, showCode = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isIncoming) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                )
                StatusBadge(status = tx.status, lang = lang)
            }
        }
    }
}
