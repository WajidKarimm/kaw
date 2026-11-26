package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RefReelsViewModel
import com.example.ui.SheetType
import com.example.ui.components.CustomCodeDialog
import com.example.ui.components.QrCodeDialog
import com.example.ui.components.ReelItemView
import com.example.ui.components.RefReelsBottomNav
import com.example.ui.components.RefReelsToast
import com.example.ui.components.RefReelsTopBar
import com.example.ui.components.SavedSheet
import com.example.ui.components.SearchSheet
import com.example.ui.theme.RefReelsTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: RefReelsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RefReelsTheme {
                RefReelsApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RefReelsApp(viewModel: RefReelsViewModel) {
    val allApps = viewModel.allApps
    val savedIds by viewModel.savedIds.collectAsState()
    val customLinks by viewModel.customLinks.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val savedApps by viewModel.savedApps.collectAsState()
    val qrApp by viewModel.qrDialogApp.collectAsState()
    val editApp by viewModel.editDialogApp.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val targetScrollIndex by viewModel.targetScrollIndex.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { allApps.size }
    )

    // Handle back button if sheet or modal is open
    BackHandler(enabled = activeSheet != SheetType.NONE || qrApp != null || editApp != null) {
        if (qrApp != null) {
            viewModel.closeQrDialog()
        } else if (editApp != null) {
            viewModel.closeEditDialog()
        } else if (activeSheet != SheetType.NONE) {
            viewModel.closeSheet()
        }
    }

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            viewModel.clearToast()
        }
    }

    // Keep pager current page synchronized with ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.updateCurrentIndex(page)
        }
    }

    // Scroll to target page when requested (e.g. from Search or Saved)
    LaunchedEffect(targetScrollIndex) {
        targetScrollIndex?.let { index ->
            pagerState.animateScrollToPage(index)
            viewModel.onTargetScrolled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
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
                    text = "Add rows to assets/referral_links.csv and rebuild.",
                    color = Color(0x99FFFFFF),
                    fontSize = 13.sp
                )
            }
        } else {
            // Full screen Vertical Reel Pager
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reels_vertical_pager"),
                key = { allApps[it].id }
            ) { page ->
                val app = allApps[page]
                val isSaved = savedIds.contains(app.id)
                val effectiveLink = viewModel.getEffectiveLink(app)
                val hasCustom = customLinks.containsKey(app.id)

                ReelItemView(
                    app = app,
                    isSaved = isSaved,
                    effectiveLink = effectiveLink,
                    hasCustomLink = hasCustom,
                    onCopy = { viewModel.copyLink(app) },
                    onQr = { viewModel.openQrDialog(app) },
                    onShare = { viewModel.shareLink(app) },
                    onToggleSave = { viewModel.toggleSave(app) },
                    onEditCode = { viewModel.openEditDialog(app) }
                )
            }
        }

        // Swipe up indicator for initial reel
        AnimatedVisibility(
            visible = pagerState.currentPage == 0,
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

        // Top Brand Header Bar
        RefReelsTopBar(
            onSearchClick = { viewModel.openSheet(SheetType.SEARCH) },
            currentIndex = pagerState.currentPage,
            totalCount = allApps.size,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Navigation Bar
        RefReelsBottomNav(
            activeSheet = activeSheet,
            savedCount = savedIds.size,
            onNavClick = { nav ->
                when (nav) {
                    "reels" -> viewModel.closeSheet()
                    "search" -> viewModel.openSheet(SheetType.SEARCH)
                    "saved" -> viewModel.openSheet(SheetType.SAVED)
                    "qr" -> {
                        val currentApp = allApps.getOrNull(pagerState.currentPage) ?: allApps.firstOrNull()
                        currentApp?.let { viewModel.openQrDialog(it) }
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Search Sheet Overlay
        AnimatedVisibility(
            visible = activeSheet == SheetType.SEARCH,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            SearchSheet(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                selectedTag = selectedTag,
                onTagSelect = { viewModel.setSelectedTag(it) },
                results = filteredApps,
                onSelectApp = { app -> viewModel.jumpToApp(app) },
                onClose = { viewModel.closeSheet() }
            )
        }

        // Saved Sheet Overlay
        AnimatedVisibility(
            visible = activeSheet == SheetType.SAVED,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
        ) {
            SavedSheet(
                savedApps = savedApps,
                onSelectApp = { app -> viewModel.jumpToApp(app) },
                onCopy = { app -> viewModel.copyLink(app) },
                onQr = { app -> viewModel.openQrDialog(app) },
                onShare = { app -> viewModel.shareLink(app) },
                onRemove = { app -> viewModel.toggleSave(app) },
                onClose = { viewModel.closeSheet() }
            )
        }

        // Floating Toast message
        RefReelsToast(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )

        // QR Code Dialog
        qrApp?.let { app ->
            val link = viewModel.getEffectiveLink(app)
            QrCodeDialog(
                app = app,
                effectiveLink = link,
                onDismiss = { viewModel.closeQrDialog() },
                onCopy = { viewModel.copyLink(app) },
                onShare = { viewModel.shareLink(app) }
            )
        }

        // Custom Referral Code Dialog
        editApp?.let { app ->
            val currentCustom = customLinks[app.id] ?: ""
            CustomCodeDialog(
                app = app,
                currentCustomValue = currentCustom,
                onDismiss = { viewModel.closeEditDialog() },
                onSave = { newCode -> viewModel.saveCustomLink(app, newCode) }
            )
        }
    }
}
