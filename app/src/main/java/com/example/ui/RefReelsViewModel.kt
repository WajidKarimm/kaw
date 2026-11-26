package com.example.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SportsbookRepository
import com.example.data.SportsbookRepository.Companion.toSportsbookApp
import com.example.data.ViralMediaStorage
import com.example.data.remote.AdminModerationItem
import com.example.data.remote.AppConfigDoc
import com.example.data.remote.AppShareConfigDoc
import com.example.data.remote.FirestoreViralRepository
import com.example.data.remote.ViralSubmissionDoc
import com.example.model.SportsbookApp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SheetType {
    NONE,
    SEARCH,
    SAVED,
    VIRAL
}

data class ViralSubmissionUiState(
    val step: Int = 1,
    val isDepositVerified: Boolean = false,
    val senderPhone: String = "",
    val transactionId: String = "",
    val receiptUri: Uri? = null,
    val title: String = "",
    val referralLink: String = "",
    val promoCode: String = "",
    val bonusOffer: String = "",
    val description: String = "",
    val pickedVideoUri: Uri? = null,
    val externalVideoUrl: String = "",
    val isSubmitting: Boolean = false,
    val isSavingMerchantConfig: Boolean = false,
    val moderatingSubmissionId: String? = null,
    val errorMessage: String? = null
)

class RefReelsViewModel(
    private val localRepo: SportsbookRepository,
    private val firestoreRepo: FirestoreViralRepository,
    private val platformActions: PlatformActions,
    private val currentUserId: String,
    private val authorName: String,
    val isAdmin: Boolean,
    initialSheet: SheetType = SheetType.NONE
) : ViewModel() {

    private companion object {
        const val TAG = "RefReelsVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val baseApps: List<SportsbookApp> = localRepo.getInitialApps()

    val easypaisaConfigState: StateFlow<UiState<AppConfigDoc?>> =
        firestoreRepo.observeEasypaisaConfig()
            .asUiState("Failed to load Easypaisa config")

    val appShareConfigState: StateFlow<UiState<AppShareConfigDoc?>> =
        firestoreRepo.observeAppShareConfig()
            .asUiState("Failed to load app share link")

    val appShareUrl: StateFlow<String> = combine(
        appShareConfigState,
        localRepo.localAppShareUrl
    ) { cloudState, localOverride ->
        val cloudUrl = (cloudState as? UiState.Success)?.data?.downloadUrl.orEmpty().trim()
        localOverride.ifBlank { cloudUrl }.ifBlank { localRepo.defaultAppShareUrl() }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        localRepo.localAppShareUrl.value.ifBlank { localRepo.defaultAppShareUrl() }
    )

    val approvedSubmissionsState: StateFlow<UiState<List<ViralSubmissionDoc>>> =
        firestoreRepo.observeApprovedSubmissions()
            .asUiState("Failed to load viral reels")

    val mySubmissionsState: StateFlow<UiState<List<ViralSubmissionDoc>>> =
        firestoreRepo.observeMySubmissions(currentUserId)
            .asUiState("Failed to load your submissions")

    val adminQueueState: StateFlow<UiState<List<AdminModerationItem>>> =
        if (isAdmin) {
            combine(
                firestoreRepo.observePendingSubmissionsForAdmin(),
                firestoreRepo.observePendingDepositProofsForAdmin()
            ) { submissions, proofs ->
                val proofBySubId = proofs.associateBy { it.submissionId }
                submissions.map { submission ->
                    AdminModerationItem(
                        submission = submission,
                        depositProof = proofBySubId[submission.id]
                    )
                }
            }.asUiState("Failed to load pending admin approvals")
        } else {
            MutableStateFlow<UiState<List<AdminModerationItem>>>(UiState.Success(emptyList()))
                .asStateFlow()
        }

    val allApps: StateFlow<List<SportsbookApp>> = combine(
        approvedSubmissionsState,
        localRepo.localVideoMap
    ) { state, videoMap ->
        val approved = (state as? UiState.Success)?.data.orEmpty()
        approved.map { sub ->
            sub.toSportsbookApp(localVideoOverride = videoMap[sub.id].orEmpty())
        } + baseApps
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), baseApps)

    val savedIds: StateFlow<Set<String>> = localRepo.savedIds
    val customLinks: StateFlow<Map<String, String>> = localRepo.customLinks

    private val _viralUiState = MutableStateFlow(ViralSubmissionUiState())
    val viralUiState: StateFlow<ViralSubmissionUiState> = _viralUiState.asStateFlow()

    private val _currentReelIndex = MutableStateFlow(0)
    val currentReelIndex: StateFlow<Int> = _currentReelIndex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _activeSheet = MutableStateFlow(initialSheet)
    val activeSheet: StateFlow<SheetType> = _activeSheet.asStateFlow()

    private val _qrDialogApp = MutableStateFlow<SportsbookApp?>(null)
    val qrDialogApp: StateFlow<SportsbookApp?> = _qrDialogApp.asStateFlow()

    private val _editDialogApp = MutableStateFlow<SportsbookApp?>(null)
    val editDialogApp: StateFlow<SportsbookApp?> = _editDialogApp.asStateFlow()

    private val _showShareAppDialog = MutableStateFlow(false)
    val showShareAppDialog: StateFlow<Boolean> = _showShareAppDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _targetScrollIndex = MutableStateFlow<Int?>(null)
    val targetScrollIndex: StateFlow<Int?> = _targetScrollIndex.asStateFlow()

    val filteredApps: StateFlow<List<SportsbookApp>> = combine(
        allApps,
        _searchQuery,
        _selectedTag
    ) { apps, query, tag ->
        val q = query.trim().lowercase()
        apps.filter { app ->
            val matchesQuery = q.isEmpty() ||
                app.name.lowercase().contains(q) ||
                app.payout.lowercase().contains(q) ||
                app.badge.lowercase().contains(q) ||
                app.description.lowercase().contains(q) ||
                app.termsHighlight.lowercase().contains(q) ||
                app.tags.any { it.lowercase().contains(q) }

            val matchesTag = tag == "All" ||
                app.name.contains(tag, ignoreCase = true) ||
                app.tags.any { it.equals(tag, ignoreCase = true) } ||
                (tag == "US" && app.region == "US") ||
                (tag == "Global" && app.region == "Global")

            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), baseApps)

    val savedApps: StateFlow<List<SportsbookApp>> = combine(
        savedIds,
        allApps
    ) { ids, list ->
        list.filter { ids.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private fun <T> Flow<T>.asUiState(fallbackMessage: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackMessage, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    fun updateCurrentIndex(index: Int) {
        if (index in allApps.value.indices) {
            _currentReelIndex.value = index
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String) {
        _selectedTag.value = tag
    }

    fun openSheet(sheet: SheetType) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = SheetType.NONE
    }

    fun openQrDialog(app: SportsbookApp) {
        _qrDialogApp.value = app
    }

    fun closeQrDialog() {
        _qrDialogApp.value = null
    }

    fun openEditDialog(app: SportsbookApp) {
        _editDialogApp.value = app
    }

    fun closeEditDialog() {
        _editDialogApp.value = null
    }

    fun openShareAppDialog() {
        _showShareAppDialog.value = true
    }

    fun closeShareAppDialog() {
        _showShareAppDialog.value = false
    }

    fun saveAppShareUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (isAdmin) {
            if (trimmed.length !in 10..500 || !ViralMediaStorage.isSafeHttpUrl(trimmed)) {
                showToast("Enter a valid https:// or http:// APK download or store link.")
                return false
            }
            viewModelScope.launch {
                val result = firestoreRepo.saveAppShareConfig(trimmed)
                result.fold(
                    onSuccess = {
                        localRepo.saveLocalAppShareUrl("")
                        showToast("Cloud App QR link updated for all users!")
                    },
                    onFailure = { error ->
                        showToast(error.localizedMessage ?: "Failed to update cloud App QR link.")
                    }
                )
            }
            return true
        } else {
            if (trimmed.isNotEmpty() && (trimmed.length > 500 || !ViralMediaStorage.isSafeHttpUrl(trimmed))) {
                showToast("Enter a valid https:// or http:// link.")
                return false
            }
            localRepo.saveLocalAppShareUrl(trimmed)
            showToast(if (trimmed.isEmpty()) "App QR link reset to default." else "App QR link updated!")
            return true
        }
    }

    fun shareAppQrToGroups() {
        viewModelScope.launch {
            val result = platformActions.shareAppQrImageToGroups(appShareUrl.value)
            result.onFailure {
                showToast("Could not prepare QR image for sharing.")
            }
        }
    }

    fun shareAppApkToGroups() {
        viewModelScope.launch {
            val result = platformActions.shareAppApkToGroups()
            result.onFailure {
                showToast("Could not prepare app APK file.")
            }
        }
    }

    fun shareAppLinkToGroups() {
        val url = appShareUrl.value
        platformActions.shareReferralText(
            chooserTitle = "Share RefReels App to Groups",
            shareBody = "Check out RefReels — discover verified referral links, QR codes & viral promo videos:\n$url"
        )
    }

    fun copyAppShareLink() {
        platformActions.copyToClipboard("RefReels App Link", appShareUrl.value)
        showToast("App download link copied!")
    }

    fun getEffectiveLink(app: SportsbookApp): String {
        return localRepo.getEffectiveLink(app)
    }

    fun copyLink(app: SportsbookApp) {
        val link = getEffectiveLink(app)
        platformActions.copyToClipboard("${app.name} referral link", link)
        if (app.isUserViral) {
            viewModelScope.launch {
                firestoreRepo.incrementViralShare(app.id)
            }
        }
        showToast("${app.name} referral link copied!")
    }

    fun shareLink(app: SportsbookApp) {
        val link = getEffectiveLink(app)
        if (app.isUserViral) {
            viewModelScope.launch {
                firestoreRepo.incrementViralShare(app.id)
            }
        }
        platformActions.shareReferralText(
            chooserTitle = "Share ${app.name} Referral",
            shareBody = "Join ${app.name} with this referral link (${app.payout}):\n$link"
        )
    }

    fun toggleSave(app: SportsbookApp) {
        val isSaved = localRepo.toggleSave(app.id)
        if (isSaved) {
            showToast("${app.name} saved to bookmarks")
        } else {
            showToast("${app.name} removed from bookmarks")
        }
    }

    fun saveCustomLink(app: SportsbookApp, linkOrCode: String) {
        localRepo.setCustomLink(app.id, linkOrCode)
        closeEditDialog()
        if (linkOrCode.isBlank()) {
            showToast("Reset ${app.name} to official page")
        } else {
            showToast("Personal referral updated for ${app.name}")
        }
    }

    fun jumpToApp(app: SportsbookApp) {
        val idx = allApps.value.indexOfFirst { it.id == app.id }
        if (idx >= 0) {
            _targetScrollIndex.value = idx
            closeSheet()
        }
    }

    fun jumpToSubmission(submission: ViralSubmissionDoc) {
        val idx = allApps.value.indexOfFirst { it.id == submission.id }
        if (idx >= 0) {
            _targetScrollIndex.value = idx
            closeSheet()
        } else {
            showToast("This submission is waiting for admin approval before going live.")
        }
    }

    fun onTargetScrolled() {
        _targetScrollIndex.value = null
    }

    fun saveEasypaisaMerchantAccount(title: String, number: String, onSaved: () -> Unit = {}) {
        if (!isAdmin) {
            _viralUiState.update { it.copy(errorMessage = "Only the admin can edit the receiving Easypaisa number.") }
            return
        }
        if (title.trim().length < 2 || title.trim().length > 60) {
            _viralUiState.update { it.copy(errorMessage = "Enter a valid Easypaisa account title (2–60 characters).") }
            return
        }
        if (!ViralMediaStorage.isValidPakistaniPhone(number)) {
            _viralUiState.update { it.copy(errorMessage = "Enter a valid Pakistani mobile number (03XXXXXXXXX).") }
            return
        }
        viewModelScope.launch {
            _viralUiState.update { it.copy(isSavingMerchantConfig = true, errorMessage = null) }
            val result = firestoreRepo.saveEasypaisaConfig(title, number)
            result.fold(
                onSuccess = {
                    _viralUiState.update { it.copy(isSavingMerchantConfig = false, errorMessage = null) }
                    showToast("Easypaisa receiving account saved to cloud!")
                    onSaved()
                },
                onFailure = { error ->
                    _viralUiState.update {
                        it.copy(
                            isSavingMerchantConfig = false,
                            errorMessage = error.localizedMessage ?: "Failed to save Easypaisa account."
                        )
                    }
                }
            )
        }
    }

    fun copyMerchantNumber() {
        val config = (easypaisaConfigState.value as? UiState.Success)?.data
        val number = config?.accountNumber.orEmpty()
        if (number.isBlank()) {
            showToast("Receiving Easypaisa number is not configured yet.")
            return
        }
        platformActions.copyToClipboard("Easypaisa Number", number)
        showToast("Easypaisa number $number copied!")
    }

    fun openEasypaisaApp() {
        val launched = platformActions.openEasypaisaApp()
        if (!launched) {
            showToast("Easypaisa app not installed — send 100 PKR to the number above and enter TID.")
        }
    }

    fun updateViralField(transform: (ViralSubmissionUiState) -> ViralSubmissionUiState) {
        _viralUiState.update { current ->
            transform(current).copy(errorMessage = null)
        }
    }

    fun verifyEasypaisaDepositAndUnlock() {
        val state = _viralUiState.value
        val merchant = (easypaisaConfigState.value as? UiState.Success)?.data
        if (merchant == null || !merchant.isConfigured) {
            _viralUiState.update {
                it.copy(
                    errorMessage = if (isAdmin) {
                        "Set up the receiving Easypaisa account above before submitting."
                    } else {
                        "Admin has not configured the receiving Easypaisa number yet. Please check back soon."
                    }
                )
            }
            return
        }
        if (!ViralMediaStorage.isValidPakistaniPhone(state.senderPhone)) {
            _viralUiState.update {
                it.copy(errorMessage = "Enter the Easypaisa sender mobile number (e.g., 03001234567).")
            }
            return
        }
        val cleanTid = state.transactionId.trim().uppercase()
        if (!ViralMediaStorage.isValidEasypaisaTid(cleanTid)) {
            _viralUiState.update {
                it.copy(errorMessage = "Enter a valid Easypaisa Transaction ID (TID, 6–24 characters).")
            }
            return
        }

        _viralUiState.update {
            it.copy(
                isDepositVerified = true,
                step = 2,
                errorMessage = null
            )
        }
        showToast("Deposit details ready! Now add your referral link & video.")
    }

    fun publishViralReel() {
        val state = _viralUiState.value
        if (!state.isDepositVerified) {
            _viralUiState.update {
                it.copy(step = 1, errorMessage = "Enter your 100 PKR Easypaisa deposit details first.")
            }
            return
        }
        val title = state.title.trim()
        if (title.length < 2 || title.length > 60) {
            _viralUiState.update { it.copy(errorMessage = "Enter an offer/app name (2–60 characters).") }
            return
        }
        val refLink = state.referralLink.trim()
        if (refLink.length < 10 || refLink.length > 500 || !ViralMediaStorage.isSafeHttpUrl(refLink)) {
            _viralUiState.update {
                it.copy(errorMessage = "Enter a valid https:// or http:// referral URL (10–500 chars).")
            }
            return
        }
        val extVideo = state.externalVideoUrl.trim()
        if (state.pickedVideoUri == null && extVideo.isBlank()) {
            _viralUiState.update {
                it.copy(errorMessage = "Pick a promo video from your gallery or paste an HTTPS video URL.")
            }
            return
        }
        if (extVideo.isNotBlank() && (extVideo.length > 500 || !ViralMediaStorage.isSafeHttpUrl(extVideo))) {
            _viralUiState.update {
                it.copy(errorMessage = "External video URL must be a valid https:// or http:// URL.")
            }
            return
        }

        viewModelScope.launch {
            _viralUiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val localVideoPath = if (state.pickedVideoUri != null) {
                platformActions.copyPickedVideoToInternal(state.pickedVideoUri)
            } else {
                null
            }

            if (state.pickedVideoUri != null && localVideoPath.isNullOrBlank()) {
                _viralUiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Could not read the selected video. Please try another video."
                    )
                }
                return@launch
            }

            val result = firestoreRepo.submitViralEntry(
                authorName = authorName,
                title = title,
                referralLink = refLink,
                promoCode = state.promoCode,
                bonusOffer = state.bonusOffer,
                description = state.description,
                videoUrl = extVideo,
                senderPhone = state.senderPhone,
                transactionId = state.transactionId
            )

            result.fold(
                onSuccess = { submissionId ->
                    if (!localVideoPath.isNullOrBlank()) {
                        localRepo.saveLocalVideoMapping(submissionId, localVideoPath)
                    }
                    _viralUiState.value = ViralSubmissionUiState()
                    showToast("Submitted! Admin will verify your 100 PKR TID before it goes live.")
                },
                onFailure = { error ->
                    if (!localVideoPath.isNullOrBlank()) {
                        platformActions.deleteLocalFile(localVideoPath)
                    }
                    val firestoreEx = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    val friendlyMessage = when (firestoreEx?.code) {
                        FirebaseFirestoreException.Code.ALREADY_EXISTS,
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                            "This Easypaisa Transaction ID was already used or could not be verified. Send a fresh 100 PKR deposit and use a new TID."
                        else -> error.localizedMessage ?: "Failed to submit viral offer."
                    }
                    _viralUiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = friendlyMessage
                        )
                    }
                }
            )
        }
    }

    fun approveSubmission(submissionId: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            _viralUiState.update { it.copy(moderatingSubmissionId = submissionId, errorMessage = null) }
            val result = firestoreRepo.moderateSubmission(submissionId, approve = true)
            result.fold(
                onSuccess = {
                    _viralUiState.update { it.copy(moderatingSubmissionId = null) }
                    showToast("Approved! Offer is now live for all users.")
                },
                onFailure = { error ->
                    _viralUiState.update {
                        it.copy(
                            moderatingSubmissionId = null,
                            errorMessage = error.localizedMessage ?: "Failed to approve submission."
                        )
                    }
                }
            )
        }
    }

    fun rejectSubmission(submissionId: String, reason: String = "Deposit could not be verified") {
        if (!isAdmin) return
        viewModelScope.launch {
            _viralUiState.update { it.copy(moderatingSubmissionId = submissionId, errorMessage = null) }
            val result = firestoreRepo.moderateSubmission(submissionId, approve = false, adminNote = reason)
            result.fold(
                onSuccess = {
                    _viralUiState.update { it.copy(moderatingSubmissionId = null) }
                    showToast("Submission rejected.")
                },
                onFailure = { error ->
                    _viralUiState.update {
                        it.copy(
                            moderatingSubmissionId = null,
                            errorMessage = error.localizedMessage ?: "Failed to reject submission."
                        )
                    }
                }
            )
        }
    }

    fun deleteSubmission(submission: ViralSubmissionDoc) {
        viewModelScope.launch {
            val result = firestoreRepo.deleteSubmission(submission.id)
            result.fold(
                onSuccess = {
                    localRepo.removeLocalVideoMapping(submission.id)
                    showToast("Removed ${submission.title}")
                },
                onFailure = { error ->
                    showToast(error.localizedMessage ?: "Could not delete submission.")
                }
            )
        }
    }

    fun goBackToDepositStep() {
        _viralUiState.update { it.copy(step = 1, errorMessage = null) }
    }

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.value = msg
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
