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
import androidx.compose.ui.unit.*
import com.joymart.llp.data.*
import kotlinx.coroutines.launch

@Composable fun AnimatedSplash() {
    val infinite= rememberInfiniteTransition(label="splash")
    val scale by infinite.animateFloat(0.92f,1.06f, infiniteRepeatable(tween(900),RepeatMode.Reverse), label="scale")
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1730),Color(0xFF1D4266),Color(0xFF087F78)))), contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(92.dp).scale(scale).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(.12f)), contentAlignment=Alignment.Center) {
                Text("J", color=Color.White, fontSize=54.sp, fontWeight=FontWeight.Black)
            }
            Spacer(Modifier.height(18.dp)); Text("JoyMart LLP", color=Color.White, fontSize=27.sp, fontWeight=FontWeight.Bold)
            Text("One business. One smart platform.", color=Color.White.copy(.76f), fontSize=13.sp)
            Spacer(Modifier.height(28.dp)); CircularProgressIndicator(color=Color(0xFF49E0BD), strokeWidth=2.dp, modifier=Modifier.size(25.dp))
        }
    }
}

@Composable fun LoginScreen(busy:Boolean,message:String?,onDismiss:()->Unit,onRegister:()->Unit,onLogin:(String,String)->Unit) {
    var login by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    AuthBackdrop {
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp), horizontalAlignment=Alignment.Start) {
            BrandMark(); Spacer(Modifier.height(30.dp))
            Text("Welcome back", fontSize=30.sp, fontWeight=FontWeight.Bold, color=Color(0xFF102447))
            Text("Sign in to your JoyMart workspace", color=Color(0xFF68758A), modifier=Modifier.padding(top=6.dp,bottom=24.dp))
            OutlinedTextField(login,{login=it},Modifier.fillMaxWidth(),label={Text("Phone or email")},singleLine=true,leadingIcon={Icon(Icons.Default.Person,null)})
            Spacer(Modifier.height(12.dp)); OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),leadingIcon={Icon(Icons.Default.Lock,null)})
            Spacer(Modifier.height(20.dp)); Button(onClick={onLogin(login.trim(),password)},Modifier.fillMaxWidth().height(54.dp),enabled=!busy && login.isNotBlank() && password.isNotBlank(),shape=RoundedCornerShape(16.dp)) { if(busy) CircularProgressIndicator(Modifier.size(20.dp),color=Color.White,strokeWidth=2.dp) else Text("Sign in",fontWeight=FontWeight.Bold) }
            TextButton(onClick=onRegister,Modifier.fillMaxWidth()) { Text("New to JoyMart?  Create an account") }
            if(!message.isNullOrBlank()) AlertDialog(onDismissRequest=onDismiss,confirmButton={TextButton(onClick=onDismiss){Text("OK")}},icon={Icon(Icons.Default.Info,null)},title={Text("Sign in")},text={Text(message)})
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
                localError = e.message ?: "Unable to verify Sponsor ID. Check your connection."
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
                    modifier = Modifier.weight(1f),
                    singleLine = true,
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
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), placeholder = { Text("Enter full name") }, singleLine = true)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("PHONE NUMBER *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), placeholder = { Text("Phone number") }, singleLine = true)
                }
                Column(Modifier.weight(1f)) {
                    Text("EMAIL ADDRESS", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), placeholder = { Text("Email (optional)") }, singleLine = true)
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("PASSWORD *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth(), placeholder = { Text("Min 6 characters") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
                Column(Modifier.weight(1f)) {
                    Text("CONFIRM PASSWORD *", fontSize = 11.sp, color = Color(0xFF53647D), fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), placeholder = { Text("Re-type password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
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
            if (!serverMessage.isNullOrBlank()) {
                Text(serverMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            if (!localError.isNullOrBlank()) {
                Text(localError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
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

@Composable fun AuthBackdrop(content:@Composable ColumnScope.()->Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF4F8),Color(0xFFF8FAFD))))) { Column(Modifier.fillMaxSize().padding(top=32.dp),content=content) }
}

@Composable fun BrandMark(){ Row(verticalAlignment=Alignment.CenterVertically){ Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF102447)),contentAlignment=Alignment.Center){Text("J",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Black)}; Spacer(Modifier.width(10.dp)); Column{Text("JOYMART",fontWeight=FontWeight.Black,color=Color(0xFF102447),letterSpacing=1.5.sp);Text("LLP BUSINESS PLATFORM",fontSize=9.sp,color=Color(0xFF718097),letterSpacing=1.sp)} } }
