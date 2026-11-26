package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SportsbookApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SportsbookRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("refreels_prefs", Context.MODE_PRIVATE)

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _customLinks = MutableStateFlow<Map<String, String>>(emptyMap())
    val customLinks: StateFlow<Map<String, String>> = _customLinks.asStateFlow()

    init {
        loadSaved()
        loadCustomLinks()
    }

    private fun loadSaved() {
        val stored = prefs.getStringSet("saved_apps", emptySet()) ?: emptySet()
        _savedIds.value = stored
    }

    private fun loadCustomLinks() {
        val all = prefs.all
        val map = mutableMapOf<String, String>()
        for ((k, v) in all) {
            if (k.startsWith("custom_ref_") && v is String) {
                val id = k.removePrefix("custom_ref_")
                map[id] = v
            }
        }
        _customLinks.value = map
    }

    fun toggleSave(appId: String): Boolean {
        val current = _savedIds.value.toMutableSet()
        val isSaved: Boolean
        if (current.contains(appId)) {
            current.remove(appId)
            isSaved = false
        } else {
            current.add(appId)
            isSaved = true
        }
        _savedIds.value = current
        prefs.edit().putStringSet("saved_apps", current).apply()
        return isSaved
    }

    fun setCustomLink(appId: String, linkOrCode: String) {
        val current = _customLinks.value.toMutableMap()
        if (linkOrCode.isBlank()) {
            current.remove(appId)
            prefs.edit().remove("custom_ref_$appId").apply()
        } else {
            val trimmed = linkOrCode.trim()
            current[appId] = trimmed
            prefs.edit().putString("custom_ref_$appId", trimmed).apply()
        }
        _customLinks.value = current
    }

    fun getEffectiveLink(app: SportsbookApp): String {
        val custom = _customLinks.value[app.id]
        if (!custom.isNullOrBlank()) {
            return if (custom.startsWith("http://") || custom.startsWith("https://")) {
                custom
            } else {
                val base = app.defaultRef.trimEnd('/')
                val separator = if (base.contains("?")) "&" else "?"
                "$base${separator}ref=$custom"
            }
        }
        return app.defaultRef
    }

    fun getInitialApps(): List<SportsbookApp> {
        val fromCsv = ReferralCsvLoader.load(context)
        if (fromCsv.isNotEmpty()) return fromCsv
        return getFallbackApps()
    }

    private fun getFallbackApps(): List<SportsbookApp> = listOf(
        SportsbookApp(
            id = "draftkings",
            name = "DraftKings",
            iconType = "football",
            payout = "Up to $100",
            payoutVal = 100,
            badge = "🏆 Most Popular",
            rating = "4.8",
            reviews = "1.4M",
            defaultRef = "https://sportsbook.draftkings.com/refer-a-friend",
            promoCode = "Personal Link",
            description = "Matched bonus bet up to $100 when a brand-new friend registers via your link, verifies ID, and deposits $25+.",
            termsHighlight = "Min $25 deposit · Max 5 referrals ($500) · 7-day bonus expiry",
            linkInstructions = "DraftKings App → Menu / Rewards Center → Promos → Refer-A-Friend",
            tags = listOf("sportsbook", "dfs", "high payout", "us"),
            gradientStart = 0xFF059669,
            gradientEnd = 0xFF04331F,
            accentColor = 0xFF6EE7B7,
            region = "US"
        ),
        SportsbookApp(
            id = "fanduel",
            name = "FanDuel",
            iconType = "trophy",
            payout = "$50–$75",
            payoutVal = 75,
            badge = "🎯 Fan Favourite",
            rating = "4.8",
            reviews = "1.3M",
            defaultRef = "https://www.fanduel.com/refer",
            promoCode = "Personal Link",
            description = "Earn $50–$75 in Bonus Bets when a new customer joins with your link, deposits $5–$10+, and settles a qualifying wager.",
            termsHighlight = "Min $10 bet · Max 5–10 per 30 days · 7-day bonus expiry",
            linkInstructions = "FanDuel App → Rewards Hub / Earn $$$ icon → Refer-A-Friend",
            tags = listOf("sportsbook", "dfs", "us", "sgp"),
            gradientStart = 0xFF2563EB,
            gradientEnd = 0xFF0A1C4D,
            accentColor = 0xFF93C5FD,
            region = "US"
        ),
        SportsbookApp(
            id = "betmgm",
            name = "BetMGM",
            iconType = "crown",
            payout = "$50–$100",
            payoutVal = 100,
            badge = "👑 High Cap",
            rating = "4.6",
            reviews = "920K",
            defaultRef = "https://sports.betmgm.com/en/sports/refer-a-friend",
            promoCode = "Personal Link",
            description = "Receive $100 Sports Bonus or $50 Casino Bonus per friend. Both accounts must register in the same state jurisdiction.",
            termsHighlight = "Same-state rule · Max 20 referrals/mo · Verified ID required",
            linkInstructions = "BetMGM App → Promotions → Refer A Friend → Copy Invite Link",
            tags = listOf("sportsbook", "casino", "high payout", "us"),
            gradientStart = 0xFFB45309,
            gradientEnd = 0xFF3A1D02,
            accentColor = 0xFFFCD34D,
            region = "US"
        ),
        SportsbookApp(
            id = "betrivers",
            name = "BetRivers",
            iconType = "trending",
            payout = "$50–$500",
            payoutVal = 500,
            badge = "🎡 Bonus Wheel",
            rating = "4.5",
            reviews = "240K",
            defaultRef = "https://www.betrivers.com",
            promoCode = "Personal Link",
            description = "Both you and your friend earn a Wheel Spin worth $50–$500 in Bonus Money (1x playthrough) after wagering $50 within 30 days.",
            termsHighlight = "$50 real-money wager · 1x playthrough · Max 3 referrals",
            linkInstructions = "BetRivers App → My Account → Promotions → Refer-A-Friend",
            tags = listOf("sportsbook", "high payout", "casino", "us"),
            gradientStart = 0xFF0284C7,
            gradientEnd = 0xFF082F49,
            accentColor = 0xFF7DD3FC,
            region = "US"
        ),
        SportsbookApp(
            id = "fanatics",
            name = "Fanatics",
            iconType = "trophy",
            payout = "$30–$100",
            payoutVal = 100,
            badge = "🧢 FanCash",
            rating = "4.7",
            reviews = "410K",
            defaultRef = "https://sportsbook.fanatics.com",
            promoCode = "Personal Link",
            description = "Includes former PointsBet US users. Earn $30–$100 in FanCash (convertible to bonus bets or gear) after a $10–$20 cash wager.",
            termsHighlight = "$10–$20 cash wager · Max 5 referrals ($500) · Link required",
            linkInstructions = "Fanatics Sportsbook App → Account Menu → Refer-A-Friend",
            tags = listOf("sportsbook", "rewards", "high payout", "us"),
            gradientStart = 0xFF7C3AED,
            gradientEnd = 0xFF2A1065,
            accentColor = 0xFFC4B5FD,
            region = "US"
        ),
        SportsbookApp(
            id = "hardrock",
            name = "Hard Rock Bet",
            iconType = "fire",
            payout = "Up to $50",
            payoutVal = 50,
            badge = "🎸 20x Limit",
            rating = "4.8",
            reviews = "380K",
            defaultRef = "https://www.hardrock.bet",
            promoCode = "Personal Link",
            description = "Both you and your friend get a matching Bonus Bet up to $50 after they deposit and place a $10+ bet at -250 odds or longer.",
            termsHighlight = "Min $10 bet (-250+ odds) · Max 20 referrals ($1,000)",
            linkInstructions = "Hard Rock Bet App → Rewards → Invite and Earn",
            tags = listOf("sportsbook", "rewards", "us"),
            gradientStart = 0xFF9333EA,
            gradientEnd = 0xFF3B0764,
            accentColor = 0xFFD8B4FE,
            region = "US"
        ),
        SportsbookApp(
            id = "caesars",
            name = "Caesars",
            iconType = "landmark",
            payout = "$25+ Bets",
            payoutVal = 25,
            badge = "🏛️ Rewards",
            rating = "4.6",
            reviews = "710K",
            defaultRef = "https://www.caesars.com/sportsbook-and-casino",
            promoCode = "Personal Link",
            description = "Receive a bundle of bonus bets (e.g., 5 bonus bets totaling $25+) when your friend deposits and wagers $25+ cumulatively.",
            termsHighlight = "Min $25 deposit & wager · Caesars Rewards account required",
            linkInstructions = "Caesars App → My Promos → Refer-A-Friend",
            tags = listOf("sportsbook", "rewards", "us"),
            gradientStart = 0xFF1D4ED8,
            gradientEnd = 0xFF0A1A45,
            accentColor = 0xFF93C5FD,
            region = "US"
        ),
        SportsbookApp(
            id = "stake",
            name = "Stake",
            iconType = "crypto",
            payout = "10% Lifetime",
            payoutVal = 120,
            badge = "⚡ Crypto Affiliate",
            rating = "4.6",
            reviews = "1.5M",
            defaultRef = "https://stake.com/affiliate/overview",
            promoCode = "Affiliate Code",
            description = "Ongoing affiliate revshare rather than a one-time cap: earn 10% of the house edge on sports (3% edge) and casino wagers.",
            termsHighlight = "Lifetime house-edge commission · Crypto instant payout",
            linkInstructions = "Stake → Profile / Affiliate Tab → Create Campaign Link",
            tags = listOf("sportsbook", "crypto", "global", "high payout"),
            gradientStart = 0xFF15803D,
            gradientEnd = 0xFF04260F,
            accentColor = 0xFF4ADE80,
            region = "Global"
        ),
        SportsbookApp(
            id = "bet365",
            name = "Bet365",
            iconType = "soccer",
            payout = "$50 (CA) / Offers",
            payoutVal = 50,
            badge = "🌍 Global #1",
            rating = "4.7",
            reviews = "3.6M",
            defaultRef = "https://www.bet365.com",
            promoCode = "TAN365",
            description = "World's largest live betting book. Direct Refer-A-Friend ($50/friend, $500 cap) runs in select markets like Canada; US uses welcome offers.",
            termsHighlight = "Region-dependent · $500 annual cap in active referral markets",
            linkInstructions = "Bet365 App (Eligible Regions) → Account Menu → Offers → Refer-A-Friend",
            tags = listOf("sportsbook", "global", "live betting"),
            gradientStart = 0xFF65A30D,
            gradientEnd = 0xFF1E2E05,
            accentColor = 0xFFBEF264,
            region = "Global"
        ),
        SportsbookApp(
            id = "underdog",
            name = "Underdog",
            iconType = "target",
            payout = "$10–$25",
            payoutVal = 25,
            badge = "🐶 Pick'em DFS",
            rating = "4.8",
            reviews = "480K",
            defaultRef = "https://underdogfantasy.com",
            promoCode = "Personal Link",
            description = "Earn $10–$25 in bonus funds plus contest leaderboard perks when your friend deposits $10+ and enters their first fantasy contest.",
            termsHighlight = "Min $10 deposit & entry · Available in most US states",
            linkInstructions = "Underdog App → Account / Profile Tab → Invite Friends",
            tags = listOf("dfs", "us", "pickem"),
            gradientStart = 0xFFD97706,
            gradientEnd = 0xFF451A03,
            accentColor = 0xFFFDE68A,
            region = "US"
        ),
        SportsbookApp(
            id = "prizepicks",
            name = "PrizePicks",
            iconType = "diamond",
            payout = "Bonus Lineups",
            payoutVal = 50,
            badge = "💎 Daily Fantasy",
            rating = "4.8",
            reviews = "620K",
            defaultRef = "https://www.prizepicks.com",
            promoCode = "Personal Code",
            description = "Earn Bonus Balance / Bonus Lineups when a friend signs up with your personal invite code and completes their minimum first deposit.",
            termsHighlight = "New users only · Min first deposit required · Flex/Power",
            linkInstructions = "PrizePicks App → Profile / Promos → Refer a Friend",
            tags = listOf("dfs", "us", "pickem"),
            gradientStart = 0xFF6D28D9,
            gradientEnd = 0xFF1E1B4B,
            accentColor = 0xFFA78BFA,
            region = "US"
        ),
        SportsbookApp(
            id = "betway",
            name = "Betway",
            iconType = "dice",
            payout = "Free Bets",
            payoutVal = 40,
            badge = "🎲 UK / CA / EU",
            rating = "4.4",
            reviews = "540K",
            defaultRef = "https://betway.com",
            promoCode = "Personal Link",
            description = "Region-specific Refer-a-Friend rewards in UK, Ontario, and European markets. Unlocks Free Bet credits after qualifying wager settles.",
            termsHighlight = "UK / Ontario / EU availability · Qualifying min-odds bet",
            linkInstructions = "Betway Account Dashboard → Promotions → Refer a Friend",
            tags = listOf("sportsbook", "casino", "global"),
            gradientStart = 0xFF0F766E,
            gradientEnd = 0xFF04211F,
            accentColor = 0xFF6EE7B7,
            region = "Global"
        )
    )
}
