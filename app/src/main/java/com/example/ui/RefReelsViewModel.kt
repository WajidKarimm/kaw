package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SportsbookRepository
import com.example.model.SportsbookApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SheetType {
    NONE,
    SEARCH,
    SAVED
}

class RefReelsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SportsbookRepository(application)

    val allApps: List<SportsbookApp> = repository.getInitialApps()

    val savedIds: StateFlow<Set<String>> = repository.savedIds
    val customLinks: StateFlow<Map<String, String>> = repository.customLinks

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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _targetScrollIndex = MutableStateFlow<Int?>(null)
    val targetScrollIndex: StateFlow<Int?> = _targetScrollIndex.asStateFlow()

    val filteredApps: StateFlow<List<SportsbookApp>> = combine(
        _searchQuery,
        _selectedTag
    ) { query, tag ->
        val q = query.trim().lowercase()
        allApps.filter { app ->
            val matchesQuery = q.isEmpty() ||
                    app.name.lowercase().contains(q) ||
                    app.payout.lowercase().contains(q) ||
                    app.badge.lowercase().contains(q) ||
                    app.description.lowercase().contains(q) ||
                    app.tags.any { it.lowercase().contains(q) }

            val matchesTag = tag == "All" ||
                    app.tags.any { it.equals(tag, ignoreCase = true) } ||
                    (tag == "US" && app.region == "US") ||
                    (tag == "Global" && app.region == "Global")

            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allApps)

    val savedApps: StateFlow<List<SportsbookApp>> = savedIds.combine(
        MutableStateFlow(allApps)
    ) { ids, list ->
        list.filter { ids.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCurrentIndex(index: Int) {
        if (index in allApps.indices) {
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

    fun getEffectiveLink(app: SportsbookApp): String {
        return repository.getEffectiveLink(app)
    }

    fun copyLink(app: SportsbookApp) {
        val link = getEffectiveLink(app)
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("${app.name} referral link", link)
        clipboard.setPrimaryClip(clip)
        showToast("${app.name} referral link copied!")
    }

    fun shareLink(app: SportsbookApp) {
        val link = getEffectiveLink(app)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Sign up for ${app.name} with my referral link and get rewarded (${app.payout}):\n$link"
            )
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "Share ${app.name} Referral").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(chooser)
    }

    fun toggleSave(app: SportsbookApp) {
        val isSaved = repository.toggleSave(app.id)
        if (isSaved) {
            showToast("${app.name} saved to bookmarks")
        } else {
            showToast("${app.name} removed from bookmarks")
        }
    }

    fun saveCustomLink(app: SportsbookApp, linkOrCode: String) {
        repository.setCustomLink(app.id, linkOrCode)
        closeEditDialog()
        showToast("Personal referral updated for ${app.name}")
    }

    fun jumpToApp(app: SportsbookApp) {
        val idx = allApps.indexOfFirst { it.id == app.id }
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
