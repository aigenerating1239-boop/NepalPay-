package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NepalPayDao {

    // Users
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile LIMIT 1")
    fun observeUser(mobile: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(paymentId) = LOWER(:paymentId) LIMIT 1")
    suspend fun getUserByPaymentId(paymentId: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET walletBalanceNpr = :newBalance, lastActiveAt = :now WHERE mobileNumber = :mobile")
    suspend fun updateWalletBalance(mobile: String, newBalance: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isSuspended = :suspended WHERE mobileNumber = :mobile")
    suspend fun setAccountSuspended(mobile: String, suspended: Boolean)

    @Query("UPDATE users SET kycStatus = :status WHERE mobileNumber = :mobile")
    suspend fun updateUserKycStatus(mobile: String, status: String)

    // Dynamic Nepal Banks Directory
    @Query("SELECT * FROM nepal_banks ORDER BY displayOrder ASC, officialNameEn ASC")
    fun observeAllBanks(): Flow<List<BankDirectoryEntity>>

    @Query("SELECT COUNT(*) FROM nepal_banks")
    suspend fun getBankCount(): Int

    @Query("SELECT * FROM nepal_banks WHERE bankId = :bankId LIMIT 1")
    suspend fun getBankById(bankId: String): BankDirectoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: BankDirectoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanks(banks: List<BankDirectoryEntity>)

    @Update
    suspend fun updateBank(bank: BankDirectoryEntity)

    @Query("UPDATE nepal_banks SET integrationEnabled = :enabled, apiStatus = :apiStatus WHERE bankId = :bankId")
    suspend fun setBankIntegrationEnabled(bankId: String, enabled: Boolean, apiStatus: String)

    @Query("UPDATE nepal_banks SET lastVerifiedWithNrbAt = :timestamp")
    suspend fun markAllBanksVerifiedWithNrb(timestamp: Long = System.currentTimeMillis())

    // Linked Bank Accounts
    @Query("SELECT * FROM linked_bank_accounts WHERE userMobile = :mobile ORDER BY isPrimary DESC, linkedAt DESC")
    fun observeLinkedBankAccounts(mobile: String): Flow<List<LinkedBankAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkedBankAccount(account: LinkedBankAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkedBankAccounts(accounts: List<LinkedBankAccountEntity>)

    @Query("DELETE FROM linked_bank_accounts WHERE linkId = :linkId")
    suspend fun deleteLinkedBankAccount(linkId: String)

    // Reconciliation Logs
    @Query("SELECT * FROM reconciliation_logs ORDER BY timestamp DESC")
    fun observeReconciliationLogs(): Flow<List<ReconciliationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliationLog(log: ReconciliationLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliationLogs(logs: List<ReconciliationLogEntity>)

    // API Error Logs
    @Query("SELECT * FROM api_error_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeApiErrorLogs(): Flow<List<ApiErrorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiErrorLog(log: ApiErrorLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiErrorLogs(logs: List<ApiErrorLogEntity>)

    @Query("UPDATE api_error_logs SET isResolved = 1 WHERE id = :id")
    suspend fun markApiErrorResolved(id: Long)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE senderMobile = :mobile OR receiverIdentifier = :mobile OR receiverIdentifier = :paymentId ORDER BY timestamp DESC")
    fun observeUserTransactions(mobile: String, paymentId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Query("UPDATE transactions SET status = :newStatus WHERE txId = :txId")
    suspend fun updateTransactionStatus(txId: String, newStatus: String)

    // Money Requests
    @Query("SELECT * FROM money_requests WHERE payerMobile = :mobile OR requesterMobile = :mobile ORDER BY timestamp DESC")
    fun observeMoneyRequestsForUser(mobile: String): Flow<List<MoneyRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyRequest(request: MoneyRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyRequests(requests: List<MoneyRequestEntity>)

    @Query("UPDATE money_requests SET status = :status WHERE requestId = :requestId")
    suspend fun updateMoneyRequestStatus(requestId: String, status: String)

    // KYC Submissions
    @Query("SELECT * FROM kyc_submissions ORDER BY submittedAt DESC")
    fun observeAllKycSubmissions(): Flow<List<KycSubmissionEntity>>

    @Query("SELECT * FROM kyc_submissions WHERE mobileNumber = :mobile LIMIT 1")
    fun observeKycForUser(mobile: String): Flow<KycSubmissionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKycSubmission(kyc: KycSubmissionEntity)

    @Query("UPDATE kyc_submissions SET status = :status, reviewerNote = :note WHERE mobileNumber = :mobile")
    suspend fun updateKycSubmissionStatus(mobile: String, status: String, note: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userMobile = :mobile ORDER BY timestamp DESC")
    fun observeNotifications(mobile: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userMobile = :mobile")
    suspend fun markAllNotificationsRead(mobile: String)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 120")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}
