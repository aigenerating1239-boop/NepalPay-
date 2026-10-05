package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AuditLogEntity
import com.example.data.local.KycSubmissionEntity
import com.example.data.local.LinkedBankAccountEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.UserEntity
import com.example.ui.components.StatusBadge
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.theme.RubyError
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.NepalPayViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    viewModel: NepalPayViewModel,
    notifications: List<NotificationEntity>,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("notifications_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = tr(lang, "Notifications & Alerts", "नोटिफिकेसन र सूचनाहरू"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                TextButton(
                    onClick = { viewModel.markAllNotificationsRead() },
                    modifier = Modifier.testTag("mark_all_read_button")
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr(lang, "Mark Read", "सबै पढियो"))
                }
            }
        }

        items(notifications, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!item.isRead)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == AppLanguage.NEPALI) item.titleNe else item.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormat.format(Date(item.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == AppLanguage.NEPALI) item.messageNe else item.messageEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ProfileScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    linkedAccounts: List<LinkedBankAccountEntity>,
    darkTheme: Boolean,
    notificationsEnabled: Boolean,
    lang: AppLanguage
) {
    var showChangeMpinCard by remember { mutableStateOf(false) }
    var currentMpinInput by remember { mutableStateOf("") }
    var newMpinInput by remember { mutableStateOf("") }
    var showHelpSupport by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = tr(lang, "Profile, Linked Banks & Settings", "प्रोफाइल, लिंक गरिएका बैंक र सेटिङ"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_avatar_default),
                        contentDescription = "Profile Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${user?.fullName ?: "Aarav Sharma"} (${user?.fullNameNe ?: "आरव शर्मा"})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Mobile: ${user?.mobileNumber ?: "+9779841234567"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Email: ${user?.email ?: "aarav.sharma@nepalpay.com.np"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Payment ID: ${user?.paymentId ?: "aarav@nepalpay"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NepalCrimson,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tr(lang, "NRB KYC Status: ", "KYC स्थिति: "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(status = user?.kycStatus ?: "VERIFIED", lang = lang)
                    }
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.KYC_VERIFICATION) },
                        modifier = Modifier.testTag("profile_open_kyc_button")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(lang, "Manage KYC", "KYC विवरण"))
                    }
                }
            }
        }

        // Linked Bank Accounts Section in User Profile
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_linked_banks_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = HimalayanNavy)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr(
                                lang,
                                "Linked Bank Accounts (${linkedAccounts.size})",
                                "लिंक गरिएका बैंक खाताहरू (${linkedAccounts.size})"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { viewModel.navigateTo(AppScreen.BANKS_DIRECTORY) },
                        modifier = Modifier.testTag("profile_open_banks_directory_btn")
                    ) {
                        Text(tr(lang, "All Banks", "सबै बैंकहरू"))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                linkedAccounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = acc.officialBankName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${acc.accountHolderName} • ${acc.accountNumberMasked} (${acc.bankCode})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = acc.verificationProvider,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.removeLinkedBankAccount(acc.linkId) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Unlink Bank Account",
                                tint = RubyError
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.openLinkBankAccount(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_link_new_bank_btn")
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(lang, "Add / Link Bank Account", "नयाँ बैंक खाता लिंक गर्नुहोस्"))
                }
            }
        }

        // Settings Switches Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NepalCrimson)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(lang, "App Language / भाषा", "एपको भाषा / Language"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "नेपाली / English",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = lang == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = lang == AppLanguage.NEPALI,
                            onClick = { viewModel.setLanguage(AppLanguage.NEPALI) },
                            label = { Text("नेपाली") }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = HimalayanNavy)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tr(lang, "Fingerprint / Face Unlock", "फिंगरप्रिन्ट र फेस अनलक"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(lang, "Biometric payment & login auth", "बायोमेट्रिक लगइन र भुक्तानी"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = user?.biometricEnabled ?: true,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = HimalayanNavy)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tr(lang, "Dark Mode Theme", "डार्क मोड थिम"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { viewModel.toggleDarkTheme() },
                        modifier = Modifier.testTag("toggle_dark_mode_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tr(lang, "Push Notifications", "पुश नोटिफिकेसन"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotificationsEnabled() }
                    )
                }
            }
        }

        // Change MPIN Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = NepalCrimson)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr(lang, "Change Secure MPIN", "MPIN परिवर्तन गर्नुहोस्"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { showChangeMpinCard = !showChangeMpinCard }) {
                        Text(if (showChangeMpinCard) tr(lang, "Hide", "लुकाउनुहोस्") else tr(lang, "Change", "बदल्नुहोस्"))
                    }
                }

                if (showChangeMpinCard) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentMpinInput,
                        onValueChange = { currentMpinInput = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(tr(lang, "Current MPIN (Default: 1234)", "हालको MPIN")) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMpinInput,
                        onValueChange = { newMpinInput = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text(tr(lang, "New 4 or 6 Digit MPIN", "नयाँ ४ वा ६ अंकको MPIN")) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newMpinInput.length != 4 && newMpinInput.length != 6) {
                                viewModel.showToast("New MPIN must be 4 or 6 digits")
                            } else {
                                viewModel.updateMpin(currentMpinInput, newMpinInput) { ok, msg ->
                                    viewModel.showToast(msg)
                                    if (ok) {
                                        currentMpinInput = ""
                                        newMpinInput = ""
                                        showChangeMpinCard = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(lang, "Update Salted SHA-256 MPIN", "MPIN अपडेट गर्नुहोस्"))
                    }
                }
            }
        }

        // Security, Admin, Help & Logout Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigateTo(AppScreen.SECURITY_AUDIT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("open_security_audit_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr(lang, "Privacy & Security", "सुरक्षा र अडिट"))
            }

            OutlinedButton(
                onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_open_admin_button")
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr(lang, "Admin Panel", "एडमिन प्यानल"))
            }
        }

        OutlinedButton(
            onClick = { showHelpSupport = !showHelpSupport },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(tr(lang, "Help, Support & NRB Compliance Notice", "सहयोग र NRB नियम जानकारी"))
        }

        if (showHelpSupport) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr(
                            lang,
                            "NepalPay Support & Regulatory Compliance",
                            "नेपालपे ग्राहक सेवा र नियामक अनुपालन"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tr(
                            lang,
                            "• Toll-Free (Nepal): 1660-01-NEPAL (Sandbox)\n" +
                                "• Support Email: support@nepalpay.com.np\n" +
                                "• Compliance: Designed in accordance with Nepal Rastra Bank (NRB) PSP licensing, AML/CFT, and KYC guidelines. Currently operating in Sandbox/Demo mode.",
                            "• टोल-फ्री (नेपाल): 1660-01-NEPAL (स्यान्डबक्स)\n" +
                                "• इमेल: support@nepalpay.com.np\n" +
                                "• अनुपालन: नेपाल राष्ट्र बैंक (NRB) को मापदण्ड र KYC/AML नियम अनुसार निर्मित डेमो/स्यान्डबक्स संस्करण।"
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(tr(lang, "Logout Securely", "सुरक्षित लगआउट गर्नुहोस्"), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun KycScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    existingKyc: KycSubmissionEntity?,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }

    var fullName by remember(existingKyc, user) {
        mutableStateOf(existingKyc?.fullName ?: user?.fullName ?: "Aarav Sharma")
    }
    var dob by remember(existingKyc) {
        mutableStateOf(existingKyc?.dateOfBirth ?: "2054-05-19 BS (1997-09-04 AD)")
    }
    var province by remember(existingKyc) {
        mutableStateOf(existingKyc?.province ?: "Bagmati Province (बागमती प्रदेश)")
    }
    var district by remember(existingKyc) {
        mutableStateOf(existingKyc?.district ?: "Kathmandu (काठमाडौँ)")
    }
    var municipality by remember(existingKyc) {
        mutableStateOf(existingKyc?.municipality ?: "Kathmandu Metropolitan City")
    }
    var wardNo by remember(existingKyc) {
        mutableStateOf(existingKyc?.wardNo ?: "10")
    }
    var docType by remember(existingKyc) {
        mutableStateOf(existingKyc?.documentType ?: "CITIZENSHIP")
    }
    var docNumber by remember { mutableStateOf("27-01-74-09482") }
    var docUploadedName by remember(existingKyc) {
        mutableStateOf(existingKyc?.documentUploadedName ?: "nepal_citizenship_front_back.jpg")
    }
    var selfieVerified by remember(existingKyc) {
        mutableStateOf(existingKyc?.selfieVerified ?: true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("kyc_verification_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = tr(lang, "NRB KYC Verification", "NRB ग्राहक पहिचान (KYC)"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(lang, "Current KYC Status", "हालको KYC स्थिति"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = existingKyc?.reviewerNote
                            ?: tr(lang, "Encrypted storage compliant with NRB KYC guidelines", "NRB मापदण्ड अनुसार सुरक्षित इन्क्रिप्टेड KYC"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (existingKyc != null) {
                        Text(
                            text = "Masked ID: ${existingKyc.documentNumberMasked}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                StatusBadge(status = user?.kycStatus ?: "VERIFIED", lang = lang)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(tr(lang, "Full Name (as per Citizenship)", "पूरा नाम (नागरिकता अनुसार)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text(tr(lang, "Date of Birth (BS / AD)", "जन्म मिति (वि.सं. / ई.सं.)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text(tr(lang, "Province", "प्रदेश")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(tr(lang, "District", "जिल्ला")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wardNo,
                        onValueChange = { wardNo = it },
                        label = { Text(tr(lang, "Ward No.", "वडा नं.")) },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f)
                    )
                }
                OutlinedTextField(
                    value = municipality,
                    onValueChange = { municipality = it },
                    label = { Text(tr(lang, "Municipality / Rural Municipality", "नगरपालिका / गाउँपालिका")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = tr(lang, "Approved Identity Document Type:", "पहिचान कागजातको प्रकार:"),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "CITIZENSHIP" to tr(lang, "Citizenship", "नागरिकता"),
                        "NATIONAL_ID" to tr(lang, "National ID", "राष्ट्रिय परिचयपत्र"),
                        "PASSPORT" to tr(lang, "Passport", "राहदानी")
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = docType == code,
                            onClick = { docType = code },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = docNumber,
                    onValueChange = { docNumber = it },
                    label = {
                        Text(
                            tr(
                                lang,
                                "Document Number (Encrypted with AES-256)",
                                "कागजात नम्बर (AES-256 द्वारा सुरक्षित गरिने)"
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kyc_doc_number_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            docUploadedName = "${docType.lowercase()}_verified_${System.currentTimeMillis() % 1000}.jpg"
                            viewModel.showToast("Attached: $docUploadedName")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(lang, "Upload ID Copy", "कागजात अपलोड"))
                    }

                    OutlinedButton(
                        onClick = {
                            selfieVerified = true
                            viewModel.showToast("Live Liveness Selfie Verified")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (selfieVerified) Icons.Default.CheckCircle else Icons.Default.CameraFront,
                            contentDescription = null,
                            tint = if (selfieVerified) EmeraldSuccess else HimalayanNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (selfieVerified) tr(lang, "Selfie Verified", "सेल्फी प्रमाणित")
                            else tr(lang, "Take Selfie", "सेल्फी खिच्नुहोस्")
                        )
                    }
                }

                Text(
                    text = "Uploaded File: $docUploadedName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        viewModel.submitKycApplication(
                            fullName = fullName,
                            dob = dob,
                            province = province,
                            district = district,
                            municipality = municipality,
                            wardNo = wardNo,
                            docType = docType,
                            docNumber = docNumber,
                            docFileName = docUploadedName,
                            selfieVerified = selfieVerified
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_kyc_button")
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(lang, "Encrypt & Submit KYC for Verification", "सुरक्षित रूपमा KYC पेश गर्नुहोस्"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SecurityAuditScreen(
    viewModel: NepalPayViewModel,
    user: UserEntity?,
    auditLogs: List<AuditLogEntity>,
    lang: AppLanguage
) {
    BackHandler { viewModel.navigateBack() }
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("security_audit_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = tr(lang, "Security Center & Audit Logs", "सुरक्षा केन्द्र र अडिट लग"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GppGood, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(lang, "Active FinTech Security Controls", "सक्रिय सुरक्षा प्रविधिहरू"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SecurityControlBullet("MPIN & OTP Hashing", "Salted SHA-256 (Zero Plain-Text Storage)")
                    SecurityControlBullet("KYC & Bank Tokens", "AES-256-GCM Authenticated Encryption")
                    SecurityControlBullet("Webhook Verification", "HMAC-SHA256 Signed Callback Verification")
                    SecurityControlBullet("API Key Vault", "Encrypted Server-Side Vault (Never in Frontend)")
                    SecurityControlBullet("Transport Security", "HTTPS / TLS 1.3 Enforced for Bank Links")
                    SecurityControlBullet("Active Session Token", user?.sessionToken ?: "NPSESS-ACTIVE")
                    SecurityControlBullet("Session Timeout", "5-Minute Auto Inactivity Lock Enabled")
                    SecurityControlBullet("Brute-Force Rate Limit", "Max 4 MPIN/OTP Attempts before Cooldown")
                    SecurityControlBullet("AML / Fraud Engine", "Real-time flagging for transfers >= NPR 50,000")
                }
            }
        }

        item {
            Text(
                text = tr(lang, "Immutable Security Audit Logs", "सुरक्षा अडिट लगहरू"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(auditLogs, key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${log.actionType} (${log.severity})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (log.severity == "CRITICAL" || log.severity == "WARNING")
                                NepalCrimson else HimalayanNavy
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${log.userMobile} • ${log.deviceMetadata}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SecurityControlBullet(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
