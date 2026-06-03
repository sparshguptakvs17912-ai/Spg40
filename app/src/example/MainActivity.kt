package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PlayerStateEntity
import com.example.data.QuestEntity
import com.example.data.QuestRepository
import com.example.ui.FitnessViewModel
import com.example.ui.FitnessViewModelFactory
import com.example.ui.ToastMessage
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.GoldColor
import com.example.ui.theme.StrengthOrange
import com.example.ui.theme.AgilityEmerald
import com.example.ui.theme.VitalityIndigo
import com.example.ui.theme.IntellectPurple
import com.example.ui.theme.ShadowPointsYellow
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize Room Database and Repository manually as per Simple Constructor Injection guideline
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context)
                val repository = QuestRepository(database)
                val fitnessViewModel: FitnessViewModel = viewModel(
                    factory = FitnessViewModelFactory(repository)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(viewModel = fitnessViewModel)
                }
            }
        }
    }
}

enum class NavigationTab {
    SYSTEM, STORAGE, MARKET, PROFILE
}

@Composable
fun MainAppContent(viewModel: FitnessViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.SYSTEM) }
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val quests by viewModel.allQuests.collectAsStateWithLifecycle()

    // Handle premium custom Game Toasts
    var activeToast by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(Unit) {
        viewModel.notification.collect { msg ->
            activeToast = msg
        }
    }

    LaunchedEffect(activeToast) {
        activeToast?.let {
            kotlinx.coroutines.delay(2200)
            activeToast = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            BottomTabBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Render gates loader if player model is null
            val player = playerState
            if (player == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ACCESSING MONARCH SYSTEM...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Responsive/Adaptive Design support: use dual panes if screen width is extensive (Tablet)
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideScreen = maxWidth > 600.dp
                        if (isWideScreen) {
                            // Split Layout: Permanent Left Bento Stats panel, Content on right
                            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PlayerIdentityCard(player = player, expNeeded = viewModel.getExpNeededForLevel(player.level))
                                    BentoStatsGrid(player = player, onUpgrade = { viewModel.upgradeStat(it) })
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .fillMaxHeight()
                                ) {
                                    when (currentTab) {
                                        NavigationTab.SYSTEM -> SystemTabView(
                                            quests = quests,
                                            viewModel = viewModel
                                        )
                                        NavigationTab.STORAGE -> StorageTabView(player = player, quests = quests)
                                        NavigationTab.MARKET -> MarketTabView(player = player, onPurchase = { id, cost -> viewModel.purchaseItem(id, cost) })
                                        NavigationTab.PROFILE -> ProfileTabView(player = player, expNeeded = viewModel.getExpNeededForLevel(player.level))
                                    }
                                }
                            }
                        } else {
                            // Standard phone view with tab-specific screens
                            when (currentTab) {
                                NavigationTab.SYSTEM -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 24.dp)
                                    ) {
                                        item { PlayerIdentityCard(player = player, expNeeded = viewModel.getExpNeededForLevel(player.level)) }
                                        item { BentoStatsGrid(player = player, onUpgrade = { viewModel.upgradeStat(it) }) }
                                        item {
                                            SystemQuestHeader(
                                                onBulkAdd5 = { viewModel.addToAllQuests(5) },
                                                onBulkAdd10 = { viewModel.addToAllQuests(10) },
                                                onBulkComplete = { viewModel.completeAllQuests() }
                                            )
                                        }
                                        items(quests, key = { it.id }) { quest ->
                                            QuestCard(
                                                quest = quest,
                                                onAddProgress = { amt -> viewModel.addProgressToQuest(quest.id, amt) }
                                            )
                                        }
                                        item {
                                            Button(
                                                onClick = { viewModel.resetQuests() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF3A1E2A),
                                                    contentColor = Color(0xFFFFAACC)
                                                ),
                                                border = BorderStroke(1.dp, Color(0xAAFF6688)),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .testTag("btn_reset_quests")
                                            ) {
                                                Icon(Icons.Filled.Refresh, contentDescription = "Reset Icon")
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "🔄 RESET DAILY QUESTS",
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                NavigationTab.STORAGE -> StorageTabView(player = player, quests = quests)
                                NavigationTab.MARKET -> MarketTabView(player = player, onPurchase = { id, cost -> viewModel.purchaseItem(id, cost) })
                                NavigationTab.PROFILE -> ProfileTabView(player = player, expNeeded = viewModel.getExpNeededForLevel(player.level))
                            }
                        }
                    }
                }
            }

            // High priority custom game notification Toast
            AnimatedVisibility(
                visible = activeToast != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                activeToast?.let { toast ->
                    val borderShadowColor = if (toast.isWarning) Color(0xFFFF6688) else NeonBlue
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xF20F1528)
                        ),
                        border = BorderStroke(1.dp, borderShadowColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .wrapContentWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (toast.isWarning) Icons.Default.Info else Icons.Default.CheckCircle,
                                contentDescription = "Outcome Status",
                                tint = if (toast.isWarning) Color(0xFFFFAACC) else Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = toast.text,
                                color = if (toast.isWarning) Color(0xFFFFAACC) else Color(0xFFEEF2F6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tablet Layout Right Panel Selector for SYSTEM Tab
@Composable
fun SystemTabView(
    quests: List<QuestEntity>,
    viewModel: FitnessViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SystemQuestHeader(
            onBulkAdd5 = { viewModel.addToAllQuests(5) },
            onBulkAdd10 = { viewModel.addToAllQuests(10) },
            onBulkComplete = { viewModel.completeAllQuests() }
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(quests, key = { it.id }) { quest ->
                QuestCard(
                    quest = quest,
                    onAddProgress = { amt -> viewModel.addProgressToQuest(quest.id, amt) }
                )
            }
        }
        Button(
            onClick = { viewModel.resetQuests() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3A1E2A),
                contentColor = Color(0xFFFFAACC)
            ),
            border = BorderStroke(1.dp, Color(0xAAFF6688)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_reset_quests")
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reset Icon")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "🔄 RESET DAILY QUESTS",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Character profile header card with Level, current EXP progress, details, and current RPG Rank.
 */
@Composable
fun PlayerIdentityCard(player: PlayerStateEntity, expNeeded: Int) {
    val rankText = when {
        player.level >= 40 -> "✦ S-RANK SHADOW ✦"
        player.level >= 25 -> "⭐ A-RANK EXECUTOR"
        player.level >= 12 -> "🔰 B-RANK KNIGHT"
        player.level >= 5 -> "⚔️ C-RANK FIGHTER"
        else -> "🌙 E-RANK INITIATE"
    }

    val rankBorderColor = when {
        player.level >= 40 -> GoldColor
        player.level >= 25 -> StrengthOrange
        player.level >= 12 -> AgilityEmerald
        player.level >= 5 -> NeonBlue
        else -> Color(0xFF6C86A3)
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Profile Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚔️ SHADOW MONARCH ⚔️",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonBlue,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SUNG JIN-WOO",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "100 Rep Challenge Active",
                        fontSize = 11.sp,
                        color = Color(0xFF6C86A3)
                    )
                }

                // S-Rank style badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(rankBorderColor.copy(alpha = 0.15f))
                        .border(1.dp, rankBorderColor, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                player.level >= 40 -> "S"
                                player.level >= 25 -> "A"
                                player.level >= 12 -> "B"
                                player.level >= 5 -> "C"
                                else -> "E"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = rankBorderColor,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Level up progression metric bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 LEVEL ${player.level}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = "✨ EXP ${player.exp} / $expNeeded",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFB8D0E8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val progressPercent = (player.exp.toFloat() / expNeeded).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2238))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressPercent)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryBlue, NeonBlue)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xFF171E36))
                    .border(BorderStroke(1.dp, rankBorderColor.copy(alpha = 0.2f)), RoundedCornerShape(40.dp))
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "CURRENT SYSTEM RANK: $rankText",
                    color = rankBorderColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Bento Grid UI structure for managing statistics.
 */
@Composable
fun BentoStatsGrid(player: PlayerStateEntity, onUpgrade: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Stats grid: Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoStatCard(
                title = "⚔️ STRENGTH",
                value = player.strength,
                accentColor = StrengthOrange,
                isPrimary = true,
                modifier = Modifier.weight(1f),
                onAction = { onUpgrade("strength") },
                upgradeTag = "btn_upgrade_strength"
            )
            BentoStatCard(
                title = "🍃 AGILITY",
                value = player.agility,
                accentColor = AgilityEmerald,
                isPrimary = true,
                modifier = Modifier.weight(1f),
                onAction = { onUpgrade("agility") },
                upgradeTag = "btn_upgrade_agility"
            )
        }

        // Stats grid: Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoStatCard(
                title = "🛡️ VITALITY",
                value = player.vitality,
                accentColor = VitalityIndigo,
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onAction = { onUpgrade("vitality") },
                upgradeTag = "btn_upgrade_vitality"
            )
            BentoStatCard(
                title = "🔮 INTELLECT",
                value = player.intellect,
                accentColor = IntellectPurple,
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onAction = { onUpgrade("intellect") },
                upgradeTag = "btn_upgrade_intellect"
            )
        }

        // Stats grid: Row 3 (Shadow Points)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141D33)),
            border = BorderStroke(1.dp, ShadowPointsYellow.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💀 SHADOW POINTS",
                        color = Color(0xFFA1B3D2),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${player.shadowPoints} SP",
                        color = ShadowPointsYellow,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ShadowPointsYellow.copy(alpha = 0.15f))
                        .border(1.dp, ShadowPointsYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💀", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * Single Bento grid block representation for a single Attribute (e.g. Strength / Agility)
 */
@Composable
fun BentoStatCard(
    title: String,
    value: Int,
    accentColor: Color,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
    upgradeTag: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, if (isPrimary) accentColor.copy(alpha = 0.4f) else DarkCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF8AA0BC),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.15f),
                    contentColor = accentColor
                ),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .testTag(upgradeTag)
            ) {
                Text(
                    text = "+ UPGRADE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Bulk actions container view for quickly augmenting workout progress
 */
@Composable
fun SystemQuestHeader(
    onBulkAdd5: () -> Unit,
    onBulkAdd10: () -> Unit,
    onBulkComplete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryBlue))
                    Text(
                        text = "📜 DAILY QUESTS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF331E0A))
                        .border(1.dp, Color(0xFFFF9900), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔥 100 CHALLENGE",
                        color = Color(0xFFFF9900),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bulk actions button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBulkAdd5,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF14273F),
                        contentColor = Color(0xFFBBDDFF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3A86FF)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_bulk_add5")
                ) {
                    Text("+5 ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onBulkAdd10,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF14273F),
                        contentColor = Color(0xFFBBDDFF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3A86FF)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_bulk_add10")
                ) {
                    Text("+10 ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onBulkComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A3B2A),
                        contentColor = Color(0xFFBBFFDD)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .testTag("btn_bulk_complete")
                ) {
                    Text("COMPLETE ALL", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * Quest tracker list card element.
 */
@Composable
fun QuestCard(
    quest: QuestEntity,
    onAddProgress: (Int) -> Unit
) {
    val isComplete = quest.progress >= quest.requirement
    val percentPercent = (quest.progress.toFloat() / quest.requirement).coerceIn(0f, 1f)
    val cardBorderColor = if (isComplete) AgilityEmerald else PrimaryBlue.copy(alpha = 0.3f)

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) Color(0xFF0F1B16) else DarkCard
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quest_item_card_${quest.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.name,
                        color = if (isComplete) AgilityEmerald else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = quest.description,
                        color = Color(0xFF8AA0BC),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Inline rewards pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222B1E))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🏆 +${quest.expReward} XP | 💀 +${quest.pointsReward} SP",
                        color = Color(0xFFFFCC66),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar and incremental adjustment controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Progress Bar Slider
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress: ${quest.progress} / ${quest.requirement} ${quest.unit}",
                            fontSize = 10.sp,
                            color = if (isComplete) AgilityEmerald else NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(percentPercent * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = if (isComplete) AgilityEmerald else NeonBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF172036))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percentPercent)
                                .clip(CircleShape)
                                .background(if (isComplete) AgilityEmerald else PrimaryBlue)
                        )
                    }
                }

                // Increment controls
                Row(
                    modifier = Modifier.weight(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onAddProgress(1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isComplete) Color(0xFF11241C) else Color(0xFF1E2F4C),
                            contentColor = if (isComplete) Color(0xFF325E45) else Color(0xFFBBDDFF)
                        ),
                        enabled = !isComplete,
                        border = BorderStroke(1.dp, if (isComplete) Color.Transparent else Color(0xFF3A86FF)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_quest_${quest.id}_add1")
                    ) {
                        Text("+1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAddProgress(10) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isComplete) Color(0xFF11241C) else Color(0xFF2A5E44),
                            contentColor = if (isComplete) Color(0xFF325E45) else Color.White
                        ),
                        enabled = !isComplete,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(36.dp)
                            .testTag("btn_quest_${quest.id}_add10")
                    ) {
                        Text("+10", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Bottom Nav switcher styled symmetrically with the S-Rank UI themes.
 */
@Composable
fun BottomTabBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101C)),
        border = BorderStroke(1.dp, Color(0xFF1F2A44)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(
                label = "System",
                icon = "🔱",
                isActive = currentTab == NavigationTab.SYSTEM,
                onClick = { onTabSelected(NavigationTab.SYSTEM) },
                testTag = "tab_system"
            )
            TabItem(
                label = "Storage",
                icon = "🎒",
                isActive = currentTab == NavigationTab.STORAGE,
                onClick = { onTabSelected(NavigationTab.STORAGE) },
                testTag = "tab_storage"
            )
            TabItem(
                label = "Market",
                icon = "🛒",
                isActive = currentTab == NavigationTab.MARKET,
                onClick = { onTabSelected(NavigationTab.MARKET) },
                testTag = "tab_market"
            )
            TabItem(
                label = "Profile",
                icon = "👤",
                isActive = currentTab == NavigationTab.PROFILE,
                onClick = { onTabSelected(NavigationTab.PROFILE) },
                testTag = "tab_profile"
            )
        }
    }
}

@Composable
fun TabItem(
    label: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp,
                modifier = Modifier.drawBehind {
                    if (isActive) {
                        drawCircle(
                            color = NeonBlue.copy(alpha = 0.2f),
                            radius = size.minDimension / 1.1f
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) NeonBlue else Color(0xFF7A8B9E),
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * STORAGE/INVENTORY TAB: Display equipments unlocked, unlocked trophies, and logged daily streaks.
 */
@Composable
fun StorageTabView(player: PlayerStateEntity, quests: List<QuestEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Core achievements storage metrics
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🏆 TRAINING RECORDS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RecordRow(label = "Completions", value = "${quests.count { it.progress >= it.requirement }} / ${quests.size}")
                        RecordRow(label = "Total Stats", value = "${player.strength + player.agility + player.vitality + player.intellect}")
                        RecordRow(label = "Rep Target", value = "100 Rep")
                    }
                }
            }
        }

        // Equipment Locks / Milestones Grid
        item {
            Text(
                text = "⚔️ SYSTEM EQUIPMENT INVENTORY",
                color = NeonBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val equipments = listOf(
            EquipmentItem("⚔️ Knight's Dagger", "Strength 15+", "Sharp blade for power strikes", player.strength >= 15),
            EquipmentItem("🍃 Swift Boots", "Agility 15+", "Featherweight shoes for flash speed", player.agility >= 15),
            EquipmentItem("🛡️ Monarch's Cloak", "Vitality 20+", "Resilient protection against damage", player.vitality >= 20),
            EquipmentItem("🔮 Spell Ring", "Intellect 20+", "Empowers concentration & mental mana", player.intellect >= 20)
        )

        items(equipments) { item ->
            val borderBrush = if (item.isUnlocked) AgilityEmerald else Color(0xFF2E3247)
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderBrush),
                colors = CardDefaults.cardColors(containerColor = if (item.isUnlocked) Color(0xFF0F1E19) else DarkCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = if (item.isUnlocked) Color.White else Color(0xFF7A8B9E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = item.benefit,
                            color = Color(0xFF8AA0BC),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (item.isUnlocked) Color(0xFF143022) else Color(0xFF2A1C14))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Requirement: ${item.req}",
                                color = if (item.isUnlocked) AgilityEmerald else StrengthOrange,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (item.isUnlocked) AgilityEmerald.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.1f))
                            .border(1.dp, if (item.isUnlocked) AgilityEmerald else Color.DarkGray, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (item.isUnlocked) "EQUIPPED" else "LOCKED",
                            color = if (item.isUnlocked) AgilityEmerald else Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRow(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFF8AA0BC), fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

data class EquipmentItem(
    val name: String,
    val req: String,
    val benefit: String,
    val isUnlocked: Boolean
)

/**
 * THE SYSTEM MARKET SHOP: Buy buffs with hard-earned Shadow Points!
 */
@Composable
fun MarketTabView(player: PlayerStateEntity, onPurchase: (String, Int) -> Unit) {
    val shopItemsList = listOf(
        ShopItemData("expPotion", "🪄 EXP ELIXIR", "+75 Experience Points", 8),
        ShopItemData("spTome", "📖 SHADOW TOME", "+5 Shadow Points net gain", 6),
        ShopItemData("strBoost", "⚡ STRENGTH BOOST", "+3 to all physical stats", 18),
        ShopItemData("doubleExp", "✨ EXP BOOSTER", "+150 EXP instantly", 15),
        ShopItemData("resetStone", "💎 RESET STONE", "+20 Shadow Points acquired", 12)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13172E)),
                border = BorderStroke(1.dp, ShadowPointsYellow.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏪", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SYSTEM EXCHANGER",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Spend Shadow Points to reinforce your Monarch growth",
                            color = Color(0xFF8C9BBD),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Available Balance: ${player.shadowPoints} SP",
                            color = ShadowPointsYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        items(shopItemsList) { item ->
            val canAfford = player.shadowPoints >= item.cost
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (canAfford) NeonBlue.copy(alpha = 0.4f) else Color(0xFF293245)),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = item.desc,
                            color = Color(0xFF8AA0BC),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Price: ", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = "${item.cost} Shadow Points",
                                color = ShadowPointsYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Button(
                        onClick = { onPurchase(item.id, item.cost) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) ShadowPointsYellow else Color(0xFF1E283A),
                            contentColor = if (canAfford) Color.Black else Color.Gray
                        ),
                        shape = RoundedCornerShape(14.dp),
                        enabled = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("btn_shop_${item.id}")
                    ) {
                        Text(
                            text = "BUY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

data class ShopItemData(
    val id: String,
    val name: String,
    val desc: String,
    val cost: Int
)

/**
 * PROFILE DETAILS TAB: Circular Balance radar chart representation, system title tags, and Rank progression tracker.
 */
@Composable
fun ProfileTabView(player: PlayerStateEntity, expNeeded: Int) {
    val totalStats = player.strength + player.agility + player.vitality + player.intellect
    val currentTitle = when {
        player.level >= 40 -> "Shadow Sovereign"
        player.level >= 25 -> "Demon Slayer Knight"
        player.level >= 12 -> "System Challenger"
        else -> "Monarch Novice"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Character card summary in Profile view
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.15f))
                            .border(1.dp, NeonBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SUNG JIN-WOO",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Title: $currentTitle",
                        color = GoldColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color(0xFF1B233C))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Radar Attributes Display Chart
                    Text(
                        text = "📊 STAT BALANCER HUD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp)
                    ) {
                        // Drawing static balance representation using basic canvas paths
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val maxRadius = size.width / 2

                            // 1. Draw helper concentric webs
                            drawCircle(color = Color(0xFF222C46), radius = maxRadius, style = Stroke(1f))
                            drawCircle(color = Color(0xFF222C46), radius = maxRadius * 0.6f, style = Stroke(1f))
                            drawCircle(color = Color(0xFF222C46), radius = maxRadius * 0.3f, style = Stroke(1f))

                            // 2. Draw axis lines
                            drawLine(color = Color(0xFF1D263D), start = Offset(centerX, 0f), end = Offset(centerX, size.height))
                            drawLine(color = Color(0xFF1D263D), start = Offset(0f, centerY), end = Offset(size.width, centerY))

                            // Calculate actual customized attributes vertices parameters: Strength (up), Agility (right), Vitality (down), Intellect (left)
                            // Scale metrics dynamically: max 200
                            val maxAttributeParam = 200f
                            val strY = centerY - (maxRadius * (player.strength.coerceAtMost(200) / maxAttributeParam))
                            val agiX = centerX + (maxRadius * (player.agility.coerceAtMost(200) / maxAttributeParam))
                            val vitY = centerY + (maxRadius * (player.vitality.coerceAtMost(200) / maxAttributeParam))
                            val intX = centerX - (maxRadius * (player.intellect.coerceAtMost(200) / maxAttributeParam))

                            // 3. Draw Polygon Path for attributes
                            val path = Path().apply {
                                moveTo(centerX, strY)
                                llineTo(agiX, centerY)
                                llineTo(centerX, vitY)
                                llineTo(intX, centerY)
                                close()
                            }

                            drawPath(
                                path = path,
                                color = NeonBlue.copy(alpha = 0.3f)
                            )
                            drawPath(
                                path = path,
                                color = NeonBlue,
                                style = Stroke(width = 3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Labels representing individual vertices inside Radar display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MiniRadarLabel("STR: ${player.strength}", StrengthOrange)
                        MiniRadarLabel("AGI: ${player.agility}", AgilityEmerald)
                        MiniRadarLabel("VIT: ${player.vitality}", VitalityIndigo)
                        MiniRadarLabel("INT: ${player.intellect}", IntellectPurple)
                    }
                }
            }
        }

        // Title Achievements Section
        item {
            Text(
                text = "🎖️ MONARCH TITLES SET",
                color = GoldColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val achievementsList = listOf(
            TitleTrophy("Monarch Novice", "Earned automatically at Level 1", player.level >= 1),
            TitleTrophy("System Challenger", "Achieved by reaching Level 12", player.level >= 12),
            TitleTrophy("Demon Slayer Knight", "Achieved by reaching Level 25", player.level >= 25),
            TitleTrophy("Shadow Sovereign", "Sovereign power unlocked at Level 40", player.level >= 40)
        )

        items(achievementsList) { title ->
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (title.isEarned) GoldColor.copy(alpha = 0.4f) else Color(0xFF26324D)),
                colors = CardDefaults.cardColors(containerColor = if (title.isEarned) Color(0xFF201B11) else DarkCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title.title,
                            color = if (title.isEarned) GoldColor else Color(0xFF7A8B9E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = title.earnedVia,
                            color = Color(0xFF8AA0BC),
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (title.isEarned) GoldColor.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.1f))
                            .border(1.dp, if (title.isEarned) GoldColor else Color.DarkGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (title.isEarned) "UNLOCKED" else "LOCKED",
                            color = if (title.isEarned) GoldColor else Color.Gray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// Canvas line helper
private fun Path.llineTo(x: Float, y: Float) {
    lineTo(x, y)
}

@Composable
fun MiniRadarLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

data class TitleTrophy(
    val title: String,
    val earnedVia: String,
    val isEarned: Boolean
)
