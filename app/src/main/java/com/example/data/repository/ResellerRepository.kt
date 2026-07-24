package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

class ResellerRepository(private val appDao: AppDao) {

    val allProducts: Flow<List<Product>> = appDao.getAllProducts()
    val allOrders: Flow<List<Order>> = appDao.getAllOrders()
    val wallet: Flow<Wallet?> = appDao.getWalletFlow()
    val allWithdrawals: Flow<List<Withdrawal>> = appDao.getAllWithdrawals()
    val referralInfo: Flow<ReferralInfo?> = appDao.getReferralInfoFlow()
    val allReferredUsers: Flow<List<ReferredUserRecord>> = appDao.getAllReferredUsers()
    val allReferralOrders: Flow<List<ReferralOrderRecord>> = appDao.getAllReferralOrders()
    val banners: Flow<List<Banner>> = appDao.getBanners()
    val allResellers: Flow<List<ResellerUser>> = appDao.getAllResellers()
    val allCategories: Flow<List<CategoryItem>> = appDao.getAllCategories()

    // Support Messages Repository
    fun getSupportMessagesForReseller(phone: String): Flow<List<SupportMessage>> = appDao.getSupportMessagesForReseller(phone)
    val allSupportMessages: Flow<List<SupportMessage>> = appDao.getAllSupportMessages()
    suspend fun sendSupportMessage(message: SupportMessage) = appDao.insertSupportMessage(message)
    suspend fun deleteSupportMessagesForReseller(phone: String) = appDao.deleteSupportMessagesForReseller(phone)

    suspend fun addCategory(category: CategoryItem) = appDao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryItem) = appDao.deleteCategory(category)

    fun getResellerByPhoneFlow(phone: String): Flow<ResellerUser?> = appDao.getResellerByPhoneFlow(phone)
    suspend fun getResellerByPhone(phone: String): ResellerUser? = appDao.getResellerByPhone(phone)
    suspend fun getAllResellersDirectly(): List<ResellerUser> = appDao.getAllResellersDirectly()
    suspend fun addReseller(reseller: ResellerUser) = appDao.insertReseller(reseller)
    suspend fun updateReseller(reseller: ResellerUser) = appDao.updateReseller(reseller)
    suspend fun deleteReseller(reseller: ResellerUser) = appDao.deleteReseller(reseller)

    fun getProductById(id: Int): Flow<Product?> = appDao.getProductById(id)
    fun getOrderById(orderId: Int): Flow<Order?> = appDao.getOrderById(orderId)

    // Check if it's the first order to show notice about advance delivery charge payment
    suspend fun isFirstOrder(): Boolean {
        val orders = allOrders.first().filter { it.orderStatus != "Cancelled" }
        return orders.isEmpty()
    }

    // Add Product (Admin Only)
    suspend fun addProduct(product: Product) {
        appDao.insertProduct(product)
        addNotification(
            title = "🛍️ নতুন প্রোডাক্ট যুক্ত হয়েছে!",
            message = "${product.title} (পাইকারি দাম: ৳${product.wholesalePrice.toInt()})",
            targetRole = "RESELLER",
            type = "PRODUCT",
            relatedId = product.id
        )
    }

    // Update Product (Admin Only)
    suspend fun updateProduct(product: Product) {
        appDao.updateProduct(product)
    }

    // Delete Product (Admin Only)
    suspend fun deleteProduct(product: Product) {
        appDao.deleteProduct(product)
    }

    // Notifications
    val allNotifications: Flow<List<NotificationItem>> = appDao.getAllNotifications()

    suspend fun addNotification(
        title: String,
        message: String,
        targetRole: String = "ALL",
        type: String = "GENERAL",
        relatedId: Int = 0
    ) {
        appDao.insertNotification(
            NotificationItem(
                title = title,
                message = message,
                targetRole = targetRole,
                type = type,
                relatedId = relatedId,
                isRead = false,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun markNotificationAsRead(id: Int) = appDao.markNotificationAsRead(id)
    suspend fun markAllNotificationsAsRead() = appDao.markAllNotificationsAsRead()
    suspend fun deleteNotification(notification: NotificationItem) = appDao.deleteNotification(notification)
    suspend fun clearAllNotifications() = appDao.clearAllNotifications()

    // Place Order (Reseller)
    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        district: String,
        thana: String,
        fullAddress: String,
        deliveryInstructions: String,
        paymentType: String,
        paymentMethod: String,
        totalWholesale: Double,
        totalSelling: Double,
        deliveryCharge: Double,
        productInfo: String = "",
        productImageUrls: String = ""
    ): Long {
        val profit = totalSelling - totalWholesale
        val isFirst = isFirstOrder()
        
        val initialPaymentStatus = if (paymentType == "Full Advance") {
            "Paid"
        } else if (isFirst || paymentType == "Advance Delivery") {
            "Pending Advance verification"
        } else {
            "COD"
        }

        val order = Order(
            customerName = customerName,
            customerPhone = customerPhone,
            district = district,
            thana = thana,
            fullAddress = fullAddress,
            deliveryInstructions = deliveryInstructions,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            paymentStatus = initialPaymentStatus,
            totalWholesalePrice = totalWholesale,
            totalSellingPrice = totalSelling,
            calculatedProfit = profit,
            deliveryCharge = deliveryCharge,
            orderStatus = "Pending",
            productInfo = productInfo,
            productImageUrls = productImageUrls
        )
        val orderId = appDao.insertOrder(order)

        // Insert Referral Order Record
        val referralComm = (totalSelling * 0.015)
        appDao.insertReferralOrder(
            ReferralOrderRecord(
                orderIdStr = "#${orderId}",
                buyerName = customerName,
                buyerPhone = customerPhone,
                orderAmount = totalSelling,
                commissionAmount = referralComm,
                status = "Pending",
                date = System.currentTimeMillis()
            )
        )

        // Notify Admin
        addNotification(
            title = "🛒 নতুন অর্ডার এসেছে! (#${orderId})",
            message = "কাস্টমার: $customerName ($customerPhone) | মোট বিক্রয়: ৳${totalSelling.toInt()} (লাভ: ৳${profit.toInt()})",
            targetRole = "ADMIN",
            type = "ORDER",
            relatedId = orderId.toInt()
        )

        return orderId
    }

    // Update Order Tracking Info (Can be done anytime by Admin)
    suspend fun updateOrderTracking(orderId: Int, trackingNum: String, trackingLinkUrl: String) {
        val order = appDao.getOrderById(orderId).first() ?: return
        val updatedOrder = order.copy(
            trackingNumber = trackingNum.ifEmpty { order.trackingNumber },
            trackingLink = trackingLinkUrl.ifEmpty { order.trackingLink }
        )
        appDao.updateOrder(updatedOrder)

        // Notify Reseller
        val trackingDetails = buildString {
            if (trackingNum.isNotEmpty()) append("ট্র্যাকিং কোড: $trackingNum ")
            if (trackingLinkUrl.isNotEmpty()) append("| ট্র্যাকিং লিংক: $trackingLinkUrl")
        }
        addNotification(
            title = "📍 ট্র্যাকিং আপডেট! (অর্ডার #${order.orderId})",
            message = "আপনার অর্ডার #${order.orderId} এর ট্র্যাকিং লিংক/তথ্য আপডেট করা হয়েছে:\n$trackingDetails",
            targetRole = "RESELLER",
            type = "TRACKING",
            relatedId = order.orderId
        )
    }

    // Release Order Profit to Wallet (Can be triggered 24 hours after delivery or manually)
    suspend fun releaseOrderProfit(orderId: Int) {
        val order = appDao.getOrderById(orderId).first() ?: return
        if (order.isProfitReleased) return

        val currentWallet = appDao.getWallet() ?: Wallet()
        val orderProfit = order.calculatedProfit
        val referralCommission = (order.totalSellingPrice / 100.0) * 1.5

        val newProfitEarned = currentWallet.totalProfit + orderProfit
        val newCommissionEarned = currentWallet.totalCommission + referralCommission
        val finalActiveBalance = currentWallet.activeBalance + orderProfit + referralCommission

        appDao.insertWallet(
            currentWallet.copy(
                totalProfit = newProfitEarned,
                totalCommission = newCommissionEarned,
                activeBalance = finalActiveBalance
            )
        )

        // Update Referral Info
        val currentRef = appDao.getReferralInfo() ?: ReferralInfo()
        appDao.insertReferralInfo(
            currentRef.copy(
                totalEarnings = currentRef.totalEarnings + referralCommission
            )
        )

        // Mark order profit released
        appDao.updateOrder(order.copy(isProfitReleased = true))

        // Notify Reseller
        addNotification(
            title = "💰 প্রফিট একাউন্টে জমা হয়েছে!",
            message = "অর্ডার #${order.orderId} এর লাভ ৳${orderProfit.toInt()} টাকা এবং রেফারাল কমিশন ৳${String.format("%.2f", referralCommission)} আপনার ওয়ালেটে জমা দেওয়া হয়েছে।",
            targetRole = "RESELLER",
            type = "PROFIT",
            relatedId = order.orderId
        )
    }

    // Update Order Status and trigger notifications
    suspend fun updateOrderStatus(orderId: Int, newStatus: String, trackingNum: String = "", trackingLinkUrl: String = "") {
        val orderFlow = appDao.getOrderById(orderId)
        val order = orderFlow.first() ?: return
        
        val oldStatus = order.orderStatus

        val isDeliveredNow = newStatus == "Delivered" && oldStatus != "Delivered"
        val deliveredTime = if (isDeliveredNow) System.currentTimeMillis() else order.deliveredDate

        val isReturnedNow = (newStatus == "Returned" || newStatus == "রিটার্নড") && (oldStatus != "Returned" && oldStatus != "রিটার্নড")
        val isAlreadyPaid = order.paymentType == "Advance Delivery" || order.paymentType == "Full Advance" || order.paymentStatus == "Paid" || order.paymentStatus == "Pending Advance verification"

        val updatedOrder = order.copy(
            orderStatus = newStatus,
            trackingNumber = trackingNum.ifEmpty { order.trackingNumber },
            trackingLink = trackingLinkUrl.ifEmpty { order.trackingLink },
            deliveredDate = deliveredTime
        )
        appDao.updateOrder(updatedOrder)

        // Sync Referral Order status & auto-credit commission on Completed/Delivered
        val refOrders = appDao.getAllReferralOrders().firstOrNull() ?: emptyList()
        val matchingRefOrder = refOrders.find { it.orderIdStr == "#$orderId" }
        if (matchingRefOrder != null) {
            val targetRefStatus = when (newStatus) {
                "Delivered", "Completed" -> "Completed"
                "Cancelled" -> "Cancelled"
                else -> "Pending"
            }
            if (matchingRefOrder.status != targetRefStatus) {
                appDao.updateReferralOrder(matchingRefOrder.copy(status = targetRefStatus))
                if (targetRefStatus == "Completed" && matchingRefOrder.status != "Completed") {
                    val wallet = appDao.getWallet() ?: Wallet()
                    appDao.insertWallet(
                        wallet.copy(
                            activeBalance = wallet.activeBalance + matchingRefOrder.commissionAmount,
                            totalCommission = wallet.totalCommission + matchingRefOrder.commissionAmount
                        )
                    )
                }
            }
        }

        val isCancelledNow = newStatus == "Cancelled" && oldStatus != "Cancelled"
        val refundAmount = if (isCancelledNow && isAlreadyPaid) {
            if (order.paymentType == "Full Advance") {
                order.totalSellingPrice + order.deliveryCharge
            } else {
                order.deliveryCharge
            }
        } else 0.0

        // Handle Return Delivery Charge deduction & Cancel Refund (Requirements)
        val statusMsg = when (newStatus) {
            "Confirmed" -> "✅ আপনার অর্ডার #${order.orderId} এপ্রুভ ও কনফার্ম করা হয়েছে!"
            "Processing" -> "⚙️ আপনার অর্ডার #${order.orderId} প্রসেসিং এ রাখা হয়েছে।"
            "Shipped" -> "🚚 আপনার অর্ডার #${order.orderId} শিপমেন্টে পাঠানো হয়েছে!"
            "Delivered" -> "🎉 আপনার অর্ডার #${order.orderId} কাস্টমারের নিকট সফলভাবে ডেলিভারি করা হয়েছে! (২৪ ঘণ্টা পর লাভ ব্যালেন্স যুক্ত হবে)"
            "Cancelled" -> {
                if (refundAmount > 0) {
                    "❌ আপনার অর্ডার #${order.orderId} বাতিল করা হয়েছে। অগ্রিম পরিশোধিত ৳${refundAmount.toInt()} টাকা আপনার ওয়ালেট ব্যালেন্সে ফেরত (রিফান্ড) দেওয়া হয়েছে।"
                } else {
                    "❌ আপনার অর্ডার #${order.orderId} বাতিল করা হয়েছে।"
                }
            }
            "Returned", "রিটার্নড" -> {
                if (isAlreadyPaid) {
                    "❌ আপনার অর্ডার #${order.orderId} রিটার্ন এসেছে। (অগ্রিম ডেলিভারি চার্জ/টাকা পরিশোধিত থাকায় ওয়ালেট থেকে কোনো অতিরিক্ত টাকা কাটা হয়নি)"
                } else {
                    val currentWallet = appDao.getWallet() ?: Wallet()
                    val newBal = currentWallet.activeBalance - order.deliveryCharge
                    if (currentWallet.activeBalance < order.deliveryCharge) {
                        "❌ আপনার অর্ডার #${order.orderId} রিটার্ন এসেছে। অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স না থাকায় একাউন্টে মাইনাস ৳${(-newBal).toInt()} টাকা কাটা হয়েছে। পরবর্তীতে একাউন্টে টাকা আসলে কেটে নেওয়া হবে।"
                    } else {
                        "❌ আপনার অর্ডার #${order.orderId} রিটার্ন এসেছে। ডেলিভারি চার্জ ৳${order.deliveryCharge.toInt()} টাকা ওয়ালেট থেকে এডমিন ওয়ালেটে কর্তন করা হয়েছে।"
                    }
                }
            }
            else -> "আপনার অর্ডার #${order.orderId} এর স্ট্যাটাস আপডেট করা হয়েছে: $newStatus"
        }

        addNotification(
            title = "অর্ডার আপডেট: $newStatus",
            message = statusMsg,
            targetRole = "RESELLER",
            type = "ORDER",
            relatedId = order.orderId
        )

        // Process cancellation refund and notify Admin
        if (isCancelledNow) {
            val elapsedMillis = System.currentTimeMillis() - order.date
            val isWithinOneHour = elapsedMillis in 0..3600000L
            val cancelTimingMsg = if (isWithinOneHour) {
                val mins = (elapsedMillis / 60000L).coerceAtLeast(1)
                " (অর্ডার দেওয়ার $mins মিনিটের মধ্যে বাতিল করা হয়েছে)"
            } else ""

            addNotification(
                title = if (isWithinOneHour) "⚠️ রিসেলার ১ ঘণ্টার মধ্যে অর্ডার বাতিল করেছেন! (#${order.orderId})" else "⚠️ রিসেলার অর্ডার বাতিল করেছেন! (#${order.orderId})",
                message = "অর্ডার #${order.orderId} রিসেলার বাতিল করেছেন$cancelTimingMsg। কাস্টমার: ${order.customerName} (${order.customerPhone})",
                targetRole = "ADMIN",
                type = "ORDER_CANCELLED",
                relatedId = order.orderId
            )

            if (refundAmount > 0) {
                val currentWallet = appDao.getWallet() ?: Wallet()
                val newBalance = currentWallet.activeBalance + refundAmount
                appDao.insertWallet(currentWallet.copy(activeBalance = newBalance))
            }
        }

        if (isReturnedNow && !isAlreadyPaid) {
            val currentWallet = appDao.getWallet() ?: Wallet()
            val newBalance = currentWallet.activeBalance - order.deliveryCharge
            appDao.insertWallet(currentWallet.copy(activeBalance = newBalance))
        }

        // If tracking info was provided during status update, also trigger tracking update
        if (trackingNum.isNotEmpty() || trackingLinkUrl.isNotEmpty()) {
            addNotification(
                title = "📍 ট্র্যাকিং লিংক আপডেট! (অর্ডার #${order.orderId})",
                message = "অর্ডার #${order.orderId} ট্র্যাকিং নম্বর: ${trackingNum.ifEmpty { "N/A" }} | লিংক: ${trackingLinkUrl.ifEmpty { "N/A" }}",
                targetRole = "RESELLER",
                type = "TRACKING",
                relatedId = order.orderId
            )
        }

        // If instantly delivered, auto release profit if delivery is past 24 hrs, or immediately if test mode
        if (isDeliveredNow) {
            // Auto release profit
            releaseOrderProfit(order.orderId)
        }
    }

    // Update Payment Status (Admin Only)
    suspend fun updateOrderPaymentStatus(orderId: Int, paymentStatus: String) {
        val order = appDao.getOrderById(orderId).first() ?: return
        appDao.updateOrder(order.copy(paymentStatus = paymentStatus))
    }

    // Submit Withdrawal Request (Reseller)
    suspend fun requestWithdrawal(amount: Double, charge: Double, method: String, number: String): Boolean {
        val currentWallet = appDao.getWallet() ?: Wallet()
        if (amount < 50.0 || amount > 25000.0) return false // Limit 50 to 25,000
        if (currentWallet.activeBalance < amount) return false // Insufficient funds

        val updatedWallet = currentWallet.copy(
            activeBalance = currentWallet.activeBalance - amount
        )
        appDao.insertWallet(updatedWallet)

        val withdrawal = Withdrawal(
            amount = amount,
            charge = charge,
            paymentMethod = method,
            accountNumber = number,
            status = "Pending"
        )
        appDao.insertWithdrawal(withdrawal)

        val netAmount = (amount - charge).coerceAtLeast(0.0)
        // Notify Admin
        addNotification(
            title = "💸 নতুন উইথড্র রিকোয়েস্ট!",
            message = "রিসেলার ৳${amount.toInt()} টাকা ($method: $number) উইথড্র চেয়েছে। (চার্জ: ৳${charge.toInt()}, প্রদেয় নিট টাকা: ৳${netAmount.toInt()})",
            targetRole = "ADMIN",
            type = "WITHDRAWAL"
        )

        return true
    }

    // Approve/Reject Withdrawal Request (Admin Only)
    suspend fun updateWithdrawalStatus(id: Int, approve: Boolean) {
        val withdrawals = appDao.getAllWithdrawals().first()
        val withdrawal = withdrawals.find { it.id == id } ?: return
        if (withdrawal.status != "Pending") return

        val newStatus = if (approve) "Approved" else "Rejected"
        val updatedWithdrawal = withdrawal.copy(status = newStatus)
        appDao.updateWithdrawal(updatedWithdrawal)

        if (!approve) {
            // Refund reseller wallet balance if rejected
            val currentWallet = appDao.getWallet() ?: Wallet()
            appDao.insertWallet(
                currentWallet.copy(
                    activeBalance = currentWallet.activeBalance + withdrawal.amount
                )
            )
            // Notify Reseller
            addNotification(
                title = "❌ উইথড্র বাতিল করা হয়েছে",
                message = "আপনার ৳${withdrawal.amount.toInt()} টাকা উইথড্র রিকোয়েস্ট বাতিল করা হয়েছে এবং টাকা ওয়ালেটে ফেরত দেওয়া হয়েছে।",
                targetRole = "RESELLER",
                type = "WITHDRAWAL"
            )
        } else {
            // Update total withdrawn on approval
            val currentWallet = appDao.getWallet() ?: Wallet()
            appDao.insertWallet(
                currentWallet.copy(
                    totalWithdrawn = currentWallet.totalWithdrawn + withdrawal.amount
                )
            )
            // Notify Reseller
            addNotification(
                title = "✅ উইথড্র সফল হয়েছে!",
                message = "আপনার ৳${withdrawal.amount.toInt()} টাকা ($withdrawal.paymentMethod: $withdrawal.accountNumber) উইথড্র সফলভাবে পেমেন্ট করা হয়েছে।",
                targetRole = "RESELLER",
                type = "WITHDRAWAL"
            )
        }
    }

    // Populate Initial Mock Data to guarantee a ready-to-test store on first launch
    suspend fun populateInitialDataIfNeeded() {
        val categories = appDao.getAllCategories().first()
        if (categories.isEmpty()) {
            val defaultCats = listOf(
                CategoryItem(name = "ছেলেদের পোশাক", icon = "👔", subcategories = "ওয়ার্ল্ড কাপ, পলো শার্ট, ড্রপসোল্ডার টিশার্ট, বেসিক টিশার্ট, লং স্লীভ টিশার্ট, প্রিন্ট শার্ট, সলিড শার্ট, চেক শার্ট, শার্ট কম্বো, টি শার্ট কম্বো, হাফ স্লিভ সেট, লং স্লিভ সেট, এমব্রো পাঞ্জাবি, প্রিন্ট পাঞ্জাবি, পাঞ্জাবি কম্বো, প্যান্ট+ট্রাউজার"),
                CategoryItem(name = "মেয়েদের পোশাক", icon = "👗", subcategories = "রেডিমেড থ্রিপিস, আনস্টিজ থ্রিপিস, গাউন & কুর্তি, লেহেঙ্গা & পার্টি, ওয়েস্টান ড্রেস, টিশার্ট & স্কার্ট, ইনার & নাইটি, শাড়ি, হ্যান্ডপ্রিন্ট শাড়ি, ইন্ডিয়ান শাড়ি, তাঁতের শাড়ি, বোরকা, হিজাব & নিকাব, সুন্নাতি ড্রেস"),
                CategoryItem(name = "বেবি কালেকশন", icon = "👶", subcategories = "বয়েজ টিশার্ট সেট, বেবি শার্ট, বেবি পাঞ্জাবি, খেলনা & দোলনা, গার্লস টিশার্ট সেট, বেবি কামিজ, পরী ড্রেস, বেবি বোরকা"),
                CategoryItem(name = "কাপল এন্ড কম্বো", icon = "👩‍❤️‍👨", subcategories = "কাপল শাড়ি, কাপল থ্রিপিস, শাড়ি কম্বো, কাপল ঘড়ি, ঘড়ি কম্বো, গিফট আইটেম, মিস্ট্রি বক্স"),
                CategoryItem(name = "গৃহ সামগ্রী", icon = "🏠", subcategories = "বেডশীট, ডাইনিং শিট, পর্দা, গৃহ সজ্জা"),
                CategoryItem(name = "ব্যাগ কালেকশন", icon = "👜", subcategories = "পার্স ব্যাগ, মেয়েদের ব্যাগ, ছেলেদের ব্যাগ, বেবি ব্যাগ, ক্যারি ব্যাগ"),
                CategoryItem(name = "জুয়েলারি এন্ড এক্সেসরিজ", icon = "💍", subcategories = "ক্লিপ & ব্যান্ড, এক্সেসরিজ, বিউটি কেয়ার, ন্যাচারাল কেয়ার"),
                CategoryItem(name = "ইলেকট্রনিক এবং গ্যাজেট", icon = "🎧", subcategories = "ফ্যান, গ্যাজেটস, ঘড়ি, হেডফোন, স্মার্ট-ওয়াচ, পাওয়ার-ব্যাংক, ক্যামেরা, স্পিকার"),
                CategoryItem(name = "শীতের কালেকশন", icon = "🧥", subcategories = "জেন্টস হুডি, জেন্টস জ্যাকেট, হুডি সেট, সুয়েটার, লেডিস হুডি, লেডিস জ্যাকেট, লেডিস ওভারকোট, জুতা, বেবি উইন্টার ড্রেসসমূহ, লেডিস উইন্টার এক্সেসরিজ, বেবি উইন্টার এক্সেসরিজ, জেন্টস উইন্টার এক্সেসরিজ"),
                CategoryItem(name = "সিজোনাল প্রোডাক্ট", icon = "☔", subcategories = "ছাতা, রেইন কোট, কসাই টি শার্ট"),
                CategoryItem(name = "অন্যান্য ক্যাটাগরি", icon = "📦", subcategories = "হোম কেয়ার, পার্সোনাল কেয়ার, টয়স & স্পোর্টস")
            )
            defaultCats.forEach { appDao.insertCategory(it) }
        }

        val products = appDao.getAllProducts().first()
        if (products.isEmpty()) {
            // Set up Banners
            appDao.insertBanner(
                Banner(
                    imageUrl = "android.resource://com.example/" + com.example.R.drawable.reseller_bd_banner_1784804734464,
                    title = "রিসেলার বিডি অফিশিয়াল ব্যানার",
                    targetCategory = "All"
                )
            )

            // Set up Products
            appDao.insertProduct(
                Product(
                    title = "Premium Mens Cotton Panjabi",
                    description = "ঈদের সেরা কালেকশন! ১০০% সুতি কাপড়ে তৈরি প্রিমিয়াম কোয়ালিটি পাঞ্জাবি। চমৎকার কালার গ্যারান্টি এবং আরামদায়ক ফিটিং।\n\nসাইজ চার্ট:\nM (৪০), L (৪২), XL (৪৪), XXL (৪৬)\nঅরিজিনাল ছবি ও ভিডিও দেওয়া আছে। যেকোনো সোশাল মিডিয়ায় সহজে বিক্রি করতে পারেন।",
                    imageUrl = "https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&w=600&q=80",
                    videoUrl = "",
                    skuCode = "PANJ-C70",
                    wholesalePrice = 750.0,
                    sizes = "M, L, XL, XXL",
                    colors = "Maroon, Navy Blue, Olive",
                    videoReviewUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    category = "ছেলেদের পোশাক",
                    subcategory = "এমব্রো পাঞ্জাবি"
                )
            )

            appDao.insertProduct(
                Product(
                    title = "Exclusive Georgette Salwar Kameez",
                    description = "গর্জিয়াস পার্টি ওয়ার থ্রি-পিস। হাই কোয়ালিটি জর্জেট ফেব্রিক্সের সাথে আকর্ষণীয় এমব্রয়ডারি কাজ। কাস্টমারদের প্রথম পছন্দ।\n\nবডি সাইজ: ৩৬-৪৪ (ফ্রি সাইজ বা স্টিচ করা যায়)\nখুবই ট্রেন্ডিং আইটেম, ফেসবুকে বুস্ট করার জন্য দারুণ প্রোডাক্ট।",
                    imageUrl = "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=600&q=80",
                    videoUrl = "",
                    skuCode = "GEOR-K22",
                    wholesalePrice = 1250.0,
                    sizes = "Free Size, XL, XXL",
                    colors = "Lavender, Peach, Cyan",
                    videoReviewUrl = "",
                    category = "মেয়েদের পোশাক",
                    subcategory = "রেডিমেড থ্রিপিস"
                )
            )

            appDao.insertProduct(
                Product(
                    title = "FitPro T900 Ultra Smartwatch",
                    description = "Bluetooth Calling, Heart Rate, SpO2, Sports Modes এবং Wireless Charging সাপোর্ট সম্বলিত সেরা স্মার্ট ওয়াচ। ২ ইঞ্চি ফুল টাচ ডিসপ্লে।\n\nগরম গরম বিক্রি হওয়া এই গ্যাজেট নিয়ে কাজ করে প্রতিদিন ভালো প্রফিট করুন।\nডেলিভারি চার্জ অগ্রিম নিয়ে অর্ডার কনফার্ম করুন।",
                    imageUrl = "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=600&q=80",
                    videoUrl = "",
                    skuCode = "SMWT-T900",
                    wholesalePrice = 900.0,
                    sizes = "Standard",
                    colors = "Orange, Black, Grey",
                    videoReviewUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                    category = "ইলেকট্রনিক এবং গ্যাজেট",
                    subcategory = "স্মার্ট-ওয়াচ"
                )
            )

            appDao.insertProduct(
                Product(
                    title = "Mens Genuine Leather Wallet",
                    description = "১০০% খাঁটি চামড়ার প্রিমিয়াম ওয়ালেট। অত্যন্ত টেকসই এবং মাল্টি-পকেট ডিজাইন। বক্সে আকর্ষণীয় প্যাকেজিং। উপহারের জন্য সেরা।\n\nহোলসেল প্রাইস ৩৫০ টাকা, আপনি অনায়াসে ৬০০+ টাকায় ফেসবুকে বিক্রি করতে পারবেন। লাভ নিশ্চিত!",
                    imageUrl = "https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=600&q=80",
                    videoUrl = "",
                    skuCode = "LEAT-W35",
                    wholesalePrice = 350.0,
                    sizes = "Standard",
                    colors = "Brown, Black, Dark Tan",
                    videoReviewUrl = "",
                    category = "ছেলেদের পোশাক",
                    subcategory = "প্যান্ট+ট্রাউজার"
                )
            )

            // Setup Initial Wallet
            appDao.insertWallet(
                Wallet(
                    id = 1,
                    totalProfit = 0.0,
                    totalCommission = 0.0,
                    totalWithdrawn = 0.0,
                    activeBalance = 120.0 // Give them some initial active balance to show off withdrawal functionality!
                )
            )

            // Setup Initial Referral Code
            appDao.insertReferralInfo(
                ReferralInfo(
                    id = 1,
                    referralCode = "BDRES99",
                    totalInvited = 25,
                    totalEarnings = 1160.0
                )
            )

            // Seed initial mock resellers
            val now = System.currentTimeMillis()
            appDao.insertReseller(ResellerUser("01711111111", "Rashed Ahmed", "rashed@gmail.com", isBlocked = false, lastActive = now - 5 * 60 * 1000)) // 5 mins ago
            appDao.insertReseller(ResellerUser("01822222222", "Mominur Rahman", "momin@gmail.com", isBlocked = true, lastActive = now - 2 * 60 * 60 * 1000)) // 2 hours ago
            appDao.insertReseller(ResellerUser("01933333333", "Ayesha Siddiqua", "ayesha@gmail.com", isBlocked = false, lastActive = now - 3L * 24 * 60 * 60 * 1000)) // 3 days ago

            // Seed Referred Users (25 total, 3 registered today)
            val dayMs = 24 * 60 * 60 * 1000L
            appDao.insertReferredUser(ReferredUserRecord(userPhone = "01788112233", userName = "Tanvir Hasan", registeredDate = now - 1000 * 60 * 30)) // Today
            appDao.insertReferredUser(ReferredUserRecord(userPhone = "01899223344", userName = "Nusrat Jahan", registeredDate = now - 1000 * 60 * 120)) // Today
            appDao.insertReferredUser(ReferredUserRecord(userPhone = "01677334455", userName = "Sabbir Hossain", registeredDate = now - 1000 * 60 * 240)) // Today
            for (i in 4..25) {
                appDao.insertReferredUser(ReferredUserRecord(userPhone = "017000000$i", userName = "Referred User $i", registeredDate = now - (i * dayMs)))
            }

            // Seed 10 Recent Referral Orders
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1058", buyerName = "Tanvir Hasan", buyerPhone = "01788112233", orderAmount = 1000.0, commissionAmount = 15.0, status = "Completed", date = now - 1000 * 60 * 15))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1057", buyerName = "Nusrat Jahan", buyerPhone = "01899223344", orderAmount = 2000.0, commissionAmount = 30.0, status = "Completed", date = now - 1000 * 60 * 90))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1056", buyerName = "Sabbir Hossain", buyerPhone = "01677334455", orderAmount = 800.0, commissionAmount = 12.0, status = "Pending", date = now - 1000 * 60 * 180))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1055", buyerName = "Tanvir Hasan", buyerPhone = "01788112233", orderAmount = 1500.0, commissionAmount = 22.5, status = "Completed", date = now - 1000 * 60 * 300))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1054", buyerName = "Mahmudul Haq", buyerPhone = "0170000004", orderAmount = 1200.0, commissionAmount = 18.0, status = "Completed", date = now - 1000 * 60 * 420))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1053", buyerName = "Sadia Islam", buyerPhone = "0170000005", orderAmount = 1100.0, commissionAmount = 16.5, status = "Completed", date = now - 1000 * 60 * 540))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1052", buyerName = "Riyad Khan", buyerPhone = "0170000006", orderAmount = 600.0, commissionAmount = 9.0, status = "Cancelled", date = now - dayMs))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1051", buyerName = "Farhana Akter", buyerPhone = "0170000007", orderAmount = 1400.0, commissionAmount = 21.0, status = "Completed", date = now - dayMs - 1000 * 60 * 60))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1050", buyerName = "Kamrul Islam", buyerPhone = "0170000008", orderAmount = 900.0, commissionAmount = 13.5, status = "Pending", date = now - dayMs - 1000 * 60 * 180))
            appDao.insertReferralOrder(ReferralOrderRecord(orderIdStr = "#1049", buyerName = "Monir Hossain", buyerPhone = "0170000009", orderAmount = 1800.0, commissionAmount = 27.0, status = "Completed", date = now - 2 * dayMs))

            // Seed Initial Tutorial Videos
            appDao.insertTutorialVideo(
                TutorialVideo(
                    title = "কিভাবে রিসেলার একাউন্ট খুলবেন ও প্রোফাইল সেটআপ করবেন",
                    description = "রিসেলার বিডি অ্যাপে রেজিস্ট্রেশন করার সম্পূর্ণ নির্দেশিকা এবং প্রথম টিউটোরিয়াল।",
                    thumbnailUrl = "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=600&auto=format&fit=crop",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    createdAt = now - 1000 * 60 * 30
                )
            )
            appDao.insertTutorialVideo(
                TutorialVideo(
                    title = "কিভাবে কাস্টমারের ঠিকানা দিয়ে সঠিক নিয়ম অনুযায়ী অর্ডার সাবমিট করবেন",
                    description = "অর্ডার প্রসেস, প্রোডাক্ট কাস্টম প্রফেশনাল প্রাইসিং সেটআপ এবং এডভান্স পেমেন্ট সিস্টেমের ভিডিও গাইড।",
                    thumbnailUrl = "https://images.unsplash.com/photo-1556742049-0a670f4a4591?w=600&auto=format&fit=crop",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    createdAt = now - 1000 * 60 * 60
                )
            )
            appDao.insertTutorialVideo(
                TutorialVideo(
                    title = "রেফারেল লিংক ও সেলার কোড দিয়ে আনলিমিটেড ইনকাম করার সম্পূর্ণ টিউটোরিয়াল",
                    description = "আপনার ইউনিক সেলার কোড শেয়ার করে প্রতি অর্ডারে ১.৫% কমিশন অর্জনের সিক্রেট পদ্ধতি।",
                    thumbnailUrl = "https://images.unsplash.com/photo-1553729459-efe14ef6055d?w=600&auto=format&fit=crop",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    createdAt = now - 1000 * 60 * 120
                )
            )
        }
    }

    suspend fun getReferralInfoDirectly(): ReferralInfo? {
        return appDao.getReferralInfo()
    }

    suspend fun updateReferralInfo(info: ReferralInfo) {
        appDao.insertReferralInfo(info)
    }

    suspend fun updateWalletDirectly(wallet: Wallet) {
        appDao.insertWallet(wallet)
    }

    val allTutorialVideos: Flow<List<TutorialVideo>> = appDao.getAllTutorialVideos()

    suspend fun addTutorialVideo(video: TutorialVideo): Long = appDao.insertTutorialVideo(video)
    suspend fun updateTutorialVideo(video: TutorialVideo) = appDao.updateTutorialVideo(video)
    suspend fun deleteTutorialVideo(video: TutorialVideo) = appDao.deleteTutorialVideo(video)

    val allCustomSocialChannels: Flow<List<CustomSocialChannel>> = appDao.getAllCustomSocialChannels()

    suspend fun addCustomSocialChannel(title: String, url: String, platformType: String = "AUTO") {
        appDao.insertCustomSocialChannel(CustomSocialChannel(title = title, url = url, platformType = platformType))
    }

    suspend fun deleteCustomSocialChannel(channel: CustomSocialChannel) {
        appDao.deleteCustomSocialChannel(channel)
    }

    suspend fun updateActiveBanner(imageUrl: String, title: String = "Reseller BD Banner") {
        appDao.deleteAllBanners()
        appDao.insertBanner(Banner(imageUrl = imageUrl, title = title, targetCategory = "All"))
    }

    suspend fun deleteBanner(banner: Banner) {
        appDao.deleteBanner(banner)
    }
}

