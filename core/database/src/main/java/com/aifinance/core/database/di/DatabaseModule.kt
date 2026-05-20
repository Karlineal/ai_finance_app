package com.aifinance.core.database.di

import android.content.Context
import androidx.room.Room
import com.aifinance.core.database.AiFinanceDatabase
import com.aifinance.core.database.DatabaseCallback
import com.aifinance.core.database.dao.AccountDao
import com.aifinance.core.database.dao.CategoryDao
import com.aifinance.core.database.dao.ScheduledRuleDao
import com.aifinance.core.database.dao.TransactionDao
import com.aifinance.core.database.dao.SavingsGoalDao // 💡 新增导入
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        accountDao: Provider<AccountDao>,
        categoryDao: Provider<CategoryDao>,
        transactionDao: Provider<TransactionDao>,
        scheduledRuleDao: Provider<ScheduledRuleDao>,
        savingsGoalDao: Provider<SavingsGoalDao>, // 💡 1. 注入新增的 SavingsGoalDao Provider，解除循环依赖
    ): AiFinanceDatabase {
        return Room.databaseBuilder(
            context,
            AiFinanceDatabase::class.java,
            "ai_finance.db"
        )
            .fallbackToDestructiveMigration()
            // 💡 2. 将新的 Provider 传给你的数据库初始回调
            .addCallback(
                DatabaseCallback(
                    accountDao,
                    categoryDao,
                    transactionDao,
                    scheduledRuleDao,
                    savingsGoalDao // 👈 记得传进去
                )
            )
            .build()
    }

    @Provides
    fun provideAccountDao(database: AiFinanceDatabase) = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AiFinanceDatabase) = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AiFinanceDatabase) = database.transactionDao()

    @Provides
    fun provideScheduledRuleDao(database: AiFinanceDatabase) = database.scheduledRuleDao()

    @Provides
    fun provideSavingsGoalDao(database: AiFinanceDatabase) = database.savingsGoalDao() // 💡 3. 向 Hilt 容器暴露 SavingsGoalDao
}