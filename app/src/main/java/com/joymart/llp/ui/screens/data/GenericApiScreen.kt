package com.joymart.llp.ui.screens.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joymart.llp.data.*

private data class DataCard(val title:String,val fields:List<Pair<String,String>>)

@Composable fun GenericApiScreen(page:String,user:UserDto,api:JoyMartApi){
    val route=when(page){
        "wallet"->"wallet"
        "notifications"->"notifications"
        "orders"->if((user.role?:"").lowercase()=="distributor")"distributor/orders" else "customer/orders"
        "products"->"customer/products"
        "membership"->"membership/me"
        "referrals"->"referrals"
        "subscriptions"->"subscriptions"
        "approvals"->"admin/payments/pending"
        "reports"->"admin/summary"
        "pos"->"sales"
        else->null
    }
    var loading by remember{mutableStateOf(true)};var cards by remember{mutableStateOf<List<DataCard>>(emptyList())};var error by remember{mutableStateOf<String?>(null)};var refresh by remember{mutableIntStateOf(0)}
    LaunchedEffect(route,refresh){
        if(route==null){loading=false;error="This section is not connected to a native API yet.";return@LaunchedEffect}
        loading=true;error=null
        runCatching{api.getRaw(route)}.onSuccess{response->
            if(response.isSuccessful){
                val root=runCatching{JsonParser.parseString(response.body()?.toString()?:"null")}.getOrNull()
                cards=parseCards(root)
                if(cards.isEmpty())error="No records are available yet."
            }else error=response.errorBody()?.string()?.take(400)?:"Unable to load this information."
        }.onFailure{error=it.message?:"Network error. Please try again."}
        loading=false
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(page.replace("-"," ").replaceFirstChar{it.uppercase()},fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color(0xFF172943));Text("Your JoyMart account information",fontSize=12.sp,color=Color.Gray)};IconButton(onClick={refresh++}){Icon(Icons.Default.Refresh,"Refresh")}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error!=null)Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(18.dp)){Text(if(cards.isEmpty())"Information" else "Notice",fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Text(error!!,fontSize=13.sp,color=Color.Gray);TextButton(onClick={refresh++}){Text("Try again")}}}
        LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(cards){card->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)){Column(Modifier.padding(16.dp)){Text(card.title,fontWeight=FontWeight.Bold,fontSize=15.sp,color=Color(0xFF172943));Spacer(Modifier.height(10.dp));card.fields.forEachIndexed{index,pair->if(index>0)HorizontalDivider(color=Color(0xFFE9EDF3));Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.Top){Text(pair.first.replace('_',' ').replaceFirstChar{it.uppercase()},Modifier.weight(1f),fontSize=12.sp,color=Color.Gray);Text(pair.second,Modifier.weight(1.2f),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF25364F))}}}}
            }
        }
    }
}

private fun parseCards(root:JsonElement?):List<DataCard>{
    if(root==null||root.isJsonNull)return emptyList()
    val data=if(root.isJsonObject&&root.asJsonObject.has("data"))root.asJsonObject.get("data") else root
    val records=when{data.isJsonArray->data.asJsonArray.toList();data.isJsonObject->{val obj=data.asJsonObject;val nested=obj.entrySet().firstOrNull{it.value.isJsonArray}?.value;if(nested!=null)nested.asJsonArray.toList() else listOf(data)};else->listOf(data)}
    return records.take(100).mapIndexedNotNull{index,element->
        if(!element.isJsonObject)return@mapIndexedNotNull null
        val obj=element.asJsonObject
        val fields=buildList {
            obj.entrySet().filter{it.key !in setOf("success","message","data")}.forEach{(key,value)->
                if(value.isJsonObject){
                    value.asJsonObject.entrySet().take(4).forEach{(childKey,childValue)->
                        if(!childValue.isJsonNull && !childValue.isJsonObject && !childValue.isJsonArray) add((key+" "+childKey) to childValue.asStringOrValue())
                    }
                } else if(!value.isJsonNull && !value.isJsonArray) add(key to value.asStringOrValue())
            }
        }.take(12)
        if(fields.isEmpty())null else DataCard(fields.firstOrNull{it.first in listOf("name","title","package_name","order_number","member_id","referral_code")}?.second?:"Record ${index+1}",fields)
    }
}
private fun JsonElement.asStringOrValue():String=when{isJsonPrimitive->asJsonPrimitive.let{if(it.isString)it.asString else it.toString()};else->toString()}
