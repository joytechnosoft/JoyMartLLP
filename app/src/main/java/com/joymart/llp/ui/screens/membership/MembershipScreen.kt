package com.joymart.llp.ui.screens.membership

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.joymart.llp.data.JoyMartApi
import com.joymart.llp.data.UserDto
import com.joymart.llp.ui.components.friendlyNetworkError
import com.joymart.llp.ui.components.readableServerError
import kotlinx.coroutines.launch

private data class MembershipPlan(
    val id: Long,
    val name: String,
    val description: String,
    val amount: Double,
    val renewal: Double,
    val validityDays: Int
)

@Composable
fun MembershipScreen(user: UserDto, api: JoyMartApi, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var plans by remember { mutableStateOf<List<MembershipPlan>>(emptyList()) }
    var activeMembership by remember { mutableStateOf<JsonElement?>(null) }
    var walletBalance by remember { mutableDoubleStateOf(0.0) }
    var selected by remember { mutableStateOf<MembershipPlan?>(null) }
    var method by remember { mutableStateOf("WALLET") }
    var utr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var alert by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }

    suspend fun load() {
        loading = true
        error = null
        try {
            val packageResponse = api.getRaw("membership/packages")
            if (!packageResponse.isSuccessful) {
                error = readableServerError(packageResponse.errorBody()?.string(), "Unable to load membership packages.")
            } else {
                val json = JsonParser.parseString(packageResponse.body()?.toString() ?: "{}")
                walletBalance = json.asJsonObject.get("wallet_balance")?.asDouble ?: 0.0
                val data = json.asJsonObject.get("data")
                plans = if (data != null && data.isJsonArray) data.asJsonArray.mapNotNull { item ->
                    runCatching {
                        val p = item.asJsonObject
                        MembershipPlan(
                            id = p.get("id").asLong,
                            name = p.get("package_name")?.asString ?: "Membership",
                            description = p.get("description")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                            amount = p.get("joining_amount")?.asDouble ?: 0.0,
                            renewal = p.get("renewal_amount")?.asDouble ?: 0.0,
                            validityDays = p.get("validity_days")?.asInt ?: 0
                        )
                    }.getOrNull()
                } else emptyList()
            }
            val currentResponse = api.getRaw("membership/me")
            if (currentResponse.isSuccessful) {
                val currentJson = JsonParser.parseString(currentResponse.body()?.toString() ?: "{}")
                val current = currentJson.asJsonObject.get("data")
                activeMembership = current?.takeUnless { it.isJsonNull }
            } else if (error == null) {
                error = readableServerError(currentResponse.errorBody()?.string(), "Unable to check membership status.")
            }
        } catch (e: Exception) {
            error = friendlyNetworkError(e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF3F6FA))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("Business Membership", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                Text("Plans, activation and payment", fontSize = 10.sp, color = Color.Gray)
            }
            IconButton(onClick = { scope.launch { load() } }) { Icon(Icons.Default.Refresh, "Refresh") }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val isActive = activeMembership != null
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        Modifier.background(
                            Brush.linearGradient(
                                if (isActive) listOf(Color(0xFF168B58), Color(0xFF087F78))
                                else listOf(Color(0xFF263B5D), Color(0xFF53658A))
                            )
                        ).padding(18.dp)
                    ) {
                        Text("MEMBERSHIP STATUS", color = Color.White.copy(.72f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isActive) "ACTIVE" else "INACTIVE", Modifier.weight(1f), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                            Surface(color = Color.White.copy(.18f), shape = RoundedCornerShape(50)) {
                                Text(if (isActive) "Verified" else "Action required", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            if (isActive) "Your membership is active. Thank you."
                            else "Your membership is inactive. Select a plan and activate your account.",
                            color = Color.White.copy(.86f), fontSize = 11.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Column {
                                Text("Available wallet balance", color = Color.White.copy(.72f), fontSize = 9.sp)
                                Text("₹${"%,.2f".format(walletBalance)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1EF)), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFB83B35))
                            Spacer(Modifier.width(8.dp))
                            Text(error.orEmpty(), Modifier.weight(1f), fontSize = 11.sp, color = Color(0xFF91342F))
                            TextButton(onClick = { scope.launch { load() } }) { Text("Retry") }
                        }
                    }
                }
            }

            if (activeMembership != null) {
                item {
                    val current = activeMembership!!.asJsonObject
                    val packageObj = current.get("package")?.takeIf { it.isJsonObject }?.asJsonObject
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("CURRENT MEMBERSHIP", fontSize = 9.sp, color = Color(0xFF16865B), fontWeight = FontWeight.Bold)
                            Text(packageObj?.get("package_name")?.asString ?: "Business Membership", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                            InfoPair("Joining amount", "₹${current.get("joining_amount")?.asString ?: "—"}")
                            InfoPair("Start date", current.get("start_date")?.asString?.take(10) ?: "—")
                            InfoPair("Expiry date", current.get("expiry_date")?.asString?.take(10) ?: "—")
                        }
                    }
                }
            } else {
                item {
                    Column {
                        Text("Membership Plans", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                        Text("Choose a plan to activate your business account.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                if (loading && plans.isEmpty()) {
                    items(2) {
                        Card(Modifier.fillMaxWidth().height(128.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.fillMaxWidth(.55f).height(15.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8EDF3)))
                                Box(Modifier.fillMaxWidth(.85f).height(10.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF0F3F7)))
                                Box(Modifier.fillMaxWidth(.35f).height(22.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8EDF3)))
                            }
                        }
                    }
                }
                items(plans, key = { it.id }) { plan ->
                    val isSelected = selected?.id == plan.id
                    Card(
                        Modifier.fillMaxWidth().clickable { selected = plan },
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) Color(0xFF3975E8) else Color(0xFFE1E7EF)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(plan.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                                    Text(plan.description.ifBlank { "Business membership access for JOYSMART Core." }, fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2873D8))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("₹${"%,.2f".format(plan.amount)}", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16865B))
                                Spacer(Modifier.width(8.dp))
                                Text("${plan.validityDays} days validity", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            OutlinedButton(onClick = { selected = plan }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (isSelected) "Selected plan" else "Select this plan")
                            }
                        }
                    }
                }

                if (selected != null) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Payment method", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PaymentChoice("Wallet", "Instant activation", method == "WALLET", Modifier.weight(1f)) { method = "WALLET" }
                                    PaymentChoice("UPI", "Admin approval", method == "UPI", Modifier.weight(1f)) { method = "UPI" }
                                }
                                if (method == "WALLET") {
                                    Text("Wallet balance: ₹${"%,.2f".format(walletBalance)}", fontSize = 11.sp, color = Color(0xFF16865B), fontWeight = FontWeight.SemiBold)
                                    if (walletBalance < selected!!.amount) {
                                        Text("Insufficient wallet balance. Add funds to your wallet or choose UPI.", fontSize = 11.sp, color = Color(0xFFB45309))
                                    }
                                } else {
                                    Surface(color = Color(0xFFEAF4FF), shape = RoundedCornerShape(13.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("UPI PAYMENT", fontSize = 10.sp, color = Color(0xFF245AA8), fontWeight = FontWeight.Bold)
                                            Text("Complete the UPI payment for ₹${"%,.2f".format(selected!!.amount)} and enter the UTR / transaction reference below. Membership will activate after admin approval.", fontSize = 10.sp, color = Color(0xFF3F5C7E))
                                        }
                                    }
                                    OutlinedTextField(
                                        value = utr,
                                        onValueChange = { utr = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Transaction / UTR number") },
                                        placeholder = { Text("Enter UTR / transaction number") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val plan = selected!!
                                        if (method == "WALLET" && walletBalance < plan.amount) {
                                            alert = Triple("Insufficient wallet balance", "Your wallet balance is not sufficient for this membership. Add funds or choose UPI.", false)
                                        } else if (method == "UPI" && utr.isBlank()) {
                                            alert = Triple("UTR required", "Please enter your UPI transaction / UTR number.", false)
                                        } else {
                                            scope.launch {
                                                submitting = true
                                                try {
                                                    val payload = mapOf<String, Any?>(
                                                        "membership_package_id" to plan.id,
                                                        "payment_method" to method,
                                                        "transaction_id" to if (method == "UPI") utr.trim() else null
                                                    )
                                                    val response = api.postRaw("membership/purchase", payload)
                                                    val json = response.body()
                                                    val success = response.isSuccessful && json?.isJsonObject == true && json.asJsonObject.get("success")?.asBoolean == true
                                                    if (success) {
                                                        val status = json!!.asJsonObject.get("status")?.asString ?: "PENDING"
                                                        val message = json.asJsonObject.get("message")?.asString
                                                            ?: if (status == "ACTIVE") "Membership activated successfully." else "UPI payment submitted. Waiting for admin approval."
                                                        alert = Triple(if (status == "ACTIVE") "Membership activated" else "Payment submitted", message, true)
                                                        utr = ""
                                                        selected = null
                                                        load()
                                                    } else {
                                                        val message = readableServerError(response.errorBody()?.string() ?: json?.asJsonObject?.get("message")?.asString, "Membership activation failed.")
                                                        alert = Triple("Activation failed", message, false)
                                                    }
                                                } catch (e: Exception) {
                                                    alert = Triple("Connection problem", friendlyNetworkError(e), false)
                                                } finally { submitting = false }
                                            }
                                        }
                                    },
                                    enabled = !submitting && (method != "WALLET" || walletBalance >= selected!!.amount),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(13.dp)
                                ) {
                                    if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    else {
                                        Icon(Icons.Default.Bolt, null)
                                        Spacer(Modifier.width(7.dp))
                                        Text(if (method == "WALLET") "Activate with Wallet" else "Submit UPI Payment")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    alert?.let { (title, message, success) ->
        AlertDialog(
            onDismissRequest = { alert = null },
            icon = { Icon(if (success) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (success) Color(0xFF16865B) else Color(0xFFB7791F)) },
            title = { Text(title) },
            text = { Text(message, fontSize = 13.sp) },
            confirmButton = { TextButton(onClick = { alert = null }) { Text("OK") } },
            shape = RoundedCornerShape(22.dp)
        )
    }
}

@Composable
private fun PaymentChoice(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier.clickable(onClick = onClick),
        color = if (selected) Color(0xFFEAF2FF) else Color(0xFFF7F9FC),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Color(0xFF3975E8) else Color(0xFFE1E7EF))
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
            Text(subtitle, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, color = Color(0xFF172943), fontWeight = FontWeight.SemiBold)
    }
}
