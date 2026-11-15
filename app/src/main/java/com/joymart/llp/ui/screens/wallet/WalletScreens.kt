package com.joymart.llp.ui.screens.wallet

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
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joymart.llp.data.JoyMartApi
import com.joymart.llp.ui.components.friendlyNetworkError
import com.joymart.llp.ui.components.readableServerError
import kotlinx.coroutines.launch

private val Navy = Color(0xFF172943)
private val Teal = Color(0xFF087F78)
private data class LedgerRow(val title:String,val subtitle:String,val amount:String,val status:String)
private data class Recipient(val id:Long,val name:String,val contact:String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(api: JoyMartApi, onNavigate:(String)->Unit) {
    val scope= rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var balance by remember { mutableStateOf("0.00") }
    var rows by remember { mutableStateOf<List<LedgerRow>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var action by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("bank") }
    var search by remember { mutableStateOf("") }
    var recipients by remember { mutableStateOf<List<Recipient>>(emptyList()) }
    var selected by remember { mutableStateOf<Recipient?>(null) }
    var actionBusy by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        loading=true; error=null
        runCatching { api.getRaw("wallet") }.onSuccess { response ->
            if(response.isSuccessful) {
                val root=response.body()?.asObject()
                if (root?.boolean("success") != true) {
                    error=root?.string("message") ?: "Wallet response was not successful."
                } else {
                    // NativeCoreApiController returns {success:true,data:{balance,history,...}}.
                    val data=root.objectValue("data")
                    if (data == null || data.get("balance") == null || data.get("balance").isJsonNull) {
                        error="Wallet balance is unavailable. Please retry."
                    } else {
                        val amount = runCatching { data.get("balance").asBigDecimal }.getOrNull()
                        balance = amount?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString() ?: "—"
                        rows=data.array("history")?.mapNotNull { el ->
                            if(!el.isJsonObject) return@mapNotNull null
                            val o=el.asJsonObject
                            val type=o.string("type")?.uppercase().orEmpty()
                            val value=runCatching { o.get("amount")?.asBigDecimal?.abs() }.getOrNull()
                            val signed=when {
                                type.contains("DEBIT") || type.contains("WITHDRAW") -> "−₹${value?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString() ?: "0.00"}"
                                type.contains("CREDIT") -> "+₹${value?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString() ?: "0.00"}"
                                else -> "₹${value?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString() ?: "0.00"}"
                            }
                            LedgerRow(o.string("description") ?: type.ifBlank { "Wallet activity" }, o.string("created_at") ?: "", signed, o.string("status") ?: "—")
                        } ?: emptyList()
                    }
                }
            } else error=readableServerError(response.errorBody()?.string(), "Wallet load failed (${response.code()}).")
        }.onFailure { error=friendlyNetworkError(it) }
        loading=false
    }

    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("My Wallet",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Navy); Text("Balance and wallet activity",fontSize=12.sp,color=Color.Gray) }
            IconButton(onClick={refresh++}) { Icon(Icons.Default.Refresh,"Refresh") }
        }
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Navy)) {
            Column(Modifier.padding(20.dp)) {
                Text("AVAILABLE BALANCE (API REPORTED)",fontSize=10.sp,color=Color.White.copy(.72f),fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text(if(balance=="—") "Balance unavailable" else "₹ $balance",fontSize=30.sp,color=Color.White,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(16.dp)); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={action="withdraw";amount=""},colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Navy)) { Icon(Icons.Default.ArrowUpward,null); Spacer(Modifier.width(5.dp)); Text("Withdraw") }
                OutlinedButton(onClick={action="transfer";amount="";selected=null;recipients=emptyList()},colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)) { Icon(Icons.Default.Send,null); Spacer(Modifier.width(5.dp)); Text("Transfer") }
            }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Recent wallet activity",Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.Bold,color=Navy)
            TextButton(onClick={onNavigate("wallet-transactions")}) { Text("Transactions") }
        }
        if(error!=null) Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF2F0))) { Column(Modifier.padding(14.dp)) { Text("Unable to load wallet",fontWeight=FontWeight.Bold,color=Color(0xFF9F3028)); Text(error!!,fontSize=12.sp); TextButton(onClick={refresh++}){Text("Retry")} } }
        else if(!loading && rows.isEmpty()) EmptyPanel("No wallet entries found yet.")
        LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            items(rows) { row -> Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) { Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(row.title,fontWeight=FontWeight.SemiBold,color=Navy,fontSize=13.sp); Text(row.subtitle,fontSize=10.sp,color=Color.Gray); Text(row.status,fontSize=10.sp,color=Teal) }; Text("₹ ${row.amount}",fontWeight=FontWeight.Bold,color=if(row.amount.startsWith("-"))Color(0xFFB54740) else Teal) } } }
        }
    }

    if(action.isNotBlank()) AlertDialog(onDismissRequest={if(!actionBusy)action=""},title={Text(if(action=="withdraw")"Withdraw request" else "Wallet transfer")},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value=amount,onValueChange={amount=it},label={Text("Amount (₹)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
            if(action=="withdraw") {
                Text("Withdrawal requests require an active membership. The server will validate eligibility.",fontSize=11.sp,color=Color.Gray)
                Text("Payout method",fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ FilterChip(selected=method=="bank",onClick={method="bank"},label={Text("Bank")}); FilterChip(selected=method=="upi",onClick={method="upi"},label={Text("UPI")}) }
            } else {
                OutlinedTextField(value=search,onValueChange={search=it},label={Text("Search recipient by name / email / mobile")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={IconButton(onClick={
                    scope.launch { runCatching{api.getRaw("wallet/recipients?search=${java.net.URLEncoder.encode(search,"UTF-8")}")}.onSuccess{r-> if(r.isSuccessful){val arr=r.body()?.asObject()?.array("users");recipients=arr?.mapNotNull{e->if(e.isJsonObject){val o=e.asJsonObject;Recipient(o.long("id"),o.string("name")?:"",o.string("email")?:o.string("mobile")?:"")}else null}?: emptyList()}else notice=r.errorBody()?.string()?.take(250)?:"Recipient search failed"}.onFailure{notice=friendlyNetworkError(it)} }
                }){Icon(Icons.Default.Search,"Search recipients")}})
                recipients.forEach { r -> Row(Modifier.fillMaxWidth().background(if(selected?.id==r.id)Color(0xFFE7F5F2) else Color.Transparent,RoundedCornerShape(10.dp)).clickable{selected=r}.padding(9.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(r.name,fontWeight=FontWeight.SemiBold,fontSize=12.sp);Text(r.contact,fontSize=10.sp,color=Color.Gray)};if(selected?.id==r.id)Icon(Icons.Default.CheckCircle,contentDescription="Selected",tint=Teal)} }
                if(selected!=null) Text("Selected: ${selected!!.name}",fontSize=11.sp,color=Teal)
            }
            notice?.let{Text(it,fontSize=11.sp,color=Color(0xFFB54740))}
        }
    },confirmButton={Button(enabled=!actionBusy && amount.toDoubleOrNull()?.let{it>0}==true && (action!="transfer" || selected!=null),onClick={
        scope.launch {
            actionBusy=true;notice=null
            val body=if(action=="withdraw") mapOf("amount" to (amount.toDoubleOrNull()?:0.0),"method" to method) else mapOf("amount" to (amount.toDoubleOrNull()?:0.0),"recipient_id" to (selected?.id?:0L))
            runCatching{api.postRaw(if(action=="withdraw")"wallet/withdraw" else "wallet/transfer",body)}.onSuccess{r->
                val obj=r.body()?.asObject();val ok=r.isSuccessful && (obj?.boolean("success")==true)
                if(ok){notice=obj?.string("message")?:if(action=="withdraw")"Withdrawal request submitted." else "Transfer completed.";action="";refresh++}
                else notice=obj?.string("message")?:r.errorBody()?.string()?.take(250)?:"Request failed (${r.code()})."
            }.onFailure{notice=friendlyNetworkError(it)}
            actionBusy=false
        }
    }){if(actionBusy)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp) else Text("Submit")}},dismissButton={TextButton(enabled=!actionBusy,onClick={action=""}){Text("Cancel")}})
    notice?.takeIf{action.isBlank()}?.let{AlertDialog(onDismissRequest={notice=null},title={Text("Wallet")},text={Text(it)},confirmButton={TextButton(onClick={notice=null}){Text("OK")}})}
}

@Composable
fun TransactionScreen(api:JoyMartApi) {
    var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }; var rows by remember { mutableStateOf<List<LedgerRow>>(emptyList()) }; var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        loading=true;error=null
        runCatching{api.getRaw("transactions")}.onSuccess{r->
            if(r.isSuccessful){val root=r.body()?.asObject();val tx=root?.objectValue("transactions");val arr=tx?.array("data") ?: root?.array("transactions");rows=arr?.mapNotNull{e->if(e.isJsonObject){val o=e.asJsonObject;LedgerRow(o.string("type")?:o.string("description")?:"Transaction",o.string("created_at")?:"",o.string("amount")?:o.string("change_amount")?:"0",o.string("status")?:"—")}else null}?:emptyList()}
            else error=readableServerError(r.errorBody()?.string(), "Unable to load transactions (${r.code()}).")
        }.onFailure{error=friendlyNetworkError(it)};loading=false
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Transactions",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Navy);Text("Latest account transactions",fontSize=12.sp,color=Color.Gray)};IconButton(onClick={refresh++}){Icon(Icons.Default.Refresh,"Refresh")}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(it,fontSize=12.sp);TextButton(onClick={refresh++}){Text("Retry")}}}}
        if(!loading && error==null && rows.isEmpty())EmptyPanel("No transactions found.")
        LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){items(rows){r->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.SemiBold,color=Navy,fontSize=13.sp);Text(r.subtitle,fontSize=10.sp,color=Color.Gray);Text(r.status,fontSize=10.sp,color=Teal)};Text("₹ ${r.amount}",fontWeight=FontWeight.Bold,color=Navy)}}}}
    }
}

@Composable private fun EmptyPanel(message:String){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Inbox,null,tint=Color.Gray,modifier=Modifier.size(30.dp));Spacer(Modifier.height(7.dp));Text(message,fontSize=12.sp,color=Color.Gray)}}}
private fun JsonElement?.asObject():JsonObject?=if(this!=null&&isJsonObject)asJsonObject else null
private fun JsonObject.string(key:String):String?=get(key)?.takeUnless{it.isJsonNull}?.let{runCatching{it.asString}.getOrElse{it.toString()}}
private fun JsonObject.long(key:String):Long=runCatching{get(key)?.asLong?:0L}.getOrDefault(0L)
private fun JsonObject.boolean(key:String):Boolean=runCatching{get(key)?.asBoolean?:false}.getOrDefault(false)
private fun JsonObject.array(key:String)=get(key)?.takeIf{it.isJsonArray}?.asJsonArray?.toList()
private fun JsonObject.objectValue(key:String)=get(key)?.takeIf{it.isJsonObject}?.asJsonObject
