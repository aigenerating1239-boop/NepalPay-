package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurityManager
import com.example.data.service.SandboxNepalPaymentGateway
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.NprFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read NepalPay app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NepalPay", appName)
    }

    @Test
    fun `validate Nepal +977 mobile numbers and salted SHA-256 MPIN verification`() {
        assertTrue(SecurityManager.isValidNepalMobile("9841234567"))
        assertTrue(SecurityManager.isValidNepalMobile("+9779801122334"))
        assertFalse(SecurityManager.isValidNepalMobile("12345"))

        val salt = SecurityManager.generateSalt()
        val mpinHash = SecurityManager.hashSecret("1234", salt)
        assertTrue(SecurityManager.verifyMpin("1234", mpinHash, salt))
        assertFalse(SecurityManager.verifyMpin("9999", mpinHash, salt))
    }

    @Test
    fun `verify 22 initial Nepal commercial banks directory and official HTTPS links`() {
        val gateway = SandboxNepalPaymentGateway()
        val banks = gateway.buildInitialNepalBanksSeed()
        assertEquals(22, banks.size)
        banks.forEach { bank ->
            assertTrue(
                "Bank ${bank.officialNameEn} must have official HTTPS link",
                SecurityManager.isSafeOfficialBankUrl(bank.officialWebsiteUrl)
            )
        }

        val qr = gateway.parseNepalPayQr(
            "NEPALPAY_QR|MERCHANT|himalayan.java@nepalpay|Himalayan Java Coffee|+9779851099887|450.0"
        )
        assertTrue(qr.isValid)
        assertEquals("himalayan.java@nepalpay", qr.paymentId)
        assertEquals(450.0, qr.presetAmountNpr ?: 0.0, 0.01)

        val formattedEn = NprFormatter.format(1500.0, AppLanguage.ENGLISH)
        assertTrue(formattedEn.contains("रु."))
    }
}
