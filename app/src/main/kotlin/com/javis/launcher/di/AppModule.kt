package com.javis.launcher.di

import android.content.Context
import androidx.room.Room
import com.javis.launcher.data.db.JavisDatabase
import com.javis.launcher.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JavisDatabase =
        Room.databaseBuilder(context, JavisDatabase::class.java, JavisDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton fun provideUserProfileDao(db: JavisDatabase): UserProfileDao = db.userProfileDao()
    @Provides @Singleton fun provideMemoryDao(db: JavisDatabase): MemoryDao = db.memoryDao()
    @Provides @Singleton fun provideConversationDao(db: JavisDatabase): ConversationDao = db.conversationDao()
    @Provides @Singleton fun provideMessageDao(db: JavisDatabase): MessageDao = db.messageDao()
    @Provides @Singleton fun provideInstalledAppDao(db: JavisDatabase): InstalledAppDao = db.installedAppDao()
    @Provides @Singleton fun provideReminderDao(db: JavisDatabase): ReminderDao = db.reminderDao()
    @Provides @Singleton fun provideNotificationDao(db: JavisDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
