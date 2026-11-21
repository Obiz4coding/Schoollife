package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Accommodation
import com.example.data.model.ActionCategory
import com.example.data.model.ActiveStatusEffect
import com.example.data.model.CampusAction
import com.example.data.model.CampusEvent
import com.example.data.model.CareerStatus
import com.example.data.model.DiaryEntry
import com.example.data.model.EventChoice
import com.example.data.model.LifeStage
import com.example.data.model.ShopItem
import com.example.data.model.StatDeltaSnapshot
import com.example.data.model.StatusEffectType
import com.example.data.model.StudentCareer
import com.example.game.StudentLifeEngine
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EnergyBlue
import com.example.ui.theme.HungerAmber
import com.example.ui.theme.MoneyGreen
import com.example.ui.viewmodel.StageCompletionBanner
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameplayScreen(
    career: StudentCareer?,
    diaryEntries: List<DiaryEntry>,
    activeStatusEffects: List<ActiveStatusEffect>,
    lastStatDelta: StatDeltaSnapshot?,
    activeEvent: CampusEvent?,
    stageCompletion: StageCompletionBanner?,
    statusFeedback: String?,
    isMutating: Boolean,
    snackbarHostState: SnackbarHostState,
    onActionClick: (StudentCareer, CampusAction) -> Unit,
    onResolveEvent: (StudentCareer, EventChoice) -> Unit,
    onAdvanceStage: (StudentCareer) -> Unit,
    onFinalizeCareer: (StudentCareer, Boolean) -> Unit,
    onBuyItem: (StudentCareer, ShopItem) -> Unit,
    onUpgradeLodge: (StudentCareer) -> Unit,
    onNewLifeClick: () -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ActionCategory.ACADEMICS) }
    var showShopSheet by remember { mutableStateOf(false) }
    var inspectedEffect by remember { mutableStateOf<ActiveStatusEffect?>(null) }

    LaunchedEffect(statusFeedback) {
        if (statusFeedback != null) {
            snackbarHostState.showSnackbar(statusFeedback)
            onClearFeedback()
        }
    }

    if (career == null) {
        EmptyCampusState(onNewLifeClick = onNewLifeClick, modifier = modifier)
        return
    }

    val actions = remember(career.stage, career.strikeDaysLeft, career.archetype, career.itemsOwned, career.accommodation) {
        StudentLifeEngine.actionsForCareer(career).filter { it.category == selectedCategory }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Banner Card
            item {
                StageHeroCard(
                    career = career,
                    onOpenShop = { showShopSheet = true }
                )
            }

            // 2. Stage Progression Map (JSS1 -> Uni -> PG -> Prof)
            item {
                StageProgressRow(career = career)
            }

            // 2b. Active Status Effects (Burnout, Academic Excellence, Hustle Streak, Sapa Alert)
            if (activeStatusEffects.isNotEmpty()) {
                item {
                    StatusEffectsSection(
                        effects = activeStatusEffects,
                        onEffectClick = { inspectedEffect = it }
                    )
                }
            }

            // 3. Vital Stats Dashboard with Stat-Change Animations
            item {
                VitalsGrid(
                    career = career,
                    lastStatDelta = lastStatDelta,
                    isWide = isWide
                )
            }

            // 4. Terminal Banner if career ended, or Core 4-Button Action Bar (Study, Work, Eat, Sleep)
            if (career.statusEnum.isTerminal()) {
                item {
                    TerminalOutcomeCard(
                        career = career,
                        onStartNewLife = onNewLifeClick
                    )
                }
            } else {
                item {
                    QuickCoreActionsRow(
                        career = career,
                        enabled = !isMutating,
                        onActionClick = onActionClick
                    )
                }
                // 5. Category Tabs + Market Shortcut
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Choices",
                            style = MaterialTheme.typography.titleLarge
                        )
                        TextButton(
                            onClick = { showShopSheet = true },
                            modifier = Modifier.testTag("open_shop_button")
                        ) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Campus Market")
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ActionCategory.entries) { category ->
                            val selected = selectedCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = category },
                                label = { Text(category.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("category_${category.name.lowercase()}")
                            )
                        }
                    }
                }

                // 6. Action Deck
                items(actions, key = { it.id }) { action ->
                    ActionDeckCard(
                        action = action,
                        enabled = !isMutating,
                        onClick = { onActionClick(career, action) }
                    )
                }
            }

            // 7. Campus Event & Action Log
            item {
                CampusDiaryCard(
                    latestSummary = career.lastLogSummary,
                    entries = diaryEntries
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Status Effect Detail Dialog
    inspectedEffect?.let { effect ->
        val isPositive = effect.type.isPositive
        AlertDialog(
            onDismissRequest = { inspectedEffect = null },
            icon = {
                Icon(
                    imageVector = statusEffectIcon(effect.type),
                    contentDescription = null,
                    tint = if (isPositive) MoneyGreen else DangerRed
                )
            },
            title = {
                Text(
                    text = effect.type.title,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = if (isPositive) MoneyGreen.copy(alpha = 0.14f) else DangerRed.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = "${effect.type.badgeText} · ${effect.turnsRemaining} turns left",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPositive) MoneyGreen else DangerRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = effect.type.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedEffect = null }) {
                    Text("Got it")
                }
            }
        )
    }

    // Interactive Dilemma Modal
    if (activeEvent != null) {
        AlertDialog(
            onDismissRequest = { /* Force player to pick a choice */ },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            title = {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = activeEvent.tag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeEvent.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = activeEvent.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    activeEvent.choices.forEachIndexed { index, choice ->
                        Button(
                            onClick = { onResolveEvent(career, choice) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("event_choice_$index"),
                            colors = if (index == 0) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        ) {
                            Text(choice.label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Stage Completion / Promotion Modal
    if (stageCompletion != null) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = if (stageCompletion.passed) Icons.Default.EmojiEvents else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (stageCompletion.passed) MaterialTheme.colorScheme.secondary else DangerRed
                )
            },
            title = {
                Text(
                    text = if (stageCompletion.passed) "Stage Milestone Reached!" else "Term / Session Ended",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stageCompletion.message,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                if (stageCompletion.canAdvanceNext && stageCompletion.nextStageName != null) {
                    Button(
                        onClick = { onAdvanceStage(career) },
                        modifier = Modifier.testTag("advance_next_stage_button")
                    ) {
                        Text("Continue to ${stageCompletion.nextStageName}")
                    }
                } else {
                    Button(
                        onClick = { onFinalizeCareer(career, stageCompletion.passed) },
                        modifier = Modifier.testTag("finalize_career_button")
                    ) {
                        Text("Record to Hall of Fame")
                    }
                }
            },
            dismissButton = {
                if (stageCompletion.canAdvanceNext) {
                    TextButton(
                        onClick = { onFinalizeCareer(career, true) },
                        modifier = Modifier.testTag("retire_now_button")
                    ) {
                        Text("Graduate & Retire Here")
                    }
                }
            }
        )
    }

    // Campus Market & Upgrades Sheet
    if (showShopSheet) {
        ModalBottomSheet(onDismissRequest = { showShopSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Campus Market & Upgrades",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = StudentLifeEngine.formatNaira(career.money),
                        style = MaterialTheme.typography.titleMedium,
                        color = MoneyGreen
                    )
                }

                // Accommodation upgrade card
                val hasLodge = career.accommodationEnum == Accommodation.LODGE ||
                    career.accommodationEnum == Accommodation.STAFF_QUARTERS
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Off-Campus Solar Lodge", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = if (hasLodge) "Active: 24/7 light & cool fan rest." else "Escape hostel mosquitoes & NEPA blackouts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onUpgradeLodge(career) },
                            enabled = !hasLodge && career.money >= Accommodation.LODGE.upgradeCost
                        ) {
                            Text(if (hasLodge) "Owned" else "₦60k")
                        }
                    }
                }

                HorizontalDivider()

                StudentLifeEngine.shopItems.forEach { item ->
                    val owned = career.itemsOwned.contains(item.id)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    item.buffLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { onBuyItem(career, item) },
                                enabled = !owned && career.money >= item.price,
                                modifier = Modifier.testTag("buy_${item.id.lowercase()}")
                            ) {
                                Text(if (owned) "Owned" else "₦${StudentLifeEngine.formatShort(item.price)}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageHeroCard(
    career: StudentCareer,
    onOpenShop: () -> Unit
) {
    val bannerRes = when (career.stageEnum) {
        LifeStage.SECONDARY -> R.drawable.img_secondary_banner
        LifeStage.UNIVERSITY -> R.drawable.img_campus_hero
        LifeStage.POSTGRADUATE -> R.drawable.img_postgrad_banner
        LifeStage.LECTURER -> R.drawable.img_lecturer_banner
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
        ) {
            Image(
                painter = painterResource(id = bannerRes),
                contentDescription = "${career.stageEnum.displayName} banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )
            // Top Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = "${career.stageEnum.displayName.uppercase()} · ${career.levelLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = "DAY ${career.day}/30",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Bottom Character Info
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = career.characterName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${career.schoolName} · ${career.archetypeEnum.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (career.strikeDaysLeft > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = DangerRed,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "ASUU STRIKE (${career.strikeDaysLeft}d left)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Surface(
                    onClick = onOpenShop,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = "Open Market",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${career.itemsOwned.size} Gear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageProgressRow(career: StudentCareer) {
    val stages = LifeStage.entries
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stages.forEachIndexed { idx, stage ->
                val isCompleted = career.completedStages.contains(stage.name) ||
                    (stage == LifeStage.LECTURER && career.statusEnum == CareerStatus.RETIRED_PROF)
                val isCurrent = career.stageEnum == stage && !isCompleted
                val pillColor = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.onSecondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = pillColor,
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = when (stage) {
                                    LifeStage.SECONDARY -> "Sec"
                                    LifeStage.UNIVERSITY -> "Uni"
                                    LifeStage.POSTGRADUATE -> "PhD"
                                    LifeStage.LECTURER -> "Prof"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                    if (idx < stages.lastIndex) {
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusEffectsSection(
    effects: List<ActiveStatusEffect>,
    onEffectClick: (ActiveStatusEffect) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("status_effects_section"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Active Status Effects",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(effects, key = { it.type.name }) { effect ->
                val isPositive = effect.type.isPositive
                val container = if (isPositive) {
                    MoneyGreen.copy(alpha = 0.14f)
                } else {
                    DangerRed.copy(alpha = 0.14f)
                }
                val content = if (isPositive) MoneyGreen else DangerRed
                Surface(
                    onClick = { onEffectClick(effect) },
                    color = container,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("status_effect_${effect.type.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = statusEffectIcon(effect.type),
                            contentDescription = effect.type.title,
                            tint = content,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = effect.type.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = content
                            )
                            Text(
                                text = "${effect.type.badgeText} · ${effect.turnsRemaining}t",
                                style = MaterialTheme.typography.labelSmall,
                                color = content.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun statusEffectIcon(type: StatusEffectType): ImageVector = when (type) {
    StatusEffectType.ACADEMIC_EXCELLENCE -> Icons.Default.AutoAwesome
    StatusEffectType.BURNOUT -> Icons.Default.LocalFireDepartment
    StatusEffectType.HUSTLE_GRIND -> Icons.Default.TrendingUp
    StatusEffectType.SAPA_ALERT -> Icons.Default.Warning
}

@Composable
private fun rememberDeltaFlash(sequenceId: Long?, hasDelta: Boolean): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(sequenceId, hasDelta) {
        if (sequenceId != null && hasDelta) {
            visible = true
            delay(1350L)
            visible = false
        } else {
            visible = false
        }
    }
    return visible
}

@Composable
private fun VitalsGrid(
    career: StudentCareer,
    lastStatDelta: StatDeltaSnapshot?,
    isWide: Boolean
) {
    val stage = career.stageEnum
    val academicValueText = when (stage) {
        LifeStage.UNIVERSITY -> String.format(Locale.US, "%.2f / 5.00", career.academicStat)
        else -> "${career.academicStat.roundToInt()}%"
    }
    val academicProgress = (career.academicStat / stage.maxAcademic).toFloat().coerceIn(0f, 1f)

    val seq = lastStatDelta?.sequenceId
    val moneyDelta = lastStatDelta?.moneyDelta ?: 0L
    val energyDelta = lastStatDelta?.energyDelta ?: 0L
    val hungerDelta = lastStatDelta?.hungerDelta ?: 0L
    val academicDelta = lastStatDelta?.academicDelta ?: 0.0
    val socialDelta = lastStatDelta?.socialDelta ?: 0L
    val heatDelta = lastStatDelta?.heatDelta ?: 0L

    val moneyDeltaText = if (moneyDelta != 0L) {
        "${if (moneyDelta > 0) "+" else "-"}₦${StudentLifeEngine.formatShort(abs(moneyDelta))}"
    } else null
    val academicDeltaText = if (abs(academicDelta) >= 0.005) {
        if (stage == LifeStage.UNIVERSITY) {
            String.format(Locale.US, "%+.2f", academicDelta)
        } else {
            String.format(Locale.US, "%+.1f%%", academicDelta)
        }
    } else null
    val energyDeltaText = if (energyDelta != 0L) "${if (energyDelta > 0) "+" else ""}$energyDelta%" else null
    val hungerDeltaText = if (hungerDelta != 0L) "${if (hungerDelta > 0) "+" else ""}$hungerDelta%" else null
    val socialDeltaText = if (socialDelta != 0L) "${if (socialDelta > 0) "+" else ""}$socialDelta%" else null
    val heatDeltaText = if (heatDelta != 0L) "${if (heatDelta > 0) "+" else ""}$heatDelta%" else null

    val moneyFlash = rememberDeltaFlash(seq, moneyDelta != 0L)
    val baseWalletColor = if (career.money < 1500) DangerRed else MoneyGreen
    val animatedWalletColor by animateColorAsState(
        targetValue = when {
            moneyFlash && moneyDelta > 0 -> MoneyGreen
            moneyFlash && moneyDelta < 0 -> DangerRed
            else -> baseWalletColor
        },
        animationSpec = tween(durationMillis = 320),
        label = "wallet_color"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Money Card with +/- floating animation
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Money",
                                tint = MoneyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wallet", style = MaterialTheme.typography.labelLarge)
                        }
                        FloatingDeltaBadge(
                            visible = moneyFlash && moneyDeltaText != null,
                            text = moneyDeltaText.orEmpty(),
                            isPositive = moneyDelta > 0
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = StudentLifeEngine.formatNaira(career.money),
                        style = MaterialTheme.typography.titleLarge,
                        color = animatedWalletColor
                    )
                    Text(
                        text = career.accommodationEnum.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Academic Stat Card
            StatMeterCard(
                icon = Icons.Default.School,
                label = stage.academicLabel,
                valueText = academicValueText,
                progress = academicProgress,
                accentColor = AcademicPurple,
                deltaSequenceId = seq,
                deltaText = academicDeltaText,
                isDeltaPositive = academicDelta > 0,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMeterCard(
                icon = Icons.Default.Bolt,
                label = "Energy",
                valueText = "${career.energy}%",
                progress = (career.energy / 100f).coerceIn(0f, 1f),
                accentColor = if (career.energy < 25) DangerRed else EnergyBlue,
                deltaSequenceId = seq,
                deltaText = energyDeltaText,
                isDeltaPositive = energyDelta > 0,
                modifier = Modifier.weight(1f)
            )
            StatMeterCard(
                icon = Icons.Default.Restaurant,
                label = "Fullness",
                valueText = "${career.hunger}%",
                progress = (career.hunger / 100f).coerceIn(0f, 1f),
                accentColor = if (career.hunger < 25) DangerRed else HungerAmber,
                deltaSequenceId = seq,
                deltaText = hungerDeltaText,
                isDeltaPositive = hungerDelta > 0,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary row: Social/Mental/Respect + Risk Heat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMeterCard(
                icon = if (stage == LifeStage.POSTGRADUATE) Icons.Default.Favorite else Icons.Default.Groups,
                label = stage.socialLabel,
                valueText = "${career.socialStat}%",
                progress = (career.socialStat / 100f).coerceIn(0f, 1f),
                accentColor = MaterialTheme.colorScheme.primary,
                deltaSequenceId = seq,
                deltaText = socialDeltaText,
                isDeltaPositive = socialDelta > 0,
                modifier = Modifier.weight(1f)
            )
            StatMeterCard(
                icon = Icons.Default.LocalFireDepartment,
                label = "Wahala Heat",
                valueText = "${career.riskHeat}%",
                progress = (career.riskHeat / 100f).coerceIn(0f, 1f),
                accentColor = if (career.riskHeat >= 60) DangerRed else MaterialTheme.colorScheme.tertiary,
                deltaSequenceId = seq,
                deltaText = heatDeltaText,
                isDeltaPositive = heatDelta < 0,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FloatingDeltaBadge(
    visible: Boolean,
    text: String,
    isPositive: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 } + scaleIn(initialScale = 0.85f),
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 2 }
    ) {
        val badgeColor = if (isPositive) MoneyGreen else DangerRed
        Surface(
            color = badgeColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StatMeterCard(
    icon: ImageVector,
    label: String,
    valueText: String,
    progress: Float,
    accentColor: Color,
    deltaSequenceId: Long? = null,
    deltaText: String? = null,
    isDeltaPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(),
        label = label
    )
    val flashActive = rememberDeltaFlash(deltaSequenceId, deltaText != null)
    val animatedValueColor by animateColorAsState(
        targetValue = when {
            flashActive && isDeltaPositive -> MoneyGreen
            flashActive && !isDeltaPositive -> DangerRed
            else -> accentColor
        },
        animationSpec = tween(durationMillis = 320),
        label = "${label}_color"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FloatingDeltaBadge(
                        visible = flashActive && deltaText != null,
                        text = deltaText.orEmpty(),
                        isPositive = isDeltaPositive
                    )
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = animatedValueColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50.dp))
            )
        }
    }
}

@Composable
private fun ActionDeckCard(
    action: CampusAction,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val badgeBg = if (action.isRisky) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val badgeFg = if (action.isRisky) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("action_${action.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(action.category),
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = action.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (action.badgeText.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = action.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CampusDiaryCard(
    latestSummary: String,
    entries: List<DiaryEntry>
) {
    var showAll by remember { mutableStateOf(false) }
    val visibleEntries = if (showAll) entries else entries.take(5)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("campus_event_log")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Event & Action Log", style = MaterialTheme.typography.titleMedium)
                    if (entries.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "${entries.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (entries.size > 5) {
                    TextButton(
                        onClick = { showAll = !showAll },
                        modifier = Modifier.testTag("toggle_event_log_button")
                    ) {
                        Text(if (showAll) "Recent 5" else "Show All")
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (showAll) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showAll) "Collapse event log" else "Expand event log",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                Text(
                    text = latestSummary.ifBlank { "Your campus journey begins today." },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                visibleEntries.forEachIndexed { idx, entry ->
                    if (idx > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = if (entry.isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = entry.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = entry.headline,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (entry.statusEffectNotice != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = entry.statusEffectNotice,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (entry.deltas.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    entry.deltas.forEach { chip ->
                                        val chipColor = if (chip.isPositive) MoneyGreen else DangerRed
                                        Surface(
                                            color = chipColor.copy(alpha = 0.13f),
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = chip.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chipColor,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (entry.deltaSummary.isNotBlank()) {
                                Text(
                                    entry.deltaSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalOutcomeCard(
    career: StudentCareer,
    onStartNewLife: () -> Unit
) {
    val positive = career.statusEnum == CareerStatus.GRADUATED || career.statusEnum == CareerStatus.RETIRED_PROF
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = career.statusEnum.displayName.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = career.lastLogSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onStartNewLife,
                modifier = Modifier.testTag("terminal_new_life_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start New Student Life")
            }
        }
    }
}

@Composable
private fun EmptyCampusState(
    onNewLifeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_campus_hero),
                    contentDescription = "Campus Gate",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Text(
                    text = "Welcome to Nigerian Student Life!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Create your student character and survive from JSS1 through University, Postgraduate, and Professor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onNewLifeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_start_life_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Your Character")
                }
            }
        }
    }
}

private fun categoryIcon(category: ActionCategory): ImageVector = when (category) {
    ActionCategory.ACADEMICS -> Icons.Default.School
    ActionCategory.HUSTLE -> Icons.Default.Work
    ActionCategory.FOOD_CARE -> Icons.Default.BatteryChargingFull
    ActionCategory.CAMPUS -> Icons.Default.Groups
}

@Composable
private fun QuickCoreActionsRow(
    career: StudentCareer,
    enabled: Boolean,
    onActionClick: (StudentCareer, CampusAction) -> Unit
) {
    val quickActions = remember(career.stage, career.strikeDaysLeft, career.archetype, career.itemsOwned, career.accommodation) {
        StudentLifeEngine.quickCoreActionsForCareer(career)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (career.day >= 30L) "Day 30: Exam Ready!" else "Quick Daily Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (career.day >= 30L) "Tap any action to sit Exam" else "Advances +1 Day & saves",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { (label, action) ->
                    val icon = when (label) {
                        "Study" -> Icons.Default.School
                        "Work" -> Icons.Default.Work
                        "Eat" -> Icons.Default.Restaurant
                        else -> Icons.Default.BatteryChargingFull
                    }
                    Button(
                        onClick = { onActionClick(career, action) },
                        enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("quick_action_${label.lowercase()}")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
