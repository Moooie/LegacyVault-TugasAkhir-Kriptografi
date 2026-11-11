package id.upnyk.legacyvault.core.session

import android.content.Context
import android.content.SharedPreferences
import id.upnyk.legacyvault.core.crypto.AuthLoginCrypto

/**
 * Kriteria 2 — penyimpanan token sesi.
 * Isi sesi TIDAK pernah disimpan sebagai plaintext; selalu `HEX(IV):BASE64(ciphertext)` AES-CBC.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("legacyvault_session", Context.MODE_PRIVATE)

    data class StoredSession(
        val username: String,
        val role: String,
        val issuedAt: Long,
    )

    fun save(session: StoredSession, passphrase: String): String {
        val json = """{"username":"${session.username}","role":"${session.role}","issuedAt":${session.issuedAt}}"""
        val ticket = AuthLoginCrypto.encryptSession(json, passphrase)
        prefs.edit()
            .putString(KEY_TICKET, ticket.serialize())
            .putString(KEY_USERNAME, session.username)
            .apply()
        return ticket.serialize()
    }

    fun rawTicket(): String? = prefs.getString(KEY_TICKET, null)

    fun lastUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun readSession(passphrase: String): String? {
        val raw = rawTicket() ?: return null
        return runCatching { AuthLoginCrypto.decryptSession(raw, passphrase) }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TICKET = "session_ticket"
        const val KEY_USERNAME = "session_username"
    }
}
