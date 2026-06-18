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

    @Provides
    @Singleton
    @Named("openrouter")
    fun provideOpenRouterRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(BuildConfig.OPENROUTER_BASE_URL, okHttpClient)

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(BuildConfig.GROQ_BASE_URL, okHttpClient)

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(BuildConfig.DEEPSEEK_BASE_URL, okHttpClient)

    @Provides
    @Singleton
    @Named("together")
    fun provideTogetherRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(BuildConfig.TOGETHER_BASE_URL, okHttpClient)

    @Provides
    @Singleton
    @Named("fireworks")
    fun provideFireworksRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(BuildConfig.FIREWORKS_BASE_URL, okHttpClient)

    @Provides
    @Singleton
    @Named("openrouter")
    fun provideOpenRouterService(@Named("openrouter") retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("groq")
    fun provideGroqService(@Named("groq") retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekService(@Named("deepseek") retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("together")
    fun provideTogetherService(@Named("together") retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides
    @Singleton
    @Named("fireworks")
    fun provideFireworksService(@Named("fireworks") retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

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
