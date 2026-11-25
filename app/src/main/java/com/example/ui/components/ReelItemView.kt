package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SportsbookApp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.LineBorder
import com.example.ui.theme.PanelDark
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReelItemView(
    app: SportsbookApp,
    isSaved: Boolean,
    effectiveLink: String,
    hasCustomLink: Boolean,
    onCopy: () -> Unit,
    onQr: () -> Unit,
    onShare: () -> Unit,
    onToggleSave: () -> Unit,
    onEditCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(app.accentColor)
    val gradStart = Color(app.gradientStart)
    val gradEnd = Color(app.gradientEnd)

    val bookmarkBgColor by animateColorAsState(
        targetValue = if (isSaved) GoldPrimary else Color(0x730A1018),
        animationSpec = tween(250),
        label = "bookmark_bg"
    )
    val bookmarkIconColor by animateColorAsState(
        targetValue = if (isSaved) Color(0xFF0A0F16) else Color.White,
        animationSpec = tween(250),
        label = "bookmark_icon"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .testTag("reel_item_${app.id}")
    ) {
        // Gradient ambient aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(gradStart.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.15f),
                    radius = size.width * 0.85f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(gradEnd.copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.90f),
                    radius = size.width * 0.8f
                )
            )
            // Subtle dark overlay to preserve high readability
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB305070A),
                        Color(0x3305070A),
                        Color(0x8C05070A),
                        Color(0xFA05070A)
                    )
                )
            )
        }

        // Center Hero
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glassmorphic App Icon Ring
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(1.5.dp, accentColor.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x40FFFFFF), Color(0x10FFFFFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AppIconGlyph(
                        iconType = app.iconType,
                        tint = accentColor,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Title
            Text(
                text = app.name,
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rating & Reviews
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = app.rating,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "·",
                    color = TextDim,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${app.reviews} reviews",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payout Pill with golden glow
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(GoldSoft)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = app.payout,
                        color = GoldPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "per referral",
                        color = GoldPrimary.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }

            // Custom code badge if user personalized
            AnimatedVisibility(
                visible = hasCustomLink,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x3338BDF8))
                        .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "✨ Your Custom Code Active",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Right Action Rail (Vertical Buttons)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 96.dp)
                .testTag("reel_action_rail_${app.id}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Copy Button
            ActionRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy referral link",
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = "Copy",
                onClick = onCopy,
                testTag = "action_copy_${app.id}"
            )

            // QR Code Button
            ActionRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Show QR code",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = "QR",
                onClick = onQr,
                testTag = "action_qr_${app.id}"
            )

            // Share Button
            ActionRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share link",
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = "Share",
                onClick = onShare,
                testTag = "action_share_${app.id}"
            )

            // Bookmark / Save Button
            ActionRailButton(
                icon = {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved" else "Save app",
                        modifier = Modifier.size(20.dp),
                        tint = bookmarkIconColor
                    )
                },
                label = if (isSaved) "Saved" else "Save",
                onClick = onToggleSave,
                customBg = bookmarkBgColor,
                testTag = "action_save_${app.id}"
            )

            // My Code / Customize Button
            ActionRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Customize referral code",
                        modifier = Modifier.size(19.dp)
                    )
                },
                label = "My Code",
                onClick = onEditCode,
                testTag = "action_edit_${app.id}"
            )
        }

        // Bottom Info Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 76.dp, bottom = 92.dp)
                .testTag("reel_info_${app.id}")
        ) {
            // Badge tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GoldPrimary)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = app.badge,
                    color = Color(0xFF0A0F16),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Referral Chip with copy action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC080D14))
                    .border(1.dp, LineBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = effectiveLink,
                        color = Color(0xFFC9D9F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(GoldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(0xFF0A0F16),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = app.description,
                color = Color(0xDBE2ECFA),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                app.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionRailButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customBg: Color? = null,
    testTag: String = ""
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(customBg ?: Color(0x730A1018))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color(0xB3FFFFFF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
