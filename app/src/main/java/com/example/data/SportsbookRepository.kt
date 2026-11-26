package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.RefReelsDatabase
import com.example.data.local.ViralReelEntity
import com.example.data.remote.ViralSubmissionDoc
import com.example.model.SportsbookApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EasypaisaMerchantConfig(
    val accountTitle: String = "",
    val accountNumber: String = ""
) {
    val isConfigured: Boolean
        get() = accountTitle.isNotBlank() && accountNumber.isNotBlank()
}

class SportsbookRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("refreels_prefs", Context.MODE_PRIVATE)

    private val viralReelDao by lazy {
        RefReelsDatabase.getInstance(context).viralReelDao()
    }

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _customLinks = MutableStateFlow<Map<String, String>>(emptyMap())
    val customLinks: StateFlow<Map<String, String>> = _customLinks.asStateFlow()

    private val _localVideoMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val localVideoMap: StateFlow<Map<String, String>> = _localVideoMap.asStateFlow()

    private val _localAppShareUrl = MutableStateFlow(loadLocalAppShareUrl())
    val localAppShareUrl: StateFlow<String> = _localAppShareUrl.asStateFlow()

    private val _merchantConfig = MutableStateFlow(loadMerchantConfig())
    val merchantConfig: StateFlow<EasypaisaMerchantConfig> = _merchantConfig.asStateFlow()

    val viralReelsFlow: Flow<List<ViralReelEntity>>
        get() = viralReelDao.observeAllViralReels()

    init {
        loadSaved()
        loadCustomLinks()
        loadLocalVideos()
    }

    fun defaultAppShareUrl(): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}"

    private fun loadLocalAppShareUrl(): String {
        return prefs.getString("app_share_download_url", "")?.trim().orEmpty()
    }

    fun saveLocalAppShareUrl(url: String) {
        val clean = url.trim()
        if (clean.isBlank()) {
            prefs.edit().remove("app_share_download_url").apply()
            _localAppShareUrl.value = ""
        } else {
            prefs.edit().putString("app_share_download_url", clean).apply()
            _localAppShareUrl.value = clean
        }
    }

    private fun loadMerchantConfig(): EasypaisaMerchantConfig {
        val title = prefs.getString("easypaisa_merchant_title", "") ?: ""
        val number = prefs.getString("easypaisa_merchant_number", "") ?: ""
        return EasypaisaMerchantConfig(accountTitle = title, accountNumber = number)
    }

    fun saveMerchantConfig(accountTitle: String, accountNumber: String) {
        val cleanTitle = accountTitle.trim()
        val cleanNumber = ViralMediaStorage.normalizePakistaniPhone(accountNumber)
        prefs.edit()
            .putString("easypaisa_merchant_title", cleanTitle)
            .putString("easypaisa_merchant_number", cleanNumber)
            .apply()
        _merchantConfig.value = EasypaisaMerchantConfig(
            accountTitle = cleanTitle,
            accountNumber = cleanNumber
        )
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

    private fun loadLocalVideos() {
        val all = prefs.all
        val map = mutableMapOf<String, String>()
        for ((k, v) in all) {
            if (k.startsWith("local_video_") && v is String) {
                val id = k.removePrefix("local_video_")
                map[id] = v
            }
        }
        _localVideoMap.value = map
    }

    fun saveLocalVideoMapping(submissionId: String, localPath: String) {
        if (submissionId.isBlank() || localPath.isBlank()) return
        prefs.edit().putString("local_video_$submissionId", localPath).apply()
        _localVideoMap.value = _localVideoMap.value + (submissionId to localPath)
    }

    fun getLocalVideoMapping(submissionId: String): String {
        return _localVideoMap.value[submissionId].orEmpty()
    }

    fun removeLocalVideoMapping(submissionId: String) {
        val existingPath = _localVideoMap.value[submissionId]
        if (!existingPath.isNullOrBlank()) {
            ViralMediaStorage.deleteStoredFileIfOwned(context, existingPath)
        }
        prefs.edit().remove("local_video_$submissionId").apply()
        _localVideoMap.value = _localVideoMap.value - submissionId
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

    suspend fun isTransactionIdUsed(tid: String): Boolean {
        return viralReelDao.countByTransactionId(tid.trim()) > 0
    }

    suspend fun insertViralReel(entity: ViralReelEntity) {
        viralReelDao.insertViralReel(entity)
    }

    suspend fun incrementViralShares(appId: String) {
        viralReelDao.incrementViralShares(appId)
    }

    suspend fun deleteViralReel(entity: ViralReelEntity) {
        ViralMediaStorage.deleteStoredFileIfOwned(context, entity.videoPath)
        ViralMediaStorage.deleteStoredFileIfOwned(context, entity.receiptImagePath)
        viralReelDao.deleteViralReel(entity.id)
    }

    fun getInitialApps(): List<SportsbookApp> {
        val fromCsv = ReferralCsvLoader.load(context)
        if (fromCsv.isNotEmpty()) return fromCsv
        return getFallbackApps()
    }

    private fun getFallbackApps(): List<SportsbookApp> = defaultCatalog()

    companion object {
        fun ViralSubmissionDoc.toSportsbookApp(localVideoOverride: String = ""): SportsbookApp {
            val codeTag = promoCode.ifBlank { "Viral" }
            val creator = authorName.ifBlank { "Community" }
            return SportsbookApp(
                id = id,
                name = title,
                iconType = "fire",
                payout = bonusOffer.ifBlank { "Viral Offer" },
                payoutVal = amountPkr.toInt(),
                badge = "🔥 Viral · Verified",
                rating = "5.0",
                reviews = if (viralShares == 1L) "1 share" else "$viralShares shares",
                defaultRef = referralLink,
                promoCode = codeTag,
                description = description.ifBlank {
                    "Admin-verified viral referral link shared by $creator. Tap Copy or Share to boost reach."
                },
                termsHighlight = "100 PKR Verified · Shared by $creator",
                linkInstructions = "Promoted viral referral link ($referralLink)",
                tags = listOf("viral", "verified", "easypaisa", "community"),
                gradientStart = 0xFF059669,
                gradientEnd = 0xFF04260F,
                accentColor = 0xFF34D399,
                region = "Global",
                videoPath = videoUrl.ifBlank { localVideoOverride },
                isUserViral = true,
                viralShares = viralShares.toInt(),
                easypaisaTid = ""
            )
        }

        fun ViralReelEntity.toSportsbookApp(): SportsbookApp {
            val codeTag = promoCode.ifBlank { "Viral" }
            return SportsbookApp(
                id = id,
                name = title,
                iconType = "fire",
                payout = bonusOffer.ifBlank { "Viral Offer" },
                payoutVal = amountPkr,
                badge = "🔥 Viral · 100 PKR Verified",
                rating = "5.0",
                reviews = if (viralShares == 1) "1 share" else "$viralShares shares",
                defaultRef = referralLink,
                promoCode = codeTag,
                description = description.ifBlank {
                    "Community promoted viral referral link with promo video. Tap Copy or Share to boost reach."
                },
                termsHighlight = "Easypaisa TID: $easypaisaTid · $amountPkr PKR Boost",
                linkInstructions = "Promoted viral referral link ($referralLink)",
                tags = listOf("viral", "video", "easypaisa", "community"),
                gradientStart = 0xFF059669,
                gradientEnd = 0xFF04260F,
                accentColor = 0xFF34D399,
                region = "Global",
                videoPath = videoPath,
                isUserViral = true,
                viralShares = viralShares,
                easypaisaTid = easypaisaTid
            )
        }

        fun defaultCatalog(): List<SportsbookApp> = listOf(
            SportsbookApp(
                id = "j10app",
                name = "J10 App",
                iconType = "crown",
                payout = "Invite Bonus",
                payoutVal = 50,
                badge = "🎁 Active Link",
                rating = "4.8",
                reviews = "Verified Link",
                defaultRef = "https://j10app.win/?dl=abxtwr",
                promoCode = "abxtwr",
                description = "Join J10 App directly through this verified referral download link with invite code abxtwr.",
                termsHighlight = "Code abxtwr embedded · Tap Copy or QR to share",
                linkInstructions = "Open https://j10app.win/?dl=abxtwr or scan QR code",
                tags = listOf("j10app", "bonus", "global", "referral"),
                gradientStart = 0xFF7C3AED,
                gradientEnd = 0xFF2A1065,
                accentColor = 0xFFC4B5FD,
                region = "Global"
            ),
            SportsbookApp(
                id = "win786",
                name = "Win786",
                iconType = "trophy",
                payout = "Invite Bonus",
                payoutVal = 51,
                badge = "⚡ Active Link",
                rating = "4.8",
                reviews = "Verified Link",
                defaultRef = "https://win786.win/?id=989008314",
                promoCode = "989008314",
                description = "Register on Win786 using this verified referral link with invite ID 989008314.",
                termsHighlight = "Referral ID 989008314 embedded · Instant QR & share",
                linkInstructions = "Open https://win786.win/?id=989008314 or scan QR code",
                tags = listOf("win786", "bonus", "global", "referral"),
                gradientStart = 0xFF059669,
                gradientEnd = 0xFF04331F,
                accentColor = 0xFF6EE7B7,
                region = "Global"
            ),
            SportsbookApp(
                id = "4pk365",
                name = "4PK365",
                iconType = "diamond",
                payout = "Invite Bonus",
                payoutVal = 52,
                badge = "💎 Active Link",
                rating = "4.8",
                reviews = "Verified Link",
                defaultRef = "https://4pk365.com/?dl=9gz5zj",
                promoCode = "9gz5zj",
                description = "Access 4PK365 directly through this verified referral download link with invite code 9gz5zj.",
                termsHighlight = "Code 9gz5zj embedded · Tap Copy or QR to share",
                linkInstructions = "Open https://4pk365.com/?dl=9gz5zj or scan QR code",
                tags = listOf("4pk365", "bonus", "global", "referral"),
                gradientStart = 0xFFB45309,
                gradientEnd = 0xFF3A1D02,
                accentColor = 0xFFFCD34D,
                region = "Global"
            )
        )
    }
}
