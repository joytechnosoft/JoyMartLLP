
package com.joymart.llp.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joymart.llp.data.JoyMartApi
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

private val financeNavy = Color(0xFF172943)
private val financeGreen = Color(0xFF12845C)
private val financeBlue = Color(0xFF3475DC)
private val financeBg = Color(0xFFF3F6FA)

private fun JsonObject.str(key: String, fallback: String = ""): String =
    get(key)?.takeUnless { it.isJsonNull }?.let {
        runCatching { it.asString }.getOrDefault(it.toString())
    } ?: fallback

private fun JsonObject.money(key: String): Double =
    get(key)?.takeUnless { it.isJsonNull }
        ?.let { runCatching { it.asDouble }.getOrDefault(0.0) } ?: 0.0

private fun JsonObject.obj(key: String): JsonObject? =
    get(key)?.takeIf { it.isJsonObject }?.asJsonObject

private fun JsonObject.array(key: String): JsonArray =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()

private fun JsonElement.asObjectOrNull(): JsonObject? =
    takeIf { it.isJsonObject }?.asJsonObject

private fun formatMoney(value: Double): String =
    "₹%,.2f".format(value)

@Composable
private fun FinanceHeader(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    refreshing: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = financeNavy)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            if (refreshing) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, "Refresh", tint = financeBlue)
            }
        }
    }
}

@Composable
private fun FinanceSummaryCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                amount,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = financeNavy
            )
            Text(subtitle, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun FinanceError(message: String, retry: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0EF)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(message, color = Color(0xFFB42318), fontSize = 12.sp)
            TextButton(onClick = retry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun FinanceLoading() {
    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Card(
                Modifier.fillMaxWidth().height(76.dp),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    Modifier.fillMaxSize().padding(15.dp)
                        .background(Color(0xFFF0F3F7), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
fun EarningsScreen(api: JoyMartApi) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var commissions by remember { mutableStateOf<List<JsonObject>>(emptyList()) }

    var totalEarned by remember { mutableDoubleStateOf(0.0) }
    var membershipEarned by remember { mutableDoubleStateOf(0.0) }
    var softwareEarned by remember { mutableDoubleStateOf(0.0) }

    var filter by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<JsonObject?>(null) }

    suspend fun loadEarnings() {
        loading = true
        error = null

        try {
            val response = api.getRaw("earnings")

            if (!response.isSuccessful) {
                error = "Unable to load earnings. Please try again."
            } else {
                val json = JsonParser.parseString(
                    response.body()?.toString() ?: "{}"
                ).asJsonObject

                if (json.get("success")?.asBoolean != true) {
                    error = json.str("message", "Unable to load earnings.")
                } else {
                    val data = json.obj("data") ?: JsonObject()

                    totalEarned = data.money("total_earned")
                    membershipEarned = data.money("membership_earned")
                    softwareEarned = data.money("software_earned")

                    commissions = data.array("commissions").mapNotNull {
                        it.asObjectOrNull()
                    }
                }
            }
        } catch (e: Exception) {
            error = "Network connection problem. Check your internet and retry."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadEarnings()
    }

    val filtered = commissions.filter { item ->
        val membership = item.get("is_membership")
            ?.takeUnless { it.isJsonNull }?.asBoolean ?: false

        val typeMatch = when (filter) {
            "MEMBERSHIP" -> membership
            "SOFTWARE" -> !membership
            else -> true
        }

        val query = search.trim().lowercase()
        val textMatch = query.isBlank() ||
                listOf(
                    item.str("source_name"),
                    item.str("source_member_id"),
                    item.str("remarks"),
                    item.str("software_order_no"),
                    item.str("commission_rate_role")
                ).any { it.lowercase().contains(query) }

        typeMatch && textMatch
    }

    Column(
        Modifier.fillMaxSize().background(financeBg)
    ) {
        FinanceHeader(
            "My Earnings",
            "Membership and software commission history",
            { scope.launch { loadEarnings() } },
            loading
        )

        if (loading && commissions.isEmpty()) {
            FinanceLoading()
        } else if (error != null && commissions.isEmpty()) {
            FinanceError(error!!) {
                scope.launch { loadEarnings() }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 15.dp, end = 15.dp, bottom = 25.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = financeNavy
                        )
                    ) {
                        Column(Modifier.padding(19.dp)) {
                            Text(
                                "TOTAL EARNINGS",
                                color = Color.White.copy(.7f),
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                formatMoney(totalEarned),
                                color = Color.White,
                                fontSize = 29.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "${commissions.size} paid commission records",
                                color = Color.White.copy(.72f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.weight(1f)) {
                            FinanceSummaryCard(
                                "Membership",
                                formatMoney(membershipEarned),
                                "Joining commissions",
                                Icons.Default.Groups,
                                financeGreen
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            FinanceSummaryCard(
                                "Software",
                                formatMoney(softwareEarned),
                                "Subscription commissions",
                                Icons.Default.Apps,
                                financeBlue
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search member, order or remarks") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(13.dp)
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(
                            "ALL" to "All",
                            "MEMBERSHIP" to "Membership",
                            "SOFTWARE" to "Software"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Commission history",
                            Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = financeNavy
                        )
                        Text(
                            "${filtered.size} records",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(25.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(35.dp)
                                )
                                Text(
                                    "No earnings found",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Paid commission records will appear here.",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                items(filtered, key = { it.str("id") }) { item ->
                    val membership = item.get("is_membership")
                        ?.takeUnless { it.isJsonNull }?.asBoolean ?: false

                    Card(
                        Modifier.fillMaxWidth().clickable {
                            selected = item
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (membership)
                                        Color(0xFFE5F5EF)
                                    else Color(0xFFEAF1FF)
                                ) {
                                    Icon(
                                        if (membership) Icons.Default.Groups
                                        else Icons.Default.Apps,
                                        null,
                                        modifier = Modifier.padding(9.dp).size(19.dp),
                                        tint = if (membership) financeGreen else financeBlue
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.str("source_name", "Commission"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = financeNavy
                                    )
                                    Text(
                                        item.str("source_member_id").let {
                                            if (it.isBlank()) "Source member"
                                            else "Member ID: $it"
                                        },
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    "+${formatMoney(item.money("commission_amount"))}",
                                    color = financeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFF0F2F5))
                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (membership) "MEMBERSHIP" else "SOFTWARE",
                                    fontSize = 9.sp,
                                    color = if (membership) financeGreen else financeBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "Level ${item.str("commission_level", "—")}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                item.str("created_at"),
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Commission details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanceDetail("Source member", item.str("source_name", "—"))
                    FinanceDetail("Member ID", item.str("source_member_id", "—"))
                    FinanceDetail("Phone", item.str("source_phone", "—"))
                    FinanceDetail("Source role", item.str("source_role", "—"))
                    FinanceDetail("Commission level", item.str("commission_level", "—"))
                    FinanceDetail("Rate", "${item.str("percentage", "—")}%")
                    FinanceDetail(
                        "Amount",
                        formatMoney(item.money("commission_amount"))
                    )
                    FinanceDetail("Status", item.str("status", "PAID"))
                    FinanceDetail("Date", item.str("created_at", "—"))
                    FinanceDetail("Remarks", item.str("remarks", "—"))
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}


@Composable
fun NativeTransactionsScreen(api: JoyMartApi) {
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf("WALLET") }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var walletBalance by remember { mutableDoubleStateOf(0.0) }
    var walletHistory by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var businessTransactions by remember { mutableStateOf<List<JsonObject>>(emptyList()) }

    var businessPage by remember { mutableIntStateOf(1) }
    var businessLastPage by remember { mutableIntStateOf(1) }

    var filter by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<JsonObject?>(null) }

    suspend fun loadWallet() {
        loading = true
        error = null
        try {
            val response = api.getRaw("wallet")
            if (!response.isSuccessful) {
                error = "Unable to load wallet history (${response.code()})."
            } else {
                val json = JsonParser.parseString(
                    response.body()?.toString() ?: "{}"
                ).asJsonObject
                if (json.get("success")?.asBoolean != true) {
                    error = json.str("message", "Unable to load wallet history.")
                } else {
                    val data = json.obj("data") ?: JsonObject()
                    walletBalance = data.money("balance")
                    walletHistory = data.array("history").mapNotNull { it.asObjectOrNull() }
                }
            }
        } catch (e: Exception) {
            error = "Network error while loading wallet history. Please retry."
        } finally {
            loading = false
        }
    }

    suspend fun loadBusinessTransactions(reset: Boolean = true) {
        if (reset) {
            loading = true
            error = null
            businessPage = 1
        } else {
            if (loadingMore || businessPage >= businessLastPage) return
            loadingMore = true
        }

        try {
            val requestedPage = if (reset) 1 else businessPage + 1
            val response = api.getRaw("transactions?page=$requestedPage")

            if (!response.isSuccessful) {
                error = "Unable to load business transactions (${response.code()})."
            } else {
                val json = JsonParser.parseString(
                    response.body()?.toString() ?: "{}"
                ).asJsonObject

                if (json.get("success")?.asBoolean != true) {
                    error = json.str("message", "Unable to load transactions.")
                } else {
                    val paginator = json.obj("transactions") ?: JsonObject()
                    val rows = paginator.array("data").mapNotNull { it.asObjectOrNull() }
                    businessPage = paginator.get("current_page")
                        ?.takeUnless { it.isJsonNull }?.asInt ?: requestedPage
                    businessLastPage = paginator.get("last_page")
                        ?.takeUnless { it.isJsonNull }?.asInt ?: businessPage

                    businessTransactions = if (reset) rows
                    else businessTransactions + rows
                }
            }
        } catch (e: Exception) {
            error = "Network error while loading business transactions. Please retry."
        } finally {
            loading = false
            loadingMore = false
        }
    }

    suspend fun refreshCurrent() {
        if (tab == "WALLET") loadWallet()
        else loadBusinessTransactions(reset = true)
    }

    LaunchedEffect(tab) {
        filter = "ALL"
        search = ""
        if (tab == "WALLET") loadWallet()
        else loadBusinessTransactions(reset = true)
    }

    val sourceRows = if (tab == "WALLET") walletHistory else businessTransactions

    val filtered = sourceRows.filter { item ->
        val type = item.str("type").uppercase()
        val status = item.str("status").uppercase()

        val isCredit = when {
            type == "CREDIT" || type == "TRANSFER_CREDIT" -> true
            type == "DEBIT" || type == "TRANSFER_DEBIT" -> false
            else -> item.money("amount") >= 0.0 && type.contains("CREDIT")
        }

        val typeMatch = when (filter) {
            "CREDIT" -> type.contains("CREDIT")
            "DEBIT" -> type.contains("DEBIT")
            "PENDING" -> status == "PENDING"
            "APPROVED" -> status == "APPROVED"
            "REJECTED" -> status == "REJECTED"
            else -> true
        }

        val query = search.trim().lowercase()
        val searchMatch = query.isBlank() || listOf(
            item.str("trx_id"),
            item.str("description"),
            item.str("request_no"),
            item.str("transaction_reference"),
            item.str("reference"),
            item.str("purpose"),
            item.str("type"),
            item.str("status")
        ).any { it.lowercase().contains(query) }

        typeMatch && searchMatch
    }

    Column(Modifier.fillMaxSize().background(financeBg)) {
        FinanceHeader(
            "Transactions",
            if (tab == "WALLET") "Wallet ledger and funding requests"
            else "Business transaction records",
            { scope.launch { refreshCurrent() } },
            loading
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = tab == "WALLET",
                onClick = { tab = "WALLET" },
                label = { Text("Wallet Ledger", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = tab == "BUSINESS",
                onClick = { tab = "BUSINESS" },
                label = { Text("Business Transactions", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.ReceiptLong, null, Modifier.size(16.dp)) }
            )
        }

        if (loading && sourceRows.isEmpty()) {
            FinanceLoading()
        } else if (error != null && sourceRows.isEmpty()) {
            FinanceError(error!!) {
                scope.launch { refreshCurrent() }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 15.dp, end = 15.dp, top = 10.dp, bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                if (tab == "WALLET") {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(21.dp),
                            colors = CardDefaults.cardColors(containerColor = financeNavy)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text(
                                    "AVAILABLE WALLET BALANCE",
                                    color = Color.White.copy(.7f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    formatMoney(walletBalance),
                                    color = Color.White,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "${walletHistory.size} wallet and funding records",
                                    color = Color.White.copy(.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ReceiptLong, null, tint = financeBlue)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Business transactions", fontWeight = FontWeight.Bold, color = financeNavy)
                                    Text(
                                        "Page $businessPage of $businessLastPage",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text("${businessTransactions.size} records", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (tab == "WALLET") "Search transaction ID, UTR or description"
                                else "Search reference, purpose or status"
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(13.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "ALL" to "All",
                            "CREDIT" to "Credit",
                            "DEBIT" to "Debit",
                            "PENDING" to "Pending",
                            "APPROVED" to "Approved",
                            "REJECTED" to "Rejected"
                        ).forEach { (key, label) ->

                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontSize = 10.sp
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tab == "WALLET") "Wallet history" else "Business history",
                            Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = financeNavy
                        )
                        Text("${filtered.size} shown", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(25.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong, null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text("No transactions found", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Records will appear here when available.",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                items(
                    filtered,
                    key = { item ->
                        "${tab}_${item.str("id", item.str("trx_id", item.str("reference")))}"
                    }
                ) { item ->
                    val status = item.str("status", "COMPLETED").uppercase()
                    val type = item.str("type", "TRANSACTION").uppercase()

                    val isCredit = type == "CREDIT" || type == "TRANSFER_CREDIT" ||
                            (tab == "BUSINESS" && type.contains("CREDIT"))
                    val isFunding = item.str("source") == "funding_request" ||
                            type == "FUNDING"

                    val statusColor = when (status) {
                        "APPROVED", "COMPLETED", "SUCCESS", "PAID" -> financeGreen
                        "REJECTED", "FAILED", "CANCELLED" -> Color(0xFFDC4444)
                        else -> Color(0xFFD18A19)
                    }

                    Card(
                        Modifier.fillMaxWidth().clickable { selected = item },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (isCredit) Color(0xFFE4F5ED)
                                    else Color(0xFFFFEEEE)
                                ) {
                                    Icon(
                                        when {
                                            isFunding -> Icons.Default.AccountBalance
                                            isCredit -> Icons.Default.ArrowDownward
                                            else -> Icons.Default.ArrowUpward
                                        },
                                        null,
                                        modifier = Modifier.padding(9.dp).size(19.dp),
                                        tint = if (isCredit) financeGreen else Color(0xFFDC4444)
                                    )
                                }

                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.str(
                                            if (tab == "WALLET") "description" else "purpose",
                                            "Wallet transaction"
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = financeNavy
                                    )
                                    Text(
                                        item.str("trx_id", item.str("request_no", item.str("reference", "—"))),
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    (if (isCredit) "+" else if (isFunding) "" else "−") +
                                            formatMoney(item.money("amount")),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCredit) financeGreen else financeNavy
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFF0F2F5))
                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = statusColor.copy(alpha = .12f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        status,
                                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        color = statusColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    item.str("created_at"),
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }

                            if (isFunding && item.str("transaction_reference").isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "UTR: ${item.str("transaction_reference")}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (tab == "BUSINESS" && businessPage < businessLastPage) {
                    item {
                        OutlinedButton(
                            onClick = {
                                scope.launch { loadBusinessTransactions(reset = false) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loadingMore
                        ) {
                            if (loadingMore) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading...")
                            } else {
                                Text("Load more transactions")
                            }
                        }
                    }
                }

                if (error != null && sourceRows.isNotEmpty()) {
                    item {
                        Text(
                            error!!,
                            color = Color(0xFFB42318),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    selected?.let { item ->
        val isWallet = tab == "WALLET"
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(if (isWallet) "Wallet transaction details" else "Business transaction details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isWallet) {
                        FinanceDetail("Transaction ID", item.str("trx_id", "—"))
                        FinanceDetail("Type", item.str("type", "—"))
                        FinanceDetail("Description", item.str("description", "—"))
                        FinanceDetail("Amount", formatMoney(item.money("amount")))
                        FinanceDetail(
                            "Balance after",
                            item.get("balance_after")?.takeUnless { it.isJsonNull }?.toString() ?: "—"
                        )
                        FinanceDetail("Source", item.str("source", "—"))
                        FinanceDetail("Request number", item.str("request_no", "—"))
                        FinanceDetail("Payment method", item.str("payment_method", "—"))
                        FinanceDetail("UTR / Reference", item.str("transaction_reference", "—"))
                        FinanceDetail("Status", item.str("status", "—"))
                        FinanceDetail("Admin remarks", item.str("admin_remarks", "—"))
                        FinanceDetail("Date", item.str("created_at", "—"))
                    } else {
                        FinanceDetail("Transaction ID", item.str("id", "—"))
                        FinanceDetail("Type", item.str("type", "—"))
                        FinanceDetail("Purpose", item.str("purpose", "—"))
                        FinanceDetail("Amount", formatMoney(item.money("amount")))
                        FinanceDetail("Status", item.str("status", "—"))
                        FinanceDetail("Reference", item.str("reference", "—"))
                        FinanceDetail("Date", item.str("created_at", "—"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}

@Composable
private fun FinanceDetail(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value.ifBlank { "—" },
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = financeNavy,
            modifier = Modifier.weight(1.2f)
        )
    }
}