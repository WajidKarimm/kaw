package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SubmissionStatus {
    PENDING_VERIFICATION,
    APPROVED,
    REJECTED;

    companion object {
        fun fromRaw(raw: String?): SubmissionStatus {
            return entries.firstOrNull { it.name == raw } ?: PENDING_VERIFICATION
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AppConfigDoc(
    @DocumentId val id: String = "",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val feePkr: Long = 100L,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isConfigured: Boolean
        get() = accountTitle.isNotBlank() && accountNumber.isNotBlank()
}

@Keep
@IgnoreExtraProperties
data class AppShareConfigDoc(
    @DocumentId val id: String = "",
    val downloadUrl: String = "",
    val shareMessage: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ViralSubmissionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val title: String = "",
    val referralLink: String = "",
    val promoCode: String = "",
    val bonusOffer: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val amountPkr: Long = 100L,
    val status: String = SubmissionStatus.PENDING_VERIFICATION.name,
    val adminNote: String = "",
    val viralShares: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: SubmissionStatus
        get() = SubmissionStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class DepositProofDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val submissionId: String = "",
    val easypaisaSenderPhone: String = "",
    val easypaisaTid: String = "",
    val amountPkr: Long = 100L,
    val status: String = SubmissionStatus.PENDING_VERIFICATION.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AdminModerationItem(
    val submission: ViralSubmissionDoc,
    val depositProof: DepositProofDoc?
)
