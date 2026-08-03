package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Product::class,
        Order::class,
        Wallet::class,
        Withdrawal::class,
        ReferralInfo::class,
        Banner::class,
        ResellerUser::class,
        CategoryItem::class,
        NotificationItem::class,
        SupportMessage::class,
        CustomSocialChannel::class,
        ReferredUserRecord::class,
        ReferralOrderRecord::class,
        TutorialVideo::class,
        SubAdminRequest::class,
        PaymentMethodConfig::class
    ],
    version = 22,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reseller_bd_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
