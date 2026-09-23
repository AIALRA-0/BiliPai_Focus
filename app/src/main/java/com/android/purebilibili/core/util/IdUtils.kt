package com.android.purebilibili.core.util

/**
 * Bilibili ID 转换工具（AV 号与 BV 号相互转换）
 */
object IdUtils {
    private const val XOR_CODE = 23442827791579L
    private const val MASK_CODE = 2251799813685247L
    private const val MAX_AID = 1L shl 51
    private const val BASE = 58L

    private const val DATA = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf"
    private val INV_DATA by lazy {
        DATA.mapIndexed { index, c -> c to index }.toMap()
    }

    private fun swap(chars: CharArray, idx1: Int, idx2: Int) {
        val temp = chars[idx1]
        chars[idx1] = chars[idx2]
        chars[idx2] = temp
    }

    /**
     * 将 aid 转为 bvid
     */
    fun av2bv(aid: Long): String {
        if (aid <= 0L) return ""
        val chars = CharArray(12) { '0' }
        chars[0] = 'B'
        chars[1] = 'V'
        chars[2] = '1'

        var bvIndex = chars.size - 1
        var tmp = (MAX_AID or aid) xor XOR_CODE
        while (tmp > 0) {
            chars[bvIndex--] = DATA[(tmp % BASE).toInt()]
            tmp /= BASE
        }

        swap(chars, 3, 9)
        swap(chars, 4, 7)

        return String(chars)
    }

    /**
     * 将 bvid 转为 aid
     */
    fun bv2av(bvid: String): Long {
        if (bvid.length < 12 || !bvid.startsWith("BV1", ignoreCase = true)) return 0L
        val chars = bvid.toCharArray()
        swap(chars, 3, 9)
        swap(chars, 4, 7)

        var tmp = 0L
        for (i in 3 until chars.size) {
            val idx = INV_DATA[chars[i]] ?: return 0L
            tmp = tmp * BASE + idx
        }

        return (tmp and MASK_CODE) xor XOR_CODE
    }
}
