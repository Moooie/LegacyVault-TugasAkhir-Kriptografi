package id.upnyk.legacyvault.core.crypto

/** Utilitas hex dipakai lintas modul kripto. */
internal const val HEX_DIGITS = "0123456789abcdef"

fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(HEX_DIGITS[v ushr 4]).append(HEX_DIGITS[v and 0x0F])
    }
    return sb.toString()
}

fun String.hexToBytes(): ByteArray {
    require(length % 2 == 0) { "Panjang hex harus genap: $length" }
    val out = ByteArray(length / 2)
    for (i in out.indices) {
        val hi = HEX_DIGITS.indexOf(this[i * 2].lowercaseChar())
        val lo = HEX_DIGITS.indexOf(this[i * 2 + 1].lowercaseChar())
        require(hi >= 0 && lo >= 0) { "Karakter hex tidak valid pada indeks ${i * 2}" }
        out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
}

fun ByteArray.maskedHex(keep: Int = 8): String {
    val hex = toHex()
    return if (hex.length <= keep * 2) hex else "${hex.take(keep * 2)}… (${size} byte)"
}
