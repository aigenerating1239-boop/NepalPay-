package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.security.SecurityManager
import com.example.ui.components.SandboxComplianceBanner
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.HimalayanNavy
import com.example.ui.theme.NepalCrimson
import com.example.ui.viewmodel.NepalPayViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: NepalPayViewModel,
    lang: AppLanguage
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Register, 2 = Forgot MPIN

    var mobileInput by remember { mutableStateOf("9841234567") }
    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var mpinLengthChoice by remember { mutableIntStateOf(4) }
    var mpinInput by remember { mutableStateOf("1234") }
    var biometricEnabled by remember { mutableStateOf(true) }

    var otpRequested by remember { mutableStateOf(false) }
    var generatedSandboxOtp by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var otpVerified by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SandboxComplianceBanner(lang = lang)

        // Hero Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "NepalPay Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                NepalCrimson.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = "NepalPay Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NepalPay • नेपालपे",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = tr(
                                lang,
                                "Secure Digital Wallet for Nepal",
                                "नेपालको सुरक्षित डिजिटल वालेट"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.toggleLanguage() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("auth_language_toggle")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == AppLanguage.ENGLISH) "नेपाली" else "English")
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = tr(
                        lang,
                        "Fast, Safe & NRB-Compliant Architecture",
                        "छिटो, सुरक्षित र भरपर्दो डिजिटल भुक्तानी"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        lang,
                        "+977 Mobile • OTP • 4/6-Digit Salted MPIN • Biometric Unlock",
                        "+977 मोबाइल • OTP • सुरक्षित MPIN र बायोमेट्रिक लगइन"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        statusMessage = null
                    },
                    text = { Text(tr(lang, "Login", "लगइन")) },
                    modifier = Modifier.testTag("tab_login")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        otpRequested = false
                        otpVerified = false
                        statusMessage = null
                    },
                    text = { Text(tr(lang, "Register", "दर्ता")) },
                    modifier = Modifier.testTag("tab_register")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        otpRequested = false
                        otpVerified = false
                        statusMessage = null
                    },
                    text = { Text(tr(lang, "Forgot MPIN", "MPIN बिर्सनुभयो?")) },
                    modifier = Modifier.testTag("tab_forgot_mpin")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // +977 Mobile Number Field
                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { input ->
                            mobileInput = input.filter { it.isDigit() }.take(10)
                        },
                        label = {
                            Text(tr(lang, "Nepal Mobile Number (+977)", "नेपाल मोबाइल नम्बर (+977)"))
                        },
                        prefix = {
                            Text("+977 ", fontWeight = FontWeight.Bold, color = NepalCrimson)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_mobile_input")
                    )

                    if (!SecurityManager.isValidNepalMobile(mobileInput) && mobileInput.isNotEmpty()) {
                        Text(
                            text = tr(
                                lang,
                                "Enter a valid 10-digit Nepal mobile starting with 98, 97, or 96",
                                "कृपया ९८, ९७ वा ९६ बाट सुरु हुने १० अंकको नम्बर राख्नुहोस्"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTab == 0) {
                        // LOGIN FLOW
                        OutlinedTextField(
                            value = mpinInput,
                            onValueChange = { mpinInput = it.filter { c -> c.isDigit() }.take(6) },
                            label = {
                                Text(tr(lang, "4 or 6 Digit Secure MPIN", "४ वा ६ अंकको सुरक्षित MPIN"))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_mpin_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "Sandbox Demo: Mobile 9841234567 • Default MPIN: 1234",
                                "स्यान्डबक्स डेमो: मोबाइल 9841234567 • डिफल्ट MPIN: 1234"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.loginWithMpinOrBiometric(
                                    mobileRaw = mobileInput,
                                    mpinInput = mpinInput,
                                    useBiometric = false
                                ) { ok, msg ->
                                    isError = !ok
                                    statusMessage = msg
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_mpin_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                tr(lang, "Login with MPIN", "MPIN बाट लगइन गर्नुहोस्"),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.loginWithMpinOrBiometric(
                                    mobileRaw = mobileInput,
                                    mpinInput = "",
                                    useBiometric = true
                                ) { ok, msg ->
                                    isError = !ok
                                    statusMessage = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_biometric_button")
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = HimalayanNavy
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                tr(
                                    lang,
                                    "Unlock with Fingerprint / Face ID",
                                    "फिंगरप्रिन्ट / फेस अनलक प्रयोग गर्नुहोस्"
                                )
                            )
                        }
                    } else {
                        // REGISTER OR FORGOT MPIN FLOW WITH OTP VERIFICATION
                        if (selectedTab == 1) {
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = { fullNameInput = it },
                                label = { Text(tr(lang, "Full Name (as per Citizenship)", "पूरा नाम (नागरिकता अनुसार)")) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text(tr(lang, "Email Address (Optional)", "इमेल ठेगाना")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Step 1: Request & Verify OTP
                        if (!otpRequested) {
                            Button(
                                onClick = {
                                    if (!SecurityManager.isValidNepalMobile(mobileInput)) {
                                        isError = true
                                        statusMessage = "Please enter a valid 10-digit +977 mobile number."
                                        return@Button
                                    }
                                    scope.launch {
                                        val code = viewModel.requestOtp(mobileInput)
                                        generatedSandboxOtp = code
                                        otpRequested = true
                                        isError = false
                                        statusMessage = "Sandbox OTP dispatched to +977-$mobileInput: $code"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HimalayanNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("send_otp_button")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    tr(
                                        lang,
                                        "Send 6-Digit OTP to +977 Mobile",
                                        "+977 नम्बरमा ६ अंकको OTP पठाउनुहोस्"
                                    )
                                )
                            }
                        } else if (!otpVerified) {
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { otpInput = it.filter { c -> c.isDigit() }.take(6) },
                                label = { Text(tr(lang, "Enter 6-Digit SMS OTP", "६ अंकको OTP कोड राख्नुहोस्")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sandbox OTP: $generatedSandboxOtp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { otpInput = generatedSandboxOtp }) {
                                    Text(tr(lang, "Auto-Fill OTP", "OTP अटो-फिल"))
                                }
                            }
                            Button(
                                onClick = {
                                    if (otpInput == generatedSandboxOtp && otpInput.isNotBlank()) {
                                        otpVerified = true
                                        isError = false
                                        statusMessage = "OTP verified! Set your secure MPIN below."
                                    } else {
                                        isError = true
                                        statusMessage = "Invalid OTP code. Use Sandbox OTP: $generatedSandboxOtp"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("verify_otp_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr(lang, "Verify OTP", "OTP प्रमाणित गर्नुहोस्"))
                            }
                        } else {
                            // Step 2: Set 4 or 6 digit MPIN
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = tr(lang, "MPIN Length:", "MPIN लम्बाइ:"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FilterChip(
                                    selected = mpinLengthChoice == 4,
                                    onClick = {
                                        mpinLengthChoice = 4
                                        mpinInput = mpinInput.take(4)
                                    },
                                    label = { Text("4-Digit MPIN") }
                                )
                                FilterChip(
                                    selected = mpinLengthChoice == 6,
                                    onClick = { mpinLengthChoice = 6 },
                                    label = { Text("6-Digit MPIN") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = mpinInput,
                                onValueChange = {
                                    mpinInput = it.filter { c -> c.isDigit() }.take(mpinLengthChoice)
                                },
                                label = {
                                    Text(
                                        tr(
                                            lang,
                                            "Set New $mpinLengthChoice-Digit MPIN",
                                            "नयाँ $mpinLengthChoice अंकको MPIN सेट गर्नुहोस्"
                                        )
                                    )
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_mpin_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(
                                        lang,
                                        "Enable Fingerprint / Face Unlock",
                                        "फिंगरप्रिन्ट / फेस अनलक सुचारु गर्नुहोस्"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { biometricEnabled = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (mpinInput.length != mpinLengthChoice) {
                                        isError = true
                                        statusMessage = "Please enter a $mpinLengthChoice-digit MPIN."
                                        return@Button
                                    }
                                    viewModel.registerOrRecoverAccount(
                                        mobileRaw = mobileInput,
                                        fullName = fullNameInput,
                                        email = emailInput,
                                        newMpin = mpinInput,
                                        biometricEnabled = biometricEnabled
                                    ) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("complete_registration_button")
                            ) {
                                Icon(Icons.Default.LockReset, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (selectedTab == 1)
                                        tr(lang, "Create NepalPay Wallet", "वालेट खाता खोल्नुहोस्")
                                    else
                                        tr(lang, "Reset MPIN & Login", "MPIN रिसेट गरी लगइन गर्नुहोस्")
                                )
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Sandbox Demo Account Switcher Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = HimalayanNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                lang,
                                "One-Tap Sandbox Demo Accounts",
                                "स्यान्डबक्स डेमो खाताहरू (एक-ट्याप लगइन)"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.switchDemoAccount("+9779841234567") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_login_aarav")
                        ) {
                            Text("Aarav (Verified)", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { viewModel.switchDemoAccount("+9779801122334") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_login_sita")
                        ) {
                            Text("Sita (User)", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { viewModel.switchDemoAccount("+9779812345678") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_login_ramesh")
                        ) {
                            Text("Ramesh (KYC)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
