package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Products
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Int): Flow<Product?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // Orders
    @Query("SELECT * FROM orders ORDER BY date DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    fun getOrderById(orderId: Int): Flow<Order?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    // Wallet
    @Query("SELECT * FROM wallet WHERE id = 1")
    fun getWalletFlow(): Flow<Wallet?>

    @Query("SELECT * FROM wallet WHERE id = 1")
    suspend fun getWallet(): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet)

    @Update
    suspend fun updateWallet(wallet: Wallet)

    // Withdrawals
    @Query("SELECT * FROM withdrawals ORDER BY date DESC")
    fun getAllWithdrawals(): Flow<List<Withdrawal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: Withdrawal)

    @Update
    suspend fun updateWithdrawal(withdrawal: Withdrawal)

    // Referral Info
    @Query("SELECT * FROM referral_info WHERE id = 1")
    fun getReferralInfoFlow(): Flow<ReferralInfo?>

    @Query("SELECT * FROM referral_info WHERE id = 1")
    suspend fun getReferralInfo(): ReferralInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferralInfo(info: ReferralInfo)

    @Update
    suspend fun updateReferralInfo(info: ReferralInfo)

    // Banners
    @Query("SELECT * FROM banners ORDER BY id DESC")
    fun getBanners(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner)

    @Delete
    suspend fun deleteBanner(banner: Banner)

    @Query("DELETE FROM banners")
    suspend fun deleteAllBanners()

    // Resellers
    @Query("SELECT * FROM resellers ORDER BY registeredDate DESC")
    fun getAllResellers(): Flow<List<ResellerUser>>

    @Query("SELECT * FROM resellers ORDER BY registeredDate DESC")
    suspend fun getAllResellersDirectly(): List<ResellerUser>

    @Query("SELECT * FROM resellers WHERE phone = :phone OR phone = :cleanPhone OR phone = '0' || :cleanPhone OR phone = '+880' || :cleanPhone OR phone = '880' || :cleanPhone LIMIT 1")
    fun getResellerByPhoneFlow(phone: String, cleanPhone: String = phone): Flow<ResellerUser?>

    @Query("SELECT * FROM resellers WHERE phone = :phone OR phone = :cleanPhone OR phone = '0' || :cleanPhone OR phone = '+880' || :cleanPhone OR phone = '880' || :cleanPhone LIMIT 1")
    suspend fun getResellerByPhone(phone: String, cleanPhone: String = phone): ResellerUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReseller(reseller: ResellerUser)

    @Update
    suspend fun updateReseller(reseller: ResellerUser)

    @Delete
    suspend fun deleteReseller(reseller: ResellerUser)

    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryItem)

    @Delete
    suspend fun deleteCategory(category: CategoryItem)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Delete
    suspend fun deleteNotification(notification: NotificationItem)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    // Support Messages
    @Query("SELECT * FROM support_messages WHERE resellerPhone = :phone ORDER BY timestamp ASC")
    fun getSupportMessagesForReseller(phone: String): Flow<List<SupportMessage>>

    @Query("SELECT * FROM support_messages ORDER BY timestamp ASC")
    fun getAllSupportMessages(): Flow<List<SupportMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupportMessage(message: SupportMessage)

    @Query("DELETE FROM support_messages WHERE resellerPhone = :phone")
    suspend fun deleteSupportMessagesForReseller(phone: String)

    // Custom Social Channels
    @Query("SELECT * FROM custom_social_channels ORDER BY id ASC")
    fun getAllCustomSocialChannels(): Flow<List<CustomSocialChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomSocialChannel(channel: CustomSocialChannel)

    @Delete
    suspend fun deleteCustomSocialChannel(channel: CustomSocialChannel)

    // Referred Users
    @Query("SELECT * FROM referred_users ORDER BY registeredDate DESC")
    fun getAllReferredUsers(): Flow<List<ReferredUserRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferredUser(user: ReferredUserRecord)

    // Referral Orders
    @Query("SELECT * FROM referral_orders ORDER BY date DESC")
    fun getAllReferralOrders(): Flow<List<ReferralOrderRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferralOrder(order: ReferralOrderRecord)

    @Update
    suspend fun updateReferralOrder(order: ReferralOrderRecord)

    // Tutorial Videos (Newest First)
    @Query("SELECT * FROM tutorial_videos ORDER BY createdAt DESC")
    fun getAllTutorialVideos(): Flow<List<TutorialVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTutorialVideo(video: TutorialVideo): Long

    @Update
    suspend fun updateTutorialVideo(video: TutorialVideo)

    @Delete
    suspend fun deleteTutorialVideo(video: TutorialVideo)
}


