package com.joymart.llp.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import com.joymart.llp.data.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.joymart.llp.ui.screens.auth.*
import com.joymart.llp.ui.screens.dashboard.MainShell
import com.joymart.llp.ui.components.LoadingOverlay

@Composable fun JoyMartApp(api: JoyMartApi, tokens: TokenStore) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var user by remember { mutableStateOf<UserDto?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("home") }
    var returnPage by remember { mutableStateOf("home") }
    var message by remember { mutableStateOf<String?>(null) }
    fun readableApiError(raw: String?): String {
        if (raw.isNullOrBlank()) return "Registration failed. Please try again."
        return runCatching {
            val json = JSONObject(raw)
            val errors = json.optJSONObject("errors")
            val firstError = errors?.keys()?.asSequence()?.mapNotNull { key ->
                errors.optJSONArray(key)?.optString(0)
            }?.firstOrNull()
            firstError ?: json.optString("message").takeIf { it.isNotBlank() } ?: raw
        }.getOrDefault(raw)
    }
    LaunchedEffect(Unit) {
        val token = tokens.get()
        if (!token.isNullOrBlank()) {
            runCatching { api.bootstrap() }.onSuccess { response ->
                if (response.isSuccessful && response.body()?.success == true) {
                    user = response.body()?.data?.user ?: response.body()?.user
                    isAdmin = response.body()?.data?.isAdmin ?: false
                    user?.let { tokens.saveProfile(it, isAdmin) }
                } else if (response.code() == 401) {
                    tokens.clear()
                } else {
                    user = tokens.getProfile()
                    isAdmin = tokens.getAdmin()
                }
            }.onFailure {
                // Keep the encrypted token and last profile while offline; the user is not logged out.
                user = tokens.getProfile()
                isAdmin = tokens.getAdmin()
            }
        }
        loading = false
    }
    if (loading && user == null && tokens.getProfile() == null) { AnimatedSplash(); return }
    Box(Modifier.fillMaxSize()) {
        if (user != null && page == "register") {
            RegisterScreen(
                busy=loading, parentDefault=user?.memberId?.takeIf { it.isNotBlank() } ?: user?.id?.toString().orEmpty(), authenticated=true, serverMessage=message, api=api,
                onBack={page=returnPage},
                onRegister={name,phone,email,password,confirm,memberId,role ->
                    scope.launch {
                        loading=true
                        try {
                            val result=api.registerMember(RegisterRequest(name,phone,email.takeIf{it.isNotBlank()},password,confirm,memberId,role))
                            val ok=result.isSuccessful && (result.body()?.success==true || result.body()?.status==true)
                            message=if(ok) "New member registered successfully." else readableApiError(result.errorBody()?.string() ?: result.body()?.message)
                            if(ok) page=returnPage
                        } catch(e:Exception){ message=e.message ?: "Network error" }
                        loading=false
                    }
                }
            )
        } else if (user == null) {
            if (page == "register") RegisterScreen(
                busy=loading, serverMessage=message, api=api, onBack={page="login"}, onRegister={name,phone,email,password,confirm,memberId,role ->
                    scope.launch {
                        loading=true
                        try {
                            val result=api.register(RegisterRequest(name,phone,email.takeIf{it.isNotBlank()},password,confirm,memberId,role))
                            message=if(result.isSuccessful && (result.body()?.success==true || result.body()?.status==true)) "Registration successful. Please sign in." else readableApiError(result.errorBody()?.string() ?: result.body()?.message)
                            if(result.isSuccessful && (result.body()?.success==true || result.body()?.status==true)) page="login"
                        } catch(e:Exception){ message=e.message ?: "Network error" }
                        loading=false
                    }
                }
            ) else LoginScreen(
                busy=loading, message=message, onDismiss={message=null}, onRegister={page="register"},
                onLogin={login,password -> scope.launch {
                    loading=true; message=null
                    try {
                        val result=api.login(LoginRequest(login,password))
                        val body=result.body()
                        val token=body?.token
                        if(result.isSuccessful && body?.success==true && !token.isNullOrBlank()) {
                            tokens.save(token)
                            val boot=api.bootstrap()
                            if(boot.isSuccessful && boot.body()?.success==true) {
                                user=boot.body()?.data?.user ?: boot.body()?.user
                                isAdmin=boot.body()?.data?.isAdmin ?: false
                                user?.let { tokens.saveProfile(it, isAdmin) }
                                page="home"
                            } else if(boot.code()==401) {
                                tokens.clear(); message="Your session could not be verified. Please sign in again."
                            } else {
                                user=body.user
                                isAdmin=body.isAdmin
                                user?.let { tokens.saveProfile(it, isAdmin) }
                                page="home"
                                message="Signed in. Some live data may be temporarily unavailable."
                            }
                        } else message=body?.message ?: result.errorBody()?.string() ?: "Login failed"
                    } catch(e:Exception){ message=e.message ?: "Unable to connect" }
                    loading=false
                }}
            )
        } else if (page != "register") {
            MainShell(user!!, isAdmin, page, { target ->
                if(target=="register") { returnPage=page; message=null }
                page=target
            }, onLogout={ scope.launch {
                runCatching { api.logout() }; tokens.clear(); user=null; isAdmin=false; page="login"
            }}, api=api)
        }
        if (loading) LoadingOverlay()
    }
}
