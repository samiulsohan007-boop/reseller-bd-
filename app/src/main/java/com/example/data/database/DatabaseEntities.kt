package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val videoUrl: String = "",
    val skuCode: String,
    val wholesalePrice: Double,
    val stockStatus: String = "In Stock", // "In Stock", "Out of Stock"
    val sizes: String = "M, L, XL, XXL",
    val colors: String = "Black, Blue, Grey",
    val videoReviewUrl: String = "",
    val additionalImageUrls: String = "",
    val galleryVideoUrls: String = "",
    val facebookVideoUrl: String = "",
    val youtubeVideoUrl: String = "",
    val tiktokVideoUrl: String = "",
    val category: String = "অন্যান্য ক্যাটাগরি",
    val subcategory: String = "",
    val addedByRole: String = "Admin",
    val addedByPhone: String = ""
)

@Entity(tableName = "categories")
data class CategoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String = "📦",
    val subcategories: String = "" // Comma-separated
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val orderId: Int = 0,
    val customerName: String,
    val customerPhone: String,
    val district: String,
    val thana: String,
    val fullAddress: String,
    val deliveryInstructions: String,
    val paymentType: String, // "COD", "Advance Delivery", "Full Advance"
    val paymentMethod: String, // "bKash", "Nagad", "Rocket"
    val paymentStatus: String = "Pending Payment", // "Pending Payment", "Paid", "COD"
    val senderNumber: String = "",
    val transactionId: String = "",
    val paidAmount: Double = 0.0,
    val totalWholesalePrice: Double,
    val totalSellingPrice: Double,
    val calculatedProfit: Double,
    val deliveryCharge: Double,
    val orderStatus: String = "Pending", // "Pending", "Confirmed", "Processing", "Shipped", "Delivered", "Cancelled"
    val trackingNumber: String = "",
    val trackingLink: String = "",
    val productInfo: String = "", // Holds product titles and quantities
    val productImageUrls: String = "", // Comma-separated list of product image URLs
    val date: Long = System.currentTimeMillis(),
    val deliveredDate: Long = 0L,
    val isProfitReleased: Boolean = false,
    val adminRole: String = "Admin",
    val adminPhone: String = "",
    val cancellationReason: String = ""
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val targetRole: String, // "ADMIN", "RESELLER", "ALL"
    val type: String = "GENERAL", // "ORDER", "PRODUCT", "WITHDRAWAL", "TRACKING", "PROFIT"
    val relatedId: Int = 0,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val id: Int = 1,
    val totalProfit: Double = 0.0,
    val totalCommission: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val activeBalance: Double = 0.0
)

@Entity(tableName = "withdrawals")
data class Withdrawal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val charge: Double = 5.0,
    val paymentMethod: String, // "bKash", "Nagad", "Rocket"
    val accountNumber: String,
    val status: String = "Pending", // "Pending", "Approved", "Rejected"
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "referral_info")
data class ReferralInfo(
    @PrimaryKey val id: Int = 1,
    val referralCode: String = "RES500K",
    val totalInvited: Int = 0,
    val totalEarnings: Double = 0.0
)

@Entity(tableName = "banners")
data class Banner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUrl: String,
    val title: String,
    val targetCategory: String = ""
)

@Entity(tableName = "resellers")
data class ResellerUser(
    @PrimaryKey val phone: String,
    val name: String,
    val email: String,
    val isBlocked: Boolean = false,
    val registeredDate: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val password: String = "123456",
    val profileImage: String = ""
)

@Entity(tableName = "support_messages")
data class SupportMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resellerPhone: String,
    val resellerName: String = "",
    val text: String,
    val isFromAdmin: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val adminKey: String = "Admin"
)

@Entity(tableName = "custom_social_channels")
data class CustomSocialChannel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val platformType: String = "AUTO",
    val isEnabled: Boolean = true
)

@Entity(tableName = "referred_users")
data class ReferredUserRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val referrerCode: String = "BDRES99",
    val userPhone: String,
    val userName: String,
    val registeredDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "referral_orders")
data class ReferralOrderRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderIdStr: String,
    val referrerCode: String = "BDRES99",
    val buyerName: String,
    val buyerPhone: String,
    val orderAmount: Double,
    val commissionAmount: Double = 20.0,
    val status: String = "Pending", // "Pending", "Completed", "Cancelled"
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "tutorial_videos")
data class TutorialVideo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val targetAudience: String = "Reseller", // "Reseller", "SubAdmin", "Both"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sub_admin_requests")
data class SubAdminRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val packageName: String, // "1 Month", "6 Months", "12 Months", "VIP Free"
    val packagePrice: Double, // 200.0, 1000.0, 1850.0, 0.0
    val paymentMethod: String, // "bKash", "Nagad", "Rocket", "VIP Free"
    val senderPhone: String,
    val trxId: String,
    val status: String = "Pending", // "Pending", "Approved", "Rejected", "Blocked"
    val requestedDate: Long = System.currentTimeMillis(),
    val approvedDate: Long = 0L,
    val expiryDate: Long = 0L,
    val isBlocked: Boolean = false
)

@Entity(tableName = "payment_method_configs")
data class PaymentMethodConfig(
    @PrimaryKey val methodKey: String, // "bKash", "Nagad", "Rocket"
    val methodName: String,
    val accountNumber: String,
    val isEnabled: Boolean = true
)

