package com.example.game

import com.example.data.model.Accommodation
import com.example.data.model.ActionCategory
import com.example.data.model.ActiveStatusEffect
import com.example.data.model.Affiliation
import com.example.data.model.Archetype
import com.example.data.model.CampusAction
import com.example.data.model.CampusEvent
import com.example.data.model.CareerStatus
import com.example.data.model.DiaryEntry
import com.example.data.model.EventChoice
import com.example.data.model.Gender
import com.example.data.model.LifeStage
import com.example.data.model.ShopItem
import com.example.data.model.StatDeltaChip
import com.example.data.model.StatDeltaSnapshot
import com.example.data.model.StatusEffectType
import com.example.data.model.StudentCareer
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

data class CareerEffectTracker(
    val consecutiveStudyCount: Int = 0,
    val consecutiveWorkCount: Int = 0,
    val drainingActionsWithoutRest: Int = 0,
    val activeEffects: List<ActiveStatusEffect> = emptyList()
)

data class StatusEffectUpdate(
    val tracker: CareerEffectTracker,
    val newlyTriggered: List<StatusEffectType> = emptyList(),
    val cleared: List<StatusEffectType> = emptyList()
) {
    val summaryNotice: String?
        get() = buildList {
            newlyTriggered.forEach { add("${it.title} triggered (${it.badgeText})") }
            cleared.forEach { add("${it.title} cleared") }
        }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

data class ActionOutcome(
    val updatedCareer: StudentCareer,
    val diaryEntry: DiaryEntry,
    val appliedDeltas: StatDeltaSnapshot = StatDeltaSnapshot(0L),
    val triggeredEvent: CampusEvent? = null,
    val stageEndReady: Boolean = false
)

object StudentLifeEngine {

    val secondarySchools = listOf(
        "King's College Lagos",
        "Queen's College Yaba",
        "FGC Ijanikin",
        "Command Secondary School",
        "Loyola Jesuit Abuja",
        "Government College Ibadan"
    )

    val universities = listOf(
        "UNILAG (Akoka)",
        "UI (Ibadan)",
        "OAU (Great Ife)",
        "UNN (Nsukka)",
        "ABU (Zaria)",
        "LASU (Ojo)",
        "FUTA (Akure)",
        "Covenant University"
    )

    private val maleNames = listOf(
        "Chinedu Okafor", "Tunde Bakare", "Seyi Adeyemi", "Ibrahim Musa",
        "Emeka Nwosu", "Damilola Ogunleye", "Yusuf Abdullahi", "Kelechi Eze"
    )

    private val femaleNames = listOf(
        "Adaeze Nwosu", "Aisha Bello", "Zainab Musa", "Temitope Balogun",
        "Chidinma Okeke", "Simisola Adebayo", "Fatima Abubakar", "Ngozi Obi"
    )

    fun randomName(gender: Gender): String =
        if (gender == Gender.MALE) maleNames.random() else femaleNames.random()

    fun schoolsForStage(stage: LifeStage): List<String> =
        if (stage == LifeStage.SECONDARY) secondarySchools else universities

    val shopItems = listOf(
        ShopItem(
            id = "Powerbank",
            title = "20,000mAh Powerbank",
            description = "Never let NEPA ruin your TDB or phone battery again.",
            price = 8_500L,
            buffLabel = "+3 Energy saved on study/hostel days"
        ),
        ShopItem(
            id = "Smartphone",
            title = "Midrange Android Phone",
            description = "Essential for WhatsApp class updates & POS transfers.",
            price = 45_000L,
            buffLabel = "+25% Hustle earnings"
        ),
        ShopItem(
            id = "Laptop",
            title = "Clean Core i5 Laptop",
            description = "Speed up assignments, final year project, and thesis drafts.",
            price = 120_000L,
            buffLabel = "+30% Academic/Research gains"
        ),
        ShopItem(
            id = "iPhone",
            title = "iPhone 16 Pro (UK Used)",
            description = "Instant campus status upgrade. Everybody notices you.",
            price = 280_000L,
            buffLabel = "+8 Social / Respect bonus"
        ),
        ShopItem(
            id = "Benz",
            title = "Tokunbo Honda / Benz",
            description = "No more queuing for campus shuttle or danfo in the rain.",
            price = 1_500_000L,
            buffLabel = "+10 Energy & +10 Respect daily comfort"
        )
    )

    fun createNewCareer(
        userId: String,
        characterName: String,
        gender: Gender,
        schoolName: String,
        archetype: Archetype,
        startingStage: LifeStage
    ): StudentCareer {
        val startingAcademic = archetype.startingAcademicFor(startingStage)
        val startingMoney = when (startingStage) {
            LifeStage.SECONDARY -> (archetype.startingMoney * 0.4).toLong().coerceAtLeast(3_000L)
            LifeStage.UNIVERSITY -> archetype.startingMoney
            LifeStage.POSTGRADUATE -> archetype.startingMoney + 40_000L
            LifeStage.LECTURER -> archetype.startingMoney + 150_000L
        }
        val startingSocial = if (startingStage == LifeStage.POSTGRADUATE) {
            (archetype.startingSocial + 15L).coerceAtMost(100L)
        } else {
            archetype.startingSocial
        }
        val levelLabel = levelLabelFor(startingStage, 1L)
        val accommodation = when (startingStage) {
            LifeStage.SECONDARY -> if (archetype == Archetype.AJEBO) Accommodation.PARENTS_HOUSE else Accommodation.HOSTEL
            LifeStage.UNIVERSITY -> if (archetype == Archetype.AJEBO) Accommodation.LODGE else Accommodation.HOSTEL
            LifeStage.POSTGRADUATE -> Accommodation.LODGE
            LifeStage.LECTURER -> Accommodation.STAFF_QUARTERS
        }

        val intro = when (startingStage) {
            LifeStage.SECONDARY -> "Resumed at $schoolName ($levelLabel) as a ${archetype.title}."
            LifeStage.UNIVERSITY -> "Entered $schoolName as a 100L Fresher (${archetype.title})."
            LifeStage.POSTGRADUATE -> "Registered for M.Sc/PhD at $schoolName (${archetype.title})."
            LifeStage.LECTURER -> "Appointed Assistant Lecturer at $schoolName (${archetype.title})."
        }

        return StudentCareer(
            userId = userId,
            characterName = characterName.trim().take(40),
            gender = gender.name,
            schoolName = schoolName.trim().take(60),
            archetype = archetype.name,
            stage = startingStage.name,
            levelLabel = levelLabel,
            day = 1L,
            totalDays = 1L,
            money = startingMoney,
            energy = archetype.startingEnergy,
            hunger = archetype.startingHunger,
            academicStat = startingAcademic,
            socialStat = startingSocial,
            riskHeat = archetype.startingHeat,
            careerStatus = CareerStatus.ACTIVE.name,
            carryoverCount = 0L,
            strikeDaysLeft = 0L,
            accommodation = accommodation.name,
            affiliation = Affiliation.NONE.name,
            itemsOwned = emptyList(),
            completedStages = emptyList(),
            lastLogSummary = intro
        )
    }

    fun actionsForCareer(career: StudentCareer): List<CampusAction> {
        val stage = career.stageEnum
        val onStrike = career.strikeDaysLeft > 0L
        val hustlerBonus = if (career.archetypeEnum == Archetype.HUSTLER) 1.35 else 1.0
        val phoneBonus = if (career.itemsOwned.contains("Smartphone")) 1.25 else 1.0
        val hustleMult = hustlerBonus * phoneBonus

        return when (stage) {
            LifeStage.SECONDARY -> listOf(
                CampusAction(
                    id = "sec_class",
                    title = if (onStrike) "Self-Study Past Questions" else "Attend Morning Assembly & Class",
                    subtitle = "Listen to Mr. Balogun explain quadratic equations.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -16,
                    hungerDelta = -12,
                    academicDelta = 4.5,
                    socialDelta = 2,
                    badgeText = "+4.5% Grade"
                ),
                CampusAction(
                    id = "sec_prep",
                    title = "Evening Prep & Homework",
                    subtitle = "Finish assignments before senior prefect lights out.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -18,
                    hungerDelta = -10,
                    academicDelta = 6.0,
                    badgeText = "+6% Grade"
                ),
                CampusAction(
                    id = "sec_dodge",
                    title = "Dodge Class to Tuck Shop",
                    subtitle = "Skip Further Maths with your guys. Risk principal wahala.",
                    category = ActionCategory.ACADEMICS,
                    moneyDelta = -800,
                    energyDelta = 6,
                    hungerDelta = 15,
                    academicDelta = -3.5,
                    socialDelta = 8,
                    heatDelta = 10,
                    badgeText = "+8 Social · Risky",
                    isRisky = true
                ),
                CampusAction(
                    id = "sec_errand",
                    title = "Sell Sweets / Copy Notes for Cash",
                    subtitle = "Small class side hustle selling chin-chin at break time.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = (2_200 * hustleMult).roundToInt().toLong(),
                    energyDelta = -15,
                    hungerDelta = -10,
                    heatDelta = 3,
                    badgeText = "+₦${formatShort((2_200 * hustleMult).toLong())}"
                ),
                CampusAction(
                    id = "sec_pocket",
                    title = "Call Parents for Pocket Money",
                    subtitle = "Beg Mummy for provision refill.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = if (career.archetypeEnum == Archetype.AJEBO) 15_000L else 4_500L,
                    energyDelta = -5,
                    hungerDelta = -5,
                    socialDelta = -2,
                    badgeText = if (career.archetypeEnum == Archetype.AJEBO) "+₦15k" else "+₦4.5k"
                ),
                CampusAction(
                    id = "sec_puff",
                    title = "Gala & Puff-Puff at Tuck Shop",
                    subtitle = "Quick break-time survival combo.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -600,
                    energyDelta = 8,
                    hungerDelta = 24,
                    advancesDay = true,
                    badgeText = "-₦600 · +24 Food"
                ),
                CampusAction(
                    id = "sec_mamaput",
                    title = "Mama Put Jollof & Beef",
                    subtitle = "Heavy lunch after closing bell.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -1_800,
                    energyDelta = 14,
                    hungerDelta = 42,
                    advancesDay = true,
                    badgeText = "-₦1.8k · +42 Food"
                ),
                CampusAction(
                    id = "sec_sleep",
                    title = "Deep Hostel Bunk Sleep",
                    subtitle = "Recover full energy for tomorrow's assembly.",
                    category = ActionCategory.FOOD_CARE,
                    energyDelta = 40,
                    hungerDelta = -12,
                    badgeText = "+40 Energy"
                ),
                CampusAction(
                    id = "sec_jet",
                    title = "JET Club / Debate Practice",
                    subtitle = "Build sharp brain and represent school.",
                    category = ActionCategory.CAMPUS,
                    energyDelta = -10,
                    hungerDelta = -8,
                    academicDelta = 3.0,
                    socialDelta = 7,
                    unlockAffiliation = Affiliation.JET_CLUB,
                    badgeText = "+3% Grade · +7 Social"
                ),
                CampusAction(
                    id = "sec_football",
                    title = "Inter-House Football Training",
                    subtitle = "Score goals on the dusty field.",
                    category = ActionCategory.CAMPUS,
                    energyDelta = -18,
                    hungerDelta = -15,
                    socialDelta = 12,
                    unlockAffiliation = Affiliation.FOOTBALL,
                    badgeText = "+12 Social"
                ),
                CampusAction(
                    id = "sec_cult",
                    title = "Join Bad Boys Clique",
                    subtitle = "Instant street fear, but Principal will expel you if caught.",
                    category = ActionCategory.CAMPUS,
                    moneyDelta = 5_000,
                    energyDelta = -10,
                    socialDelta = 10,
                    heatDelta = 28,
                    isRisky = true,
                    unlockAffiliation = Affiliation.CULT,
                    badgeText = "High Risk!"
                )
            )

            LifeStage.UNIVERSITY -> listOf(
                CampusAction(
                    id = "uni_lecture",
                    title = if (onStrike) "ASUU Strike: Self-Study" else "Attend 8am Lecture",
                    subtitle = if (onStrike) "Lecturers are on strike, revise handouts solo." else "Rush for front seat before LT gets packed.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -16,
                    hungerDelta = -12,
                    academicDelta = if (onStrike) 0.08 else 0.16,
                    socialDelta = 2,
                    badgeText = if (onStrike) "+0.08 CGPA" else "+0.16 CGPA"
                ),
                CampusAction(
                    id = "uni_tdb",
                    title = "Library TDB (Till Day Break)",
                    subtitle = "Armed with mosquito coil & strong coffee.",
                    category = ActionCategory.ACADEMICS,
                    moneyDelta = -500,
                    energyDelta = -26,
                    hungerDelta = -15,
                    academicDelta = 0.25,
                    badgeText = "+0.25 CGPA"
                ),
                CampusAction(
                    id = "uni_handout",
                    title = "Buy Lecturer's Mandatory Handout",
                    subtitle = "Pay ₦3,500 so your name enters the attendance sheet.",
                    category = ActionCategory.ACADEMICS,
                    moneyDelta = -3_500,
                    energyDelta = -8,
                    hungerDelta = -6,
                    academicDelta = 0.22,
                    badgeText = "-₦3.5k · +0.22 CGPA"
                ),
                CampusAction(
                    id = "uni_pos",
                    title = "Campus POS Agent Shift",
                    subtitle = "Charge ₦100 per ₦5k withdrawal at hostel gate.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = (6_500 * hustleMult).roundToInt().toLong(),
                    energyDelta = -18,
                    hungerDelta = -12,
                    socialDelta = 3,
                    badgeText = "+₦${formatShort((6_500 * hustleMult).toLong())}"
                ),
                CampusAction(
                    id = "uni_tutor",
                    title = "100L / JAMB Tutorial Class",
                    subtitle = "Teach freshers CHM 101 and collect tutorial dues.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = (8_500 * hustleMult).roundToInt().toLong(),
                    energyDelta = -20,
                    hungerDelta = -12,
                    academicDelta = 0.06,
                    badgeText = "+₦${formatShort((8_500 * hustleMult).toLong())}"
                ),
                CampusAction(
                    id = "uni_skit",
                    title = "Film Campus Skit / Thrift Sales",
                    subtitle = "Post funny hostel reels or sell Okrika wears.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = (11_000 * hustleMult).roundToInt().toLong(),
                    energyDelta = -20,
                    hungerDelta = -12,
                    socialDelta = 8,
                    badgeText = "+₦${formatShort((11_000 * hustleMult).toLong())}"
                ),
                CampusAction(
                    id = "uni_risk",
                    title = "Shady Fast-Money Wire",
                    subtitle = "Massive payout, but EFCC raid risk spikes fast!",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = 55_000,
                    energyDelta = -16,
                    hungerDelta = -10,
                    heatDelta = 32,
                    isRisky = true,
                    badgeText = "+₦55k · EFCC Risk!"
                ),
                CampusAction(
                    id = "uni_garri",
                    title = "Soak Garri & Groundnut",
                    subtitle = "Student savior when sapa hits hard.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -300,
                    energyDelta = 6,
                    hungerDelta = 22,
                    advancesDay = true,
                    badgeText = "-₦300 · +22 Food"
                ),
                CampusAction(
                    id = "uni_indomie",
                    title = "Aboki Indomie & Double Egg",
                    subtitle = "Hot late-night campus special.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -1_400,
                    energyDelta = 12,
                    hungerDelta = 36,
                    advancesDay = true,
                    badgeText = "-₦1.4k · +36 Food"
                ),
                CampusAction(
                    id = "uni_shawarma",
                    title = "Chicken Shawarma & Malt",
                    subtitle = "Treat yourself like a baller.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -4_500,
                    energyDelta = 20,
                    hungerDelta = 52,
                    socialDelta = 4,
                    advancesDay = true,
                    badgeText = "-₦4.5k · +52 Food"
                ),
                CampusAction(
                    id = "uni_sleep",
                    title = if (career.accommodationEnum == Accommodation.LODGE) "Sleep in Solar Lodge" else "Hostel Sleep (NEPA Heat)",
                    subtitle = if (career.accommodationEnum == Accommodation.LODGE) "Cool fan & full recharge." else "Mosquitoes & heat, but rest is free.",
                    category = ActionCategory.FOOD_CARE,
                    energyDelta = if (career.accommodationEnum == Accommodation.LODGE) 44 else 32,
                    hungerDelta = -10,
                    badgeText = if (career.accommodationEnum == Accommodation.LODGE) "+44 Energy" else "+32 Energy"
                ),
                CampusAction(
                    id = "uni_fellowship",
                    title = "Campus Fellowship / MSSN",
                    subtitle = "Peace of mind, free snacks, and solid network.",
                    category = ActionCategory.CAMPUS,
                    energyDelta = -8,
                    hungerDelta = 6,
                    socialDelta = 10,
                    heatDelta = -10,
                    unlockAffiliation = Affiliation.FELLOWSHIP,
                    badgeText = "+10 Social · -10 Heat"
                ),
                CampusAction(
                    id = "uni_sug",
                    title = "SUG Politics & Rally",
                    subtitle = "Campaign for Welfare Director with Aluta chants.",
                    category = ActionCategory.CAMPUS,
                    moneyDelta = -2_000,
                    energyDelta = -15,
                    hungerDelta = -10,
                    socialDelta = 15,
                    unlockAffiliation = Affiliation.SUG,
                    badgeText = "+15 Social"
                ),
                CampusAction(
                    id = "uni_party",
                    title = "Off-Campus Friday Party",
                    subtitle = "Turn up till dawn — grades might suffer!",
                    category = ActionCategory.CAMPUS,
                    moneyDelta = -6_000,
                    energyDelta = -22,
                    hungerDelta = -10,
                    academicDelta = -0.10,
                    socialDelta = 18,
                    badgeText = "+18 Social"
                )
            )

            LifeStage.POSTGRADUATE -> listOf(
                CampusAction(
                    id = "pg_thesis",
                    title = "Write Thesis Chapter",
                    subtitle = "Grind through Literature Review & SPSS analysis.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -20,
                    hungerDelta = -12,
                    academicDelta = 7.0,
                    socialDelta = -6,
                    badgeText = "+7% Research"
                ),
                CampusAction(
                    id = "pg_supervisor",
                    title = "Meet Thesis Supervisor",
                    subtitle = "Pray Prof doesn't say 'Go and redo Chapter 3'.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -15,
                    hungerDelta = -10,
                    academicDelta = 8.5,
                    socialDelta = -8,
                    badgeText = "+8.5% Research"
                ),
                CampusAction(
                    id = "pg_publish",
                    title = "Submit Journal Paper",
                    subtitle = "Peer-review grind for international publication.",
                    category = ActionCategory.ACADEMICS,
                    moneyDelta = -12_000,
                    energyDelta = -22,
                    hungerDelta = -10,
                    academicDelta = 11.0,
                    badgeText = "-₦12k · +11% Research"
                ),
                CampusAction(
                    id = "pg_teach",
                    title = "Teach Undergrad Lab / Tutorial",
                    subtitle = "Mark 100L attendance and earn stipend.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = (18_000 * hustleMult).roundToInt().toLong(),
                    energyDelta = -18,
                    hungerDelta = -12,
                    academicDelta = 2.0,
                    badgeText = "+₦${formatShort((18_000 * hustleMult).toLong())}"
                ),
                CampusAction(
                    id = "pg_remote",
                    title = "Remote Data / Tech Freelance",
                    subtitle = "Earn foreign-client pay between thesis chapters.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = (32_000 * hustleMult).roundToInt().toLong(),
                    energyDelta = -22,
                    hungerDelta = -14,
                    socialDelta = -4,
                    badgeText = "+₦${formatShort((32_000 * hustleMult).toLong())}"
                ),
                CampusAction(
                    id = "pg_coffee",
                    title = "Coffee, Bread & Akara",
                    subtitle = "Keep midnight research momentum alive.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -1_800,
                    energyDelta = 12,
                    hungerDelta = 30,
                    advancesDay = true,
                    badgeText = "-₦1.8k · +30 Food"
                ),
                CampusAction(
                    id = "pg_mamaput",
                    title = "Proper Amala & Ewedu",
                    subtitle = "Refuel body and soul after supervisor meeting.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -3_500,
                    energyDelta = 16,
                    hungerDelta = 48,
                    socialDelta = 4,
                    advancesDay = true,
                    badgeText = "-₦3.5k · +48 Food"
                ),
                CampusAction(
                    id = "pg_therapy",
                    title = "Mental Health Walk & Hangout",
                    subtitle = "Step away from thesis drafts and breathe.",
                    category = ActionCategory.CAMPUS,
                    moneyDelta = -4_000,
                    energyDelta = 14,
                    hungerDelta = -6,
                    socialDelta = 22,
                    badgeText = "+22 Mental Health"
                ),
                CampusAction(
                    id = "pg_sleep",
                    title = "Full Night Rest (No Laptop)",
                    subtitle = "Protect your sanity and recharge deeply.",
                    category = ActionCategory.FOOD_CARE,
                    energyDelta = 42,
                    hungerDelta = -10,
                    socialDelta = 10,
                    badgeText = "+42 Energy · +10 Mental"
                )
            )

            LifeStage.LECTURER -> listOf(
                CampusAction(
                    id = "lec_teach",
                    title = if (onStrike) "ASUU Strike Solidarity" else "Deliver 8am Lecture",
                    subtitle = "Explain complex theory so even back-benchers smile.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -16,
                    hungerDelta = -10,
                    academicDelta = 4.5,
                    socialDelta = 8,
                    badgeText = "+4.5 Promo · +8 Respect"
                ),
                CampusAction(
                    id = "lec_cancel",
                    title = "Cancel Class ('I Am in Abuja')",
                    subtitle = "Send message through Class Rep at 7:55am.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = 15,
                    hungerDelta = -6,
                    academicDelta = -1.5,
                    socialDelta = -10,
                    badgeText = "+15 Energy · -10 Respect"
                ),
                CampusAction(
                    id = "lec_research",
                    title = "Publish Academic Paper",
                    subtitle = "Publish or perish! Moves you closer to Professor.",
                    category = ActionCategory.ACADEMICS,
                    moneyDelta = -15_000,
                    energyDelta = -22,
                    hungerDelta = -12,
                    academicDelta = 10.5,
                    socialDelta = 4,
                    badgeText = "+10.5 Promo Pts"
                ),
                CampusAction(
                    id = "lec_mark",
                    title = "Mark Scripts & Release Results",
                    subtitle = "Grade 400 booklets fast without missing any score.",
                    category = ActionCategory.ACADEMICS,
                    energyDelta = -20,
                    hungerDelta = -12,
                    academicDelta = 5.0,
                    socialDelta = 12,
                    badgeText = "+5 Promo · +12 Respect"
                ),
                CampusAction(
                    id = "lec_handout",
                    title = "Sell Mandatory Course Compendium",
                    subtitle = "Big ₦₦₦ from 300 students, but students murmur!",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = 75_000,
                    energyDelta = -10,
                    hungerDelta = -8,
                    socialDelta = -14,
                    heatDelta = 12,
                    isRisky = true,
                    badgeText = "+₦75k · -14 Respect"
                ),
                CampusAction(
                    id = "lec_salary",
                    title = "Monthly Salary & Consultancy",
                    subtitle = "Collect faculty pay plus external board review.",
                    category = ActionCategory.HUSTLE,
                    moneyDelta = 110_000,
                    energyDelta = -18,
                    hungerDelta = -12,
                    academicDelta = 3.0,
                    badgeText = "+₦110k"
                ),
                CampusAction(
                    id = "lec_club",
                    title = "Senior Staff Club Pepper Soup",
                    subtitle = "Network with Deans and eat fresh catfish.",
                    category = ActionCategory.FOOD_CARE,
                    moneyDelta = -6_000,
                    energyDelta = 16,
                    hungerDelta = 48,
                    socialDelta = 6,
                    advancesDay = true,
                    badgeText = "-₦6k · +48 Food"
                ),
                CampusAction(
                    id = "lec_sleep",
                    title = "Rest at Staff Quarters",
                    subtitle = "Unwind with evening breeze and cold water.",
                    category = ActionCategory.FOOD_CARE,
                    energyDelta = 42,
                    hungerDelta = -10,
                    badgeText = "+42 Energy"
                ),
                CampusAction(
                    id = "lec_supervise",
                    title = "Supervise Final Year Projects",
                    subtitle = "Mentor undergrads to defend their projects proudly.",
                    category = ActionCategory.CAMPUS,
                    energyDelta = -14,
                    hungerDelta = -10,
                    academicDelta = 5.5,
                    socialDelta = 10,
                    badgeText = "+5.5 Promo · +10 Respect"
                )
            )
        }
    }

    fun performAction(
        career: StudentCareer,
        action: CampusAction,
        activeEffects: List<ActiveStatusEffect> = emptyList()
    ): ActionOutcome {
        val stage = career.stageEnum
        val hasPowerbank = career.itemsOwned.contains("Powerbank")
        val hasLaptop = career.itemsOwned.contains("Laptop")
        val hasIphone = career.itemsOwned.contains("iPhone")
        val hasBenz = career.itemsOwned.contains("Benz")

        val activeTypes = activeEffects.map { it.type }.toSet()
        val hasAcademicExcellence = StatusEffectType.ACADEMIC_EXCELLENCE in activeTypes
        val hasBurnout = StatusEffectType.BURNOUT in activeTypes
        val hasHustleGrind = StatusEffectType.HUSTLE_GRIND in activeTypes
        val hasSapaAlert = StatusEffectType.SAPA_ALERT in activeTypes

        // Apply item & status effect buffs/debuffs
        var moneyDelta = action.moneyDelta
        if (action.category == ActionCategory.HUSTLE && moneyDelta > 0) {
            if (hasHustleGrind) moneyDelta = (moneyDelta * 1.25).roundToInt().toLong()
            if (hasBurnout) moneyDelta = (moneyDelta * 0.65).roundToInt().toLong()
        }

        var energyDelta = action.energyDelta
        if (energyDelta < 0 && hasPowerbank) energyDelta += 3
        if (hasBenz && action.advancesDay) energyDelta += 6
        if (hasBurnout && action.category != ActionCategory.FOOD_CARE) energyDelta -= 4
        if (hasSapaAlert && action.category != ActionCategory.FOOD_CARE) energyDelta -= 2

        var academicDelta = action.academicDelta
        if (academicDelta > 0) {
            if (hasLaptop) academicDelta *= 1.30
            if (career.archetypeEnum == Archetype.STRUGGLE) {
                academicDelta *= 1.15
            }
            if (hasAcademicExcellence) academicDelta *= 1.30
            if (hasBurnout) academicDelta *= 0.65
        }

        var socialDelta = action.socialDelta
        if (hasIphone && socialDelta > 0) socialDelta += 2
        if (hasBenz && action.advancesDay && socialDelta >= 0) socialDelta += 2
        if (hasSapaAlert && action.category != ActionCategory.FOOD_CARE) socialDelta -= 3

        val newMoney = (career.money + moneyDelta).coerceIn(-1_000_000L, 1_000_000_000L)
        val newEnergy = (career.energy + energyDelta).coerceIn(0L, 100L)
        val newHunger = (career.hunger + action.hungerDelta).coerceIn(0L, 100L)
        val newAcademic = (career.academicStat + academicDelta).coerceIn(0.0, stage.maxAcademic)
        val newSocial = (career.socialStat + socialDelta).coerceIn(0L, 100L)
        val newHeat = (career.riskHeat + action.heatDelta).coerceIn(0L, 100L)

        val actualMoneyDelta = newMoney - career.money
        val actualEnergyDelta = newEnergy - career.energy
        val actualHungerDelta = newHunger - career.hunger
        val actualAcademicDelta = newAcademic - career.academicStat
        val actualSocialDelta = newSocial - career.socialStat
        val actualHeatDelta = newHeat - career.riskHeat

        var nextDay = career.day
        var nextTotalDays = career.totalDays
        var nextStrike = career.strikeDaysLeft
        if (action.advancesDay) {
            nextDay = (career.day + 1L).coerceAtMost(31L)
            nextTotalDays = (career.totalDays + 1L).coerceAtMost(500L)
            if (nextStrike > 0L) nextStrike -= 1L
        }

        // Check collapse conditions (Hunger 0, Energy 0, or Heat >= 95)
        var status = CareerStatus.ACTIVE
        var extraWarning = ""
        if (newHunger <= 0L) {
            status = CareerStatus.DROPPED_OUT
            extraWarning = "Severe sapa and hunger forced you to withdraw from school!"
        } else if (newEnergy <= 0L) {
            status = CareerStatus.DROPPED_OUT
            extraWarning = "Total exhaustion! Energy hit 0 and you collapsed out of the term."
        } else if (newHeat >= 95L) {
            status = CareerStatus.EXPELLED
            extraWarning = when (stage) {
                LifeStage.SECONDARY -> "The Principal caught you! Expelled on assembly ground!"
                LifeStage.UNIVERSITY -> "EFCC / Senate Disciplinary Panel caught up with you. Expelled!"
                LifeStage.POSTGRADUATE -> "Academic misconduct panel terminated your programme!"
                LifeStage.LECTURER -> "VC & Senate dismissed you over bribery/handout scandal!"
            }
        }

        val newAffiliation = action.unlockAffiliation?.name ?: career.affiliation
        val levelLabel = levelLabelFor(stage, nextDay)

        val deltaChips = buildDeltaChips(
            stage = stage,
            moneyDelta = actualMoneyDelta,
            energyDelta = actualEnergyDelta,
            hungerDelta = actualHungerDelta,
            academicDelta = actualAcademicDelta,
            socialDelta = actualSocialDelta,
            heatDelta = actualHeatDelta
        )

        val headline = if (extraWarning.isNotBlank()) extraWarning else action.title
        val summaryText = deltaChips.joinToString(", ") { it.label }
        val summary = "$headline ($summaryText)".take(300)

        val updated = career.copy(
            levelLabel = levelLabel,
            day = nextDay,
            totalDays = nextTotalDays,
            money = newMoney,
            energy = newEnergy,
            hunger = newHunger,
            academicStat = newAcademic,
            socialStat = newSocial,
            riskHeat = newHeat,
            careerStatus = status.name,
            strikeDaysLeft = nextStrike,
            affiliation = newAffiliation,
            lastLogSummary = summary
        )

        val isExamDay = status == CareerStatus.ACTIVE && nextDay >= 30L
        val triggeredEvent = when {
            isExamDay -> buildDay30ExamEvent(updated)
            status == CareerStatus.ACTIVE && action.advancesDay && Random.nextFloat() < 0.34f -> pickRandomEvent(updated)
            else -> null
        }

        val appliedSnapshot = StatDeltaSnapshot(
            sequenceId = 0L,
            moneyDelta = actualMoneyDelta,
            energyDelta = actualEnergyDelta,
            hungerDelta = actualHungerDelta,
            academicDelta = actualAcademicDelta,
            socialDelta = actualSocialDelta,
            heatDelta = actualHeatDelta,
            stage = stage
        )

        return ActionOutcome(
            updatedCareer = updated,
            diaryEntry = DiaryEntry(
                dayLabel = "Day $nextDay",
                headline = headline,
                deltaSummary = deltaChips.joinToString(" · ") { it.label },
                isWarning = status != CareerStatus.ACTIVE || action.isRisky,
                category = action.category,
                deltas = deltaChips
            ),
            appliedDeltas = appliedSnapshot,
            triggeredEvent = triggeredEvent,
            stageEndReady = false
        )
    }

    fun evaluateStatusEffectsAfterAction(
        previousTracker: CareerEffectTracker,
        action: CampusAction,
        updatedCareer: StudentCareer
    ): StatusEffectUpdate {
        if (updatedCareer.statusEnum.isTerminal()) {
            return StatusEffectUpdate(tracker = CareerEffectTracker())
        }

        val isStudy = action.category == ActionCategory.ACADEMICS && action.academicDelta > 0.0
        val isWork = action.category == ActionCategory.HUSTLE && action.moneyDelta > 0L
        val isRest = action.id.contains("sleep") ||
            action.id.contains("therapy") ||
            (action.category == ActionCategory.FOOD_CARE && action.energyDelta >= 25L)
        val isDraining = action.energyDelta < 0L && !isRest

        val nextStudyCount = when {
            isStudy -> previousTracker.consecutiveStudyCount + 1
            action.category == ActionCategory.FOOD_CARE -> previousTracker.consecutiveStudyCount
            else -> 0
        }
        val nextWorkCount = when {
            isWork -> previousTracker.consecutiveWorkCount + 1
            action.category == ActionCategory.FOOD_CARE -> previousTracker.consecutiveWorkCount
            else -> 0
        }
        val nextDrainCount = if (isRest) 0 else if (isDraining) previousTracker.drainingActionsWithoutRest + 1 else previousTracker.drainingActionsWithoutRest

        val prevTypes = previousTracker.activeEffects.map { it.type }.toSet()
        val tickedMap = previousTracker.activeEffects
            .mapNotNull { effect ->
                val rem = effect.turnsRemaining - 1
                if (rem > 0) effect.type to rem else null
            }
            .toMap()
            .toMutableMap()

        val newlyTriggered = mutableListOf<StatusEffectType>()
        val cleared = mutableListOf<StatusEffectType>()

        // 1. Academic Excellence: 3+ consistent study sessions
        if (isStudy && nextStudyCount >= 3) {
            if (StatusEffectType.ACADEMIC_EXCELLENCE !in prevTypes) {
                newlyTriggered += StatusEffectType.ACADEMIC_EXCELLENCE
            }
            tickedMap[StatusEffectType.ACADEMIC_EXCELLENCE] = 3
        }

        // 2. Hustle Streak: 3+ hustle sessions
        if (isWork && nextWorkCount >= 3) {
            if (StatusEffectType.HUSTLE_GRIND !in prevTypes) {
                newlyTriggered += StatusEffectType.HUSTLE_GRIND
            }
            tickedMap[StatusEffectType.HUSTLE_GRIND] = 3
        }

        // 3. Burnout: low energy (<25%) or 4+ draining actions without sleep; Sleep cures it!
        if (isRest) {
            if (StatusEffectType.BURNOUT in prevTypes || StatusEffectType.BURNOUT in tickedMap) {
                tickedMap.remove(StatusEffectType.BURNOUT)
                cleared += StatusEffectType.BURNOUT
            }
        } else if (updatedCareer.energy < 25L || nextDrainCount >= 4) {
            if (StatusEffectType.BURNOUT !in prevTypes) {
                newlyTriggered += StatusEffectType.BURNOUT
            }
            tickedMap[StatusEffectType.BURNOUT] = 3
        }

        // 4. Sapa Alert: critically low cash (<1500) & low hunger (<35)
        if (updatedCareer.money < 1_500L && updatedCareer.hunger < 35L) {
            if (StatusEffectType.SAPA_ALERT !in prevTypes) {
                newlyTriggered += StatusEffectType.SAPA_ALERT
            }
            tickedMap[StatusEffectType.SAPA_ALERT] = 2
        } else if (StatusEffectType.SAPA_ALERT in prevTypes && (updatedCareer.money >= 1_500L || updatedCareer.hunger >= 50L)) {
            tickedMap.remove(StatusEffectType.SAPA_ALERT)
            cleared += StatusEffectType.SAPA_ALERT
        }

        val nextEffects = tickedMap.entries.map { (type, turns) ->
            ActiveStatusEffect(type = type, turnsRemaining = turns)
        }

        return StatusEffectUpdate(
            tracker = CareerEffectTracker(
                consecutiveStudyCount = nextStudyCount,
                consecutiveWorkCount = nextWorkCount,
                drainingActionsWithoutRest = nextDrainCount,
                activeEffects = nextEffects
            ),
            newlyTriggered = newlyTriggered,
            cleared = cleared
        )
    }

    private fun buildDeltaChips(
        stage: LifeStage,
        moneyDelta: Long,
        energyDelta: Long,
        hungerDelta: Long,
        academicDelta: Double,
        socialDelta: Long,
        heatDelta: Long
    ): List<StatDeltaChip> = buildList {
        if (moneyDelta != 0L) {
            val sign = if (moneyDelta > 0) "+" else "-"
            add(StatDeltaChip("${sign}₦${formatShort(abs(moneyDelta))}", isPositive = moneyDelta > 0))
        }
        if (energyDelta != 0L) {
            add(StatDeltaChip("${if (energyDelta > 0) "+" else ""}$energyDelta Energy", isPositive = energyDelta > 0))
        }
        if (hungerDelta != 0L) {
            add(StatDeltaChip("${if (hungerDelta > 0) "+" else ""}$hungerDelta Food", isPositive = hungerDelta > 0))
        }
        if (abs(academicDelta) >= 0.005) {
            val formatted = if (stage == LifeStage.UNIVERSITY) {
                String.format(Locale.US, "%+.2f CGPA", academicDelta)
            } else {
                String.format(Locale.US, "%+.1f %s", academicDelta, stage.academicLabel)
            }
            add(StatDeltaChip(formatted, isPositive = academicDelta > 0))
        }
        if (socialDelta != 0L) {
            add(StatDeltaChip("${if (socialDelta > 0) "+" else ""}$socialDelta ${stage.socialLabel}", isPositive = socialDelta > 0))
        }
        if (heatDelta != 0L) {
            add(StatDeltaChip("${if (heatDelta > 0) "+" else ""}$heatDelta Heat", isPositive = heatDelta < 0))
        }
    }

    fun quickCoreActionsForCareer(career: StudentCareer): List<Pair<String, CampusAction>> {
        val all = actionsForCareer(career)
        val study = all.firstOrNull { it.category == ActionCategory.ACADEMICS } ?: all.first()
        val work = all.firstOrNull { it.category == ActionCategory.HUSTLE && !it.isRisky }
            ?: all.first { it.category == ActionCategory.HUSTLE }
        val eat = all.firstOrNull { it.category == ActionCategory.FOOD_CARE && it.hungerDelta > 0 }
            ?: all.first { it.category == ActionCategory.FOOD_CARE }
        val sleep = all.firstOrNull { it.category == ActionCategory.FOOD_CARE && it.energyDelta >= 30 }
            ?: all.last { it.category == ActionCategory.FOOD_CARE }
        return listOf(
            "Study" to study,
            "Work" to work,
            "Eat" to eat,
            "Sleep" to sleep
        )
    }

    fun buildDay30ExamEvent(career: StudentCareer): CampusEvent = when (career.stageEnum) {
        LifeStage.SECONDARY -> CampusEvent(
            id = "day30_exam",
            title = "Day 30: WAEC & JAMB Final Exam!",
            description = "The hall is dead silent and the invigilator just shared the question papers. How do you handle the final exam?",
            tag = "Day 30 Exam",
            choices = listOf(
                EventChoice("Write Exam Honestly", "You wrote everything you studied cleanly.", energyDelta = -12, academicDelta = 4.0),
                EventChoice("Last-Minute Midnight Cram", "Pushed your brain to the limit for extra marks!", energyDelta = -24, hungerDelta = -10, academicDelta = 8.0),
                EventChoice("Risk 'Expo' Paper in Hall", "High risk! Could boost your grade or get you expelled.", academicDelta = 14.0, heatDelta = 35, forceExpulsionChance = 0.25f)
            )
        )
        LifeStage.UNIVERSITY -> CampusEvent(
            id = "day30_exam",
            title = "Day 30: Final Semester Exam!",
            description = "It's exam day at the main Lecture Theatre! Your CGPA (currently ${String.format(Locale.US, "%.2f", career.academicStat)}) depends on this paper.",
            tag = "Day 30 Exam",
            choices = listOf(
                EventChoice("Sit Exam With Honest Prep", "Answered questions steadily using your TDB notes.", energyDelta = -14, academicDelta = 0.20),
                EventChoice("Final All-Nighter Cram", "Drank strong coffee and crammed every past question!", energyDelta = -24, hungerDelta = -10, academicDelta = 0.35),
                EventChoice("Use Micro-Chips ('Expo')", "Risky! Invigilators are patrolling every row.", academicDelta = 0.55, heatDelta = 35, forceExpulsionChance = 0.28f)
            )
        )
        LifeStage.POSTGRADUATE -> CampusEvent(
            id = "day30_exam",
            title = "Day 30: Thesis Viva & Defence!",
            description = "The external examiner just walked in to grill Chapters 1–5 of your thesis.",
            tag = "Day 30 Viva",
            choices = listOf(
                EventChoice("Defend Methodology Calmly", "Clear presentation impressed the panel.", energyDelta = -14, academicDelta = 8.0),
                EventChoice("Present Extra Statistical Proofs", "Intense defence! Research score jumped sharply.", energyDelta = -22, socialDelta = -8, academicDelta = 14.0)
            )
        )
        LifeStage.LECTURER -> CampusEvent(
            id = "day30_exam",
            title = "Day 30: Professorial Promotion Board!",
            description = "Senate Appointments & Promotions Committee is reviewing your publications and student feedback.",
            tag = "Day 30 Board",
            choices = listOf(
                EventChoice("Present Published Journals", "Board commended your academic rigor.", energyDelta = -12, academicDelta = 8.0, socialDelta = 8),
                EventChoice("Defend Full Grant Dossier", "Exhausting board session, huge promotion boost!", energyDelta = -20, academicDelta = 14.0)
            )
        )
    }

    fun resolveEventChoice(career: StudentCareer, event: CampusEvent, choice: EventChoice): ActionOutcome {
        val stage = career.stageEnum
        val newMoney = (career.money + choice.moneyDelta).coerceIn(-1_000_000L, 1_000_000_000L)
        val newEnergy = (career.energy + choice.energyDelta).coerceIn(0L, 100L)
        val newHunger = (career.hunger + choice.hungerDelta).coerceIn(0L, 100L)
        val newAcademic = (career.academicStat + choice.academicDelta).coerceIn(0.0, stage.maxAcademic)
        val newSocial = (career.socialStat + choice.socialDelta).coerceIn(0L, 100L)
        val newHeat = (career.riskHeat + choice.heatDelta).coerceIn(0L, 100L)
        val newStrike = (career.strikeDaysLeft + choice.triggerStrikeDays).coerceIn(0L, 30L)

        val expelledByRoll = choice.forceExpulsionChance > 0f && Random.nextFloat() < choice.forceExpulsionChance
        val status = when {
            expelledByRoll || newHeat >= 95L -> CareerStatus.EXPELLED
            newHunger <= 0L || newEnergy <= 0L -> CareerStatus.DROPPED_OUT
            else -> CareerStatus.ACTIVE
        }

        val outcomeHeadline = when {
            expelledByRoll -> "Caught red-handed! Disciplinary panel expelled you immediately!"
            newHunger <= 0L -> "Collapsed from hunger during the event!"
            newEnergy <= 0L -> "Collapsed from total exhaustion!"
            else -> choice.outcomeText
        }

        val updated = career.copy(
            money = newMoney,
            energy = newEnergy,
            hunger = newHunger,
            academicStat = newAcademic,
            socialStat = newSocial,
            riskHeat = newHeat,
            strikeDaysLeft = newStrike,
            careerStatus = status.name,
            lastLogSummary = outcomeHeadline.take(300)
        )

        val actualMoneyDelta = newMoney - career.money
        val actualEnergyDelta = newEnergy - career.energy
        val actualHungerDelta = newHunger - career.hunger
        val actualAcademicDelta = newAcademic - career.academicStat
        val actualSocialDelta = newSocial - career.socialStat
        val actualHeatDelta = newHeat - career.riskHeat

        val deltaChips = buildDeltaChips(
            stage = stage,
            moneyDelta = actualMoneyDelta,
            energyDelta = actualEnergyDelta,
            hungerDelta = actualHungerDelta,
            academicDelta = actualAcademicDelta,
            socialDelta = actualSocialDelta,
            heatDelta = actualHeatDelta
        )

        val stageEndReady = status == CareerStatus.ACTIVE && (event.id == "day30_exam" || updated.day >= 30L)

        return ActionOutcome(
            updatedCareer = updated,
            diaryEntry = DiaryEntry(
                dayLabel = event.tag,
                headline = outcomeHeadline,
                deltaSummary = choice.label,
                isWarning = status != CareerStatus.ACTIVE || choice.heatDelta > 15,
                category = ActionCategory.CAMPUS,
                deltas = deltaChips
            ),
            appliedDeltas = StatDeltaSnapshot(
                sequenceId = 0L,
                moneyDelta = actualMoneyDelta,
                energyDelta = actualEnergyDelta,
                hungerDelta = actualHungerDelta,
                academicDelta = actualAcademicDelta,
                socialDelta = actualSocialDelta,
                heatDelta = actualHeatDelta,
                stage = stage
            ),
            stageEndReady = stageEndReady
        )
    }

    fun evaluateStageEnd(career: StudentCareer): Pair<Boolean, String> {
        val stage = career.stageEnum
        val passed = career.academicStat >= stage.passThreshold
        return when (stage) {
            LifeStage.SECONDARY -> {
                if (passed) {
                    true to "WAEC & JAMB Cleared! Score ${career.academicStat.roundToInt()}% — Admission letter to University is ready!"
                } else {
                    false to "WAEC Wahala! Score ${career.academicStat.roundToInt()}% fell below 45%. Time to rewrite next year."
                }
            }
            LifeStage.UNIVERSITY -> {
                val cgpa = career.academicStat
                val degreeClass = when {
                    cgpa >= 4.50 -> "First Class Honours!"
                    cgpa >= 3.50 -> "Second Class Upper (2:1)!"
                    cgpa >= 2.50 -> "Second Class Lower (2:2)"
                    cgpa >= 1.50 -> "Third Class Honours"
                    else -> "Carryover Probation"
                }
                if (passed) {
                    true to "Convocation Day! Graduated from ${career.schoolName} with $degreeClass (${String.format(Locale.US, "%.2f", cgpa)} CGPA)."
                } else {
                    false to "Senate list dropped! CGPA ${String.format(Locale.US, "%.2f", cgpa)} is below 1.50 — Too many carryovers."
                }
            }
            LifeStage.POSTGRADUATE -> {
                if (passed && career.socialStat > 10) {
                    true to "Thesis Viva Passed! You successfully defended Chapter 1–5. Congratulations, Dr. ${career.characterName}!"
                } else {
                    false to "Viva Deferred! Either Research (${career.academicStat.roundToInt()}%) or Mental Health wasn't strong enough."
                }
            }
            LifeStage.LECTURER -> {
                if (passed) {
                    true to "Inaugural Lecture Delivered! Senate promoted you to Full Professor!"
                } else {
                    false to "Promotion stalled at Senior Lecturer (${career.academicStat.roundToInt()}/70 pts)."
                }
            }
        }
    }

    fun promoteToNextStage(career: StudentCareer): StudentCareer? {
        val current = career.stageEnum
        val next = current.nextStage() ?: return null
        val completed = (career.completedStages + current.name).distinct().take(4)
        val nextSchool = if (current == LifeStage.SECONDARY) universities.first() else career.schoolName
        val nextAcademic = when (next) {
            LifeStage.UNIVERSITY -> 4.00
            LifeStage.POSTGRADUATE -> 15.0
            LifeStage.LECTURER -> 15.0
            LifeStage.SECONDARY -> 60.0
        }
        val nextAccom = when (next) {
            LifeStage.UNIVERSITY -> if (career.accommodationEnum == Accommodation.LODGE) Accommodation.LODGE else Accommodation.HOSTEL
            LifeStage.POSTGRADUATE -> Accommodation.LODGE
            LifeStage.LECTURER -> Accommodation.STAFF_QUARTERS
            LifeStage.SECONDARY -> Accommodation.HOSTEL
        }
        val bonusGrant = when (next) {
            LifeStage.UNIVERSITY -> 20_000L
            LifeStage.POSTGRADUATE -> 55_000L
            LifeStage.LECTURER -> 150_000L
            LifeStage.SECONDARY -> 0L
        }
        return career.copy(
            stage = next.name,
            schoolName = nextSchool,
            levelLabel = levelLabelFor(next, 1L),
            day = 1L,
            money = (career.money + bonusGrant).coerceIn(-1_000_000L, 1_000_000_000L),
            energy = 90L,
            hunger = 85L,
            academicStat = nextAcademic,
            socialStat = 65L,
            riskHeat = (career.riskHeat / 2L),
            careerStatus = CareerStatus.ACTIVE.name,
            strikeDaysLeft = 0L,
            accommodation = nextAccom.name,
            completedStages = completed,
            lastLogSummary = "Promoted to ${next.displayName} at $nextSchool! Received ₦${formatShort(bonusGrant)} milestone bonus."
        )
    }

    fun computeLeaderboardSummary(career: StudentCareer, finalTitleOverride: String? = null): Triple<String, String, Long> {
        val stage = career.stageEnum
        val stageReached = if (stage == LifeStage.LECTURER && career.academicStat >= stage.passThreshold) {
            "PROFESSOR"
        } else {
            stage.name
        }
        val cgpaDisplay = when (stage) {
            LifeStage.SECONDARY -> "${career.academicStat.roundToInt()}% WAEC"
            LifeStage.UNIVERSITY -> "${String.format(Locale.US, "%.2f", career.academicStat)} CGPA"
            LifeStage.POSTGRADUATE -> "${career.academicStat.roundToInt()}% PhD"
            LifeStage.LECTURER -> "${career.academicStat.roundToInt()} Promo Pts"
        }
        val title = finalTitleOverride ?: when (stage) {
            LifeStage.SECONDARY -> if (career.academicStat >= 75) "WAEC Distinction Scholar" else "Senior Sec Graduate"
            LifeStage.UNIVERSITY -> when {
                career.academicStat >= 4.50 -> "First Class Graduate"
                career.academicStat >= 3.50 -> "2:1 Honours Graduate"
                career.academicStat >= 2.50 -> "2:2 Graduate"
                else -> "University Survivor"
            }
            LifeStage.POSTGRADUATE -> "Dr. ${career.characterName.substringBefore(' ')}"
            LifeStage.LECTURER -> if (career.academicStat >= 70) "Professor Emeritus" else "Senior Lecturer"
        }
        val stageBonus = (career.completedStages.size + 1) * 2_500L
        val academicPts = when (stage) {
            LifeStage.UNIVERSITY -> (career.academicStat * 1_200.0).toLong()
            else -> (career.academicStat * 55.0).toLong()
        }
        val moneyPts = (career.money.coerceAtLeast(0L) / 250L).coerceAtMost(15_000L)
        val score = (stageBonus + academicPts + moneyPts + career.socialStat * 15L - career.riskHeat * 10L)
            .coerceIn(100L, 100_000_000L)
        return Triple(title, "$cgpaDisplay · $stageReached", score)
    }

    private fun levelLabelFor(stage: LifeStage, day: Long): String = when (stage) {
        LifeStage.SECONDARY -> when {
            day <= 5 -> "JSS 1"
            day <= 10 -> "JSS 2"
            day <= 15 -> "JSS 3"
            day <= 20 -> "SS 1"
            day <= 25 -> "SS 2"
            else -> "SS 3 (WAEC)"
        }
        LifeStage.UNIVERSITY -> when {
            day <= 7 -> "100 Level"
            day <= 14 -> "200 Level"
            day <= 22 -> "300 Level"
            else -> "400L Final Year"
        }
        LifeStage.POSTGRADUATE -> when {
            day <= 12 -> "M.Sc Thesis"
            day <= 24 -> "PhD Research"
            else -> "PhD Viva Prep"
        }
        LifeStage.LECTURER -> when {
            day <= 8 -> "Asst. Lecturer"
            day <= 16 -> "Lecturer I"
            day <= 24 -> "Senior Lecturer"
            else -> "Assoc. Prof"
        }
    }

    private fun pickRandomEvent(career: StudentCareer): CampusEvent {
        return when (career.stageEnum) {
            LifeStage.SECONDARY -> listOf(
                CampusEvent(
                    id = "sec_senior",
                    title = "Senior Wahala at Hostel!",
                    description = "Senior Chidi calls you from his corner: 'Smallie, run go tuck shop buy me Gala and Lacasera now!'",
                    tag = "Hostel Drama",
                    choices = listOf(
                        EventChoice("Buy it for him (-₦1,000)", "Senior patted your back and left you in peace.", moneyDelta = -1_000, energyDelta = -5, socialDelta = 6),
                        EventChoice("Tell him you're reading for test", "You held your ground! He made you frog-jump for 5 mins though.", energyDelta = -14, academicDelta = 3.0, socialDelta = 4)
                    )
                ),
                CampusEvent(
                    id = "sec_pta",
                    title = "PTA Meeting Result Check",
                    description = "Your class teacher just handed your mid-term booklet to your parents at the school hall!",
                    tag = "PTA Meeting",
                    choices = listOf(
                        EventChoice(
                            "Show booklet boldly",
                            if (career.academicStat >= 60.0) "Parents smiled and blessed you with ₦8,000!" else "Mummy frowned and cut your pocket money!",
                            moneyDelta = if (career.academicStat >= 60.0) 8_000 else -2_500,
                            socialDelta = if (career.academicStat >= 60.0) 8 else -5
                        ),
                        EventChoice("Promise to double your prep hours", "Dad gave you one last warning and ₦2,000.", moneyDelta = 2_000, academicDelta = 2.5)
                    )
                ),
                CampusEvent(
                    id = "sec_crush",
                    title = "Note Passed in Class!",
                    description = "Your seatmate just slid a folded paper across your locker during Biology class.",
                    tag = "Class Crush",
                    choices = listOf(
                        EventChoice("Reply & gist after assembly", "Smooth! Your social confidence soared.", academicDelta = -2.0, socialDelta = 14),
                        EventChoice("Focus on chalkboard instead", "Zero distractions. Teacher praised your concentration.", academicDelta = 4.0, socialDelta = -3)
                    )
                )
            ).random()

            LifeStage.UNIVERSITY -> listOf(
                CampusEvent(
                    id = "uni_asuu",
                    title = "ASUU Declares Warning Strike!",
                    description = "Breaking news on campus radio: ASUU has suspended lectures for 4 days over unpaid allowances.",
                    tag = "ASUU Strike",
                    choices = listOf(
                        EventChoice("Use strike days to hustle hard", "Smart move! Extra time for POS & content gigs.", moneyDelta = 12_000, energyDelta = -10, triggerStrikeDays = 4),
                        EventChoice("Travel home for Mama's food", "Free jollof and rest! Strike lasts 4 days.", hungerDelta = 30, energyDelta = 20, triggerStrikeDays = 4)
                    )
                ),
                CampusEvent(
                    id = "uni_nepa",
                    title = "NEPA Took Light During TDB!",
                    description = "Total blackout across hostel and reading room. Your phone torch is at 9%.",
                    tag = "NEPA Wahala",
                    choices = listOf(
                        EventChoice("Buy fuel for reading room gen (-₦2,000)", "Light came back and you smashed past questions!", moneyDelta = -2_000, academicDelta = 0.14, energyDelta = -8),
                        EventChoice("Sleep with mosquitoes instead", "Rough night, woke up sweating.", energyDelta = 15, hungerDelta = -8)
                    )
                ),
                CampusEvent(
                    id = "uni_malpractice",
                    title = "Mid-Semester Test Temptation",
                    description = "A friend whispers he has 'expo' (micro-chips) for tomorrow's tough GST paper.",
                    tag = "Exam Ethics",
                    choices = listOf(
                        EventChoice("Reject expo and burn midnight oil", "Tough night, clean conscience, solid score!", energyDelta = -18, academicDelta = 0.12, heatDelta = -5),
                        EventChoice("Use micro-chips in hall", "Risky business! Invigilator nearly spotted you.", academicDelta = 0.22, heatDelta = 28, forceExpulsionChance = 0.20f)
                    )
                )
            ).random()

            LifeStage.POSTGRADUATE -> listOf(
                CampusEvent(
                    id = "pg_chapter3",
                    title = "Supervisor Email: 'Redo Chapter 3'",
                    description = "After 3 weeks of waiting, Prof replied with 1 sentence: 'Methodology weak. Redo Chapter 3.'",
                    tag = "Supervisor Wahala",
                    choices = listOf(
                        EventChoice("Cry small, drink coffee & rewrite", "Painful grind, but your thesis is stronger now.", moneyDelta = -2_000, energyDelta = -15, academicDelta = 5.0, socialDelta = -10),
                        EventChoice("Take a mental reset day first", "Protected your sanity before tackling edits.", socialDelta = 15, academicDelta = -2.0)
                    )
                ),
                CampusEvent(
                    id = "pg_funding",
                    title = "Research Stipend Delayed",
                    description = "Grant disbursement is stuck in bursary paperwork this week.",
                    tag = "Funding Wahala",
                    choices = listOf(
                        EventChoice("Pick up urgent freelance review", "Stressful week, but rent & food are covered.", moneyDelta = 24_000, energyDelta = -18, socialDelta = -6),
                        EventChoice("Borrow from colleague (-₦0 now)", "Survived on garri and good friends.", hungerDelta = -10, socialDelta = 6)
                    )
                )
            ).random()

            LifeStage.LECTURER -> listOf(
                CampusEvent(
                    id = "lec_bribe",
                    title = "Student Knocks on Office Door",
                    description = "A student slips an envelope on your desk: 'Sir, please help my carry-over. ₦45,000 appreciation.'",
                    tag = "Integrity Test",
                    choices = listOf(
                        EventChoice("Reject firmly & mentor them", "Word spreads across faculty that you're incorruptible!", academicDelta = 4.0, socialDelta = 18, heatDelta = -8),
                        EventChoice("Quietly pocket the envelope", "Fast cash, but VC panel rumors are swirling.", moneyDelta = 45_000, socialDelta = -15, heatDelta = 35, forceExpulsionChance = 0.22f)
                    )
                ),
                CampusEvent(
                    id = "lec_protest",
                    title = "SUG Protests Tough Exam!",
                    description = "Students gathered outside Senate Building chanting your course code after mass failure.",
                    tag = "Campus Protest",
                    choices = listOf(
                        EventChoice("Organize free extra revision class", "Tension cooled down and students respect your fairness.", energyDelta = -15, socialDelta = 16, academicDelta = 3.0),
                        EventChoice("Ignore them ('Read your books!')", "VC gave you a side-eye, students fear your course.", socialDelta = -12, heatDelta = 12)
                    )
                )
            ).random()
        }
    }

    fun formatNaira(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        val sign = if (amount < 0) "-" else ""
        return "${sign}₦${formatter.format(kotlin.math.abs(amount))}"
    }

    fun formatShort(amount: Long): String = when {
        amount >= 1_000_000 -> String.format(Locale.US, "%.1fM", amount / 1_000_000.0)
        amount >= 1_000 -> String.format(Locale.US, "%.1fk", amount / 1_000.0).replace(".0k", "k")
        else -> amount.toString()
    }
}
