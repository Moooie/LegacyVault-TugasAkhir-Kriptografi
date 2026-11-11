package id.upnyk.legacyvault.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.upnyk.legacyvault.BuildConfig
import id.upnyk.legacyvault.core.crypto.AuthLoginCrypto
import id.upnyk.legacyvault.core.crypto.Db3DesEcbCipher
import id.upnyk.legacyvault.core.crypto.DongleHmacVerifier
import id.upnyk.legacyvault.core.crypto.LsbSteganographyEngine
import id.upnyk.legacyvault.core.crypto.Rc4FileCipher
import id.upnyk.legacyvault.core.crypto.SimpleBlockchainEngine
import id.upnyk.legacyvault.core.crypto.SuperTextCipher
import id.upnyk.legacyvault.core.crypto.toHex
import id.upnyk.legacyvault.core.database.AppDatabase
import id.upnyk.legacyvault.core.database.entity.BlockchainBlockEntity
import id.upnyk.legacyvault.core.database.entity.CriticalNoteEntity
import id.upnyk.legacyvault.core.database.entity.EncryptedFileEntity
import id.upnyk.legacyvault.core.database.entity.StegoImageEntity
import id.upnyk.legacyvault.core.database.entity.UserEntity
import id.upnyk.legacyvault.core.database.entity.WillMessageEntity
import id.upnyk.legacyvault.core.hardware.DongleGate
import id.upnyk.legacyvault.core.hardware.DongleStorageManager
import id.upnyk.legacyvault.core.hardware.UsbDongleReceiver
import id.upnyk.legacyvault.core.session.SessionStore
import id.upnyk.legacyvault.ui.components.CipherTrace
import id.upnyk.legacyvault.ui.components.InspectorRow
import id.upnyk.legacyvault.ui.components.PillTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class GateStage { DONGLE_LOCKED, BIOMETRIC_LOCKED, AUTH_LOGIN, VAULT_UNLOCKED }

data class GateState(
    val stage: GateStage = GateStage.DONGLE_LOCKED,
    val usb: UsbDongleReceiver.UsbState = UsbDongleReceiver.UsbState(),
    val simulationEnabled: Boolean = BuildConfig.DEBUG,
    val simulationUsed: Boolean = false,
    val partitionLabel: String = "Belum ada lokasi kunci dipilih",
    val volumeId: String = "",
    val volumeRemovable: Boolean = false,
    val dongleVerified: Boolean = false,
    val dongleMessage: String = "Colokkan dongle, lalu pilih lokasi berkas kunci di dalamnya.",
    val dongleToken: String = "",
    val expectedHmac: String = "",
    val fileHmac: String = "",
    val biometricAvailable: Boolean = false,
    val biometricLabel: String = "Memeriksa sensor…",
    val biometricPassed: Boolean = false,
    val biometricProof: String = "",
    val biometricMessage: String = "",
    val busy: Boolean = false,
) {
    /** Lapis 1 lolos: media USB mass storage tercolok DAN kunci valid dari volume eksternal. */
    val hardwareOk: Boolean
        get() = usb.isMassStorage || simulationUsed

    val dongleOk: Boolean
        get() = (dongleVerified && hardwareOk) || simulationUsed

    /** Gerbang lolos jika dongle valid (atau simulasi debug) DAN biometrik sukses. */
    val canAdvanceToLogin: Boolean
        get() = biometricPassed && dongleOk
}


data class AuthState(
    val username: String = "",
    val password: String = "",
    val role: String = "NOTARIS",
    val existingUsers: Int = 0,
    val message: String = "",
    val isError: Boolean = false,
    val saltHex: String = "",
    val hashHex: String = "",
    val sessionPayload: String = "",
)

data class WillUiState(
    val title: String = "",
    val plaintext: String = "",
    val vigenereKey: String = "",
    val passphrase: String = "",
    val busy: Boolean = false,
    val message: String = "",
    val isError: Boolean = false,
    val decryptTarget: WillMessageEntity? = null,
    val decryptVigenereKey: String = "",
    val decryptPassphrase: String = "",
    val decryptedText: String = "",
)

data class FileUiState(
    val pickedUri: Uri? = null,
    val pickedName: String = "",
    val pickedSize: Long = 0,
    val passphrase: String = "",
    val busy: Boolean = false,
    val message: String = "",
    val isError: Boolean = false,
    val decryptTarget: EncryptedFileEntity? = null,
    val decryptPassphrase: String = "",
    val decryptedText: String = "",
    val decryptedPath: String = "",
    val decryptedMime: String = "",
    val integrityNote: String = "",
)

data class NoteUiState(
    val label: String = "",
    val category: String = "",
    val accountOrPin: String = "",
    val secretNote: String = "",
    val passphrase: String = "",
    val busy: Boolean = false,
    val message: String = "",
    val isError: Boolean = false,
    val revealed: Map<Int, Triple<String, String, String>> = emptyMap(),
)

data class StegoUiState(
    val title: String = "",
    val secretMessage: String = "",
    val stegoKey: String = "",
    val coverUri: Uri? = null,
    val coverName: String = "",
    val coverBitmap: Bitmap? = null,
    val resultBitmap: Bitmap? = null,
    val resultPath: String = "",
    val mse: Double = 0.0,
    val psnr: Double = 0.0,
    val payloadBits: Int = 0,
    val busy: Boolean = false,
    val message: String = "",
    val isError: Boolean = false,
    val extractTarget: StegoImageEntity? = null,
    val extractKey: String = "",
    val extractedText: String = "",
)

data class ChainUiState(
    val assetTitle: String = "",
    val beneficiaryName: String = "",
    val documentSha256: String = "",
    val documentSource: String = "Input manual",
    val mining: Boolean = false,
    val attempts: Long = 0,
    val message: String = "",
    val isError: Boolean = false,
    val verification: SimpleBlockchainEngine.Verification? = null,
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val sessionStore = SessionStore(application)
    private val dongleStorage = DongleStorageManager(application)
    private val vaultDir = File(application.filesDir, "vault").apply { mkdirs() }

    private val _usb = UsbDongleReceiver.state
    val usbState: kotlinx.coroutines.flow.StateFlow<UsbDongleReceiver.UsbState> = _usb

    fun onUsbChanged(state: UsbDongleReceiver.UsbState) {
        if (gate.usb == state) return
        val wasStorage = gate.usb.isMassStorage
        gate = gate.copy(usb = state)
        // Dongle dicabut saat lapis 1 sudah lolos: kunci brankas lagi.
        if (wasStorage && !state.isMassStorage) {
            val verifiedBefore = gate.dongleVerified
            gate = gate.copy(
                dongleVerified = false,
                dongleMessage = "Dongle dilepas — lapis 1 terkunci kembali.",
            )
            if (gate.stage != GateStage.DONGLE_LOCKED || verifiedBefore) {
                lockVault()
                gate = gate.copy(
                    usb = state,
                    dongleMessage = "Dongle dilepas — lapis 1 terkunci kembali.",
                )
            }
        }
    }

    var gate by mutableStateOf(GateState())
        private set
    var auth by mutableStateOf(AuthState())
        private set
    var will by mutableStateOf(WillUiState())
        private set
    var fileVault by mutableStateOf(FileUiState())
        private set
    var notes by mutableStateOf(NoteUiState())
        private set
    var stego by mutableStateOf(StegoUiState())
        private set
    var chain by mutableStateOf(ChainUiState())
        private set

    var trace by mutableStateOf<CipherTrace?>(null)
        private set
    var lastTrace by mutableStateOf<CipherTrace?>(null)
        private set
    var toast by mutableStateOf<String?>(null)
        private set

    val willMessages = db.willMessageDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val encryptedFiles = db.encryptedFileDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val criticalNotes = db.criticalNoteDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stegoImages = db.stegoImageDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ledger = db.blockchainBlockDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        UsbDongleReceiver.refresh(application)
        viewModelScope.launch {
            db.userDao().count().let { count ->
                // Hint ini sudah tercetak di layar (judul + subjudul), jadi status tidak diulang.
                auth = auth.copy(existingUsers = count, message = "")
            }
            dongleStorage.savedTreeUri()?.let { uri ->
                val removable = dongleStorage.isRemovableVolume(uri)
                gate = gate.copy(
                    partitionLabel = if (removable) dongleStorage.volumeLabel(uri) else "Belum ada lokasi kunci dipilih",
                    volumeId = if (removable) dongleStorage.volumeId(uri) else "",
                    volumeRemovable = removable,
                )
                // Dongle sudah tercolok sejak awal: coba verifikasi otomatis, tanpa pindah lapis.
                if (_usb.value.isMassStorage && removable) verifyDongle(uri)
            }
            if (db.blockchainBlockDao().count() == 0) seedGenesis()
        }
        gate = gate.copy(
            biometricAvailable = true,
            biometricLabel = "Sensor siap; jalankan verifikasi.",
        )
    }

    // ---------------- Inspector ----------------

    /**
     * Rekaman disimpan, TIDAK langsung dibuka. Sesuai keputusan desain, seluruh hash/ciphertext
     * hanya boleh muncul lewat ikon ⓘ di pojok kanan atas atau baris "Detail kripto".
     */
    fun showTrace(cipherTrace: CipherTrace) {
        lastTrace = cipherTrace
    }

    fun reopenTrace() {
        trace = lastTrace
    }

    fun dismissTrace() {
        trace = null
    }

    fun clearToast() {
        toast = null
    }

    private fun notify(message: String) {
        toast = message
    }

    // ---------------- Kriteria 1: Dongle USB ----------------

    fun toggleSimulation(enabled: Boolean) {
        if (!BuildConfig.DEBUG) {
            notify("Mode simulasi hanya tersedia pada build debug.")
            return
        }
        gate = gate.copy(simulationEnabled = enabled, simulationUsed = false)
    }

    fun useSimulatedDongle() {
        if (!BuildConfig.DEBUG) return
        gate = gate.copy(
            simulationUsed = true,
            dongleVerified = true,
            dongleMessage = "SIMULASI DEBUG: lapis hardware dilewati, HMAC tidak diverifikasi.",
        )
        refreshStage()
        showTrace(
            CipherTrace(
                criterion = "Kriteria 1 — Gatekeeper",
                title = "Simulasi Dongle",
                algorithm = "USB OTG + HMAC-SHA256",
                rows = listOf(
                    InspectorRow("Mode", "ENABLE_DONGLE_SIMULATION = true (debug only)", PillTone.WARN),
                    InspectorRow("Perangkat USB", gate.usb.lastEvent),
                    InspectorRow("Target VID/PID", "0x05E3 / 0x0747"),
                    InspectorRow("Efek", "Lapis hardware & storage key dianggap lolos"),
                ),
                verdict = "Mode simulasi aktif. Pada build release jalur ini tidak tersedia.",
                verdictTone = PillTone.WARN,
            ),
        )
    }

    fun attachPartition(uri: Uri) {
        val blocker = dongleBlocker()
        if (blocker != null) {
            notify(blocker)
            return
        }
        if (!dongleStorage.isRemovableVolume(uri)) {
            gate = gate.copy(
                dongleMessage = "Lokasi harus di kartu SD atau flashdisk dongle, bukan penyimpanan internal HP.",
                dongleVerified = false,
            )
            notify("Lokasi kunci harus berada di media dongle.")
            return
        }
        dongleStorage.takePersistablePermission(uri)
        gate = gate.copy(
            partitionLabel = dongleStorage.volumeLabel(uri),
            volumeId = dongleStorage.volumeId(uri),
            volumeRemovable = true,
        )
        verifyDongle(uri)
    }

    fun verifySavedPartition() {
        val uri = dongleStorage.savedTreeUri() ?: run {
            notify("Belum ada lokasi kunci dipilih.")
            return
        }
        val blocker = dongleBlocker()
        if (blocker != null) {
            notify(blocker)
            gate = gate.copy(dongleVerified = false, dongleMessage = blocker)
            return
        }
        verifyDongle(uri)
    }

    /** Alasan lapis 1 belum bisa jalan, atau null bila media USB sudah tercolok. */
    private fun dongleBlocker(): String? = DongleGate.blocker(
        usbAttached = _usb.value.attached,
        isMassStorage = _usb.value.isMassStorage,
        simulated = BuildConfig.DEBUG && gate.simulationUsed,
    )

    private fun verifyDongle(uri: Uri) {
        val blocker = dongleBlocker()
        if (blocker != null) {
            gate = gate.copy(dongleVerified = false, dongleMessage = blocker)
            notify(blocker)
            return
        }
        if (!dongleStorage.isRemovableVolume(uri)) {
            gate = gate.copy(
                dongleVerified = false,
                dongleMessage = "Lokasi kunci bukan media dongle (penyimpanan internal ditolak).",
            )
            notify("Pilih kartu SD atau flashdisk dongle.")
            return
        }
        gate = gate.copy(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { dongleStorage.readAndVerify(uri) }
            val verification = result.verification
            gate = gate.copy(
                busy = false,
                dongleVerified = result.success,
                dongleMessage = result.message,
                dongleToken = result.content,
                volumeId = dongleStorage.volumeId(uri),
                volumeRemovable = true,
                partitionLabel = dongleStorage.volumeLabel(uri),
                expectedHmac = verification?.expectedHmac.orEmpty(),
                fileHmac = verification?.fileHmac.orEmpty(),
                simulationUsed = false,
            )
            refreshStage()
            showTrace(
                CipherTrace(
                    criterion = "Kriteria 1 — Gatekeeper",
                    title = "Verifikasi Dongle",
                    algorithm = "Lapis 1 USB mass storage + Lapis 2 HMAC-SHA256",
                    rows = listOf(
                        InspectorRow(
                            "Lapis 1 — media USB",
                            "${gate.usb.deviceName} (mass storage)",
                            PillTone.OK,
                        ),
                        InspectorRow("VID/PID reader", "${UsbDongleReceiver.hex(gate.usb.vendorId)} / ${UsbDongleReceiver.hex(gate.usb.productId)}"),
                        InspectorRow("UUID volume", gate.volumeId, PillTone.INFO),
                        InspectorRow("URI dokumen", result.uri.ifBlank { "-" }),
                        InspectorRow("Isi berkas", result.content.ifBlank { "-" }),
                        InspectorRow("Payload ditandatangani", verification?.payload.orEmpty()),
                        InspectorRow("HMAC dihitung aplikasi", verification?.expectedHmac.orEmpty()),
                        InspectorRow("HMAC di berkas", verification?.fileHmac.orEmpty()),
                        InspectorRow("Serial kartu", verification?.serial ?: "-"),
                        InspectorRow(
                            "Diterbitkan",
                            verification?.issuedAt?.let { java.util.Date(it * 1000).toString() } ?: "-",
                        ),
                        InspectorRow("Hasil", if (result.success) "VALID" else "DITOLAK",
                            if (result.success) PillTone.OK else PillTone.ERROR),
                    ),
                    verdict = result.message,
                    verdictTone = if (result.success) PillTone.OK else PillTone.ERROR,
                ),
            )
        }
    }

    /** Tombol "Lanjut" pada layar dongle: baru pindah ke lapis biometrik setelah lapis 1 lolos. */
    fun advanceToBiometric() {
        if (!gate.dongleOk) {
            notify("Lapis 1 belum lolos.")
            return
        }
        gate = gate.copy(stage = GateStage.BIOMETRIC_LOCKED)
    }

    /** Kembali ke layar dongle dari lapis 2 tanpa membuang status dongle. */
    fun backToDongle() {
        gate = gate.copy(stage = GateStage.DONGLE_LOCKED)
    }

    fun generateDongleKey(uri: Uri) {
        val blocker = dongleBlocker()
        if (blocker != null) {
            notify("Pembuatan kunci hanya bisa saat dongle tercolok.")
            return
        }
        if (!dongleStorage.isRemovableVolume(uri)) {
            notify("Kunci hanya boleh ditulis ke kartu SD atau flashdisk dongle.")
            return
        }
        dongleStorage.takePersistablePermission(uri)
        viewModelScope.launch {
            val serial = dongleStorage.volumeId(uri).ifBlank { "000000000819" }
            val result = withContext(Dispatchers.IO) { dongleStorage.writeDongleFile(uri, serial) }
            gate = gate.copy(
                partitionLabel = dongleStorage.volumeLabel(uri),
                volumeId = dongleStorage.volumeId(uri),
                volumeRemovable = true,
                dongleMessage = result.message,
            )
            notify(result.message)
            verifyDongle(uri)
        }
    }


    fun refreshUsb() {
        UsbDongleReceiver.refresh(getApplication())
        gate = gate.copy(usb = _usb.value)
    }

    fun syncUsbState() {
        if (gate.usb != _usb.value) gate = gate.copy(usb = _usb.value)
    }

    fun usbProbe(): List<String> = UsbDongleReceiver.probe(getApplication())

    // ---------------- Kriteria 1: Biometrik ----------------

    fun onBiometricAvailability(available: Boolean, label: String) {
        gate = gate.copy(biometricAvailable = available, biometricLabel = label)
    }

    fun onBiometricSuccess(proof: String) {
        gate = gate.copy(
            biometricPassed = true,
            biometricProof = proof,
            biometricMessage = "CryptoObject Keystore berhasil dibuka.",
        )
        refreshStage()
        showTrace(
            CipherTrace(
                criterion = "Kriteria 1 — Biometrik",
                title = "BiometricPrompt + CryptoObject",
                algorithm = "AES/GCM di Android Keystore, setUserAuthenticationRequired(true)",
                rows = listOf(
                    InspectorRow("Alias kunci", "legacyvault_biometric_key"),
                    InspectorRow("Plaintext uji", "LEGACYVAULT-BIOMETRIC-PROOF"),
                    InspectorRow("Hasil Cipher", proof),
                    InspectorRow("Status", "Terautentikasi sidik jari"),
                ),
                verdict = "Biometrik lolos dan kunci Keystore terbuka hanya lewat CryptoObject.",
            ),
        )
    }

    fun onBiometricFailure(message: String) {
        gate = gate.copy(biometricPassed = false, biometricMessage = message)
        notify(message)
    }

    fun skipBiometricForDebug() {
        if (!BuildConfig.DEBUG) return
        gate = gate.copy(
            biometricPassed = true,
            biometricProof = "SIMULASI DEBUG: BiometricPrompt tidak dijalankan.",
            biometricMessage = "SIMULASI DEBUG: biometrik dilewati.",
        )
        refreshStage()
    }

    private fun refreshStage() {
        val next = when {
            gate.stage == GateStage.VAULT_UNLOCKED -> GateStage.VAULT_UNLOCKED
            gate.stage == GateStage.AUTH_LOGIN -> GateStage.AUTH_LOGIN
            gate.stage == GateStage.BIOMETRIC_LOCKED && gate.canAdvanceToLogin -> GateStage.AUTH_LOGIN
            else -> gate.stage
        }
        gate = gate.copy(stage = next)
    }

    fun lockVault() {
        sessionStore.clear()
        gate = GateState(
            simulationEnabled = BuildConfig.DEBUG,
            partitionLabel = dongleStorage.savedTreeUri()
                ?.takeIf { dongleStorage.isRemovableVolume(it) }
                ?.let { dongleStorage.volumeLabel(it) }
                ?: "Belum ada lokasi kunci dipilih",
            volumeId = dongleStorage.savedTreeUri()
                ?.takeIf { dongleStorage.isRemovableVolume(it) }
                ?.let { dongleStorage.volumeId(it) }
                .orEmpty(),
            biometricAvailable = true,
            biometricLabel = "Sensor siap; jalankan verifikasi.",
        )
        will = WillUiState()
        fileVault = FileUiState()
        notes = NoteUiState()
        stego = StegoUiState()
        chain = ChainUiState()
    }

    // ---------------- Kriteria 2: Registrasi & Login ----------------

    fun onUsernameChange(value: String) { auth = auth.copy(username = value) }
    fun onPasswordChange(value: String) { auth = auth.copy(password = value) }
    fun onRoleChange(value: String) { auth = auth.copy(role = value) }

    fun register() {
        val username = auth.username.trim()
        val password = auth.password
        if (username.length < 3) {
            auth = auth.copy(message = "Username minimal 3 karakter.", isError = true); return
        }
        if (password.length < 6) {
            auth = auth.copy(message = "Password minimal 6 karakter.", isError = true); return
        }
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { db.userDao().findByUsername(username) }
            if (existing != null) {
                auth = auth.copy(message = "Username sudah terdaftar.", isError = true); return@launch
            }
            val salt = AuthLoginCrypto.randomSaltHex()
            val step = AuthLoginCrypto.credentialTrace(password, salt)
            val usb = _usb.value
            val entity = UserEntity(
                username = username,
                role = auth.role,
                passwordHashSha256 = step.hashHex,
                saltHex = salt,
                boundUsbVid = usb.vendorId,
                boundUsbPid = usb.productId,
                dongleSignatureHmac = gate.expectedHmac,
            )
            withContext(Dispatchers.IO) { db.userDao().insert(entity) }
            auth = auth.copy(
                existingUsers = auth.existingUsers + 1,
                saltHex = salt,
                hashHex = step.hashHex,
                message = "Akun '$username' terdaftar. Hash SHA-256 + salt 16 byte tersimpan.",
                isError = false,
            )
            showTrace(
                CipherTrace(
                    criterion = "Kriteria 2 — Registrasi",
                    title = "SHA-256 + Random Salt",
                    algorithm = "SHA256(password + salt_hex)",
                    rows = listOf(
                        InspectorRow("Username", username),
                        InspectorRow("Role", auth.role),
                        InspectorRow("Salt (16 byte)", salt),
                        InspectorRow("Input digest", step.combined),
                        InspectorRow("Hash SHA-256", step.hashHex, PillTone.OK),
                        InspectorRow("Ikatan USB", "VID=${UsbDongleReceiver.hex(usb.vendorId)} PID=${UsbDongleReceiver.hex(usb.productId)}"),
                        InspectorRow("HMAC dongle tersimpan", gate.expectedHmac.ifBlank { "belum diverifikasi" }),
                    ),
                    verdict = "Password asli tidak pernah disimpan; hanya hash + salt.",
                ),
            )
        }
    }

    fun login() {
        val username = auth.username.trim()
        val password = auth.password
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { db.userDao().findByUsername(username) }
            if (user == null) {
                auth = auth.copy(message = "Akun tidak ditemukan.", isError = true); return@launch
            }
            val step = AuthLoginCrypto.credentialTrace(password, user.saltHex)
            if (step.hashHex != user.passwordHashSha256) {
                auth = auth.copy(
                    message = "Password salah. Hash tidak cocok.",
                    isError = true,
                    saltHex = user.saltHex,
                    hashHex = step.hashHex,
                )
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 2 — Login",
                        title = "Verifikasi Gagal",
                        algorithm = "SHA256(password + salt_hex)",
                        rows = listOf(
                            InspectorRow("Salt tersimpan", user.saltHex),
                            InspectorRow("Hash tersimpan", user.passwordHashSha256),
                            InspectorRow("Hash input", step.hashHex),
                            InspectorRow("Hasil", "TIDAK COCOK", PillTone.ERROR),
                        ),
                        verdict = "Autentikasi ditolak.",
                        verdictTone = PillTone.ERROR,
                    ),
                )
                return@launch
            }
            val issuedAt = System.currentTimeMillis()
            val payload = sessionStore.save(
                SessionStore.StoredSession(user.username, user.role, issuedAt),
                password + user.saltHex,
            )
            val decrypted = sessionStore.readSession(password + user.saltHex)
            gate = gate.copy(stage = GateStage.VAULT_UNLOCKED)
            auth = auth.copy(
                message = "Login berhasil. Token sesi AES-CBC tersimpan.",
                isError = false,
                role = user.role,
                saltHex = user.saltHex,
                hashHex = step.hashHex,
                sessionPayload = payload,
                password = "",
            )
            showTrace(
                CipherTrace(
                    criterion = "Kriteria 2 — Login",
                    title = "Verifikasi + Token Sesi AES",
                    algorithm = "SHA-256 hash + AES/CBC/PKCS5Padding 128-bit",
                    rows = listOf(
                        InspectorRow("Salt tersimpan", user.saltHex),
                        InspectorRow("Hash SHA-256 cocok", user.passwordHashSha256, PillTone.OK),
                        InspectorRow("Kunci sesi", AuthLoginCrypto.deriveSessionKey(password + user.saltHex).toHex()),
                        InspectorRow("Token (HEX(IV):BASE64)", payload),
                        InspectorRow("Hasil dekripsi token", decrypted ?: "gagal", PillTone.INFO),
                        InspectorRow("Disimpan di", "SharedPreferences 'legacyvault_session'"),
                    ),
                    verdict = "Sesi berjalan dan token di disk selalu berbentuk ciphertext.",
                ),
            )
        }
    }

    // ---------------- Kriteria 3: Surat Wasiat ----------------

    fun onWillTitle(v: String) { will = will.copy(title = v) }
    fun onWillPlaintext(v: String) { will = will.copy(plaintext = v) }
    fun onWillKey(v: String) { will = will.copy(vigenereKey = v) }
    fun onWillPassphrase(v: String) { will = will.copy(passphrase = v) }

    fun saveWill() {
        val title = will.title.trim()
        if (title.isEmpty() || will.plaintext.isBlank()) {
            will = will.copy(message = "Judul dan isi surat wajib diisi.", isError = true); return
        }
        if (will.vigenereKey.isEmpty() || will.passphrase.isEmpty()) {
            will = will.copy(message = "Kunci Vigenere dan passphrase AES wajib diisi.", isError = true); return
        }
        will = will.copy(busy = true)
        viewModelScope.launch {
            val traceData = withContext(Dispatchers.Default) {
                SuperTextCipher.encryptSuper(will.plaintext, will.vigenereKey, will.passphrase)
            }
            val steps = SuperTextCipher.vigenereSteps(will.plaintext, will.vigenereKey)
            withContext(Dispatchers.IO) {
                db.willMessageDao().insert(
                    WillMessageEntity(
                        title = title,
                        vigenerePreviewCipher = traceData.stage1,
                        superCipherPayload = traceData.payload,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }
            will = will.copy(
                busy = false,
                title = "", plaintext = "", vigenereKey = "", passphrase = "",
                message = "Surat '$title' tersimpan sebagai super cipher.",
                isError = false,
            )
            showTrace(
                CipherTrace(
                    criterion = "Kriteria 3 — Super Enkripsi",
                    title = "Vigenère + AES-256-CBC",
                    algorithm = "Extended Vigenère (ASCII 32–126, mod 95) → AES/CBC/PKCS5Padding 256-bit",
                    rows = buildList {
                        add(InspectorRow("Plaintext", traceData.plain, PillTone.INFO))
                        add(InspectorRow("Kunci Vigenère", traceData.vigenereKey))
                        steps.forEach {
                            add(
                                InspectorRow(
                                    "C[${it.index}]  '${it.plainChar}'(${it.plainCode}) + '${it.keyChar}'(${it.keyCode}) mod 95",
                                    "${it.cipherCode} → '${it.cipherChar}'",
                                ),
                            )
                        }
                        add(InspectorRow("Tahap 1 (cipher Vigenère)", traceData.stage1, PillTone.INFO))
                        add(InspectorRow("Kunci AES-256 (SHA-256 passphrase)", traceData.aesKeyHex))
                        add(InspectorRow("IV 16 byte", traceData.ivHex))
                        add(InspectorRow("Payload HEX(IV):BASE64(CT)", traceData.payload, PillTone.OK))
                    },
                    verdict = "Dua lapis cipher: klasik lalu modern. Ciphertext akhir tidak dapat dibaca tanpa kedua kunci.",
                ),
            )
        }
    }

    fun startWillDecrypt(item: WillMessageEntity) {
        will = will.copy(
            decryptTarget = item,
            decryptVigenereKey = "",
            decryptPassphrase = "",
            decryptedText = "",
            message = "",
        )
    }

    fun onWillDecryptVigenereKey(v: String) { will = will.copy(decryptVigenereKey = v) }

    fun onWillDecryptPassphrase(v: String) { will = will.copy(decryptPassphrase = v) }

    fun cancelWillDecrypt() { will = will.copy(decryptTarget = null, decryptedText = "") }

    fun decryptWill() {
        val item = will.decryptTarget ?: return
        val vigenereKey = will.decryptVigenereKey
        val passphrase = will.decryptPassphrase
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    SuperTextCipher.decryptSuper(item.superCipherPayload, vigenereKey, passphrase)
                }
            }
            result.onSuccess { (stage1, plain) ->
                will = will.copy(
                    decryptedText = plain,
                    decryptVigenereKey = "",
                    decryptPassphrase = "",
                    message = "",
                    isError = false,
                )
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 3 — Dekripsi",
                        title = "Membuka Super Enkripsi",
                        algorithm = "AES-256-CBC → Extended Vigenère",
                        rows = listOf(
                            InspectorRow("Payload tersimpan", item.superCipherPayload),
                            InspectorRow("Passphrase AES-256", passphrase),
                            InspectorRow("Hasil dekripsi AES (tahap 1)", stage1, PillTone.INFO),
                            InspectorRow("Kunci Vigenère", vigenereKey),
                            InspectorRow("Plaintext akhir", plain, PillTone.OK),
                        ),
                        verdict = "Dekripsi berhasil, isi surat kembali terbaca.",
                    ),
                )
            }.onFailure {
                will = will.copy(
                    decryptedText = "",
                    message = "Kunci salah — blok AES-256 gagal divalidasi (padding tidak cocok).",
                    isError = true,
                )
            }
        }
    }

    fun deleteWill(id: Int) {
        viewModelScope.launch { withContext(Dispatchers.IO) { db.willMessageDao().delete(id) } }
    }

    // ---------------- Kriteria 4: File RC4 ----------------

    fun onFilePassphrase(v: String) { fileVault = fileVault.copy(passphrase = v) }

    fun onFilePicked(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        var name = uri.lastPathSegment ?: "berkas"
        var size = 0L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        }
        fileVault = fileVault.copy(pickedUri = uri, pickedName = name, pickedSize = size, message = "")
    }

    fun encryptPickedFile() {
        val uri = fileVault.pickedUri ?: run {
            fileVault = fileVault.copy(message = "Pilih dokumen terlebih dahulu.", isError = true); return
        }
        val passphrase = fileVault.passphrase
        if (passphrase.isEmpty()) {
            fileVault = fileVault.copy(message = "Passphrase RC4 wajib diisi.", isError = true); return
        }
        fileVault = fileVault.copy(busy = true)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() }
                        ?: error("Berkas tidak dapat dibaca.")
                    val checksum = Rc4FileCipher.sha256Hex(bytes)
                    val cipher = Rc4FileCipher.encrypt(bytes, passphrase)
                    val target = File(vaultDir, "${fileVault.pickedName}.rc4vault")
                    target.writeBytes(cipher)
                    val trace = Rc4FileCipher.trace(bytes, passphrase)
                    Triple(bytes.size.toLong(), checksum, trace) to target
                }
            }
            result.onSuccess { (info, target) ->
                val (size, checksum, rc4Trace) = info
                val originalName = fileVault.pickedName
                val mimeType = getApplication<Application>().contentResolver.getType(uri)
                    ?: "application/octet-stream"
                withContext(Dispatchers.IO) {
                    db.encryptedFileDao().insert(
                        EncryptedFileEntity(
                            originalName = originalName,
                            mimeType = mimeType,
                            originalSize = size,
                            originalSha256Checksum = checksum,
                            encryptedFilePath = target.absolutePath,
                            encryptedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                fileVault = fileVault.copy(
                    busy = false,
                    pickedUri = null,
                    pickedName = "",
                    pickedSize = 0,
                    passphrase = "",
                    message = "Tersimpan: ${target.name} (${target.length()} byte)",
                    isError = false,
                )
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 4 — File RC4",
                        title = "Stream Cipher RC4",
                        algorithm = "ARCFOUR: KSA 256 byte + PRGA XOR byte-per-byte",
                        rows = listOf(
                            InspectorRow("Nama asli", originalName),
                            InspectorRow("Ukuran plaintext", "$size byte"),
                            InspectorRow("SHA-256 berkas asli", checksum, PillTone.INFO),
                            InspectorRow("Kunci RC4 (UTF-8 hex)", rc4Trace.keyHex),
                            InspectorRow("S[0..15] setelah KSA", rc4Trace.sBoxPreview),
                            InspectorRow("Keystream[0..15]", rc4Trace.keystreamPreview),
                            InspectorRow("Plaintext[0..15]", rc4Trace.plainPreview),
                            InspectorRow("Ciphertext[0..15]", rc4Trace.cipherPreview, PillTone.OK),
                            InspectorRow("Berkas keluaran", target.absolutePath),
                        ),
                        verdict = "RC4 berbeda keluarga dari AES: stream cipher, tanpa padding, panjang ciphertext = panjang file.",
                    ),
                )
            }.onFailure {
                fileVault = fileVault.copy(busy = false, message = "Enkripsi gagal: ${it.message}", isError = true)
            }
        }
    }

    fun startFileDecrypt(item: EncryptedFileEntity) {
        fileVault = fileVault.copy(
            decryptTarget = item,
            decryptPassphrase = "",
            decryptedText = "",
            decryptedPath = "",
            decryptedMime = "",
            integrityNote = "",
            message = "",
        )
    }

    fun onFileDecryptPassphrase(v: String) { fileVault = fileVault.copy(decryptPassphrase = v) }

    fun cancelFileDecrypt() {
        fileVault = fileVault.copy(
            decryptTarget = null,
            decryptedText = "",
            decryptedPath = "",
            decryptedMime = "",
            integrityNote = "",
        )
    }

    fun decryptFile() {
        val item = fileVault.decryptTarget ?: return
        val passphrase = fileVault.decryptPassphrase
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val cipher = File(item.encryptedFilePath).readBytes()
                    val plain = Rc4FileCipher.decrypt(cipher, passphrase)
                    val checksum = Rc4FileCipher.sha256Hex(plain)
                    Triple(plain, checksum, Rc4FileCipher.trace(plain, passphrase))
                }
            }
            result.onSuccess { (plain, checksum, rc4Trace) ->
                val integrityOk = checksum == item.originalSha256Checksum
                val safeName = item.originalName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val output = withContext(Dispatchers.IO) {
                    val dir = File(getApplication<Application>().cacheDir, "decrypted").apply { mkdirs() }
                    File(dir, safeName.ifBlank { "dokumen" }).apply { writeBytes(plain) }
                }
                val preview = if (isProbablyText(plain)) {
                    String(plain, Charsets.UTF_8).take(2000)
                } else {
                    "Berkas ${item.mimeType} (${plain.size} byte) sudah dipulihkan. Buka dengan aplikasi penampil."
                }
                fileVault = fileVault.copy(
                    decryptedText = preview,
                    decryptedPath = output.absolutePath,
                    decryptedMime = item.mimeType,
                    decryptPassphrase = "",
                    integrityNote = if (integrityOk) {
                        "SHA-256 cocok — integritas berkas asli terverifikasi."
                    } else {
                        "SHA-256 TIDAK cocok — kunci salah atau berkas berubah."
                    },
                    message = if (integrityOk) "Dekripsi berhasil." else "Dekripsi selesai, integritas gagal.",
                    isError = !integrityOk,
                )
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 4 — Dekripsi RC4",
                        title = "Membuka .rc4vault",
                        algorithm = "RC4 PRGA + verifikasi SHA-256",
                        rows = listOf(
                            InspectorRow("Berkas", item.encryptedFilePath),
                            InspectorRow("Kunci RC4", rc4Trace.keyHex),
                            InspectorRow("Ciphertext[0..15]", rc4Trace.plainPreview),
                            InspectorRow("Plaintext[0..15]", rc4Trace.cipherPreview),
                            InspectorRow("SHA-256 tersimpan", item.originalSha256Checksum),
                            InspectorRow("SHA-256 hasil dekripsi", checksum, if (integrityOk) PillTone.OK else PillTone.ERROR),
                        ),
                        verdict = if (integrityOk) "Integritas terverifikasi." else "Integritas gagal.",
                        verdictTone = if (integrityOk) PillTone.OK else PillTone.ERROR,
                    ),
                )
            }.onFailure {
                fileVault = fileVault.copy(
                    decryptedText = "",
                    message = "Dekripsi gagal: ${it.message}",
                    isError = true,
                )
            }
        }
    }

    fun deleteFile(item: EncryptedFileEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { File(item.encryptedFilePath).delete() }
                db.encryptedFileDao().delete(item.id)
            }
        }
    }

    private fun isProbablyText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val sample = bytes.take(minOf(bytes.size, 512))
        val bad = sample.count { it.toInt() == 0 }
        return bad == 0
    }

    // ---------------- Kriteria 5: 3DES ECB ----------------

    fun onNoteLabel(v: String) { notes = notes.copy(label = v) }
    fun onNoteCategory(v: String) { notes = notes.copy(category = v) }
    fun onNoteAccount(v: String) { notes = notes.copy(accountOrPin = v) }
    fun onNoteSecret(v: String) { notes = notes.copy(secretNote = v) }
    fun onNotePassphrase(v: String) { notes = notes.copy(passphrase = v) }

    fun saveNote() {
        if (notes.label.isBlank()) {
            notes = notes.copy(message = "Label catatan wajib diisi.", isError = true); return
        }
        if (notes.passphrase.isBlank()) {
            notes = notes.copy(message = "Passphrase kunci 3DES wajib diisi.", isError = true); return
        }
        if (notes.category.isBlank() && notes.accountOrPin.isBlank() && notes.secretNote.isBlank()) {
            notes = notes.copy(message = "Isi minimal satu kolom sensitif.", isError = true); return
        }
        notes = notes.copy(busy = true)
        viewModelScope.launch {
            val plainCategory = notes.category
            val key = Db3DesEcbCipher.deriveKey(notes.passphrase)
            val encCategory = withContext(Dispatchers.Default) { Db3DesEcbCipher.encrypt(notes.category, key) }
            val encAccount = withContext(Dispatchers.Default) { Db3DesEcbCipher.encrypt(notes.accountOrPin, key) }
            val encSecret = withContext(Dispatchers.Default) { Db3DesEcbCipher.encrypt(notes.secretNote, key) }
            withContext(Dispatchers.IO) {
                db.criticalNoteDao().insert(
                    CriticalNoteEntity(
                        label = notes.label.trim(),
                        encryptedCategory3Des = encCategory,
                        encryptedAccountOrPin3Des = encAccount,
                        encryptedSecretNote3Des = encSecret,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            notes = notes.copy(
                busy = false,
                label = "", category = "", accountOrPin = "", secretNote = "", passphrase = "",
                message = "Catatan tersimpan; kolom sensitif berisi hex 3DES-ECB.",
                isError = false,
            )
            showTrace(
                CipherTrace(
                    criterion = "Kriteria 5 — Enkripsi Basis Data",
                    title = "Triple DES Mode ECB",
                    algorithm = "DESede/ECB/PKCS5Padding, kunci 24 byte dari SHA-256",
                    rows = listOf(
                        InspectorRow("Kunci 3DES (hex)", key.toHex()),
                        InspectorRow("Kategori plaintext", plainCategory.ifBlank { "-" }),
                        InspectorRow("Kategori ciphertext", encCategory, PillTone.OK),
                        InspectorRow("Blok ECB (8 byte)", Db3DesEcbCipher.blockPreview(encCategory).joinToString(" | ")),
                        InspectorRow("Rekening/PIN ciphertext", encAccount, PillTone.OK),
                        InspectorRow("Catatan rahasia ciphertext", encSecret, PillTone.OK),
                        InspectorRow("Tabel", "critical_notes (kolom *_3Des)"),
                    ),
                    verdict = "ECB dipilih agar tiap field/baris bisa didekripsi independen tanpa IV.",
                ),
            )
        }
    }

    fun revealNote(item: CriticalNoteEntity, passphrase: String) {
        if (passphrase.isBlank()) {
            notes = notes.copy(message = "Masukkan kunci 3DES dulu.", isError = true)
            return
        }
        viewModelScope.launch {
            val key = Db3DesEcbCipher.deriveKey(passphrase)
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    Triple(
                        Db3DesEcbCipher.decrypt(item.encryptedCategory3Des, key),
                        Db3DesEcbCipher.decrypt(item.encryptedAccountOrPin3Des, key),
                        Db3DesEcbCipher.decrypt(item.encryptedSecretNote3Des, key),
                    )
                }
            }
            result.onSuccess { revealed ->
                notes = notes.copy(
                    revealed = notes.revealed + (item.id to revealed),
                    message = "",
                    isError = false,
                )
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 5 — Dekripsi Baris",
                        title = "Dekripsi 3DES-ECB per record",
                        algorithm = "DESede/ECB/PKCS5Padding",
                        rows = listOf(
                            InspectorRow("Baris id", item.id.toString()),
                            InspectorRow("Kunci 3DES", key.toHex()),
                            InspectorRow("Cipher kategori", item.encryptedCategory3Des),
                            InspectorRow("Plain kategori", revealed.first, PillTone.OK),
                            InspectorRow("Cipher rekening/PIN", item.encryptedAccountOrPin3Des),
                            InspectorRow("Plain rekening/PIN", revealed.second, PillTone.OK),
                            InspectorRow("Plain catatan", revealed.third, PillTone.OK),
                        ),
                        verdict = "Satu baris dapat didekripsi tanpa menyentuh baris lain — konsekuensi mode ECB.",
                    ),
                )
            }.onFailure {
                notes = notes.copy(
                    message = "Kunci salah — blok 3DES gagal divalidasi (padding tidak cocok).",
                    isError = true,
                )
            }
        }
    }

    fun hideNote(id: Int) {
        notes = notes.copy(revealed = notes.revealed - id)
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.criticalNoteDao().delete(id) }
            notes = notes.copy(revealed = notes.revealed - id)
        }
    }

    // ---------------- Kriteria 6: Steganografi LSB ----------------

    fun onStegoTitle(v: String) { stego = stego.copy(title = v) }
    fun onStegoMessage(v: String) { stego = stego.copy(secretMessage = v) }
    fun onStegoKey(v: String) { stego = stego.copy(stegoKey = v) }

    fun onCoverPicked(uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadBitmap(uri) }
            if (bitmap == null) {
                stego = stego.copy(message = "Gambar tidak dapat dibaca.", isError = true)
                return@launch
            }
            stego = stego.copy(
                coverUri = uri,
                coverName = uri.lastPathSegment ?: "cover",
                coverBitmap = bitmap,
                resultBitmap = null,
                resultPath = "",
                message = "Cover ${bitmap.width}×${bitmap.height} dimuat.",
                isError = false,
            )
        }
    }

    fun embedStego() {
        val cover = stego.coverBitmap ?: run {
            stego = stego.copy(message = "Pilih foto cover terlebih dahulu.", isError = true); return
        }
        if (stego.secretMessage.isBlank()) {
            stego = stego.copy(message = "Pesan rahasia wajib diisi.", isError = true); return
        }
        if (stego.stegoKey.isBlank()) {
            stego = stego.copy(message = "Stego-key wajib diisi.", isError = true); return
        }
        stego = stego.copy(busy = true)
        val stegoKey = stego.stegoKey
        val title = stego.title.ifBlank { "Memori ${System.currentTimeMillis()}" }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    val embedded = LsbSteganographyEngine.embed(cover, stego.secretMessage, stegoKey)
                    val coverPath = File(vaultDir, "cover_${System.currentTimeMillis()}.png")
                    val stegoPath = File(vaultDir, "stego_${System.currentTimeMillis()}.png")
                    coverPath.outputStream().use { cover.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    stegoPath.outputStream().use { embedded.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    Triple(embedded, coverPath to stegoPath, stego.secretMessage)
                }
            }
            result.onSuccess { (embedded, paths, plain) ->
                val (coverPath, stegoPath) = paths
                withContext(Dispatchers.IO) {
                    db.stegoImageDao().insert(
                        StegoImageEntity(
                            title = title,
                            coverImagePath = coverPath.absolutePath,
                            stegoImagePath = stegoPath.absolutePath,
                            payloadBitLength = embedded.payloadBits,
                            psnrScore = embedded.psnr,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
                stego = stego.copy(
                    busy = false,
                    title = "",
                    secretMessage = "",
                    stegoKey = "",
                    resultBitmap = embedded.bitmap,
                    resultPath = stegoPath.absolutePath,
                    mse = embedded.mse,
                    psnr = embedded.psnr,
                    payloadBits = embedded.payloadBits,
                    message = "Stego-object tersimpan sebagai PNG (nirrugi).",
                    isError = false,
                )
                val verify = runCatching {
                    LsbSteganographyEngine.extract(embedded.bitmap, stegoKey)
                }.getOrNull()
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 6 — Steganografi LSB",
                        title = "LSB 24-bit + PRNG Seed",
                        algorithm = "Spatial LSB kanal R/G/B, urutan indeks diacak java.util.Random(seed)",
                        rows = listOf(
                            InspectorRow("Stego-key", stegoKey),
                            InspectorRow("Seed PRNG (SHA-256 → Long)", embedded.seed.toString()),
                            InspectorRow("Offset permutasi", embedded.offset.toString()),
                            InspectorRow("Step permutasi (koprima)", embedded.step.toString()),
                            InspectorRow("Pesan", plain.ifBlank { "-" }, PillTone.INFO),
                            InspectorRow("Panjang pesan", "${embedded.messageBytes} byte = ${embedded.payloadBits - 32} bit"),
                            InspectorRow("Total bit disisipkan (termasuk header)", "${embedded.payloadBits} bit"),
                            InspectorRow("Kapasitas gambar", "${embedded.capacityBits} bit"),
                            InspectorRow("Rasio pemakaian", "%.3f %%".format(embedded.payloadBits * 100.0 / embedded.capacityBits)),
                            InspectorRow("MSE", "%.6f".format(embedded.mse)),
                            InspectorRow("PSNR", "%.4f dB".format(embedded.psnr), PillTone.OK),
                            InspectorRow("Verifikasi ekstraksi", verify?.message ?: "gagal", if (verify?.message == plain) PillTone.OK else PillTone.ERROR),
                            InspectorRow("Berkas keluaran", stegoPath.absolutePath),
                            InspectorRow("Format kompresi", "Bitmap.CompressFormat.PNG 100% (nirrugi)"),
                        ),
                        verdict = "PSNR ${"%.2f".format(embedded.psnr)} dB menandakan perubahan piksel tidak kasat mata.",
                    ),
                )
            }.onFailure {
                stego = stego.copy(busy = false, message = "Penyisipan gagal: ${it.message}", isError = true)
            }
        }
    }

    fun startStegoExtract(item: StegoImageEntity) {
        stego = stego.copy(extractTarget = item, extractKey = "", extractedText = "", message = "")
    }

    fun onExtractKey(v: String) { stego = stego.copy(extractKey = v) }

    fun cancelStegoExtract() { stego = stego.copy(extractTarget = null, extractKey = "", extractedText = "") }

    fun extractStego() {
        val item = stego.extractTarget ?: return
        val key = stego.extractKey
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeFile(item.stegoImagePath)
                        ?: error("Berkas stego tidak dapat dibuka.")
                    LsbSteganographyEngine.extract(bitmap, key)
                }
            }
            result.onSuccess { extracted ->
                stego = stego.copy(
                    extractedText = extracted.message,
                    extractKey = "",
                    message = "Pesan berhasil diekstrak.",
                    isError = false,
                )
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 6 — Ekstraksi LSB",
                        title = "Membaca Bit Pesan",
                        algorithm = "PRNG seed yang sama → urutan bit yang sama",
                        rows = listOf(
                            InspectorRow("Stego-key", key),
                            InspectorRow("Seed PRNG", extracted.seed.toString()),
                            InspectorRow("Header 32 bit (panjang)", "${(extracted.payloadBits - 32) / 8} byte"),
                            InspectorRow("Total bit dibaca", extracted.payloadBits.toString()),
                            InspectorRow("Pesan terpulihkan", extracted.message, PillTone.OK),
                        ),
                        verdict = "Pesan identik dengan yang disisipkan; PNG mempertahankan LSB.",
                    ),
                )
            }.onFailure {
                stego = stego.copy(
                    extractedText = "",
                    extractKey = "",
                    message = "Ekstraksi gagal: ${it.message}",
                    isError = true,
                )
            }
        }
    }

    fun deleteStego(item: StegoImageEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { File(item.coverImagePath).delete() }
                runCatching { File(item.stegoImagePath).delete() }
                db.stegoImageDao().delete(item.id)
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val maxDim = 1400
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        return if (decoded.config == Bitmap.Config.ARGB_8888) {
            decoded
        } else {
            decoded.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    // ---------------- Kriteria 7: Blockchain ----------------

    fun onChainAsset(v: String) { chain = chain.copy(assetTitle = v) }
    fun onChainBeneficiary(v: String) { chain = chain.copy(beneficiaryName = v) }
    fun onChainDocumentHash(v: String) { chain = chain.copy(documentSha256 = v, documentSource = "Input manual") }

    fun onChainDocumentPicked(uri: Uri) {
        viewModelScope.launch {
            val hash = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes() } ?: error("Berkas tidak terbaca")
                    Rc4FileCipher.sha256Hex(bytes)
                }
            }.getOrNull()
            if (hash == null) {
                chain = chain.copy(message = "Gagal menghitung hash dokumen.", isError = true)
            } else {
                chain = chain.copy(
                    documentSha256 = hash,
                    documentSource = "SHA-256 berkas: ${uri.lastPathSegment}",
                    message = "Hash dokumen dihitung.",
                    isError = false,
                )
            }
        }
    }

    fun mineBlock() {
        if (chain.assetTitle.isBlank() || chain.beneficiaryName.isBlank()) {
            chain = chain.copy(message = "Nama aset dan ahli waris wajib diisi.", isError = true); return
        }
        val docHash = chain.documentSha256.ifBlank {
            SimpleBlockchainEngine.sha256Hex("${chain.assetTitle}|${chain.beneficiaryName}")
        }
        chain = chain.copy(mining = true, attempts = 0, message = "", isError = false)
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { db.blockchainBlockDao().getAll() }
            val previous = existing.lastOrNull()
            val index = (previous?.blockIndex ?: -1) + 1
            val previousHash = previous?.currentHash ?: SimpleBlockchainEngine.GENESIS_PREVIOUS_HASH
            val timestamp = System.currentTimeMillis()
            val mined = withContext(Dispatchers.Default) {
                SimpleBlockchainEngine.mine(
                    index = index,
                    timestamp = timestamp,
                    assetTitle = chain.assetTitle.trim(),
                    beneficiaryName = chain.beneficiaryName.trim(),
                    documentSha256 = docHash,
                    previousHash = previousHash,
                ) { attempt ->
                    viewModelScope.launch { chain = chain.copy(attempts = attempt) }
                }
            }
            withContext(Dispatchers.IO) {
                db.blockchainBlockDao().insert(
                    BlockchainBlockEntity(
                        blockIndex = mined.block.index,
                        timestamp = mined.block.timestamp,
                        assetTitle = mined.block.assetTitle,
                        beneficiaryName = mined.block.beneficiaryName,
                        documentSha256 = mined.block.documentSha256,
                        previousHash = mined.block.previousHash,
                        nonce = mined.block.nonce,
                        currentHash = mined.block.currentHash,
                    ),
                )
            }
            chain = chain.copy(
                mining = false,
                attempts = mined.attempts,
                assetTitle = "",
                beneficiaryName = "",
                documentSha256 = "",
                message = "Blok #${mined.block.index} ditambang dalam ${mined.elapsedMs} ms (${mined.attempts} percobaan).",
                isError = false,
            )
            showTrace(
                CipherTrace(
                    criterion = "Kriteria 7 — Blockchain",
                    title = "Proof-of-Work Blok #${mined.block.index}",
                    algorithm = "SHA256(index+timestamp+assetTitle+beneficiary+docHash+prevHash+nonce), target '000'",
                    rows = listOf(
                        InspectorRow("index", mined.block.index.toString()),
                        InspectorRow("timestamp", mined.block.timestamp.toString()),
                        InspectorRow("assetTitle", mined.block.assetTitle),
                        InspectorRow("beneficiaryName", mined.block.beneficiaryName),
                        InspectorRow("documentSha256", mined.block.documentSha256),
                        InspectorRow("previousHash", mined.block.previousHash),
                        InspectorRow("nonce pemenang", mined.block.nonce.toString(), PillTone.OK),
                        InspectorRow("currentHash", mined.block.currentHash, PillTone.OK),
                        InspectorRow("Percobaan", "${mined.attempts} (${mined.elapsedMs} ms)"),
                        InspectorRow("Difficulty", "3 nol di depan (${SimpleBlockchainEngine.DIFFICULTY_PREFIX})"),
                    ),
                    verdict = "Nonce hanya sah jika hash memenuhi target kesulitan.",
                ),
            )
            verifyChain(showTraceAfter = false)
        }
    }

    fun verifyChain(showTraceAfter: Boolean = true) {
        viewModelScope.launch {
            val blocks = withContext(Dispatchers.IO) { db.blockchainBlockDao().getAll() }
            val assetBlocks = blocks.map {
                id.upnyk.legacyvault.core.crypto.AssetBlock(
                    index = it.blockIndex,
                    timestamp = it.timestamp,
                    assetTitle = it.assetTitle,
                    beneficiaryName = it.beneficiaryName,
                    documentSha256 = it.documentSha256,
                    previousHash = it.previousHash,
                    nonce = it.nonce,
                    currentHash = it.currentHash,
                )
            }
            val verification = SimpleBlockchainEngine.verifyChain(assetBlocks)
            chain = chain.copy(
                verification = verification,
                message = verification.summary,
                isError = !verification.valid,
            )
            if (showTraceAfter) {
                showTrace(
                    CipherTrace(
                        criterion = "Kriteria 7 — Verifikasi Rantai",
                        title = "verifyChain()",
                        algorithm = "Rekomputasi SHA-256 + pemeriksaan keterkaitan previousHash",
                        rows = assetBlocks.map { block ->
                            val status = verification.blockStatus.firstOrNull { it.index == block.index }
                            InspectorRow(
                                "Blok #${block.index} — ${block.assetTitle}",
                                "${block.currentHash}\n${status?.message ?: "-"}",
                                if (status?.valid == true) PillTone.OK else PillTone.ERROR,
                            )
                        },
                        verdict = verification.summary,
                        verdictTone = if (verification.valid) PillTone.OK else PillTone.ERROR,
                    ),
                )
            }
        }
    }

    fun tamperBlock(index: Int) {
        viewModelScope.launch {
            val block = withContext(Dispatchers.IO) { db.blockchainBlockDao().get(index) } ?: return@launch
            withContext(Dispatchers.IO) {
                db.blockchainBlockDao().tamperTitle(index, "${block.assetTitle} (DIUBAH)")
            }
            verifyChain(showTraceAfter = true)
            notify("Blok #$index diubah. Rantai seharusnya terdeteksi rusak.")
        }
    }

    fun resetLedger() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.blockchainBlockDao().deleteAllExceptGenesis()
            }
            chain = chain.copy(verification = null, message = "Ledger direset ke genesis.")
            seedGenesis()
        }
    }

    private suspend fun seedGenesis() {
        val genesis = withContext(Dispatchers.Default) {
            SimpleBlockchainEngine.mine(
                index = 0,
                timestamp = 1735689600000L,
                assetTitle = "GENESIS — Buku Besar LegacyVault",
                beneficiaryName = "Sistem",
                documentSha256 = SimpleBlockchainEngine.sha256Hex("legacyvault-genesis"),
                previousHash = SimpleBlockchainEngine.GENESIS_PREVIOUS_HASH,
            ).block
        }
        withContext(Dispatchers.IO) {
            db.blockchainBlockDao().insert(
                BlockchainBlockEntity(
                    blockIndex = genesis.index,
                    timestamp = genesis.timestamp,
                    assetTitle = genesis.assetTitle,
                    beneficiaryName = genesis.beneficiaryName,
                    documentSha256 = genesis.documentSha256,
                    previousHash = genesis.previousHash,
                    nonce = genesis.nonce,
                    currentHash = genesis.currentHash,
                ),
            )
        }
    }

    // ---------------- util ----------------

    private fun shortUri(uri: Uri): String {
        // Nama volume lebih berguna di layar daripada URI mentah content://.
        val volume = uri.lastPathSegment?.let(Uri::decode) ?: return "Partisi terpilih (SAF)"
        return volume.replace(":", " · ").ifBlank { "Partisi terpilih (SAF)" }
    }

    fun vaultDirectory(): String = vaultDir.absolutePath

    fun databasePath(): String = getApplication<Application>().getDatabasePath("legacy_vault.db").absolutePath
}
