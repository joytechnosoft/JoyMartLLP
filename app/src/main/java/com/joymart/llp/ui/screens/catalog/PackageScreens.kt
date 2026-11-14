package com.joymart.llp.ui.screens.catalog

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

@Composable fun VrindabanPackagesScreen(api:JoyMartApi) {
    var items by remember{mutableStateOf<List<PackageDto>>(emptyList())}; var loading by remember{mutableStateOf(true)}; var error by remember{mutableStateOf<String?>(null)}; var showCreate by remember{mutableStateOf(false)}; var notice by remember{mutableStateOf<String?>(null)}
    val scope= rememberCoroutineScope()
    suspend fun refresh(){runCatching{api.packages()}.onSuccess{r->if(r.isSuccessful)items=r.body()?.data?: emptyList() else error=r.errorBody()?.string()}.onFailure{error=it.message};loading=false}
    LaunchedEffect(Unit){refresh()}
    Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Vrindaban packages",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Package catalogue and pricing",color=Color.Gray,fontSize=12.sp)};Button(onClick={showCreate=true}){Icon(Icons.Default.Add,null);Text("Create")}};Spacer(Modifier.height(12.dp));if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error);LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(items.size){i->val p=items[i];Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Text(p.name,fontWeight=FontWeight.Bold,fontSize=16.sp);Text(p.code,color=Color.Gray,fontSize=11.sp);Spacer(Modifier.height(6.dp));Text("₹${p.price}",fontWeight=FontWeight.Bold,color=Color(0xFF087F78),fontSize=19.sp);Text("${p.status ?: "—"}  •  Cost ₹${p.totalCost ?: "—"}  •  Commission pool ₹${p.commissionPool ?: "—"}",fontSize=11.sp,color=Color.Gray)}}}}}
    if(showCreate) CreatePackageDialog(api,{showCreate=false},{msg->notice=msg;showCreate=false;scope.launch{refresh()}})
    notice?.let{AlertDialog(onDismissRequest={notice=null},icon={Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF0A987D))},title={Text("Success")},text={Text(it)},confirmButton={TextButton(onClick={notice=null}){Text("Done")}})}
}

@Composable fun CreatePackageDialog(api:JoyMartApi,onClose:()->Unit,onDone:(String)->Unit) {
    var code by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var price by remember{mutableStateOf("")};var productIds by remember{mutableStateOf("")};var quantity by remember{mutableStateOf("1")};var pool by remember{mutableStateOf("50")};var validity by remember{mutableStateOf("365")};var membershipIds by remember{mutableStateOf("")};var softwareIds by remember{mutableStateOf("")};var levelPercents by remember{mutableStateOf("")};var status by remember{mutableStateOf("DRAFT")};var saving by remember{mutableStateOf(false)};var err by remember{mutableStateOf<String?>(null)};val scope= rememberCoroutineScope()
    AlertDialog(onDismissRequest=onClose,title={Text("Create package")},text={Column(Modifier.verticalScroll(rememberScrollState())){OutlinedTextField(code,{code=it},label={Text("Package code")});OutlinedTextField(name,{name=it},label={Text("Package name")});OutlinedTextField(price,{price=it},label={Text("Package price")});OutlinedTextField(productIds,{productIds=it},label={Text("Product IDs (comma separated)")});OutlinedTextField(quantity,{quantity=it},label={Text("Quantities in same order (comma separated)")});OutlinedTextField(pool,{pool=it},label={Text("Commission pool % (default 50)")});OutlinedTextField(validity,{validity=it},label={Text("Validity days")});OutlinedTextField(membershipIds,{membershipIds=it},label={Text("Membership package IDs (comma separated)")});OutlinedTextField(softwareIds,{softwareIds=it},label={Text("Software catalog IDs (comma separated)")});OutlinedTextField(levelPercents,{levelPercents=it},label={Text("Commission levels % (comma separated)")});OutlinedTextField(status,{status=it.uppercase()},label={Text("Status: DRAFT / ACTIVE / INACTIVE")});if(err!=null)Text(err!!,color=MaterialTheme.colorScheme.error,fontSize=11.sp)}},confirmButton={TextButton(enabled=!saving,onClick={val p=price.toDoubleOrNull();val ids=productIds.split(",").mapNotNull{it.trim().toLongOrNull()};val quantities=quantity.split(",").mapNotNull{it.trim().toDoubleOrNull()};val poolValue=pool.toDoubleOrNull();val days=validity.toIntOrNull();val memberships=membershipIds.split(",").mapNotNull{it.trim().toLongOrNull()};val softwares=softwareIds.split(",").mapNotNull{it.trim().toLongOrNull()};val levels=levelPercents.split(",").mapIndexedNotNull{index,value->value.trim().toDoubleOrNull()?.let{(index+1).toString() to it}}.toMap();if(code.isBlank()||name.isBlank()||p==null||ids.isEmpty()||quantities.size!=ids.size||poolValue==null||days==null||status !in listOf("DRAFT","ACTIVE","INACTIVE")){err="Check required values, product/quantity pairs and package status"}else{saving=true;scope.launch{try{val packageItems=ids.mapIndexed{index,productId->NativePackageItem(productId,quantities[index])};val r=api.createPackage(NativePackageRequest(code,name,null,p,p,days,poolValue,memberships.isNotEmpty(),status,packageItems,softwares,emptyMap(),memberships,levels));if(r.isSuccessful)onDone("Package created")else err=r.errorBody()?.string()?:"Unable to create package"}catch(e:Exception){err=e.message};saving=false}}}){Text(if(saving)"Saving…" else "Save package")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
