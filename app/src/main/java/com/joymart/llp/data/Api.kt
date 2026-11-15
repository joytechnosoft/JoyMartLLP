package com.joymart.llp.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import com.google.gson.JsonElement
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class TokenStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "joymart_secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun get(): String? = prefs.getString("token", null)
    fun save(token: String) { prefs.edit().putString("token", token).apply() }
    fun saveProfile(profile: UserDto, isAdmin: Boolean) { prefs.edit().putString("profile", com.google.gson.Gson().toJson(profile)).putBoolean("is_admin", isAdmin).apply() }
    fun getProfile(): UserDto? = prefs.getString("profile", null)?.let { runCatching { com.google.gson.Gson().fromJson(it, UserDto::class.java) }.getOrNull() }
    fun getAdmin(): Boolean = prefs.getBoolean("is_admin", false)
    fun clear() { prefs.edit().remove("token").remove("profile").remove("is_admin").apply() }
}

interface JoyMartApi {
    @POST("login") suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<Any>>
    @POST("register") suspend fun register(@Body body: RegisterRequest): Response<ApiEnvelope<Any>>
    @POST("register-member") suspend fun registerMember(@Body body: RegisterRequest): Response<ApiEnvelope<Any>>
    @GET("check-sponsor/{memberId}") suspend fun checkSponsor(@Path("memberId") memberId: String): Response<SponsorCheckDto>
    @GET("me") suspend fun me(): Response<ApiEnvelope<UserDto>>
    @GET("bootstrap") suspend fun bootstrap(): Response<ApiEnvelope<BootstrapDto>>
    @POST("logout") suspend fun logout(): Response<ApiEnvelope<Any>>
    @GET suspend fun getRaw(@Url path: String): Response<JsonElement>
    @POST suspend fun postRaw(@Url path: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<JsonElement>
    @GET("vrindaban/categories") suspend fun categories(): Response<ApiEnvelope<List<CategoryDto>>>
    @POST("vrindaban/categories") suspend fun createCategory(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiEnvelope<Any>>
    @GET("vrindaban/products") suspend fun products(): Response<ApiEnvelope<List<ProductDto>>>
    @POST("vrindaban/products") suspend fun createProduct(@Body body: NativeProductRequest): Response<ApiEnvelope<Any>>
    @GET("vrindaban/packages") suspend fun packages(): Response<ApiEnvelope<List<PackageDto>>>
    @POST("vrindaban/packages") suspend fun createPackage(@Body body: NativePackageRequest): Response<ApiEnvelope<Any>>
}

object ApiFactory {
    fun create(context: Context, tokens: TokenStore): JoyMartApi {
        val auth = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("X-Requested-With", "XMLHttpRequest")
            tokens.get()?.let { request.header("Authorization", "Bearer $it") }
            chain.proceed(request.build())
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.joymart.llp.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        return Retrofit.Builder().baseUrl(com.joymart.llp.BuildConfig.API_BASE_URL)
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
            .create(JoyMartApi::class.java)
    }
}
