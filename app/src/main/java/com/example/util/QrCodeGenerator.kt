package com.example.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Pure Kotlin QR Code matrix and Bitmap generator.
 * Implements ISO/IEC 18004 Versions 1–9 (Byte mode, Error Correction Level M)
 * with exact multi-block Reed-Solomon interleaving and version information bits for V7–V9.
 */
object QrCodeGenerator {

    private val exp = IntArray(512)
    private val log = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            log[x] = i
            x = (x shl 1) xor if ((x and 0x80) != 0) 0x11D else 0
        }
        for (i in 255 until 512) {
            exp[i] = exp[i - 255]
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return exp[log[x] + log[y]]
    }

    private fun rsPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val factor = exp[i]
            for (j in poly.indices) {
                next[j] = next[j] xor gfMul(poly[j], factor)
                next[j + 1] = next[j + 1] xor poly[j]
            }
            poly = next
        }
        return poly
    }

    private fun rsEncode(data: ByteArray, ecCount: Int): ByteArray {
        val poly = rsPoly(ecCount)
        val remainder = IntArray(ecCount)
        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor remainder[0]
            System.arraycopy(remainder, 1, remainder, 0, ecCount - 1)
            remainder[ecCount - 1] = 0
            if (factor != 0) {
                for (j in 0 until ecCount) {
                    remainder[j] = remainder[j] xor gfMul(poly[poly.size - 2 - j], factor)
                }
            }
        }
        return ByteArray(ecCount) { i -> remainder[i].toByte() }
    }

    private data class RsBlockGroup(
        val blockCount: Int,
        val dataBytesPerBlock: Int,
        val ecBytesPerBlock: Int
    )

    private data class QrVersion(
        val version: Int,
        val groups: List<RsBlockGroup>,
        val alignPos: IntArray,
        val remainderBits: Int = 0,
        val versionInfoBits: Int = 0
    ) {
        val size: Int get() = 17 + 4 * version
        val totalDataBytes: Int get() = groups.sumOf { it.blockCount * it.dataBytesPerBlock }
    }

    // ISO/IEC 18004 Error Correction Level M (Versions 1 to 9, supports up to ~179 UTF-8 bytes)
    private val VERSIONS = listOf(
        QrVersion(1, listOf(RsBlockGroup(1, 16, 10)), intArrayOf()),
        QrVersion(2, listOf(RsBlockGroup(1, 28, 16)), intArrayOf(6, 18), remainderBits = 7),
        QrVersion(3, listOf(RsBlockGroup(1, 44, 26)), intArrayOf(6, 22), remainderBits = 7),
        QrVersion(4, listOf(RsBlockGroup(2, 32, 18)), intArrayOf(6, 26), remainderBits = 7),
        QrVersion(5, listOf(RsBlockGroup(2, 43, 24)), intArrayOf(6, 30), remainderBits = 7),
        QrVersion(6, listOf(RsBlockGroup(4, 27, 16)), intArrayOf(6, 34), remainderBits = 7),
        QrVersion(7, listOf(RsBlockGroup(4, 31, 18)), intArrayOf(6, 22, 38), versionInfoBits = 0x07C94),
        QrVersion(8, listOf(RsBlockGroup(2, 38, 22), RsBlockGroup(2, 39, 22)), intArrayOf(6, 24, 42), versionInfoBits = 0x085BC),
        QrVersion(9, listOf(RsBlockGroup(3, 36, 22), RsBlockGroup(2, 37, 22)), intArrayOf(6, 26, 46), versionInfoBits = 0x09A99)
    )

    fun encode(text: String): Array<BooleanArray> {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val maxSupported = VERSIONS.last().totalDataBytes - 2
        val textBytes = if (rawBytes.size > maxSupported) {
            rawBytes.copyOf(maxSupported)
        } else {
            rawBytes
        }

        // Byte mode header for V1-V9: 4-bit mode + 8-bit char count = 12 bits (~2 bytes overhead)
        val ver = VERSIONS.firstOrNull { it.totalDataBytes * 8 >= 4 + 8 + textBytes.size * 8 }
            ?: VERSIONS.last()
        val size = ver.size

        // Build data bitstream
        val bits = BitBuffer()
        bits.append(0x4, 4) // Byte mode
        bits.append(textBytes.size, 8)
        for (b in textBytes) {
            bits.append(b.toInt() and 0xFF, 8)
        }
        val maxDataBits = ver.totalDataBytes * 8
        val termLen = minOf(4, maxDataBits - bits.bitLength)
        bits.append(0, termLen)
        val pad = (8 - (bits.bitLength % 8)) % 8
        bits.append(0, pad)

        var toggle = 0xEC
        while (bits.bitLength < maxDataBits) {
            bits.append(toggle, 8)
            toggle = if (toggle == 0xEC) 0x11 else 0xEC
        }

        val rawData = bits.toByteArray()

        // Split into RS blocks & compute EC blocks
        val dataBlocks = ArrayList<ByteArray>()
        val ecBlocks = ArrayList<ByteArray>()
        var offset = 0
        for (group in ver.groups) {
            for (b in 0 until group.blockCount) {
                val block = rawData.copyOfRange(offset, offset + group.dataBytesPerBlock)
                offset += group.dataBytesPerBlock
                dataBlocks.add(block)
                ecBlocks.add(rsEncode(block, group.ecBytesPerBlock))
            }
        }

        // Interleave data codewords, then EC codewords
        val interleaved = ArrayList<Byte>()
        val maxDataLen = dataBlocks.maxOf { it.size }
        for (i in 0 until maxDataLen) {
            for (block in dataBlocks) {
                if (i < block.size) interleaved.add(block[i])
            }
        }
        val maxEcLen = ecBlocks.maxOf { it.size }
        for (i in 0 until maxEcLen) {
            for (block in ecBlocks) {
                if (i < block.size) interleaved.add(block[i])
            }
        }

        // Matrix initialization
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, v: Boolean) {
            matrix[r][c] = v
            reserved[r][c] = true
        }

        // 1. Finder patterns (7x7) + Separators
        fun drawFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val mr = top + r
                    val mc = left + c
                    if (mr in 0 until size && mc in 0 until size) {
                        val isBlack = (r in 0..6 && (c == 0 || c == 6)) ||
                            (c in 0..6 && (r == 0 || r == 6)) ||
                            (r in 2..4 && c in 2..4)
                        setModule(mr, mc, isBlack)
                    }
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // 2. Alignment patterns for Version >= 2
        for (r in ver.alignPos) {
            for (c in ver.alignPos) {
                if (reserved[r][c]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val isBlack = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc)) != 1
                        setModule(r + dr, c + dc, isBlack)
                    }
                }
            }
        }

        // 3. Timing patterns
        for (i in 8 until size - 8) {
            val v = (i % 2 == 0)
            if (!reserved[6][i]) setModule(6, i, v)
            if (!reserved[i][6]) setModule(i, 6, v)
        }

        // 4. Dark module
        setModule(size - 8, 8, true)

        // 5. Reserve format info areas
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in 0..7) {
            reserved[8][size - 1 - i] = true
            reserved[size - 1 - i][8] = true
        }

        // 6. Version information for Version >= 7
        if (ver.version >= 7) {
            for (i in 0 until 18) {
                val bit = ((ver.versionInfoBits ushr i) and 1) != 0
                val row = i / 3
                val col = size - 11 + (i % 3)
                setModule(row, col, bit)
                setModule(col, row, bit)
            }
        }

        // 7. Place interleaved data + remainder bits using Mask 0: (row + col) % 2 == 0
        val allBits = BitBuffer()
        for (b in interleaved) {
            allBits.append(b.toInt() and 0xFF, 8)
        }
        repeat(ver.remainderBits) {
            allBits.append(0, 1)
        }

        var bitIdx = 0
        var col = size - 1
        var upward = true
        while (col > 0) {
            if (col == 6) col--
            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (dc in 0..1) {
                    val c = col - dc
                    if (!reserved[row][c]) {
                        val bit = if (bitIdx < allBits.bitLength) allBits.get(bitIdx++) else false
                        val mask = (row + c) % 2 == 0
                        matrix[row][c] = bit xor mask
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 8. Format information for Level M (00), Mask 0 (000) -> 0x5412
        val formatBits = 0x5412
        for (i in 0 until 15) {
            val bit = ((formatBits ushr (14 - i)) and 1) != 0
            when {
                i < 6 -> matrix[8][i] = bit
                i == 6 -> matrix[8][7] = bit
                i == 7 -> matrix[8][8] = bit
                i == 8 -> matrix[7][8] = bit
                else -> matrix[14 - i][8] = bit
            }
            if (i < 7) {
                matrix[size - 1 - i][8] = bit
            } else {
                matrix[8][size - 15 + i] = bit
            }
        }

        return matrix
    }

    fun generateBitmap(
        text: String,
        pixelSize: Int = 512,
        darkColor: Color = Color(0xFF05070A),
        lightColor: Color = Color.White
    ): Bitmap {
        val matrix = encode(text)
        val matrixSize = matrix.size
        val border = 4
        val fullSize = matrixSize + border * 2
        val scale = maxOf(1, pixelSize / fullSize)
        val bmpWidth = fullSize * scale
        val bitmap = Bitmap.createBitmap(bmpWidth, bmpWidth, Bitmap.Config.ARGB_8888)

        val dark = darkColor.toArgb()
        val light = lightColor.toArgb()

        for (x in 0 until bmpWidth) {
            for (y in 0 until bmpWidth) {
                val mx = (x / scale) - border
                val my = (y / scale) - border
                val isDark = if (mx in 0 until matrixSize && my in 0 until matrixSize) {
                    matrix[my][mx]
                } else {
                    false
                }
                bitmap.setPixel(x, y, if (isDark) dark else light)
            }
        }
        return bitmap
    }

    private class BitBuffer {
        val list = ArrayList<Boolean>()
        val bitLength: Int get() = list.size

        fun append(value: Int, len: Int) {
            for (i in len - 1 downTo 0) {
                list.add(((value ushr i) and 1) != 0)
            }
        }

        fun get(index: Int): Boolean = list[index]

        fun toByteArray(): ByteArray {
            val bytes = ByteArray((list.size + 7) / 8)
            for (i in list.indices) {
                if (list[i]) {
                    bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
                }
            }
            return bytes
        }
    }
}
