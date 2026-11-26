package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SportsbookRepository
import com.example.data.remote.FirestoreViralRepository
import com.example.model.SportsbookApp
import com.example.ui.LocalBrowseViewModel
import com.example.ui.PlatformActions
import com.example.ui.RefReelsViewModel
import com.example.ui.SheetType
import com.example.ui.components.AuthScreen
import com.example.ui.components.CustomCodeDialog
import com.example.ui.components.QrCodeDialog
import com.example.ui.components.ReelItemView
import com.example.ui.components.RefReelsBottomNav
import com.example.ui.components.RefReelsToast
import com.example.ui.components.RefReelsTopBar
import com.example.ui.components.SavedSheet
import com.example.ui.components.SearchSheet
import com.example.ui.components.ShareAppDialog
import com.example.ui.components.ViralUploadSheet
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.components.signOutUser
import com.example.ui.theme.BgDark
import com.example.ui.theme.RefReelsTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RefReelsTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    var openViralAfterSignIn by remember { mutableStateOf(false) }

    // Silently restore previously authorized Google session in the background without blocking startup.
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    if (user == null) {
        val localViewModel: LocalBrowseViewModel = viewModel(
            key = "local_direct_browse",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    LocalBrowseViewModel(
                        localRepo = SportsbookRepository(app),
                        platformActions = PlatformActions(app)
                    )
                }
            }
        )
        DirectBrowseApp(
            viewModel = localViewModel,
            onViralSignInSuccess = { openViralAfterSignIn = true }
        )
    } else {
        key(user.uid) {
            val isAdmin = user.email == FirestoreViralRepository.ADMIN_EMAIL && user.isEmailVerified
            val authorName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Creator"
            val initialSheet = if (openViralAfterSignIn) SheetType.VIRAL else SheetType.NONE
            LaunchedEffect(user.uid) {
                if (openViralAfterSignIn) {
                    openViralAfterSignIn = false
                }
            }

            val viewModel: RefReelsViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        RefReelsViewModel(
                            localRepo = SportsbookRepository(app),
                            firestoreRepo = FirestoreViralRepository(db),
                            platformActions = PlatformActions(app),
                            currentUserId = user.uid,
                            authorName = authorName,
                            isAdmin = isAdmin,
                            initialSheet = initialSheet
                        )
                    }
                }
            )

            RefReelsApp(
                viewModel = viewModel,
                onSignOut = {
                    openViralAfterSignIn = false
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@Composable
fun DirectBrowseApp(
    viewModel: LocalBrowseViewModel,
    onViralSignInSuccess: () -> Unit
) {
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val customLinks by viewModel.customLinks.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val savedApps by viewModel.savedApps.collectAsStateWithLifecycle()
    val qrApp by viewModel.qrDialogApp.collectAsStateWithLifecycle()
    val editApp by viewModel.editDialogApp.collectAsStateWithLifecycle()
    val showShareAppDialog by viewModel.showShareAppDialog.collectAsStateWithLifecycle()
    val appShareUrl by viewModel.appShareUrl.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val targetScrollIndex by viewModel.targetScrollIndex.collectAsStateWithLifecycle()

    RefReelsFeedScaffold(
        allApps = allApps,
        savedIds = savedIds,
        customLinks = customLinks,
        activeSheet = activeSheet,
        searchQuery = searchQuery,
        selectedTag = selectedTag,
        filteredApps = filteredApps,
        savedApps = savedApps,
        qrApp = qrApp,
        editApp = editApp,
        isShareAppDialogOpen = showShareAppDialog,
        toastMessage = toastMessage,
        targetScrollIndex = targetScrollIndex,
        isAdmin = false,
        onSignOut = null,
        getEffectiveLink = viewModel::getEffectiveLink,
        onUpdateCurrentIndex = viewModel::updateCurrentIndex,
        onTargetScrolled = viewModel::onTargetScrolled,
        onClearToast = viewModel::clearToast,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
        onSearchQueryChange = viewModel::setSearchQuery,
        onTagSelect = viewModel::setSelectedTag,
        onJumpToApp = viewModel::jumpToApp,
        onCopy = viewModel::copyLink,
        onShare = viewModel::shareLink,
        onToggleSave = viewModel::toggleSave,
        onOpenQr = viewModel::openQrDialog,
        onCloseQr = viewModel::closeQrDialog,
        onOpenEdit = viewModel::openEditDialog,
        onCloseEdit = viewModel::closeEditDialog,
        onOpenShareApp = viewModel::openShareAppDialog,
        onCloseShareApp = viewModel::closeShareAppDialog,
        onSaveCustomLink = viewModel::saveCustomLink,
        shareAppDialogContent = {
            ShareAppDialog(
                appDownloadUrl = appShareUrl,
                isAdmin = false,
                onDismiss = viewModel::closeShareAppDialog,
                onShareQrToGroups = viewModel::shareAppQrToGroups,
                onShareApkToGroups = viewModel::shareAppApkToGroups,
                onShareLinkToGroups = viewModel::shareAppLinkToGroups,
                onCopyAppLink = viewModel::copyAppShareLink,
                onSaveAppLink = viewModel::saveAppShareUrl
            )
        },
        viralSheetContent = {
            AuthScreen(
                onAuthSuccess = onViralSignInSuccess,
                onClose = { viewModel.closeSheet() },
                autoAttemptSignIn = false
            )
        }
    )
}

@Composable
fun RefReelsApp(
    viewModel: RefReelsViewModel,
    onSignOut: () -> Unit
) {
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val easypaisaConfigState by viewModel.easypaisaConfigState.collectAsStateWithLifecycle()
    val mySubmissionsState by viewModel.mySubmissionsState.collectAsStateWithLifecycle()
    val adminQueueState by viewModel.adminQueueState.collectAsStateWithLifecycle()
    val viralUiState by viewModel.viralUiState.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val customLinks by viewModel.customLinks.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val savedApps by viewModel.savedApps.collectAsStateWithLifecycle()
    val qrApp by viewModel.qrDialogApp.collectAsStateWithLifecycle()
    val editApp by viewModel.editDialogApp.collectAsStateWithLifecycle()
    val showShareAppDialog by viewModel.showShareAppDialog.collectAsStateWithLifecycle()
    val appShareUrl by viewModel.appShareUrl.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val targetScrollIndex by viewModel.targetScrollIndex.collectAsStateWithLifecycle()

    RefReelsFeedScaffold(
        allApps = allApps,
        savedIds = savedIds,
        customLinks = customLinks,
        activeSheet = activeSheet,
        searchQuery = searchQuery,
        selectedTag = selectedTag,
        filteredApps = filteredApps,
        savedApps = savedApps,
        qrApp = qrApp,
        editApp = editApp,
        isShareAppDialogOpen = showShareAppDialog,
        toastMessage = toastMessage,
        targetScrollIndex = targetScrollIndex,
        isAdmin = viewModel.isAdmin,
        onSignOut = onSignOut,
        getEffectiveLink = viewModel::getEffectiveLink,
        onUpdateCurrentIndex = viewModel::updateCurrentIndex,
        onTargetScrolled = viewModel::onTargetScrolled,
        onClearToast = viewModel::clearToast,
        onOpenSheet = viewModel::openSheet,
        onCloseSheet = viewModel::closeSheet,
        onSearchQueryChange = viewModel::setSearchQuery,
        onTagSelect = viewModel::setSelectedTag,
        onJumpToApp = viewModel::jumpToApp,
        onCopy = viewModel::copyLink,
        onShare = viewModel::shareLink,
        onToggleSave = viewModel::toggleSave,
        onOpenQr = viewModel::openQrDialog,
        onCloseQr = viewModel::closeQrDialog,
        onOpenEdit = viewModel::openEditDialog,
        onCloseEdit = viewModel::closeEditDialog,
        onOpenShareApp = viewModel::openShareAppDialog,
        onCloseShareApp = viewModel::closeShareAppDialog,
        onSaveCustomLink = viewModel::saveCustomLink,
        shareAppDialogContent = {
            ShareAppDialog(
                appDownloadUrl = appShareUrl,
                isAdmin = viewModel.isAdmin,
                onDismiss = viewModel::closeShareAppDialog,
                onShareQrToGroups = viewModel::shareAppQrToGroups,
                onShareApkToGroups = viewModel::shareAppApkToGroups,
                onShareLinkToGroups = viewModel::shareAppLinkToGroups,
                onCopyAppLink = viewModel::copyAppShareLink,
                onSaveAppLink = viewModel::saveAppShareUrl
            )
        },
        viralSheetContent = {
            ViralUploadSheet(
                isAdmin = viewModel.isAdmin,
                uiState = viralUiState,
                easypaisaConfigState = easypaisaConfigState,
                mySubmissionsState = mySubmissionsState,
                adminQueueState = adminQueueState,
                onUpdateState = { viewModel.updateViralField(it) },
                onSaveMerchantConfig = { title, number, onSaved ->
                    viewModel.saveEasypaisaMerchantAccount(title, number, onSaved)
                },
                onCopyMerchantNumber = { viewModel.copyMerchantNumber() },
                onOpenEasypaisa = { viewModel.openEasypaisaApp() },
                onVerifyDeposit = { viewModel.verifyEasypaisaDepositAndUnlock() },
                onPublishViralReel = { viewModel.publishViralReel() },
                onBackToDepositStep = { viewModel.goBackToDepositStep() },
                onApproveSubmission = { id -> viewModel.approveSubmission(id) },
                onRejectSubmission = { id, note -> viewModel.rejectSubmission(id, note) },
                onJumpToSubmission = { sub -> viewModel.jumpToSubmission(sub) },
                onDeleteSubmission = { sub -> viewModel.deleteSubmission(sub) },
                onClose = { viewModel.closeSheet() }
            )
        }
    )
}

@Composable
private fun RefReelsFeedScaffold(
    allApps: List<SportsbookApp>,
    savedIds: Set<String>,
    customLinks: Map<String, String>,
    activeSheet: SheetType,
    searchQuery: String,
    selectedTag: String,
    filteredApps: List<SportsbookApp>,
    savedApps: List<SportsbookApp>,
    qrApp: SportsbookApp?,
    editApp: SportsbookApp?,
    isShareAppDialogOpen: Boolean,
    toastMessage: String?,
    targetScrollIndex: Int?,
    isAdmin: Boolean,
    onSignOut: (() -> Unit)?,
    getEffectiveLink: (SportsbookApp) -> String,
    onUpdateCurrentIndex: (Int) -> Unit,
    onTargetScrolled: () -> Unit,
    onClearToast: () -> Unit,
    onOpenSheet: (SheetType) -> Unit,
    onCloseSheet: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTagSelect: (String) -> Unit,
    onJumpToApp: (SportsbookApp) -> Unit,
    onCopy: (SportsbookApp) -> Unit,
    onShare: (SportsbookApp) -> Unit,
    onToggleSave: (SportsbookApp) -> Unit,
    onOpenQr: (SportsbookApp) -> Unit,
    onCloseQr: () -> Unit,
    onOpenEdit: (SportsbookApp) -> Unit,
    onCloseEdit: () -> Unit,
    onOpenShareApp: () -> Unit,
    onCloseShareApp: () -> Unit,
    onSaveCustomLink: (SportsbookApp, String) -> Unit,
    shareAppDialogContent: @Composable () -> Unit,
    viralSheetContent: @Composable () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { allApps.size }
    )

    BackHandler(enabled = activeSheet != SheetType.NONE || qrApp != null || editApp != null || isShareAppDialogOpen) {
        if (isShareAppDialogOpen) {
            onCloseShareApp()
        } else if (qrApp != null) {
            onCloseQr()
        } else if (editApp != null) {
            onCloseEdit()
        } else if (activeSheet != SheetType.NONE) {
            onCloseSheet()
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2400)
            onClearToast()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onUpdateCurrentIndex(page)
        }
    }

    LaunchedEffect(targetScrollIndex, allApps.size) {
        targetScrollIndex?.let { index ->
            if (index in allApps.indices) {
                pagerState.animateScrollToPage(index)
            }
            onTargetScrolled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .testTag("refreels_main_screen")
    ) {
        if (allApps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("empty_feed_state"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No referral offers yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap Go Viral below to submit a referral link and video.",
                    color = Color(0x99FFFFFF),
                    fontSize = 13.sp
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager"),
                key = { allApps[it].id }
            ) { page ->
                val app = allApps[page]
                val isSaved = savedIds.contains(app.id)
                val effectiveLink = getEffectiveLink(app)
                val hasCustom = customLinks.containsKey(app.id)
                val isVideoActive = page == pagerState.currentPage &&
                    activeSheet == SheetType.NONE &&
                    qrApp == null &&
                    editApp == null &&
                    !isShareAppDialogOpen

                ReelItemView(
                    app = app,
                    isSaved = isSaved,
                    effectiveLink = effectiveLink,
                    hasCustomLink = hasCustom,
                    isVideoActive = isVideoActive,
                    onCopy = { onCopy(app) },
                    onQr = { onOpenQr(app) },
                    onShare = { onShare(app) },
                    onToggleSave = { onToggleSave(app) },
                    onEditCode = { onOpenEdit(app) }
                )
            }
        }

        AnimatedVisibility(
            visible = pagerState.currentPage == 0 && allApps.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 78.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "bob")
            val bobOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bob_offset"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = bobOffset.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0x99FFFFFF),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "SWIPE UP",
                    color = Color(0x99FFFFFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        RefReelsTopBar(
            onSearchClick = { onOpenSheet(SheetType.SEARCH) },
            onShareAppClick = onOpenShareApp,
            onSignOutClick = onSignOut,
            isAdmin = isAdmin,
            currentIndex = pagerState.currentPage,
            totalCount = allApps.size,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        RefReelsBottomNav(
            activeSheet = activeSheet,
            savedCount = savedIds.size,
            onNavClick = { nav ->
                when (nav) {
                    "reels" -> onCloseSheet()
                    "search" -> onOpenSheet(SheetType.SEARCH)
                    "viral" -> onOpenSheet(SheetType.VIRAL)
                    "saved" -> onOpenSheet(SheetType.SAVED)
                    "qr" -> {
                        val currentApp = allApps.getOrNull(pagerState.currentPage) ?: allApps.firstOrNull()
                        currentApp?.let { onOpenQr(it) }
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AnimatedVisibility(
            visible = activeSheet == SheetType.SEARCH,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            SearchSheet(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                selectedTag = selectedTag,
                onTagSelect = onTagSelect,
                results = filteredApps,
                getEffectiveLink = getEffectiveLink,
                onSelectApp = onJumpToApp,
                onClose = onCloseSheet
            )
        }

        AnimatedVisibility(
            visible = activeSheet == SheetType.SAVED,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            SavedSheet(
                savedApps = savedApps,
                getEffectiveLink = getEffectiveLink,
                onSelectApp = onJumpToApp,
                onCopy = onCopy,
                onQr = onOpenQr,
                onShare = onShare,
                onRemove = onToggleSave,
                onClose = onCloseSheet
            )
        }

        AnimatedVisibility(
            visible = activeSheet == SheetType.VIRAL,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            viralSheetContent()
        }

        RefReelsToast(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )

        qrApp?.let { app ->
            val link = getEffectiveLink(app)
            QrCodeDialog(
                app = app,
                effectiveLink = link,
                onDismiss = onCloseQr,
                onCopy = { onCopy(app) },
                onShare = { onShare(app) }
            )
        }

        editApp?.let { app ->
            val currentCustom = customLinks[app.id] ?: ""
            CustomCodeDialog(
                app = app,
                currentCustomValue = currentCustom,
                onDismiss = onCloseEdit,
                onSave = { newCode -> onSaveCustomLink(app, newCode) }
            )
        }

        if (isShareAppDialogOpen) {
            shareAppDialogContent()
        }
    }
}
