package com.joymart.llp.ui.screens.dashboard

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
import com.joymart.llp.ui.screens.profile.ProfileScreen
import com.joymart.llp.ui.screens.data.GenericApiScreen
import com.joymart.llp.ui.screens.data.GenealogyScreen
import com.joymart.llp.ui.screens.data.ErpWebScreen
import com.joymart.llp.ui.screens.wallet.WalletScreen
import com.joymart.llp.ui.screens.wallet.TransactionScreen
import com.joymart.llp.ui.screens.catalog.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MainShell(user:UserDto,isAdmin:Boolean,page:String,onNavigate:(String)->Unit,onLogout:()->Unit,api:JoyMartApi) {
    val drawerState=rememberDrawerState(DrawerValue.Closed); val scope=rememberCoroutineScope()
    ModalNavigationDrawer(drawerState=drawerState,drawerContent={ModalDrawerSheet(modifier=Modifier.width(304.dp),drawerContainerColor=Color(0xFF1E293B),drawerContentColor=Color.White){JoyMartDrawer(user,isAdmin,page){target->onNavigate(target);scope.launch{drawerState.close()}}}}) {
        Scaffold(containerColor=Color(0xFFF4F6FA),topBar={TopAppBar(title={Column{Text("JoyMart LLP",fontWeight=FontWeight.Bold,fontSize=17.sp);Text(user.name,fontSize=11.sp,color=Color.Gray)}},navigationIcon={IconButton(onClick={scope.launch{drawerState.open()}}){Icon(Icons.Default.Menu,"Menu")}},actions={var confirm by remember{mutableStateOf(false)};IconButton(onClick={confirm=true}){Icon(Icons.Default.Logout,"Logout")};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text("You will need to sign in again to access your account.")},confirmButton={TextButton(onClick=onLogout){Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})})},bottomBar={NavigationBar(containerColor=Color.White){listOf("home" to Icons.Default.Home,"profile" to Icons.Default.Person,"notifications" to Icons.Default.Notifications).forEach{(key,icon)->NavigationBarItem(selected=page==key,onClick={onNavigate(key)},icon={Icon(icon,null)},label={Text(key.replaceFirstChar{it.uppercase()})})}}}){padding->when(page){"profile"->Box(Modifier.padding(padding)){ProfileScreen(user,isAdmin){onNavigate("register")}};"wallet"->Box(Modifier.padding(padding)){WalletScreen(api){onNavigate(it)}};"wallet-transactions"->Box(Modifier.padding(padding)){TransactionScreen(api)};"referrals","team"->Box(Modifier.padding(padding)){GenealogyScreen(api){onNavigate("register")}};"notifications"->Box(Modifier.padding(padding)){GenericApiScreen("notifications",user,api)};"vr-packages"->Box(Modifier.padding(padding)){VrindabanPackagesScreen(api)};"vr-products"->Box(Modifier.padding(padding)){VrindabanProductsScreen(api)};"vr-categories"->Box(Modifier.padding(padding)){VrindabanCategoriesScreen(api)};"erp-shop"->Box(Modifier.padding(padding)){ErpWebScreen{onNavigate("home")}};else->if(page=="home")DashboardScreen(user,isAdmin,onNavigate,Modifier.padding(padding))else Box(Modifier.padding(padding)){GenericApiScreen(page,user,api)}}}
    }
}

@Composable private fun JoyMartDrawer(user:UserDto,isAdmin:Boolean,current:String,onNavigate:(String)->Unit){
    val role=(user.role?:"customer").lowercase()
    val groups=remember(isAdmin,role){buildList {
        if(role != "customer") add("Genealogy" to listOf("My Team" to "referrals","Referrals" to "referrals"))
        if(isAdmin){add("Admin" to listOf("Admin Dashboard" to "reports","User Management" to "users","Members" to "members"));add("Membership" to listOf("Membership Dashboard" to "membership","Members" to "members","Membership Orders" to "membership-orders","Membership Packages" to "membership-packages"));add("Software & ERP" to listOf("Software Orders" to "subscriptions","ERP Web Portal" to "erp-shop"));add("Wallet Management" to listOf("Wallet Overview" to "wallet","Wallet Requests" to "wallet-requests"));add("Vrindaban" to listOf("Packages" to "vr-packages","Products" to "vr-products","Categories" to "vr-categories"));add("Finance & Reports" to listOf("Commission" to "commission","Finance" to "finance","Reports" to "reports","Settings" to "settings"))}
    }}
    var expanded by remember{mutableStateOf(setOf("Admin","Membership"))}
    Column(Modifier.fillMaxSize().background(Color(0xFF1E293B))){Column(Modifier.fillMaxWidth().background(Color(0xFF2868F5)).padding(18.dp)){Text("JOYSMART OS",color=Color.White,fontWeight=FontWeight.ExtraBold,letterSpacing=1.2.sp)};Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF60B9F5)),contentAlignment=Alignment.Center){Text(user.name.take(1).uppercase(),color=Color(0xFF10233D),fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column{Text(user.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp);Text(if(isAdmin)"DIRECTOR" else role.replaceFirstChar{it.uppercase()},color=Color(0xFFFFD54F),fontSize=10.sp,fontWeight=FontWeight.Bold)}};DrawerLabel("CORE ENGINE");DrawerEntry("Dashboard",Icons.Default.Dashboard,current=="home"){onNavigate("home")};DrawerEntry("My Profile",Icons.Default.AccountCircle,current=="profile"){onNavigate("profile")};DrawerEntry("My Wallet",Icons.Default.AccountBalanceWallet,current=="wallet"){onNavigate("wallet")};DrawerEntry("My Earnings",Icons.Default.Paid,current=="earnings"){onNavigate("earnings")};DrawerEntry("Subscriptions",Icons.Default.CardMembership,current=="subscriptions"){onNavigate("subscriptions")};DrawerLabel(if(isAdmin)"ADMINISTRATION" else "MY WORKSPACE");LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=18.dp)){items(groups){group->val open=group.first in expanded;Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=2.dp).clip(RoundedCornerShape(9.dp)).background(if(open)Color(0xFF2B394D)else Color.Transparent).clickable{expanded=if(open)expanded-group.first else expanded+group.first}.padding(horizontal=12.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){Text(group.first,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Icon(if(open)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,tint=Color.LightGray)};AnimatedVisibility(open){Column{group.second.forEach{(title,target)->DrawerEntry(title,Icons.Default.ChevronRight,current==target,true){onNavigate(target)}}}}}}}
}
@Composable private fun DrawerLabel(label:String){Text(label,Modifier.padding(start=16.dp,top=8.dp,bottom=7.dp),color=Color(0xFF8FA6C7),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
@Composable private fun DrawerEntry(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,selected:Boolean,nested:Boolean=false,onClick:()->Unit){Row(Modifier.fillMaxWidth().padding(start=if(nested)18.dp else 8.dp,end=8.dp,top=2.dp,bottom=2.dp).clip(RoundedCornerShape(9.dp)).background(if(selected)Color(0xFF2868F5)else Color.Transparent).clickable(onClick=onClick).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=if(selected)Color.White else Color(0xFF73B8FF),modifier=Modifier.size(17.dp));Spacer(Modifier.width(12.dp));Text(title,color=Color.White,fontSize=11.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium)}}

@Composable fun DashboardScreen(user:UserDto,isAdmin:Boolean,onNavigate:(String)->Unit,modifier:Modifier=Modifier) {
    val role=(user.roleMaster?.displayName ?: user.role ?: "Member").replaceFirstChar{it.uppercase()}
    LazyColumn(modifier.fillMaxSize().padding(horizontal=18.dp),contentPadding=PaddingValues(top=18.dp,bottom=22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF102447),Color(0xFF176B75)))).padding(20.dp)) { Column { Text("GOOD TO SEE YOU",color=Color.White.copy(.72f),fontSize=10.sp,letterSpacing=1.5.sp); Text(user.name, color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold); Text(role + if(isAdmin) "  •  Admin workspace" else "  •  Member workspace",color=Color(0xFFB8F4E6),fontSize=12.sp); Spacer(Modifier.height(20.dp)); Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Account status",color=Color.White.copy(.7f),fontSize=11.sp);Text(user.status ?: "Available",color=Color.White,fontWeight=FontWeight.SemiBold)};Column{Text("Referral code",color=Color.White.copy(.7f),fontSize=11.sp);Text(user.referralCode ?: "—",color=Color.White,fontWeight=FontWeight.SemiBold)}} } } }
        item { Text(if(isAdmin) "Administration" else "Your workspace",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color(0xFF102447)) }
        val roleKey=(user.role ?: "customer").lowercase()
        val modules=when {
            isAdmin -> listOf(Triple("Vrindaban Packages","Manage package plans and commission setup","vr-packages"),Triple("Vrindaban Products","Products, pricing and inventory","vr-products"),Triple("Vrindaban Categories","Manage product categories","vr-categories"),Triple("Approvals","Review pending business approvals","approvals"),Triple("Business Reports","Performance and transactions","reports"),Triple("Memberships","Membership and activation","membership"),Triple("Subscriptions","Software subscriptions and orders","subscriptions"),Triple("Wallet & Transactions","Financial activity and withdrawals","wallet"))
            roleKey == "coo" -> listOf(Triple("Business Overview","Operations and business performance","reports"),Triple("Memberships","Membership status and activity","membership"),Triple("Subscriptions","Software subscriptions","subscriptions"),Triple("Wallet & Transactions","Financial activity","wallet"),Triple("Referrals","Your team and referrals","referrals"))
            roleKey in listOf("zm","manager","executive","senior_executive","officer","senior_officer") -> listOf(Triple("My Team","Team and referral network","referrals"),Triple("Membership","Membership status","membership"),Triple("Wallet","Balance and transactions","wallet"),Triple("Orders","Business orders","orders"))
            roleKey == "distributor" -> listOf(Triple("Distributor Orders","Accept and manage incoming orders","orders"),Triple("Products","Browse products","products"),Triple("POS Billing","Create sales bills","pos"),Triple("Wallet","Balance and transactions","wallet"),Triple("My Team","Referral network","referrals"))
            else -> listOf(Triple("Browse Products","View available products","products"),Triple("My Orders","Track your orders","orders"),Triple("My Wallet","Balance and transactions","wallet"),Triple("Membership","Membership and account status","membership"))
        }
        items(modules.size){i->val m=modules[i]; ModuleCard(m.first,m.second,if(i==0) Icons.Default.Inventory2 else Icons.Default.ArrowForward){onNavigate(m.third)} }
        item { Text("Your account",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color(0xFF102447)); ModuleCard("My Profile","Personal details and account information",Icons.Default.Person){onNavigate("profile")} }
    }
}

@Composable fun ModuleCard(title:String,desc:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFE8F5F3)),contentAlignment=Alignment.Center){Icon(icon,null,tint=Color(0xFF087F78))};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold,color=Color(0xFF172943));Text(desc,fontSize=12.sp,color=Color(0xFF778399))};Icon(Icons.Default.ChevronRight,null,tint=Color.Gray)} }
}
