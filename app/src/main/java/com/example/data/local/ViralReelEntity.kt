package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "viral_reels")
data class ViralReelEntity(
    @PrimaryKey val id: String,
    val title: String,
    val referralLink: String,
    val promoCode: String,
    val bonusOffer: String,
    val description: String,
    val videoPath: String,
    val easypaisaSenderPhone: String,
    val easypaisaTid: String,
    val receiptImagePath: String = "",
    val amountPkr: Int = 100,
    val viralShares: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)
