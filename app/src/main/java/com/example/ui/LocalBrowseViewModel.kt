package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SportsbookRepository
import com.example.data.ViralMediaStorage
import com.example.model.SportsbookApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Purely local ViewModel used for direct browsing when the user has not signed in with Google.
 * Holds zero Firebase/Firestore references so no unauthenticated queries can ever be executed.
 */
class LocalBrowseViewModel(
    private val localRepo: SportsbookRepository,
    private val platformActions: PlatformActions
) : ViewModel() {

    private val baseApps: List<SportsbookApp> = localRepo.getInitialApps()

    val allApps: StateFlow<List<SportsbookApp>> = MutableStateFlow(baseApps).asStateFlow()
    val savedIds: StateFlow<Set<String>> = localRepo.savedIds
    val customLinks: StateFlow<Map<String, String>> = localRepo.customLinks

    val appShareUrl: StateFlow<String> = localRepo.localAppShareUrl
        .map { saved -> saved.ifBlank { localRepo.defaultAppShareUrl() } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            localRepo.localAppShareUrl.value.ifBlank { localRepo.defaultAppShareUrl() }
        )

    private val _currentReelIndex = MutableStateFlow(0)
    val currentReelIndex: StateFlow<Int> = _currentReelIndex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _activeSheet = MutableStateFlow(SheetType.NONE)
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), baseApps)

    val savedApps: StateFlow<List<SportsbookApp>> = combine(
        savedIds,
        allApps
    ) { ids, list ->
        list.filter { ids.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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
        if (trimmed.isNotEmpty() && !ViralMediaStorage.isSafeHttpUrl(trimmed)) {
            showToast("Enter a valid https:// or http:// link.")
            return false
        }
        localRepo.saveLocalAppShareUrl(trimmed)
        showToast("App QR link updated!")
        return true
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
        showToast("${app.name} referral link copied!")
    }

    fun shareLink(app: SportsbookApp) {
        val link = getEffectiveLink(app)
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

    fun onTargetScrolled() {
        _targetScrollIndex.value = null
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
