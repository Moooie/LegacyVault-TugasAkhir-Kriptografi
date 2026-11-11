package id.upnyk.legacyvault.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.upnyk.legacyvault.ui.VaultViewModel
import id.upnyk.legacyvault.ui.components.Eyebrow
import id.upnyk.legacyvault.ui.components.Hairline
import id.upnyk.legacyvault.ui.components.InfoLine
import id.upnyk.legacyvault.ui.components.PillTone
import id.upnyk.legacyvault.ui.components.VaultButton
import id.upnyk.legacyvault.ui.components.VaultOutlineButton
import id.upnyk.legacyvault.ui.components.VaultScaffold
import id.upnyk.legacyvault.ui.components.VaultSpacing
import id.upnyk.legacyvault.ui.components.VaultTextField
import id.upnyk.legacyvault.ui.theme.VaultBrand
import id.upnyk.legacyvault.ui.theme.VaultError
import id.upnyk.legacyvault.ui.theme.VaultText
import id.upnyk.legacyvault.ui.theme.VaultTextFaint
import id.upnyk.legacyvault.ui.theme.VaultTextMuted

/**
 * STATE_3 (AUTH_LOGIN) — Kriteria 2.
 * Password di-hash SHA-256 + salt 16 byte; sesi disimpan sebagai AES-CBC di SharedPreferences.
 * Hash mentah tidak ditampilkan di layar; buka ikon info di pojok kanan atas.
 */
@Composable
fun AuthScreen(vm: VaultViewModel) {
    val auth = vm.auth
    val hasUsers = auth.existingUsers > 0

    VaultScaffold(
        title = "Autentikasi",
        subtitle = "Kriteria 2 · SHA-256 + salt 16 byte",
        onLock = vm::lockVault,
        onInfo = vm::reopenTrace,
    ) {
        Eyebrow("Langkah 3 dari 3 · kredensial")
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasUsers) "Masuk brankas" else "Buat akun pertama",
            style = MaterialTheme.typography.headlineLarge,
            color = VaultText,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            if (hasUsers) "${auth.existingUsers} akun terdaftar di perangkat ini."
            else "Belum ada akun. Akun pertama menjadi pemilik brankas.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextMuted,
        )

        Spacer(Modifier.height(VaultSpacing.lg))
        VaultTextField(
            value = auth.username,
            onValueChange = vm::onUsernameChange,
            label = "Username",
            placeholder = "notaris_utama",
        )
        Spacer(Modifier.height(VaultSpacing.sm))
        VaultTextField(
            value = auth.password,
            onValueChange = vm::onPasswordChange,
            label = "Password",
            placeholder = "minimal 6 karakter",
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(Modifier.height(VaultSpacing.md))
        Text("Peran", style = MaterialTheme.typography.bodySmall, color = VaultTextFaint)
        Spacer(Modifier.height(6.dp))
        RoleSelector(current = auth.role, onSelect = vm::onRoleChange)
        Spacer(Modifier.height(4.dp))
        Text(
            "Peran disimpan saat registrasi akun baru.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextFaint,
        )

        Spacer(Modifier.height(VaultSpacing.lg))
        // Satu CTA terisi: registrasi saat belum ada akun, login setelahnya.
        if (hasUsers) {
            VaultButton(
                text = "Masuk brankas",
                onClick = vm::login,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Daftarkan akun baru",
                onClick = vm::register,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            VaultButton(
                text = "Daftarkan akun baru",
                onClick = vm::register,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(VaultSpacing.sm))
            VaultOutlineButton(
                text = "Sudah punya akun? Masuk",
                onClick = vm::login,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (auth.message.isNotBlank()) {
            Spacer(Modifier.height(VaultSpacing.md))
            InfoLine(auth.message, if (auth.isError) PillTone.ERROR else PillTone.OK)
        }

        Spacer(Modifier.height(VaultSpacing.lg))
        Hairline()
        Spacer(Modifier.height(VaultSpacing.sm))
        Text(
            "Password tidak pernah disimpan sebagai plaintext. Token sesi di disk selalu ciphertext " +
                "AES/CBC/PKCS5Padding.",
            style = MaterialTheme.typography.bodySmall,
            color = VaultTextMuted,
        )
    }
}

@Composable
private fun RoleSelector(current: String, onSelect: (String) -> Unit) {
    val roles = listOf("NOTARIS" to "Notaris", "PEWARIS" to "Pewaris", "AHLI_WARIS" to "Ahli waris")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultText.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        roles.forEach { (value, label) ->
            val selected = current == value
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else VaultTextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) VaultBrand else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
            )
        }
    }
}
