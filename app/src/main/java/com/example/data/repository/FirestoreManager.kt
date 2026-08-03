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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
        sellerCode: String = "",
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
                "sellerCode" to sellerCode,
                "role" to role,
                "profileImage" to profileImage,
                "createdAt" to System.currentTimeMillis(),
                "lastActive" to System.currentTimeMillis()
            )
            val docId = uid.ifEmpty { phone }
            firestore.collection("users").document(docId).set(userMap, SetOptions.merge()).await()
            firestore.collection("resellers").document(docId).set(userMap, SetOptions.merge()).await()
            Log.i("FirestoreManager", "Successfully saved user profile to Cloud Firestore for UID: $docId")
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
            if (doc.exists()) {
                doc.data
            } else {
                val phoneQuery = firestore.collection("users").whereEqualTo("phone", uidOrPhone).limit(1).get().await()
                if (!phoneQuery.isEmpty) {
                    phoneQuery.documents.first().data
                } else {
                    val emailQuery = firestore.collection("users").whereEqualTo("email", uidOrPhone).limit(1).get().await()
                    if (!emailQuery.isEmpty) {
                        emailQuery.documents.first().data
                    } else {
                        val uidQuery = firestore.collection("users").whereEqualTo("uid", uidOrPhone).limit(1).get().await()
                        if (!uidQuery.isEmpty) {
                            uidQuery.documents.first().data
                        } else null
                    }
                }
            }
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

    suspend fun deleteProductFromFirestore(productId: Int, skuCode: String): Boolean {
        return try {
            val firestore = db ?: return false
            val docId = if (productId > 0) productId.toString() else skuCode
            firestore.collection("products").document(docId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error deleting product from Firestore: ${e.message}")
            false
        }
    }

    suspend fun getAllProductsFromFirestore(): List<Product> {
        return try {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(3500L) {
                    val firestore = db ?: return@withTimeoutOrNull emptyList()
                    val snapshot = firestore.collection("products").get().await()
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getLong("id")?.toInt() ?: doc.id.toIntOrNull() ?: 0
                            val title = doc.getString("title") ?: return@mapNotNull null
                            val description = doc.getString("description") ?: ""
                            val imageUrl = doc.getString("imageUrl") ?: ""
                            val videoUrl = doc.getString("videoUrl") ?: ""
                            val skuCode = doc.getString("skuCode") ?: "SKU-$id"
                            val wholesalePrice = doc.getDouble("wholesalePrice") ?: (doc.getLong("wholesalePrice")?.toDouble() ?: 0.0)
                            val stockStatus = doc.getString("stockStatus") ?: "In Stock"
                            val sizes = doc.getString("sizes") ?: "S, M, L, XL"
                            val colors = doc.getString("colors") ?: "Default"
                            val category = doc.getString("category") ?: "সব প্রোডাক্ট"
                            val subcategory = doc.getString("subcategory") ?: ""

                            Product(
                                id = id,
                                title = title,
                                description = description,
                                imageUrl = imageUrl,
                                videoUrl = videoUrl,
                                skuCode = skuCode,
                                wholesalePrice = wholesalePrice,
                                stockStatus = stockStatus,
                                sizes = sizes,
                                colors = colors,
                                category = category,
                                subcategory = subcategory
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                } ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error fetching products from Firestore: ${e.message}")
            emptyList()
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
    suspend fun isTransactionIdExists(transactionId: String): Boolean {
        val cleanTrx = transactionId.trim()
        if (cleanTrx.isEmpty()) return false
        return try {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(2000L) {
                    val firestore = db ?: return@withTimeoutOrNull false
                    val querySnapshot = firestore.collection("orders")
                        .whereEqualTo("transactionId", cleanTrx)
                        .limit(1)
                        .get()
                        .await()
                    !querySnapshot.isEmpty
                } ?: false
            }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error checking transaction ID: ${e.message}")
            false
        }
    }

    suspend fun getMaxOrderIdFromFirestore(): Int {
        return try {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(2000L) {
                    val firestore = db ?: return@withTimeoutOrNull 0
                    val snapshot = firestore.collection("orders")
                        .orderBy("orderId", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()
                    val doc = snapshot.documents.firstOrNull()
                    val oidFromField = doc?.getLong("orderId")?.toInt()
                    val oidFromDocId = doc?.id?.toIntOrNull()
                    oidFromField ?: oidFromDocId ?: 0
                } ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun saveOrderToFirestore(order: Order): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(2500L) {
                    val firestore = db ?: return@withTimeoutOrNull false
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
                        "senderNumber" to order.senderNumber,
                        "transactionId" to order.transactionId,
                        "paidAmount" to order.paidAmount,
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
                        "isProfitReleased" to order.isProfitReleased,
                        "adminRole" to order.adminRole,
                        "adminPhone" to order.adminPhone
                    )
                    firestore.collection("orders").document(order.orderId.toString()).set(orderMap, SetOptions.merge()).await()
                    true
                } ?: false
            }
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
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(2000L) {
                    val firestore = db ?: return@withTimeoutOrNull false
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
                } ?: false
            }
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
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(2500L) {
                    val firestore = db ?: return@withTimeoutOrNull false
                    firestore.collection("settings").document("app_notice").set(mapOf("notice" to notice, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
                    true
                } ?: false
            }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving app notice: ${e.message}")
            false
        }
    }

    suspend fun getAppNoticeFromFirestore(): String? {
        return try {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(2500L) {
                    val firestore = db ?: return@withTimeoutOrNull null
                    val doc = firestore.collection("settings").document("app_notice").get().await()
                    doc.getString("notice")
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error getting app notice: ${e.message}")
            null
        }
    }
}
