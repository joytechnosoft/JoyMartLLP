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
import com.joymart.llp.data.JoyMartApi
import com.joymart.llp.ui.components.friendlyNetworkError
import com.joymart.llp.ui.components.readableServerError
import com.joymart.llp.ui.components.ServerDateTime
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val Navy = Color(0xFF172943)
private val Teal = Color(0xFF087F78)
private data class LedgerRow(val title:String,val subtitle:String,val amount:String,val status:String)
private data class Recipient(val id:Long,val name:String,val memberId:String,val email:String,val phone:String)

private fun walletMoney(raw:String):String = runCatching {
    val symbols=DecimalFormatSymbols(Locale("en","IN"))
    DecimalFormat("#,##,##0.00",symbols).format(BigDecimal(raw.trim()))
}.getOrDefault(raw)

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
    var upiId by remember { mutableStateOf("") }
    var savedUpiId by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var recipients by remember { mutableStateOf<List<Recipient>>(emptyList()) }
    var selected by remember { mutableStateOf<Recipient?>(null) }
    var actionBusy by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        loading=true; error=null
        runCatching { api.getRaw("wallet") }.onSuccess { response ->
            if(response.isSuccessful) {
                val root=response.body()?.asObject(); val data=root?.objectValue("data") ?: root
                balance=data?.string("balance") ?: data?.objectValue("wallet")?.string("available_balance") ?: "0.00"
                val list=data?.array("history") ?: data?.array("wallets") ?: root?.array("wallets")
                rows=list?.mapNotNull { el -> if(!el.isJsonObject)null else {
                    val o=el.asJsonObject; val raw=o.string("amount") ?: o.string("change_amount") ?: "0"
                    LedgerRow(o.string("description") ?: o.string("reason") ?: o.string("type") ?: "Wallet activity",ServerDateTime.format(o.string("created_at")),raw,o.string("status") ?: "—")
                }} ?: emptyList()
            } else error=readableServerError(response.errorBody()?.string(), "Wallet load failed (${response.code()}).")
        }.onFailure { error=friendlyNetworkError(it) }
        loading=false
    }

    LaunchedEffect(action) {
        if(action=="withdraw") {
            runCatching { api.getRaw("wallet/payout-profile") }.onSuccess { r ->
                if(r.isSuccessful) {
                    val d=r.body()?.asObject()?.objectValue("data")
                    savedUpiId=d?.string("upi_id").orEmpty()
                    upiId=savedUpiId
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("My Wallet",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Navy); Text("Balance and wallet activity",fontSize=12.sp,color=Color.Gray) }
            IconButton(onClick={refresh++}) { Icon(Icons.Default.Refresh,"Refresh") }
        }
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Navy)) {
            Column(Modifier.padding(20.dp)) {
                Text("AVAILABLE BALANCE",fontSize=10.sp,color=Color.White.copy(.72f),fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text("₹${walletMoney(balance)}",fontSize=30.sp,color=Color.White,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(16.dp)); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={action="withdraw";amount="";notice=null},colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Navy)) { Icon(Icons.Default.ArrowUpward,null); Spacer(Modifier.width(5.dp)); Text("Withdraw") }
                OutlinedButton(onClick={action="transfer";amount="";selected=null;recipients=emptyList();search="";notice=null},colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)) { Icon(Icons.Default.Send,null); Spacer(Modifier.width(5.dp)); Text("Transfer") }
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
            items(rows) { row ->
                val value=runCatching{BigDecimal(row.amount)}.getOrNull()
                val credit=value?.signum()?.let{it>0}==true
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(row.title,fontWeight=FontWeight.SemiBold,color=Navy,fontSize=13.sp); Text(row.subtitle,fontSize=10.sp,color=Color.Gray); Text(row.status,fontSize=10.sp,color=Teal) }
                        Text((if(credit) "+₹" else "−₹")+walletMoney(value?.abs()?.toPlainString()?:row.amount),fontWeight=FontWeight.Bold,color=if(credit)Teal else Color(0xFFB54740))
                    }
                }
            }
        }
    }

    if(action.isNotBlank()) AlertDialog(
        onDismissRequest={if(!actionBusy)action=""},
        title={Text(if(action=="withdraw")"Withdraw to UPI" else "Wallet transfer")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=amount,onValueChange={amount=it.filter{c->c.isDigit()||c=='.'}.take(12)},label={Text("Amount (₹)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
                if(action=="withdraw") {
                    Text("Your saved UPI ID is shown below. You can edit it or add one if it is missing.",fontSize=11.sp,color=Color.Gray)
                    OutlinedTextField(value=upiId,onValueChange={upiId=it.trim()},label={Text("UPI ID (example: name@bank)")},singleLine=true,modifier=Modifier.fillMaxWidth(),leadingIcon={Icon(Icons.Default.AccountBalance,null)})
                    if(savedUpiId.isNotBlank() && upiId==savedUpiId) Text("Saved UPI ID",fontSize=11.sp,color=Teal)
                    Text("Withdrawal will be submitted for company review. Payment is not instant.",fontSize=11.sp,color=Color.Gray)
                } else {
                    OutlinedTextField(value=search,onValueChange={search=it;selected=null},label={Text("Member ID / phone / email / name")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={
                        IconButton(enabled=!searching && search.trim().length>=2,onClick={
                            scope.launch {
                                searching=true; recipients=emptyList(); notice=null
                                runCatching{api.getRaw("wallet/recipients?search=${java.net.URLEncoder.encode(search.trim(),"UTF-8")}")}.onSuccess{r->
                                    if(r.isSuccessful){val arr=r.body()?.asObject()?.array("users");recipients=arr?.mapNotNull{e->if(e.isJsonObject){val o=e.asJsonObject;Recipient(o.long("id"),o.string("name").orEmpty(),o.string("member_id").orEmpty(),o.string("email").orEmpty(),o.string("phone").orEmpty())}else null}?:emptyList();if(recipients.isEmpty())notice="No matching member found."}
                                    else notice=readableServerError(r.errorBody()?.string(),"Recipient search failed.")
                                }.onFailure{notice=friendlyNetworkError(it)}
                                searching=false
                            }
                        }){if(searching)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Default.Search,"Search members")}
                    })
                    recipients.forEach { r ->
                        Row(Modifier.fillMaxWidth().background(if(selected?.id==r.id)Color(0xFFE7F5F2) else Color(0xFFF5F7FA),RoundedCornerShape(12.dp)).clickable{selected=r}.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(r.name,fontWeight=FontWeight.Bold,fontSize=13.sp,color=Navy); if(r.memberId.isNotBlank())Text("Member ID: ${r.memberId}",fontSize=11.sp,color=Teal); Text(listOf(r.phone,r.email).filter{it.isNotBlank()}.joinToString(" • "),fontSize=10.sp,color=Color.Gray) }
                            if(selected?.id==r.id)Icon(Icons.Default.CheckCircle,null,tint=Teal)
                        }
                    }
                    selected?.let{Text("Transfer to ${it.name}" + it.memberId.takeIf { id -> id.isNotBlank() }?.let { id -> " (Member ID: $id)" }.orEmpty(),fontSize=11.sp,color=Teal,fontWeight=FontWeight.SemiBold)}
                }
                notice?.let{Text(it,fontSize=11.sp,color=Color(0xFFB54740))}
            }
        },
        confirmButton={Button(enabled=!actionBusy && amount.toBigDecimalOrNull()?.let{it>BigDecimal.ZERO}==true && (action!="transfer" || selected!=null) && (action!="withdraw" || upiId.matches(Regex("^[A-Za-z0-9._-]+@[A-Za-z0-9.-]+$"))),onClick={
            scope.launch {
                actionBusy=true;notice=null
                val amountValue=amount.toBigDecimalOrNull()?.toPlainString().orEmpty()
                val result=runCatching {
                    if(action=="withdraw") {
                        val profile=api.postRaw("wallet/payout-profile",mapOf("upi_id" to upiId.trim()))
                        if(!profile.isSuccessful) throw IllegalStateException(readableServerError(profile.errorBody()?.string(),"Could not save UPI ID."))
                        api.postRaw("wallet/withdraw",mapOf("amount" to amountValue,"method" to "upi","upi_id" to upiId.trim()))
                    } else api.postRaw("wallet/transfer",mapOf("amount" to amountValue,"recipient_id" to (selected?.id?:0L)))
                }
                result.onSuccess{r->val obj=r.body()?.asObject();if(r.isSuccessful && obj?.boolean("success")==true){notice=obj.string("message")?:if(action=="withdraw")"Withdrawal request submitted." else "Transfer completed successfully.";action="";refresh++}else notice=readableServerError(r.errorBody()?.string(),obj?.string("message")?:"Request failed (${r.code()}).")}.onFailure{notice=it.message?:friendlyNetworkError(it)}
                actionBusy=false
            }
        }){if(actionBusy)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp)else Text(if(action=="withdraw")"Submit withdrawal" else "Transfer now")}},
        dismissButton={TextButton(enabled=!actionBusy,onClick={action=""}){Text("Cancel")}}
    )
    notice?.takeIf{action.isBlank()}?.let{AlertDialog(onDismissRequest={notice=null},title={Text("Wallet")},text={Text(it)},confirmButton={TextButton(onClick={notice=null}){Text("OK")}})}
}

@Composable
fun TransactionScreen(api:JoyMartApi) {
    var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }; var rows by remember { mutableStateOf<List<LedgerRow>>(emptyList()) }; var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        loading=true;error=null
        runCatching{api.getRaw("transactions")}.onSuccess{r->
            if(r.isSuccessful){val root=r.body()?.asObject();val tx=root?.objectValue("transactions");val arr=tx?.array("data") ?: root?.array("transactions");rows=arr?.mapNotNull{e->if(e.isJsonObject){val o=e.asJsonObject;LedgerRow(o.string("type")?:o.string("description")?:"Transaction",ServerDateTime.format(o.string("created_at")),o.string("amount")?:o.string("change_amount")?:"0",o.string("status")?:"—")}else null}?:emptyList()}
            else error=readableServerError(r.errorBody()?.string(), "Unable to load transactions (${r.code()}).")
        }.onFailure{error=friendlyNetworkError(it)};loading=false
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Transactions",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Navy);Text("Latest account transactions",fontSize=12.sp,color=Color.Gray)};IconButton(onClick={refresh++}){Icon(Icons.Default.Refresh,"Refresh")}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(it,fontSize=12.sp);TextButton(onClick={refresh++}){Text("Retry")}}}}
        if(!loading && error==null && rows.isEmpty())EmptyPanel("No transactions found.")
        LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){items(rows){r->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(r.title,fontWeight=FontWeight.SemiBold,color=Navy,fontSize=13.sp);Text(r.subtitle,fontSize=10.sp,color=Color.Gray);Text(r.status,fontSize=10.sp,color=Teal)};Text("₹${walletMoney(r.amount)}",fontWeight=FontWeight.Bold,color=Navy)}}}}
    }
}

@Composable private fun EmptyPanel(message:String){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Inbox,null,tint=Color.Gray,modifier=Modifier.size(30.dp));Spacer(Modifier.height(7.dp));Text(message,fontSize=12.sp,color=Color.Gray)}}}
private fun JsonElement?.asObject():JsonObject?=if(this!=null&&isJsonObject)asJsonObject else null
private fun JsonObject.string(key:String):String?=get(key)?.takeUnless{it.isJsonNull}?.let{runCatching{it.asString}.getOrElse{it.toString()}}
private fun JsonObject.long(key:String):Long=runCatching{get(key)?.asLong?:0L}.getOrDefault(0L)
private fun JsonObject.boolean(key:String):Boolean=runCatching{get(key)?.asBoolean?:false}.getOrDefault(false)
private fun JsonObject.array(key:String)=get(key)?.takeIf{it.isJsonArray}?.asJsonArray?.toList()
private fun JsonObject.objectValue(key:String)=get(key)?.takeIf{it.isJsonObject}?.asJsonObject
