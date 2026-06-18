package com.javis.launcher.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.javis.launcher.data.local.*
import com.javis.launcher.data.network.AiApiService
import com.javis.launcher.data.network.ElevenLabsApiService
import com.javis.launcher.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JavisDatabase =
        Room.databaseBuilder(context, JavisDatabase::class.java, "javis_database")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideConversationDao(db: JavisDatabase) = db.conversationDao()
    @Provides fun provideMemoryDao(db: JavisDatabase) = db.memoryDao()
    @Provides fun provideAppDao(db: JavisDatabase) = db.appDao()
    @Provides fun provideTaskDao(db: JavisDatabase) = db.taskDao()
    @Provides fun provideContactDao(db: JavisDatabase) = db.contactDao()
    @Provides fun provideNotificationDao(db: JavisDatabase) = db.notificationDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // ── AI Services (one provider per service, no duplicate @Named bindings) ────

    @Provides
    @Singleton
    @Named("openrouter")
    fun provideOpenRouterService(okHttpClient: OkHttpClient): AiApiService =
        buildRetrofit(BuildConfig.OPENROUTER_BASE_URL, okHttpClient)
            .create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqService(okHttpClient: OkHttpClient): AiApiService =
        buildRetrofit(BuildConfig.GROQ_BASE_URL, okHttpClient)
            .create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekService(okHttpClient: OkHttpClient): AiApiService =
        buildRetrofit(BuildConfig.DEEPSEEK_BASE_URL, okHttpClient)
            .create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("together")
    fun provideTogetherService(okHttpClient: OkHttpClient): AiApiService =
        buildRetrofit(BuildConfig.TOGETHER_BASE_URL, okHttpClient)
            .create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("fireworks")
    fun provideFireworksService(okHttpClient: OkHttpClient): AiApiService =
        buildRetrofit(BuildConfig.FIREWORKS_BASE_URL, okHttpClient)
            .create(AiApiService::class.java)

    @Provides
    @Singleton
    fun provideElevenLabsService(okHttpClient: OkHttpClient): ElevenLabsApiService =
        buildRetrofit(BuildConfig.ELEVENLABS_BASE_URL, okHttpClient)
            .create(ElevenLabsApiService::class.java)

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(GsonBuilder().setLenient().create())
            )
            .build()
}
