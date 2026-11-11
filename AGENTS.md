# AGENTS.md — Spesifikasi Arsitektur & Instruksi Eksekusi Master (LegacyVault)

## 0. Konteks Proyek & Aturan Kerja Agent (Grill-Me Rules)
Kamu adalah Principal Android & Cryptography Engineer. Tugasmu adalah membangun aplikasi Android Native bernama **LegacyVault** (Sistem Brankas Wasiat Digital & Eksekusi Warisan Otomatis) untuk memenuhi 7 kriteria wajib Tugas Akhir Kriptografi.

### Aturan Mutlak (Zero-Ambiguity Constraints)
1. **Delegasi Desain ke Skill `@ui-ux-pro-max`**:
   - Gunakan skill **`@ui-ux-pro-max`** yang kamu miliki untuk menentukan sistem desain, palet warna, tipografi, komponen kartu, animasi transisi, dan tata letak terbaik bagi aplikasi brankas wasiat & kriptografi di Android.
   - Pastikan antarmuka memiliki panel inspeksi (**Crypto Inspector / X-Ray View**) di setiap fitur agar pengguna dapat mendemonstrasikan perubahan *plaintext*, *key*, proses antara, hingga *ciphertext* secara langsung di depan penguji.
2. **Platform & Arsitektur**:
   - Bahasa: **Kotlin** (100%), Minimum SDK: **26 (Android 8.0)**, Target SDK: **34 (Android 14)**.
   - UI Framework: **Jetpack Compose** (Material 3) dengan pola arsitektur **MVVM (Model-View-ViewModel)** + Clean Crypto Layer.
   - Database Lokal: **Room Database (SQLite)** tanpa enkripsi penuh dari library luar, karena enkripsi field spesifik wajib dilakukan oleh modul kriptografi internal.
3. **Aturan Implementasi Algoritma Kriptografi**:
   - DILARANG menggunakan library pihak ketiga instan (seperti BouncyCastle untuk cipher klasik/RC4/steganografi/blockchain).
   - Algoritma Klasik (Vigenère), Stream Cipher (RC4), Steganografi (LSB + PRNG), dan Blockchain wajib ditulis mandiri (*from scratch*) dalam Kotlin murni agar logika perhitungannya transparan.
   - Untuk AES, 3DES, dan SHA-256, gunakan paket standar bawaan Android/Java: `javax.crypto` (JCrypto) dan `java.security.MessageDigest` sesuai instruksi tugas.

---

## 1. Spesifikasi 7 Kriteria Kriptografi (Non-Overlapping Matrix)

Setiap fitur WAJIB menggunakan algoritma yang berbeda sesuai kontrak berikut:

1. **Kriteria 1 — Gatekeeper: Hardware Dongle & Login Biometrik**
   - **Hardware Target**: Card Reader USB Lexar berisi kartu microSDHC SanDisk Ultra 32GB[cite: 2] dengan label partisi `SDHC` (kapasitas ~29,7 GB, format FAT32) yang dihubungkan via konektor OTG.
   - **Mekanisme Validasi Dongle**:
     - Lapis 1 (Hardware): Deteksi pencolokan USB via `UsbManager` (`ACTION_USB_DEVICE_ATTACHED` & `ACTION_USB_DEVICE_DETACHED`) dan validasi `vendorId` serta `productId` perangkat USB[cite: 3].
     - Lapis 2 (Storage Key): Baca file lisensi `.legacy_dongle.key` di root partisi `SDHC`[cite: 4] melalui *Storage Access Framework* (SAF), lalu verifikasi tanda tangan **HMAC-SHA256** di dalamnya.
     - **Mode Simulasi Debug**: Sediakan sakelar `ENABLE_DONGLE_SIMULATION` pada mode *debug/development* agar pengujian aplikasi tetap berjalan lancar saat dongle fisik belum dicolokkan.
   - **Mekanisme Biometrik**: Setelah dongle terverifikasi valid, tampilkan dialog `androidx.biometric.BiometricPrompt` yang diikat dengan `CryptoObject` (kunci AES di dalam *Android Keystore*). Aplikasi tidak boleh melewati layar ini jika salah satu gagal.

2. **Kriteria 2 — Login Aplikasi dengan Algoritma Kriptografi**
   - **Algoritma Password**: Fungsi hash satu-arah **SHA-256** dengan penambahan *random salt* 16-byte (`Hash = SHA256(password + salt_hex)`).
   - **Algoritma Sesi Login**: Gunakan **JCrypto (`javax.crypto.Cipher`)** dengan algoritma `AES/CBC/PKCS5Padding` (128-bit) untuk mengenkripsi payload token sesi pengguna yang sedang login sebelum disimpan di `SharedPreferences`.

3. **Kriteria 3 — Pesan Teks: Super Enkripsi (Klasik + Modern)**
   - **Konteks Fitur**: Penulisan dan pembacaan **Surat Wasiat Keluarga**.
   - **Tahap 1 (Kriptografi Klasik — Extended Vigenère Cipher)**:
     - Beroperasi pada karakter ASCII yang dapat dicetak (rentang desimal 32 s.d. 126, total 95 karakter) agar spasi, angka, dan tanda baca pada surat wasiat tetap terjaga.
     - Rumus Enkripsi: `C_i = ((P_i - 32 + (K_{i mod m} - 32)) mod 95) + 32`.
     - Rumus Dekripsi: `P_i = ((C_i - 32 - (K_{i mod m} - 32) + 95) mod 95) + 32`.
   - **Tahap 2 (Kriptografi Modern — Block Cipher AES-256 Mode CBC)**:
     - Ambil output string dari Tahap 1, lalu enkripsi menggunakan `AES/CBC/PKCS5Padding` dengan kunci 256-bit (turunan SHA-256 dari passphrase) dan `Initialization Vector` (IV) 16-byte acak.
     - Format penyimpanan akhir: `HEX(IV) + ":" + BASE64(Ciphertext_AES)`.

4. **Kriteria 4 — Pesan File: Kriptografi Berbeda dari Teks (Stream Cipher RC4)**
   - **Konteks Fitur**: Brankas Dokumen Warisan (mengenkripsi file PDF sertifikat tanah, akta notaris, atau dokumen gambar/audio)[cite: 1].
   - **Algoritma**: **Stream Cipher RC4 (ARCFOUR)** yang diimplementasikan secara mandiri byte-per-byte:
     - **KSA (Key-Scheduling Algorithm)**: Inisialisasi array `S[0..255]`, lakukan permutasi nilai `S` menggunakan byte kunci `U`.
     - **PRGA (Pseudo-Random Generation Algorithm)**: Bangkitkan aliran kunci (*keystream*) `K` dan lakukan operasi bitwise XOR (`c = K xor P[idx]`) terhadap seluruh byte file.
   - Simpan file hasil enkripsi dengan ekstensi `.rc4vault` di direktori internal aplikasi beserta nilai checksum MD5/SHA-256 file asli untuk verifikasi integritas.

5. **Kriteria 5 — Pesan Penting Dienkripsi pada Basis Data (3DES Mode ECB)**[cite: 1]
   - **Konteks Fitur**: Penyimpanan Catatan Finansial Krusial (Nomor Rekening, PIN Brankas Fisik, Seed Phrase, Nilai Taksiran Harta) di dalam tabel SQLite/Room[cite: 1].
   - **Algoritma**: **Block Cipher Triple DES (3DES) Mode ECB** (`DESede/ECB/PKCS5Padding`) dengan panjang kunci 24-byte (192-bit)[cite: 1].
   - **Alasan Pemilihan**: Sesuai teori perkuliahan, mode operasi ECB mengenkripsi setiap blok secara independen sehingga record/field basis data dapat diakses dan didekripsi secara acak per baris tanpa bergantung pada record lainnya.
   - Seluruh kolom sensitif pada tabel `critical_notes` wajib tersimpan dalam wujud string Heksadesimal hasil enkripsi 3DES-ECB[cite: 1].

6. **Kriteria 6 — Pesan Gambar: Steganografi LSB + PRNG Seed**[cite: 1]
   - **Konteks Fitur**: Galeri Memori Keluarga (menyisipkan teks petunjuk lokasi penyimpanan harta ke dalam foto keluarga)[cite: 1].
   - **Algoritma**: **Spatial Domain LSB (Least Significant Bit)** pada citra `Bitmap` 24-bit (`ARGB_8888`, kanal R, G, dan B) yang diperkuat dengan **PRNG (`java.util.Random`)**[cite: 1]:
     - Konversi *Stego-Key* (string) menjadi nilai `Long` (melalui hash) sebagai *seed* PRNG untuk mengacak urutan indeks koordinat piksel yang akan disisipi bit pesan.
     - 32 bit pertama yang disisipkan adalah panjang pesan (dalam satuan byte), diikuti oleh bit-bit pesan rahasia (8 bit per karakter UTF-8).
     - Sisipkan 1 bit pesan pada bit ke-0 (LSB) dari masing-masing kanal R, G, atau B.
   - **Aturan Format Output**: Citra hasil steganografi (*stego-object*) WAJIB disimpan menggunakan kompresi nirrugi `Bitmap.CompressFormat.PNG` (100% quality) agar bit LSB tidak rusak oleh kompresi JPEG. Hitung dan tampilkan nilai **MSE** dan **PSNR (dB)** antara citra asli (*cover-object*) dan *stego-object*.

7. **Kriteria 7 — Aset Penting: Teknologi Blockchain Sederhana**[cite: 1]
   - **Konteks Fitur**: Buku Besar Akta Kepemilikan & Pembagian Warisan (*Local Immutable Asset Ledger*)[cite: 1].
   - **Struktur Blok (`AssetBlock`)**:
     - `index`: Int (dimulai dari 0 untuk *Genesis Block*).
     - `timestamp`: Long (Epoch milliseconds).
     - `assetTitle`: String (Nama aset warisan).
     - `beneficiaryName`: String (Nama ahli waris penerima).
     - `documentSha256`: String (Nilai hash SHA-256 dari dokumen fisik/sertifikat aset).
     - `previousHash`: String (Nilai hash dari blok `index - 1`, atau `"0".repeat(64)` untuk Genesis Block).
     - `nonce`: Long (Angka tebakan Proof-of-Work).
     - `currentHash`: String (`SHA256(index + timestamp + assetTitle + beneficiaryName + documentSha256 + previousHash + nonce)`).
   - **Mekanisme Konsensus & Validasi**:
     - Gunakan **Proof-of-Work (PoW)** dengan tingkat kesulitan (*difficulty*) **3 angka nol di depan** (`currentHash.startsWith("000")`).
     - Sediakan fungsi `verifyChain()` yang memeriksa ulang kecocokan `currentHash` dan keterkaitan `previousHash` di seluruh blok, serta fitur **Tamper Simulation** (simulasi pengubahan data di salah satu blok untuk mendemonstrasikan rusaknya rantai blok di layar UI)[cite: 1].

---

## 2. Mesin Status Aplikasi (Deterministic State Machine)

Aplikasi hanya boleh berada di salah satu state berikut dan berpindah sesuai aturan transisi:

```text
[STATE_1: DONGLE_LOCKED]
   |-- (USB OTG Lexar terpasang + file .legacy_dongle.key valid ATAU Simulasi Debug Aktif)
   v
[STATE_2: BIOMETRIC_LOCKED]
   |-- (BiometricPrompt sukses memvalidasi CryptoObject Keystore)
   v
[STATE_3: AUTH_LOGIN]
   |-- (Input Username & Password cocok dengan SHA-256 Hash + Salt, Token JCrypto AES dibuat)
   v
[STATE_4: VAULT_UNLOCKED] (Akses ke 5 Modul: Wasiat, File RC4, Catatan DB 3DES, Stegano LSB, Blockchain)