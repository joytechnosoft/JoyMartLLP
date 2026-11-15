package com.joymart.llp.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import com.joymart.llp.ui.screens.membership.MembershipScreen
import com.joymart.llp.ui.screens.data.GenealogyScreen
import com.joymart.llp.ui.screens.data.ErpWebScreen
import com.joymart.llp.ui.screens.wallet.WalletScreen
import com.joymart.llp.ui.screens.wallet.TransactionScreen
import com.joymart.llp.ui.screens.catalog.*
import com.google.gson.JsonParser
import com.joymart.llp.ui.screens.finance.EarningsScreen
import com.joymart.llp.ui.screens.finance.NativeTransactionsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MainShell(user:UserDto,isAdmin:Boolean,page:String,onNavigate:(String)->Unit,onLogout:()->Unit,api:JoyMartApi) {
    val drawerState=rememberDrawerState(DrawerValue.Closed); val scope=rememberCoroutineScope()
    ModalNavigationDrawer(drawerState=drawerState,drawerContent={ModalDrawerSheet(modifier=Modifier.width(304.dp),drawerContainerColor=Color(0xFF1E293B),drawerContentColor=Color.White){JoyMartDrawer(user,isAdmin,page){target->onNavigate(target);scope.launch{drawerState.close()}}}}) {
        Scaffold(containerColor=Color(0xFFF4F6FA),topBar={TopAppBar(title={Column{Text("JoyMart LLP",fontWeight=FontWeight.Bold,fontSize=17.sp);Text(user.name,fontSize=11.sp,color=Color.Gray)}},
            navigationIcon={IconButton(onClick={scope.launch{drawerState.open()}}){Icon(Icons.Default.Menu,"Menu")}},actions={var confirm by remember{mutableStateOf(false)};
                IconButton(onClick={confirm=true}){Icon(Icons.Default.Logout,"Logout")};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Sign out?")},text={Text("You will need to sign in again to access your account.")},confirmButton={TextButton(onClick=onLogout){Text("Sign out")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})})},bottomBar={NavigationBar(containerColor=Color.White){listOf("home" to Icons.Default.Home,"profile" to Icons.Default.Person,"notifications" to Icons.Default.Notifications).forEach{(key,icon)->NavigationBarItem(selected=page==key,onClick={onNavigate(key)},icon={Icon(icon,null)},
            label={Text(key.replaceFirstChar{it.uppercase()})})}}}){padding->when(page){ "earnings" -> Box(Modifier.padding(padding)) {
            EarningsScreen(api)
        }

            "wallet-transactions" -> Box(Modifier.padding(padding)) { NativeTransactionsScreen(api) } "profile"->Box(Modifier.padding(padding)){ProfileScreen(user,isAdmin,{onNavigate("register")},{onNavigate("membership")})};"membership"->Box(Modifier.padding(padding)){MembershipScreen(user,api){onNavigate("home")}};"wallet"->Box(Modifier.padding(padding)){WalletScreen(api){onNavigate(it)}};"referrals","team"->Box(Modifier.padding(padding)){GenealogyScreen(api){onNavigate("register")}};"notifications"->Box(Modifier.padding(padding)){GenericApiScreen("notifications",user,api)};"vr-packages"->Box(Modifier.padding(padding)){VrindabanPackagesScreen(api)};"vr-products"->Box(Modifier.padding(padding)){VrindabanProductsScreen(api)};"vr-categories"->Box(Modifier.padding(padding)){VrindabanCategoriesScreen(api)};"erp-shop"->Box(Modifier.padding(padding)){ErpWebScreen{onNavigate("home")}};else->if(page=="home")
                DashboardScreen(
                    user = user,
                    isAdmin = isAdmin,
                    onNavigate = onNavigate,
                    api = api,
                    modifier = Modifier.padding(padding)
                )
            else Box(Modifier.padding(padding)){GenericApiScreen(page,user,api)}}}
    }
}

@Composable
private fun JoyMartDrawer(user:UserDto,isAdmin:Boolean,current:String,onNavigate:(String)->Unit){
    val role=(user.role?:"customer").lowercase()
    val groups=remember(isAdmin,role){
        buildList {
            val coreItems = mutableListOf(
                "Dashboard" to "home",
                "My Profile" to "profile",
                "My Wallet" to "wallet",
                "My Earnings" to "earnings",
                "Transactions" to "wallet-transactions",
                "Subscriptions" to "subscriptions",
                "Membership" to "membership",
                "Membership Packages" to "membership-packages",
                "My Orders" to "orders",
                "Products" to "products",
                "Notifications" to "notifications"
            )
            if (role in setOf("director","coo","zm","manager","distributor","zonal_manager","senior_executive","executive") || isAdmin) {
                coreItems.add(7, "My Team" to "referrals")
            }
            add("MY JOYMART" to coreItems)
            if(role=="distributor") add("DISTRIBUTOR" to listOf(
                "Incoming Orders" to "orders",
                "POS / Sales" to "pos",
                "Product Catalogue" to "products"
            ))
            if(isAdmin) {
                add("DIRECTOR / ADMIN" to listOf(
                    "Admin Overview" to "reports",
                    "Payment Approvals" to "approvals",
                    "Profit Transactions" to "profit-transactions",
                    "Software Orders" to "software-orders",
                    "ERP Subscriptions" to "admin-subscriptions"
                ))
                add("VRINDABAN" to listOf(
                    "Packages" to "vr-packages",
                    "Products" to "vr-products",
                    "Categories" to "vr-categories"
                ))
            }
            add("ERP WORKSPACE" to listOf("ERP Web Portal" to "erp-shop"))
        }
    }
    var expanded by remember{mutableStateOf(setOf("MY JOYMART","DIRECTOR / ADMIN"))}
    Column(Modifier.fillMaxSize().background(Color(0xFF14243B))) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102447),Color(0xFF176B75)))).padding(20.dp)) {
            Text("JOYSMART OS",color=Color.White,fontWeight=FontWeight.ExtraBold,letterSpacing=1.3.sp,fontSize=17.sp)
            Text("Business workspace",color=Color.White.copy(.7f),fontSize=10.sp)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFBCEBE3)),contentAlignment=Alignment.Center) {
                Text(user.name.take(1).uppercase(),color=Color(0xFF12314A),fontWeight=FontWeight.Bold,fontSize=18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=1)
                Text(if(isAdmin)"DIRECTOR / ADMIN" else role.replaceFirstChar{it.uppercase()},color=Color(0xFF9DE5D9),fontSize=10.sp)
                Text("ID: ${user.memberId ?: user.id}",color=Color(0xFF9BAEC7),fontSize=9.sp)
            }
        }
        HorizontalDivider(color=Color.White.copy(.10f))
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(vertical=10.dp)) {
            items(groups) { group ->
                val open=group.first in expanded
                Row(
                    Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=3.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if(open)Color.White.copy(.07f)else Color.Transparent)
                        .clickable{expanded=if(open)expanded-group.first else expanded+group.first}
                        .padding(horizontal=12.dp,vertical=12.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Text(group.first,Modifier.weight(1f),color=Color(0xFF9BAEC7),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                    Icon(if(open)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null,tint=Color(0xFF9BAEC7),modifier=Modifier.size(17.dp))
                }
                AnimatedVisibility(open) {
                    Column {
                        group.second.distinctBy{it.second}.forEach { (title,target) ->
                            DrawerEntry(title,drawerIcon(target),current==target,true){onNavigate(target)}
                        }
                    }
                }
            }
        }
        Text("JoyMart LLP  •  Native App",Modifier.padding(16.dp),color=Color(0xFF8294AC),fontSize=9.sp)
    }
}

private fun drawerIcon(target:String):androidx.compose.ui.graphics.vector.ImageVector = when(target) {
    "home","reports" -> Icons.Default.Dashboard
    "profile" -> Icons.Default.AccountCircle
    "wallet" -> Icons.Default.AccountBalanceWallet
    "earnings" -> Icons.Default.CurrencyRupee
    "wallet-transactions","profit-transactions" -> Icons.Default.ReceiptLong
    "subscriptions","admin-subscriptions" -> Icons.Default.Layers
    "my-services" -> Icons.Default.Apps
    "membership","membership-packages" -> Icons.Default.CardMembership
    "referrals" -> Icons.Default.AccountTree
    "orders","software-orders" -> Icons.Default.ShoppingBag
    "products","vr-products" -> Icons.Default.Inventory2
    "notifications" -> Icons.Default.Notifications
    "approvals" -> Icons.Default.VerifiedUser
    "pos" -> Icons.Default.PointOfSale
    "vr-packages" -> Icons.Default.Inventory
    "vr-categories" -> Icons.Default.Category
    "erp-shop" -> Icons.Default.OpenInBrowser
    else -> Icons.Default.ChevronRight
}
@Composable private fun DrawerLabel(label:String){Text(label,Modifier.padding(start=16.dp,top=8.dp,bottom=7.dp),color=Color(0xFF8FA6C7),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
@Composable private fun DrawerEntry(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,selected:Boolean,nested:Boolean=false,onClick:()->Unit){Row(Modifier.fillMaxWidth().padding(start=if(nested)18.dp else 8.dp,end=8.dp,top=2.dp,bottom=2.dp).clip(RoundedCornerShape(9.dp)).background(if(selected)Color(0xFF2868F5)else Color.Transparent).clickable(onClick=onClick).padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=if(selected)Color.White else Color(0xFF73B8FF),modifier=Modifier.size(17.dp));Spacer(Modifier.width(12.dp));Text(title,color=Color.White,fontSize=11.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium)}}

private data class DashboardAction(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val group: String
)

private fun dashboardActions(isAdmin: Boolean, role: String): List<DashboardAction> {
    val common = listOf(
        DashboardAction("profile", "My Profile", "Personal details", Icons.Default.Person, "Account"),
        DashboardAction(
            "earnings",
            "My Earnings",
            "Membership & software commission",
            Icons.Default.CurrencyRupee,
            "Finance"
        ),
        DashboardAction("wallet", "Wallet", "Balance & transfers", Icons.Default.AccountBalanceWallet, "Finance"),
        DashboardAction("wallet-transactions", "Transactions", "Wallet activity", Icons.Default.ReceiptLong, "Finance"),
        DashboardAction("referrals", "My Team", "Team & referrals", Icons.Default.AccountTree, "Business"),
        DashboardAction("membership", "Membership", "Membership status", Icons.Default.CardMembership, "Business"),
        DashboardAction("membership-packages", "Membership Packages", "Available plans", Icons.Default.Inventory, "Business"),
        DashboardAction("subscriptions", "Subscriptions", "Plans & services", Icons.Default.Layers, "Business"),
        DashboardAction("software-orders", "Software Orders", "Orders & activation", Icons.Default.ReceiptLong, "Business"),
        DashboardAction("orders", "My Orders", "Order history", Icons.Default.ShoppingBag, "Business"),
        DashboardAction("my-services", "My ERP Modules", "Subscribed software", Icons.Default.Apps, "Business"),
        DashboardAction("products", "Products", "Browse products", Icons.Default.Inventory2, "Business"),
        DashboardAction("notifications", "Notifications", "Alerts & updates", Icons.Default.Notifications, "Account")
    )
    val distributor = listOf(
        DashboardAction("orders", "Distributor Orders", "Incoming orders", Icons.Default.LocalShipping, "Business"),
        DashboardAction("products", "Products", "Browse inventory", Icons.Default.Inventory2, "Business"),
        DashboardAction("pos", "POS / Sales", "Create sales", Icons.Default.PointOfSale, "Business")
    )
    val admin = listOf(
        DashboardAction("reports", "Admin Overview", "Business performance", Icons.Default.Dashboard, "Administration"),
        DashboardAction("approvals", "Payment Approvals", "Review pending payments", Icons.Default.VerifiedUser, "Administration"),
        DashboardAction("profit-transactions", "Profit Transactions", "Business ledger", Icons.Default.CurrencyRupee, "Administration"),
        DashboardAction("admin-subscriptions", "ERP Subscriptions", "Manage software plans", Icons.Default.AdminPanelSettings, "Administration"),
        DashboardAction("vr-packages", "Vrindaban Packages", "Manage packages", Icons.Default.Inventory, "Vrindaban"),
        DashboardAction("vr-products", "Vrindaban Products", "Manage products", Icons.Default.Store, "Vrindaban"),
        DashboardAction("vr-categories", "Vrindaban Categories", "Product categories", Icons.Default.Category, "Vrindaban")
    )
    val teamRoles = setOf("director", "coo", "zm", "manager", "distributor", "zonal_manager", "senior_executive", "executive")
    val roleFiltered = common.filterNot { it.key == "referrals" && role !in teamRoles && !isAdmin }
    return (roleFiltered + (if (role == "distributor") distributor else emptyList()) + (if (isAdmin) admin else emptyList()))
        .distinctBy { it.key }
}

@Composable
fun DashboardScreen(   user: UserDto,
                       isAdmin: Boolean,
                       onNavigate: (String) -> Unit,
                       api: JoyMartApi,
                       modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val roleKey = (user.role ?: user.roleMaster?.name ?: "customer").lowercase()
    val actions = remember(isAdmin, roleKey) { dashboardActions(isAdmin, roleKey) }
    val prefs = remember(user.id) { context.getSharedPreferences("joymart_dashboard_${user.id}", android.content.Context.MODE_PRIVATE) }
    val defaultShortcuts = remember(actions) {
        val defaults = if (isAdmin) listOf("reports","approvals","wallet","wallet-transactions","referrals","membership")
        else if (roleKey == "distributor") listOf("orders","products","pos","wallet","referrals","wallet-transactions")
        else listOf("referrals","membership","wallet","orders","subscriptions","wallet-transactions")
        defaults.filter { key -> actions.any { it.key == key } }.take(8)
    }
    var selectedKeys by remember(user.id, actions) {
        val saved = prefs.getStringSet("shortcuts", null)?.toList().orEmpty()
        mutableStateOf(if (saved.isEmpty()) defaultShortcuts else saved.filter { key -> actions.any { it.key == key } }.take(8))
    }
    var customize by remember { mutableStateOf(false) }
    val selectedActions = selectedKeys.mapNotNull { key -> actions.firstOrNull { it.key == key } }
    val role = (user.roleMaster?.displayName ?: user.role ?: "Member").replaceFirstChar { it.uppercase() }
    var liveWalletBalance by remember(user.id) { mutableStateOf<String?>(null) }
    var walletBalanceLoading by remember(user.id) { mutableStateOf(true) }
    LaunchedEffect(user.id) {
        walletBalanceLoading = true
        try {
            val response = api.getRaw("wallet")
            if (response.isSuccessful) {
                val root = JsonParser.parseString(response.body()?.toString() ?: "{}").asJsonObject
                val data = root.getAsJsonObject("data")
                if (root.get("success")?.asBoolean == true && data?.has("balance") == true && data.get("balance") != null && !data.get("balance").isJsonNull) {
                    val amount = data.get("balance").asBigDecimal.setScale(2, java.math.RoundingMode.HALF_UP)
                    liveWalletBalance = amount.toPlainString()
                }
            }
        } catch (_: Exception) {
            // Keep the live balance unavailable rather than showing a potentially stale profile value as current.
        } finally { walletBalanceLoading = false }
    }
    val walletDisplay = when {
        liveWalletBalance != null -> "₹${liveWalletBalance}"
        walletBalanceLoading -> "Loading…"
        else -> "Unavailable"
    }
    var membershipActive by remember(user.id) {
        mutableStateOf(user.isActiveMember == 1)
    }

    var pendingOrder by remember(user.id) {
        mutableStateOf<com.google.gson.JsonObject?>(null)
    }

    var latestOrder by remember(user.id) {
        mutableStateOf<com.google.gson.JsonObject?>(null)
    }

    var showInactiveAlert by remember(user.id) {
        mutableStateOf(false)
    }

    var showThankYou by remember(user.id) {
        mutableStateOf(false)
    }

    LaunchedEffect(user.id) {
        try {
            val response = api.getRaw("membership/me")

            if (response.isSuccessful) {
                val json = JsonParser.parseString(
                    response.body()?.toString() ?: "{}"
                ).asJsonObject

                val wasInactive = !membershipActive

                membershipActive =
                    json.get("membership_active")?.asBoolean
                        ?: (json.get("data")?.isJsonObject == true)

                val pending = json.get("pending_order")
                pendingOrder =
                    if (pending != null && pending.isJsonObject)
                        pending.asJsonObject
                    else null

                val latest = json.get("latest_order")
                latestOrder =
                    if (latest != null && latest.isJsonObject)
                        latest.asJsonObject
                    else null

                if (membershipActive && wasInactive) {
                    showThankYou = true
                } else if (!membershipActive) {
                    showInactiveAlert = true
                }
            }
        } catch (e: Exception) {
            // Network failure হলে existing dashboard state থাকবে।
        }
    }

    LazyColumn(
        modifier.fillMaxSize().background(Color(0xFFF4F6FA)).padding(horizontal=16.dp),
        contentPadding = PaddingValues(top=16.dp,bottom=28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF122B50),Color(0xFF176B75),Color(0xFF167E83))))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(.15f)),contentAlignment=Alignment.Center) {
                            Text(user.name.take(1).uppercase(),color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("GOOD TO SEE YOU",color=Color.White.copy(.70f),fontSize=9.sp,letterSpacing=1.4.sp)
                            Text(user.name, color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text("$role  •  ${if(isAdmin) "Admin workspace" else "Member workspace"}",color=Color(0xFFB8F4E6),fontSize=11.sp)
                        }
                        Surface(shape=RoundedCornerShape(50),color=if(membershipActive)Color(0xFFDCFCE7) else Color(0xFFFFF0C2)) {
                            Text(if(membershipActive)"MEMBERSHIP ACTIVE" else "MEMBERSHIP INACTIVE",modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp),fontSize=8.sp,fontWeight=FontWeight.Bold,color=if(membershipActive)Color(0xFF167447) else Color(0xFF946200))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Member ID",color=Color.White.copy(.68f),fontSize=10.sp)
                            Text(user.memberId ?: "—",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Referral code",color=Color.White.copy(.68f),fontSize=10.sp)
                            Text(user.referralCode ?: "—",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                        }
                        Column {
                            Text("Wallet",color=Color.White.copy(.68f),fontSize=10.sp)
                            Text(walletDisplay,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            val statItems = listOf(
                Triple("Wallet balance",walletDisplay,Icons.Default.AccountBalanceWallet),
                Triple("Membership",if(membershipActive)"ACTIVE" else "CHECK STATUS",Icons.Default.CardMembership),
                Triple("Member ID",user.memberId ?: "—",Icons.Default.Badge),
                Triple("Referral code",user.referralCode ?: "—",Icons.Default.Share)
            )
            LazyVerticalGrid(
                columns=GridCells.Fixed(2),
                modifier=Modifier.fillMaxWidth().height(150.dp),
                userScrollEnabled=false,
                horizontalArrangement=Arrangement.spacedBy(9.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp)
            ) {
                gridItems(statItems) { stat ->
                    Card(
                        Modifier.fillMaxWidth().height(70.dp),
                        shape=RoundedCornerShape(15.dp),
                        colors=CardDefaults.cardColors(containerColor=Color.White),
                        elevation=CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal=11.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFEAF4F3)),contentAlignment=Alignment.Center) {
                                Icon(stat.third,null,tint=Color(0xFF087F78),modifier=Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stat.first,fontSize=9.sp,color=Color(0xFF8792A3),maxLines=1)
                                Text(stat.second,fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF1C2D48),maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quick access",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color(0xFF172943))
                    Text("Your frequently used JoyMart tools",fontSize=11.sp,color=Color(0xFF7A879B))
                }
                OutlinedButton(
                    onClick={customize=true},
                    shape=RoundedCornerShape(12.dp),
                    contentPadding=PaddingValues(horizontal=11.dp,vertical=7.dp)
                ) {
                    Icon(Icons.Default.Tune,null,Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Customize",fontSize=11.sp)
                }
            }
        }

        item {
            val rows = (selectedActions.size + 1) / 2
            LazyVerticalGrid(
                columns=GridCells.Fixed(2),
                modifier=Modifier.fillMaxWidth().height((rows * 112).dp),
                userScrollEnabled=false,
                horizontalArrangement=Arrangement.spacedBy(10.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ) {
                gridItems(selectedActions,key={it.key}) { action ->
                    ShortcutTile(action=action,onClick={onNavigate(action.key)})
                }
            }
            if(selectedActions.isEmpty()) {
                Card(shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                    Text("No shortcuts selected. Tap Customize to add your favourite tools.",Modifier.padding(16.dp),fontSize=12.sp,color=Color.Gray)
                }
            }
        }

        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Your workspace",Modifier.weight(1f),fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color(0xFF172943))
                Text("${actions.size} tools",fontSize=10.sp,color=Color(0xFF7A879B))
            }
        }

        items(actions.groupBy { it.group }.toList(), key={it.first}) { group ->
            Card(
                Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(20.dp),
                colors=CardDefaults.cardColors(containerColor=Color.White),
                elevation=CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(group.first.uppercase(),fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Color(0xFF8995A8))
                    Spacer(Modifier.height(8.dp))
                    group.second.forEach { action ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onNavigate(action.key) }.padding(vertical=10.dp,horizontal=4.dp),
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFE7F4F2)),contentAlignment=Alignment.Center) {
                                Icon(action.icon,null,tint=Color(0xFF087F78),modifier=Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(action.title,fontWeight=FontWeight.SemiBold,fontSize=13.sp,color=Color(0xFF172943))
                                Text(action.subtitle,fontSize=10.sp,color=Color(0xFF7A879B))
                            }
                            if (selectedKeys.contains(action.key)) {
                                Surface(shape=RoundedCornerShape(50),color=Color(0xFFE4F5EF)) {
                                    Text("PINNED",Modifier.padding(horizontal=8.dp,vertical=4.dp),fontSize=8.sp,fontWeight=FontWeight.Bold,color=Color(0xFF16865B))
                                }
                            } else {
                                Icon(Icons.Default.ChevronRight,null,tint=Color.LightGray)
                            }
                        }
                        if (action != group.second.last()) HorizontalDivider(color=Color(0xFFF0F2F6))
                    }
                }
            }
        }

        item {
            Text("Your account",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color(0xFF172943))
            ModuleCard("My Profile","Personal details and account information",Icons.Default.Person){onNavigate("profile")}
        }
    }

    if (showInactiveAlert) {

        val order = latestOrder

        val paymentStatus = order
            ?.get("payment_status")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.uppercase()
            ?: ""

        val isPending = paymentStatus == "PENDING"
        val isRejected = paymentStatus == "REJECTED"

        val packageName = order
            ?.get("package")
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.get("package_name")
            ?.takeIf { !it.isJsonNull }
            ?.asString ?: "Membership Package"

        val amount = order
            ?.get("amount")
            ?.takeIf { !it.isJsonNull }
            ?.asString ?: "—"

        val utr = order
            ?.get("transaction_id")
            ?.takeIf { !it.isJsonNull }
            ?.asString ?: "—"

        val remarks = order
            ?.get("remarks")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.takeIf { it.isNotBlank() }
            ?: "Please contact admin for more information."

        val alertTitle = when {
            isPending -> "Payment submitted"
            isRejected -> "Payment rejected"
            else -> "Membership inactive"
        }

        val alertMessage = when {
            isPending ->
                "Your UPI payment has been submitted successfully. It is waiting for admin approval."

            isRejected ->
                "Your previous membership payment was rejected by admin."

            else ->
                "Your membership is currently inactive. Select a package and activate your account."
        }

        AlertDialog(
            onDismissRequest = {
                showInactiveAlert = false
            },

            icon = {
                Icon(
                    when {
                        isPending -> Icons.Default.HourglassTop
                        isRejected -> Icons.Default.Cancel
                        else -> Icons.Default.Warning
                    },
                    null,
                    tint = when {
                        isPending -> Color(0xFFD97706)
                        isRejected -> Color(0xFFDC2626)
                        else -> Color(0xFFB7791F)
                    }
                )
            },

            title = {
                Text(alertTitle, fontWeight = FontWeight.Bold)
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(alertMessage, fontSize = 13.sp)

                    if (isPending || isRejected) {
                        HorizontalDivider()

                        Text("Package: $packageName", fontSize = 12.sp)
                        Text("Amount: ₹$amount", fontSize = 12.sp)
                        Text("UTR: $utr", fontSize = 12.sp)

                        if (isRejected) {
                            Surface(
                                color = Color(0xFFFFEEEE),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(11.dp)) {
                                    Text(
                                        "ADMIN REMARKS",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )

                                    Text(
                                        remarks,
                                        color = Color(0xFF991B1B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Please wait for admin approval. Do not submit another payment.",
                                color = Color(0xFFD97706),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        showInactiveAlert = false
                        onNavigate("membership")
                    }
                ) {
                    Text(
                        when {
                            isPending -> "View status"
                            isRejected -> "Try again"
                            else -> "Activate now"
                        }
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showInactiveAlert = false
                    }
                ) {
                    Text("Later")
                }
            },

            shape = RoundedCornerShape(22.dp)
        )
    }

    if (customize) {
        ShortcutCustomizeDialog(
            actions=actions,
            selected=selectedKeys.toSet(),
            onDismiss={customize=false},
            onSave={keys->
                selectedKeys=keys
                prefs.edit().putStringSet("shortcuts",keys.toSet()).apply()
                customize=false
            }
        )
    }
}

@Composable
private fun ShortcutTile(action:DashboardAction,onClick:()->Unit) {
    Card(
        onClick=onClick,
        modifier=Modifier.fillMaxWidth().height(102.dp),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=Color.White),
        elevation=CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFE6F4F2)),contentAlignment=Alignment.Center) {
                Icon(action.icon,null,tint=Color(0xFF07877D),modifier=Modifier.size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(action.title,fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color(0xFF1C2D48),maxLines=2,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(action.subtitle,fontSize=9.sp,color=Color(0xFF7D8A9E),maxLines=2,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ShortcutCustomizeDialog(
    actions:List<DashboardAction>,
    selected:Set<String>,
    onDismiss:()->Unit,
    onSave:(List<String>)->Unit
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Column { Text("Customize shortcuts",fontWeight=FontWeight.Bold);Text("Choose up to 8 quick actions",fontSize=11.sp,color=Color.Gray) }},
        text={
            Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState())) {
                actions.forEach { action ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            draft=if(draft.contains(action.key)) draft-action.key else if(draft.size<8) draft+action.key else draft
                        }.padding(vertical=7.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Checkbox(checked=draft.contains(action.key),onCheckedChange={checked->
                            draft=if(checked && draft.size<8) draft+action.key else if(!checked) draft-action.key else draft
                        })
                        Icon(action.icon,null,tint=Color(0xFF087F78),modifier=Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(action.title,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                            Text(action.subtitle,fontSize=10.sp,color=Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton={Button(onClick={onSave(actions.map{it.key}.filter{draft.contains(it)})}){Text("Save shortcuts")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}},
        shape=RoundedCornerShape(24.dp)
    )
}

@Composable fun ModuleCard(title:String,desc:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFE8F5F3)),contentAlignment=Alignment.Center){Icon(icon,null,tint=Color(0xFF087F78))};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold,color=Color(0xFF172943));Text(desc,fontSize=12.sp,color=Color(0xFF778399))};Icon(Icons.Default.ChevronRight,null,tint=Color.Gray)} }
}
