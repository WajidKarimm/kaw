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
                "${app.defaultRef.substringBeforeLast('/')}/$custom"
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
            id = "pointsbet",
            name = "PointsBet",
            iconType = "target",
            payout = "$50–150",
            payoutVal = 150,
            badge = "🔥 Highest Payout",
            rating = "4.6",
            reviews = "210K",
            defaultRef = "https://pointsbet.com/ref/REFREELS150",
            promoCode = "REFREELS150",
            description = "Earn up to $150 per friend. Referral bonus unlocks after your referee makes an initial deposit and places their qualifying bet.",
            tags = listOf("sportsbook", "high payout", "us", "pointsbetting"),
            gradientStart = 0xFF7C3AED,
            gradientEnd = 0xFF2A1065,
            accentColor = 0xFFC4B5FD,
            region = "US"
        ),
        SportsbookApp(
            id = "draftkings",
            name = "DraftKings",
            iconType = "football",
            payout = "$50–100",
            payoutVal = 100,
            badge = "🏆 Most Popular",
            rating = "4.7",
            reviews = "1.2M",
            defaultRef = "https://draftkings.com/r/refreels",
            promoCode = "REELS100",
            description = "Get up to $100 in bonus bets for every friend who registers and makes a minimum qualifying first deposit.",
            tags = listOf("sportsbook", "dfs", "us", "live streaming"),
            gradientStart = 0xFF059669,
            gradientEnd = 0xFF04331F,
            accentColor = 0xFF6EE7B7,
            region = "US"
        ),
        SportsbookApp(
            id = "betmgm",
            name = "BetMGM",
            iconType = "crown",
            payout = "$50–100",
            payoutVal = 100,
            badge = "👑 Premium",
            rating = "4.5",
            reviews = "890K",
            defaultRef = "https://betmgm.com/ref/reelsbonus",
            promoCode = "KINGMGM",
            description = "Refer friends to the King of Sportsbooks and get $50 to $100. Bonus is credited right after their wager settles.",
            tags = listOf("sportsbook", "casino", "us", "vip rewards"),
            gradientStart = 0xFFB45309,
            gradientEnd = 0xFF3A1D02,
            accentColor = 0xFFFCD34D,
            region = "US"
        ),
        SportsbookApp(
            id = "caesars",
            name = "Caesars",
            iconType = "landmark",
            payout = "$50–100",
            payoutVal = 100,
            badge = "🏛️ Rewards",
            rating = "4.4",
            reviews = "640K",
            defaultRef = "https://caesars.com/sportsbook/r/refreels",
            promoCode = "EMPEROR100",
            description = "Earn $50–$100 bonus bets per signup plus earn Caesars Rewards tier points on every dollar they bet.",
            tags = listOf("sportsbook", "rewards", "us", "hotel perks"),
            gradientStart = 0xFF1D4ED8,
            gradientEnd = 0xFF0A1A45,
            accentColor = 0xFF93C5FD,
            region = "US"
        ),
        SportsbookApp(
            id = "pinnacle",
            name = "Pinnacle",
            iconType = "trending",
            payout = "Varies (Volume)",
            payoutVal = 85,
            badge = "📈 Pro Choice",
            rating = "4.3",
            reviews = "150K",
            defaultRef = "https://pinnacle.com/ref/proreels",
            promoCode = "PROREELS",
            description = "World renowned low-margin sportsbook offering volume-based affiliate and friend reward tiers for serious punters.",
            tags = listOf("sportsbook", "pro", "global", "high limits"),
            gradientStart = 0xFF0D9488,
            gradientEnd = 0xFF04302E,
            accentColor = 0xFF5EEAD4,
            region = "Global"
        ),
        SportsbookApp(
            id = "stake",
            name = "Stake",
            iconType = "fire",
            payout = "Up to $75",
            payoutVal = 75,
            badge = "⚡ Crypto",
            rating = "4.6",
            reviews = "1.5M",
            defaultRef = "https://stake.com/r/refreels",
            promoCode = "STAKEREEL",
            description = "Instant deposits & withdrawals with Bitcoin, Ethereum, and USDT. Unlock rakeback and bonus rewards when referred players wager.",
            tags = listOf("sportsbook", "crypto", "global", "instant payout"),
            gradientStart = 0xFF15803D,
            gradientEnd = 0xFF04260F,
            accentColor = 0xFF4ADE80,
            region = "Global"
        ),
        SportsbookApp(
            id = "fanduel",
            name = "FanDuel",
            iconType = "trophy",
            payout = "$50–75",
            payoutVal = 75,
            badge = "🎯 Fan Favourite",
            rating = "4.7",
            reviews = "1.1M",
            defaultRef = "https://fanduel.com/r/refreels75",
            promoCode = "FDREELS",
            description = "Refer a friend and you both score bonus bets after they place their first $10 wager. Fast cashouts and Same Game Parlays.",
            tags = listOf("sportsbook", "dfs", "us", "sgp"),
            gradientStart = 0xFF2563EB,
            gradientEnd = 0xFF0A1C4D,
            accentColor = 0xFF93C5FD,
            region = "US"
        ),
        SportsbookApp(
            id = "bet365",
            name = "Bet365",
            iconType = "soccer",
            payout = "Up to $70",
            payoutVal = 70,
            badge = "🌍 Global #1",
            rating = "4.5",
            reviews = "3.4M",
            defaultRef = "https://bet365.com/ref/reelspack",
            promoCode = "365REELS",
            description = "World's most popular sportsbook. Huge live in-play betting market selection and region-based referral deposit match promotions.",
            tags = listOf("sportsbook", "global", "live betting", "soccer"),
            gradientStart = 0xFF65A30D,
            gradientEnd = 0xFF1E2E05,
            accentColor = 0xFFBEF264,
            region = "Global"
        ),
        SportsbookApp(
            id = "betway",
            name = "Betway",
            iconType = "dice",
            payout = "$30–50",
            payoutVal = 50,
            badge = "🎲 Casino",
            rating = "4.3",
            reviews = "520K",
            defaultRef = "https://betway.com/ref/reelway",
            promoCode = "WAYREELS",
            description = "Earn $30–$50 across sports bets and casino slots. Seamless signup with verified fast withdrawals across UK, Europe, & NA.",
            tags = listOf("sportsbook", "casino", "global", "esports"),
            gradientStart = 0xFF0F766E,
            gradientEnd = 0xFF04211F,
            accentColor = 0xFF6EE7B7,
            region = "Global"
        ),
        SportsbookApp(
            id = "888sport",
            name = "888sport",
            iconType = "diamond",
            payout = "$30–50",
            payoutVal = 50,
            badge = "💎 Classic",
            rating = "4.2",
            reviews = "410K",
            defaultRef = "https://888sport.com/ref/888reels",
            promoCode = "REELS888",
            description = "Referral bonuses credited as free bets. Benefit from enhanced accumulators, daily bet builder boosts, and VIP loyalty tiers.",
            tags = listOf("sportsbook", "free bets", "global", "horse racing"),
            gradientStart = 0xFFEA580C,
            gradientEnd = 0xFF3D1203,
            accentColor = 0xFFFDBA74,
            region = "Global"
        ),
        SportsbookApp(
            id = "bovada",
            name = "Bovada",
            iconType = "crypto",
            payout = "200% up to $200",
            payoutVal = 200,
            badge = "🚀 Crypto Match",
            rating = "4.5",
            reviews = "780K",
            defaultRef = "https://bovada.lv/ref/reelsmatch",
            promoCode = "BOVREEL",
            description = "Get 200% match up to $200 on referee's first crypto deposit, or $100 for traditional cards. Instant Bitcoin payouts.",
            tags = listOf("sportsbook", "crypto", "global", "poker"),
            gradientStart = 0xFFDC2626,
            gradientEnd = 0xFF450A0A,
            accentColor = 0xFFF87171,
            region = "Global"
        ),
        SportsbookApp(
            id = "fanatics",
            name = "Fanatics",
            iconType = "trophy",
            payout = "Up to $100",
            payoutVal = 100,
            badge = "🧢 Merch & Cash",
            rating = "4.6",
            reviews = "320K",
            defaultRef = "https://fanatics.com/sportsbook/r/refreels",
            promoCode = "FANCASH",
            description = "Earn FanCash rewards per referral usable for official licensed NFL, NBA, & MLB jerseys or direct bet credits.",
            tags = listOf("sportsbook", "rewards", "us", "fancash"),
            gradientStart = 0xFF2563EB,
            gradientEnd = 0xFF172554,
            accentColor = 0xFF60A5FA,
            region = "US"
        )
    )
}
