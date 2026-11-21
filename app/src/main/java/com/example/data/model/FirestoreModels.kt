package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Fresh Student",
    val bestCareerTitle: String = "Jambite",
    val highestNetWorth: Long = 0L,
    val highestStage: String = LifeStage.SECONDARY.name,
    val totalGraduations: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(60).ifBlank { "Fresh Student" },
        "bestCareerTitle" to bestCareerTitle.take(80),
        "highestNetWorth" to highestNetWorth.coerceIn(-1_000_000L, 1_000_000_000L),
        "highestStage" to highestStage,
        "totalGraduations" to totalGraduations.coerceIn(0L, 10_000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class StudentCareer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val characterName: String = "",
    val gender: String = Gender.MALE.name,
    val schoolName: String = "",
    val archetype: String = Archetype.STRUGGLE.name,
    val stage: String = LifeStage.UNIVERSITY.name,
    val levelLabel: String = "100 Level",
    val day: Long = 1L,
    val totalDays: Long = 1L,
    val money: Long = 12_000L,
    val energy: Long = 85L,
    val hunger: Long = 80L,
    val academicStat: Double = 4.5,
    val socialStat: Long = 50L,
    val riskHeat: Long = 0L,
    val careerStatus: String = CareerStatus.ACTIVE.name,
    val carryoverCount: Long = 0L,
    val strikeDaysLeft: Long = 0L,
    val accommodation: String = Accommodation.HOSTEL.name,
    val affiliation: String = Affiliation.NONE.name,
    val itemsOwned: List<String> = emptyList(),
    val completedStages: List<String> = emptyList(),
    val lastLogSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val stageEnum: LifeStage get() = LifeStage.fromRaw(stage)
    val archetypeEnum: Archetype get() = Archetype.fromRaw(archetype)
    val genderEnum: Gender get() = Gender.fromRaw(gender)
    val statusEnum: CareerStatus get() = CareerStatus.fromRaw(careerStatus)
    val accommodationEnum: Accommodation get() = Accommodation.fromRaw(accommodation)
    val affiliationEnum: Affiliation get() = Affiliation.fromRaw(affiliation)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "characterName" to characterName.take(40).ifBlank { "Student" },
        "gender" to genderEnum.name,
        "schoolName" to schoolName.take(60).ifBlank { "UNILAG (Akoka)" },
        "archetype" to archetypeEnum.name,
        "stage" to stageEnum.name,
        "levelLabel" to levelLabel.take(40).ifBlank { "100 Level" },
        "day" to day.coerceIn(1L, 31L),
        "totalDays" to totalDays.coerceIn(1L, 500L),
        "money" to money.coerceIn(-1_000_000L, 1_000_000_000L),
        "energy" to energy.coerceIn(0L, 100L),
        "hunger" to hunger.coerceIn(0L, 100L),
        "academicStat" to academicStat.coerceIn(0.0, 100.0),
        "socialStat" to socialStat.coerceIn(0L, 100L),
        "riskHeat" to riskHeat.coerceIn(0L, 100L),
        "careerStatus" to statusEnum.name,
        "carryoverCount" to carryoverCount.coerceIn(0L, 50L),
        "strikeDaysLeft" to strikeDaysLeft.coerceIn(0L, 30L),
        "accommodation" to accommodationEnum.name,
        "affiliation" to affiliationEnum.name,
        "itemsOwned" to itemsOwned.take(10),
        "completedStages" to completedStages.take(4),
        "lastLogSummary" to lastLogSummary.take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "schoolName" to schoolName.take(60).ifBlank { "UNILAG (Akoka)" },
        "stage" to stageEnum.name,
        "levelLabel" to levelLabel.take(40).ifBlank { "100 Level" },
        "day" to day.coerceIn(1L, 31L),
        "totalDays" to totalDays.coerceIn(1L, 500L),
        "money" to money.coerceIn(-1_000_000L, 1_000_000_000L),
        "energy" to energy.coerceIn(0L, 100L),
        "hunger" to hunger.coerceIn(0L, 100L),
        "academicStat" to academicStat.coerceIn(0.0, 100.0),
        "socialStat" to socialStat.coerceIn(0L, 100L),
        "riskHeat" to riskHeat.coerceIn(0L, 100L),
        "careerStatus" to statusEnum.name,
        "carryoverCount" to carryoverCount.coerceIn(0L, 50L),
        "strikeDaysLeft" to strikeDaysLeft.coerceIn(0L, 30L),
        "accommodation" to accommodationEnum.name,
        "affiliation" to affiliationEnum.name,
        "itemsOwned" to itemsOwned.take(10),
        "completedStages" to completedStages.take(4),
        "lastLogSummary" to lastLogSummary.take(300),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val characterName: String = "",
    val schoolName: String = "",
    val stageReached: String = LifeStage.UNIVERSITY.name,
    val careerTitle: String = "",
    val score: Long = 0L,
    val netWorth: Long = 0L,
    val cgpaDisplay: String = "",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "characterName" to characterName.take(40).ifBlank { "Student" },
        "schoolName" to schoolName.take(60).ifBlank { "UNILAG (Akoka)" },
        "stageReached" to stageReached,
        "careerTitle" to careerTitle.take(80).ifBlank { "Campus Survivor" },
        "score" to score.coerceIn(0L, 100_000_000L),
        "netWorth" to netWorth.coerceIn(-1_000_000L, 1_000_000_000L),
        "cgpaDisplay" to cgpaDisplay.take(30).ifBlank { "N/A" },
        "visibility" to "PUBLIC",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "characterName" to characterName.take(40).ifBlank { "Student" },
        "schoolName" to schoolName.take(60).ifBlank { "UNILAG (Akoka)" },
        "stageReached" to stageReached,
        "careerTitle" to careerTitle.take(80).ifBlank { "Campus Survivor" },
        "score" to score.coerceIn(0L, 100_000_000L),
        "netWorth" to netWorth.coerceIn(-1_000_000L, 1_000_000_000L),
        "cgpaDisplay" to cgpaDisplay.take(30).ifBlank { "N/A" },
        "visibility" to "PUBLIC",
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
