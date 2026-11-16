package com.joymart.llp.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joymart.llp.data.JoyMartApi
import com.joymart.llp.ui.components.MoneyFormatter
import com.joymart.llp.ui.components.ServerDateTime
import com.joymart.llp.ui.components.friendlyNetworkError
import com.joymart.llp.ui.components.readableServerError
import kotlinx.coroutines.launch

private data class WithdrawalItem(
    val id: Long,
    val userId: Long,
    val memberName: String,
    val memberCode: String,
    val phone: String,
    val email: String,
    val amount: String,
    val upiId: String,
    val status: String,
    val createdAt: String,
    val remarks: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWithdrawalApprovalScreen(api: JoyMartApi) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var rows by remember { mutableStateOf<List<WithdrawalItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("pending") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var busyId by remember { mutableLongStateOf(0L) }
    var selected by remember { mutableStateOf<WithdrawalItem?>(null) }
    var decision by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    LaunchedEffect(filter, refreshKey) {
        loading = rows.isEmpty()
        refreshing = rows.isNotEmpty()
        error = null
        runCatching { api.getRaw("admin/wallet/withdrawals?status=$filter") }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val root = runCatching { JsonParser.parseString(response.body()?.toString() ?: "null") }.getOrNull()
                    rows = parseWithdrawalRows(root)
                } else error = readableServerError(response.errorBody()?.string(), "Unable to load withdrawal requests (${response.code()}).")
            }
            .onFailure { error = friendlyNetworkError(it) }
        loading = false
        refreshing = false
    }

    fun submitDecision(item: WithdrawalItem, approve: Boolean) {
        scope.launch {
            busyId = item.id
            val endpoint = "admin/wallet/withdrawals/${item.id}/${if (approve) "approve" else "reject"}"
            val body = if (remarks.isBlank()) emptyMap() else mapOf("remarks" to remarks.trim())
            runCatching { api.postRaw(endpoint, body) }
                .onSuccess { response ->
                    val payload = runCatching { JsonParser.parseString(response.body()?.toString() ?: "{}").asJsonObject }.getOrNull()
                    if (response.isSuccessful && (payload?.get("success")?.asBoolean == true || payload?.get("status")?.asBoolean == true || response.code() in 200..299)) {
                        snackbar.showSnackbar(payload?.get("message")?.takeIf { it.isJsonPrimitive }?.asString ?: if (approve) "Withdrawal marked as paid." else "Withdrawal rejected and hold released.")
                        selected = null
                        decision = ""
                        remarks = ""
                        refreshKey++
                    } else snackbar.showSnackbar(readableServerError(response.errorBody()?.string(), "Could not update withdrawal request."))
                }
                .onFailure { snackbar.showSnackbar(friendlyNetworkError(it)) }
            busyId = 0L
        }
    }

    Scaffold(
        containerColor = Color(0xFFF3F6FA),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Withdrawal approvals", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                    Text("Review UPI payout requests", fontSize = 12.sp, color = Color(0xFF758399))
                }
                IconButton(onClick = { refreshKey++ }, enabled = !refreshing) {
                    if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, "Refresh", tint = Color(0xFF087F78))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("pending", "paid", "rejected", "all").forEach { state ->
                    FilterChip(selected = filter == state, onClick = { filter = state }, label = { Text(state.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) })
                }
            }
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF087F78))
                repeat(3) {
                    Card(Modifier.fillMaxWidth().padding(top = 10.dp).height(112.dp), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.fillMaxWidth(.45f).height(12.dp))
                            Box(Modifier.fillMaxWidth(.8f).height(10.dp))
                            Box(Modifier.fillMaxWidth(.6f).height(10.dp))
                        }
                    }
                }
            }
            error?.let { message ->
                Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F0))) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Could not load requests", fontWeight = FontWeight.Bold, color = Color(0xFF9F3028))
                        Text(message, fontSize = 12.sp, color = Color(0xFF8F4545))
                        TextButton(onClick = { refreshKey++ }) { Text("Try again") }
                    }
                }
            }
            if (!loading && error == null && rows.isEmpty()) {
                Card(Modifier.fillMaxWidth().padding(top = 14.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16865B), modifier = Modifier.size(38.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No ${filter} withdrawals", fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                        Text("There are no requests in this filter.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.id }) { item ->
                    AnimatedVisibility(true, enter = fadeIn() + slideInVertically { it / 10 }) {
                        WithdrawalCard(item, busyId == item.id, onCall = { number ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.filter { it.isDigit() || it == '+' } }"))) }
                        }, onReview = { selected = item; decision = ""; remarks = "" }, onApprove = { selected = item; decision = "approve"; remarks = "" }, onReject = { selected = item; decision = "reject"; remarks = "" })
                    }
                }
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { if (busyId == 0L) { selected = null; decision = "" } },
            title = { Text(when (decision) { "approve" -> "Confirm payout"; "reject" -> "Reject withdrawal"; else -> "Withdrawal details" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item.memberName.ifBlank { "Member details unavailable" }, fontWeight = FontWeight.Bold)
                    Text("Member ID: ${item.memberCode.ifBlank { "Not available" }}", fontSize = 12.sp)
                    Text("Mobile: ${item.phone.ifBlank { "Not provided" }}", fontSize = 12.sp)
                    if (item.phone.isNotBlank()) TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}"))) } }) { Icon(Icons.Default.Call, null); Text("Call member") }
                    Text("Email: ${item.email.ifBlank { "Not provided" }}", fontSize = 12.sp)
                    Text("Amount: ${MoneyFormatter.inr(item.amount)}", fontWeight = FontWeight.Bold)
                    Text("UPI ID: ${item.upiId.ifBlank { "Not provided" }}", fontSize = 12.sp)
                    Text("Request: WD-${item.id}", fontSize = 11.sp, color = Color.Gray)
                    Text("Submitted: ${ServerDateTime.format(item.createdAt)}", fontSize = 11.sp, color = Color.Gray)
                    if (decision == "reject") OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Rejection reason") }, minLines = 2)
                    if (decision == "approve") Text("Verify that the UPI payment has actually been sent before confirming this action. This marks the request paid; it does not send money automatically.", fontSize = 11.sp, color = Color(0xFF8A5A12))
                }
            },
            confirmButton = {
                when (decision) {
                    "approve" -> Button(enabled = busyId == 0L && item.status.equals("pending", true), onClick = { submitDecision(item, true) }) { if (busyId == item.id) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Confirm paid") }
                    "reject" -> Button(enabled = busyId == 0L && item.status.equals("pending", true), onClick = { submitDecision(item, false) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB74343))) { Text("Confirm reject") }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(enabled = item.status.equals("pending", true), onClick = { decision = "approve" }) { Text("Approve / Paid") }
                        OutlinedButton(enabled = item.status.equals("pending", true), onClick = { decision = "reject" }) { Text("Reject") }
                    }
                }
            },
            dismissButton = { TextButton(enabled = busyId == 0L, onClick = { selected = null; decision = "" }) { Text("Close") } }
        )
    }
}

@Composable
private fun WithdrawalCard(item: WithdrawalItem, busy: Boolean, onCall: (String) -> Unit, onReview: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    val pending = item.status.equals("pending", true)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF0EE)) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFB54740), modifier = Modifier.padding(10.dp).size(20.dp)) }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(item.memberName.ifBlank { "Member details unavailable" }, fontWeight = FontWeight.Bold, color = Color(0xFF172943), fontSize = 14.sp)
                    Text("Member ID: ${item.memberCode.ifBlank { "Not available" }}  •  WD-${item.id}", color = Color.Gray, fontSize = 10.sp)
                }
                Text(MoneyFormatter.inr(item.amount), fontWeight = FontWeight.Bold, color = Color(0xFFB54740), fontSize = 15.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Member ID: ${item.memberCode.ifBlank { "Not available" }}", fontSize = 11.sp, color = Color(0xFF43536B))
            if (item.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mobile: ${item.phone}", Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF43536B))
                    TextButton(onClick = { onCall(item.phone) }) { Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Call") }
                }
            } else Text("Mobile: Not provided", fontSize = 11.sp, color = Color.Gray)
            if (item.email.isNotBlank()) Text("Email: ${item.email}", fontSize = 11.sp, color = Color(0xFF43536B))
            Text("UPI: ${item.upiId.ifBlank { "Not provided" }}", fontSize = 12.sp, color = Color(0xFF43536B))
            Text(ServerDateTime.format(item.createdAt), fontSize = 10.sp, color = Color.Gray)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = if (pending) Color(0xFFFFF3DF) else Color(0xFFE8F5EF)) {
                    Text(item.status.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (pending) Color(0xFF9A650C) else Color(0xFF16865B))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReview) { Text("Details") }
                if (pending) {
                    TextButton(onClick = onReject, enabled = !busy) { Text("Reject", color = Color(0xFFB54740)) }
                    Button(onClick = onApprove, enabled = !busy) { if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp) else Text("Approve") }
                }
            }
        }
    }
}

private fun parseWithdrawalRows(root: JsonElement?): List<WithdrawalItem> {
    if (root == null || root.isJsonNull) return emptyList()
    val data = if (root.isJsonObject && root.asJsonObject.has("data")) root.asJsonObject.get("data") else root
    val obj = data.takeIf { it.isJsonObject }?.asJsonObject
    val array = when {
        data.isJsonArray -> data.asJsonArray
        obj != null -> listOf("withdrawals", "requests", "data").firstNotNullOfOrNull { key -> obj.get(key)?.takeIf { it.isJsonArray }?.asJsonArray }
        else -> null
    } ?: return emptyList()
    return array.mapNotNull { element ->
        if (!element.isJsonObject) return@mapNotNull null
        val o = element.asJsonObject
        fun str(vararg keys: String): String = keys.firstNotNullOfOrNull { key -> o.get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrElse { it.toString().trim('"') } } }.orEmpty()
        val user = o.getAsJsonObject("user")
        fun userStr(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
            o.get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }
                ?: user?.get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }
        }.orEmpty()
        WithdrawalItem(
            id = str("id").toLongOrNull() ?: 0L,
            userId = str("user_id").toLongOrNull() ?: 0L,
            memberName = userStr("user_name", "name", "member_name"),
            memberCode = userStr("member_id", "member_code"),
            phone = userStr("phone", "mobile", "mobile_no"),
            email = userStr("email"),
            amount = str("amount", "requested_amount", "withdrawal_amount").ifBlank { "0.00" },
            upiId = str("upi_id", "payment_upi", "account_number"),
            status = str("status").ifBlank { "pending" },
            createdAt = str("created_at"),
            remarks = str("remarks", "admin_remarks")
        ).takeIf { it.id > 0L }
    }
}
