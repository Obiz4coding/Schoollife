package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Accommodation
import com.example.data.model.ActionCategory
import com.example.data.model.ActiveStatusEffect
import com.example.data.model.Archetype
import com.example.data.model.CampusAction
import com.example.data.model.CampusEvent
import com.example.data.model.CareerStatus
import com.example.data.model.DiaryEntry
import com.example.data.model.EventChoice
import com.example.data.model.Gender
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LifeStage
import com.example.data.model.ShopItem
import com.example.data.model.StatDeltaChip
import com.example.data.model.StatDeltaSnapshot
import com.example.data.model.StudentCareer
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.StudentLifeRepository
import com.example.game.CareerEffectTracker
import com.example.game.StudentLifeEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CoreStatsSnapshot(
    val money: Long,
    val energy: Long,
    val hunger: Long,
    val academicStat: Double,
    val day: Long,
    val stage: LifeStage
)

data class StageCompletionBanner(
    val passed: Boolean,
    val message: String,
    val canAdvanceNext: Boolean,
    val nextStageName: String?
)

class StudentLifeViewModel(
    private val repository: StudentLifeRepository,
    private val currentUserId: String,
    private val userDisplayName: String
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val careersState: StateFlow<UiState<List<StudentCareer>>> = repository.observeCareers(currentUserId)
        .map<List<StudentCareer>, UiState<List<StudentCareer>>> { list ->
            autoSelectActiveCareerIfUnset(list)
            val active = list.firstOrNull { it.id == _selectedCareerId.value } ?: list.firstOrNull()
            _coreStats.value = active?.let {
                CoreStatsSnapshot(
                    money = it.money,
                    energy = it.energy,
                    hunger = it.hunger,
                    academicStat = it.academicStat,
                    day = it.day,
                    stage = it.stageEnum
                )
            }
            if (active != null) {
                syncCareerUiCache(active)
            } else {
                _activeStatusEffects.value = emptyList()
                _diaryEntries.value = emptyList()
            }
            UiState.Success(list)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing careers", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load campus lives"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard()
        .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing leaderboard", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load Hall of Fame"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedCareerId = MutableStateFlow<String?>(null)
    val selectedCareerId: StateFlow<String?> = _selectedCareerId.asStateFlow()

    private val _coreStats = MutableStateFlow<CoreStatsSnapshot?>(null)
    val coreStats: StateFlow<CoreStatsSnapshot?> = _coreStats.asStateFlow()

    private val _diaryEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaryEntries: StateFlow<List<DiaryEntry>> = _diaryEntries.asStateFlow()

    private val _activeStatusEffects = MutableStateFlow<List<ActiveStatusEffect>>(emptyList())
    val activeStatusEffects: StateFlow<List<ActiveStatusEffect>> = _activeStatusEffects.asStateFlow()

    private val _lastStatDelta = MutableStateFlow<StatDeltaSnapshot?>(null)
    val lastStatDelta: StateFlow<StatDeltaSnapshot?> = _lastStatDelta.asStateFlow()

    private val _activeEvent = MutableStateFlow<CampusEvent?>(null)
    val activeEvent: StateFlow<CampusEvent?> = _activeEvent.asStateFlow()

    private val _stageCompletion = MutableStateFlow<StageCompletionBanner?>(null)
    val stageCompletion: StateFlow<StageCompletionBanner?> = _stageCompletion.asStateFlow()

    private val _statusFeedback = MutableStateFlow<String?>(null)
    val statusFeedback: StateFlow<String?> = _statusFeedback.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val trackersByCareer = mutableMapOf<String, CareerEffectTracker>()
    private val diaryByCareer = mutableMapOf<String, List<DiaryEntry>>()
    private var deltaSequenceCounter = 0L

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(userDisplayName)
        }
    }

    private fun autoSelectActiveCareerIfUnset(list: List<StudentCareer>) {
        if (_selectedCareerId.value == null && list.isNotEmpty()) {
            val preferred = list.firstOrNull { !it.statusEnum.isTerminal() } ?: list.first()
            _selectedCareerId.value = preferred.id
        } else if (_selectedCareerId.value != null && list.none { it.id == _selectedCareerId.value }) {
            _selectedCareerId.value = list.firstOrNull()?.id
        }
    }

    private fun syncCareerUiCache(career: StudentCareer) {
        val tracker = trackersByCareer[career.id] ?: CareerEffectTracker()
        _activeStatusEffects.value = tracker.activeEffects
        val existingLog = diaryByCareer[career.id]
        if (existingLog != null) {
            _diaryEntries.value = existingLog
        } else {
            val seeded = if (career.lastLogSummary.isNotBlank()) {
                listOf(
                    DiaryEntry(
                        dayLabel = "Day ${career.day}",
                        headline = career.lastLogSummary,
                        deltaSummary = "${career.stageEnum.displayName} · ${career.archetypeEnum.title}",
                        isWarning = career.statusEnum.isTerminal()
                    )
                )
            } else {
                emptyList()
            }
            diaryByCareer[career.id] = seeded
            _diaryEntries.value = seeded
        }
    }

    fun selectCareer(careerId: String) {
        _selectedCareerId.value = careerId
        _activeEvent.value = null
        _stageCompletion.value = null
        _lastStatDelta.value = null
        _activeStatusEffects.value = trackersByCareer[careerId]?.activeEffects ?: emptyList()
        _diaryEntries.value = diaryByCareer[careerId] ?: emptyList()
    }

    fun clearFeedback() {
        _statusFeedback.value = null
    }

    fun startNewCareer(
        name: String,
        gender: Gender,
        schoolName: String,
        archetype: Archetype,
        startingStage: LifeStage,
        onCreated: () -> Unit
    ) {
        if (name.isBlank()) {
            _statusFeedback.value = "Please enter a character name."
            return
        }
        viewModelScope.launch {
            _isMutating.value = true
            val career = StudentLifeEngine.createNewCareer(
                userId = currentUserId,
                characterName = name,
                gender = gender,
                schoolName = schoolName,
                archetype = archetype,
                startingStage = startingStage
            )
            val result = repository.createCareer(career)
            _isMutating.value = false
            result.onSuccess { newId ->
                val initialDiary = listOf(
                    DiaryEntry(
                        dayLabel = "Day 1",
                        headline = career.lastLogSummary,
                        deltaSummary = "${startingStage.displayName} · ${archetype.title}"
                    )
                )
                trackersByCareer[newId] = CareerEffectTracker()
                diaryByCareer[newId] = initialDiary
                _selectedCareerId.value = newId
                _coreStats.value = CoreStatsSnapshot(
                    money = career.money,
                    energy = career.energy,
                    hunger = career.hunger,
                    academicStat = career.academicStat,
                    day = career.day,
                    stage = career.stageEnum
                )
                _activeStatusEffects.value = emptyList()
                _lastStatDelta.value = null
                _diaryEntries.value = initialDiary
                _activeEvent.value = null
                _stageCompletion.value = null
                onCreated()
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Could not create character."
            }
        }
    }

    fun onStudy(career: StudentCareer) {
        val action = StudentLifeEngine.quickCoreActionsForCareer(career)[0].second
        executeAction(career, action)
    }

    fun onWork(career: StudentCareer) {
        val action = StudentLifeEngine.quickCoreActionsForCareer(career)[1].second
        executeAction(career, action)
    }

    fun onEat(career: StudentCareer) {
        val action = StudentLifeEngine.quickCoreActionsForCareer(career)[2].second
        executeAction(career, action)
    }

    fun onSleep(career: StudentCareer) {
        val action = StudentLifeEngine.quickCoreActionsForCareer(career)[3].second
        executeAction(career, action)
    }

    fun executeAction(career: StudentCareer, action: CampusAction) {
        if (career.statusEnum.isTerminal()) {
            _statusFeedback.value = "This journey has ended (${career.statusEnum.displayName}). Start a new life!"
            return
        }
        if (_isMutating.value) return
        if (career.day >= 30L) {
            _activeEvent.value = StudentLifeEngine.buildDay30ExamEvent(career)
            return
        }
        if (action.moneyDelta < 0 && career.money + action.moneyDelta < 0) {
            _statusFeedback.value = "Not enough Naira! Work/Hustle first or soak Garri."
            return
        }

        val prevTracker = trackersByCareer[career.id] ?: CareerEffectTracker()
        val outcome = StudentLifeEngine.performAction(career, action, prevTracker.activeEffects)
        val updated = outcome.updatedCareer

        val effectUpdate = StudentLifeEngine.evaluateStatusEffectsAfterAction(
            previousTracker = prevTracker,
            action = action,
            updatedCareer = updated
        )
        trackersByCareer[career.id] = effectUpdate.tracker
        _activeStatusEffects.value = effectUpdate.tracker.activeEffects

        val enrichedEntry = outcome.diaryEntry.copy(
            statusEffectNotice = effectUpdate.summaryNotice
        )
        appendDiaryEntry(career.id, enrichedEntry)

        deltaSequenceCounter += 1L
        _lastStatDelta.value = outcome.appliedDeltas.copy(sequenceId = deltaSequenceCounter)

        _coreStats.value = CoreStatsSnapshot(
            money = updated.money,
            energy = updated.energy,
            hunger = updated.hunger,
            academicStat = updated.academicStat,
            day = updated.day,
            stage = updated.stageEnum
        )

        if (effectUpdate.newlyTriggered.isNotEmpty() && outcome.triggeredEvent == null) {
            _statusFeedback.value = effectUpdate.newlyTriggered.joinToString(" & ") {
                "${it.title}: ${it.badgeText}"
            }
        }

        viewModelScope.launch {
            _isMutating.value = true
            val res = saveCharacterStatsToFirestore(updated)
            _isMutating.value = false
            res.onSuccess {
                if (updated.statusEnum.isTerminal()) {
                    submitRecord(updated, outcome.diaryEntry.headline, graduated = false)
                } else if (outcome.triggeredEvent != null) {
                    _activeEvent.value = outcome.triggeredEvent
                } else if (outcome.stageEndReady) {
                    triggerStageEndEvaluation(updated)
                }
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Failed to save action."
            }
        }
    }

    fun resolveEvent(career: StudentCareer, choice: EventChoice) {
        val event = _activeEvent.value ?: return
        _activeEvent.value = null
        val outcome = StudentLifeEngine.resolveEventChoice(career, event, choice)
        val updated = outcome.updatedCareer
        appendDiaryEntry(career.id, outcome.diaryEntry)

        deltaSequenceCounter += 1L
        _lastStatDelta.value = outcome.appliedDeltas.copy(sequenceId = deltaSequenceCounter)

        _coreStats.value = CoreStatsSnapshot(
            money = updated.money,
            energy = updated.energy,
            hunger = updated.hunger,
            academicStat = updated.academicStat,
            day = updated.day,
            stage = updated.stageEnum
        )

        viewModelScope.launch {
            _isMutating.value = true
            val res = saveCharacterStatsToFirestore(updated)
            _isMutating.value = false
            res.onSuccess {
                if (updated.statusEnum.isTerminal()) {
                    submitRecord(updated, outcome.diaryEntry.headline, graduated = false)
                } else if (outcome.stageEndReady) {
                    triggerStageEndEvaluation(updated)
                }
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Could not save event outcome."
            }
        }
    }

    private fun appendDiaryEntry(careerId: String, entry: DiaryEntry) {
        val current = diaryByCareer[careerId] ?: _diaryEntries.value
        val updatedList = (listOf(entry) + current).take(40)
        diaryByCareer[careerId] = updatedList
        if (_selectedCareerId.value == careerId || careerId.isBlank()) {
            _diaryEntries.value = updatedList
        }
    }

    suspend fun saveCharacterStatsToFirestore(career: StudentCareer): Result<Unit> {
        return repository.saveStatsAndDay(career)
    }

    private fun triggerStageEndEvaluation(career: StudentCareer) {
        val (passed, message) = StudentLifeEngine.evaluateStageEnd(career)
        val nextStage = career.stageEnum.nextStage()
        _stageCompletion.value = StageCompletionBanner(
            passed = passed,
            message = message,
            canAdvanceNext = passed && nextStage != null,
            nextStageName = nextStage?.displayName
        )
    }

    fun advanceToNextLifeStage(career: StudentCareer) {
        val promoted = StudentLifeEngine.promoteToNextStage(career) ?: return
        _stageCompletion.value = null
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.updateCareer(promoted)
            _isMutating.value = false
            res.onSuccess {
                trackersByCareer[career.id] = CareerEffectTracker()
                _activeStatusEffects.value = emptyList()
                appendDiaryEntry(
                    career.id,
                    DiaryEntry(
                        dayLabel = "Promoted!",
                        headline = promoted.lastLogSummary,
                        deltaSummary = "Welcome to ${promoted.stageEnum.displayName}",
                        category = ActionCategory.ACADEMICS
                    )
                )
                submitRecord(career, null, graduated = true)
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Promotion save failed."
            }
        }
    }

    fun finalizeCareerAtStageEnd(career: StudentCareer, passed: Boolean) {
        _stageCompletion.value = null
        val finalStatus = when {
            !passed -> CareerStatus.DROPPED_OUT
            career.stageEnum == LifeStage.LECTURER -> CareerStatus.RETIRED_PROF
            else -> CareerStatus.GRADUATED
        }
        val finalized = career.copy(
            careerStatus = finalStatus.name,
            completedStages = if (passed) (career.completedStages + career.stage).distinct().take(4) else career.completedStages,
            lastLogSummary = if (passed) "Completed ${career.stageEnum.displayName} in style!" else "Academic setback on Day 30."
        )
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.updateCareer(finalized)
            _isMutating.value = false
            res.onSuccess {
                submitRecord(finalized, null, graduated = passed)
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Failed to record graduation."
            }
        }
    }

    fun purchaseShopItem(career: StudentCareer, item: ShopItem) {
        if (career.statusEnum.isTerminal()) return
        if (career.itemsOwned.contains(item.id)) {
            _statusFeedback.value = "You already own ${item.title}!"
            return
        }
        if (career.itemsOwned.size >= 10) {
            _statusFeedback.value = "Inventory full (10 items max)!"
            return
        }
        if (career.money < item.price) {
            _statusFeedback.value = "Need ${StudentLifeEngine.formatNaira(item.price)} to buy ${item.title}."
            return
        }

        val socialBoost = if (item.id == "iPhone" || item.id == "Benz") 12L else 3L
        val nextSocial = (career.socialStat + socialBoost).coerceAtMost(100L)
        val updated = career.copy(
            money = career.money - item.price,
            itemsOwned = career.itemsOwned + item.id,
            socialStat = nextSocial,
            lastLogSummary = "Bought ${item.title} for ${StudentLifeEngine.formatNaira(item.price)}!"
        )
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.updateCareer(updated)
            _isMutating.value = false
            res.onSuccess {
                deltaSequenceCounter += 1L
                _lastStatDelta.value = StatDeltaSnapshot(
                    sequenceId = deltaSequenceCounter,
                    moneyDelta = -item.price,
                    socialDelta = nextSocial - career.socialStat,
                    stage = career.stageEnum
                )
                appendDiaryEntry(
                    career.id,
                    DiaryEntry(
                        dayLabel = "Market",
                        headline = "Bought ${item.title}",
                        deltaSummary = item.buffLabel,
                        deltas = listOf(
                            StatDeltaChip("-₦${StudentLifeEngine.formatShort(item.price)}", isPositive = false),
                            StatDeltaChip("+${nextSocial - career.socialStat} ${career.stageEnum.socialLabel}", isPositive = true)
                        )
                    )
                )
                _statusFeedback.value = "Equipped ${item.title}!"
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Purchase failed."
            }
        }
    }

    fun upgradeAccommodation(career: StudentCareer) {
        if (career.statusEnum.isTerminal()) return
        if (career.accommodationEnum == Accommodation.LODGE || career.accommodationEnum == Accommodation.STAFF_QUARTERS) {
            _statusFeedback.value = "Already living in upgraded quarters!"
            return
        }
        val cost = Accommodation.LODGE.upgradeCost
        if (career.money < cost) {
            _statusFeedback.value = "Need ${StudentLifeEngine.formatNaira(cost)} for Off-Campus Solar Lodge."
            return
        }
        val nextEnergy = (career.energy + 15L).coerceAtMost(100L)
        val updated = career.copy(
            money = career.money - cost,
            accommodation = Accommodation.LODGE.name,
            energy = nextEnergy,
            lastLogSummary = "Moved into Off-Campus Solar Lodge! Goodbye hostel NEPA wahala."
        )
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.updateCareer(updated)
            _isMutating.value = false
            res.onSuccess {
                deltaSequenceCounter += 1L
                _lastStatDelta.value = StatDeltaSnapshot(
                    sequenceId = deltaSequenceCounter,
                    moneyDelta = -cost,
                    energyDelta = nextEnergy - career.energy,
                    stage = career.stageEnum
                )
                appendDiaryEntry(
                    career.id,
                    DiaryEntry(
                        dayLabel = "Lodge",
                        headline = "Upgraded to Off-Campus Solar Lodge",
                        deltaSummary = "24/7 light & better hostel rest",
                        deltas = listOf(
                            StatDeltaChip("-₦${StudentLifeEngine.formatShort(cost)}", isPositive = false),
                            StatDeltaChip("+${nextEnergy - career.energy} Energy", isPositive = true)
                        )
                    )
                )
                _statusFeedback.value = "Upgraded to Off-Campus Solar Lodge!"
            }.onFailure { err ->
                _statusFeedback.value = err.localizedMessage ?: "Upgrade failed."
            }
        }
    }

    fun deleteCareer(careerId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            repository.deleteCareer(careerId)
            _isMutating.value = false
        }
    }

    private suspend fun submitRecord(career: StudentCareer, overrideTitle: String?, graduated: Boolean) {
        val (computedTitle, cgpaDisplay, score) = StudentLifeEngine.computeLeaderboardSummary(
            career = career,
            finalTitleOverride = if (!graduated && overrideTitle != null && career.statusEnum == CareerStatus.EXPELLED) {
                "Expelled (${career.stageEnum.displayName})"
            } else null
        )
        val stageReached = if (career.stageEnum == LifeStage.LECTURER && graduated) "PROFESSOR" else career.stage
        repository.recordHallOfFameAndProfile(
            career = career,
            careerTitle = computedTitle,
            score = score,
            stageReached = stageReached,
            cgpaDisplay = cgpaDisplay,
            graduated = graduated
        )
    }

    private companion object {
        const val TAG = "StudentLifeVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
