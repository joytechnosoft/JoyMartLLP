package com.joymart.llp.ui.screens.catalog

import com.joymart.llp.ui.components.friendlyNetworkError
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

@Composable fun VrindabanProductsScreen(api:JoyMartApi) {
    var products by remember{mutableStateOf<List<ProductDto>>(emptyList())};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf<String?>(null)};var showCreate by remember{mutableStateOf(false)};var notice by remember{mutableStateOf<String?>(null)};val scope= rememberCoroutineScope()
    suspend fun refresh(){runCatching{api.products()}.onSuccess{r->if(r.isSuccessful)products=r.body()?.data?:emptyList() else error=r.errorBody()?.string()}.onFailure{error=friendlyNetworkError(it)};loading=false}
    LaunchedEffect(Unit){refresh()}
    Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Vrindaban products",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("Products and stock overview",color=Color.Gray,fontSize=12.sp)};Button(onClick={showCreate=true}){Icon(Icons.Default.Add,null);Text("Add")}};Spacer(Modifier.height(12.dp));if(loading)LinearProgressIndicator();error?.let{Text(it,color=MaterialTheme.colorScheme.error)};LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(products.size){i->val p=products[i];Card(shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text(p.code+" • "+(p.unit?:"PCS"),fontSize=11.sp,color=Color.Gray)};Column(horizontalAlignment=Alignment.End){Text("₹${p.sellPrice?:"0"}",fontWeight=FontWeight.Bold);Text("Stock ${p.stockQty?:"0"}",fontSize=11.sp,color=Color.Gray)}}}}}}
    if(showCreate) CreateProductDialog(api,{showCreate=false},{msg->notice=msg;showCreate=false;scope.launch{refresh()}})
    notice?.let{AlertDialog(onDismissRequest={notice=null},icon={Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF0A987D))},title={Text("Success")},text={Text(it)},confirmButton={TextButton(onClick={notice=null}){Text("Done")}})}
}

@Composable fun CreateProductDialog(api:JoyMartApi,onClose:()->Unit,onDone:(String)->Unit){
    var code by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var unit by remember{mutableStateOf("PCS")};var base by remember{mutableStateOf("0")};var mrp by remember{mutableStateOf("0")};var sell by remember{mutableStateOf("0")};var stock by remember{mutableStateOf("0")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)};val scope= rememberCoroutineScope()
    AlertDialog(onDismissRequest=onClose,title={Text("Add Vrindaban product")},text={Column(Modifier.verticalScroll(rememberScrollState())){OutlinedTextField(code,{code=it},label={Text("Product code")});OutlinedTextField(name,{name=it},label={Text("Product name")});OutlinedTextField(unit,{unit=it},label={Text("Unit")});OutlinedTextField(base,{base=it},label={Text("Base cost")});OutlinedTextField(mrp,{mrp=it},label={Text("MRP")});OutlinedTextField(sell,{sell=it},label={Text("Selling price")});OutlinedTextField(stock,{stock=it},label={Text("Opening stock")});error?.let{Text(it,color=MaterialTheme.colorScheme.error,fontSize=11.sp)}}},confirmButton={TextButton(enabled=!busy,onClick={val b=base.toDoubleOrNull();val m=mrp.toDoubleOrNull();val s=sell.toDoubleOrNull();val st=stock.toDoubleOrNull();if(code.isBlank()||name.isBlank()||b==null||m==null||s==null||st==null)error="Complete all fields with valid numbers" else{busy=true;scope.launch{try{val r=api.createProduct(NativeProductRequest(code,name,null,null,unit,b,m,s,st,5.0));if(r.isSuccessful)onDone("Product created")else error=r.errorBody()?.string()?:"Unable to create product"}catch(e:Exception){error=friendlyNetworkError(e)};busy=false}}}){Text(if(busy)"Saving…" else "Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
