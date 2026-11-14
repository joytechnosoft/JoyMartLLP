package com.joymart.llp.ui.screens.profile

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

@Composable fun ProfileScreen(user:UserDto,isAdmin:Boolean,onRegister:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(isAdmin) "Administrator profile" else "My profile",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Color(0xFF102447))
        Card(shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFF102447)),contentAlignment=Alignment.Center){Text(user.name.take(1).uppercase(),color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(14.dp));Column{Text(user.name,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(user.roleMaster?.displayName ?: user.role ?: "Member",color=Color.Gray,fontSize=12.sp)}};HorizontalDivider(Modifier.padding(vertical=14.dp));InfoLine("Phone",user.phone ?: "—");InfoLine("Email",user.email ?: "—");InfoLine("Member ID",user.memberId ?: "—");InfoLine("Status",user.status ?: "—");InfoLine("Referral code",user.referralCode ?: "—");InfoLine("Membership",if(user.isActiveMember==1) "Active" else "Not active"); Spacer(Modifier.height(12.dp)); Button(onClick=onRegister,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.PersonAdd,null); Spacer(Modifier.width(8.dp)); Text("Register New Member") } } }
        if(isAdmin) Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEAF7F4)),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Admin workspace",fontWeight=FontWeight.Bold,color=Color(0xFF087F78));Text("Administrative tools are enabled for this active administrator account.",fontSize=12.sp,color=Color(0xFF526C70))}}
    }
}

@Composable fun InfoLine(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Color.Gray,fontSize=13.sp);Text(value,color=Color(0xFF172943),fontWeight=FontWeight.Medium,fontSize=13.sp)}}
