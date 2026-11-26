package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.SportsbookApp

/**
 * Loads the referral/bonus feed from assets/referral_links.csv so the list can be
 * maintained in a spreadsheet instead of hardcoded Kotlin. Only "Name" and
 * "ReferralLink" are required columns — everything else falls back to a generated
 * default so a plain two-column export from Excel still works.
 */
object ReferralCsvLoader {

    private const val TAG = "ReferralCsvLoader"
    private const val FILE_NAME = "referral_links.csv"

    private val ICONS = listOf(
        "target", "football", "crown", "landmark", "trending",
        "fire", "trophy", "soccer", "dice", "diamond", "crypto"
    )

    // (gradientStart, gradientEnd, accentColor) cycled by row index when a row omits Icon/colors.
    private val PALETTE = listOf(
        Triple(0xFF7C3AEDL, 0xFF2A1065L, 0xFFC4B5FDL),
        Triple(0xFF059669L, 0xFF04331FL, 0xFF6EE7B7L),
        Triple(0xFFB45309L, 0xFF3A1D02L, 0xFFFCD34DL),
        Triple(0xFF1D4ED8L, 0xFF0A1A45L, 0xFF93C5FDL),
        Triple(0xFF0D9488L, 0xFF04302EL, 0xFF5EEAD4L),
        Triple(0xFF15803DL, 0xFF04260FL, 0xFF4ADE80L),
        Triple(0xFF2563EBL, 0xFF0A1C4DL, 0xFF93C5FDL),
        Triple(0xFF65A30DL, 0xFF1E2E05L, 0xFFBEF264L),
        Triple(0xFF0F766EL, 0xFF04211FL, 0xFF6EE7B7L),
        Triple(0xFFEA580CL, 0xFF3D1203L, 0xFFFDBA74L),
        Triple(0xFFDC2626L, 0xFF450A0AL, 0xFFF87171L),
        Triple(0xFF2563EBL, 0xFF172554L, 0xFF60A5FAL)
    )

    fun load(context: Context): List<SportsbookApp> {
        val lines = try {
            context.assets.open(FILE_NAME).bufferedReader().use { it.readLines() }
        } catch (e: Exception) {
            Log.w(TAG, "No $FILE_NAME found in assets, no referral data loaded.", e)
            return emptyList()
        }

        val rows = lines.filter { it.isNotBlank() }
        if (rows.size < 2) return emptyList()

        val header = parseCsvLine(rows.first()).map { it.trim().lowercase() }
        val usedIds = mutableSetOf<String>()

        return rows.drop(1).mapIndexedNotNull { index, line ->
            try {
                val cols = parseCsvLine(line)
                val row = header.zip(cols).toMap()
                buildApp(row, index, usedIds)
            } catch (e: Exception) {
                Log.w(TAG, "Skipping malformed row ${index + 2}", e)
                null
            }
        }
    }

    private fun buildApp(
        row: Map<String, String>,
        index: Int,
        usedIds: MutableSet<String>
    ): SportsbookApp? {
        val name = row.value("name")
        val link = row.value("referrallink").ifBlank { row.value("link") }
            .ifBlank { row.value("defaultref") }
        if (name.isBlank() || link.isBlank()) return null

        var id = row.value("id").ifBlank { name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') }
        if (id.isBlank()) id = "entry_$index"
        while (!usedIds.add(id)) {
            id = "${id}_$index"
        }

        val (defaultGradStart, defaultGradEnd, defaultAccent) = PALETTE[index % PALETTE.size]
        val icon = row.value("icon").ifBlank { ICONS[index % ICONS.size] }
        val payout = row.value("payout").ifBlank { "Bonus Available" }
        val promoCode = row.value("promocode").ifBlank { row.value("code") }
        val description = row.value("description").ifBlank {
            "Sign up with $name using this referral link to claim your bonus."
        }
        val tags = row.value("tags")
            .split(";", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("referral") }

        return SportsbookApp(
            id = id,
            name = name,
            iconType = icon,
            payout = payout,
            payoutVal = payout.filter { it.isDigit() }.take(4).toIntOrNull() ?: (50 + index),
            badge = row.value("badge").ifBlank { "🎁 Bonus" },
            rating = row.value("rating").ifBlank { "4.5" },
            reviews = row.value("reviews").ifBlank { "—" },
            defaultRef = link,
            promoCode = promoCode,
            description = description,
            tags = tags,
            gradientStart = defaultGradStart,
            gradientEnd = defaultGradEnd,
            accentColor = defaultAccent,
            region = row.value("region").ifBlank { "Global" },
            termsHighlight = row.value("termshighlight"),
            linkInstructions = row.value("linkinstructions")
        )
    }

    private fun Map<String, String>.value(key: String): String = (this[key] ?: "").trim()

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        result.add(sb.toString())
                        sb.clear()
                    }
                    else -> sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
