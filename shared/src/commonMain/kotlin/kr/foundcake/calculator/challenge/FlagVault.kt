package kr.foundcake.calculator.challenge

object FlagVault {
    internal val encryptedPartA = GeneratedChallengeMaterial.encryptedPartA

    internal val encryptedPartB = GeneratedChallengeMaterial.encryptedPartB

    private val saltFragments = listOf(
        intArrayOf(115, 57, 64),
        intArrayOf(75, 33, 50),
        intArrayOf(109, 80, 35, 105, 111, 115),
    )

    private val byteMixer: List<(Int, Int, Int) -> Int> = listOf(
        { i, v, n -> (v xor ((i * 19 + n) and 0xFF)) and 0xFF },
        { i, v, n -> (v + (i * 7) + n) and 0xFF },
        { i, v, n -> (v xor (n * 3 + i)) and 0xFF }
    )

    fun reveal(canonical: String): String? {
        return decrypt(canonical, encryptedPartA + encryptedPartB)
    }

    fun deriveKey(canonical: String, salt: String = salt(), size: Int): ByteArray {
        val seed = ("$canonical|$salt").encodeToByteArray()
        val foldedSeed = seed.foldIndexed(0x5A) { index, acc, b ->
            val byteValue = b.toInt() and 0xFF
            byteMixer[index % byteMixer.size](index, acc, byteValue)
        }

        var acc = foldedSeed
        return ByteArray(size) { index ->
            val left = seed[index % seed.size].toInt() and 0xFF
            val right = seed[(index * 7) % seed.size].toInt() and 0xFF
            acc = (acc * 131 + left + index * 17) and 0xFF
            ((acc xor ((index * 31 + right) and 0xFF)) and 0xFF).toByte()
        }
    }

    private fun mask(index: Int): Int = (index * 29 + 17) and 0xFF

    private fun salt(): String = saltFragments
        .flatMap { it.asIterable() }
        .joinToString(separator = "") { code -> code.toChar().toString() }

    private fun looksLikeFlag(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val end = 125
        return (bytes.last().toInt() and 0xFF) == end && checksum(bytes) == GeneratedChallengeMaterial.plainCheck
    }

    private fun checksum(bytes: ByteArray): Int {
        var acc = 0x7A452F1B
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            acc = ((acc xor (value + index * 257)) * 16777619) + index
        }
        return acc
    }

    internal fun decrypt(canonical: String, encrypted: IntArray): String? {
        val key = deriveKey(canonical, salt(), encrypted.size)
        val plainBytes = ByteArray(encrypted.size)

        encrypted.forEachIndexed { idx, byte ->
            val transformed = byte xor key[idx].toInt().and(0xFF) xor mask(idx)
            plainBytes[idx] = transformed.toByte()
        }

        if (!looksLikeFlag(plainBytes)) return null
        return plainBytes.decodeToString()
    }

    fun encryptedSize(): Int = encryptedPartA.size + encryptedPartB.size
}
