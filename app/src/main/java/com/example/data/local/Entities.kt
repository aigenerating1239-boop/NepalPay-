package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val mobileNumber: String, // e.g., "+9779841234567"
    val fullName: String,
    val fullNameNe: String,
    val email: String,
    val paymentId: String, // e.g., "aarav@nepalpay"
    val walletBalanceNpr: Double,
    val mpinHash: String, // SHA-256 salted hash, NEVER plain text
    val mpinSalt: String,
    val mpinLength: Int = 4,
    val biometricEnabled: Boolean = true,
    val kycStatus: String = "VERIFIED", // UNSUBMITTED, PENDING, VERIFIED, REJECTED
    val isSuspended: Boolean = false,
    val role: String = "USER", // USER, MERCHANT, ADMIN
    val deviceName: String = "Android 15 • Kathmandu, NP",
    val sessionToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

/**
 * Editable Room Database entity for Nepal's Licensed Banks Directory.
 * Supports bank mergers, acquisitions, official website updates, per-bank API adapter endpoints,
 * Sandbox/Production environment switching, and Enable/Disable integration from Admin Panel.
 */
@Entity(tableName = "nepal_banks")
data class BankDirectoryEntity(
    @PrimaryKey val bankId: String, // e.g., "NBL", "RBB", "NABIL"
    val displayOrder: Int,
    val officialNameEn: String,
    val officialNameNe: String,
    val shortName: String,
    val bankCode: String, // SWIFT / NCHL routing code
    val category: String, // COMMERCIAL_STATE, COMMERCIAL_PRIVATE, MERGED_SUCCESSOR
    val mergerNoteEn: String = "",
    val mergerNoteNe: String = "",
    val successorBankId: String = "",
    val officialWebsiteUrl: String,
    val logoMonogram: String,
    val logoPrimaryHex: String,
    val logoSecondaryHex: String,
    val apiAdapterKey: String, // Connector identifier in BankApiConnectorRegistry
    val apiEndpointUrl: String, // HTTPS endpoint (credentials stored server-side only)
    val apiStatus: String, // CONNECTED_SANDBOX, AUTHORIZED_READY, MAINTENANCE, DISABLED
    val environmentMode: String, // SANDBOX, PRODUCTION_READY
    val integrationEnabled: Boolean = true,
    val transferFeeNpr: Double = 10.0,
    val lastVerifiedWithNrbAt: Long = System.currentTimeMillis()
)

/**
 * User's linked bank accounts stored with tokenized/masked account numbers.
 */
@Entity(tableName = "linked_bank_accounts")
data class LinkedBankAccountEntity(
    @PrimaryKey val linkId: String, // e.g., "LNK-NP-9012"
    val userMobile: String,
    val bankId: String,
    val officialBankName: String,
    val shortBankName: String,
    val bankCode: String,
    val accountHolderName: String,
    val accountNumberMasked: String, // Never plain text account number
    val accountTokenEncrypted: String, // AES-256 encrypted bank token
    val verificationProvider: String, // e.g. "NCHL connectIPS / Bank Adapter Sandbox"
    val environmentMode: String, // SANDBOX
    val isPrimary: Boolean = false,
    val linkedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reconciliation_logs")
data class ReconciliationLogEntity(
    @PrimaryKey val reconId: String,
    val bankId: String,
    val bankName: String,
    val totalTxCount: Int,
    val matchedTxCount: Int,
    val discrepancyCount: Int,
    val totalVolumeNpr: Double,
    val status: String, // BALANCED, DISCREPANCY_REVIEW, PENDING_SETTLEMENT
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_error_logs")
data class ApiErrorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankId: String,
    val bankName: String,
    val endpoint: String,
    val httpStatusCode: Int,
    val errorCode: String,
    val errorMessage: String,
    val environmentMode: String,
    val isResolved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val txId: String, // e.g., "NP-SBX-849201"
    val senderMobile: String,
    val senderName: String,
    val receiverIdentifier: String, // Mobile, Payment ID, Merchant QR, or Bank A/C
    val receiverName: String,
    val amountNpr: Double,
    val feeNpr: Double,
    val totalAmountNpr: Double,
    val note: String,
    val txType: String, // SEND_MONEY, QR_PAY, ADD_MONEY, WITHDRAW_BANK, BANK_TRANSFER, REQUEST_PAY
    val paymentMethod: String, // NEPALPAY_WALLET, NEPALQR_STANDARD, CONNECT_IPS_SANDBOX, BANK_DIRECT
    val bankName: String = "",
    val status: String, // SUCCESSFUL, PENDING, FAILED
    val isSandbox: Boolean = true,
    val isFlaggedSuspicious: Boolean = false,
    val fraudReason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "money_requests")
data class MoneyRequestEntity(
    @PrimaryKey val requestId: String,
    val requesterMobile: String,
    val requesterName: String,
    val requesterPaymentId: String,
    val payerMobile: String,
    val payerName: String,
    val amountNpr: Double,
    val note: String,
    val status: String, // PENDING, PAID, DECLINED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "kyc_submissions")
data class KycSubmissionEntity(
    @PrimaryKey val mobileNumber: String,
    val fullName: String,
    val dateOfBirth: String, // BS / AD e.g. "2054-05-19 BS (1997-09-04)"
    val province: String,
    val district: String,
    val municipality: String,
    val wardNo: String,
    val documentType: String, // CITIZENSHIP, NATIONAL_ID, PASSPORT
    val documentNumberEncrypted: String, // Encrypted via SecurityManager
    val documentNumberMasked: String, // e.g., "27-01-74-***89"
    val documentUploadedName: String,
    val selfieVerified: Boolean,
    val status: String, // PENDING, VERIFIED, REJECTED
    val reviewerNote: String = "",
    val submittedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMobile: String,
    val titleEn: String,
    val titleNe: String,
    val messageEn: String,
    val messageNe: String,
    val category: String, // PAYMENT_RECEIVED, PAYMENT_SENT, PAYMENT_FAILED, MONEY_REQUEST, TOP_UP, WITHDRAWAL, SECURITY_ALERT
    val isRead: Boolean = false,
    val relatedTxId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userMobile: String,
    val actionType: String, // LOGIN, MPIN_VERIFY, TX_INITIATED, TX_FLAGGED, KYC_SUBMIT, ADMIN_ACTION, RATE_LIMIT
    val details: String,
    val deviceMetadata: String,
    val severity: String, // INFO, WARNING, CRITICAL
    val timestamp: Long = System.currentTimeMillis()
)

data class SandboxMerchantQr(
    val qrPayload: String,
    val merchantNameEn: String,
    val merchantNameNe: String,
    val paymentId: String,
    val mobile: String,
    val categoryEn: String,
    val categoryNe: String,
    val suggestedAmount: Double
)
