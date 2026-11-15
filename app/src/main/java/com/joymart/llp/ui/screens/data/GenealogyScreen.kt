package com.joymart.llp.ui.screens.data

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.google.gson.JsonObject
import com.joymart.llp.data.JoyMartApi
import kotlinx.coroutines.launch
import kotlin.math.max

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
    var activeLinesId by remember { mutableStateOf<Long?>(null) }
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
            activeLinesId = null
            treeScale = 1f
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
                val d = json.getAsJsonObject("data") ?: json.getAsJsonObject("member") ?: error("Invalid search response.")
                searchHits = listOf(MemberSearchHit(
                    d.get("id")?.asLong ?: 0L,
                    d.get("member_id")?.asString.orEmpty(),
                    d.get("name")?.asString ?: "Member",
                    d.get("role")?.asString.orEmpty()
                ))
            } else searchMessage = json?.get("message")?.asString ?: response.errorBody()?.string()?.take(180) ?: "Member not found."
        } catch (e: Exception) {
            searchMessage = e.message ?: "Search failed. Check your connection."
        } finally {
            searching = false
        }
    }

    LaunchedEffect(refreshKey) { loadMember(currentRootId, depth = visibleLevels) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF3F5FA)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Genealogy / My Team", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172943))
                Text("Uni-Level • Every member has equal opportunity", fontSize = 9.sp, color = Color(0xFF7A8495))
            }
            IconButton(onClick = { refreshKey++ }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Refresh, "Refresh", Modifier.size(19.dp)) }
            IconButton(onClick = onRegister, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.PersonAddAlt1, "Register member", tint = Color(0xFF1769D2), modifier = Modifier.size(19.dp)) }
        }

        if (history.isNotEmpty()) {
            TextButton(onClick = {
                val previous = history.removeAt(history.lastIndex)
                scope.launch { loadMember(previous.id, depth = visibleLevels) }
            }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Icon(Icons.Default.ArrowBack, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Back to previous team", fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it; searchMessage = null },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                placeholder = { Text("Search Member ID, name or phone", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = { if (searchText.isNotBlank()) IconButton(onClick = { searchText = ""; searchHits = emptyList(); searchMessage = null }) { Icon(Icons.Default.Close, "Clear", Modifier.size(17.dp)) } }
            )
            Spacer(Modifier.width(5.dp))
            FilledTonalIconButton(onClick = { scope.launch { searchMembers() } }, enabled = !searching, modifier = Modifier.size(42.dp)) {
                if (searching) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Search, "Search", Modifier.size(18.dp))
            }
        }

        if (searchHits.isNotEmpty() || searchMessage != null) {
            Column(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                searchHits.forEach { hit ->
                    Card(Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable {
                        searchHits = emptyList(); searchMessage = null
                        scope.launch { loadMember(hit.id, pushHistory = true) }
                    }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color(0xFF1769D2))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text(hit.name, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("${hit.memberId} • ${hit.role}", color = Color.Gray, fontSize = 9.sp) }
                            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                        }
                    }
                }
                searchMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp, modifier = Modifier.padding(6.dp)) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            GenealogySummary("Direct team", directCount.toString(), Modifier.weight(1f), Color(0xFF008F83))
            GenealogySummary("Total downline", totalCount.toString(), Modifier.weight(1f), Color(0xFF2869D8))
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 7.dp))
        errorMessage?.let { msg ->
            Card(Modifier.fillMaxWidth().padding(top = 9.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Could not load genealogy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(msg, color = Color.Gray, fontSize = 10.sp)
                    TextButton(onClick = { refreshKey++ }) { Text("Try again", fontSize = 11.sp) }
                }
            }
        }

        root?.let { current ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = viewMode == "chart", onClick = { viewMode = "chart" }, label = { Text("Uni-Level Chart", fontSize = 9.sp) }, leadingIcon = { Icon(Icons.Default.AccountTree, null, Modifier.size(14.dp)) })
                FilterChip(selected = viewMode == "classic", onClick = { viewMode = "classic" }, label = { Text("Classic Team", fontSize = 9.sp) }, leadingIcon = { Icon(Icons.Default.Groups, null, Modifier.size(14.dp)) })
            }

            if (viewMode == "classic") {
                CompactMemberCard(current, isRoot = true, Modifier.fillMaxWidth().padding(top = 7.dp), onOpen = {}, onInfo = { detailMember = current }, onLines = { activeLinesId = if (activeLinesId == current.id) null else current.id }, highlighted = false)
                var levelMembers = current.children
                for (level in 1..visibleLevels) {
                    if (levelMembers.isEmpty()) break
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("LEVEL $level", fontSize = 9.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${levelMembers.size} members", fontSize = 8.sp, color = Color.Gray)
                    }
                    levelMembers.forEach { member ->
                        CompactMemberCard(member, false, Modifier.fillMaxWidth().padding(bottom = 5.dp),
                            onOpen = { scope.launch { loadMember(member.id, true) } }, onInfo = { detailMember = member },
                            onLines = { activeLinesId = if (activeLinesId == member.id) null else member.id }, highlighted = false)
                    }
                    levelMembers = levelMembers.flatMap { it.children }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("NETWORK VIEW", fontSize = 9.sp, color = Color(0xFF7D8798), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Showing $visibleLevels / 10 levels", fontSize = 9.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.SemiBold)
                }
                Card(Modifier.fillMaxWidth().padding(bottom = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(11.dp)) {
                    Column(Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Visible levels", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("$visibleLevels of 10", fontSize = 10.sp, color = Color(0xFF1769D2), fontWeight = FontWeight.Bold)
                        }
                        Slider(value = visibleLevels.toFloat(), onValueChange = { visibleLevels = it.toInt().coerceIn(1, 10) }, valueRange = 1f..10f, steps = 8,
                            onValueChangeFinished = { scope.launch { loadMember(currentRootId, depth = visibleLevels) } }, modifier = Modifier.height(30.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1 level", fontSize = 8.sp, color = Color.Gray); Text("Default: 4", fontSize = 8.sp, color = Color.Gray); Text("Maximum: 10", fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Adjust card size • Swipe each level horizontally", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    IconButton(onClick = { treeScale = (treeScale - 0.1f).coerceAtLeast(0.65f) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.ZoomOut, "Make cards smaller", Modifier.size(17.dp)) }
                    Text("${(treeScale * 100).toInt()}%", fontSize = 8.sp, color = Color.Gray)
                    IconButton(onClick = { treeScale = (treeScale + 0.1f).coerceAtMost(1.35f) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.ZoomIn, "Make cards larger", Modifier.size(17.dp)) }
                    IconButton(onClick = { treeScale = 1f }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.CenterFocusStrong, "Reset card size", Modifier.size(17.dp)) }
                }

                val visibleMembersByLevel = remember(current, visibleLevels) {
                    buildList {
                        var layer = current.children
                        for (level in 1..visibleLevels) {
                            add(level to layer)
                            layer = layer.flatMap { it.children }
                        }
                    }
                }
                val allMembers = remember(current, visibleLevels) { flattenMembers(current, visibleLevels) }
                val descendantIds = remember(activeLinesId, allMembers, current) { activeLinesId?.let { getDescendantIds(it, current, allMembers) } ?: emptySet() }
                val cardRects = remember(current.id) { mutableStateMapOf<Long, Rect>() }
                var boardOrigin by remember(current.id) { mutableStateOf(Offset.Zero) }
                val blinkTransition = rememberInfiniteTransition(label = "teamBlink")
                val blinkAlpha by blinkTransition.animateFloat(initialValue = 1f, targetValue = 0.45f, animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "blinkAlpha")

                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFEAF0F8))
                        .onGloballyPositioned { boardOrigin = it.positionInRoot() }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        val rootRect = cardRects[current.id]
                        val stroke = 1.5.dp.toPx()
                        fun drawConnector(parent: Rect, child: Rect, color: Color, active: Boolean) {
                            val safeScale = treeScale.coerceAtLeast(0.01f)
                            val start = Offset((parent.center.x - boardOrigin.x) / safeScale, (parent.bottom - boardOrigin.y) / safeScale)
                            val end = Offset((child.center.x - boardOrigin.x) / safeScale, (child.top - boardOrigin.y) / safeScale)
                            val midY = (start.y + end.y) / 2f
                            val path = Path().apply { moveTo(start.x, start.y); cubicTo(start.x, midY, end.x, midY, end.x, end.y) }
                            drawPath(path, color.copy(alpha = if (active) blinkAlpha else 0.35f), style = Stroke(width = if (active) 2.5.dp.toPx() else stroke, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))) )
                            drawCircle(color.copy(alpha = if (active) blinkAlpha else 0.6f), radius = 2.5.dp.toPx(), center = end)
                        }
                        if (rootRect != null) {
                            allMembers.forEach { child ->
                                val parentId = if (child.parentMemberId == current.memberId) current.id else allMembers.firstOrNull { it.memberId == child.parentMemberId }?.id
                                val parentRect = parentId?.let { cardRects[it] }
                                val childRect = cardRects[child.id]
                                if (parentRect != null && childRect != null) {
                                    val lineActive = activeLinesId != null && (child.id in descendantIds || parentId == activeLinesId)
                                    val lineColor = if (lineActive) Color(0xFF6557E8) else Color(0xFF8A9BC4)
                                    if (activeLinesId == null || lineActive) drawConnector(parentRect, childRect, lineColor, lineActive)
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                        GenealogyLevelRow(label = "ROOT", title = "Main Member", count = "1 member", root = true, rowHeight = (174 * treeScale).dp.coerceAtLeast(112.dp)) {
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                CompactMemberCard(current, true, Modifier.width((142 * treeScale).dp).height((156 * treeScale).dp)
                                    .onGloballyPositioned { coords -> cardRects[current.id] = coords.rectInRoot() },
                                    onOpen = {}, onInfo = { detailMember = current }, onLines = { activeLinesId = if (activeLinesId == current.id) null else current.id }, highlighted = current.id in descendantIds || activeLinesId == current.id, cardScale = treeScale)
                            }
                        }
                        visibleMembersByLevel.forEach { (level, members) ->
                            GenealogyLevelRow(label = "LEVEL $level", title = "Level $level", count = "${members.size} members", rowHeight = (174 * treeScale).dp.coerceAtLeast(112.dp)) {
                                if (members.isEmpty()) {
                                    Text("No members in this level", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp))
                                } else {
                                    Row(Modifier.fillMaxHeight().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        members.forEachIndexed { memberIndex, member ->
                                            val parentIsActive = activeLinesId == member.id || member.id in descendantIds
                                            CompactMemberCard(member, false, Modifier.width((128 * treeScale).dp).height((154 * treeScale).dp)
                                                .onGloballyPositioned { coords -> cardRects[member.id] = coords.rectInRoot() },
                                                onOpen = { scope.launch { loadMember(member.id, true, visibleLevels) } },
                                                onInfo = { detailMember = member },
                                                onLines = { activeLinesId = if (activeLinesId == member.id) null else member.id },
                                                highlighted = parentIsActive, blinkAlpha = if (parentIsActive) blinkAlpha else 1f, serial = memberIndex + 1, cardScale = treeScale)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Tap a card to open its team • Share icon highlights all downline links • ⓘ shows details", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    detailMember?.let { member ->
        AlertDialog(onDismissRequest = { detailMember = null }, icon = { Icon(Icons.Default.Info, null, tint = Color(0xFF1769D2)) }, title = { Text("Member Details") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                DetailLine("Name", member.name); DetailLine("Member ID", member.memberId); DetailLine("Designation", member.role); DetailLine("Rank", member.rank)
                DetailLine("Phone", member.phone); DetailLine("Email", member.email); DetailLine("Referral Code", member.referralCode); DetailLine("Status", member.status)
                DetailLine("Joined", member.joinedAt); DetailLine("Sponsor", member.sponsorName); DetailLine("Sponsor Member ID", member.sponsorMemberId)
                DetailLine("Parent", member.parentName); DetailLine("Parent Member ID", member.parentMemberId); DetailLine("Direct Team", member.teamCount.toString())
            } }, confirmButton = { TextButton(onClick = { detailMember = null }) { Text("Close") } })
    }
}

private fun LayoutCoordinates.rectInRoot(): Rect {
    val p = positionInRoot()
    return Rect(p.x, p.y, p.x + size.width, p.y + size.height)
}

@Composable
private fun GenealogyLevelRow(label: String, title: String, count: String, root: Boolean = false, rowHeight: Dp = 174.dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(rowHeight).padding(bottom = 1.dp).background(if (root) Color(0xFFF8FAFF) else Color.White), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(82.dp).fillMaxHeight().background(Color(0xFFF0F4FB)).padding(horizontal = 7.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
            Surface(color = Color(0xFF5B55E8), shape = RoundedCornerShape(20.dp)) { Text(label, Modifier.padding(horizontal = 6.dp, vertical = 4.dp), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
            Spacer(Modifier.height(6.dp)); Text(title, fontSize = 10.sp, color = Color(0xFF26344C), fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(count, fontSize = 8.sp, color = Color(0xFF718096), maxLines = 1)
        }
        Row(Modifier.weight(1f).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun CompactMemberCard(
    member: UniMember,
    isRoot: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onInfo: () -> Unit,
    onLines: () -> Unit,
    highlighted: Boolean,
    blinkAlpha: Float = 1f,
    serial: Int? = null,
    cardScale: Float = 1f
) {
    val rankColor = remember(member.id, member.rank) { rankColorFor(member.rank, isRoot) }
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(baseDensity.density * cardScale, baseDensity.fontScale * cardScale)) {
        Card(
            modifier = modifier.graphicsLayer { alpha = if (highlighted) blinkAlpha else 1f }.clickable { onOpen() },
            shape = RoundedCornerShape(11.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(if (isRoot || highlighted) 1.8.dp else 1.dp, if (highlighted) rankColor else if (isRoot) Color(0xFF6366F1) else Color(0xFFDDE5F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 5.dp)) {
                Text(if (isRoot) "ROOT" else (serial?.toString() ?: "•"), Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(5.dp)).background(Color(0xFFEAF0FF)).padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 7.sp, color = Color(0xFF3443A5), fontWeight = FontWeight.Bold)
                IconButton(onClick = onLines, modifier = Modifier.align(Alignment.TopEnd).size(20.dp)) { Icon(Icons.Default.Share, "Show team connections", tint = if (highlighted) rankColor else Color(0xFF64748B), modifier = Modifier.size(12.dp)) }
                Column(Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                    Surface(color = rankColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.widthIn(max = 112.dp)) {
                        Text(member.rank.ifBlank { member.role.ifBlank { "DISTRIBUTOR" } }.uppercase(), Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(rankColor), contentAlignment = Alignment.Center) {
                        Text(member.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(member.name, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(member.memberId, fontSize = 7.sp, color = Color(0xFF64748B), maxLines = 1)
                    if (!isRoot && member.parentMemberId.isNotBlank()) {
                        Text("Under: ${member.parentMemberId}", fontSize = 6.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(member.phone.ifBlank { "-" }, fontSize = 6.sp, color = Color(0xFF8792A4), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Surface(color = if (member.status.equals("ACTIVE", true)) Color(0xFFE4F8EA) else Color(0xFFFFF0E8), shape = RoundedCornerShape(20.dp)) {
                        Text(member.status.uppercase(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 6.sp, fontWeight = FontWeight.Bold, color = if (member.status.equals("ACTIVE", true)) Color(0xFF16834A) else Color(0xFFB66A20))
                    }
                    Text("${member.teamCount} Direct Members", fontSize = 6.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 3.dp), maxLines = 1)
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.weight(1f).height(18.dp).clickable(onClick = onOpen), color = Color(0xFFF5F7FF), shape = RoundedCornerShape(6.dp), border = BorderStroke(0.7.dp, Color(0xFFDCE4F5))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.Groups, null, Modifier.size(10.dp), tint = Color(0xFF4053B5)); Spacer(Modifier.width(3.dp)); Text("View Team", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4053B5)) }
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onInfo, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Info, "Member details", Modifier.size(12.dp), tint = Color(0xFF647CEB)) }
                }
            }
        }
    }
}

@Composable
private fun GenealogySummary(label: String, value: String, modifier: Modifier, tint: Color) {
    Card(modifier, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp)) { Text(value, color = tint, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(label, color = Color.Gray, fontSize = 9.sp) }
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

private fun flattenMembers(root: UniMember, depth: Int): List<UniMember> {
    val result = mutableListOf<UniMember>()
    var layer = root.children
    repeat(depth.coerceIn(1, 10)) {
        result.addAll(layer)
        layer = layer.flatMap { it.children }
    }
    return result
}

private fun getDescendantIds(parentId: Long, root: UniMember, members: List<UniMember>): Set<Long> {
    val result = mutableSetOf<Long>()
    val selectedMemberId = if (root.id == parentId) root.memberId else members.firstOrNull { it.id == parentId }?.memberId.orEmpty()
    var frontier = listOf(selectedMemberId)
    if (frontier.firstOrNull().isNullOrBlank()) return emptySet()
    while (frontier.isNotEmpty()) {
        val next = members.filter { it.parentMemberId in frontier && it.id !in result }
        next.forEach { result.add(it.id) }
        frontier = next.map { it.memberId }
    }
    return result
}

private fun rankColorFor(rank: String, root: Boolean): Color {
    if (root) return Color(0xFF2859C5)
    val value = rank.lowercase()
    return when {
        "executive" in value -> Color(0xFF6257E8)
        "diamond" in value -> Color(0xFF0E9F9A)
        "gold" in value -> Color(0xFFD97706)
        "silver" in value -> Color(0xFF64748B)
        "bronze" in value -> Color(0xFFB7791F)
        else -> Color(0xFF16A34A)
    }
}

private fun parseUniMember(o: JsonObject): UniMember {
    val children = o.getAsJsonArray("children")?.mapNotNull { runCatching { parseUniMember(it.asJsonObject) }.getOrNull() } ?: emptyList()
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
