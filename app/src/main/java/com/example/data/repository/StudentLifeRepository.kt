package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LifeStage
import com.example.data.model.StudentCareer
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudentLifeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user profile read." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeCareers(userId: String): Flow<List<StudentCareer>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user careers read." }
        val path = "users/$uid/careers"
        emitAll(
            db.collection("users").document(uid).collection("careers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudentCareer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.score }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val docRef = db.collection("users").document(activeUid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.ifBlank {
                    auth.currentUser?.displayName ?: "Naija Student"
                }.take(60)
                val profile = UserProfile(
                    userId = activeUid,
                    displayName = safeName,
                    bestCareerTitle = "Fresher",
                    highestNetWorth = 0L,
                    highestStage = LifeStage.SECONDARY.name,
                    totalGraduations = 0L
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createCareer(career: StudentCareer): Result<String> {
        val uid = auth.currentUser?.uid
        var docPath = "users/${uid ?: "unauthenticated"}/careers"
        return try {
            val activeUid = requireUserId()
            val docRef = db.collection("users").document(activeUid).collection("careers").document()
            docPath = docRef.path
            val payload = career.copy(userId = activeUid).toCreateMap()
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCareer(career: StudentCareer): Result<Unit> = saveStatsAndDay(career)

    suspend fun saveStatsAndDay(career: StudentCareer): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docPath = "users/${uid ?: "unauthenticated"}/careers/${career.id}"
        return try {
            val activeUid = requireUserId()
            require(career.id.isNotBlank()) { "Career ID must not be blank for saving progress." }
            val docRef = db.collection("users").document(activeUid)
                .collection("careers").document(career.id)
            docRef.update(career.toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCareer(careerId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docPath = "users/${uid ?: "unauthenticated"}/careers/$careerId"
        return try {
            val activeUid = requireUserId()
            require(careerId.isNotBlank()) { "Career ID must not be blank for deletion." }
            db.collection("users").document(activeUid)
                .collection("careers").document(careerId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordHallOfFameAndProfile(
        career: StudentCareer,
        careerTitle: String,
        score: Long,
        stageReached: String,
        cgpaDisplay: String,
        graduated: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val leaderboardPath = "leaderboard/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()

            // 1. Update UserProfile highest achievements
            val profileRef = db.collection("users").document(activeUid)
            val profileSnap = profileRef.get().await()
            if (profileSnap.exists()) {
                val existingHighestNet = profileSnap.getLong("highestNetWorth") ?: 0L
                val existingGrads = profileSnap.getLong("totalGraduations") ?: 0L
                val existingStage = profileSnap.getString("highestStage") ?: LifeStage.SECONDARY.name
                val existingTitle = profileSnap.getString("bestCareerTitle") ?: "Fresher"

                val newHighestStage = if (stageRank(stageReached) >= stageRank(existingStage)) {
                    stageReached
                } else {
                    existingStage
                }
                val newTitle = if (stageRank(stageReached) >= stageRank(existingStage)) {
                    careerTitle.take(80)
                } else {
                    existingTitle
                }

                profileRef.update(
                    mapOf(
                        "bestCareerTitle" to newTitle,
                        "highestNetWorth" to maxOf(existingHighestNet, career.money).coerceIn(-1_000_000L, 1_000_000_000L),
                        "highestStage" to newHighestStage,
                        "totalGraduations" to (existingGrads + if (graduated) 1L else 0L).coerceIn(0L, 10_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // 2. Update Leaderboard entry if this run beats their previous best score
            val lbRef = db.collection("leaderboard").document(activeUid)
            val lbSnap = lbRef.get().await()
            val entry = LeaderboardEntry(
                userId = activeUid,
                characterName = career.characterName,
                schoolName = career.schoolName,
                stageReached = stageReached,
                careerTitle = careerTitle,
                score = score,
                netWorth = career.money,
                cgpaDisplay = cgpaDisplay,
                visibility = "PUBLIC"
            )
            if (!lbSnap.exists()) {
                lbRef.set(entry.toCreateMap()).await()
            } else {
                val existingScore = lbSnap.getLong("score") ?: 0L
                if (score >= existingScore) {
                    lbRef.update(entry.toUpdateMap()).await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, leaderboardPath)
            Result.failure(e)
        }
    }

    private fun stageRank(stage: String): Int = when (stage) {
        "SECONDARY" -> 1
        "UNIVERSITY" -> 2
        "POSTGRADUATE" -> 3
        "LECTURER" -> 4
        "PROFESSOR" -> 5
        else -> 0
    }
}
