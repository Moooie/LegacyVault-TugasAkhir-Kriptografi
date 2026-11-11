# LegacyVault — Brankas Wasiat Digital Berbasis Android

Aplikasi Android Native untuk Tugas Akhir mata kuliah **Kriptografi**. LegacyVault berperan
sebagai brankas wasiat digital: aplikasi hanya dapat dihidupkan dengan dongle perangkat keras
dan autentikasi biometrik, lalu menyediakan lima modul yang masing-masing memakai algoritma
kriptografi berbeda.

Seluruh data tersimpan luring di perangkat. Tidak ada server, tidak ada sinkronisasi.

> **English summary** — [jump to English section](#english)

---

## Unduh dan pasang

| Berkas | Untuk | Ukuran |
|---|---|---|
| [`dist/LegacyVault-1.0/LegacyVault-1.0-release.apk`](dist/LegacyVault-1.0/LegacyVault-1.0-release.apk) | penguji dan demo resmi | 9,5 MB |
| [`dist/LegacyVault-1.0/LegacyVault-1.0-dev.apk`](dist/LegacyVault-1.0/LegacyVault-1.0-dev.apk) | anggota kelompok, ada mode simulasi | 13,2 MB |

Petunjuk pemasangan lengkap: [`dist/LegacyVault-1.0/BACA-DULU.txt`](dist/LegacyVault-1.0/BACA-DULU.txt).
Checksum SHA-256: [`dist/LegacyVault-1.0/SHA256SUMS.txt`](dist/LegacyVault-1.0/SHA256SUMS.txt).

Kedua APK dapat dipasang bersamaan karena nama paketnya berbeda:

| | Rilis | Debug |
|---|---|---|
| `applicationId` | `id.upnyk.legacyvault` | `id.upnyk.legacyvault.dev` |
| Nama di launcher | LegacyVault | LegacyVault DEV |
| Ikon | latar hijau pinus | latar jingga |
| Tombol simulasi dongle | tidak ada | ada |
| Tombol "Lewati biometrik (debug)" | tidak ada | ada |

Build rilis tidak memiliki jalan pintas apa pun. Kedua tombol simulasi diikat
`BuildConfig.DEBUG`, sehingga tidak pernah ikut ke build rilis.

---

## Tujuh kriteria kriptografi

| Kriteria | Konteks | Algoritma | Keluaran tersimpan |
|---|---|---|---|
| 1 | Gerbang aplikasi | USB mass storage + SAF + HMAC-SHA256 + Android Keystore + BiometricPrompt | status valid/terkunci |
| 2 | Login | SHA-256 + salt 16 byte; sesi AES-128-CBC (JCrypto) | hash heksadesimal, token `HEX(IV):BASE64` |
| 3 | Surat wasiat (teks) | Extended Vigenère ASCII 95 karakter + AES-256-CBC | `HEX(IV):BASE64` |
| 4 | Dokumen warisan (berkas) | stream cipher RC4 (KSA + PRGA ditulis mandiri) | berkas `.rc4vault` + SHA-256 asli |
| 5 | Catatan finansial (basis data) | Triple DES 192 bit mode ECB | string heksadesimal per kolom |
| 6 | Galeri memori (citra) | LSB kanal R/G/B + PRNG ber-seed | PNG + nilai MSE/PSNR |
| 7 | Buku besar aset | rantai blok SHA-256 + Proof-of-Work `000` | tabel `blockchain_ledger` |

Aturan pengerjaan yang dipatuhi:

- Kotlin 100%, minSdk 26, targetSdk 34, Jetpack Compose Material 3, pola MVVM dengan lapisan
  kriptografi terpisah.
- Vigenère, RC4, steganografi LSB, dan blockchain **ditulis mandiri** tanpa pustaka pihak ketiga.
- AES, 3DES, dan SHA-256 memakai `javax.crypto` dan `java.security.MessageDigest`.
- Setiap modul memiliki panel **Crypto Inspector** yang memperlihatkan plaintext, kunci, nilai
  antara, dan ciphertext untuk keperluan demonstrasi di depan penguji.

---

## Arsitektur

Dokumen lengkap: [`docs/ARSITEKTUR-LegacyVault.md`](docs/ARSITEKTUR-LegacyVault.md) — rumus tiap
algoritma, mesin status, skema basis data, hingga tabel hasil pengujian.

```
app/src/main/java/id/upnyk/legacyvault/
  core/crypto/     algoritma: Vigenere+AES, RC4, 3DES, LSB, blockchain, HMAC dongle
  core/hardware/   deteksi USB, SAF, verifikasi HMAC, BiometricPrompt
  core/session/    token sesi AES-CBC di SharedPreferences
  core/database/   Room: 6 entitas + DAO
  ui/              ViewModel, navigasi, layar per modul, panel Crypto Inspector
app/src/test/      9 pengujian unit algoritma (CryptoSelfCheckTest)
docs/              dokumen arsitektur untuk penyusunan laporan
tools/             generator dokumen laporan (python-docx)
dist/              APK siap pasang + petunjuk + checksum
```

Mesin status aplikasi:

```
DONGLE_LOCKED --(USB mass storage + HMAC valid)--> DONGLE_VERIFIED
              --(biometrik Keystore)--> AUTH_LOGIN
              --(SHA-256 + salt cocok)--> VAULT_UNLOCKED
              --(dongle dicabut)--> DONGLE_LOCKED
```

---

## Build dari kode sumber

Prasyarat: JDK 17 atau lebih baru, Android SDK 37.

```bash
./gradlew assembleDebug      # menghasilkan LegacyVault DEV (id.upnyk.legacyvault.dev)
./gradlew assembleRelease    # menghasilkan LegacyVault (id.upnyk.legacyvault)
```

Build rilis ditandatangani dengan keystore debug karena hanya untuk keperluan demo kampus,
bukan untuk Play Store. Konsekuensinya, APK rilis yang Anda bangun di komputer lain akan punya
tanda tangan berbeda; keduanya tetap bisa dipasang pada perangkat yang belum pernah memuat
LegacyVault.

Menjalankan pengujian unit:

```bash
./gradlew testDebugUnitTest
```

---

## Hasil pengujian

Diuji pada perangkat fisik (Poco F5, Android, layar 1080 × 2400).

| Parameter | Nilai |
|---|---|
| MSE stego-object | 0,000084 |
| PSNR stego-object | 88,87 dB |
| Payload pesan stego | 336 bit (42 byte UTF-8) |
| Nonce blok genesis | 3866 |
| Nonce blok aset #1 | 867 |
| Difficulty Proof-of-Work | 3 nol (`000`) |
| Berkas uji RC4 | 663.923 byte, checksum SHA-256 terverifikasi |
| Pengujian unit | 9, seluruhnya lulus |

---

## Batasan keamanan

Aplikasi ini adalah media pembelajaran dan demonstrasi, bukan sistem penyimpanan warisan
bertingkat produksi. Batasan berikut dinyatakan terbuka:

1. **Kunci HMAC dongle tertanam di kode dan APK.** Karena repositori ini publik, siapa pun dapat
   membuat berkas lisensi `.legacy_dongle.key` sendiri. Untuk penggunaan nyata, kunci harus
   berada di server penerbit lisensi.
2. **Lisensi tidak terikat nomor seri kartu**, sehingga satu lisensi dapat disalin ke kartu lain.
3. **Triple DES mode ECB bersifat deterministik.** Plaintext identik menghasilkan ciphertext
   identik. Mode ini dipilih sesuai tuntutan tugas agar baris basis data dapat didekripsi acak.
4. **Steganografi LSB rapuh** terhadap pengubahan ukuran dan kompresi lossy; karena itu keluaran
   selalu PNG.
5. **Blockchain bersifat lokal.** Penyerang dengan akses ke basis data dapat menambang ulang
   seluruh rantai sehingga tampak valid. Blockchain terdistribusi mengatasi hal ini melalui
   konsensus jaringan yang tidak diimplementasikan di sini.
6. **Difficulty 3 nol** hanya membutuhkan ratusan hingga ribuan percobaan, dipilih agar demo
   cepat.
7. **Validasi dongle fisik** belum dapat dilakukan pada perangkat keras target karena media kartu
   belum tersedia saat pengujian; lapis 1 diuji memakai mode simulasi yang hanya ada di build
   debug, sementara logika deteksi kelas mass storage dan validasi volume eksternal tercakup
   pengujian unit.

---

## Lisensi dan kredit

Proyek tugas akhir Program Studi Informatika, Fakultas Teknik Industri,
Universitas Pembangunan Nasional "Veteran" Yogyakarta, 2026.

Daftar penyusun: lihat sampul laporan pada `dist/LAPORAN AKHIR KRIPTOGRAFI - LegacyVault.docx`.

Kode sumber dirilis untuk keperluan penilaian akademik.

---

## English

LegacyVault is a native Android app for a Cryptography final project. It acts as a digital will
vault that can only be unlocked with a hardware dongle (USB mass storage carrying an
HMAC-SHA256-signed license file) followed by Android Keystore-bound biometric authentication.
After unlocking, five modules are available, each using a different algorithm:

1. Gatekeeper — USB mass-storage detection, SAF-based license file, HMAC-SHA256, BiometricPrompt.
2. Login — salted SHA-256 password hashing and an AES-128-CBC encrypted session token.
3. Will text — layered encryption: Extended Vigenère (classical) then AES-256-CBC (modern).
4. Legacy files — hand-written RC4 stream cipher producing `.rc4vault` files with SHA-256 integrity
   checks.
5. Financial notes — Triple DES in ECB mode, stored as hex per database column.
6. Family photos — LSB steganography with a PRNG-seeded permutation, PNG output, MSE/PSNR reported.
7. Asset ledger — a local SHA-256 blockchain with 3-zero Proof-of-Work plus chain verification and
   a tamper simulator.

Stack: Kotlin, Jetpack Compose (Material 3), MVVM, Room, minSdk 26, targetSdk 34.
Classical cipher, RC4, steganography and the blockchain are implemented from scratch; AES, 3DES
and SHA-256 use `javax.crypto` / `MessageDigest`.

Install: download the APK from `dist/LegacyVault-1.0/`. The `release` APK is the real product with
no bypass buttons; the `dev` APK additionally exposes a debug-only dongle simulation and a
"skip biometric" button, both gated behind `BuildConfig.DEBUG`.

Build: `./gradlew assembleDebug` or `./gradlew assembleRelease` (release is signed with the debug
keystore for campus demo purposes only).

Known limitations are listed in the Indonesian section above; the most important one is that the
dongle HMAC secret ships inside the APK, so anyone can mint a license file. See
`docs/ARSITEKTUR-LegacyVault.md` for the full architecture specification.
