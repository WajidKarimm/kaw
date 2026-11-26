package com.example.model

data class SportsbookApp(
    val id: String,
    val name: String,
    val iconType: String,
    val payout: String,
    val payoutVal: Int,
    val badge: String,
    val rating: String,
    val reviews: String,
    val defaultRef: String,
    val promoCode: String,
    val description: String,
    val tags: List<String>,
    val gradientStart: Long,
    val gradientEnd: Long,
    val accentColor: Long,
    val region: String = "US",
    val termsHighlight: String = "",
    val linkInstructions: String = "",
    val videoPath: String = "",
    val isUserViral: Boolean = false,
    val viralShares: Int = 0,
    val easypaisaTid: String = ""
)
