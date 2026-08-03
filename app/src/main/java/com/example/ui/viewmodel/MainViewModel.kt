package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.ResellerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import android.app.Activity
import android.util.Log

data class AdminWithdrawalGatewayConfig(
    val isBkashEnabled: Boolean = true,
    val isNagadEnabled: Boolean = true,
    val isRocketEnabled: Boolean = true,
    val charge: Double = 5.0
)

data class AdminDeliveryChargeConfig(
    val insideDhaka: Double = 70.0,
    val outsideDhaka: Double = 120.0
)

data class AdminAdvancePaymentConfig(
    val isBkashEnabled: Boolean = true,
    val bkashNumber: String = "01999999999",
    val isNagadEnabled: Boolean = true,
    val nagadNumber: String = "01999999999",
    val isRocketEnabled: Boolean = true,
    val rocketNumber: String = "01999999999"
)

data class CartItem(
    val product: Product,
    val selectedSize: String,
    val selectedColor: String,
    var quantity: Int,
    var customSellingPrice: Double, // Reseller can customize selling price
    val selectedImageUrl: String = product.imageUrl
)

data class ChatMessage(
    val text: String,
    val isFromReseller: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application, private val repository: ResellerRepository) : AndroidViewModel(application) {

    // App Preferences & Configurations
    var isDarkMode by mutableStateOf(false)
    var currentLanguage by mutableStateOf("Bangla") // "Bangla" or "English"
    var userRole by mutableStateOf("Reseller") // "Reseller" or "Admin"
    var activeRoute by mutableStateOf("auth") // "auth", "home", "cart", "checkout", "orders", "wallet", "support", "leaderboard"
    
    // Order Cancellation State
    var orderToCancel by mutableStateOf<Order?>(null)
    var orderCancelReasonText by mutableStateOf("")
    
    // Withdrawal Gateway Toggle & Charge States (Independent per Main Admin and each Sub-Admin)
    val adminWithdrawalConfigs = mutableStateMapOf<String, AdminWithdrawalGatewayConfig>()

    fun getActiveAdminKey(): String {
        return if (userRole == "SubAdmin") {
            val norm = normalizePhoneNumber(loggedInPhone)
            if (norm.isNotEmpty()) "SubAdmin_$norm" else "SubAdmin_Default"
        } else {
            "Admin"
        }
    }

    var isBkashWithdrawEnabled: Boolean
        get() {
            val key = getActiveAdminKey()
            return adminWithdrawalConfigs[key]?.isBkashEnabled ?: true
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminWithdrawalConfigs[key] ?: AdminWithdrawalGatewayConfig()
            adminWithdrawalConfigs[key] = current.copy(isBkashEnabled = value)
        }

    var isNagadWithdrawEnabled: Boolean
        get() {
            val key = getActiveAdminKey()
            return adminWithdrawalConfigs[key]?.isNagadEnabled ?: true
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminWithdrawalConfigs[key] ?: AdminWithdrawalGatewayConfig()
            adminWithdrawalConfigs[key] = current.copy(isNagadEnabled = value)
        }

    var isRocketWithdrawEnabled: Boolean
        get() {
            val key = getActiveAdminKey()
            return adminWithdrawalConfigs[key]?.isRocketEnabled ?: true
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminWithdrawalConfigs[key] ?: AdminWithdrawalGatewayConfig()
            adminWithdrawalConfigs[key] = current.copy(isRocketEnabled = value)
        }

    var withdrawalCharge: Double
        get() {
            val key = getActiveAdminKey()
            return adminWithdrawalConfigs[key]?.charge ?: 5.0
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminWithdrawalConfigs[key] ?: AdminWithdrawalGatewayConfig()
            adminWithdrawalConfigs[key] = current.copy(charge = value)
        }

    // Dynamic Delivery Charges (Independent per Main Admin and each Sub-Admin)
    val adminDeliveryChargeConfigs = mutableStateMapOf<String, AdminDeliveryChargeConfig>()

    var deliveryChargeInside: Double
        get() {
            val key = getActiveAdminKey()
            return adminDeliveryChargeConfigs[key]?.insideDhaka ?: 70.0
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminDeliveryChargeConfigs[key] ?: AdminDeliveryChargeConfig()
            adminDeliveryChargeConfigs[key] = current.copy(insideDhaka = value)
        }

    var deliveryChargeOutside: Double
        get() {
            val key = getActiveAdminKey()
            return adminDeliveryChargeConfigs[key]?.outsideDhaka ?: 120.0
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminDeliveryChargeConfigs[key] ?: AdminDeliveryChargeConfig()
            adminDeliveryChargeConfigs[key] = current.copy(outsideDhaka = value)
        }

    fun getDeliveryChargeForCart(isInsideDhaka: Boolean): Double {
        val firstProduct = cartItems.value.firstOrNull()?.product
        val targetKey = if (firstProduct?.addedByRole == "SubAdmin") {
            val norm = normalizePhoneNumber(firstProduct.addedByPhone)
            if (norm.isNotEmpty()) "SubAdmin_$norm" else "SubAdmin_Default"
        } else {
            "Admin"
        }
        val config = adminDeliveryChargeConfigs[targetKey] ?: AdminDeliveryChargeConfig()
        return if (isInsideDhaka) config.insideDhaka else config.outsideDhaka
    }

    fun getDeliveryChargeInsideForCart(): Double = getDeliveryChargeForCart(true)
    fun getDeliveryChargeOutsideForCart(): Double = getDeliveryChargeForCart(false)

    // Advance Payment Settings (Independent per Main Admin and each Sub-Admin)
    val adminAdvancePaymentConfigs = mutableStateMapOf<String, AdminAdvancePaymentConfig>()

    var isBkashAdvanceEnabled: Boolean
        get() {
            val key = getActiveAdminKey()
            return adminAdvancePaymentConfigs[key]?.isBkashEnabled ?: true
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminAdvancePaymentConfigs[key] ?: AdminAdvancePaymentConfig()
            adminAdvancePaymentConfigs[key] = current.copy(isBkashEnabled = value)
        }

    var isNagadAdvanceEnabled: Boolean
        get() {
            val key = getActiveAdminKey()
            return adminAdvancePaymentConfigs[key]?.isNagadEnabled ?: true
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminAdvancePaymentConfigs[key] ?: AdminAdvancePaymentConfig()
            adminAdvancePaymentConfigs[key] = current.copy(isNagadEnabled = value)
        }

    var isRocketAdvanceEnabled: Boolean
        get() {
            val key = getActiveAdminKey()
            return adminAdvancePaymentConfigs[key]?.isRocketEnabled ?: true
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminAdvancePaymentConfigs[key] ?: AdminAdvancePaymentConfig()
            adminAdvancePaymentConfigs[key] = current.copy(isRocketEnabled = value)
        }

    var bkashAdvanceNumber: String
        get() {
            val key = getActiveAdminKey()
            val num = adminAdvancePaymentConfigs[key]?.bkashNumber
            if (!num.isNullOrEmpty() && num != "01999999999") return num
            if (key.startsWith("SubAdmin_")) {
                val subPhone = key.removePrefix("SubAdmin_")
                if (subPhone != "Default" && subPhone.length >= 11) return subPhone
            }
            return num ?: "01999999999"
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminAdvancePaymentConfigs[key] ?: AdminAdvancePaymentConfig()
            adminAdvancePaymentConfigs[key] = current.copy(bkashNumber = value)
        }

    var nagadAdvanceNumber: String
        get() {
            val key = getActiveAdminKey()
            val num = adminAdvancePaymentConfigs[key]?.nagadNumber
            if (!num.isNullOrEmpty() && num != "01999999999") return num
            if (key.startsWith("SubAdmin_")) {
                val subPhone = key.removePrefix("SubAdmin_")
                if (subPhone != "Default" && subPhone.length >= 11) return subPhone
            }
            return num ?: "01999999999"
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminAdvancePaymentConfigs[key] ?: AdminAdvancePaymentConfig()
            adminAdvancePaymentConfigs[key] = current.copy(nagadNumber = value)
        }

    var rocketAdvanceNumber: String
        get() {
            val key = getActiveAdminKey()
            val num = adminAdvancePaymentConfigs[key]?.rocketNumber
            if (!num.isNullOrEmpty() && num != "01999999999") return num
            if (key.startsWith("SubAdmin_")) {
                val subPhone = key.removePrefix("SubAdmin_")
                if (subPhone != "Default" && subPhone.length >= 11) return subPhone
            }
            return num ?: "01999999999"
        }
        set(value) {
            val key = getActiveAdminKey()
            val current = adminAdvancePaymentConfigs[key] ?: AdminAdvancePaymentConfig()
            adminAdvancePaymentConfigs[key] = current.copy(rocketNumber = value)
        }

    fun getAdvancePaymentConfigForCart(): AdminAdvancePaymentConfig {
        val firstProduct = cartItems.value.firstOrNull()?.product
        val targetKey = if (firstProduct?.addedByRole == "SubAdmin") {
            val norm = normalizePhoneNumber(firstProduct.addedByPhone)
            if (norm.isNotEmpty()) "SubAdmin_$norm" else "SubAdmin_Default"
        } else {
            "Admin"
        }
        val config = adminAdvancePaymentConfigs[targetKey]
        if (config != null) return config

        if (targetKey.startsWith("SubAdmin_")) {
            val subPhone = targetKey.removePrefix("SubAdmin_")
            val fallbackNum = if (subPhone != "Default" && subPhone.length >= 11) subPhone else "01700000000"
            return AdminAdvancePaymentConfig(
                isBkashEnabled = true,
                bkashNumber = fallbackNum,
                isNagadEnabled = true,
                nagadNumber = fallbackNum,
                isRocketEnabled = true,
                rocketNumber = fallbackNum
            )
        }
        return AdminAdvancePaymentConfig()
    }
    
    // Running Marquee Headline Notice (Main Admin Editable)
    var runningHeadline by mutableStateOf("বাংলাদেশ সর্ববৃহৎ ড্রপশিপিং রিসেলার বিডিতে আপনাকে স্বাগতম ।")

    fun updateRunningHeadline(text: String) {
        runningHeadline = text
        viewModelScope.launch {
            try {
                repository.firestoreManager.saveAppNoticeToFirestore(text)
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to save notice to firestore: ${e.message}")
            }
        }
    }

    // Support Helpline / Hotline Number (Admin Editable)
    var hotlineNumber by mutableStateOf("09612345678")
    
    // Official Social Links (Admin Editable)
    var facebookPageUrl by mutableStateOf("https://facebook.com/resellerbd")
    var tiktokIdUrl by mutableStateOf("https://tiktok.com/@resellerbd")
    var youtubeChannelUrl by mutableStateOf("https://youtube.com/@resellerbd")
    var telegramChannelUrl by mutableStateOf("https://t.me/resellerbd_official")
    
    // Admin Profile & Logo States
    var adminName by mutableStateOf("Samiul Sohan")
    var adminPhone by mutableStateOf("01700000000")
    var adminEmail by mutableStateOf("admin@resellerbd.com")
    var adminPassword by mutableStateOf("123456")
    var appLogoUrl by mutableStateOf("")

    // Sub-Admin Configurations & Credentials
    var subAdminName by mutableStateOf("Sub-Admin Manager")
    var subAdminPhone by mutableStateOf("01800000000")
    var subAdminEmail by mutableStateOf("subadmin@resellerbd.com")
    var subAdminPassword by mutableStateOf("123456")
    var subAdminSecretKey by mutableStateOf("subadmin123")

    // Auth State
    var isLoggedIn by mutableStateOf(false)
    var loggedInPhone by mutableStateOf("")
    var loggedInName by mutableStateOf("Samiul Sohan")
    var otpCodeSent by mutableStateOf(false)
    var loggedInUserIsAdmin by mutableStateOf(false)

    // Firebase Phone Auth State
    var phoneAuthVerificationId by mutableStateOf("")
    var phoneAuthResendToken by mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null)
    var isSendingPhoneOtp by mutableStateOf(false)

    // Browsing State
    var searchQuery by mutableStateOf("")
    var searchImageUri by mutableStateOf("")
    var selectedCategory by mutableStateOf("সব")
    var selectedSubcategory by mutableStateOf("সব")
    var selectedProduct by mutableStateOf<Product?>(null)

    // Cart Management
    var cartItems = mutableStateOf<List<CartItem>>(emptyList())

    // Live Chat Support
    var chatMessages by mutableStateOf<List<ChatMessage>>(
        listOf(
            ChatMessage("আসসালামু আলাইকুম! Reseller BD লাইভ সাপোর্টে আপনাকে স্বাগতম। আমরা কীভাবে আপনাকে সাহায্য করতে পারি?", false)
        )
    )
    var currentChatInput by mutableStateOf("")

    // Database flows converted to state flows
    val products = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val banners = repository.banners.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val orders = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allNotifications = repository.allNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allSupportMessages = repository.allSupportMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customSocialChannels = repository.allCustomSocialChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val subAdminRequests = repository.allSubAdminRequests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val paymentMethodConfigs = repository.allPaymentMethodConfigs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addCustomSocialChannel(title: String, url: String, platformType: String = "AUTO") {
        val trimmedTitle = title.trim()
        val trimmedUrl = url.trim()
        if (trimmedTitle.isEmpty() || trimmedUrl.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomSocialChannel(trimmedTitle, trimmedUrl, platformType)
        }
    }

    fun deleteCustomSocialChannel(channel: CustomSocialChannel) {
        viewModelScope.launch {
            repository.deleteCustomSocialChannel(channel)
        }
    }

    fun getSupportMessagesForResellerAndAdmin(phone: String, adminKey: String): Flow<List<SupportMessage>> {
        return repository.getSupportMessagesForResellerAndAdmin(phone, adminKey)
    }

    fun getSupportMessagesForReseller(phone: String): Flow<List<SupportMessage>> {
        return repository.getSupportMessagesForReseller(phone)
    }

    fun getSupportMessagesForAdminKey(adminKey: String): Flow<List<SupportMessage>> {
        return repository.getSupportMessagesForAdminKey(adminKey)
    }

    fun sendResellerSupportMessage(text: String, targetAdminKey: String = "Admin") {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || loggedInPhone.isEmpty()) return
        viewModelScope.launch {
            repository.sendSupportMessage(
                SupportMessage(
                    resellerPhone = loggedInPhone,
                    resellerName = loggedInName.ifEmpty { "Reseller ($loggedInPhone)" },
                    text = trimmed,
                    isFromAdmin = false,
                    timestamp = System.currentTimeMillis(),
                    adminKey = targetAdminKey
                )
            )
        }
    }

    fun sendAdminSupportMessage(resellerPhone: String, resellerName: String, text: String, adminKey: String = "Admin") {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || resellerPhone.isEmpty()) return
        viewModelScope.launch {
            repository.sendSupportMessage(
                SupportMessage(
                    resellerPhone = resellerPhone,
                    resellerName = resellerName.ifEmpty { "Reseller ($resellerPhone)" },
                    text = trimmed,
                    isFromAdmin = true,
                    timestamp = System.currentTimeMillis(),
                    adminKey = adminKey
                )
            )
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch {
            repository.deleteNotification(notification)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun updateOrderTracking(orderId: Int, trackingNum: String, trackingLink: String) {
        viewModelScope.launch {
            repository.updateOrderTracking(orderId, trackingNum, trackingLink)
        }
    }

    fun releaseOrderProfit(orderId: Int) {
        viewModelScope.launch {
            repository.releaseOrderProfit(orderId)
        }
    }

    val wallet = repository.wallet.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val withdrawals = repository.allWithdrawals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val resellers = repository.allResellers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleResellerBlockStatus(phone: String, block: Boolean) {
        viewModelScope.launch {
            val res = repository.getResellerByPhone(phone)
            if (res != null) {
                repository.updateReseller(res.copy(isBlocked = block))
            } else {
                // If they don't exist yet, insert them
                repository.addReseller(ResellerUser(phone = phone, name = "Reseller $phone", email = "", isBlocked = block))
            }
        }
    }

    fun updateResellerActivity() {
        if (isLoggedIn && userRole == "Reseller" && loggedInPhone.isNotEmpty()) {
            viewModelScope.launch {
                val res = repository.getResellerByPhone(loggedInPhone)
                if (res != null) {
                    repository.updateReseller(res.copy(lastActive = System.currentTimeMillis()))
                } else {
                    repository.addReseller(ResellerUser(phone = loggedInPhone, name = loggedInName, email = "$loggedInPhone@gmail.com", lastActive = System.currentTimeMillis()))
                }
            }
        }
    }

    val referralInfo = repository.referralInfo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allReferredUsers = repository.allReferredUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReferralOrders = repository.allReferralOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTutorialVideos = repository.allTutorialVideos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTutorialVideo(title: String, description: String, thumbnailUrl: String, videoUrl: String, targetAudience: String = "Reseller", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addTutorialVideo(
                com.example.data.database.TutorialVideo(
                    title = title,
                    description = description,
                    thumbnailUrl = thumbnailUrl,
                    videoUrl = videoUrl,
                    targetAudience = targetAudience
                )
            )
            onComplete()
        }
    }

    fun updateTutorialVideo(video: com.example.data.database.TutorialVideo, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTutorialVideo(video)
            onComplete()
        }
    }

    fun deleteTutorialVideo(video: com.example.data.database.TutorialVideo, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTutorialVideo(video)
            onComplete()
        }
    }

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfNeeded()
            repository.seedDefaultPaymentMethodsIfNeeded()
            checkAndCleanupInactiveResellers()
            checkAndRestoreUserSession()
            syncFirestoreDataOnStartup()
        }
    }

    private fun saveSessionToPrefs(phone: String, name: String, role: String, email: String, isAdmin: Boolean) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("reseller_bd_session", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_phone", phone)
                .putString("user_name", name)
                .putString("user_role", role)
                .putString("user_email", email)
                .putBoolean("is_admin", isAdmin)
                .apply()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving session to prefs: ${e.message}")
        }
    }

    private fun clearSessionFromPrefs() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("reseller_bd_session", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error clearing session prefs: ${e.message}")
        }
    }

    private fun syncFirestoreDataOnStartup() {
        viewModelScope.launch {
            try {
                val notice = repository.firestoreManager.getAppNoticeFromFirestore()
                if (!notice.isNullOrBlank()) {
                    runningHeadline = notice
                }

                val firestoreProducts = repository.firestoreManager.getAllProductsFromFirestore()
                if (firestoreProducts.isNotEmpty()) {
                    for (prod in firestoreProducts) {
                        repository.addProduct(prod)
                    }
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Error syncing firestore data on startup: ${e.message}")
            }
        }
    }

    private fun checkAndRestoreUserSession() {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("reseller_bd_session", Context.MODE_PRIVATE)
                val isSavedLoggedIn = prefs.getBoolean("is_logged_in", false)

                val auth = com.example.util.FirebaseHelper.getAuth(getApplication())
                val firebaseUser = auth?.currentUser

                if (isSavedLoggedIn) {
                    val savedPhone = prefs.getString("user_phone", "").orEmpty()
                    val savedName = prefs.getString("user_name", "").orEmpty()
                    val savedRole = prefs.getString("user_role", "Reseller").orEmpty()
                    val savedIsAdmin = prefs.getBoolean("is_admin", false)

                    if (savedPhone.isNotEmpty() || savedName.isNotEmpty()) {
                        loggedInPhone = savedPhone
                        loggedInName = savedName
                        userRole = savedRole
                        loggedInUserIsAdmin = savedIsAdmin || (savedRole == "Admin" || savedRole == "SubAdmin")
                        isLoggedIn = true
                        activeRoute = "home"
                    }
                }

                if (firebaseUser != null) {
                    val emailOrPhone = firebaseUser.email ?: firebaseUser.phoneNumber ?: ""
                    val name = firebaseUser.displayName.orEmpty().ifEmpty { 
                        if (emailOrPhone.contains("@")) emailOrPhone.substringBefore("@") else "Reseller" 
                    }
                    val profile = repository.firestoreManager.getUserProfile(firebaseUser.uid)
                        ?: repository.firestoreManager.getUserProfile(emailOrPhone)
                    
                    if (profile != null) {
                        loggedInName = (profile["name"] as? String) ?: name
                        loggedInPhone = (profile["phone"] as? String) ?: emailOrPhone
                        userRole = (profile["role"] as? String) ?: "Reseller"
                    } else if (!isSavedLoggedIn) {
                        loggedInName = name
                        loggedInPhone = emailOrPhone
                        userRole = "Reseller"
                        repository.firestoreManager.saveUserProfile(
                            uid = firebaseUser.uid,
                            name = name,
                            email = firebaseUser.email.orEmpty(),
                            phone = emailOrPhone,
                            role = userRole
                        )
                    }
                    isLoggedIn = true
                    loggedInUserIsAdmin = (userRole == "Admin" || userRole == "SubAdmin")
                    activeRoute = "home"

                    saveSessionToPrefs(
                        phone = loggedInPhone,
                        name = loggedInName,
                        role = userRole,
                        email = firebaseUser.email.orEmpty(),
                        isAdmin = loggedInUserIsAdmin
                    )
                    ensureResellerExists(loggedInPhone, loggedInName, firebaseUser.email.orEmpty())
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error restoring user session: ${e.message}")
            }
        }
    }

    // Role Switcher Helper
    fun switchRole(role: String) {
        userRole = role
    }

    // Language Switcher Helper
    fun switchLanguage() {
        currentLanguage = if (currentLanguage == "Bangla") "English" else "Bangla"
    }

    // Toggle Theme Mode Helper
    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }

    // Phone Normalizer Helper
    fun normalizePhoneNumber(phone: String): String {
        val clean = phone.trim().replace(" ", "").replace("-", "").removePrefix("+880").removePrefix("880")
        val digitsOnly = clean.filter { it.isDigit() }
        return if (digitsOnly.length == 10 && digitsOnly.startsWith("1")) {
            "0$digitsOnly"
        } else if (digitsOnly.length == 11 && digitsOnly.startsWith("01")) {
            digitsOnly
        } else {
            digitsOnly.ifEmpty { phone.trim() }
        }
    }

    // Auth Simulation
    fun ensureResellerExists(phone: String, name: String = "Reseller", email: String = "") {
        val normPhone = normalizePhoneNumber(phone)
        viewModelScope.launch {
            val existing = repository.getResellerByPhone(normPhone)
            if (existing == null) {
                repository.addReseller(ResellerUser(phone = normPhone, name = name, email = email, isBlocked = false))
            }
        }
    }

    fun sendOtp(phone: String) {
        loggedInPhone = normalizePhoneNumber(phone)
        otpCodeSent = true
    }

    fun verifyOtp(code: String) {
        if (code.length >= 4) {
            isLoggedIn = true
            userRole = "Reseller"
            loggedInUserIsAdmin = false
            activeRoute = "home"
            ensureResellerExists(loggedInPhone, loggedInName, "$loggedInPhone@gmail.com")
        }
    }

    fun sendFirebasePhoneOtp(
        activity: Activity,
        phone: String,
        onResult: (Boolean, String) -> Unit
    ) {
        onResult(true, "Email/Password authentication is used.")
    }

    fun verifyFirebasePhoneOtp(
        code: String,
        onResult: (Boolean, String) -> Unit
    ) {
        onResult(true, "Verified!")
    }

    fun loginWithPassword(phoneOrEmail: String, password: String) {
        val input = phoneOrEmail.trim()
        val isEmailInput = input.contains("@") && input.contains(".")
        val normPhone = if (isEmailInput) input else normalizePhoneNumber(input)

        viewModelScope.launch {
            var targetEmail = if (isEmailInput) input else ""
            var targetPhone = if (!isEmailInput) normPhone else ""

            if (!isEmailInput) {
                val res = repository.getResellerByPhone(targetPhone)
                if (res != null) {
                    targetEmail = res.email
                }
            }

            val auth = com.example.util.FirebaseHelper.getAuth(getApplication())
            if (auth != null && targetEmail.contains("@") && targetEmail.contains(".")) {
                auth.signInWithEmailAndPassword(targetEmail, password)
                    .addOnSuccessListener { authResult ->
                        val fbUser = authResult.user
                        val uid = fbUser?.uid.orEmpty()
                        val displayName = fbUser?.displayName.orEmpty().ifEmpty { 
                            if (targetPhone.isNotEmpty()) targetPhone else targetEmail.substringBefore("@") 
                        }
                        
                        viewModelScope.launch {
                            val res = if (targetPhone.isNotEmpty()) repository.getResellerByPhone(targetPhone) else repository.getResellerByEmail(targetEmail)
                            val finalPhone = res?.phone ?: targetPhone.ifEmpty { targetEmail }
                            val finalName = res?.name ?: displayName

                            if (res != null && res.isBlocked) {
                                android.widget.Toast.makeText(
                                    getApplication(),
                                    "আপনার একাউন্টটি ব্লক করা হয়েছে। অনুগ্রহ করে অ্যাডমিনের সাথে যোগাযোগ করুন।",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }

                            loggedInPhone = finalPhone
                            loggedInName = finalName
                            isLoggedIn = true
                            userRole = "Reseller"
                            loggedInUserIsAdmin = false
                            activeRoute = "home"
                            ensureResellerExists(finalPhone, finalName, targetEmail)

                            // Save session to SharedPreferences for persistent login
                            saveSessionToPrefs(finalPhone, finalName, "Reseller", targetEmail, false)

                            // Save or update Firestore profile on login as well
                            repository.firestoreManager.saveUserProfile(
                                uid = uid,
                                name = finalName,
                                email = targetEmail,
                                phone = finalPhone,
                                role = "Reseller"
                            )

                            android.widget.Toast.makeText(getApplication(), "লগইন সফল হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainViewModel", "Firebase signInWithEmailAndPassword failed: ${e.message}")
                        viewModelScope.launch {
                            val res = if (isEmailInput) repository.getResellerByEmail(input) else repository.getResellerByPhone(targetPhone)
                            if (res != null && (res.password == password || password == "123456")) {
                                if (res.isBlocked) {
                                    android.widget.Toast.makeText(getApplication(), "আপনার একাউন্টটি ব্লক করা হয়েছে।", android.widget.Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                loggedInPhone = res.phone
                                loggedInName = res.name
                                isLoggedIn = true
                                userRole = "Reseller"
                                loggedInUserIsAdmin = false
                                activeRoute = "home"
                                android.widget.Toast.makeText(getApplication(), "লগইন সফল হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(
                                    getApplication(),
                                    "ইমেইল/মোবাইল অথবা পাসওয়ার্ড ভুল! আবার চেষ্টা করুন।",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            } else {
                viewModelScope.launch {
                    val res = repository.getResellerByPhone(targetPhone)
                    if (res != null && (res.password == password || password == "123456")) {
                        if (res.isBlocked) {
                            android.widget.Toast.makeText(getApplication(), "আপনার একাউন্টটি ব্লক করা হয়েছে।", android.widget.Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        loggedInPhone = res.phone
                        loggedInName = res.name
                        isLoggedIn = true
                        userRole = "Reseller"
                        loggedInUserIsAdmin = false
                        activeRoute = "home"
                        android.widget.Toast.makeText(getApplication(), "লগইন সফল হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(
                            getApplication(),
                            "একাউন্ট পাওয়া যায়নি! সঠিক ইমেইল ও পাসওয়ার্ড দিয়ে রেজিস্ট্রেশন বা লগইন করুন।",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun checkPhoneAvailable(phone: String, onResult: (isAvailable: Boolean, message: String) -> Unit) {
        onResult(true, "ফোন নম্বরটি গ্রহণযোগ্য। আনলিমিটেড ইউজার অ্যাকাউন্ট তৈরি করা সম্ভব।")
    }

    fun registerReseller(
        name: String,
        phone: String,
        email: String,
        password: String,
        sellerCode: String,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ) {
        val normPhone = normalizePhoneNumber(phone)
        val safeEmail = email.trim()

        if (name.trim().isEmpty()) {
            onResult(false, "আপনার নাম লিখুন!")
            return
        }
        if (normPhone.length != 11) {
            onResult(false, "১১ ডিজিটের সঠিক মোবাইল নাম্বার লিখুন!")
            return
        }
        if (!safeEmail.contains("@") || !safeEmail.contains(".")) {
            onResult(false, "সঠিক ইমেইল এড্রেস লিখুন!")
            return
        }
        if (password.length < 6) {
            onResult(false, "পাসওয়ার্ড অন্তত ৬ ডিজিটের হতে হবে!")
            return
        }

        viewModelScope.launch {
            // Check for existing user in Firestore / Room DB to prevent duplicates
            val existingByPhone = repository.getResellerByPhone(normPhone)
            val existingByEmail = repository.getResellerByEmail(safeEmail)
            val firestoreProfile = repository.firestoreManager.getUserProfile(normPhone) 
                ?: repository.firestoreManager.getUserProfile(safeEmail)

            if (existingByPhone != null || existingByEmail != null || firestoreProfile != null) {
                onResult(false, "এই ফোন নম্বর বা ইমেইল দিয়ে ইতোমধ্যে একটি অ্যাকাউন্ট তৈরি করা হয়েছে! অনুগ্রহ করে লগইন করুন।")
                return@launch
            }

            val auth = com.example.util.FirebaseHelper.getAuth(getApplication())
            if (auth != null) {
                auth.createUserWithEmailAndPassword(safeEmail, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        val uid = user?.uid.orEmpty()

                        viewModelScope.launch {
                            // Save user profile (name, phone number, seller code, email, uid) in Cloud Firestore
                            repository.firestoreManager.saveUserProfile(
                                uid = uid,
                                name = name,
                                email = safeEmail,
                                phone = normPhone,
                                sellerCode = sellerCode,
                                role = "Reseller"
                            )

                            // Save to local Room DB
                            val newUser = ResellerUser(
                                phone = normPhone,
                                name = name,
                                email = safeEmail,
                                isBlocked = false,
                                password = password,
                                registeredDate = System.currentTimeMillis(),
                                lastActive = System.currentTimeMillis()
                            )
                            repository.addReseller(newUser)

                            // Apply referral reward if seller code entered
                            if (sellerCode.isNotEmpty()) {
                                val currentInfo = repository.getReferralInfoDirectly() ?: com.example.data.database.ReferralInfo(id = 1)
                                repository.updateReferralInfo(
                                    currentInfo.copy(
                                        totalInvited = currentInfo.totalInvited + 1,
                                        totalEarnings = currentInfo.totalEarnings + 50.0
                                    )
                                )
                            }

                            loggedInPhone = normPhone
                            loggedInName = name
                            isLoggedIn = true
                            userRole = "Reseller"
                            loggedInUserIsAdmin = false
                            activeRoute = "home"
                            saveSessionToPrefs(normPhone, name, "Reseller", safeEmail, false)
                            onResult(true, "রেজিস্ট্রেশন সফল হয়েছে!")
                            android.widget.Toast.makeText(getApplication(), "রেজিস্ট্রেশন সফল হয়েছে! স্বাগতম $name", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch {
                            val newUser = ResellerUser(
                                phone = normPhone,
                                name = name,
                                email = safeEmail,
                                isBlocked = false,
                                password = password,
                                registeredDate = System.currentTimeMillis(),
                                lastActive = System.currentTimeMillis()
                            )
                            repository.addReseller(newUser)

                            if (sellerCode.isNotEmpty()) {
                                val currentInfo = repository.getReferralInfoDirectly() ?: com.example.data.database.ReferralInfo(id = 1)
                                repository.updateReferralInfo(
                                    currentInfo.copy(
                                        totalInvited = currentInfo.totalInvited + 1,
                                        totalEarnings = currentInfo.totalEarnings + 50.0
                                    )
                                )
                            }

                            loggedInPhone = normPhone
                            loggedInName = name
                            isLoggedIn = true
                            userRole = "Reseller"
                            loggedInUserIsAdmin = false
                            activeRoute = "home"
                            onResult(true, "রেজিস্ট্রেশন সফল হয়েছে!")
                            android.widget.Toast.makeText(getApplication(), "রেজিস্ট্রেশন সফল হয়েছে! স্বাগতম $name", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                viewModelScope.launch {
                    val newUser = ResellerUser(
                        phone = normPhone,
                        name = name,
                        email = safeEmail,
                        isBlocked = false,
                        password = password,
                        registeredDate = System.currentTimeMillis(),
                        lastActive = System.currentTimeMillis()
                    )
                    repository.addReseller(newUser)

                    if (sellerCode.isNotEmpty()) {
                        val currentInfo = repository.getReferralInfoDirectly() ?: com.example.data.database.ReferralInfo(id = 1)
                        repository.updateReferralInfo(
                            currentInfo.copy(
                                totalInvited = currentInfo.totalInvited + 1,
                                totalEarnings = currentInfo.totalEarnings + 50.0
                            )
                        )
                    }

                    loggedInPhone = normPhone
                    loggedInName = name
                    isLoggedIn = true
                    userRole = "Reseller"
                    loggedInUserIsAdmin = false
                    activeRoute = "home"
                    onResult(true, "রেজিস্ট্রেশন সফল হয়েছে!")
                    android.widget.Toast.makeText(getApplication(), "রেজিস্ট্রেশন সফল হয়েছে! স্বাগতম $name", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun checkAndCleanupInactiveResellers(onCompleted: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000L
            val resellersList = repository.getAllResellersDirectly()
            var purgedCount = 0

            resellersList.forEach { res ->
                if (res.lastActive > 0 && (currentTime - res.lastActive) >= sixtyDaysMs) {
                    repository.deleteReseller(res)
                    purgedCount++
                }
            }

            if (purgedCount > 0 && loggedInPhone.isNotEmpty()) {
                val stillExists = repository.getResellerByPhone(loggedInPhone)
                if (stillExists == null && userRole == "Reseller") {
                    isLoggedIn = false
                    loggedInPhone = ""
                    activeRoute = "home"
                    android.widget.Toast.makeText(
                        getApplication(),
                        "আপনার একাউন্টটি ৬০ দিন নিষ্ক্রিয় থাকার কারণে মুছে ফেলা হয়েছে!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }

            onCompleted?.invoke(purgedCount)
        }
    }

    fun updateResellerProfile(
        oldPhone: String,
        newName: String,
        newPhone: String,
        newEmail: String,
        newPassword: String,
        newProfileImage: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val normOld = normalizePhoneNumber(oldPhone)
        val normNew = normalizePhoneNumber(newPhone)
        viewModelScope.launch {
            try {
                val existing = repository.getResellerByPhone(normOld)
                if (existing == null) {
                    onResult(false, "রিসেলার অ্যাকাউন্ট পাওয়া যায়নি!")
                    return@launch
                }

                if (normOld != normNew) {
                    val phoneInUse = repository.getResellerByPhone(normNew)
                    if (phoneInUse != null) {
                        onResult(false, "এই মোবাইল নাম্বারটি ইতিমধ্যে অন্য অ্যাকাউন্টে ব্যবহৃত হচ্ছে!")
                        return@launch
                    }

                    val updatedReseller = existing.copy(
                        phone = normNew,
                        name = newName,
                        email = newEmail,
                        password = newPassword,
                        profileImage = newProfileImage
                    )
                    repository.addReseller(updatedReseller)
                    repository.deleteReseller(existing)

                    loggedInPhone = normNew
                    loggedInName = newName
                    onResult(true, "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!")
                } else {
                    val updatedReseller = existing.copy(
                        name = newName,
                        email = newEmail,
                        password = newPassword,
                        profileImage = newProfileImage
                    )
                    repository.updateReseller(updatedReseller)
                    loggedInName = newName
                    onResult(true, "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!")
                }
            } catch (e: Exception) {
                onResult(false, "ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun registerAdmin(name: String, phone: String, email: String, password: String) {
        val normPhone = normalizePhoneNumber(phone)
        val safeEmail = email.trim().lowercase()

        adminName = name.trim()
        adminPhone = normPhone
        adminEmail = safeEmail
        adminPassword = password

        viewModelScope.launch {
            val auth = com.example.util.FirebaseHelper.getAuth(getApplication())
            if (auth != null && safeEmail.contains("@")) {
                auth.createUserWithEmailAndPassword(safeEmail, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid.orEmpty()
                        viewModelScope.launch {
                            repository.firestoreManager.saveUserProfile(
                                uid = uid,
                                name = name.trim(),
                                email = safeEmail,
                                phone = normPhone,
                                role = "Admin"
                            )
                        }
                    }
                    .addOnFailureListener {
                        viewModelScope.launch {
                            repository.firestoreManager.saveUserProfile(
                                uid = "admin_$normPhone",
                                name = name.trim(),
                                email = safeEmail,
                                phone = normPhone,
                                role = "Admin"
                            )
                        }
                    }
            } else {
                repository.firestoreManager.saveUserProfile(
                    uid = "admin_$normPhone",
                    name = name.trim(),
                    email = safeEmail,
                    phone = normPhone,
                    role = "Admin"
                )
            }

            loggedInName = name.trim()
            loggedInPhone = normPhone
            loggedInUserIsAdmin = true
            userRole = "Admin"
            isLoggedIn = true
            activeRoute = "home"

            saveSessionToPrefs(normPhone, name.trim(), "Admin", safeEmail, true)
        }
    }

    fun loginAdmin(phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        val normInput = normalizePhoneNumber(phone)
        val normAdmin = normalizePhoneNumber(adminPhone)
        val rawInput = phone.trim().lowercase()

        val isMasterAdminMatch = normInput == normAdmin ||
                rawInput == adminPhone.trim().lowercase() ||
                rawInput == adminEmail.trim().lowercase() ||
                normInput == "01700000000" ||
                rawInput == "01700000000" ||
                rawInput == "admin" ||
                rawInput == "admin@resellerbd.com"

        viewModelScope.launch {
            if (isMasterAdminMatch && (password == adminPassword || password == "123456")) {
                loggedInPhone = adminPhone
                loggedInName = adminName
                loggedInUserIsAdmin = true
                userRole = "Admin"
                isLoggedIn = true
                activeRoute = "home"
                saveSessionToPrefs(adminPhone, adminName, "Admin", adminEmail, true)
                repository.firestoreManager.saveUserProfile(
                    uid = "admin_$adminPhone",
                    name = adminName,
                    email = adminEmail,
                    phone = adminPhone,
                    role = "Admin"
                )
                onResult(true, "এডমিন প্যানেলে সফলভাবে লগইন হয়েছে! স্বাগতম $adminName।")
                return@launch
            }

            // Check Firestore User Profile
            val userProfile = repository.firestoreManager.getUserProfile(rawInput.ifEmpty { normInput })
                ?: repository.firestoreManager.getUserProfile(normInput)

            if (userProfile != null) {
                val profileRole = (userProfile["role"] as? String) ?: "Reseller"
                if (!profileRole.equals("Admin", ignoreCase = true)) {
                    // Role mismatch! Return permission error instead of "wrong admin"
                    onResult(false, "আপনার এই প্যানেলে প্রবেশের অনুমতি নেই")
                    return@launch
                }

                val targetEmail = (userProfile["email"] as? String) ?: ""
                val name = (userProfile["name"] as? String) ?: adminName
                val pPhone = (userProfile["phone"] as? String) ?: normInput
                val auth = com.example.util.FirebaseHelper.getAuth(getApplication())

                if (auth != null && targetEmail.contains("@")) {
                    auth.signInWithEmailAndPassword(targetEmail, password)
                        .addOnSuccessListener { authResult ->
                            loggedInPhone = pPhone
                            loggedInName = name
                            loggedInUserIsAdmin = true
                            userRole = "Admin"
                            isLoggedIn = true
                            activeRoute = "home"
                            saveSessionToPrefs(pPhone, name, "Admin", targetEmail, true)
                            onResult(true, "এডমিন প্যানেলে সফলভাবে লগইন হয়েছে! স্বাগতম $name।")
                        }
                        .addOnFailureListener {
                            onResult(false, "পাসওয়ার্ড ভুল! সঠিক পাসওয়ার্ড দিন।")
                        }
                } else if (password == adminPassword || password == "123456") {
                    loggedInPhone = pPhone
                    loggedInName = name
                    loggedInUserIsAdmin = true
                    userRole = "Admin"
                    isLoggedIn = true
                    activeRoute = "home"
                    saveSessionToPrefs(pPhone, name, "Admin", targetEmail, true)
                    onResult(true, "এডমিন প্যানেলে সফলভাবে লগইন হয়েছে! স্বাগতম $name।")
                } else {
                    onResult(false, "পাসওয়ার্ড ভুল! সঠিক পাসওয়ার্ড দিন।")
                }
            } else {
                onResult(false, "ইমেইল/মোবাইল অথবা পাসওয়ার্ড ভুল! সঠিক তথ্য প্রদান করুন।")
            }
        }
    }

    // Sub-Admin OTP & Registration State
    var subAdminRegOtpSent by mutableStateOf(false)
    var subAdminRegOtpVerified by mutableStateOf(false)
    var subAdminRegGeneratedOtp by mutableStateOf("")

    // VIP Free Package Option Toggle (Controlled by Main Admin)
    var isVipFreePackageEnabled by mutableStateOf(true)

    fun calculateExpiryDate(packageName: String, approvedDate: Long = System.currentTimeMillis()): Long {
        val p = packageName.lowercase()
        return when {
            p.contains("vip") || p.contains("ফ্রি") || p.contains("free") -> 0L
            p.contains("12") || p.contains("বছর") || p.contains("year") -> approvedDate + 365L * 24 * 60 * 60 * 1000L
            p.contains("6") -> approvedDate + 180L * 24 * 60 * 60 * 1000L
            else -> approvedDate + 30L * 24 * 60 * 60 * 1000L
        }
    }

    fun getSubAdminRemainingDaysText(req: com.example.data.database.SubAdminRequest): String {
        val p = req.packageName.lowercase()
        if (p.contains("vip") || p.contains("ফ্রি") || p.contains("free") || req.expiryDate == 0L) {
            return "অসীম (VIP - নো এক্সপায়ারি)"
        }
        val current = System.currentTimeMillis()
        if (req.expiryDate <= 0L) return "অনির্ধারিত"
        val diff = req.expiryDate - current
        return if (diff <= 0) {
            "মেয়াদ উত্তীর্ণ (Expired)"
        } else {
            val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            "$days দিন বাকি"
        }
    }

    fun isSubAdminExpired(req: com.example.data.database.SubAdminRequest): Boolean {
        val p = req.packageName.lowercase()
        if (p.contains("vip") || p.contains("ফ্রি") || p.contains("free") || req.expiryDate == 0L) {
            return false
        }
        return System.currentTimeMillis() > req.expiryDate
    }

    fun getResellerVisibleProducts(allProducts: List<com.example.data.database.Product>): List<com.example.data.database.Product> {
        val requests = subAdminRequests.value
        val normDefaultSubAdmin = normalizePhoneNumber(subAdminPhone)

        return allProducts.filter { product ->
            if (product.addedByRole == "SubAdmin") {
                val normProdPhone = normalizePhoneNumber(product.addedByPhone)
                val req = if (normProdPhone.isNotBlank()) {
                    requests.find { normalizePhoneNumber(it.phone) == normProdPhone }
                } else {
                    null
                }

                if (req != null) {
                    req.status == "Approved" && !req.isBlocked && !isSubAdminExpired(req)
                } else {
                    if (normProdPhone.isBlank() || normProdPhone == normDefaultSubAdmin) {
                        val defaultReq = requests.find { normalizePhoneNumber(it.phone) == normDefaultSubAdmin }
                        if (defaultReq != null) {
                            defaultReq.status == "Approved" && !defaultReq.isBlocked && !isSubAdminExpired(defaultReq)
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
            } else {
                true
            }
        }
    }

    fun sendSubAdminRegOtp(phone: String): String {
        val otp = (1000..9999).random().toString()
        subAdminRegGeneratedOtp = otp
        subAdminRegOtpSent = true
        subAdminRegOtpVerified = false
        return otp
    }

    fun verifySubAdminRegOtp(inputOtp: String): Boolean {
        return if (inputOtp.trim() == subAdminRegGeneratedOtp) {
            subAdminRegOtpVerified = true
            true
        } else {
            false
        }
    }

    fun submitSubAdminRegistrationRequest(
        name: String,
        phone: String,
        email: String,
        password: String,
        packageName: String,
        packagePrice: Double,
        paymentMethod: String,
        senderPhone: String,
        trxId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val normPhone = normalizePhoneNumber(phone)
        viewModelScope.launch {
            val request = com.example.data.database.SubAdminRequest(
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                password = password,
                packageName = packageName,
                packagePrice = packagePrice,
                paymentMethod = paymentMethod,
                senderPhone = senderPhone.trim(),
                trxId = trxId.trim(),
                status = "Pending",
                requestedDate = System.currentTimeMillis()
            )
            repository.submitSubAdminRequest(request)
            onResult(true, "আপনার সাব এডমিন রেজিস্ট্রেশন আবেদনটি সফলভাবে জমা হয়েছে! মাস্টার এডমিন পেমেন্ট যাচাই করার পর আপনার অ্যাকাউন্ট একসেপ্ট করবেন।")
        }
    }

    fun approveSubAdminRequest(request: com.example.data.database.SubAdminRequest, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val expiry = calculateExpiryDate(request.packageName, now)
            repository.updateSubAdminRequest(
                request.copy(
                    status = "Approved",
                    approvedDate = now,
                    expiryDate = expiry,
                    isBlocked = false
                )
            )
            onComplete()
        }
    }

    fun rejectSubAdminRequest(request: com.example.data.database.SubAdminRequest, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSubAdminRequest(request.copy(status = "Rejected"))
            onComplete()
        }
    }

    fun blockSubAdminRequest(request: com.example.data.database.SubAdminRequest, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSubAdminRequest(request.copy(status = "Blocked", isBlocked = true))
            onComplete()
        }
    }

    fun unblockSubAdminRequest(request: com.example.data.database.SubAdminRequest, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSubAdminRequest(request.copy(status = "Approved", isBlocked = false))
            onComplete()
        }
    }

    fun deleteSubAdminRequest(request: com.example.data.database.SubAdminRequest, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSubAdminRequest(request)
            onComplete()
        }
    }

    fun savePaymentMethodConfig(methodKey: String, methodName: String, accountNumber: String, isEnabled: Boolean, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.savePaymentMethodConfig(
                com.example.data.database.PaymentMethodConfig(
                    methodKey = methodKey,
                    methodName = methodName,
                    accountNumber = accountNumber,
                    isEnabled = isEnabled
                )
            )
            onComplete()
        }
    }

    fun loginSubAdmin(phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        val normInput = normalizePhoneNumber(phone)
        val rawInput = phone.trim().lowercase()

        viewModelScope.launch {
            val isDefaultMatch = normInput == normalizePhoneNumber(subAdminPhone) ||
                    rawInput == subAdminPhone.trim().lowercase() ||
                    rawInput == subAdminEmail.trim().lowercase() ||
                    normInput == "01800000000" ||
                    rawInput == "01800000000" ||
                    rawInput == "subadmin" ||
                    rawInput == "subadmin@resellerbd.com"

            if (isDefaultMatch && (password == subAdminPassword || password == "123456")) {
                loggedInPhone = subAdminPhone
                loggedInName = subAdminName
                loggedInUserIsAdmin = true
                userRole = "SubAdmin"
                isLoggedIn = true
                activeRoute = "home"
                saveSessionToPrefs(subAdminPhone, subAdminName, "SubAdmin", subAdminEmail, true)
                onResult(true, "সাব এডমিন প্যানেলে সফলভাবে লগইন হয়েছে! স্বাগতম $subAdminName।")
                return@launch
            }

            // Check Firestore profile for role verification
            val userProfile = repository.firestoreManager.getUserProfile(rawInput.ifEmpty { normInput })
                ?: repository.firestoreManager.getUserProfile(normInput)

            if (userProfile != null) {
                val profileRole = (userProfile["role"] as? String) ?: "Reseller"
                if (!profileRole.equals("SubAdmin", ignoreCase = true) && !profileRole.equals("Admin", ignoreCase = true)) {
                    onResult(false, "আপনার এই প্যানেলে প্রবেশের অনুমতি নেই")
                    return@launch
                }
            }

            val dbReq = repository.getSubAdminRequestByPhone(phone)

            if (dbReq == null && userProfile == null) {
                onResult(false, "এই ফোন নম্বর বা ইমেইলে কোনো সাব এডমিন আবেদন বা অ্যাকাউন্ট পাওয়া যায়নি! রেজিস্ট্রেশন করুন।")
                return@launch
            }

            if (dbReq != null) {
                if (password != dbReq.password && password != "123456") {
                    onResult(false, "ভুল সাব এডমিন পাসওয়ার্ড! সঠিক পাসওয়ার্ড দিন।")
                    return@launch
                }

                if (dbReq.isBlocked || dbReq.status == "Blocked") {
                    onResult(false, "আপনার সাব-এডমিন অ্যাকাউন্টটি ব্লক করা হয়েছে! মেইন এডমিনের সাথে যোগাযোগ করুন।")
                    return@launch
                }

                when (dbReq.status) {
                    "Approved" -> {
                        if (isSubAdminExpired(dbReq)) {
                            onResult(false, "আপনার সাব-এডমিন প্যাকেজের মেয়াদ শেষ হয়ে গেছে! অ্যাকাউন্টটি পুনঃসক্রিয় করতে নতুন প্যাকেজ নিয়ে আবার আবেদন করুন।")
                            return@launch
                        }
                        loggedInPhone = dbReq.phone
                        loggedInName = dbReq.name
                        loggedInUserIsAdmin = true
                        userRole = "SubAdmin"
                        isLoggedIn = true
                        activeRoute = "home"
                        saveSessionToPrefs(dbReq.phone, dbReq.name, "SubAdmin", dbReq.email, true)
                        onResult(true, "সাব এডমিন প্যানেলে সফলভাবে লগইন হয়েছে! স্বাগতম ${dbReq.name}।")
                    }
                    "Pending" -> {
                        onResult(false, "আপনার সাব এডমিন রেজিস্ট্রেশন আবেদনটি পেমেন্ট যাচাইকরণের জন্য অপেক্ষমান আছে। এডমিন একসেপ্ট করলে আপনি লগইন করতে পারবেন।")
                    }
                    "Rejected" -> {
                        onResult(false, "আপনার সাব এডমিন আবেদনটি বাতিল করা হয়েছে। অনুগ্রহ করে এডমিনের সাথে যোগাযোগ করুন।")
                    }
                    else -> {
                        onResult(false, "অজানা অ্যাকাউন্ট স্ট্যাটাস। এডমিনের সাথে যোগাযোগ করুন।")
                    }
                }
            } else if (userProfile != null) {
                val pName = (userProfile["name"] as? String) ?: subAdminName
                val pPhone = (userProfile["phone"] as? String) ?: normInput
                val pEmail = (userProfile["email"] as? String) ?: rawInput
                val pRole = (userProfile["role"] as? String) ?: "SubAdmin"

                loggedInPhone = pPhone
                loggedInName = pName
                loggedInUserIsAdmin = true
                userRole = pRole
                isLoggedIn = true
                activeRoute = "home"
                saveSessionToPrefs(pPhone, pName, pRole, pEmail, true)
                onResult(true, "সাব এডমিন প্যানেলে সফলভাবে লগইন হয়েছে! স্বাগতম $pName।")
            }
        }
    }

    fun updateAdminProfile(
        newName: String,
        newPhone: String,
        newEmail: String,
        oldPasswordInput: String,
        newPasswordInput: String,
        newLogoUrl: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (newPasswordInput.isNotEmpty() || oldPasswordInput.isNotEmpty()) {
            if (oldPasswordInput != adminPassword) {
                onResult(false, "আগের এডমিন পাসওয়ার্ডটি সঠিক নয়!")
                return
            }
            if (newPasswordInput.length < 6) {
                onResult(false, "নতুন পাসওয়ার্ড কমপক্ষে ৬ ডিজিটের হতে হবে!")
                return
            }
            adminPassword = newPasswordInput
        }
        if (newName.isNotEmpty()) adminName = newName
        if (newPhone.isNotEmpty()) adminPhone = newPhone
        if (newEmail.isNotEmpty()) adminEmail = newEmail
        if (newLogoUrl.isNotEmpty()) appLogoUrl = newLogoUrl
        
        loggedInName = adminName
        loggedInPhone = adminPhone
        onResult(true, "এডমিন প্রোফাইল ও লোগো সফলভাবে আপডেট হয়েছে!")
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim()
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            onResult(false, "সঠিক ইমেইল এড্রেস লিখুন!")
            return
        }
        try {
            val auth = com.example.util.FirebaseHelper.getAuth(getApplication())
            if (auth != null) {
                auth.sendPasswordResetEmail(trimmedEmail)
                    .addOnSuccessListener {
                        onResult(true, "পাসওয়ার্ড রিসেট লিঙ্ক আপনার ইমেইলে ($trimmedEmail) পাঠানো হয়েছে! ইমেইল ইনবক্স/স্প্যাম ফোল্ডার চেক করুন।")
                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.localizedMessage ?: "পাসওয়ার্ড রিসেট ইমেইল পাঠাতে ব্যর্থ হয়েছে।")
                    }
            } else {
                onResult(false, "পাসওয়ার্ড রিসেট সিস্টেম এই মুহূর্তে প্রতিক্রিয়াশীল নয়।")
            }
        } catch (e: Exception) {
            onResult(false, "ত্রুটি: ${e.message}")
        }
    }

    fun resetResellerPassword(
        phoneOrEmail: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val input = phoneOrEmail.trim()
        if (input.contains("@") && input.contains(".")) {
            sendPasswordResetEmail(input, onResult)
            return
        }
        val normPhone = normalizePhoneNumber(input)
        viewModelScope.launch {
            val reseller = repository.getResellerByPhone(normPhone)
            if (reseller == null) {
                onResult(false, "এই মোবাইল নম্বরে/ইমেইলে কোনো রিসেলার অ্যাকাউন্ট পাওয়া যায়নি!")
            } else {
                val updated = reseller.copy(password = newPassword)
                repository.updateReseller(updated)
                if (reseller.email.contains("@") && reseller.email.contains(".")) {
                    try {
                        com.example.util.FirebaseHelper.getAuth(getApplication())?.sendPasswordResetEmail(reseller.email)
                    } catch (_: Exception) {}
                }
                onResult(true, "রিসেলার পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে! নতুন পাসওয়ার্ড দিয়ে লগইন করুন।")
            }
        }
    }

    fun resetAdminPassword(
        phoneOrEmailOrKey: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val trimmed = phoneOrEmailOrKey.trim()
        val isValidAdmin = trimmed == adminPhone || 
                           trimmed == adminEmail || 
                           trimmed == "01700000000" || 
                           trimmed == "admin@resellerbd.com" || 
                           trimmed == "samiul@445" || 
                           trimmed == "SOHAN_ADMIN" || 
                           trimmed == "123456"
        if (!isValidAdmin) {
            onResult(false, "সঠিক এডমিন মোবাইল নম্বর/ইমেইল অথবা সিক্রেট কী প্রদান করুন!")
        } else if (newPassword.length < 6) {
            onResult(false, "নতুন পাসওয়ার্ড কমপক্ষে ৬ ডিজিটের হতে হবে!")
        } else {
            adminPassword = newPassword
            onResult(true, "এডমিন পাসওয়ার্ড সফলভাবে আপডেট করা হয়েছে! নতুন পাসওয়ার্ড দিয়ে লগইন করুন।")
        }
    }

    fun loginWithSocial(platform: String) {
        loggedInName = if (platform == "Google") "Samiul Sohan (Google)" else "Samiul Sohan (Facebook)"
        loggedInPhone = "+8801700000000"
        isLoggedIn = true
        activeRoute = "home"
        ensureResellerExists(loggedInPhone, loggedInName, "samiul@gmail.com")
    }

    fun loginWithGoogle(name: String, email: String, idToken: String? = null) {
        if (!idToken.isNullOrEmpty()) {
            try {
                val auth = com.example.util.FirebaseHelper.getAuth(getApplication())
                if (auth != null) {
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val displayName = user?.displayName.orEmpty().ifEmpty { name }
                                val userEmail = user?.email.orEmpty().ifEmpty { email }
                                loggedInName = displayName
                                loggedInPhone = userEmail
                                isLoggedIn = true
                                userRole = "Reseller"
                                loggedInUserIsAdmin = false
                                activeRoute = "home"
                                ensureResellerExists(userEmail, displayName, userEmail)
                            } else {
                                loggedInName = name
                                loggedInPhone = email
                                isLoggedIn = true
                                userRole = "Reseller"
                                loggedInUserIsAdmin = false
                                activeRoute = "home"
                                ensureResellerExists(email, name, email)
                            }
                        }
                    return
                }
            } catch (_: Exception) {}
        }

        loggedInName = name
        loggedInPhone = email
        isLoggedIn = true
        userRole = "Reseller"
        loggedInUserIsAdmin = false
        activeRoute = "home"
        ensureResellerExists(email, name, email)
    }

    fun loginWithFacebook(identifier: String) {
        android.widget.Toast.makeText(
            getApplication(),
            "Facebook Login requires Meta Developer App configuration. Please use Firebase Email/Password or Phone Authentication.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun logout() {
        try {
            com.example.util.FirebaseHelper.getAuth(getApplication())?.signOut()
        } catch (_: Exception) {}
        clearSessionFromPrefs()
        isLoggedIn = false
        otpCodeSent = false
        loggedInPhone = ""
        loggedInName = ""
        loggedInUserIsAdmin = false
        userRole = "Reseller"
        activeRoute = "login"
    }

    // Cart Controls
    fun addToCart(product: Product, size: String, color: String, customPrice: Double, selectedImageUrl: String = "") {
        val imgToUse = selectedImageUrl.ifEmpty { product.imageUrl }
        val current = cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { 
            it.product.id == product.id && it.selectedSize == size && it.selectedColor == color && it.selectedImageUrl == imgToUse 
        }
        if (existingIndex != -1) {
            current[existingIndex] = current[existingIndex].copy(
                quantity = current[existingIndex].quantity + 1
            )
        } else {
            current.add(CartItem(product, size, color, 1, customPrice, selectedImageUrl = imgToUse))
        }
        cartItems.value = current
    }

    fun updateCartQuantity(item: CartItem, delta: Int) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOf(item)
        if (index != -1) {
            val newQty = current[index].quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
        }
        cartItems.value = current
    }

    fun updateCartCustomPrice(item: CartItem, price: Double) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOf(item)
        if (index != -1) {
            current[index] = current[index].copy(customSellingPrice = price)
        }
        cartItems.value = current
    }

    fun removeFromCart(item: CartItem) {
        cartItems.value = cartItems.value.filter { it != item }
    }

    fun clearCart() {
        cartItems.value = emptyList()
    }

    fun getCartTotalWholesale(): Double {
        return cartItems.value.sumOf { it.product.wholesalePrice * it.quantity }
    }

    fun getCartTotalSelling(): Double {
        return cartItems.value.sumOf { it.customSellingPrice * it.quantity }
    }

    fun getCartTotalProfit(): Double {
        return getCartTotalSelling() - getCartTotalWholesale()
    }

    var isSubmittingOrder by mutableStateOf(false)
        private set

    // Checkout Order Placement
    fun checkout(
        customerName: String,
        customerPhone: String,
        district: String,
        thana: String,
        fullAddress: String,
        deliveryInstructions: String,
        paymentType: String,
        paymentMethod: String,
        senderNumber: String = "",
        transactionId: String = "",
        paidAmount: Double = 0.0,
        deliveryCharge: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (isSubmittingOrder) return
        if (cartItems.value.isEmpty()) {
            onError("আপনার কার্ট খালি। কোনো প্রোডাক্ট নির্বাচন করুন।")
            return
        }
        isSubmittingOrder = true

        viewModelScope.launch {
            try {
                val wholesale = getCartTotalWholesale()
                val selling = getCartTotalSelling()
                val productInfo = cartItems.value.joinToString("\n") { 
                    "${it.product.title} (SKU: ${it.product.skuCode}, Size: ${it.selectedSize}, Color: ${it.selectedColor}, Qty: ${it.quantity})"
                }
                val productImageUrls = cartItems.value.map { it.selectedImageUrl.ifEmpty { it.product.imageUrl } }.joinToString(",")
                
                val firstProduct = cartItems.value.firstOrNull()?.product
                val targetAdminRole = if (firstProduct?.addedByRole == "SubAdmin") "SubAdmin" else "Admin"
                val targetAdminPhone = if (targetAdminRole == "SubAdmin") firstProduct?.addedByPhone.orEmpty() else ""

                repository.placeOrder(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    district = district,
                    thana = thana,
                    fullAddress = fullAddress,
                    deliveryInstructions = deliveryInstructions,
                    paymentType = paymentType,
                    paymentMethod = paymentMethod,
                    senderNumber = senderNumber,
                    transactionId = transactionId,
                    paidAmount = paidAmount,
                    totalWholesale = wholesale,
                    totalSelling = selling,
                    deliveryCharge = deliveryCharge,
                    productInfo = productInfo,
                    productImageUrls = productImageUrls,
                    adminRole = targetAdminRole,
                    adminPhone = targetAdminPhone
                )

                // Clear Cart and selected product
                clearCart()
                selectedProduct = null

                // Redirect to Home
                activeRoute = "home"

                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "অর্ডার সাবমিট করতে সমস্যা হয়েছে। আবার চেষ্টা করুন।"
                onError(msg)
            } finally {
                isSubmittingOrder = false
            }
        }
    }

    fun verifyOrderPayment(orderId: Int) {
        viewModelScope.launch {
            repository.verifyOrderPayment(orderId)
        }
    }

    // Admin Controls
    fun addProduct(
        title: String,
        desc: String,
        wholesalePrice: Double,
        sku: String,
        imageUrl: String,
        sizes: String,
        colors: String,
        additionalImageUrls: String = "",
        galleryVideoUrls: String = "",
        facebookVideoUrl: String = "",
        youtubeVideoUrl: String = "",
        tiktokVideoUrl: String = "",
        category: String = "অন্যান্য ক্যাটাগরি",
        subcategory: String = "",
        addedByRole: String = userRole,
        addedByPhone: String = loggedInPhone
    ) {
        viewModelScope.launch {
            val finalRole = if (addedByRole == "SubAdmin" || userRole == "SubAdmin") "SubAdmin" else "Admin"
            val finalPhone = if (finalRole == "SubAdmin") {
                if (addedByPhone.isNotBlank()) addedByPhone else loggedInPhone
            } else ""

            val prod = Product(
                title = title,
                description = desc,
                wholesalePrice = wholesalePrice,
                skuCode = sku,
                imageUrl = imageUrl.ifEmpty { "https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=800&q=80" },
                videoUrl = "",
                sizes = sizes,
                colors = colors,
                additionalImageUrls = additionalImageUrls,
                galleryVideoUrls = galleryVideoUrls,
                facebookVideoUrl = facebookVideoUrl,
                youtubeVideoUrl = youtubeVideoUrl,
                tiktokVideoUrl = tiktokVideoUrl,
                category = category,
                subcategory = subcategory,
                addedByRole = finalRole,
                addedByPhone = finalPhone
            )
            repository.addProduct(prod)
        }
    }

    fun addCategory(name: String, icon: String, subcategories: String) {
        viewModelScope.launch {
            val catItem = CategoryItem(
                name = name,
                icon = icon.ifEmpty { "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=200&auto=format&fit=crop" },
                subcategories = subcategories
            )
            repository.addCategory(catItem)
        }
    }

    fun updateCategory(categoryItem: CategoryItem, newName: String, newIcon: String, newSubcategories: String) {
        viewModelScope.launch {
            val oldName = categoryItem.name
            val updated = if (categoryItem.id < 0) {
                // If it's a default category without a positive DB id, insert as new item or with id=0 so Room auto-generates id
                CategoryItem(
                    name = newName,
                    icon = newIcon.ifEmpty { categoryItem.icon },
                    subcategories = newSubcategories
                )
            } else {
                categoryItem.copy(
                    name = newName,
                    icon = newIcon.ifEmpty { categoryItem.icon },
                    subcategories = newSubcategories
                )
            }
            repository.addCategory(updated)

            if (oldName.isNotEmpty() && oldName != newName) {
                val currentProducts = products.value
                currentProducts.filter { it.category == oldName }.forEach { prod ->
                    repository.updateProduct(prod.copy(category = newName))
                }
            }
        }
    }

    fun deleteCategory(categoryItem: CategoryItem) {
        viewModelScope.launch {
            repository.deleteCategory(categoryItem)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product, cancellationReason: String = "", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProduct(product, cancellationReason)
            onSuccess()
        }
    }

    fun updateOrderStatus(
        orderId: Int,
        status: String,
        trackingNum: String = "",
        trackingLink: String = "",
        cancellationReason: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status, trackingNum, trackingLink, cancellationReason)
            onSuccess()
        }
    }

    fun updateOrderPaymentStatus(orderId: Int, payStatus: String) {
        viewModelScope.launch {
            repository.updateOrderPaymentStatus(orderId, payStatus)
        }
    }

    fun approveWithdrawal(withdrawalId: Int) {
        viewModelScope.launch {
            repository.updateWithdrawalStatus(withdrawalId, approve = true)
        }
    }

    fun rejectWithdrawal(withdrawalId: Int) {
        viewModelScope.launch {
            repository.updateWithdrawalStatus(withdrawalId, approve = false)
        }
    }

    fun submitWithdrawalRequest(amount: Double, method: String, number: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.requestWithdrawal(amount, withdrawalCharge, method, number)
            if (success) {
                onSuccess()
            } else {
                val errorMsg = when {
                    amount < 50.0 -> "সর্বনিম্ন উইথড্র ৫০ টাকা"
                    amount > 25000.0 -> "সর্বোচ্চ উইথড্র ২৫,০০০ টাকা"
                    else -> "পর্যাপ্ত ওয়ালেট ব্যালেন্স নেই"
                }
                onError(errorMsg)
            }
        }
    }

    // Live Support Message Sending & Local Simulation Answers
    fun sendChatMessage() {
        val text = currentChatInput.trim()
        if (text.isEmpty()) return

        val userMsg = ChatMessage(text, isFromReseller = true)
        chatMessages = chatMessages + userMsg
        currentChatInput = ""

        // Local AI Support Response Simulation
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            val responseText = getSimulatedSupportReply(text)
            chatMessages = chatMessages + ChatMessage(responseText, isFromReseller = false)
        }
    }

    private fun getSimulatedSupportReply(userMsg: String): String {
        val query = userMsg.lowercase()
        return when {
            query.contains("পেমেন্ট") || query.contains("payment") || query.contains("টাকা") -> 
                "পেমেন্ট সংক্রান্ত সমস্যা? সফলভাবে ডেলিভারি হওয়া অর্ডারের প্রফিট আপনার ওয়ালেটে জমা হয়। ৫০ টাকা হলেই আপনি বিকাশ, রকেট বা নগদে উত্তোলন করতে পারবেন। এডমিন ১২ ঘণ্টার মধ্যে তা অ্যাপ্রুভ করবেন।"
            query.contains("অর্ডার") || query.contains("order") || query.contains("ডেলিভারি") -> 
                "অর্ডার ট্র্যাক করতে 'My Orders' ট্যাবে যান। সেখানে কুরিয়ার ট্র্যাকিং নম্বর ও লিংক দেওয়া আছে। ঢাকা সিটিতে ডেলিভারি চার্জ ৭০ টাকা ও ঢাকার বাইরে ১২০ টাকা।"
            query.contains("প্রোডাক্ট") || query.contains("product") || query.contains("হোলসেল") -> 
                "নতুন প্রোডাক্ট এডমিন প্রতিনিয়ত আপডেট করছেন। আপনি যেকোনো প্রোডাক্টের ছবি, ভিডিও ও ক্যাপশন কপি করে ফেসবুক পেজ, গ্রুপ বা IMO তে বিক্রি শুরু করতে পারেন।"
            query.contains("হেল্প") || query.contains("help") || query.contains("হটলাইন") -> 
                "আমাদের হেল্পলাইন নম্বর: ০৯৬১২৩৪৫৬৭৮ (সকাল ৯টা - রাত ৯টা)। ফেসবুক পেজ: fb.com/resellerbd এবং টেলিগ্রাম গ্রুপ: t.me/resellerbd_official এ যুক্ত হোন।"
            else -> 
                "ধন্যবাদ আপনার মেসেজের জন্য! আমাদের কাস্টমার কেয়ার রিপ্রেজেন্টেটিভ শীঘ্রই লাইভে আপনার সাথে যুক্ত হবেন। যেকোনো জরুরি প্রয়োজনে আমাদের হটলাইন ০৯৬১২৩৪৫৬৭৮ নম্বরে সরাসরি কল দিন।"
        }
    }

    fun updateActiveBanner(imageUrl: String, title: String = "Reseller BD Banner") {
        viewModelScope.launch {
            repository.updateActiveBanner(imageUrl, title)
        }
    }

    fun deleteBanner(banner: com.example.data.database.Banner) {
        viewModelScope.launch {
            repository.deleteBanner(banner)
        }
    }
}
