package com.example.data.remote

import com.example.data.ViralMediaStorage
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreViralRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    companion object {
        const val ADMIN_EMAIL = "karimwajid0101@gmail.com"
        private const val COLL_APP_CONFIG = "app_config"
        private const val DOC_EASYPAISA = "easypaisa"
        private const val DOC_APP_SHARE = "app_share"
        private const val COLL_SUBMISSIONS = "viral_submissions"
        private const val COLL_PROOFS = "deposit_proofs"
        private const val COLL_TID_REGISTRY = "tid_registry"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(ADMIN_EMAIL, ignoreCase = false) && user.isEmailVerified
    }

    fun observeEasypaisaConfig(): Flow<AppConfigDoc?> {
        val path = "$COLL_APP_CONFIG/$DOC_EASYPAISA"
        return db.collection(COLL_APP_CONFIG).document(DOC_EASYPAISA)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(AppConfigDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeAppShareConfig(): Flow<AppShareConfigDoc?> {
        val path = "$COLL_APP_CONFIG/$DOC_APP_SHARE"
        return db.collection(COLL_APP_CONFIG).document(DOC_APP_SHARE)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(AppShareConfigDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeApprovedSubmissions(): Flow<List<ViralSubmissionDoc>> {
        return db.collection(COLL_SUBMISSIONS)
            .whereEqualTo("status", SubmissionStatus.APPROVED.name)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ViralSubmissionDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLL_SUBMISSIONS)
                }
                throw error
            }
    }

    fun observeMySubmissions(userId: String): Flow<List<ViralSubmissionDoc>> {
        return db.collection(COLL_SUBMISSIONS)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ViralSubmissionDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLL_SUBMISSIONS)
                }
                throw error
            }
    }

    fun observePendingSubmissionsForAdmin(): Flow<List<ViralSubmissionDoc>> {
        return db.collection(COLL_SUBMISSIONS)
            .whereEqualTo("status", SubmissionStatus.PENDING_VERIFICATION.name)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ViralSubmissionDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (isCurrentUserAdmin()) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLL_SUBMISSIONS)
                }
                throw error
            }
    }

    fun observePendingDepositProofsForAdmin(): Flow<List<DepositProofDoc>> {
        return db.collection(COLL_PROOFS)
            .whereEqualTo("status", SubmissionStatus.PENDING_VERIFICATION.name)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DepositProofDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (isCurrentUserAdmin()) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLL_PROOFS)
                }
                throw error
            }
    }

    suspend fun saveEasypaisaConfig(accountTitle: String, accountNumber: String): Result<Unit> {
        val path = "$COLL_APP_CONFIG/$DOC_EASYPAISA"
        return try {
            val uid = requireUserId()
            check(isCurrentUserAdmin()) { "Only the verified admin can update Easypaisa account settings." }
            val cleanTitle = accountTitle.trim()
            val cleanNumber = ViralMediaStorage.normalizePakistaniPhone(accountNumber)
            require(cleanTitle.length in 2..60) { "Account title must be 2–60 characters." }
            require(Regex("^03\\d{9}$").matches(cleanNumber)) { "Enter a valid 11-digit Easypaisa number (03XXXXXXXXX)." }

            val payload = mapOf(
                "accountTitle" to cleanTitle,
                "accountNumber" to cleanNumber,
                "feePkr" to 100L,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COLL_APP_CONFIG).document(DOC_EASYPAISA).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAppShareConfig(downloadUrl: String, shareMessage: String = ""): Result<Unit> {
        val path = "$COLL_APP_CONFIG/$DOC_APP_SHARE"
        return try {
            val uid = requireUserId()
            check(isCurrentUserAdmin()) { "Only the verified admin can update the cloud app download link." }
            val cleanUrl = downloadUrl.trim()
            val cleanMsg = shareMessage.trim().take(200)
            require(cleanUrl.length in 10..500 && ViralMediaStorage.isSafeHttpUrl(cleanUrl)) {
                "Enter a valid https:// or http:// APK download or store link."
            }

            val payload = mutableMapOf<String, Any>(
                "downloadUrl" to cleanUrl,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanMsg.isNotEmpty()) {
                payload["shareMessage"] = cleanMsg
            }
            db.collection(COLL_APP_CONFIG).document(DOC_APP_SHARE).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitViralEntry(
        authorName: String,
        title: String,
        referralLink: String,
        promoCode: String,
        bonusOffer: String,
        description: String,
        videoUrl: String,
        senderPhone: String,
        transactionId: String
    ): Result<String> {
        val submissionId = "sub_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "$COLL_SUBMISSIONS/$submissionId"
        return try {
            val uid = requireUserId()
            val cleanAuthor = authorName.trim().ifBlank { "Creator" }.take(60)
            val cleanTitle = title.trim()
            val cleanLink = referralLink.trim()
            val cleanPromo = promoCode.trim().take(40)
            val cleanBonus = bonusOffer.trim().ifBlank { "Viral Bonus" }.take(50)
            val cleanDesc = description.trim().take(300)
            val cleanVideoUrl = videoUrl.trim()
            val cleanPhone = ViralMediaStorage.normalizePakistaniPhone(senderPhone)
            val cleanTid = transactionId.trim().uppercase()

            require(cleanTitle.length in 2..60) { "Title must be 2–60 characters." }
            require(cleanLink.length in 10..500 && ViralMediaStorage.isSafeHttpUrl(cleanLink)) {
                "Enter a valid https:// or http:// referral link."
            }
            require(cleanVideoUrl.isEmpty() || (cleanVideoUrl.length <= 500 && ViralMediaStorage.isSafeHttpUrl(cleanVideoUrl))) {
                "External video URL must be a valid https:// or http:// link."
            }
            require(Regex("^03\\d{9}$").matches(cleanPhone)) {
                "Enter a valid 11-digit Easypaisa sender number (03XXXXXXXXX)."
            }
            require(Regex("^[A-Z0-9\\-]{6,24}$").matches(cleanTid)) {
                "Transaction ID must be 6–24 alphanumeric characters."
            }

            val submissionRef = db.collection(COLL_SUBMISSIONS).document(submissionId)
            val proofRef = db.collection(COLL_PROOFS).document(submissionId)
            val tidRef = db.collection(COLL_TID_REGISTRY).document(cleanTid)

            val submissionPayload = mapOf(
                "userId" to uid,
                "authorName" to cleanAuthor,
                "title" to cleanTitle,
                "referralLink" to cleanLink,
                "promoCode" to cleanPromo,
                "bonusOffer" to cleanBonus,
                "description" to cleanDesc,
                "videoUrl" to cleanVideoUrl,
                "amountPkr" to 100L,
                "status" to SubmissionStatus.PENDING_VERIFICATION.name,
                "viralShares" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val proofPayload = mapOf(
                "userId" to uid,
                "submissionId" to submissionId,
                "easypaisaSenderPhone" to cleanPhone,
                "easypaisaTid" to cleanTid,
                "amountPkr" to 100L,
                "status" to SubmissionStatus.PENDING_VERIFICATION.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val tidPayload = mapOf(
                "userId" to uid,
                "submissionId" to submissionId,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(submissionRef, submissionPayload)
                batch.set(proofRef, proofPayload)
                batch.set(tidRef, tidPayload)
            }.await()

            Result.success(submissionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun moderateSubmission(
        submissionId: String,
        approve: Boolean,
        adminNote: String = ""
    ): Result<Unit> {
        val path = "$COLL_SUBMISSIONS/$submissionId"
        return try {
            requireUserId()
            check(isCurrentUserAdmin()) { "Only the verified admin can approve or reject deposits." }
            require(submissionId.isNotBlank()) { "Submission ID must not be blank." }

            val newStatus = if (approve) {
                SubmissionStatus.APPROVED.name
            } else {
                SubmissionStatus.REJECTED.name
            }
            val cleanNote = adminNote.trim().take(200)

            val submissionRef = db.collection(COLL_SUBMISSIONS).document(submissionId)
            val proofRef = db.collection(COLL_PROOFS).document(submissionId)

            db.runBatch { batch ->
                batch.update(
                    submissionRef,
                    mapOf(
                        "status" to newStatus,
                        "adminNote" to cleanNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    proofRef,
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementViralShare(submissionId: String): Result<Unit> {
        val path = "$COLL_SUBMISSIONS/$submissionId"
        return try {
            requireUserId()
            require(submissionId.isNotBlank()) { "Submission ID must not be blank." }
            db.collection(COLL_SUBMISSIONS).document(submissionId)
                .update(
                    mapOf(
                        "viralShares" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        val path = "$COLL_SUBMISSIONS/$submissionId"
        return try {
            requireUserId()
            require(submissionId.isNotBlank()) { "Submission ID must not be blank." }
            db.runBatch { batch ->
                batch.delete(db.collection(COLL_SUBMISSIONS).document(submissionId))
                batch.delete(db.collection(COLL_PROOFS).document(submissionId))
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
