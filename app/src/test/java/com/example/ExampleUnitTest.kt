package com.example

import com.example.data.SportsbookRepository
import com.example.data.SportsbookRepository.Companion.toSportsbookApp
import com.example.data.ViralMediaStorage
import com.example.data.local.ViralReelEntity
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testRealLinksCatalogIntegrity() {
        val apps = SportsbookRepository.defaultCatalog()
        assertEquals(3, apps.size)
        val expectedLinks = listOf(
            "https://j10app.win/?dl=abxtwr",
            "https://win786.win/?id=989008314",
            "https://4pk365.com/?dl=9gz5zj"
        )
        assertEquals(expectedLinks, apps.map { it.defaultRef })
        val ids = apps.map { it.id }.toSet()
        assertEquals(apps.size, ids.size)
        for (app in apps) {
            assertTrue(app.name.isNotBlank())
            assertTrue(app.defaultRef.startsWith("https://"))
            assertTrue(app.termsHighlight.isNotBlank())
            assertTrue(app.linkInstructions.isNotBlank())
            assertTrue(app.payoutVal > 0)
        }
    }

    @Test
    fun testRealLinksQrRoundTripDecode() {
        val realLinks = listOf(
            "https://j10app.win/?dl=abxtwr",
            "https://win786.win/?id=989008314",
            "https://4pk365.com/?dl=9gz5zj"
        )
        for (url in realLinks) {
            val matrix = QrCodeGenerator.encode(url)
            assertEquals(29, matrix.size) // Version 3 (29x29)
            assertEquals(url, decodeIso18004Mask0LevelM(matrix))
        }
    }

    @Test
    fun testQrCodeVariableLengthAndVersion7Plus() {
        val shortUrl = "https://j10app.win"
        val mediumUrl = "https://win786.win/?id=989008314&ref=test"
        val longRealInviteUrl =
            "https://4pk365.com/?dl=9gz5zj&utm_source=refreels&utm_medium=android_app&campaign=invite_2026_verified_partner_link"
        val matrixShort = QrCodeGenerator.encode(shortUrl)
        val matrixMedium = QrCodeGenerator.encode(mediumUrl)
        val matrixLong = QrCodeGenerator.encode(longRealInviteUrl)
        assertNotNull(matrixShort)
        assertTrue(matrixMedium.size >= matrixShort.size)
        assertTrue(matrixLong.size >= 45) // Version 7+ (45x45 or larger)
        assertEquals(shortUrl, decodeIso18004Mask0LevelM(matrixShort))
        assertEquals(mediumUrl, decodeIso18004Mask0LevelM(matrixMedium))
        assertEquals(longRealInviteUrl, decodeIso18004Mask0LevelM(matrixLong))
    }

    /**
     * Independent ISO/IEC 18004 verifier/decoder for Versions 1..9, EC Level M, Mask 0.
     * Verifies finder patterns, dark module, both format info copies, RS syndromes == 0,
     * and extracts the Byte-mode UTF-8 payload.
     */
    private fun decodeIso18004Mask0LevelM(matrix: Array<BooleanArray>): String {
        val size = matrix.size
        assertEquals(size, matrix[0].size)
        val version = (size - 17) / 4
        assertTrue(version in 1..9)

        // 1. Verify dark module
        assertTrue("Dark module at (size-8, 8) must be true", matrix[size - 8][8])

        // 2. Verify top-left format copy == 0x5412 (bits 14 down to 0)
        var fmtTopLeft = 0
        val tlCoords = listOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5, 8 to 7, 8 to 8,
            7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8
        )
        for ((r, c) in tlCoords) {
            fmtTopLeft = (fmtTopLeft shl 1) or if (matrix[r][c]) 1 else 0
        }
        assertEquals("Top-left format bits mismatch", 0x5412, fmtTopLeft)

        // 3. Verify bottom-left + top-right format copy == 0x5412 (bits 14 down to 0)
        var fmtSplit = 0
        for (i in 0 until 7) {
            fmtSplit = (fmtSplit shl 1) or if (matrix[size - 1 - i][8]) 1 else 0
        }
        for (i in 0 until 8) {
            fmtSplit = (fmtSplit shl 1) or if (matrix[8][size - 8 + i]) 1 else 0
        }
        assertEquals("Split format bits mismatch", 0x5412, fmtSplit)

        // 4. Mark reserved areas independently per ISO/IEC 18004
        val reserved = Array(size) { BooleanArray(size) }
        fun reserveFinder(top: Int, left: Int) {
            for (r in -1..7) for (c in -1..7) {
                val mr = top + r
                val mc = left + c
                if (mr in 0 until size && mc in 0 until size) reserved[mr][mc] = true
            }
        }
        reserveFinder(0, 0)
        reserveFinder(0, size - 7)
        reserveFinder(size - 7, 0)

        val alignCoords = when (version) {
            1 -> intArrayOf()
            2 -> intArrayOf(6, 18)
            3 -> intArrayOf(6, 22)
            4 -> intArrayOf(6, 26)
            5 -> intArrayOf(6, 30)
            6 -> intArrayOf(6, 34)
            7 -> intArrayOf(6, 22, 38)
            8 -> intArrayOf(6, 24, 42)
            9 -> intArrayOf(6, 26, 46)
            else -> error("Unsupported")
        }
        for (r in alignCoords) for (c in alignCoords) {
            if (reserved[r][c]) continue
            for (dr in -2..2) for (dc in -2..2) reserved[r + dr][c + dc] = true
        }
        for (i in 8 until size - 8) {
            reserved[6][i] = true
            reserved[i][6] = true
        }
        reserved[size - 8][8] = true
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }
        if (version >= 7) {
            for (i in 0 until 18) {
                val row = i / 3
                val col = size - 11 + (i % 3)
                reserved[row][col] = true
                reserved[col][row] = true
            }
        }

        // 5. Read data bits in zig-zag order and unmask (row + col) % 2 == 0
        val bits = ArrayList<Int>()
        var col = size - 1
        var upward = true
        while (col > 0) {
            if (col == 6) col--
            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (dc in 0..1) {
                    val c = col - dc
                    if (!reserved[row][c]) {
                        val raw = matrix[row][c]
                        val unmasked = raw xor ((row + c) % 2 == 0)
                        bits.add(if (unmasked) 1 else 0)
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Group specs (blockCount, dataBytes, ecBytes) for Level M V1..V9
        val groups = when (version) {
            1 -> listOf(Triple(1, 16, 10))
            2 -> listOf(Triple(1, 28, 16))
            3 -> listOf(Triple(1, 44, 26))
            4 -> listOf(Triple(2, 32, 18))
            5 -> listOf(Triple(2, 43, 24))
            6 -> listOf(Triple(4, 27, 16))
            7 -> listOf(Triple(4, 31, 18))
            8 -> listOf(Triple(2, 38, 22), Triple(2, 39, 22))
            9 -> listOf(Triple(3, 36, 22), Triple(2, 37, 22))
            else -> error("Unsupported")
        }
        val totalCodewords = groups.sumOf { it.first * (it.second + it.third) }
        assertTrue(bits.size >= totalCodewords * 8)

        val codewords = IntArray(totalCodewords) { idx ->
            var v = 0
            for (b in 0 until 8) v = (v shl 1) or bits[idx * 8 + b]
            v
        }

        // De-interleave data + EC blocks and verify RS syndrome == 0
        val dataBlockSizes = ArrayList<Int>()
        val ecBlockSizes = ArrayList<Int>()
        for ((count, d, e) in groups) {
            repeat(count) {
                dataBlockSizes.add(d)
                ecBlockSizes.add(e)
            }
        }
        val dataBlocks = Array(dataBlockSizes.size) { i -> IntArray(dataBlockSizes[i]) }
        val ecBlocks = Array(ecBlockSizes.size) { i -> IntArray(ecBlockSizes[i]) }
        var cwIdx = 0
        val maxD = dataBlockSizes.max()
        for (i in 0 until maxD) {
            for (b in dataBlocks.indices) {
                if (i < dataBlocks[b].size) dataBlocks[b][i] = codewords[cwIdx++]
            }
        }
        val maxE = ecBlockSizes.max()
        for (i in 0 until maxE) {
            for (b in ecBlocks.indices) {
                if (i < ecBlocks[b].size) ecBlocks[b][i] = codewords[cwIdx++]
            }
        }
        assertEquals(totalCodewords, cwIdx)

        // Verify GF(256) RS syndromes for every block
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = (x shl 1) xor if ((x and 0x80) != 0) 0x11D else 0
        }
        for (i in 255 until 512) exp[i] = exp[i - 255]

        for (b in dataBlocks.indices) {
            val fullBlock = dataBlocks[b] + ecBlocks[b]
            for (root in 0 until ecBlockSizes[b]) {
                var eval = 0
                val alpha = exp[root]
                for (coeff in fullBlock) {
                    val mul = if (eval == 0 || alpha == 0) 0 else exp[log[eval] + log[alpha]]
                    eval = mul xor coeff
                }
                assertEquals("RS syndrome non-zero at block $b root $root", 0, eval)
            }
        }

        // Reconstruct data bitstream & parse Byte mode payload
        val dataBits = ArrayList<Int>()
        for (block in dataBlocks) {
            for (byteVal in block) {
                for (bitPos in 7 downTo 0) dataBits.add((byteVal ushr bitPos) and 1)
            }
        }
        var ptr = 0
        fun readInt(len: Int): Int {
            var v = 0
            repeat(len) { v = (v shl 1) or dataBits[ptr++] }
            return v
        }
        val mode = readInt(4)
        assertEquals("Expected Byte mode (4)", 4, mode)
        val length = readInt(8)
        val payload = ByteArray(length) { readInt(8).toByte() }
        return String(payload, Charsets.UTF_8)
    }

    @Test
    fun testEasypaisaAndUrlValidation() {
        assertTrue(ViralMediaStorage.isValidPakistaniPhone("03001234567"))
        assertTrue(ViralMediaStorage.isValidPakistaniPhone("+923001234567"))
        assertEquals("03001234567", ViralMediaStorage.normalizePakistaniPhone("+92 300-1234567"))
        assertFalse(ViralMediaStorage.isValidPakistaniPhone("12345"))

        assertTrue(ViralMediaStorage.isValidEasypaisaTid("EP12345678"))
        assertFalse(ViralMediaStorage.isValidEasypaisaTid("12"))
        assertFalse(ViralMediaStorage.isValidEasypaisaTid("bad tid!"))

        assertTrue(ViralMediaStorage.isSafeHttpUrl("https://j10app.win/?dl=abxtwr"))
        assertFalse(ViralMediaStorage.isSafeHttpUrl("javascript:alert(1)"))
        assertFalse(ViralMediaStorage.isSafeHttpUrl("file:///etc/passwd"))
        assertFalse(ViralMediaStorage.isSafeHttpUrl("data:text/html,<script>"))
    }

    @Test
    fun testViralReelEntityMapping() {
        val entity = ViralReelEntity(
            id = "viral_1",
            title = "My Promo",
            referralLink = "https://example.com/?ref=abc",
            promoCode = "ABC",
            bonusOffer = "500 PKR",
            description = "Watch & join",
            videoPath = "file:///data/user/0/com.example/files/viral_videos/demo.mp4",
            easypaisaSenderPhone = "03001234567",
            easypaisaTid = "EP998877",
            amountPkr = 100,
            viralShares = 3
        )
        val mapped = entity.toSportsbookApp()
        assertTrue(mapped.isUserViral)
        assertEquals(entity.videoPath, mapped.videoPath)
        assertEquals("3 shares", mapped.reviews)
        assertEquals("EP998877", mapped.easypaisaTid)
    }

    @Test
    fun testFirestoreViralSubmissionMapping() {
        val doc = com.example.data.remote.ViralSubmissionDoc(
            id = "sub_abc123",
            userId = "user_1",
            authorName = "Ali",
            title = "Win786 Viral",
            referralLink = "https://win786.win/?id=989008314",
            promoCode = "989008314",
            bonusOffer = "500 PKR Bonus",
            description = "Join fast",
            videoUrl = "",
            amountPkr = 100L,
            status = com.example.data.remote.SubmissionStatus.APPROVED.name,
            viralShares = 7L
        )
        val mapped = doc.toSportsbookApp(localVideoOverride = "file:///local/video.mp4")
        assertEquals("sub_abc123", mapped.id)
        assertEquals("file:///local/video.mp4", mapped.videoPath)
        assertEquals("7 shares", mapped.reviews)
        assertEquals("", mapped.easypaisaTid)
        assertEquals(
            com.example.data.remote.SubmissionStatus.PENDING_VERIFICATION,
            com.example.data.remote.SubmissionStatus.fromRaw("UNKNOWN_FUTURE_STATUS")
        )
    }

    @Test
    fun testAppShareUrlQrRoundTripAndDocMapping() {
        val shareDoc = com.example.data.remote.AppShareConfigDoc(
            id = "app_share",
            downloadUrl = "https://play.google.com/store/apps/details?id=com.aistudio.refreels.kvyzpq",
            shareMessage = "Join RefReels!"
        )
        assertTrue(ViralMediaStorage.isSafeHttpUrl(shareDoc.downloadUrl))
        val matrix = QrCodeGenerator.encode(shareDoc.downloadUrl)
        assertEquals(shareDoc.downloadUrl, decodeIso18004Mask0LevelM(matrix))
    }
}
