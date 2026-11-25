package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AppIconGlyph(
    iconType: String,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val imageVector = when (iconType.lowercase()) {
        "target" -> Icons.Default.Adjust
        "football" -> Icons.Default.SportsFootball
        "crown" -> Icons.Default.Stars
        "landmark" -> Icons.Default.Apartment
        "trending" -> Icons.Default.TrendingUp
        "fire" -> Icons.Default.LocalFireDepartment
        "trophy" -> Icons.Default.EmojiEvents
        "soccer" -> Icons.Default.SportsSoccer
        "dice" -> Icons.Default.Casino
        "diamond" -> Icons.Default.Diamond
        "crypto" -> Icons.Default.CurrencyBitcoin
        else -> Icons.Default.EmojiEvents
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}
