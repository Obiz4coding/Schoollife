package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Archetype
import com.example.data.model.Gender
import com.example.data.model.LifeStage
import com.example.game.StudentLifeEngine
import com.example.ui.theme.AcademicPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EnergyBlue
import com.example.ui.theme.HungerAmber
import com.example.ui.theme.MoneyGreen
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCharacterScreen(
    isCreating: Boolean,
    onCreateCharacter: (
        name: String,
        gender: Gender,
        schoolName: String,
        archetype: Archetype,
        startingStage: LifeStage
    ) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var gender by remember { mutableStateOf(Gender.MALE) }
    var name by remember { mutableStateOf(StudentLifeEngine.randomName(Gender.MALE)) }
    var selectedStage by remember { mutableStateOf(LifeStage.UNIVERSITY) }
    var selectedSchool by remember { mutableStateOf(StudentLifeEngine.schoolsForStage(LifeStage.UNIVERSITY).first()) }
    var selectedArchetype by remember { mutableStateOf(Archetype.STRUGGLE) }

    val availableSchools = remember(selectedStage) {
        StudentLifeEngine.schoolsForStage(selectedStage)
    }

    val previewCareer = remember(name, gender, selectedSchool, selectedArchetype, selectedStage) {
        StudentLifeEngine.createNewCareer(
            userId = "preview",
            characterName = name.ifBlank { "Student" },
            gender = gender,
            schoolName = selectedSchool,
            archetype = selectedArchetype,
            startingStage = selectedStage
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Your Student", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("create_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Character Name & Gender
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Name & Gender", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text("Character Name") },
                    placeholder = { Text("Enter student name") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = { name = StudentLifeEngine.randomName(gender) },
                            modifier = Modifier.testTag("randomize_name_button")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "Generate random Nigerian name")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Gender.entries.forEach { g ->
                        FilterChip(
                            selected = gender == g,
                            onClick = { gender = g },
                            label = { Text(g.displayName) },
                            leadingIcon = if (gender == g) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("gender_${g.name.lowercase()}")
                        )
                    }
                }
            }

            // 2. Starting Background (Archetype)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2. Starting Background", style = MaterialTheme.typography.titleMedium)
                Archetype.entries.forEach { arch ->
                    val isSelected = selectedArchetype == arch
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { selectedArchetype = arch }
                            .testTag("archetype_${arch.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(arch.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = StudentLifeEngine.formatNaira(
                                            when (selectedStage) {
                                                LifeStage.SECONDARY -> (arch.startingMoney * 0.4).toLong().coerceAtLeast(3_000L)
                                                LifeStage.UNIVERSITY -> arch.startingMoney
                                                LifeStage.POSTGRADUATE -> arch.startingMoney + 40_000L
                                                LifeStage.LECTURER -> arch.startingMoney + 150_000L
                                            }
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = arch.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Live Initial Stats Preview driven by Background + Stage
            InitialStatsPreviewCard(
                previewCareer = previewCareer,
                stage = selectedStage
            )

            // 3. Starting Stage
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Starting Life Mode", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LifeStage.entries.forEach { stage ->
                        FilterChip(
                            selected = selectedStage == stage,
                            onClick = {
                                selectedStage = stage
                                selectedSchool = StudentLifeEngine.schoolsForStage(stage).first()
                            },
                            label = { Text("${stage.displayName} (${stage.shortLabel})") },
                            modifier = Modifier.testTag("stage_${stage.name.lowercase()}")
                        )
                    }
                }
            }

            // 4. School Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4. Pick Your Campus / School", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSchools.forEach { school ->
                        FilterChip(
                            selected = selectedSchool == school,
                            onClick = { selectedSchool = school },
                            label = { Text(school) },
                            modifier = Modifier.testTag("school_chip")
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onCreateCharacter(name, gender, selectedSchool, selectedArchetype, selectedStage)
                },
                enabled = !isCreating && name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_character_submit")
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enrolling…")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Begin Student Life")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InitialStatsPreviewCard(
    previewCareer: com.example.data.model.StudentCareer,
    stage: LifeStage
) {
    val academicText = if (stage == LifeStage.UNIVERSITY) {
        String.format(Locale.US, "%.2f", previewCareer.academicStat)
    } else {
        "${previewCareer.academicStat.roundToInt()}%"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("initial_stats_preview")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Initial Stats (${previewCareer.archetypeEnum.title})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = previewCareer.accommodationEnum.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewStatPill(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Money",
                    value = StudentLifeEngine.formatNaira(previewCareer.money),
                    accent = MoneyGreen,
                    modifier = Modifier.weight(1f)
                )
                PreviewStatPill(
                    icon = Icons.Default.School,
                    label = stage.academicLabel,
                    value = academicText,
                    accent = AcademicPurple,
                    modifier = Modifier.weight(1f)
                )
                PreviewStatPill(
                    icon = Icons.Default.Bolt,
                    label = "Energy",
                    value = "${previewCareer.energy}%",
                    accent = EnergyBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewStatPill(
                    icon = Icons.Default.Restaurant,
                    label = "Fullness",
                    value = "${previewCareer.hunger}%",
                    accent = HungerAmber,
                    modifier = Modifier.weight(1f)
                )
                PreviewStatPill(
                    icon = Icons.Default.Groups,
                    label = stage.socialLabel,
                    value = "${previewCareer.socialStat}%",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                PreviewStatPill(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Heat",
                    value = "${previewCareer.riskHeat}%",
                    accent = if (previewCareer.riskHeat > 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreviewStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
        }
    }
}
