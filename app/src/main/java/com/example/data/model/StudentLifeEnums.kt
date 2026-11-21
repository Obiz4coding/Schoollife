package com.example.data.model

import androidx.compose.runtime.Immutable

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female");

    companion object {
        fun fromRaw(raw: String): Gender =
            entries.firstOrNull { it.name == raw } ?: MALE
    }
}

enum class Archetype(
    val title: String,
    val subtitle: String,
    val startingMoney: Long,
    val startingEnergy: Long,
    val startingHunger: Long,
    val startingSocial: Long,
    val secondaryStartingGrade: Double,
    val uniStartingCgpa: Double,
    val pgStartingResearch: Double,
    val lecturerStartingPts: Double,
    val academicBonus: Double,
    val startingHeat: Long
) {
    AJEBO(
        title = "Ajebo Student",
        subtitle = "Rich parents, plenty pocket money, lodge comfort — lower starting academic discipline.",
        startingMoney = 85_000L,
        startingEnergy = 92L,
        startingHunger = 90L,
        startingSocial = 72L,
        secondaryStartingGrade = 62.0,
        uniStartingCgpa = 3.70,
        pgStartingResearch = 12.0,
        lecturerStartingPts = 10.0,
        academicBonus = 0.0,
        startingHeat = 0L
    ),
    STRUGGLE(
        title = "Struggle Student",
        subtitle = "Tight budget (₦12k), sharp brain & pure grit. Starts with strong grades and +15% study boost.",
        startingMoney = 12_000L,
        startingEnergy = 78L,
        startingHunger = 65L,
        startingSocial = 45L,
        secondaryStartingGrade = 78.0,
        uniStartingCgpa = 4.55,
        pgStartingResearch = 22.0,
        lecturerStartingPts = 18.0,
        academicBonus = 0.25,
        startingHeat = 0L
    ),
    HUSTLER(
        title = "Risk-Taker / Hustler",
        subtitle = "₦35k capital & street smarts. High social cred & +35% hustle pay, but starts with 15% Heat.",
        startingMoney = 35_000L,
        startingEnergy = 85L,
        startingHunger = 75L,
        startingSocial = 68L,
        secondaryStartingGrade = 58.0,
        uniStartingCgpa = 3.40,
        pgStartingResearch = 10.0,
        lecturerStartingPts = 12.0,
        academicBonus = 0.05,
        startingHeat = 15L
    );

    fun startingAcademicFor(stage: LifeStage): Double = when (stage) {
        LifeStage.SECONDARY -> secondaryStartingGrade
        LifeStage.UNIVERSITY -> uniStartingCgpa
        LifeStage.POSTGRADUATE -> pgStartingResearch
        LifeStage.LECTURER -> lecturerStartingPts
    }

    companion object {
        fun fromRaw(raw: String): Archetype =
            entries.firstOrNull { it.name == raw } ?: STRUGGLE
    }
}

enum class LifeStage(
    val displayName: String,
    val shortLabel: String,
    val academicLabel: String,
    val socialLabel: String,
    val maxAcademic: Double,
    val passThreshold: Double
) {
    SECONDARY(
        displayName = "Secondary School",
        shortLabel = "JSS1 – SS3",
        academicLabel = "Class Grade",
        socialLabel = "Social Life",
        maxAcademic = 100.0,
        passThreshold = 45.0
    ),
    UNIVERSITY(
        displayName = "University",
        shortLabel = "100L – Final Yr",
        academicLabel = "CGPA",
        socialLabel = "Social Life",
        maxAcademic = 5.0,
        passThreshold = 1.5
    ),
    POSTGRADUATE(
        displayName = "Postgraduate",
        shortLabel = "M.Sc / PhD",
        academicLabel = "Thesis Progress",
        socialLabel = "Mental Health",
        maxAcademic = 100.0,
        passThreshold = 55.0
    ),
    LECTURER(
        displayName = "Lecturer Life",
        shortLabel = "Asst. Lect – Prof",
        academicLabel = "Promo Points",
        socialLabel = "Student Respect",
        maxAcademic = 100.0,
        passThreshold = 70.0
    );

    fun nextStage(): LifeStage? = when (this) {
        SECONDARY -> UNIVERSITY
        UNIVERSITY -> POSTGRADUATE
        POSTGRADUATE -> LECTURER
        LECTURER -> null
    }

    companion object {
        fun fromRaw(raw: String): LifeStage =
            entries.firstOrNull { it.name == raw } ?: UNIVERSITY
    }
}

enum class CareerStatus(val displayName: String) {
    ACTIVE("In Session"),
    GRADUATED("Graduated"),
    EXPELLED("Expelled"),
    DROPPED_OUT("Dropped Out"),
    RETIRED_PROF("Professor Emeritus");

    fun isTerminal(): Boolean = this != ACTIVE

    companion object {
        fun fromRaw(raw: String): CareerStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class Accommodation(val displayName: String, val upgradeCost: Long) {
    PARENTS_HOUSE("Parents' House", 0L),
    HOSTEL("School Hostel (NEPA Wahala)", 0L),
    LODGE("Self-Con Lodge (24/7 Solar)", 60_000L),
    STAFF_QUARTERS("Senior Staff Quarters", 0L);

    companion object {
        fun fromRaw(raw: String): Accommodation =
            entries.firstOrNull { it.name == raw } ?: HOSTEL
    }
}

enum class Affiliation(val displayName: String, val risky: Boolean) {
    NONE("Independent", false),
    JET_CLUB("JET Club", false),
    PRESS_CLUB("Press Club", false),
    FOOTBALL("School Football Team", false),
    FELLOWSHIP("Campus Fellowship / MSSN", false),
    SUG("Student Union Gov (SUG)", false),
    CULT("Secret Confraternity", true);

    companion object {
        fun fromRaw(raw: String): Affiliation =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class ActionCategory(val label: String) {
    ACADEMICS("Study & Duty"),
    HUSTLE("Hustle & Side Gig"),
    FOOD_CARE("Food & Rest"),
    CAMPUS("Campus Life")
}

@Immutable
data class CampusAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ActionCategory,
    val moneyDelta: Long = 0L,
    val energyDelta: Long = 0L,
    val hungerDelta: Long = 0L,
    val academicDelta: Double = 0.0,
    val socialDelta: Long = 0L,
    val heatDelta: Long = 0L,
    val advancesDay: Boolean = true,
    val badgeText: String = "",
    val isRisky: Boolean = false,
    val unlockAffiliation: Affiliation? = null
)

@Immutable
data class EventChoice(
    val label: String,
    val outcomeText: String,
    val moneyDelta: Long = 0L,
    val energyDelta: Long = 0L,
    val hungerDelta: Long = 0L,
    val academicDelta: Double = 0.0,
    val socialDelta: Long = 0L,
    val heatDelta: Long = 0L,
    val triggerStrikeDays: Long = 0L,
    val forceExpulsionChance: Float = 0f
)

@Immutable
data class CampusEvent(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val choices: List<EventChoice>
)

@Immutable
data class ShopItem(
    val id: String,
    val title: String,
    val description: String,
    val price: Long,
    val buffLabel: String
)

enum class StatusEffectType(
    val title: String,
    val badgeText: String,
    val description: String,
    val isPositive: Boolean
) {
    ACADEMIC_EXCELLENCE(
        title = "Academic Excellence",
        badgeText = "+30% Study",
        description = "3+ consecutive study sessions! Academic & CGPA gains boosted by +30%.",
        isPositive = true
    ),
    BURNOUT(
        title = "Burnout",
        badgeText = "-35% Output",
        description = "Overworked or exhausted! Study & Hustle gains -35%, extra -4 Energy drain. Sleep to recover.",
        isPositive = false
    ),
    HUSTLE_GRIND(
        title = "Hustle Streak",
        badgeText = "+25% Pay",
        description = "On a hustle roll! Hustle & side gig Naira earnings boosted by +25%.",
        isPositive = true
    ),
    SAPA_ALERT(
        title = "Sapa Alert",
        badgeText = "-3 Social/Turn",
        description = "Critically low cash & food! Extra -2 Energy & -3 Social drain until you eat or earn ₦1.5k+.",
        isPositive = false
    )
}

@Immutable
data class ActiveStatusEffect(
    val type: StatusEffectType,
    val turnsRemaining: Int
)

@Immutable
data class StatDeltaChip(
    val label: String,
    val isPositive: Boolean
)

@Immutable
data class StatDeltaSnapshot(
    val sequenceId: Long,
    val moneyDelta: Long = 0L,
    val energyDelta: Long = 0L,
    val hungerDelta: Long = 0L,
    val academicDelta: Double = 0.0,
    val socialDelta: Long = 0L,
    val heatDelta: Long = 0L,
    val stage: LifeStage = LifeStage.UNIVERSITY
)

@Immutable
data class DiaryEntry(
    val dayLabel: String,
    val headline: String,
    val deltaSummary: String,
    val isWarning: Boolean = false,
    val category: ActionCategory? = null,
    val deltas: List<StatDeltaChip> = emptyList(),
    val statusEffectNotice: String? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
