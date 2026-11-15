package com.joymart.llp.ui.components

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

@Composable fun SimpleScreen(title:String,desc:String,icon:androidx.compose.ui.graphics.vector.ImageVector){Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(icon,null,tint=Color(0xFF087F78),modifier=Modifier.size(54.dp));Spacer(Modifier.height(14.dp));Text(title,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(desc,color=Color.Gray)}}

@Composable
fun LoadingOverlay() {
    Box(
        Modifier.fillMaxSize().background(Color(0x660B1730)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(25.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFF087F78)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Please wait", fontWeight = FontWeight.Bold, color = Color(0xFF172B4D))
                    Text("Connecting securely to JoyMart…", color = Color(0xFF718097), fontSize = 11.sp)
                }
            }
        }
    }
}
