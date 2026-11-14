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

@Composable fun VrindabanCategoriesScreen(api:JoyMartApi){
    var cats by remember{mutableStateOf<List<CategoryDto>>(emptyList())};var showCreate by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)};var notice by remember{mutableStateOf<String?>(null)};val scope= rememberCoroutineScope()
    LaunchedEffect(Unit){runCatching{api.categories()}.onSuccess{if(it.isSuccessful)cats=it.body()?.data?:emptyList() else error=it.errorBody()?.string()}.onFailure{error=it.message}}
    Column(Modifier.fillMaxSize().padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Vrindaban categories",Modifier.weight(1f),fontSize=22.sp,fontWeight=FontWeight.Bold);Button(onClick={showCreate=true}){Icon(Icons.Default.Add,null);Text("Add")}};error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Spacer(Modifier.height(12.dp));cats.forEach{Card(Modifier.fillMaxWidth().padding(bottom=8.dp),shape=RoundedCornerShape(16.dp)){Text(it.name,Modifier.padding(16.dp),fontWeight=FontWeight.SemiBold)}}}
    if(showCreate) CreateCategoryDialog(api,{showCreate=false},{name->scope.launch{runCatching{api.categories()}.onSuccess{if(it.isSuccessful)cats=it.body()?.data?:cats}};notice="Category created";showCreate=false})
    notice?.let{AlertDialog(onDismissRequest={notice=null},icon={Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF0A987D))},title={Text("Success")},text={Text(it)},confirmButton={TextButton(onClick={notice=null}){Text("Done")}})}
}

@Composable fun CreateCategoryDialog(api:JoyMartApi,onClose:()->Unit,onDone:(String)->Unit){var name by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};val scope= rememberCoroutineScope();AlertDialog(onDismissRequest=onClose,title={Text("Add category")},text={Column{OutlinedTextField(name,{name=it},label={Text("Category name")});OutlinedTextField(desc,{desc=it},label={Text("Description")});error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton(onClick={if(name.isBlank())error="Category name is required" else scope.launch{try{val r=api.createCategory(mapOf("category_name" to name,"description" to desc,"status" to "ACTIVE"));if(r.isSuccessful)onDone(name) else error=r.errorBody()?.string()}catch(e:Exception){error=e.message}}}){Text("Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})}
