package com.example

import com.example.data.model.Accommodation
import com.example.data.model.Archetype
import com.example.data.model.CareerStatus
import com.example.data.model.Gender
import com.example.data.model.LifeStage
import com.example.game.StudentLifeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentLifeEngineTest {

    @Test
    fun `initial stats differ properly by chosen character background`() {
        val ajebo = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Seyi Adeyemi",
            gender = Gender.MALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.AJEBO,
            startingStage = LifeStage.UNIVERSITY
        )
        val struggle = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Chinedu Okafor",
            gender = Gender.MALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.STRUGGLE,
            startingStage = LifeStage.UNIVERSITY
        )
        val hustler = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Adaeze Nwosu",
            gender = Gender.FEMALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.HUSTLER,
            startingStage = LifeStage.UNIVERSITY
        )

        // Money & accommodation reflect background
        assertEquals(85_000L, ajebo.money)
        assertEquals(12_000L, struggle.money)
        assertEquals(35_000L, hustler.money)
        assertEquals(Accommodation.LODGE, ajebo.accommodationEnum)
        assertEquals(Accommodation.HOSTEL, struggle.accommodationEnum)

        // Energy, Hunger, Academic CGPA, Social, and Risk Heat reflect background
        assertTrue(ajebo.energy > struggle.energy)
        assertTrue(ajebo.hunger > struggle.hunger)
        assertTrue(struggle.academicStat > ajebo.academicStat)
        assertTrue(ajebo.socialStat > struggle.socialStat)
        assertEquals(0L, ajebo.riskHeat)
        assertEquals(15L, hustler.riskHeat)
    }

    @Test
    fun `character can progress from Secondary through Lecturer`() {
        val career = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Chinedu Okafor",
            gender = Gender.MALE,
            schoolName = "King's College Lagos",
            archetype = Archetype.STRUGGLE,
            startingStage = LifeStage.SECONDARY
        )
        assertEquals(LifeStage.SECONDARY, career.stageEnum)
        assertEquals(CareerStatus.ACTIVE, career.statusEnum)

        // Advance to University
        val uni = StudentLifeEngine.promoteToNextStage(career)
        assertNotNull(uni)
        assertEquals(LifeStage.UNIVERSITY, uni!!.stageEnum)
        assertTrue(uni.completedStages.contains(LifeStage.SECONDARY.name))

        // Perform 8am Lecture action
        val lecture = StudentLifeEngine.actionsForCareer(uni).first { it.id == "uni_lecture" }
        val outcome = StudentLifeEngine.performAction(uni, lecture)
        assertEquals(2L, outcome.updatedCareer.day)
        assertTrue(outcome.updatedCareer.academicStat > uni.academicStat)

        // Advance to Postgraduate & Lecturer
        val pg = StudentLifeEngine.promoteToNextStage(outcome.updatedCareer)!!
        assertEquals(LifeStage.POSTGRADUATE, pg.stageEnum)
        val lecturer = StudentLifeEngine.promoteToNextStage(pg)!!
        assertEquals(LifeStage.LECTURER, lecturer.stageEnum)
    }

    @Test
    fun `every core action increments day and day 30 triggers exam event`() {
        var career = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Tunde Bakare",
            gender = Gender.MALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.STRUGGLE,
            startingStage = LifeStage.UNIVERSITY
        )

        val quick = StudentLifeEngine.quickCoreActionsForCareer(career)
        val study = quick[0].second
        val work = quick[1].second
        val eat = quick[2].second
        val sleep = quick[3].second

        // Study, Work, Eat, Sleep each advance Day by 1
        career = StudentLifeEngine.performAction(career, study).updatedCareer
        assertEquals(2L, career.day)
        career = StudentLifeEngine.performAction(career, work).updatedCareer
        assertEquals(3L, career.day)
        career = StudentLifeEngine.performAction(career, eat).updatedCareer
        assertEquals(4L, career.day)
        career = StudentLifeEngine.performAction(career, sleep).updatedCareer
        assertEquals(5L, career.day)

        // Advancing from Day 29 to Day 30 triggers the Day 30 Exam Event
        val day29Career = career.copy(day = 29L, energy = 80L, hunger = 80L)
        val day30Outcome = StudentLifeEngine.performAction(day29Career, study)
        assertEquals(30L, day30Outcome.updatedCareer.day)
        assertNotNull(day30Outcome.triggeredEvent)
        assertEquals("day30_exam", day30Outcome.triggeredEvent!!.id)
    }

    @Test
    fun `hunger hitting zero triggers dropout`() {
        val career = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Tunde Bakare",
            gender = Gender.MALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.STRUGGLE,
            startingStage = LifeStage.UNIVERSITY
        ).copy(hunger = 5L)

        val tdb = StudentLifeEngine.actionsForCareer(career).first { it.id == "uni_tdb" }
        val outcome = StudentLifeEngine.performAction(career, tdb)
        assertEquals(CareerStatus.DROPPED_OUT, outcome.updatedCareer.statusEnum)
    }

    @Test
    fun `consistent study triggers academic excellence and boosts cgpa gain`() {
        var career = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Chinedu Okafor",
            gender = Gender.MALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.AJEBO,
            startingStage = LifeStage.UNIVERSITY
        )
        val lecture = StudentLifeEngine.actionsForCareer(career).first { it.id == "uni_lecture" }
        var tracker = com.example.game.CareerEffectTracker()

        // Perform 3 consecutive study actions
        repeat(3) {
            val outcome = StudentLifeEngine.performAction(career, lecture, tracker.activeEffects)
            career = outcome.updatedCareer
            tracker = StudentLifeEngine.evaluateStatusEffectsAfterAction(tracker, lecture, career).tracker
        }

        assertTrue(tracker.activeEffects.any { it.type == com.example.data.model.StatusEffectType.ACADEMIC_EXCELLENCE })

        // Compare study gain with Academic Excellence vs baseline
        val baselineOutcome = StudentLifeEngine.performAction(career, lecture, emptyList())
        val boostedOutcome = StudentLifeEngine.performAction(career, lecture, tracker.activeEffects)
        assertTrue(boostedOutcome.appliedDeltas.academicDelta > baselineOutcome.appliedDeltas.academicDelta)
        assertTrue(boostedOutcome.diaryEntry.deltas.isNotEmpty())
    }

    @Test
    fun `low energy triggers burnout, reduces gains, and sleep clears burnout`() {
        val tiredCareer = StudentLifeEngine.createNewCareer(
            userId = "test_uid",
            characterName = "Seyi Adeyemi",
            gender = Gender.MALE,
            schoolName = "UNILAG (Akoka)",
            archetype = Archetype.AJEBO,
            startingStage = LifeStage.UNIVERSITY
        ).copy(energy = 32L)

        val lecture = StudentLifeEngine.actionsForCareer(tiredCareer).first { it.id == "uni_lecture" }
        val sleep = StudentLifeEngine.actionsForCareer(tiredCareer).first { it.id == "uni_sleep" }

        val firstOutcome = StudentLifeEngine.performAction(tiredCareer, lecture)
        val update1 = StudentLifeEngine.evaluateStatusEffectsAfterAction(
            previousTracker = com.example.game.CareerEffectTracker(),
            action = lecture,
            updatedCareer = firstOutcome.updatedCareer
        )

        // Energy dropped to 16% (<25%), so BURNOUT should be triggered
        assertTrue(update1.newlyTriggered.contains(com.example.data.model.StatusEffectType.BURNOUT))
        assertTrue(update1.tracker.activeEffects.any { it.type == com.example.data.model.StatusEffectType.BURNOUT })

        // While BURNOUT is active, study academic gain is reduced compared to non-burnout
        val normalOutcome = StudentLifeEngine.performAction(firstOutcome.updatedCareer, lecture, emptyList())
        val burnoutOutcome = StudentLifeEngine.performAction(firstOutcome.updatedCareer, lecture, update1.tracker.activeEffects)
        assertTrue(burnoutOutcome.appliedDeltas.academicDelta < normalOutcome.appliedDeltas.academicDelta)

        // Sleep clears BURNOUT
        val sleepOutcome = StudentLifeEngine.performAction(firstOutcome.updatedCareer, sleep, update1.tracker.activeEffects)
        val updateAfterSleep = StudentLifeEngine.evaluateStatusEffectsAfterAction(
            previousTracker = update1.tracker,
            action = sleep,
            updatedCareer = sleepOutcome.updatedCareer
        )
        assertTrue(updateAfterSleep.cleared.contains(com.example.data.model.StatusEffectType.BURNOUT))
        assertTrue(updateAfterSleep.tracker.activeEffects.none { it.type == com.example.data.model.StatusEffectType.BURNOUT })
    }
}
