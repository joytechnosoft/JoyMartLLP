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

@Composable fun LoadingOverlay(){Box(Modifier.fillMaxSize().background(Color.Black.copy(.12f)),contentAlignment=Alignment.Center){Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Spacer(Modifier.height(12.dp));Text("Please wait…",fontWeight=FontWeight.Medium)}}}}
