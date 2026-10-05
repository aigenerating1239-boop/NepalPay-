package com.example.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.local.BankDirectoryEntity
import com.example.data.local.SandboxMerchantQr
import com.example.data.security.SecurityManager
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Connected Payment Architecture:
 *
 * NepalPay App
 *   ↓ (TLS 1.3 + Session Token)
 * Secure Backend Gateway
 *   ↓ (Server-Side Encrypted Credentials Vault + HMAC Webhook Verifier)
 * Payment / Bank Integration Layer
 *   ↓ (Per-Bank API Adapter / Connector via BankApiConnectorRegistry)
 * Authorized Bank APIs / Licensed Payment Providers (connectIPS / NCHL / OpenBanking)
 *   ↓
 * Bank Account
 */
interface BankApiConnector {
    val adapterKey: String
    suspend fun verifyAccountHolder(
        bank: BankDirectoryEntity,
        accountNumber: String,
        expectedHolderName: String
    ): AccountVerificationResult

    suspend fun executeTransfer(
        bank: BankDirectoryEntity,
        accountNumberMasked: String,
        accountHolderName: String,
        amountNpr: Double,
        simulateOutcome: String
    ): GatewayResponse
}

data class AccountVerificationResult(
    val isVerified: Boolean,
    val verifiedHolderName: String,
    val maskedAccountNumber: String,
    val providerReference: String,
    val environmentMode: String,
    val messageEn: String,
    val messageNe: String
)

/**
 * Modular Per-Bank API Connector implementation.
 * Each bank in the directory resolves to its dedicated connector instance so that when
 * production credentials & ISO20022/REST specs are provisioned in the backend vault,
 * individual bank connectors can be switched from SANDBOX to PRODUCTION without app rebuilds.
 */
class StandardNepalBankConnector(override val adapterKey: String) : BankApiConnector {
    override suspend fun verifyAccountHolder(
        bank: BankDirectoryEntity,
        accountNumber: String,
        expectedHolderName: String
    ): AccountVerificationResult {
        delay(280)
        val masked = SecurityManager.maskCardOrAccountNumber(accountNumber)
        if (!bank.integrationEnabled || bank.apiStatus == "DISABLED") {
            return AccountVerificationResult(
                isVerified = false,
                verifiedHolderName = "",
                maskedAccountNumber = masked,
                providerReference = "ERR-DISABLED",
                environmentMode = bank.environmentMode,
                messageEn = "${bank.officialNameEn} API integration is currently disabled by Admin.",
                messageNe = "${bank.officialNameNe} को एपीआई एकीकरण हाल बन्द गरिएको छ।"
            )
        }
        val ref = "NCHL-VRF-${bank.bankId}-${UUID.randomUUID().toString().uppercase().take(5)}"
        val resolvedName = expectedHolderName.trim().ifBlank { "Verified Account Holder" }
        return AccountVerificationResult(
            isVerified = true,
            verifiedHolderName = resolvedName,
            maskedAccountNumber = masked,
            providerReference = ref,
            environmentMode = bank.environmentMode,
            messageEn = "[${bank.environmentMode}] Verified via $adapterKey (${bank.bankCode}) • Ref: $ref",
            messageNe = "[${bank.environmentMode}] $adapterKey (${bank.bankCode}) मार्फत खाता प्रमाणित भयो"
        )
    }

    override suspend fun executeTransfer(
        bank: BankDirectoryEntity,
        accountNumberMasked: String,
        accountHolderName: String,
        amountNpr: Double,
        simulateOutcome: String
    ): GatewayResponse {
        delay(320)
        val refId = "NP-${bank.bankId}-" + UUID.randomUUID().toString().uppercase().take(6)
        val payload = "$refId|${bank.bankId}|$amountNpr|$simulateOutcome"
        val signature = SecurityManager.computeWebhookHmacSignature(payload, bank.bankId)
        val webhookValid = SecurityManager.verifyWebhookSignature(payload, bank.bankId, signature)

        if (!bank.integrationEnabled || bank.apiStatus == "DISABLED") {
            return GatewayResponse(
                referenceId = refId,
                status = "FAILED",
                isSandboxMock = true,
                gatewayMessageEn = "[${bank.environmentMode}] Connector $adapterKey is disabled in Admin Panel.",
                gatewayMessageNe = "[${bank.environmentMode}] एपीआई कनेक्टर एडमिनद्वारा बन्द गरिएको छ।"
            )
        }

        return when (simulateOutcome) {
            "FAILED" -> GatewayResponse(
                referenceId = refId,
                status = "FAILED",
                isSandboxMock = bank.environmentMode != "PRODUCTION_READY",
                gatewayMessageEn = "[${bank.environmentMode}] Declined by $adapterKey (${bank.officialNameEn})",
                gatewayMessageNe = "[${bank.environmentMode}] ${bank.officialNameNe} स्विचबाट अस्वीकृत"
            )
            "PENDING" -> GatewayResponse(
                referenceId = refId,
                status = "PENDING",
                isSandboxMock = bank.environmentMode != "PRODUCTION_READY",
                gatewayMessageEn = "[${bank.environmentMode}] Queued in ${bank.officialNameEn} settlement ($adapterKey)",
                gatewayMessageNe = "[${bank.environmentMode}] ${bank.officialNameNe} क्लियरिङमा पेन्डिङ"
            )
            else -> GatewayResponse(
                referenceId = refId,
                status = if (webhookValid) "SUCCESSFUL" else "PENDING",
                isSandboxMock = bank.environmentMode != "PRODUCTION_READY",
                gatewayMessageEn = "[${bank.environmentMode}] Processed via $adapterKey (${bank.bankCode}) • HMAC Verified",
                gatewayMessageNe = "[${bank.environmentMode}] ${bank.officialNameNe} ($adapterKey) मार्फत सफल"
            )
        }
    }
}

object BankApiConnectorRegistry {
    private val connectors = mutableMapOf<String, BankApiConnector>()

    fun getConnector(adapterKey: String): BankApiConnector {
        return connectors.getOrPut(adapterKey) {
            StandardNepalBankConnector(adapterKey)
        }
    }
}

interface QrPaymentNetworkService {
    fun parseNepalPayQr(rawPayload: String): ParsedQrResult
    fun generatePersonalQrPayload(mobile: String, paymentId: String, fullName: String): String
}

interface OtpSmsGateway {
    suspend fun sendSandboxOtp(mobileNumber: String): String
}

data class GatewayResponse(
    val referenceId: String,
    val status: String, // SUCCESSFUL, PENDING, FAILED
    val isSandboxMock: Boolean = true,
    val gatewayMessageEn: String,
    val gatewayMessageNe: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ParsedQrResult(
    val isValid: Boolean,
    val recipientType: String, // MERCHANT or USER
    val paymentId: String,
    val recipientName: String,
    val mobileNumber: String,
    val presetAmountNpr: Double? = null
)

class SandboxNepalPaymentGateway : QrPaymentNetworkService, OtpSmsGateway {

    /**
     * Initial seed directory of all 22 Nepal Commercial Banks (including merged/successor institutions).
     * Stored in Room (`nepal_banks` table) so it is never hardcoded in UI and can be dynamically
     * updated from the Admin Dashboard or verified against Nepal Rastra Bank's official registry.
     */
    fun buildInitialNepalBanksSeed(): List<BankDirectoryEntity> = listOf(
        BankDirectoryEntity(
            bankId = "NBL",
            displayOrder = 1,
            officialNameEn = "Nepal Bank Limited",
            officialNameNe = "नेपाल बैंक लिमिटेड",
            shortName = "Nepal Bank",
            bankCode = "NEBLNPKA",
            category = "COMMERCIAL_STATE",
            officialWebsiteUrl = "https://www.nepalbank.com.np",
            logoMonogram = "NBL",
            logoPrimaryHex = "#003893",
            logoSecondaryHex = "#C8102E",
            apiAdapterKey = "CONNECTOR_NBL_ISO20022",
            apiEndpointUrl = "https://sandbox.api.nepalbank.com.np/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 5.0
        ),
        BankDirectoryEntity(
            bankId = "RBB",
            displayOrder = 2,
            officialNameEn = "Rastriya Banijya Bank Limited",
            officialNameNe = "राष्ट्रिय वाणिज्य बैंक लिमिटेड",
            shortName = "RBB",
            bankCode = "RBBANPKA",
            category = "COMMERCIAL_STATE",
            officialWebsiteUrl = "https://www.rbb.com.np",
            logoMonogram = "RBB",
            logoPrimaryHex = "#1E3A8A",
            logoSecondaryHex = "#0284C7",
            apiAdapterKey = "CONNECTOR_RBB_OPENBANK",
            apiEndpointUrl = "https://sandbox.api.rbb.com.np/v1/transfers",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 5.0
        ),
        BankDirectoryEntity(
            bankId = "NABIL",
            displayOrder = 3,
            officialNameEn = "Nabil Bank Limited",
            officialNameNe = "नबिल बैंक लिमिटेड",
            shortName = "Nabil Bank",
            bankCode = "NARBNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.nabilbank.com",
            logoMonogram = "NABIL",
            logoPrimaryHex = "#047857",
            logoSecondaryHex = "#10B981",
            apiAdapterKey = "CONNECTOR_NABIL_NAPI",
            apiEndpointUrl = "https://sandbox.api.nabilbank.com/v2/connect",
            apiStatus = "AUTHORIZED_READY",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "NIMB",
            displayOrder = 4,
            officialNameEn = "Nepal Investment Mega Bank Limited",
            officialNameNe = "नेपाल इन्भेष्टमेन्ट मेगा बैंक लिमिटेड",
            shortName = "NIMB",
            bankCode = "NIBLNPKT",
            category = "COMMERCIAL_PRIVATE",
            mergerNoteEn = "Formed by merger of Nepal Investment Bank & Mega Bank Nepal",
            mergerNoteNe = "नेपाल इन्भेष्टमेन्ट बैंक र मेगा बैंक गाभिएर बनेको",
            officialWebsiteUrl = "https://www.nimb.com.np",
            logoMonogram = "NIMB",
            logoPrimaryHex = "#7C2D12",
            logoSecondaryHex = "#EA580C",
            apiAdapterKey = "CONNECTOR_NIMB_DIRECT",
            apiEndpointUrl = "https://sandbox.api.nimb.com.np/v1/openbanking",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "SCBNL",
            displayOrder = 5,
            officialNameEn = "Standard Chartered Bank Nepal Limited",
            officialNameNe = "स्ट्यान्डर्ड चार्टर्ड बैंक नेपाल लिमिटेड",
            shortName = "Standard Chartered",
            bankCode = "SCBLNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.sc.com/np/",
            logoMonogram = "SCB",
            logoPrimaryHex = "#0369A1",
            logoSecondaryHex = "#16A34A",
            apiAdapterKey = "CONNECTOR_SCB_STRAIGHT2BANK",
            apiEndpointUrl = "https://sandbox.api.sc.com/np/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "HBL",
            displayOrder = 6,
            officialNameEn = "Himalayan Bank Limited",
            officialNameNe = "हिमालयन बैंक लिमिटेड",
            shortName = "Himalayan Bank",
            bankCode = "HIMANPKA",
            category = "COMMERCIAL_PRIVATE",
            mergerNoteEn = "Acquired Civil Bank Limited",
            mergerNoteNe = "सिभिल बैंकलाई प्राप्ति गरेको",
            officialWebsiteUrl = "https://www.himalayanbank.com",
            logoMonogram = "HBL",
            logoPrimaryHex = "#1E40AF",
            logoSecondaryHex = "#D97706",
            apiAdapterKey = "CONNECTOR_HBL_GATEWAY",
            apiEndpointUrl = "https://sandbox.api.himalayanbank.com/v1/transfer",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "NSBI",
            displayOrder = 7,
            officialNameEn = "Nepal SBI Bank Limited",
            officialNameNe = "नेपाल एसबिआई बैंक लिमिटेड",
            shortName = "Nepal SBI",
            bankCode = "NSBINPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://nsbl.statebank",
            logoMonogram = "SBI",
            logoPrimaryHex = "#1D4ED8",
            logoSecondaryHex = "#38BDF8",
            apiAdapterKey = "CONNECTOR_NSBI_YONO_API",
            apiEndpointUrl = "https://sandbox.api.nsbl.statebank/v1/settlement",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "EBL",
            displayOrder = 8,
            officialNameEn = "Everest Bank Limited",
            officialNameNe = "एभरेष्ट बैंक लिमिटेड",
            shortName = "Everest Bank",
            bankCode = "EVBLNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.everestbankltd.com",
            logoMonogram = "EBL",
            logoPrimaryHex = "#B91C1C",
            logoSecondaryHex = "#F59E0B",
            apiAdapterKey = "CONNECTOR_EBL_REST",
            apiEndpointUrl = "https://sandbox.api.everestbankltd.com/v1/pay",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "KBL",
            displayOrder = 9,
            officialNameEn = "Kumari Bank Limited",
            officialNameNe = "कुमारी बैंक लिमिटेड",
            shortName = "Kumari Bank",
            bankCode = "KMBLNPKA",
            category = "COMMERCIAL_PRIVATE",
            mergerNoteEn = "Merged with Nepal Credit and Commerce (NCC) Bank",
            mergerNoteNe = "एनसीसी (NCC) बैंकसँग मर्ज भएको",
            officialWebsiteUrl = "https://www.kumaribank.com",
            logoMonogram = "KBL",
            logoPrimaryHex = "#991B1B",
            logoSecondaryHex = "#EA580C",
            apiAdapterKey = "CONNECTOR_KBL_OPENAPI",
            apiEndpointUrl = "https://sandbox.api.kumaribank.com/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "LSBL",
            displayOrder = 10,
            officialNameEn = "Laxmi Sunrise Bank Limited",
            officialNameNe = "लक्ष्मी सनराइज बैंक लिमिटेड",
            shortName = "Laxmi Sunrise",
            bankCode = "LXBLNPKA",
            category = "COMMERCIAL_PRIVATE",
            mergerNoteEn = "Formed by merger of Laxmi Bank & Sunrise Bank",
            mergerNoteNe = "लक्ष्मी बैंक र सनराइज बैंक गाभिएर बनेको",
            officialWebsiteUrl = "https://www.laxmisunrise.com",
            logoMonogram = "LSBL",
            logoPrimaryHex = "#C2410C",
            logoSecondaryHex = "#F59E0B",
            apiAdapterKey = "CONNECTOR_LSBL_API",
            apiEndpointUrl = "https://sandbox.api.laxmisunrise.com/v1/connect",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "CZBIL",
            displayOrder = 11,
            officialNameEn = "Citizens Bank International Limited",
            officialNameNe = "सिटिजन्स बैंक इन्टरनेसनल लिमिटेड",
            shortName = "Citizens Bank",
            bankCode = "CTZNNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.ctznbank.com",
            logoMonogram = "CTZN",
            logoPrimaryHex = "#0F766E",
            logoSecondaryHex = "#14B8A6",
            apiAdapterKey = "CONNECTOR_CTZN_API",
            apiEndpointUrl = "https://sandbox.api.ctznbank.com/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "PCBL",
            displayOrder = 12,
            officialNameEn = "Prime Commercial Bank Limited",
            officialNameNe = "प्राइम कमर्सियल बैंक लिमिटेड",
            shortName = "Prime Bank",
            bankCode = "PCBLNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.primebank.com.np",
            logoMonogram = "PCBL",
            logoPrimaryHex = "#1E3A8A",
            logoSecondaryHex = "#DC2626",
            apiAdapterKey = "CONNECTOR_PRIME_API",
            apiEndpointUrl = "https://sandbox.api.primebank.com.np/v1/transfer",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "SANIMA",
            displayOrder = 13,
            officialNameEn = "Sanima Bank Limited",
            officialNameNe = "सानिमा बैंक लिमिटेड",
            shortName = "Sanima Bank",
            bankCode = "SNMANPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.sanimabank.com",
            logoMonogram = "SNMA",
            logoPrimaryHex = "#047857",
            logoSecondaryHex = "#0284C7",
            apiAdapterKey = "CONNECTOR_SANIMA_SAJILO",
            apiEndpointUrl = "https://sandbox.api.sanimabank.com/v1/payments",
            apiStatus = "AUTHORIZED_READY",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "MBL",
            displayOrder = 14,
            officialNameEn = "Machhapuchchhre Bank Limited",
            officialNameNe = "माछापुच्छ्रे बैंक लिमिटेड",
            shortName = "Machhapuchchhre",
            bankCode = "MBLNNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.machbank.com",
            logoMonogram = "MBL",
            logoPrimaryHex = "#0369A1",
            logoSecondaryHex = "#DC2626",
            apiAdapterKey = "CONNECTOR_MBL_SMART",
            apiEndpointUrl = "https://sandbox.api.machbank.com/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "NICA",
            displayOrder = 15,
            officialNameEn = "NIC Asia Bank Limited",
            officialNameNe = "एनआईसी एशिया बैंक लिमिटेड",
            shortName = "NIC Asia",
            bankCode = "NICENPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.nicasiabank.com",
            logoMonogram = "NICA",
            logoPrimaryHex = "#C8102E",
            logoSecondaryHex = "#991B1B",
            apiAdapterKey = "CONNECTOR_NICA_MOBANK",
            apiEndpointUrl = "https://sandbox.api.nicasiabank.com/v2/payments",
            apiStatus = "AUTHORIZED_READY",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "SBL",
            displayOrder = 16,
            officialNameEn = "Siddhartha Bank Limited",
            officialNameNe = "सिद्धार्थ बैंक लिमिटेड",
            shortName = "Siddhartha Bank",
            bankCode = "SIDDNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.siddharthabank.com",
            logoMonogram = "SBL",
            logoPrimaryHex = "#B45309",
            logoSecondaryHex = "#1E3A8A",
            apiAdapterKey = "CONNECTOR_SBL_API",
            apiEndpointUrl = "https://sandbox.api.siddharthabank.com/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "ADBL",
            displayOrder = 17,
            officialNameEn = "Agriculture Development Bank Limited",
            officialNameNe = "कृषि विकास बैंक लिमिटेड",
            shortName = "ADBL",
            bankCode = "ADBLNPKA",
            category = "COMMERCIAL_STATE",
            officialWebsiteUrl = "https://www.adbl.gov.np",
            logoMonogram = "ADBL",
            logoPrimaryHex = "#15803D",
            logoSecondaryHex = "#CA8A04",
            apiAdapterKey = "CONNECTOR_ADBL_GOV",
            apiEndpointUrl = "https://sandbox.api.adbl.gov.np/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 5.0
        ),
        BankDirectoryEntity(
            bankId = "GIME",
            displayOrder = 18,
            officialNameEn = "Global IME Bank Limited",
            officialNameNe = "ग्लोबल आइएमई बैंक लिमिटेड",
            shortName = "Global IME",
            bankCode = "GLBBNPKA",
            category = "COMMERCIAL_PRIVATE",
            mergerNoteEn = "Merged with Bank of Kathmandu (BOK)",
            mergerNoteNe = "बैंक अफ काठमाण्डू (BOK) सँग मर्ज भएको",
            officialWebsiteUrl = "https://www.globalimebank.com",
            logoMonogram = "GIME",
            logoPrimaryHex = "#1E3A8A",
            logoSecondaryHex = "#C8102E",
            apiAdapterKey = "CONNECTOR_GIME_GLOBALSMART",
            apiEndpointUrl = "https://sandbox.api.globalimebank.com/v2/payments",
            apiStatus = "AUTHORIZED_READY",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "PRABHU",
            displayOrder = 19,
            officialNameEn = "Prabhu Bank Limited",
            officialNameNe = "प्रभु बैंक लिमिटेड",
            shortName = "Prabhu Bank",
            bankCode = "PRVUNPKA",
            category = "COMMERCIAL_PRIVATE",
            mergerNoteEn = "Acquired Century Commercial Bank Limited",
            mergerNoteNe = "सेन्चुरी कमर्सियल बैंकलाई प्राप्ति गरेको",
            officialWebsiteUrl = "https://www.prabhubank.com",
            logoMonogram = "PRVU",
            logoPrimaryHex = "#B91C1C",
            logoSecondaryHex = "#4B5563",
            apiAdapterKey = "CONNECTOR_PRABHU_API",
            apiEndpointUrl = "https://sandbox.api.prabhubank.com/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "NMB",
            displayOrder = 20,
            officialNameEn = "NMB Bank Limited",
            officialNameNe = "एनएमबि बैंक लिमिटेड",
            shortName = "NMB Bank",
            bankCode = "NMBBNPKA",
            category = "COMMERCIAL_PRIVATE",
            officialWebsiteUrl = "https://www.nmb.com.np",
            logoMonogram = "NMB",
            logoPrimaryHex = "#0369A1",
            logoSecondaryHex = "#0D9488",
            apiAdapterKey = "CONNECTOR_NMB_OPENAPI",
            apiEndpointUrl = "https://sandbox.api.nmb.com.np/v1/payments",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "NBB_SUCC",
            displayOrder = 21,
            officialNameEn = "Nepal Bangladesh Bank (Successor: Nabil Bank Limited)",
            officialNameNe = "नेपाल बंगलादेश बैंक (हाल: नबिल बैंक लिमिटेड)",
            shortName = "NBB → Nabil",
            bankCode = "NARBNPKA",
            category = "MERGED_SUCCESSOR",
            mergerNoteEn = "Acquired by Nabil Bank Limited; routed via Nabil Bank API Connector",
            mergerNoteNe = "नबिल बैंक लिमिटेडमा गाभिएको (उत्तराधिकारी संस्था: नबिल बैंक)",
            successorBankId = "NABIL",
            officialWebsiteUrl = "https://www.nabilbank.com",
            logoMonogram = "NBB",
            logoPrimaryHex = "#047857",
            logoSecondaryHex = "#64748B",
            apiAdapterKey = "CONNECTOR_NABIL_NAPI",
            apiEndpointUrl = "https://sandbox.api.nabilbank.com/v2/legacy-nbb",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        ),
        BankDirectoryEntity(
            bankId = "CCBL_SUCC",
            displayOrder = 22,
            officialNameEn = "Century Commercial Bank (Successor: Prabhu Bank Limited)",
            officialNameNe = "सेन्चुरी कमर्सियल बैंक (हाल: प्रभु बैंक लिमिटेड)",
            shortName = "Century → Prabhu",
            bankCode = "PRVUNPKA",
            category = "MERGED_SUCCESSOR",
            mergerNoteEn = "Acquired by Prabhu Bank Limited; routed via Prabhu Bank API Connector",
            mergerNoteNe = "प्रभु बैंक लिमिटेडमा गाभिएको (उत्तराधिकारी संस्था: प्रभु बैंक)",
            successorBankId = "PRABHU",
            officialWebsiteUrl = "https://www.prabhubank.com",
            logoMonogram = "CCBL",
            logoPrimaryHex = "#B91C1C",
            logoSecondaryHex = "#64748B",
            apiAdapterKey = "CONNECTOR_PRABHU_API",
            apiEndpointUrl = "https://sandbox.api.prabhubank.com/v1/legacy-ccbl",
            apiStatus = "CONNECTED_SANDBOX",
            environmentMode = "SANDBOX",
            integrationEnabled = true,
            transferFeeNpr = 10.0
        )
    )

    val sandboxMerchants: List<SandboxMerchantQr> = listOf(
        SandboxMerchantQr(
            qrPayload = "NEPALPAY_QR|MERCHANT|himalayan.java@nepalpay|Himalayan Java Coffee - Durbar Marg|+9779851099887|450.0",
            merchantNameEn = "Himalayan Java Coffee",
            merchantNameNe = "हिमालयन जाभा कफी - दरबारमार्ग",
            paymentId = "himalayan.java@nepalpay",
            mobile = "+9779851099887",
            categoryEn = "Cafe & Restaurant • NepalQR Sandbox",
            categoryNe = "क्याफे र रेस्टुरेन्ट • नेपाल क्यूआर",
            suggestedAmount = 450.0
        ),
        SandboxMerchantQr(
            qrPayload = "NEPALPAY_QR|MERCHANT|bhatbhateni@nepalpay|Bhat-Bhateni Supermarket - Maharajgunj|+9779851011223|2850.0",
            merchantNameEn = "Bhat-Bhateni Supermarket",
            merchantNameNe = "भाट-भटेनी सुपरमार्केट - महाराजगन्ज",
            paymentId = "bhatbhateni@nepalpay",
            mobile = "+9779851011223",
            categoryEn = "Department Store • NepalQR Sandbox",
            categoryNe = "डिपार्टमेन्ट स्टोर • नेपाल क्यूआर",
            suggestedAmount = 2850.0
        ),
        SandboxMerchantQr(
            qrPayload = "NEPALPAY_QR|MERCHANT|nea.electricity@nepalpay|Nepal Electricity Authority (NEA)|+9779851044556|1420.0",
            merchantNameEn = "Nepal Electricity Authority (NEA)",
            merchantNameNe = "नेपाल विद्युत प्राधिकरण (NEA)",
            paymentId = "nea.electricity@nepalpay",
            mobile = "+9779851044556",
            categoryEn = "Government Utility • Sandbox",
            categoryNe = "विद्युत महसुल भुक्तानी • स्यान्डबक्स",
            suggestedAmount = 1420.0
        ),
        SandboxMerchantQr(
            qrPayload = "NEPALPAY_QR|USER|sita@nepalpay|Sita Gurung|+9779801122334|0.0",
            merchantNameEn = "Sita Gurung (Personal QR)",
            merchantNameNe = "सीता गुरुङ (व्यक्तिगत QR)",
            paymentId = "sita@nepalpay",
            mobile = "+9779801122334",
            categoryEn = "Peer-to-Peer Wallet QR",
            categoryNe = "व्यक्तिगत वालेट क्यूआर",
            suggestedAmount = 1000.0
        )
    )

    suspend fun verifyBankAccountHolderViaConnector(
        bank: BankDirectoryEntity,
        accountNumber: String,
        holderName: String
    ): AccountVerificationResult {
        val connector = BankApiConnectorRegistry.getConnector(bank.apiAdapterKey)
        return connector.verifyAccountHolder(bank, accountNumber, holderName)
    }

    suspend fun executeBankTransferViaConnector(
        bank: BankDirectoryEntity,
        accountNumberMasked: String,
        accountHolderName: String,
        amountNpr: Double,
        simulateOutcome: String
    ): GatewayResponse {
        val connector = BankApiConnectorRegistry.getConnector(bank.apiAdapterKey)
        return connector.executeTransfer(bank, accountNumberMasked, accountHolderName, amountNpr, simulateOutcome)
    }

    override fun parseNepalPayQr(rawPayload: String): ParsedQrResult {
        val trimmed = rawPayload.trim()
        if (trimmed.startsWith("NEPALPAY_QR|")) {
            val parts = trimmed.split("|")
            if (parts.size >= 5) {
                val type = parts[1]
                val paymentId = parts[2]
                val name = parts[3]
                val mobile = parts[4]
                val amount = parts.getOrNull(5)?.toDoubleOrNull()?.takeIf { it > 0.0 }
                return ParsedQrResult(
                    isValid = true,
                    recipientType = type,
                    paymentId = paymentId,
                    recipientName = name,
                    mobileNumber = mobile,
                    presetAmountNpr = amount
                )
            }
        }
        if (trimmed.contains("@nepalpay")) {
            val name = trimmed.substringBefore("@").replace(".", " ")
                .replaceFirstChar { it.uppercase() }
            return ParsedQrResult(
                isValid = true,
                recipientType = "MERCHANT",
                paymentId = trimmed,
                recipientName = "$name (NepalPay QR)",
                mobileNumber = "+9779851000000",
                presetAmountNpr = null
            )
        }
        return ParsedQrResult(
            isValid = false,
            recipientType = "UNKNOWN",
            paymentId = "",
            recipientName = "",
            mobileNumber = ""
        )
    }

    override fun generatePersonalQrPayload(
        mobile: String,
        paymentId: String,
        fullName: String
    ): String {
        return "NEPALPAY_QR|USER|$paymentId|$fullName|$mobile|0.0"
    }

    override suspend fun sendSandboxOtp(mobileNumber: String): String {
        delay(200)
        return "482910"
    }
}

object SystemNotificationDispatcher {
    private const val CHANNEL_ID = "nepalpay_transactions_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NepalPay Sandbox Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for NepalPay sandbox payments, requests, and security alerts"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun dispatchPush(context: Context, title: String, body: String) {
        try {
            ensureChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: Exception) {
            // Safe fallback in unit/Robolectric environments
        }
    }
}
