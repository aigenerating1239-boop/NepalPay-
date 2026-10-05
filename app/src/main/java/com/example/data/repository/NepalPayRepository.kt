package com.example.data.repository

import android.content.Context
import com.example.data.local.ApiErrorLogEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.KycSubmissionEntity
import com.example.data.local.LinkedBankAccountEntity
import com.example.data.local.MoneyRequestEntity
import com.example.data.local.NepalPayDao
import com.example.data.local.NotificationEntity
import com.example.data.local.ReconciliationLogEntity
import com.example.data.local.SandboxMerchantQr
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.security.SecurityManager
import com.example.data.service.AccountVerificationResult
import com.example.data.service.GatewayResponse
import com.example.data.service.ParsedQrResult
import com.example.data.service.SandboxNepalPaymentGateway
import com.example.data.service.SystemNotificationDispatcher
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class NepalPayRepository(
    private val dao: NepalPayDao,
    private val gateway: SandboxNepalPaymentGateway = SandboxNepalPaymentGateway(),
    private val appContext: Context? = null
) {

    val allUsersFlow: Flow<List<UserEntity>> = dao.observeAllUsers()
    val allBanksFlow: Flow<List<BankDirectoryEntity>> = dao.observeAllBanks()
    val allTransactionsFlow: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val allKycSubmissionsFlow: Flow<List<KycSubmissionEntity>> = dao.observeAllKycSubmissions()
    val reconciliationLogsFlow: Flow<List<ReconciliationLogEntity>> = dao.observeReconciliationLogs()
    val apiErrorLogsFlow: Flow<List<ApiErrorLogEntity>> = dao.observeApiErrorLogs()
    val auditLogsFlow: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()

    val sandboxMerchants: List<SandboxMerchantQr> get() = gateway.sandboxMerchants

    fun observeUser(mobile: String): Flow<UserEntity?> = dao.observeUser(mobile)

    fun observeLinkedBankAccounts(mobile: String): Flow<List<LinkedBankAccountEntity>> =
        dao.observeLinkedBankAccounts(mobile)

    fun observeUserTransactions(mobile: String, paymentId: String): Flow<List<TransactionEntity>> =
        dao.observeUserTransactions(mobile, paymentId)

    fun observeMoneyRequests(mobile: String): Flow<List<MoneyRequestEntity>> =
        dao.observeMoneyRequestsForUser(mobile)

    fun observeKyc(mobile: String): Flow<KycSubmissionEntity?> =
        dao.observeKycForUser(mobile)

    fun observeNotifications(mobile: String): Flow<List<NotificationEntity>> =
        dao.observeNotifications(mobile)

    fun parseQrCode(payload: String): ParsedQrResult = gateway.parseNepalPayQr(payload)

    fun buildPersonalQrPayload(user: UserEntity): String =
        gateway.generatePersonalQrPayload(user.mobileNumber, user.paymentId, user.fullName)

    suspend fun requestSandboxOtp(mobile: String): String {
        val code = gateway.sendSandboxOtp(mobile)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = mobile,
                actionType = "OTP_DISPATCHED",
                details = "Sandbox 6-digit OTP generated via Telecom SMS Gateway (Hashed verification)",
                deviceMetadata = "Android 15 • +977 Gateway",
                severity = "INFO"
            )
        )
        return code
    }

    suspend fun verifyBankAccountHolder(
        bank: BankDirectoryEntity,
        accountNumber: String,
        holderName: String
    ): AccountVerificationResult {
        return gateway.verifyBankAccountHolderViaConnector(bank, accountNumber, holderName)
    }

    suspend fun linkBankAccount(
        user: UserEntity,
        bank: BankDirectoryEntity,
        accountNumberPlain: String,
        verifiedHolderName: String,
        isPrimary: Boolean
    ): LinkedBankAccountEntity {
        val masked = SecurityManager.maskCardOrAccountNumber(accountNumberPlain)
        val encryptedToken = SecurityManager.encryptSensitiveField("${bank.bankId}|$accountNumberPlain|${user.mobileNumber}")
        val linkId = "LNK-${bank.bankId}-" + UUID.randomUUID().toString().uppercase().take(5)
        val linked = LinkedBankAccountEntity(
            linkId = linkId,
            userMobile = user.mobileNumber,
            bankId = bank.bankId,
            officialBankName = bank.officialNameEn,
            shortBankName = bank.shortName,
            bankCode = bank.bankCode,
            accountHolderName = verifiedHolderName,
            accountNumberMasked = masked,
            accountTokenEncrypted = encryptedToken,
            verificationProvider = "${bank.apiAdapterKey} (${bank.environmentMode})",
            environmentMode = bank.environmentMode,
            isPrimary = isPrimary
        )
        dao.insertLinkedBankAccount(linked)
        dao.insertNotification(
            NotificationEntity(
                userMobile = user.mobileNumber,
                titleEn = "Bank Account Linked • ${bank.shortName}",
                titleNe = "बैंक खाता लिंक भयो • ${bank.officialNameNe}",
                messageEn = "[${bank.environmentMode}] ${bank.officialNameEn} ($masked) linked securely via ${bank.apiAdapterKey}.",
                messageNe = "[${bank.environmentMode}] ${bank.officialNameNe} ($masked) सुरक्षित रूपमा नेपालपेमा लिंक गरियो।",
                category = "SECURITY_ALERT"
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = user.mobileNumber,
                actionType = "BANK_LINKED",
                details = "Linked ${bank.officialNameEn} ($masked) via connector ${bank.apiAdapterKey}",
                deviceMetadata = user.deviceName,
                severity = "INFO"
            )
        )
        return linked
    }

    suspend fun unlinkBankAccount(userMobile: String, linkId: String) {
        dao.deleteLinkedBankAccount(linkId)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = userMobile,
                actionType = "BANK_UNLINKED",
                details = "Removed linked bank account $linkId",
                deviceMetadata = "Android 15",
                severity = "INFO"
            )
        )
    }

    suspend fun ensureSeedData() {
        if (dao.getBankCount() == 0) {
            dao.insertBanks(gateway.buildInitialNepalBanksSeed())
        }
        if (dao.getUserCount() > 0) return

        val now = System.currentTimeMillis()
        val salt1 = SecurityManager.generateSalt()
        val salt2 = SecurityManager.generateSalt()
        val salt3 = SecurityManager.generateSalt()

        val primaryUser = UserEntity(
            mobileNumber = "+9779841234567",
            fullName = "Aarav Sharma",
            fullNameNe = "आरव शर्मा",
            email = "aarav.sharma@nepalpay.com.np",
            paymentId = "aarav@nepalpay",
            walletBalanceNpr = 48750.00,
            mpinHash = SecurityManager.hashSecret("1234", salt1),
            mpinSalt = salt1,
            mpinLength = 4,
            biometricEnabled = true,
            kycStatus = "VERIFIED",
            isSuspended = false,
            role = "ADMIN",
            deviceName = "Pixel 9 Pro • Kathmandu, Bagmati",
            sessionToken = SecurityManager.generateSessionToken(),
            createdAt = now - 86_400_000L * 30,
            lastActiveAt = now
        )

        val userSita = UserEntity(
            mobileNumber = "+9779801122334",
            fullName = "Sita Gurung",
            fullNameNe = "सीता गुरुङ",
            email = "sita.gurung@nepalpay.com.np",
            paymentId = "sita@nepalpay",
            walletBalanceNpr = 32400.00,
            mpinHash = SecurityManager.hashSecret("1234", salt2),
            mpinSalt = salt2,
            mpinLength = 4,
            biometricEnabled = true,
            kycStatus = "VERIFIED",
            isSuspended = false,
            role = "USER",
            deviceName = "Samsung S24 • Pokhara, Gandaki",
            sessionToken = SecurityManager.generateSessionToken(),
            createdAt = now - 86_400_000L * 20,
            lastActiveAt = now - 3_600_000L
        )

        val userRamesh = UserEntity(
            mobileNumber = "+9779812345678",
            fullName = "Ramesh Thapa",
            fullNameNe = "रमेश थापा",
            email = "ramesh.thapa@nepalpay.com.np",
            paymentId = "ramesh@nepalpay",
            walletBalanceNpr = 15200.00,
            mpinHash = SecurityManager.hashSecret("1234", salt3),
            mpinSalt = salt3,
            mpinLength = 4,
            biometricEnabled = false,
            kycStatus = "PENDING",
            isSuspended = false,
            role = "USER",
            deviceName = "Redmi Note 13 • Lalitpur, Bagmati",
            sessionToken = SecurityManager.generateSessionToken(),
            createdAt = now - 86_400_000L * 7,
            lastActiveAt = now - 7_200_000L
        )

        val userBikash = UserEntity(
            mobileNumber = "+9779860998877",
            fullName = "Bikash Shrestha",
            fullNameNe = "बिकास श्रेष्ठ",
            email = "bikash@nepalpay.com.np",
            paymentId = "bikash@nepalpay",
            walletBalanceNpr = 4100.00,
            mpinHash = SecurityManager.hashSecret("1234", salt3),
            mpinSalt = salt3,
            mpinLength = 4,
            biometricEnabled = true,
            kycStatus = "REJECTED",
            isSuspended = true,
            role = "USER",
            deviceName = "OnePlus 12 • Biratnagar, Koshi",
            sessionToken = "",
            createdAt = now - 86_400_000L * 14,
            lastActiveAt = now - 86_400_000L * 2
        )

        dao.insertUsers(listOf(primaryUser, userSita, userRamesh, userBikash))

        // Seed Linked Bank Accounts for Primary User
        dao.insertLinkedBankAccounts(
            listOf(
                LinkedBankAccountEntity(
                    linkId = "LNK-NABIL-8821",
                    userMobile = primaryUser.mobileNumber,
                    bankId = "NABIL",
                    officialBankName = "Nabil Bank Limited",
                    shortBankName = "Nabil Bank",
                    bankCode = "NARBNPKA",
                    accountHolderName = "Aarav Sharma",
                    accountNumberMasked = "**** **** 4821",
                    accountTokenEncrypted = SecurityManager.encryptSensitiveField("NABIL|012004821|Aarav"),
                    verificationProvider = "CONNECTOR_NABIL_NAPI (SANDBOX)",
                    environmentMode = "SANDBOX",
                    isPrimary = true,
                    linkedAt = now - 86_400_000L * 15
                ),
                LinkedBankAccountEntity(
                    linkId = "LNK-GIME-9012",
                    userMobile = primaryUser.mobileNumber,
                    bankId = "GIME",
                    officialBankName = "Global IME Bank Limited",
                    shortBankName = "Global IME",
                    bankCode = "GLBBNPKA",
                    accountHolderName = "Aarav Sharma",
                    accountNumberMasked = "**** **** 9012",
                    accountTokenEncrypted = SecurityManager.encryptSensitiveField("GIME|009129012|Aarav"),
                    verificationProvider = "CONNECTOR_GIME_GLOBALSMART (SANDBOX)",
                    environmentMode = "SANDBOX",
                    isPrimary = false,
                    linkedAt = now - 86_400_000L * 8
                )
            )
        )

        // Seed Reconciliation & API Error Logs for Admin Dashboard
        dao.insertReconciliationLogs(
            listOf(
                ReconciliationLogEntity(
                    reconId = "RECON-2026-101",
                    bankId = "NABIL",
                    bankName = "Nabil Bank Limited",
                    totalTxCount = 42,
                    matchedTxCount = 42,
                    discrepancyCount = 0,
                    totalVolumeNpr = 415800.0,
                    status = "BALANCED",
                    notes = "ISO20022 EOD Batch matched 100% with NCHL connectIPS Sandbox",
                    timestamp = now - 3_600_000L * 3
                ),
                ReconciliationLogEntity(
                    reconId = "RECON-2026-102",
                    bankId = "GIME",
                    bankName = "Global IME Bank Limited",
                    totalTxCount = 29,
                    matchedTxCount = 28,
                    discrepancyCount = 1,
                    totalVolumeNpr = 289500.0,
                    status = "PENDING_SETTLEMENT",
                    notes = "1 pending transaction (NP-SBX-907540 • रु. 5,010) awaiting T+1 settlement",
                    timestamp = now - 3_600_000L * 2
                ),
                ReconciliationLogEntity(
                    reconId = "RECON-2026-103",
                    bankId = "NICA",
                    bankName = "NIC Asia Bank Limited",
                    totalTxCount = 18,
                    matchedTxCount = 17,
                    discrepancyCount = 1,
                    totalVolumeNpr = 164200.0,
                    status = "DISCREPANCY_REVIEW",
                    notes = "Tx NP-SBX-907105 auto-reversed after sandbox switch timeout",
                    timestamp = now - 3_600_000L
                )
            )
        )

        dao.insertApiErrorLogs(
            listOf(
                ApiErrorLogEntity(
                    bankId = "NICA",
                    bankName = "NIC Asia Bank Limited",
                    endpoint = "https://sandbox.api.nicasiabank.com/v2/payments",
                    httpStatusCode = 504,
                    errorCode = "SWITCH_GATEWAY_TIMEOUT",
                    errorMessage = "[SANDBOX] Upstream switch timeout on Tx NP-SBX-907105; auto-refunded to wallet",
                    environmentMode = "SANDBOX",
                    isResolved = false,
                    timestamp = now - 86_400_000L * 3
                ),
                ApiErrorLogEntity(
                    bankId = "CCBL_SUCC",
                    bankName = "Century Commercial Bank (Successor: Prabhu Bank)",
                    endpoint = "https://sandbox.api.prabhubank.com/v1/legacy-ccbl",
                    httpStatusCode = 301,
                    errorCode = "MERGER_REDIRECT_NOTICE",
                    errorMessage = "Legacy Century Commercial routing code automatically mapped to PRVUNPKA (Prabhu Bank)",
                    environmentMode = "SANDBOX",
                    isResolved = true,
                    timestamp = now - 86_400_000L * 5
                )
            )
        )

        // Seed KYC submissions
        dao.insertKycSubmission(
            KycSubmissionEntity(
                mobileNumber = primaryUser.mobileNumber,
                fullName = primaryUser.fullName,
                dateOfBirth = "2053-04-12 BS (1996-07-27 AD)",
                province = "Bagmati Province (बागमती प्रदेश)",
                district = "Kathmandu (काठमाडौँ)",
                municipality = "Kathmandu Metropolitan City",
                wardNo = "10 (Baneshwor)",
                documentType = "CITIZENSHIP",
                documentNumberEncrypted = SecurityManager.encryptSensitiveField("27-01-74-09482"),
                documentNumberMasked = "27-****-82",
                documentUploadedName = "citizenship_front_aarav.jpg",
                selfieVerified = true,
                status = "VERIFIED",
                reviewerNote = "Verified against National Identity Sandbox Registry",
                submittedAt = now - 86_400_000L * 25
            )
        )

        dao.insertKycSubmission(
            KycSubmissionEntity(
                mobileNumber = userRamesh.mobileNumber,
                fullName = userRamesh.fullName,
                dateOfBirth = "2056-11-08 BS (2000-02-20 AD)",
                province = "Bagmati Province (बागमती प्रदेश)",
                district = "Lalitpur (ललितपुर)",
                municipality = "Lalitpur Metropolitan City",
                wardNo = "04 (Jawalakhel)",
                documentType = "NATIONAL_ID",
                documentNumberEncrypted = SecurityManager.encryptSensitiveField("421-908-7731"),
                documentNumberMasked = "42-****-31",
                documentUploadedName = "nid_card_ramesh.jpg",
                selfieVerified = true,
                status = "PENDING",
                reviewerNote = "Awaiting Compliance Officer review",
                submittedAt = now - 3_600_000L * 5
            )
        )

        // Seed Transactions
        val seededTransactions = listOf(
            TransactionEntity(
                txId = "NP-SBX-908241",
                senderMobile = "+9779801122334",
                senderName = "Sita Gurung",
                receiverIdentifier = "+9779841234567",
                receiverName = "Aarav Sharma",
                amountNpr = 3500.0,
                feeNpr = 0.0,
                totalAmountNpr = 3500.0,
                note = "Pokhara trip hotel share / पोखरा भ्रमण खर्च",
                txType = "SEND_MONEY",
                paymentMethod = "NEPALPAY_WALLET",
                status = "SUCCESSFUL",
                isSandbox = true,
                timestamp = now - 3_600_000L * 2
            ),
            TransactionEntity(
                txId = "NP-SBX-908198",
                senderMobile = "+9779841234567",
                senderName = "Aarav Sharma",
                receiverIdentifier = "himalayan.java@nepalpay",
                receiverName = "Himalayan Java Coffee - Durbar Marg",
                amountNpr = 680.0,
                feeNpr = 0.0,
                totalAmountNpr = 680.0,
                note = "Iced Americano & Croissant (QR Pay)",
                txType = "QR_PAY",
                paymentMethod = "NEPALQR_STANDARD",
                status = "SUCCESSFUL",
                isSandbox = true,
                timestamp = now - 3_600_000L * 6
            ),
            TransactionEntity(
                txId = "NP-SBX-907812",
                senderMobile = "Nabil Bank Sandbox (****4821)",
                senderName = "Nabil Bank Limited",
                receiverIdentifier = "+9779841234567",
                receiverName = "Aarav Sharma",
                amountNpr = 15000.0,
                feeNpr = 0.0,
                totalAmountNpr = 15000.0,
                note = "Wallet Top-Up via Nabil Bank Connector",
                txType = "ADD_MONEY",
                paymentMethod = "BANK_DIRECT",
                bankName = "Nabil Bank Limited",
                status = "SUCCESSFUL",
                isSandbox = true,
                timestamp = now - 86_400_000L
            ),
            TransactionEntity(
                txId = "NP-SBX-907540",
                senderMobile = "+9779841234567",
                senderName = "Aarav Sharma",
                receiverIdentifier = "GLBBNPKA • ****9012",
                receiverName = "Global IME Bank Limited (Self A/C)",
                amountNpr = 5000.0,
                feeNpr = 10.0,
                totalAmountNpr = 5010.0,
                note = "Monthly savings transfer (connectIPS Sandbox)",
                txType = "BANK_TRANSFER",
                paymentMethod = "CONNECT_IPS_SANDBOX",
                bankName = "Global IME Bank Limited",
                status = "PENDING",
                isSandbox = true,
                timestamp = now - 86_400_000L * 2
            ),
            TransactionEntity(
                txId = "NP-SBX-907105",
                senderMobile = "+9779841234567",
                senderName = "Aarav Sharma",
                receiverIdentifier = "NICENPKA • ****3319",
                receiverName = "NIC Asia Bank Limited",
                amountNpr = 2200.0,
                feeNpr = 10.0,
                totalAmountNpr = 2210.0,
                note = "Sandbox switch timeout simulation",
                txType = "WITHDRAW_BANK",
                paymentMethod = "CONNECT_IPS_SANDBOX",
                bankName = "NIC Asia Bank Limited",
                status = "FAILED",
                isSandbox = true,
                timestamp = now - 86_400_000L * 3
            ),
            TransactionEntity(
                txId = "NP-SBX-906990",
                senderMobile = "+9779812345678",
                senderName = "Ramesh Thapa",
                receiverIdentifier = "+9779801122334",
                receiverName = "Sita Gurung",
                amountNpr = 65000.0,
                feeNpr = 15.0,
                totalAmountNpr = 65015.0,
                note = "Laptop purchase advance (High-value AML alert)",
                txType = "SEND_MONEY",
                paymentMethod = "NEPALPAY_WALLET",
                status = "PENDING",
                isSandbox = true,
                isFlaggedSuspicious = true,
                fraudReason = "High-value transaction (>= NPR 50,000) flagged under NRB AML threshold",
                timestamp = now - 3_600_000L * 4
            )
        )
        dao.insertTransactions(seededTransactions)

        // Seed Money Requests
        dao.insertMoneyRequests(
            listOf(
                MoneyRequestEntity(
                    requestId = "REQ-SBX-301",
                    requesterMobile = "+9779801122334",
                    requesterName = "Sita Gurung",
                    requesterPaymentId = "sita@nepalpay",
                    payerMobile = "+9779841234567",
                    payerName = "Aarav Sharma",
                    amountNpr = 1250.0,
                    note = "Thamel dinner split / ठमेल डिनर बिल",
                    status = "PENDING",
                    timestamp = now - 1_800_000L
                )
            )
        )

        // Seed Notifications
        dao.insertNotifications(
            listOf(
                NotificationEntity(
                    userMobile = primaryUser.mobileNumber,
                    titleEn = "Payment Received • रु. 3,500.00",
                    titleNe = "रकम प्राप्त भयो • रु. 3,500.00",
                    messageEn = "[SANDBOX] Sita Gurung (+9779801122334) sent you NPR रु. 3,500.00.",
                    messageNe = "[स्यान्डबक्स] सीता गुरुङबाट तपाईंको वालेटमा रु. 3,500.00 प्राप्त भयो।",
                    category = "PAYMENT_RECEIVED",
                    relatedTxId = "NP-SBX-908241",
                    timestamp = now - 3_600_000L * 2
                ),
                NotificationEntity(
                    userMobile = primaryUser.mobileNumber,
                    titleEn = "22 Nepal Licensed Banks Loaded in Directory",
                    titleNe = "२२ इजाजतपत्र प्राप्त बैंकहरूको डाइरेक्टरी अपडेट",
                    messageEn = "Dynamic Nepal Banks Directory active with official HTTPS links & per-bank API connectors.",
                    messageNe = "आधिकारिक वेबसाइट र एपीआई कनेक्टर सहित नेपाल बैंक डाइरेक्टरी तयार छ।",
                    category = "SECURITY_ALERT",
                    timestamp = now - 900_000L
                )
            )
        )

        // Seed Audit Logs
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = primaryUser.mobileNumber,
                actionType = "SYSTEM_INIT",
                details = "NepalPay NRB Sandbox initialized with 22 editable Bank Directory records & HMAC Webhook verifier",
                deviceMetadata = "Pixel 9 Pro • Android 15",
                severity = "INFO",
                timestamp = now - 86_400_000L
            )
        )
    }

    suspend fun getUserByMobile(mobile: String): UserEntity? =
        dao.getUserByMobile(SecurityManager.normalizeNepalMobile(mobile))

    suspend fun findReceiver(identifier: String): Pair<String, String> {
        val trimmed = identifier.trim()
        if (trimmed.contains("@")) {
            val byId = dao.getUserByPaymentId(trimmed)
            if (byId != null) return byId.mobileNumber to byId.fullName
            val merchant = sandboxMerchants.find { it.paymentId.equals(trimmed, ignoreCase = true) }
            if (merchant != null) return merchant.paymentId to merchant.merchantNameEn
            val fallbackName = trimmed.substringBefore("@").replace(".", " ")
                .replaceFirstChar { it.uppercase() }
            return trimmed to "$fallbackName (NepalPay)"
        }
        val normalized = SecurityManager.normalizeNepalMobile(trimmed)
        val user = dao.getUserByMobile(normalized)
        return if (user != null) {
            user.mobileNumber to user.fullName
        } else {
            normalized to "NepalPay User (${normalized.takeLast(4)})"
        }
    }

    suspend fun registerOrResetUser(
        mobileRaw: String,
        fullName: String,
        email: String,
        plainMpin: String,
        biometricEnabled: Boolean
    ): UserEntity {
        val mobile = SecurityManager.normalizeNepalMobile(mobileRaw)
        val existing = dao.getUserByMobile(mobile)
        val salt = SecurityManager.generateSalt()
        val mpinHash = SecurityManager.hashSecret(plainMpin, salt)
        val cleanHandle = fullName.trim().lowercase().replace(Regex("[^a-z0-9]"), ".")
            .trim('.').ifEmpty { "user" + mobile.takeLast(4) }

        val user = if (existing != null) {
            existing.copy(
                fullName = fullName.ifBlank { existing.fullName },
                email = email.ifBlank { existing.email },
                mpinHash = mpinHash,
                mpinSalt = salt,
                mpinLength = plainMpin.length,
                biometricEnabled = biometricEnabled,
                sessionToken = SecurityManager.generateSessionToken(),
                lastActiveAt = System.currentTimeMillis()
            )
        } else {
            UserEntity(
                mobileNumber = mobile,
                fullName = fullName.ifBlank { "NepalPay User" },
                fullNameNe = fullName.ifBlank { "नेपालपे प्रयोगकर्ता" },
                email = email.ifBlank { "$cleanHandle@nepalpay.com.np" },
                paymentId = "$cleanHandle@nepalpay",
                walletBalanceNpr = 10000.00,
                mpinHash = mpinHash,
                mpinSalt = salt,
                mpinLength = plainMpin.length,
                biometricEnabled = biometricEnabled,
                kycStatus = "UNSUBMITTED",
                isSuspended = false,
                role = "USER",
                sessionToken = SecurityManager.generateSessionToken()
            )
        }
        dao.insertUser(user)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = mobile,
                actionType = if (existing == null) "USER_REGISTERED" else "MPIN_RECOVERY",
                details = "Account configured with ${plainMpin.length}-digit salted SHA-256 MPIN",
                deviceMetadata = user.deviceName,
                severity = "INFO"
            )
        )
        return user
    }

    suspend fun executePayment(
        sender: UserEntity,
        receiverIdentifier: String,
        receiverName: String,
        amountNpr: Double,
        feeNpr: Double,
        note: String,
        txType: String,
        paymentMethod: String,
        bankName: String = "",
        bankId: String = "",
        simulateStatus: String = "SUCCESSFUL"
    ): Result<TransactionEntity> {
        if (sender.isSuspended) {
            return Result.failure(IllegalStateException("Account is currently suspended by Admin / Compliance."))
        }
        val totalDeduction = amountNpr + feeNpr
        if (txType != "ADD_MONEY" && sender.walletBalanceNpr < totalDeduction) {
            return Result.failure(IllegalStateException("Insufficient NPR wallet balance."))
        }

        val fraudEval = SecurityManager.evaluateTransactionFraud(
            amountNpr = amountNpr,
            kycStatus = sender.kycStatus,
            recentTxCountLastMinute = 1
        )

        var finalStatus = when {
            simulateStatus == "FAILED" -> "FAILED"
            simulateStatus == "PENDING" || fraudEval.requiresPendingReview -> "PENDING"
            else -> "SUCCESSFUL"
        }

        var gatewayResult: GatewayResponse? = null
        if (bankName.isNotBlank() || bankId.isNotBlank()) {
            val bankEntity = if (bankId.isNotBlank()) dao.getBankById(bankId) else null
            if (bankEntity != null) {
                gatewayResult = gateway.executeBankTransferViaConnector(
                    bank = bankEntity,
                    accountNumberMasked = receiverIdentifier,
                    accountHolderName = receiverName,
                    amountNpr = amountNpr,
                    simulateOutcome = finalStatus
                )
                finalStatus = gatewayResult.status
                if (finalStatus == "FAILED") {
                    dao.insertApiErrorLog(
                        ApiErrorLogEntity(
                            bankId = bankEntity.bankId,
                            bankName = bankEntity.officialNameEn,
                            endpoint = bankEntity.apiEndpointUrl,
                            httpStatusCode = if (bankEntity.integrationEnabled) 502 else 403,
                            errorCode = if (bankEntity.integrationEnabled) "CONNECTOR_DECLINED" else "INTEGRATION_DISABLED",
                            errorMessage = gatewayResult.gatewayMessageEn,
                            environmentMode = bankEntity.environmentMode
                        )
                    )
                }
            }
        }

        val txId = gatewayResult?.referenceId
            ?: ("NP-SBX-" + UUID.randomUUID().toString().uppercase().take(6))

        val tx = TransactionEntity(
            txId = txId,
            senderMobile = if (txType == "ADD_MONEY") bankName.ifBlank { "Nepal Bank Sandbox" } else sender.mobileNumber,
            senderName = if (txType == "ADD_MONEY") bankName.ifBlank { "Linked Bank Sandbox" } else sender.fullName,
            receiverIdentifier = if (txType == "ADD_MONEY") sender.mobileNumber else receiverIdentifier,
            receiverName = if (txType == "ADD_MONEY") sender.fullName else receiverName,
            amountNpr = amountNpr,
            feeNpr = feeNpr,
            totalAmountNpr = totalDeduction,
            note = note.ifBlank { "NepalPay Sandbox Transfer" },
            txType = txType,
            paymentMethod = paymentMethod,
            bankName = bankName,
            status = finalStatus,
            isSandbox = true,
            isFlaggedSuspicious = fraudEval.isSuspicious,
            fraudReason = fraudEval.reason
        )

        dao.insertTransaction(tx)

        if (finalStatus == "SUCCESSFUL") {
            if (txType == "ADD_MONEY") {
                dao.updateWalletBalance(sender.mobileNumber, sender.walletBalanceNpr + amountNpr)
            } else {
                dao.updateWalletBalance(sender.mobileNumber, sender.walletBalanceNpr - totalDeduction)
                val receiverUser = dao.getUserByMobile(SecurityManager.normalizeNepalMobile(receiverIdentifier))
                    ?: dao.getUserByPaymentId(receiverIdentifier)
                if (receiverUser != null && receiverUser.mobileNumber != sender.mobileNumber) {
                    dao.updateWalletBalance(
                        receiverUser.mobileNumber,
                        receiverUser.walletBalanceNpr + amountNpr
                    )
                    dao.insertNotification(
                        NotificationEntity(
                            userMobile = receiverUser.mobileNumber,
                            titleEn = "Payment Received • रु. ${amountNpr.toInt()}",
                            titleNe = "रकम प्राप्त भयो • रु. ${amountNpr.toInt()}",
                            messageEn = "[SANDBOX] Received NPR रु. $amountNpr from ${sender.fullName} ($txId).",
                            messageNe = "[स्यान्डबक्स] ${sender.fullName} बाट रु. $amountNpr प्राप्त भयो ($txId)।",
                            category = "PAYMENT_RECEIVED",
                            relatedTxId = txId
                        )
                    )
                }
            }
        }

        val notifCategory = when {
            finalStatus == "FAILED" -> "PAYMENT_FAILED"
            txType == "ADD_MONEY" -> "TOP_UP"
            txType == "WITHDRAW_BANK" || txType == "BANK_TRANSFER" -> "WITHDRAWAL"
            else -> "PAYMENT_SENT"
        }
        val notifTitleEn = when (finalStatus) {
            "FAILED" -> "Sandbox Payment Failed • रु. ${amountNpr.toInt()}"
            "PENDING" -> "Sandbox Payment Pending • रु. ${amountNpr.toInt()}"
            else -> if (txType == "ADD_MONEY") "Wallet Loaded • रु. ${amountNpr.toInt()}"
            else "Payment Sent • रु. ${amountNpr.toInt()}"
        }
        val notifTitleNe = when (finalStatus) {
            "FAILED" -> "कारोबार असफल भयो • रु. ${amountNpr.toInt()}"
            "PENDING" -> "कारोबार पेन्डिङ छ • रु. ${amountNpr.toInt()}"
            else -> if (txType == "ADD_MONEY") "वालेटमा रकम जम्मा भयो • रु. ${amountNpr.toInt()}"
            else "रकम पठाइयो • रु. ${amountNpr.toInt()}"
        }
        val notifBodyEn = "[SANDBOX DEMO] $txType of NPR रु. $totalDeduction to $receiverName ($txId) is $finalStatus."
        val notifBodyNe = "[स्यान्डबक्स डेमो] $receiverName लाई रु. $totalDeduction को कारोबार ($txId) $finalStatus।"

        dao.insertNotification(
            NotificationEntity(
                userMobile = sender.mobileNumber,
                titleEn = notifTitleEn,
                titleNe = notifTitleNe,
                messageEn = notifBodyEn,
                messageNe = notifBodyNe,
                category = notifCategory,
                relatedTxId = txId
            )
        )

        appContext?.let {
            SystemNotificationDispatcher.dispatchPush(it, notifTitleEn, notifBodyEn)
        }

        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = sender.mobileNumber,
                actionType = if (fraudEval.isSuspicious) "TX_FLAGGED" else "TX_EXECUTED",
                details = "[$finalStatus] $txId • NPR $totalDeduction to $receiverName (${fraudEval.reason.ifBlank { "Standard Sandbox Check Passed" }})",
                deviceMetadata = sender.deviceName,
                severity = if (fraudEval.isSuspicious) "WARNING" else "INFO"
            )
        )

        return Result.success(tx)
    }

    suspend fun createMoneyRequest(
        requester: UserEntity,
        payerMobileRaw: String,
        amountNpr: Double,
        note: String
    ): MoneyRequestEntity {
        val payerMobile = SecurityManager.normalizeNepalMobile(payerMobileRaw)
        val payerUser = dao.getUserByMobile(payerMobile)
        val reqId = "REQ-SBX-" + UUID.randomUUID().toString().uppercase().take(5)
        val entity = MoneyRequestEntity(
            requestId = reqId,
            requesterMobile = requester.mobileNumber,
            requesterName = requester.fullName,
            requesterPaymentId = requester.paymentId,
            payerMobile = payerMobile,
            payerName = payerUser?.fullName ?: "NepalPay User ($payerMobile)",
            amountNpr = amountNpr,
            note = note.ifBlank { "Money Request via NepalPay" },
            status = "PENDING"
        )
        dao.insertMoneyRequest(entity)
        dao.insertNotification(
            NotificationEntity(
                userMobile = requester.mobileNumber,
                titleEn = "Money Request Sent • रु. ${amountNpr.toInt()}",
                titleNe = "रकम अनुरोध पठाइयो • रु. ${amountNpr.toInt()}",
                messageEn = "[SANDBOX] Requested NPR रु. $amountNpr from ${entity.payerName}.",
                messageNe = "[स्यान्डबक्स] ${entity.payerName} सँग रु. $amountNpr अनुरोध गरियो।",
                category = "MONEY_REQUEST",
                relatedTxId = reqId
            )
        )
        if (payerUser != null) {
            dao.insertNotification(
                NotificationEntity(
                    userMobile = payerUser.mobileNumber,
                    titleEn = "Money Request from ${requester.fullName}",
                    titleNe = "${requester.fullName} बाट रकम अनुरोध",
                    messageEn = "${requester.fullName} requested NPR रु. $amountNpr ($note).",
                    messageNe = "${requester.fullName} ले रु. $amountNpr अनुरोध गर्नुभएको छ।",
                    category = "MONEY_REQUEST",
                    relatedTxId = reqId
                )
            )
        }
        return entity
    }

    suspend fun respondToMoneyRequest(
        payer: UserEntity,
        request: MoneyRequestEntity,
        approve: Boolean
    ): Result<TransactionEntity?> {
        if (!approve) {
            dao.updateMoneyRequestStatus(request.requestId, "DECLINED")
            return Result.success(null)
        }
        val txResult = executePayment(
            sender = payer,
            receiverIdentifier = request.requesterMobile,
            receiverName = request.requesterName,
            amountNpr = request.amountNpr,
            feeNpr = 0.0,
            note = "Paid Request: ${request.note}",
            txType = "REQUEST_PAY",
            paymentMethod = "NEPALPAY_WALLET"
        )
        if (txResult.isSuccess) {
            dao.updateMoneyRequestStatus(request.requestId, "PAID")
        }
        return txResult.map { it }
    }

    suspend fun submitKyc(
        user: UserEntity,
        fullName: String,
        dob: String,
        province: String,
        district: String,
        municipality: String,
        wardNo: String,
        docType: String,
        docNumberPlain: String,
        docFileName: String,
        selfieVerified: Boolean
    ) {
        val encryptedDoc = SecurityManager.encryptSensitiveField(docNumberPlain)
        val maskedDoc = SecurityManager.maskDocumentNumber(docNumberPlain)
        val submission = KycSubmissionEntity(
            mobileNumber = user.mobileNumber,
            fullName = fullName,
            dateOfBirth = dob,
            province = province,
            district = district,
            municipality = municipality,
            wardNo = wardNo,
            documentType = docType,
            documentNumberEncrypted = encryptedDoc,
            documentNumberMasked = maskedDoc,
            documentUploadedName = docFileName,
            selfieVerified = selfieVerified,
            status = "PENDING",
            reviewerNote = "Submitted for NRB Compliance KYC verification"
        )
        dao.insertKycSubmission(submission)
        dao.updateUserKycStatus(user.mobileNumber, "PENDING")
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = user.mobileNumber,
                actionType = "KYC_SUBMIT",
                details = "KYC submitted ($docType: $maskedDoc) encrypted with AES-256-GCM",
                deviceMetadata = user.deviceName,
                severity = "INFO"
            )
        )
    }

    suspend fun changeMpin(user: UserEntity, newMpin: String) {
        val newSalt = SecurityManager.generateSalt()
        val newHash = SecurityManager.hashSecret(newMpin, newSalt)
        dao.updateUser(
            user.copy(
                mpinHash = newHash,
                mpinSalt = newSalt,
                mpinLength = newMpin.length
            )
        )
        dao.insertNotification(
            NotificationEntity(
                userMobile = user.mobileNumber,
                titleEn = "Security Alert: MPIN Updated",
                titleNe = "सुरक्षा सूचना: MPIN परिवर्तन भयो",
                messageEn = "Your NepalPay MPIN was securely updated using SHA-256 salted hashing.",
                messageNe = "तपाईंको नेपालपे MPIN सुरक्षित रूपमा परिवर्तन गरिएको छ।",
                category = "SECURITY_ALERT"
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = user.mobileNumber,
                actionType = "MPIN_CHANGED",
                details = "User updated ${newMpin.length}-digit MPIN hash",
                deviceMetadata = user.deviceName,
                severity = "INFO"
            )
        )
    }

    suspend fun toggleBiometric(user: UserEntity, enabled: Boolean) {
        dao.updateUser(user.copy(biometricEnabled = enabled))
    }

    suspend fun markNotificationsRead(mobile: String) {
        dao.markAllNotificationsRead(mobile)
    }

    // Admin Operations for Dynamic Bank Directory, Reconciliation & Error Logs
    suspend fun adminSaveOrUpdateBank(bank: BankDirectoryEntity) {
        dao.insertBank(bank.copy(lastVerifiedWithNrbAt = System.currentTimeMillis()))
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = "ADMIN",
                actionType = "ADMIN_BANK_UPDATE",
                details = "Updated Bank Directory entry ${bank.bankId} (${bank.officialNameEn}) • Website: ${bank.officialWebsiteUrl} • Mode: ${bank.environmentMode}",
                deviceMetadata = "Admin Console",
                severity = "INFO"
            )
        )
    }

    suspend fun adminToggleBankIntegration(bank: BankDirectoryEntity, enabled: Boolean) {
        val newStatus = if (enabled) "CONNECTED_SANDBOX" else "DISABLED"
        dao.setBankIntegrationEnabled(bank.bankId, enabled, newStatus)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = "ADMIN",
                actionType = "ADMIN_BANK_TOGGLE",
                details = "${if (enabled) "Enabled" else "Disabled"} API connector ${bank.apiAdapterKey} for ${bank.officialNameEn}",
                deviceMetadata = "Admin Console",
                severity = if (enabled) "INFO" else "WARNING"
            )
        )
    }

    suspend fun adminVerifyAllBanksWithNrb() {
        dao.markAllBanksVerifiedWithNrb(System.currentTimeMillis())
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = "ADMIN",
                actionType = "NRB_DIRECTORY_SYNC",
                details = "Verified all licensed banks & merger mappings against Nepal Rastra Bank (NRB) registry",
                deviceMetadata = "Admin Console",
                severity = "INFO"
            )
        )
    }

    suspend fun adminRunReconciliation(bank: BankDirectoryEntity) {
        val reconId = "RECON-" + UUID.randomUUID().toString().uppercase().take(6)
        val log = ReconciliationLogEntity(
            reconId = reconId,
            bankId = bank.bankId,
            bankName = bank.officialNameEn,
            totalTxCount = 15,
            matchedTxCount = 15,
            discrepancyCount = 0,
            totalVolumeNpr = 185000.0,
            status = "BALANCED",
            notes = "Automated reconciliation completed via ${bank.apiAdapterKey} (${bank.environmentMode})"
        )
        dao.insertReconciliationLog(log)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = "ADMIN",
                actionType = "ADMIN_RECONCILIATION",
                details = "Completed ledger vs switch reconciliation $reconId for ${bank.officialNameEn}",
                deviceMetadata = "Admin Console",
                severity = "INFO"
            )
        )
    }

    suspend fun adminResolveApiError(errorId: Long) {
        dao.markApiErrorResolved(errorId)
    }

    suspend fun adminToggleUserSuspension(mobile: String, suspend: Boolean) {
        dao.setAccountSuspended(mobile, suspend)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = mobile,
                actionType = "ADMIN_ACTION",
                details = if (suspend) "Account suspended by Compliance Admin" else "Account reactivated by Compliance Admin",
                deviceMetadata = "Admin Console",
                severity = if (suspend) "CRITICAL" else "INFO"
            )
        )
    }

    suspend fun adminReviewKyc(mobile: String, newStatus: String, reviewNote: String) {
        dao.updateKycSubmissionStatus(mobile, newStatus, reviewNote)
        dao.updateUserKycStatus(mobile, newStatus)
        dao.insertNotification(
            NotificationEntity(
                userMobile = mobile,
                titleEn = "KYC Status Updated: $newStatus",
                titleNe = "KYC स्थिति अपडेट: $newStatus",
                messageEn = "Your NepalPay KYC verification is now $newStatus. Note: $reviewNote",
                messageNe = "तपाईंको KYC प्रमाणीकरण स्थिति: $newStatus। कैफियत: $reviewNote",
                category = "SECURITY_ALERT"
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = mobile,
                actionType = "ADMIN_KYC_DECISION",
                details = "Admin updated KYC for $mobile to $newStatus ($reviewNote)",
                deviceMetadata = "Admin Console",
                severity = "INFO"
            )
        )
    }

    suspend fun adminUpdateTransactionStatus(txId: String, newStatus: String) {
        dao.updateTransactionStatus(txId, newStatus)
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = "ADMIN",
                actionType = "ADMIN_TX_REVIEW",
                details = "Transaction $txId status manually updated to $newStatus",
                deviceMetadata = "Admin Console",
                severity = "WARNING"
            )
        )
    }

    suspend fun logSecurityEvent(mobile: String, action: String, details: String, severity: String = "WARNING") {
        dao.insertAuditLog(
            AuditLogEntity(
                userMobile = mobile,
                actionType = action,
                details = details,
                deviceMetadata = "Android 15 • Security Monitor",
                severity = severity
            )
        )
    }
}
