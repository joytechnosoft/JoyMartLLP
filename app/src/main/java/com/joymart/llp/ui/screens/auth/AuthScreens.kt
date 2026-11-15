package com.joymart.llp.ui.screens.auth

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.*
import com.joymart.llp.data.*
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplash() {
    val transition = rememberInfiniteTransition(label = "joymart-splash")
    val logoScale by transition.animateFloat(
        initialValue = .94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logo-scale"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = .16f,
        targetValue = .34f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow-alpha"
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF0B1830), Color(0xFF153B61), Color(0xFF087F78)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 100.dp, end = 28.dp)
                .size(190.dp).clip(CircleShape).background(Color.White.copy(alpha = glowAlpha))
        )
        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 120.dp)
                .size(230.dp).clip(CircleShape).background(Color(0xFF4BD6B7).copy(alpha = .10f))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(104.dp).scale(logoScale).clip(RoundedCornerShape(31.dp))
                    .background(Color.White.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Text("J", color = Color.White, fontSize = 61.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(22.dp))
            Text("JoyMart LLP", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("ONE BUSINESS. ONE SMART PLATFORM.", color = Color.White.copy(alpha = .76f), fontSize = 10.sp, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = Color(0xFF65E4C7), strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(12.dp))
            Text("Preparing your workspace", color = Color.White.copy(alpha = .70f), fontSize = 11.sp)
        }
    }
}

@Composable
fun LoginScreen(
    busy: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AuthBackdrop {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            BrandMark()
            Spacer(Modifier.height(34.dp))
            Text("Welcome back", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF102447), fontWeight = FontWeight.Bold)
            Text("Sign in to your JoyMart workspace", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF68758A), modifier = Modifier.padding(top = 7.dp, bottom = 25.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("ACCOUNT LOGIN", style = MaterialTheme.typography.labelLarge, color = Color(0xFF53647D), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        label = { Text("Phone number or email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onLogin(login.trim(), password) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = !busy && login.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Sign in securely", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
                        Text("New to JoyMart?  Create an account")
                    }
                }
            }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
            Spacer(Modifier.height(18.dp))
            Text("Secure access to your business workspace", color = Color(0xFF8290A3), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun RegisterScreen(
    busy: Boolean,
    parentDefault: String = "",
    authenticated: Boolean = false,
    serverMessage: String? = null,
    api: JoyMartApi,
    onBack: () -> Unit,
    onRegister: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var memberId by remember(parentDefault) { mutableStateOf(parentDefault) }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var sponsorName by remember { mutableStateOf<String?>(null) }
    var sponsorRole by remember { mutableStateOf<String?>(null) }
    var allowedRoles by remember { mutableStateOf<List<AllowedRoleDto>>(emptyList()) }
    var selectedRole by remember { mutableStateOf("") }
    var roleExpanded by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun verifySponsor(value: String) {
        val id = value.trim()
        if (id.isBlank()) {
            localError = "Enter Sponsor / Parent Member ID."
            sponsorName = null
            allowedRoles = emptyList()
            selectedRole = ""
            return
        }
        scope.launch {
            verifying = true
            localError = null
            sponsorName = null
            sponsorRole = null
            allowedRoles = emptyList()
            selectedRole = ""
            try {
                val response = api.checkSponsor(id)
                val result = response.body()
                if (response.isSuccessful && result?.success == true) {
                    sponsorName = result.sponsorName
                    sponsorRole = result.sponsorRole
                    allowedRoles = result.allowedRoles
                    if (allowedRoles.isEmpty()) {
                        localError = "No registration position is available under this sponsor."
                    } else {
                        selectedRole = allowedRoles.first().name
                    }
                } else {
                    localError = result?.message
                        ?: response.errorBody()?.string()
                        ?: "Invalid Sponsor / Parent Member ID."
                }
            } catch (e: Exception) {
                localError = when (e) {
                is java.net.UnknownHostException -> "Cannot reach JoyMart server. Check your internet connection and API domain."
                is java.net.SocketTimeoutException -> "Server response timed out. Please try again."
                else -> e.message ?: "Unable to verify Sponsor ID. Check your connection."
            }
            } finally {
                verifying = false
            }
        }
    }

    LaunchedEffect(parentDefault) {
        if (parentDefault.isNotBlank()) verifySponsor(parentDefault)
    }

    AuthBackdrop {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
                Text(if (authenticated) "  Back to My Profile" else "  Back to login")
            }
            BrandMark()
            Spacer(Modifier.height(18.dp))
            Text("Create New Business Account", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF102447))
            Text("Create New Business Account (Dynamic Hierarchy)", color = Color.Gray, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))

            Text("SPONSOR / PARENT MEMBER ID *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = memberId,
                    onValueChange = {
                        memberId = it
                        sponsorName = null
                        sponsorRole = null
                        allowedRoles = emptyList()
                        selectedRole = ""
                        localError = null
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    placeholder = { Text("Enter Sponsor ID") }
                )
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { verifySponsor(memberId) },
                    enabled = !verifying && memberId.isNotBlank()
                ) {
                    if (verifying) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Verify")
                }
            }
            if (!sponsorName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF078A57), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Sponsor: $sponsorName (${sponsorRole ?: "—"})", color = Color(0xFF078A57), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }

            Text("FULL NAME *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().heightIn(min = 54.dp), placeholder = { Text("Enter full name") }, singleLine = true, shape = RoundedCornerShape(13.dp))
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("PHONE NUMBER *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth().heightIn(min = 54.dp), placeholder = { Text("Phone number") }, singleLine = true, shape = RoundedCornerShape(13.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
                Column(Modifier.weight(1f)) {
                    Text("EMAIL ADDRESS", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().heightIn(min = 54.dp), placeholder = { Text("Email (optional)") }, singleLine = true, shape = RoundedCornerShape(13.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("PASSWORD *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth().heightIn(min = 54.dp), placeholder = { Text("Min 6 characters") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(13.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("CONFIRM PASSWORD *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth().heightIn(min = 54.dp), placeholder = { Text("Re-type password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, shape = RoundedCornerShape(13.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            Text("BUSINESS ROLE (Dynamic Based on Sponsor) *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
            Box {
                OutlinedButton(
                    onClick = { roleExpanded = true },
                    enabled = allowedRoles.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val roleLabel = allowedRoles.firstOrNull { it.name == selectedRole }?.displayName
                    Text(roleLabel ?: if (allowedRoles.isEmpty()) "-- Verify Sponsor First --" else "-- Select Allowed Role --", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                    allowedRoles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.displayName.ifBlank { role.name }) },
                            onClick = { selectedRole = role.name; roleExpanded = false }
                        )
                    }
                }
            }
            val visibleError = localError ?: serverMessage
            AnimatedVisibility(visible = !visibleError.isNullOrBlank(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Card(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFC43D3D), modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(visibleError.orEmpty(), color = Color(0xFF9E3030), style = MaterialTheme.typography.bodySmall)
                            if (!localError.isNullOrBlank()) {
                                TextButton(
                                    onClick = { verifySponsor(memberId) },
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                ) { Text("Try again", color = Color(0xFF9E3030), fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    when {
                        sponsorName.isNullOrBlank() || selectedRole.isBlank() -> localError = "Verify Sponsor Member ID and select an allowed Business Role."
                        name.isBlank() || phone.isBlank() || pass.length < 6 || pass != confirm ->
                            localError = "Enter name, phone and matching password (minimum 6 characters)."
                        else -> {
                            localError = null
                            onRegister(name.trim(), phone.trim(), email.trim(), pass, confirm, memberId.trim(), selectedRole)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !busy && !verifying && allowedRoles.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Complete Registration", fontWeight = FontWeight.Bold)
            }
            Text(
                "Role options are loaded from your Sponsor's allowed hierarchy.",
                fontSize = 11.sp, color = Color.Gray,
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
fun AuthBackdrop(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFEAF3F8), Color(0xFFF7F9FC), Color(0xFFF9FBFD)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            content = content
        )
    }
}

@Composable
fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF132B50), Color(0xFF087F78)))),
            contentAlignment = Alignment.Center
        ) {
            Text("J", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text("JOYMART", fontWeight = FontWeight.ExtraBold, color = Color(0xFF102447), letterSpacing = 1.4.sp)
            Text("LLP BUSINESS PLATFORM", fontSize = 9.sp, color = Color(0xFF718097), letterSpacing = 1.sp)
        }
    }
}
