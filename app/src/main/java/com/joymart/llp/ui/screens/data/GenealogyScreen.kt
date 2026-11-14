package com.joymart.llp.ui.screens.data

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.google.gson.JsonObject
import com.joymart.llp.data.JoyMartApi
import kotlinx.coroutines.launch

private data class UniMember(
    val id: Long,
    val memberId: String,
    val name: String,
    val phone: String,
    val email: String,
    val role: String,
    val rank: String,
    val status: String,
    val referralCode: String,
    val joinedAt: String,
    val sponsorName: String,
    val sponsorMemberId: String,
    val parentName: String,
    val parentMemberId: String,
    val teamCount: Int,
    val children: List<UniMember>
)

private data class MemberSearchHit(val id: Long, val memberId: String, val name: String, val role: String)

@Composable
fun GenealogyScreen(api: JoyMartApi, onRegister: () -> Unit = {}) {
    var root by remember { mutableStateOf<UniMember?>(null) }
    var directCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var searching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var searchHits by remember { mutableStateOf<List<MemberSearchHit>>(emptyList()) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var detailMember by remember { mutableStateOf<UniMember?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var visibleLevels by remember { mutableIntStateOf(4) }
    var viewMode by remember { mutableStateOf("chart") }
    var currentRootId by remember { mutableStateOf<Long?>(null) }
    var treeScale by remember { mutableFloatStateOf(1f) }
    var treeOffset by remember { mutableStateOf(Offset.Zero) }
    val history = remember { mutableStateListOf<UniMember>() }
    val scope = rememberCoroutineScope()

    suspend fun loadMember(databaseId: Long? = null, pushHistory: Boolean = false, depth: Int = visibleLevels) {
        loading = true
        errorMessage = null
        try {
            val targetId = databaseId ?: currentRootId
            val safeDepth = depth.coerceIn(1, 10)
            val path = if (targetId == null) "genealogy?depth=$safeDepth"
                       else "genealogy?member_id=$targetId&depth=$safeDepth"
            val response = api.getRaw(path)
            if (!response.isSuccessful) {
                errorMessage = when (response.code()) {
                    401 -> "Session expired. Please sign in again."
                    403 -> "This member is outside your genealogy."
                    404 -> "Member not found."
                    in 500..599 -> "Server error (${response.code()}). Please try again."
                    else -> response.errorBody()?.string()?.take(220) ?: "Could not load team."
                }
                return
            }
            val json = response.body()?.asJsonObject ?: error("Empty server response.")
            if (json.get("success")?.asBoolean != true) error(json.get("message")?.asString ?: "Could not load team.")
            val data = json.getAsJsonObject("data") ?: error("Invalid server response.")
            val newRoot = parseUniMember(data.getAsJsonObject("root") ?: error("Root member missing."))
            if (pushHistory) root?.let { history.add(it) }
            root = newRoot
            currentRootId = newRoot.id
            treeScale = 1f
            treeOffset = Offset.Zero
            directCount = data.get("direct_count")?.asInt ?: newRoot.children.size
            totalCount = data.get("total_count")?.asInt ?: directCount
        } catch (e: Exception) {
            errorMessage = e.message ?: "Network error. Please retry."
        } finally {
            loading = false
        }
    }

    suspend fun searchMembers() {
        val q = searchText.trim()
        if (q.isBlank()) {
            searchHits = emptyList()
            searchMessage = "Enter Member ID, name or phone."
            return
        }
        searching = true
        searchMessage = null
        searchHits = emptyList()
        try {
            val response = api.getRaw("genealogy/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}")
            val json = response.body()?.asJsonObject
            if (response.isSuccessful && json?.get("success")?.asBoolean == true) {
                val d = json.getAsJsonObject("data")
                    ?: json.getAsJsonObject("member")
                    ?: error("Invalid search response.")
                searchHits = listOf(MemberSearchHit(
                    d.get("id")?.asLong ?: 0L,
                    d.get("member_id")?.asString.orEmpty(),
                    d.get("name")?.asString ?: "Member",
                    d.get("role")?.asString.orEmpty()
                ))
            } else {
                searchMessage = json?.get("message")?.asString
                    ?: response.errorBody()?.string()?.take(180)
                    ?: "Member not found."
            }
        } catch (e: Exception) {
            searchMessage = e.message ?: "Search failed. Check your connection."
        } finally {
            searching = false
        }
    }

    LaunchedEffect(refreshKey) { loadMember(currentRootId, depth = visibleLevels) }

    Column(
        Modifier.fillMaxSize()
            .background(Color(0xFFF3F5FA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Genealogy / My Team", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                Text("Uni-Level • Every member has equal opportunity", fontSize = 11.sp, color = Color(0xFF7A8495))
            }
            IconButton(onClick = { refreshKey++ }) { Icon(Icons.Default.Refresh, "Refresh") }
            IconButton(onClick = onRegister) { Icon(Icons.Default.PersonAddAlt1, "Register member", tint = Color(0xFF1769D2)) }
        }

        if (history.isNotEmpty()) {
            TextButton(onClick = {
                val previous = history.removeAt(history.lastIndex)
                // Reload the previous root from the API so the full selected depth
                // and true descendant count are restored (not just direct children).
                scope.launch { loadMember(previous.id, pushHistory = false, depth = visibleLevels) }
            }) {
                Icon(Icons.Default.ArrowBack, null)
                Text(" Back to previous team")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it; searchMessage = null },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Search Member ID, name or phone", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchText.isNotBlank()) IconButton(onClick = {
                        searchText = ""; searchHits = emptyList(); searchMessage = null
                    }) { Icon(Icons.Default.Close, "Clear") }
                }
            )
            Spacer(Modifier.width(6.dp))
            FilledTonalIconButton(onClick = { scope.launch { searchMembers() } }, enabled = !searching) {
                if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Search, "Search")
            }
        }

        AnimatedVisibility(searchHits.isNotEmpty() || searchMessage != null) {
            Column(Modifier.fillMaxWidth().padding(top = 7.dp)) {
                searchHits.forEach { hit ->
                    Card(
                        Modifier.fillMaxWidth().padding(bottom = 5.dp).clickable {
                            searchHits = emptyList()
                            searchMessage = null
                            scope.launch { loadMember(hit.id, pushHistory = true) }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF1769D2))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(hit.name, fontWeight = FontWeight.Bold)
                                Text("${hit.memberId} • ${hit.role}", color = Color.Gray, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
                searchMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UniSummary("Direct team", directCount.toString(), Modifier.weight(1f), Color(0xFF008F83))
            UniSummary("Total downline", totalCount.toString(), Modifier.weight(1f), Color(0xFF2869D8))
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
            Text("Loading your hierarchy…", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        }
        errorMessage?.let {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Default.CloudOff, null, tint = Color(0xFFD95757))
                    Text("Could not load genealogy", fontWeight = FontWeight.Bold)
                    Text(it, color = Color.Gray, fontSize = 12.sp)
                    TextButton(onClick = { refreshKey++ }) { Text("Try again") }
                }
            }
        }

        root?.let { current ->
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewMode == "chart",
                    onClick = { viewMode = "chart" },
                    label = { Text("Uni-Level Chart", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.AccountTree, null, Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = viewMode == "classic",
                    onClick = { viewMode = "classic" },
                    label = { Text("Classic Team", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Groups, null, Modifier.size(16.dp)) }
                )
            }
            if (viewMode == "classic") {
                Text("CLASSIC TEAM VIEW", fontSize = 10.sp, color = Color(0xFF7D8798), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                UniMemberCard(
                    current, isRoot = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    onOpen = { detailMember = current }, onInfo = { detailMember = current }
                )
                var classicMembers = current.children
                for (level in 1..visibleLevels) {
                    if (classicMembers.isEmpty()) break
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("LEVEL $level", fontSize = 10.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${classicMembers.size} members", fontSize = 8.sp, color = Color.Gray)
                    }
                    classicMembers.forEach { member ->
                        Card(
                            Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable {
                                scope.launch { loadMember(member.id, pushHistory = true, depth = visibleLevels) }
                            },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFE7F0FF)), contentAlignment = Alignment.Center) {
                                    Text(member.name.take(1).uppercase(), color = Color(0xFF245DA8), fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${member.memberId} • ${member.role}", fontSize = 8.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { detailMember = member }) { Icon(Icons.Default.Info, "Member details", tint = Color(0xFF2869D8)) }
                                IconButton(onClick = { scope.launch { loadMember(member.id, pushHistory = true, depth = visibleLevels) } }) {
                                    Icon(Icons.Default.ChevronRight, "Open team")
                                }
                            }
                        }
                    }
                    classicMembers = classicMembers.flatMap { it.children }
                }
            } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NETWORK VIEW", fontSize = 10.sp, color = Color(0xFF7D8798), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Showing $visibleLevels / 10 levels", fontSize = 11.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.SemiBold)
            }
            Card(
                Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Visible levels", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("$visibleLevels of 10", fontSize = 12.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = visibleLevels.toFloat(),
                        onValueChange = { visibleLevels = it.toInt().coerceIn(1, 10) },
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChangeFinished = { scope.launch { loadMember(currentRootId, depth = visibleLevels) } }
                    )
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("1 level", fontSize = 8.sp, color = Color.Gray)
                        Text("Default: 4", fontSize = 8.sp, color = Color.Gray)
                        Text("Maximum: 10", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Pinch to zoom • Drag to move", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                IconButton(onClick = { treeScale = (treeScale * 1.2f).coerceAtMost(2.5f) }) {
                    Icon(Icons.Default.ZoomIn, "Zoom in")
                }
                IconButton(onClick = { treeScale = (treeScale / 1.2f).coerceAtLeast(0.55f) }) {
                    Icon(Icons.Default.ZoomOut, "Zoom out")
                }
                IconButton(onClick = { treeScale = 1f; treeOffset = Offset.Zero }) {
                    Icon(Icons.Default.CenterFocusStrong, "Reset zoom")
                }
            }

            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                treeScale = (treeScale * zoomChange).coerceIn(0.55f, 2.5f)
                treeOffset += panChange
            }
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEAF0F8))
                    .transformable(transformState)
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    Modifier.graphicsLayer {
                        scaleX = treeScale
                        scaleY = treeScale
                        translationX = treeOffset.x
                        translationY = treeOffset.y
                    }.padding(horizontal = 10.dp)
                ) {
                    UniMemberCard(
                        current, isRoot = true, modifier = Modifier.width(166.dp).height(142.dp).align(Alignment.CenterHorizontally),
                        onOpen = { detailMember = current },
                        onInfo = { detailMember = current }
                    )
                    var levelMembers = current.children
                    for (level in 1..visibleLevels) {
                        val membersAtLevel = levelMembers
                        if (membersAtLevel.isEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LEVEL $level", Modifier.width(76.dp), fontSize = 9.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.Bold)
                                Text("No members in this level", fontSize = 8.sp, color = Color.Gray)
                            }
                            break
                        }

                        // Website-style level board: fixed level information on the left,
                        // independently horizontally scrollable member cards on the right.
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(top = 7.dp)
                                .height(154.dp)
                                .background(Color(0xFFF7F9FD), RoundedCornerShape(11.dp))
                        ) {
                            Column(
                                Modifier.width(76.dp).fillMaxHeight()
                                    .background(Color(0xFFEFF3FA), RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp))
                                    .padding(horizontal = 7.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(color = Color(0xFF4F46E5), shape = RoundedCornerShape(20.dp)) {
                                    Text("LEVEL $level", Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                }
                                Spacer(Modifier.height(7.dp))
                                Text("${membersAtLevel.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26344C))
                                Text("members", fontSize = 9.sp, color = Color(0xFF718096))
                                Spacer(Modifier.height(5.dp))
                                Text("Team level", fontSize = 8.sp, color = Color(0xFF8792A4))
                            }

                            Row(
                                Modifier.weight(1f).fillMaxHeight()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 7.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                membersAtLevel.forEach { member ->
                                    UniMemberCard(
                                        member = member,
                                        isRoot = false,
                                        modifier = Modifier.width(132.dp).height(140.dp),
                                        onOpen = { scope.launch { loadMember(member.id, pushHistory = true, depth = visibleLevels) } },
                                        onInfo = { detailMember = member }
                                    )
                                }
                            }
                        }
                        levelMembers = membersAtLevel.flatMap { it.children }
                    }
                }
            }
            Text(
                "Tap any member card to open their own network. Use ⓘ for member details.",
                fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 7.dp, bottom = 8.dp)
            )
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    detailMember?.let { member ->
        AlertDialog(
            onDismissRequest = { detailMember = null },
            icon = { Icon(Icons.Default.Info, null, tint = Color(0xFF1769D2)) },
            title = { Text("Member Details") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DetailLine("Name", member.name)
                    DetailLine("Member ID", member.memberId)
                    DetailLine("Designation", member.role)
                    DetailLine("Rank", member.rank)
                    DetailLine("Phone", member.phone)
                    DetailLine("Email", member.email)
                    DetailLine("Referral Code", member.referralCode)
                    DetailLine("Status", member.status)
                    DetailLine("Joined", member.joinedAt)
                    DetailLine("Sponsor", member.sponsorName)
                    DetailLine("Sponsor Member ID", member.sponsorMemberId)
                    DetailLine("Parent", member.parentName)
                    DetailLine("Parent Member ID", member.parentMemberId)
                    DetailLine("Direct Team", member.teamCount.toString())
                }
            },
            confirmButton = { TextButton(onClick = { detailMember = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun UniSummary(label: String, value: String, modifier: Modifier, tint: Color) {
    Card(modifier, shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) {
            Text(value, color = tint, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun UniMemberCard(
    member: UniMember,
    isRoot: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onInfo: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = if (isRoot) Color(0xFFEAF2FF) else Color.White),
        border = BorderStroke(if (isRoot) 1.5.dp else 1.dp, if (isRoot) Color(0xFF3977D8) else Color(0xFFE5EAF1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(29.dp).clip(CircleShape)
                        .background(if (isRoot) Color(0xFF1D4F91) else Color(0xFFE4F2F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(member.name.take(1).uppercase(), color = if (isRoot) Color.White else Color(0xFF087F78), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF172943), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(member.role.ifBlank { "Member" }, fontSize = 8.sp, color = Color(0xFF68778D), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onInfo, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Info, "Member details", tint = Color(0xFF2869D8), modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text("ID: ${member.memberId}", fontSize = 8.sp, color = Color(0xFF26364E), fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (member.rank.isNotBlank()) {
                Text(member.rank, fontSize = 7.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (member.parentMemberId.isNotBlank()) {
                Text("Under: ${member.parentMemberId}", fontSize = 7.sp, color = Color(0xFF0F766E), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (member.phone.isNotBlank()) {
                Text(member.phone, fontSize = 7.sp, color = Color(0xFF8792A4), maxLines = 1)
            }
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (member.status == "ACTIVE") Color(0xFFE2F6EC) else Color(0xFFFFF0E8), shape = RoundedCornerShape(20.dp)) {
                    Text(member.status, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), fontSize = 7.sp, color = if (member.status == "ACTIVE") Color(0xFF16834A) else Color(0xFFB66A20), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text("${member.teamCount} team", fontSize = 8.sp, color = Color(0xFF778398))
                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF2869D8), modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
            Text(label, Modifier.width(105.dp), fontSize = 12.sp, color = Color.Gray)
            Text(value, Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF1B2B43), fontWeight = FontWeight.Medium)
        }
        HorizontalDivider(color = Color(0xFFEAEFF5))
    }
}

private fun parseUniMember(o: JsonObject): UniMember {
    val children = o.getAsJsonArray("children")?.mapNotNull {
        runCatching { parseUniMember(it.asJsonObject) }.getOrNull()
    } ?: emptyList()
    return UniMember(
        id = o.get("id")?.asLong ?: 0L,
        memberId = o.get("member_id")?.asString.orEmpty(),
        name = o.get("name")?.asString ?: "Member",
        phone = o.get("phone")?.asString.orEmpty(),
        email = o.get("email")?.asString.orEmpty(),
        role = o.get("role")?.asString.orEmpty(),
        rank = o.get("rank")?.asString.orEmpty(),
        status = o.get("status")?.asString ?: "UNKNOWN",
        referralCode = o.get("referral_code")?.asString.orEmpty(),
        joinedAt = o.get("joined_at")?.asString.orEmpty(),
        sponsorName = o.get("sponsor_name")?.asString.orEmpty(),
        sponsorMemberId = o.get("sponsor_member_id")?.asString.orEmpty(),
        parentName = o.get("parent_name")?.asString.orEmpty(),
        parentMemberId = o.get("parent_member_id")?.asString.orEmpty(),
        teamCount = o.get("team_count")?.asInt ?: children.size,
        children = children
    )
}
