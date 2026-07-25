package com.example.data.repository

import android.util.Log
import com.example.data.database.Banner
import com.example.data.database.CategoryItem
import com.example.data.database.NotificationItem
import com.example.data.database.Order
import com.example.data.database.PaymentMethodConfig
import com.example.data.database.Product
import com.example.data.database.ResellerUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreManager {

    private val db: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Firestore access error: ${e.message}")
            null
        }

    // ---------------- USERS COLLECTION ----------------
    suspend fun saveUserProfile(
        uid: String,
        name: String,
        email: String,
        phone: String,
        role: String = "Reseller",
        profileImage: String = ""
    ): Boolean {
        return try {
            val firestore = db ?: return false
            val userMap = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to name,
                "email" to email,
                "phone" to phone,
                "role" to role,
                "profileImage" to profileImage,
                "createdAt" to System.currentTimeMillis(),
                "lastActive" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid.ifEmpty { phone }).set(userMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving user profile: ${e.message}")
            false
        }
    }

    suspend fun getUserProfile(uidOrPhone: String): Map<String, Any>? {
        return try {
            val firestore = db ?: return null
            val doc = firestore.collection("users").document(uidOrPhone).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error getting user profile: ${e.message}")
            null
        }
    }

    // ---------------- RESELLERS COLLECTION ----------------
    suspend fun saveResellerToFirestore(reseller: ResellerUser): Boolean {
        return try {
            val firestore = db ?: return false
            val resellerMap = hashMapOf<String, Any>(
                "phone" to reseller.phone,
                "name" to reseller.name,
                "email" to reseller.email,
                "isBlocked" to reseller.isBlocked,
                "registeredDate" to reseller.registeredDate,
                "lastActive" to reseller.lastActive,
                "profileImage" to reseller.profileImage
            )
            firestore.collection("resellers").document(reseller.phone).set(resellerMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving reseller: ${e.message}")
            false
        }
    }

    // ---------------- PRODUCTS COLLECTION ----------------
    suspend fun saveProductToFirestore(product: Product): Boolean {
        return try {
            val firestore = db ?: return false
            val prodMap = hashMapOf<String, Any>(
                "id" to product.id,
                "title" to product.title,
                "description" to product.description,
                "imageUrl" to product.imageUrl,
                "videoUrl" to product.videoUrl,
                "skuCode" to product.skuCode,
                "wholesalePrice" to product.wholesalePrice,
                "stockStatus" to product.stockStatus,
                "sizes" to product.sizes,
                "colors" to product.colors,
                "category" to product.category,
                "subcategory" to product.subcategory
            )
            val docId = if (product.id > 0) product.id.toString() else product.skuCode
            firestore.collection("products").document(docId).set(prodMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving product: ${e.message}")
            false
        }
    }

    // ---------------- CATEGORIES COLLECTION ----------------
    suspend fun saveCategoryToFirestore(category: CategoryItem): Boolean {
        return try {
            val firestore = db ?: return false
            val catMap = hashMapOf<String, Any>(
                "id" to category.id,
                "name" to category.name,
                "icon" to category.icon,
                "subcategories" to category.subcategories
            )
            val docId = if (category.id > 0) category.id.toString() else category.name
            firestore.collection("categories").document(docId).set(catMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving category: ${e.message}")
            false
        }
    }

    // ---------------- ORDERS COLLECTION ----------------
    suspend fun saveOrderToFirestore(order: Order): Boolean {
        return try {
            val firestore = db ?: return false
            val orderMap = hashMapOf<String, Any>(
                "orderId" to order.orderId,
                "customerName" to order.customerName,
                "customerPhone" to order.customerPhone,
                "district" to order.district,
                "thana" to order.thana,
                "fullAddress" to order.fullAddress,
                "deliveryInstructions" to order.deliveryInstructions,
                "paymentType" to order.paymentType,
                "paymentMethod" to order.paymentMethod,
                "paymentStatus" to order.paymentStatus,
                "totalWholesalePrice" to order.totalWholesalePrice,
                "totalSellingPrice" to order.totalSellingPrice,
                "calculatedProfit" to order.calculatedProfit,
                "deliveryCharge" to order.deliveryCharge,
                "orderStatus" to order.orderStatus,
                "trackingNumber" to order.trackingNumber,
                "trackingLink" to order.trackingLink,
                "productInfo" to order.productInfo,
                "productImageUrls" to order.productImageUrls,
                "date" to order.date,
                "deliveredDate" to order.deliveredDate,
                "isProfitReleased" to order.isProfitReleased
            )
            firestore.collection("orders").document(order.orderId.toString()).set(orderMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving order: ${e.message}")
            false
        }
    }

    // ---------------- BANNERS COLLECTION ----------------
    suspend fun saveBannerToFirestore(banner: Banner): Boolean {
        return try {
            val firestore = db ?: return false
            val bannerMap = hashMapOf<String, Any>(
                "id" to banner.id,
                "imageUrl" to banner.imageUrl,
                "title" to banner.title,
                "targetCategory" to banner.targetCategory
            )
            firestore.collection("banners").document(banner.id.toString()).set(bannerMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving banner: ${e.message}")
            false
        }
    }

    // ---------------- NOTIFICATIONS COLLECTION ----------------
    suspend fun saveNotificationToFirestore(notification: NotificationItem): Boolean {
        return try {
            val firestore = db ?: return false
            val notifMap = hashMapOf<String, Any>(
                "id" to notification.id,
                "title" to notification.title,
                "message" to notification.message,
                "targetRole" to notification.targetRole,
                "type" to notification.type,
                "relatedId" to notification.relatedId,
                "isRead" to notification.isRead,
                "timestamp" to notification.timestamp
            )
            firestore.collection("notifications").document(notification.id.toString()).set(notifMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving notification: ${e.message}")
            false
        }
    }

    // ---------------- SETTINGS COLLECTION ----------------
    suspend fun savePaymentConfigToFirestore(config: PaymentMethodConfig): Boolean {
        return try {
            val firestore = db ?: return false
            val configMap = hashMapOf<String, Any>(
                "methodKey" to config.methodKey,
                "methodName" to config.methodName,
                "accountNumber" to config.accountNumber,
                "isEnabled" to config.isEnabled
            )
            firestore.collection("settings").document("payment_${config.methodKey}").set(configMap, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving payment config: ${e.message}")
            false
        }
    }

    suspend fun saveAppNoticeToFirestore(notice: String): Boolean {
        return try {
            val firestore = db ?: return false
            firestore.collection("settings").document("app_notice").set(mapOf("notice" to notice, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving app notice: ${e.message}")
            false
        }
    }
}
