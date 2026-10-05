package com.offline_First.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.offline_First.data.repository.AuthRepository
import com.offline_First.data.repository.RegistrationInput
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    context: Context,
    private val supabaseUrl: String,
    private val anonKey: String
) : AuthRepository {

    private val tokenStore = EncryptedRefreshTokenStore(context.applicationContext)
    private val authenticated = MutableStateFlow(false)
    private var accessToken: String? = null

    override fun observeSession(): Flow<Boolean> = authenticated.asStateFlow()

    override suspend fun restoreSession(): Result<Unit> = try {
        withContext(Dispatchers.IO) {
            val refreshToken = tokenStore.read() ?: return@withContext
            authenticated.value = true
            try {
                val response = post(
                    path = "token?grant_type=refresh_token",
                    body = buildJsonObject { put("refresh_token", refreshToken) }
                )
                storeSession(response, requireAccessToken = true)
            } catch (error: SupabaseAuthException) {
                if (error.statusCode in 400..403) {
                    accessToken = null
                    authenticated.value = false
                    tokenStore.clear()
                } else {
                    throw error
                }
            }
        }
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    override suspend fun signIn(contact: String, password: String): Result<Unit> =
        performAuth {
            val response = post(
                path = "token?grant_type=password",
                body = buildJsonObject {
                    put("email", contact.trim())
                    put("password", password)
                }
            )
            storeSession(response, requireAccessToken = true)
        }

    override suspend fun register(input: RegistrationInput): Result<Unit> =
        performAuth {
            val response = post(
                path = "signup",
                body = buildJsonObject {
                    put("email", input.email.trim())
                    put("password", input.password)
                    put(
                        "data",
                        buildJsonObject {
                            put("full_name", input.fullName.trim())
                            put("mobile", input.mobile.trim())
                        }
                    )
                }
            )
            storeSession(response, requireAccessToken = false)
        }

    override suspend fun requestPasswordReset(contact: String): Result<Unit> =
        performAuth {
            post(
                path = "recover",
                body = buildJsonObject { put("email", contact.trim()) }
            )
        }

    override suspend fun signOut(): Result<Unit> {
        var remoteFailure: Exception? = null
        var cleanupFailure: Exception? = null
        try {
            withContext(Dispatchers.IO) {
                accessToken?.let { token ->
                    post(path = "logout", body = JsonObject(emptyMap()), bearerToken = token)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            remoteFailure = error
        } finally {
            try {
                withContext(NonCancellable + Dispatchers.IO) {
                    tokenStore.clear()
                }
            } catch (error: Exception) {
                cleanupFailure = error
            }
            accessToken = null
            authenticated.value = false
        }
        if (remoteFailure != null && cleanupFailure != null) {
            remoteFailure.addSuppressed(cleanupFailure)
        }
        val failure = remoteFailure ?: cleanupFailure
        return if (failure != null) {
            Result.failure(failure)
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun performAuth(operation: suspend () -> Unit): Result<Unit> = try {
        withContext(Dispatchers.IO) {
            ensureConfigured()
            operation()
        }
        Result.success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private fun storeSession(response: JsonObject?, requireAccessToken: Boolean) {
        val access = response?.string("access_token")
        val refresh = response?.string("refresh_token")
        if (access.isNullOrBlank()) {
            if (requireAccessToken) {
                throw IllegalStateException("Supabase did not return an authenticated session.")
            }
            return
        }
        accessToken = access
        if (!refresh.isNullOrBlank()) tokenStore.write(refresh)
        authenticated.value = true
    }

    private fun post(
        path: String,
        body: JsonObject,
        bearerToken: String = anonKey
    ): JsonObject? {
        ensureConfigured()
        val connection = URL("${supabaseUrl.trimEnd('/')}/auth/v1/$path")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("apikey", anonKey)
            connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { stream ->
                stream.write(Json.encodeToString(JsonObject.serializer(), body).toByteArray(Charsets.UTF_8))
            }

            val statusCode = connection.responseCode
            val responseText = (if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (statusCode !in 200..299) {
                val errorMessage = runCatching {
                    Json.parseToJsonElement(responseText).jsonObject.let { error ->
                        error.string("msg")
                            ?: error.string("message")
                            ?: error.string("error_description")
                            ?: error.string("error")
                    }
                }.getOrNull() ?: "Supabase authentication failed (HTTP $statusCode)."
                throw SupabaseAuthException(statusCode, errorMessage)
            }
            return responseText.takeIf(String::isNotBlank)
                ?.let { Json.parseToJsonElement(it).jsonObject }
        } finally {
            connection.disconnect()
        }
    }

    private fun ensureConfigured() {
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            throw IllegalStateException(
                "Supabase is not configured. Set supabaseUrl and supabaseAnonKey in Gradle properties " +
                    "or SUPABASE_URL and SUPABASE_ANON_KEY in the environment."
            )
        }
        val url = URL(supabaseUrl)
        require(url.protocol == "https" || url.protocol == "http") {
            "Supabase URL must use HTTP or HTTPS."
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}

private class SupabaseAuthException(
    val statusCode: Int,
    message: String
) : IllegalStateException(message)

private class EncryptedRefreshTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun read(): String? {
        val encoded = preferences.getString(TOKEN_KEY, null) ?: return null
        val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        require(encrypted.size > GCM_IV_LENGTH) { "Stored authentication session is invalid." }
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun write(refreshToken: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.iv + cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        val saved = preferences.edit()
            .putString(TOKEN_KEY, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
            .commit()
        check(saved) { "Could not securely save the authentication session." }
    }

    fun clear() {
        check(preferences.edit().remove(TOKEN_KEY).commit()) {
            "Could not clear the saved authentication session."
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "supabase_auth"
        const val TOKEN_KEY = "refresh_token"
        const val KEY_ALIAS = "edunova_supabase_refresh_token"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH = 128
    }
}
