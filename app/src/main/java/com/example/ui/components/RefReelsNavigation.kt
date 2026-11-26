package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SheetType
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LineLight
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextPrimary

@Composable
fun RefReelsTopBar(
    onSearchClick: () -> Unit,
    currentIndex: Int = 0,
    totalCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("brand_header")
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.linearGradient(listOf(GoldPrimary, GoldDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color(0xFF0A0F16),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column {
                Row {
                    Text(
                        text = "REF",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "REELS",
                        color = GoldPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
                if (totalCount > 0) {
                    Text(
                        text = "$totalCount bonuses live",
                        color = TextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Position pill, e.g. "4 / 12"
            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0x8C0A1018))
                        .border(1.dp, LineLight, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("top_position_pill")
                ) {
                    Text(
                        text = "${currentIndex + 1} / $totalCount",
                        color = TextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Search icon button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x8C0A1018))
                    .border(1.dp, LineLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RefReelsBottomNav(
    activeSheet: SheetType,
    savedCount: Int,
    onNavClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xF005070A), Color(0xFF05070A))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("bottom_nav")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.PlayArrow,
                label = "Reels",
                isSelected = activeSheet == SheetType.NONE,
                onClick = { onNavClick("reels") },
                testTag = "nav_reels"
            )

            NavItem(
                icon = Icons.Default.Search,
                label = "Search",
                isSelected = activeSheet == SheetType.SEARCH,
                onClick = { onNavClick("search") },
                testTag = "nav_search"
            )

            NavItem(
                icon = Icons.Default.QrCode,
                label = "My QR",
                isSelected = false,
                onClick = { onNavClick("qr") },
                testTag = "nav_qr"
            )

            NavItem(
                icon = Icons.Default.Bookmark,
                label = "Saved",
                isSelected = activeSheet == SheetType.SAVED,
                badgeCount = savedCount,
                onClick = { onNavClick("saved") },
                testTag = "nav_saved"
            )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    testTag: String = ""
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 12.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (badgeCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = GoldPrimary,
                        contentColor = Color(0xFF0A0F16)
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else "$badgeCount",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) GoldPrimary else TextDim,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) GoldPrimary else TextDim,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = if (isSelected) GoldPrimary else TextDim,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun RefReelsToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (message != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xF5141E2A))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("refreels_toast")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
