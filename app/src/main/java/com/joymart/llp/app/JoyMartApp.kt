package com.joymart.llp.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.joymart.llp.data.*
import com.joymart.llp.ui.components.LoadingOverlay
import com.joymart.llp.ui.screens.auth.AnimatedSplash
import com.joymart.llp.ui.screens.auth.LoginScreen
import com.joymart.llp.ui.screens.auth.RegisterScreen
import com.joymart.llp.ui.screens.dashboard.MainShell
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException

private enum class NoticeKind { SUCCESS, ERROR, WARNING, INFO }
private data class AppNotice(val title: String, val message: String, val kind: NoticeKind)

private fun friendlyThrowable(error: Throwable): String = when (error) {
    is UnknownHostException -> "JoyMart server could not be reached. Please check your internet connection and confirm the API domain."
    is SocketTimeoutException -> "The server is taking too long to respond. Please try again."
    is IOException -> "Network connection problem. Check your internet and try again."
    else -> error.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}

private fun readableApiError(raw: String?): String {
    if (raw.isNullOrBlank()) return "The request could not be completed. Please try again."
    return runCatching {
        val json = JSONObject(raw)
        val errors = json.optJSONObject("errors")
        val firstError = errors?.keys()?.asSequence()?.mapNotNull { key ->
            errors.optJSONArray(key)?.optString(0)?.takeIf { it.isNotBlank() }
        }?.firstOrNull()
        firstError ?: json.optString("message").takeIf { it.isNotBlank() } ?: raw
    }.getOrDefault(raw)
}

@Composable
fun JoyMartApp(api: JoyMartApi, tokens: TokenStore) {
    val scope = rememberCoroutineScope()
    var appReady by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<UserDto?>(null) }
    var isAdmin by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("home") }
    var returnPage by remember { mutableStateOf("home") }
    var inlineMessage by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<AppNotice?>(null) }

    LaunchedEffect(Unit) {
        val token = tokens.get()
        if (!token.isNullOrBlank()) {
            try {
                val response = api.bootstrap()
                val envelope = response.body()
                if (response.isSuccessful && envelope?.success == true) {
                    user = envelope.data?.user ?: envelope.user
                    isAdmin = envelope.data?.isAdmin ?: envelope.isAdmin
                    user?.let { tokens.saveProfile(it, isAdmin) }
                    if (user == null) {
                        tokens.clear()
                        notice = AppNotice("Session unavailable", "Your account details could not be loaded. Please sign in again.", NoticeKind.WARNING)
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    tokens.clear()
                    notice = AppNotice("Session expired", "Please sign in again to continue.", NoticeKind.INFO)
                } else {
                    user = tokens.getProfile()
                    isAdmin = tokens.getAdmin()
                    if (user == null) notice = AppNotice("Unable to connect", "JoyMart could not load your account. Please check the server connection and sign in again.", NoticeKind.ERROR)
                    else notice = AppNotice("Connection limited", "Showing your saved account profile. Some live information may not refresh until the server is reachable.", NoticeKind.WARNING)
                }
            } catch (e: Exception) {
                user = tokens.getProfile()
                isAdmin = tokens.getAdmin()
                notice = if (user == null) AppNotice("Connection problem", friendlyThrowable(e), NoticeKind.ERROR)
                else AppNotice("Connection limited", "${friendlyThrowable(e)} Showing your saved account profile.", NoticeKind.WARNING)
            }
        }
        appReady = true
    }

    if (!appReady) {
        AnimatedSplash()
        return
    }

    Box(Modifier.fillMaxSize()) {
        when {
            user != null && page == "register" -> RegisterScreen(
                busy = busy,
                parentDefault = user?.memberId?.takeIf { it.isNotBlank() } ?: user?.id?.toString().orEmpty(),
                authenticated = true,
                serverMessage = inlineMessage,
                api = api,
                onBack = { inlineMessage = null; page = returnPage },
                onRegister = { name, phone, email, password, confirm, memberId, role ->
                    scope.launch {
                        busy = true
                        inlineMessage = null
                        try {
                            val response = api.registerMember(RegisterRequest(name, phone, email.takeIf { it.isNotBlank() }, password, confirm, memberId, role))
                            val body = response.body()
                            val ok = response.isSuccessful && (body?.success == true || body?.status == true)
                            if (ok) {
                                page = returnPage
                                notice = AppNotice("Member registered", body?.message ?: "The new member account was created successfully.", NoticeKind.SUCCESS)
                            } else {
                                val error = readableApiError(response.errorBody()?.string() ?: body?.message)
                                inlineMessage = error
                                notice = AppNotice("Registration failed", error, NoticeKind.ERROR)
                            }
                        } catch (e: Exception) {
                            val error = friendlyThrowable(e)
                            inlineMessage = error
                            notice = AppNotice("Connection problem", error, NoticeKind.ERROR)
                        } finally { busy = false }
                    }
                }
            )
            user == null && page == "register" -> RegisterScreen(
                busy = busy,
                serverMessage = inlineMessage,
                api = api,
                onBack = { inlineMessage = null; page = "login" },
                onRegister = { name, phone, email, password, confirm, memberId, role ->
                    scope.launch {
                        busy = true
                        inlineMessage = null
                        try {
                            val response = api.register(RegisterRequest(name, phone, email.takeIf { it.isNotBlank() }, password, confirm, memberId, role))
                            val body = response.body()
                            val ok = response.isSuccessful && (body?.success == true || body?.status == true)
                            if (ok) {
                                page = "login"
                                notice = AppNotice("Registration successful", body?.message ?: "Your JoyMart account has been created. Please sign in.", NoticeKind.SUCCESS)
                            } else {
                                val error = readableApiError(response.errorBody()?.string() ?: body?.message)
                                inlineMessage = error
                                notice = AppNotice("Registration failed", error, NoticeKind.ERROR)
                            }
                        } catch (e: Exception) {
                            val error = friendlyThrowable(e)
                            inlineMessage = error
                            notice = AppNotice("Connection problem", error, NoticeKind.ERROR)
                        } finally { busy = false }
                    }
                }
            )
            user == null -> {
                LoginScreen(
                    busy = busy,
                    message = null,
                    onDismiss = { notice = null },
                    onRegister = { inlineMessage = null; page = "register" },
                    onLogin = { login, password ->
                        scope.launch {
                            busy = true
                            try {
                                val response = api.login(LoginRequest(login, password))
                                val body = response.body()
                                val token = body?.token
                                if (response.isSuccessful && body?.success == true && !token.isNullOrBlank()) {
                                    tokens.save(token)
                                    try {
                                        val boot = api.bootstrap()
                                        val bootBody = boot.body()
                                        if (boot.isSuccessful && bootBody?.success == true) {
                                            user = bootBody.data?.user ?: bootBody.user ?: body.user
                                            isAdmin = (bootBody.data?.isAdmin ?: bootBody.isAdmin) || body.isAdmin
                                            user?.let { tokens.saveProfile(it, isAdmin) }
                                            page = "home"
                                            if (user == null) notice = AppNotice("Signed in", "Login succeeded, but the account profile was not returned. Please retry after checking the server.", NoticeKind.WARNING)
                                        } else if (boot.code() == 401 || boot.code() == 403) {
                                            tokens.clear()
                                            notice = AppNotice("Session verification failed", "The server did not accept the new session. Please sign in again.", NoticeKind.ERROR)
                                        } else {
                                            user = body.user
                                            isAdmin = body.isAdmin
                                            user?.let { tokens.saveProfile(it, isAdmin) }
                                            if (user != null) page = "home"
                                            notice = AppNotice("Limited connection", "Sign in succeeded, but some account data could not be refreshed.", NoticeKind.WARNING)
                                        }
                                    } catch (e: Exception) {
                                        user = body.user
                                        isAdmin = body.isAdmin
                                        user?.let { tokens.saveProfile(it, isAdmin) }
                                        if (user != null) page = "home"
                                        notice = AppNotice("Connection problem", "Your sign-in was accepted, but the profile refresh failed. ${friendlyThrowable(e)}", NoticeKind.WARNING)
                                    }
                                } else {
                                    tokens.clear()
                                    val error = readableApiError(response.errorBody()?.string() ?: body?.message ?: "Invalid login details. Please try again.")
                                    notice = AppNotice("Sign-in failed", error, NoticeKind.ERROR)
                                }
                            } catch (e: Exception) {
                                notice = AppNotice("Unable to sign in", friendlyThrowable(e), NoticeKind.ERROR)
                            } finally { busy = false }
                        }
                    }
                )
            }
            else -> MainShell(
                user = user!!,
                isAdmin = isAdmin,
                page = page,
                onNavigate = { target ->
                    if (target == "register") { returnPage = page; inlineMessage = null }
                    page = target
                },
                onLogout = {
                    scope.launch {
                        runCatching { api.logout() }
                        tokens.clear()
                        user = null
                        isAdmin = false
                        page = "login"
                        notice = AppNotice("Signed out", "You have been securely signed out.", NoticeKind.SUCCESS)
                    }
                },
                api = api
            )
        }

        if (busy) LoadingOverlay()
        notice?.let { current ->
            AppNoticeDialog(notice = current, onDismiss = { notice = null })
        }
    }
}

@Composable
private fun AppNoticeDialog(notice: AppNotice, onDismiss: () -> Unit) {
    val tint = when (notice.kind) {
        NoticeKind.SUCCESS -> Color(0xFF16865B)
        NoticeKind.ERROR -> Color(0xFFD64545)
        NoticeKind.WARNING -> Color(0xFFB7791F)
        NoticeKind.INFO -> MaterialTheme.colorScheme.primary
    }
    val icon = when (notice.kind) {
        NoticeKind.SUCCESS -> Icons.Default.CheckCircle
        NoticeKind.ERROR -> Icons.Default.Error
        NoticeKind.WARNING -> Icons.Default.Warning
        NoticeKind.INFO -> Icons.Default.Info
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Surface(shape = MaterialTheme.shapes.large, color = tint.copy(alpha = .10f)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(12.dp).size(26.dp))
        } },
        title = { Text(notice.title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(notice.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Button(onClick = onDismiss, shape = MaterialTheme.shapes.medium) { Text("Continue") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
