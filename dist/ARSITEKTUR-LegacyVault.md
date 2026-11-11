# LegacyVault — Spesifikasi Arsitektur dan Rancangan

Dokumen ini dipakai sebagai bahan penyusunan **Laporan Tugas Akhir Kriptografi**. Semua angka,
nama berkas, nama fungsi, dan pesan yang tertulis di sini diambil dari kode sumber yang benar-benar
dibangun dan diuji, bukan dari rencana.

---

## 0. Panduan penyusunan laporan

Bagian ini berisi aturan penulisan laporan supaya isinya tetap sesuai dengan kode yang ada.

1. **Data pengujian** yang boleh dipakai hanya yang ada di Bagian 12. Kalau sebuah data belum ada,
   tulis `[BELUM DIUJI]`, jangan diisi sendiri.
2. **Daftar pustaka** yang boleh dipakai ada di Bagian 16.
3. **Struktur** mengikuti contoh laporan kampus: Sampul, DAFTAR ISI, BAB I
   PENDAHULUAN, BAB II DASAR TEORI, BAB III PERANCANGAN DAN ARSITEKTUR SISTEM,
   BAB IV HASIL IMPLEMENTASI DAN PENGUJIAN, BAB V PENUTUP, DAFTAR PUSTAKA.
4. **Format**: kertas A4; margin atas 3 cm, bawah 3 cm, kiri 2,5 cm, kanan 2,5 cm;
   isi 12 pt, spasi 1,5, rata kanan-kiri; judul BAB 14 pt bold rata tengah; subbab bold dengan
   nomor manual (`3.1`, `3.2`, …); keterangan gambar dan tabel 10 pt italic rata tengah;
   penomoran gambar `Gambar 4.1`, tabel `Tabel 3.1` per bab.
5. **Gaya bahasa**: Indonesia baku, kalimat lengkap, tanpa kata ganti orang pertama, tanpa
   hiperbola. Sebut algoritma dengan nama tepat (`AES-256-CBC`, `DESede/ECB/PKCS5Padding`,
   `Extended Vigenère`, `RC4`, `SHA-256`).
6. Setiap subbab BAB II perlu memuat rumus dalam bentuk persamaan dan satu paragraf "Keterangan"
   yang menjelaskan setiap simbol pada rumus.
7. Setiap subbab BAB III perlu menjelaskan mengapa algoritma itu dipilih untuk konteks tersebut,
   bukan hanya cara kerjanya.
8. BAB IV memuat tabel pengujian yang isinya diambil dari Bagian 12 dan 13, ditambah minimal tujuh
   gambar tangkapan layar (Bagian 14).
9. Bagian 15 muncul sebagai subbab "Analisis Keamanan dan Batasan". Jangan dihapus, karena penguji
   hampir selalu menanyakan batas keamanan.
10. Panjang wajar 20–30 halaman sebelum lampiran.

---

## 1. Ringkasan proyek

| Butir | Isi |
|---|---|
| Nama aplikasi | LegacyVault |
| Tema | Brankas wasiat digital dan eksekusi warisan otomatis |
| Bentuk | Aplikasi Android Native, berjalan luring (tanpa server) |
| Bahasa | Kotlin 100% |
| Antarmuka | Jetpack Compose, Material 3, tema terang tunggal |
| Arsitektur | MVVM + lapisan kriptografi terpisah |
| Basis data | Room (SQLite), enkripsi per kolom oleh modul internal |
| Minimum SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Nama paket | `id.upnyk.legacyvault` |
| Versi | 1.0 |
| Perangkat uji | Poco F5, layar 1080 × 2400 piksel |
| Alat build | Gradle 9.9.0, JDK 22, Android SDK 37 |

Tujuh kriteria wajib yang harus dibuktikan (sumber: slide Tugas Akhir mata kuliah Kriptografi):

1. Aplikasi memiliki fungsi login biometrik dan hanya dapat dihidupkan menggunakan dongle.
2. Login aplikasi menggunakan algoritma kriptografi (hash, JCrypto, AES, dan sejenisnya).
3. Pesan berupa teks dienkripsi menggunakan super enkripsi (algoritma modern dan klasik).
4. Pesan berupa berkas dienkripsi menggunakan kriptografi yang berbeda dari teks.
5. Pesan penting dienkripsi pada basis data yang digunakan.
6. Pesan berupa gambar diamankan menggunakan konsep steganografi.
7. Aset penting diamankan menggunakan teknologi blockchain sederhana.

Pemetaan kriteria terhadap algoritma — **tidak ada algoritma yang dipakai dua kali**:

| Kriteria | Konteks fitur | Algoritma | Keluaran tersimpan |
|---|---|---|---|
| 1 | Gerbang aplikasi | USB mass storage + SAF + HMAC-SHA256 + Android Keystore + BiometricPrompt | Status valid/terkunci |
| 2 | Login | SHA-256 + salt 16 byte; sesi AES-128-CBC | Hash heksadesimal, token `HEX(IV):BASE64` |
| 3 | Surat wasiat (teks) | Extended Vigenère ASCII 95 karakter + AES-256-CBC | `HEX(IV):BASE64` |
| 4 | Dokumen warisan (berkas) | Stream cipher RC4 (KSA + PRGA mandiri) | Berkas `.rc4vault` + SHA-256 asli |
| 5 | Catatan finansial (basis data) | Triple DES 192 bit mode ECB | String heksadesimal per kolom |
| 6 | Galeri memori (citra) | LSB kanal R/G/B + PRNG ber-seed | Berkas PNG + nilai MSE/PSNR |
| 7 | Buku besar aset | Rantai blok SHA-256 + Proof-of-Work `000` | Baris tabel `blockchain_ledger` |

---

## 2. Peta kode sumber

Akar: `app/src/main/java/id/upnyk/legacyvault/`

```
MainActivity.kt                     Activity tunggal, memegang ViewModel, meneruskan intent USB
core/crypto/
  AuthLoginCrypto.kt                SHA-256 + salt, AES/CBC/PKCS5Padding untuk token sesi
  SuperTextCipher.kt                Extended Vigenere + AES-256-CBC (Kriteria 3)
  Rc4FileCipher.kt                  KSA, PRGA, XOR byte-per-byte, SHA-256 berkas (Kriteria 4)
  Db3DesEcbCipher.kt                DESede/ECB/PKCS5Padding, cuplikan blok (Kriteria 5)
  LsbSteganographyEngine.kt         LSB + PRNG seed, MSE, PSNR (Kriteria 6)
  SimpleBlockchainEngine.kt         Blok, PoW, verifyChain (Kriteria 7)
  DongleHmacVerifier.kt             HMAC-SHA256 berkas lisensi (Kriteria 1 lapis 2)
  Hex.kt                            Konversi byte <-> heksadesimal
core/hardware/
  DongleGate.kt                     Aturan lapis 1 murni (dapat diuji unit)
  UsbDongleReceiver.kt              Broadcast attach/detach + deteksi kelas mass storage
  DongleStorageManager.kt           SAF: baca/tulis .legacy_dongle.key, validasi volume
  BiometricGateHelper.kt            Kunci Keystore + BiometricPrompt + CryptoObject
core/session/SessionStore.kt        Token sesi terenkripsi di SharedPreferences
core/database/
  AppDatabase.kt                    Definisi Room
  dao/VaultDaos.kt                  DAO per entitas
  entity/*.kt                       Enam entitas (lihat Bagian 11)
ui/
  VaultViewModel.kt                 Mesin status, aksi modul, pesan galat, trace kripto
  navigation/VaultNavGraph.kt       Rute: gatekeeper, biometrik, auth, modul
  components/                       CryptoInspectorSheet, VaultScaffold, komponen kartu
  screens/gatekeeper/               GatekeeperScreen (lapis 1), BiometricScreen (lapis 2)
  screens/auth/AuthScreen.kt        Daftar akun dan login
  screens/dashboard/                Dasbor lima modul
  screens/willtext/                 Modul 1 surat wasiat
  screens/filevault/                Modul 2 dokumen warisan
  screens/dbnotes/                  Modul 3 catatan penting
  screens/stegano/                  Modul 4 galeri memori
  screens/blockchain/               Modul 5 buku besar aset
  theme/                            Color.kt, Type.kt, Theme.kt
app/src/test/java/id/upnyk/legacyvault/CryptoSelfCheckTest.kt   9 pengujian unit
```

Pemisahan lapisan: `ui` tidak pernah memanggil `javax.crypto` langsung; seluruh operasi
kriptografi berada di `core/crypto` dan `core/hardware`. Semua operasi berat dijalankan pada
`Dispatchers.IO` atau `Dispatchers.Default` agar antarmuka tidak membeku.

---

## 3. Mesin status aplikasi

Empat keadaan, transisi hanya terjadi karena tindakan eksplisit pengguna.

```
STATE_1 DONGLE_LOCKED
   | syarat: perangkat USB dengan interface mass storage tercolok
   |         DAN berkas .legacy_dongle.key dibaca dari volume eksternal (bukan "primary"/"home")
   |         DAN HMAC-SHA256 pada isi berkas cocok
   | aksi: tombol "Lanjut ke biometrik"
   v
STATE_2 BIOMETRIC_LOCKED
   | syarat: BiometricPrompt memvalidasi CryptoObject (kunci AES di Android Keystore)
   | aksi: sidik jari terverifikasi
   v
STATE_3 AUTH_LOGIN
   | syarat: username terdaftar DAN SHA256(password + salt_hex) sama dengan hash tersimpan
   | aksi: tombol masuk, token sesi AES-CBC dibuat
   v
STATE_4 VAULT_UNLOCKED
   | akses lima modul
   | aksi: dongle dicabut (ACTION_USB_DEVICE_DETACHED) -> kembali ke STATE_1
```

Aturan tambahan:
- Pencabutan dongle saat brankas terbuka langsung memanggil `lockVault()` dan kembali ke lapis 1
  dengan pesan "Dongle dilepas - lapis 1 terkunci kembali." Dongle karena itu berfungsi sebagai
  kunci fisik yang harus tetap terpasang selama sesi.
- Verifikasi dongle yang berhasil hanya menandai status valid; status tidak berpindah sendiri ke
  lapis 2 sebelum pengguna menekan tombol lanjut.
- Dua sakelar simulasi hanya aktif pada build debug karena diikat pada `BuildConfig.DEBUG`:
  `ENABLE_DONGLE_SIMULATION` (melewati lapis hardware dan HMAC) dan "Lewati biometrik (debug)".
  Keduanya tidak ada pada build rilis.

---

## 4. Kriteria 1 — Gerbang dongle dan autentikasi biometrik

### 4.1 Lapis 1: deteksi perangkat keras

- `UsbDongleReceiver` mendengarkan `ACTION_USB_DEVICE_ATTACHED` dan `ACTION_USB_DEVICE_DETACHED`
  melalui `UsbManager`, dan juga memeriksa perangkat yang sudah tercolok saat aplikasi dibuka.
- Perangkat dinyatakan media penyimpanan bila salah satu interface-nya memiliki kelas
  `USB_CLASS_MASS_STORAGE` (kode `0x08`).
- **Keputusan desain penting**: pemeriksaan memakai kelas antarmuka, bukan `vendorId`/`productId`.
  Alasannya, kode kelas bersifat standar untuk semua media penyimpanan USB, sehingga card reader
  merek apa pun dan flashdisk apa pun dapat dipakai sebagai dongle. VID/PID hanya ditampilkan
  sebagai informasi pada layar, tidak dipakai sebagai syarat.
- `res/xml/usb_device_filter.xml` sengaja diisi `<usb-device />` tanpa atribut agar semua
  perangkat USB memicu intent, lalu penyaringan dilakukan di kode.

### 4.2 Lapis 2: berkas lisensi di media dongle

- Media yang sah: kartu microSDHC di dalam card reader USB, atau flashdisk, terhubung melalui OTG.
- Pengguna memilih akar volume melalui `ACTION_OPEN_DOCUMENT_TREE` (SAF). Izin persisten diambil
  dengan `takePersistableUriPermission`.
- `DongleStorageManager.isRemovableVolume()` menolak volume `primary` dan `home`, karena keduanya
  adalah penyimpanan internal. Hanya volume eksternal yang diterima. Pembuatan kunci karena itu
  tidak mungkin menulis ke memori internal.
- Berkas lisensi bernama `.legacy_dongle.key`, diletakkan di akar volume. Format isi:

```
LEGACYVAULT-DONGLE-v1|serial|issuedEpoch|hmacHex
```

- HMAC dihitung atas string `LEGACYVAULT-DONGLE-v1|serial|issuedEpoch` memakai HMAC-SHA256 dan
  kunci rahasia bersama. Nilai `hmacHex` yang tidak cocok menandakan berkas dimodifikasi.
- Rumus:

```
HMAC(K, m) = H((K' ⊕ opad) ‖ H((K' ⊕ ipad) ‖ m))
```

  Keterangan: `H` adalah SHA-256, `K` kunci rahasia, `K'` kunci yang disesuaikan panjangnya,
  `opad` dan `ipad` padding luar dan dalam, `m` pesan yang diautentikasi.

- Fitur "Buat kunci di dongle" hanya berjalan bila dongle tercolok dan hanya menulis ke volume
  kartu/flashdisk yang dipilih, dengan pesan penolakan "Kunci hanya boleh ditulis ke kartu SD atau
  flashdisk dongle."

### 4.3 Biometrik

- `BiometricGateHelper` membuat kunci AES di `AndroidKeyStore` dengan
  `setUserAuthenticationRequired(true)`, lalu mengikat `Cipher` ke `BiometricPrompt` melalui
  `CryptoObject`.
- Deskripsi prompt: "Cipher AES/GCM di Android Keystore hanya terbuka setelah sidik jari
  terverifikasi."
- Keberhasilan callback berarti kunci benar-benar dibuka oleh sistem operasi, bukan oleh aplikasi,
  sehingga tidak dapat dipalsukan aplikasi lain.

---

## 5. Kriteria 2 — Login dengan algoritma kriptografi

- Kata sandi tidak pernah disimpan. Yang disimpan: `passwordHashSha256` dan `saltHex`.
- Salt acak 16 byte per akun (`SALT_BYTES = 16`), dibangkitkan `java.security.SecureRandom`.

```
Hash = SHA256(password + salt_hex)
```

  Keterangan: `password` adalah kata sandi asli, `salt_hex` adalah salt 16 byte dalam bentuk
  heksadesimal, hasilnya berupa 64 karakter heksadesimal.

- Token sesi tidak disimpan sebagai teks biasa. `AuthLoginCrypto.encryptSession()` memakai
  `AES/CBC/PKCS5Padding` dengan kunci 128 bit, yaitu 16 byte pertama dari
  `SHA256(password + saltHex)`. IV 16 byte acak per sesi.
- Format envelope sesi: `HEX(IV):BASE64(ciphertext)`, disimpan di `SharedPreferences` berkas
  `legacyvault_session` pada kunci `session_ticket`, bersama `session_username`.
- Rumus mode CBC:

```
Cᵢ = Eₖ(Pᵢ ⊕ Cᵢ₋₁),  C₀ = IV
```

  Keterangan: `P` blok plaintext, `C` blok ciphertext, `E` enkripsi AES, `K` kunci 128 bit,
  `IV` Initialization Vector 16 byte.

- Pesan galat yang tampil: "Password salah. Hash tidak cocok." dan "Akun tidak ditemukan."

---

## 6. Kriteria 3 — Super enkripsi surat wasiat (klasik + modern)

Konteks: penulisan dan pembacaan surat wasiat keluarga.

### 6.1 Tahap 1 — Extended Vigenère Cipher

- Bekerja pada karakter ASCII yang dapat dicetak, desimal 32 sampai 126, total 95 karakter
  (`ASCII_START = 32`, `ASCII_RANGE = 95`). Spasi, angka, dan tanda baca pada surat tetap terjaga;
  inilah alasan varian diperluas dipakai, bukan Vigenère A–Z.

```
Enkripsi: Cᵢ = ((Pᵢ − 32 + (Kᵢ mod m − 32)) mod 95) + 32
Dekripsi: Pᵢ = ((Cᵢ − 32 − (Kᵢ mod m − 32) + 95) mod 95) + 32
```

  Keterangan: `Pᵢ` nilai desimal karakter plaintext ke-i, `Cᵢ` ciphertext ke-i, `K` kunci,
  `m` panjang kunci, kunci berulang secara siklik.

### 6.2 Tahap 2 — AES-256-CBC

- Masukan tahap 2 adalah keluaran tahap 1 berupa string.
- Kunci 256 bit diturunkan dari passphrase: `SHA-256(passphrase)` (32 byte).
- IV 16 byte acak per pesan.
- Format simpan akhir: `HEX(IV) + ":" + BASE64(ciphertext_AES)`.

### 6.3 Penyimpanan dan tampilan

- Entitas `will_messages` menyimpan `title`, `vigenerePreviewCipher` (cuplikan hasil tahap 1 untuk
  kebutuhan demonstrasi), `superCipherPayload`, `createdAt`.
- Ciphertext tidak pernah tampil terbuka pada daftar. Nilai tersimpan disembunyikan di balik
  tombol "Ciphertext tersimpan".
- Panel Crypto Inspector menampilkan langkah per karakter Vigenère (maksimum 10 langkah), hasil
  tahap 1, kunci AES, IV, dan payload akhir, sehingga terlihat bahwa ini benar-benar dua tahap.
- Dekripsi membutuhkan dua kunci: kunci Vigenère dan passphrase AES. Keduanya dikosongkan setelah
  proses. Kunci salah menghasilkan pesan
  "Kunci salah - blok AES-256 gagal divalidasi (padding tidak cocok)."

---

## 7. Kriteria 4 — Enkripsi berkas dokumen dengan RC4

Konteks: brankas dokumen warisan (PDF sertifikat tanah, akta notaris, gambar, audio).

- Algoritma: RC4 (ARCFOUR) ditulis mandiri, byte per byte, tanpa pustaka pihak ketiga.
- Kunci: byte dari passphrase yang dimasukkan pengguna.
- KSA menyiapkan dan mempermutasi larik `S[0..255]`:

```
j = (j + S[i] + U[i mod 256]) mod 256;  swap(S[i], S[j])
```

- PRGA membangkitkan aliran kunci dan melakukan XOR:

```
i = (i + 1) mod 256
j = (j + S[i]) mod 256;  swap(S[i], S[j])
t = (S[i] + S[j]) mod 256;  K = S[t]
c = K ⊕ p
```

  Keterangan: `S` larik permutasi 256 byte, `U` byte kunci, `K` byte aliran kunci, `p` byte
  plaintext, `c` byte ciphertext. Karena XOR simetris, dekripsi memakai algoritma yang sama.

- Berkas hasil disimpan di direktori internal aplikasi (`filesDir/vault`) dengan nama
  `<namaAsli>.rc4vault`, sehingga tidak dapat dibaca aplikasi lain.
- Integritas: `originalSha256Checksum` berkas asli dicatat pada entitas `encrypted_files`. Setelah
  dekripsi, SHA-256 hasil dibandingkan; bila cocok tampil
  "SHA-256 cocok - integritas berkas asli terverifikasi.", bila tidak
  "SHA-256 TIDAK cocok - kunci salah atau berkas berubah."
- Hasil dekripsi ditulis ke `cacheDir/decrypted/`, dibagikan melalui `FileProvider`
  (`res/xml/file_paths.xml`), lalu dibuka dengan `ACTION_VIEW` sehingga pengguna benar-benar dapat
  melihat dokumen (gambar tampil inline, PDF/audio terbuka di penampil perangkat).
- Metadata tersimpan: `originalName`, `mimeType`, `originalSize`, `originalSha256Checksum`,
  `encryptedFilePath`, `encryptedAt`.
- Panel X-Ray menampilkan potongan keystream, potongan ciphertext, dan perbandingan checksum.

---

## 8. Kriteria 5 — Catatan penting terenkripsi pada basis data (3DES-ECB)

Konteks: catatan finansial krusial (nomor rekening, PIN brankas fisik, seed phrase, nilai taksiran
harta) pada tabel `critical_notes`.

- Algoritma: `DESede/ECB/PKCS5Padding`, kunci 24 byte (192 bit) yang diturunkan dari passphrase
  melalui SHA-256 lalu dipotong 24 byte.
- Kolom sensitif disimpan sebagai string heksadesimal:
  `encryptedCategory3Des`, `encryptedAccountOrPin3Des`, `encryptedSecretNote3Des`.
- Rumus:

```
Cᵢ = Eₖ₃(Dₖ₂(Eₖ₁(Pᵢ)))
```

  Keterangan: `Pᵢ` blok plaintext 8 byte, `E` enkripsi DES, `D` dekripsi DES, `K1`, `K2`, `K3`
  tiga subkunci 8 byte. Dekripsi membalik urutan menjadi `Dₖ₁(Eₖ₂(Dₖ₃(C)))`.

- **Alasan pemilihan mode ECB**: ECB mengenkripsi setiap blok secara independen tanpa IV, sehingga
  satu baris/kolom dapat didekripsi secara acak tanpa bergantung pada baris lain. Ini sesuai
  kebutuhan tabel basis data yang menampilkan catatan per baris.
- **Konsekuensi yang harus ditulis di laporan**: plaintext identik menghasilkan ciphertext identik
  dan pola blok dapat terlihat; mode ini tidak dianjurkan untuk berkas atau jaringan.
- Perilaku antarmuka: setiap baris daftar menampilkan status "TERKUNCI" dan hanya terbuka setelah
  pengguna memasukkan kunci 3DES pada baris tersebut. Setelah terbuka, tombol berubah menjadi
  "Sembunyikan"/"Hapus". Kunci salah menghasilkan
  "Kunci salah - blok 3DES gagal divalidasi (padding tidak cocok)."
- `Db3DesEcbCipher.blockPreview()` memotong ciphertext per blok 8 byte agar sifat ECB dapat
  diperlihatkan di panel X-Ray.

---

## 9. Kriteria 6 — Steganografi LSB dengan PRNG seed

Konteks: galeri memori keluarga; pesan berisi petunjuk lokasi penyimpanan harta disisipkan ke foto.

- Media: `Bitmap` 24 bit `ARGB_8888`, hanya kanal R, G, dan B (kanal alpha tidak dipakai).
- Satu bit pesan per kanal, kanal dipakai bergilang R → G → B.
- Sisipan pada bit ke-0:

```
p' = p − (p mod 2) + b
```

  Keterangan: `p` nilai kanal asli (0–255), `p'` nilai setelah disisipi, `b` bit pesan (0 atau 1).
  Ekstraksi cukup `b = p' mod 2`.

- **Pengacakan urutan (PRNG)**: string stego-key di-hash SHA-256, 8 byte pertama dirangkai menjadi
  nilai `Long` sebagai seed. Dari seed itu dibangkitkan `offset` dan `step`, lalu posisi kanal
  ke-i dihitung:

```
pos(i) = (offset + i × step) mod totalKanal
```

  dengan `step` dipilih saling prima terhadap `totalKanal` agar permutasi tidak pernah berulang dan
  tidak ada kanal yang terpakai dua kali. Pendekatan ini dipakai karena `Random` bawaan tidak
  menjamin permutasi unik untuk ruang indeks yang sangat besar; versi aritmetika ini tetap
  deterministik terhadap seed dan menjamin penyebaran merata.

- Format payload: 32 bit pertama menyimpan panjang pesan dalam satuan byte, diikuti 8 bit per
  karakter UTF-8.
- Kapasitas: `3 × W × H` bit dikurangi header 32 bit. Contoh terukur: citra 540 × 1200 memiliki
  kapasitas 1.944.000 bit.
- Keluaran wajib `Bitmap.CompressFormat.PNG` kualitas 100% karena JPEG bersifat lossy dan merusak
  bit LSB.
- Kualitas diukur dan ditampilkan:

```
MSE  = (1 / (W × H)) Σ (I(x,y) − I'(x,y))²
PSNR = 10 log₁₀ (MAX² / MSE)
```

  Keterangan: `I` citra asli, `I'` citra hasil, `MAX = 255`. Nilai PSNR di atas 40 dB dianggap
  tidak kasat mata.

- Stego-key salah ditolak dengan pesan
  "Ekstraksi gagal: Header panjang tidak valid (N byte). Stego-key salah atau gambar bukan hasil
  sisipan.", dan bidang kunci dikosongkan.
- Metadata tersimpan: `title`, `coverImagePath`, `stegoImagePath`, `payloadBitLength`,
  `psnrScore`, `createdAt`.

---

## 10. Kriteria 7 — Blockchain sederhana untuk buku besar aset

Konteks: buku besar akta kepemilikan dan pembagian warisan (local immutable asset ledger).

Struktur blok (`AssetBlock`):

| Field | Tipe | Keterangan |
|---|---|---|
| `index` | Int | 0 untuk genesis |
| `timestamp` | Long | epoch milidetik |
| `assetTitle` | String | nama aset warisan |
| `beneficiaryName` | String | nama ahli waris |
| `documentSha256` | String | SHA-256 dokumen/sertifikat |
| `previousHash` | String | hash blok sebelumnya; genesis = 64 karakter nol |
| `nonce` | Long | angka tebakan PoW |
| `currentHash` | String | hasil hash blok |

Rumus hash blok — seluruh field disambung tanpa pemisah:

```
currentHash = SHA256(index ‖ timestamp ‖ assetTitle ‖ beneficiaryName ‖
                     documentSha256 ‖ previousHash ‖ nonce)
```

Proof-of-Work:

- `DIFFICULTY = 3`, `DIFFICULTY_PREFIX = "000"`.
- `mine()` menaikkan `nonce` dari 0 sampai `currentHash.startsWith("000")` terpenuhi. Progres
  dilaporkan setiap 2000 percobaan; proses berjalan di `Dispatchers.Default`.
- Rata-rata dibutuhkan sekitar 16³ ≈ 4096 percobaan; hasil nyata terukur pada Bagian 12.

Verifikasi `verifyChain()` memeriksa tiga hal per blok:

1. `hashMatches` — hitung ulang SHA-256 dari isi blok dan bandingkan dengan `currentHash` tersimpan.
2. `linkMatches` — `previousHash` sama dengan `currentHash` blok sebelumnya; untuk genesis harus
   64 nol.
3. `meetsDifficulty` — `currentHash` berawalan `000`.

Blok yang gagal ditandai `RUSAK`, indeks pertama yang rusak dicatat, dan status rantai menjadi
"Rantai RUSAK pada blok #N."

Fitur demonstrasi:
- "Simulasi ubah data blok ini" menulis `assetTitle + " (DIUBAH)"` langsung ke basis data **tanpa**
  menghitung ulang hash, meniru penyerang yang mengubah isi. Hasilnya blok tersebut dan seluruh
  rantai setelahnya dinyatakan tidak valid.
- "Reset ledger" menghapus semua blok kecuali genesis lalu menambang ulang genesis
  (judul `GENESIS — Buku Besar LegacyVault`, timestamp `1735689600000`).
- "Hitung hash dari berkas" menghitung SHA-256 isi dokumen nyata; alternatifnya "Input manual"
  untuk menempel hash yang sudah ada.

Penyimpanan: tabel `blockchain_ledger` dengan primary key `blockIndex`.

---

## 11. Skema basis data Room

| Tabel | Kolom | Bentuk penyimpanan |
|---|---|---|
| `users` | `id`, `username` (unik), `role`, `passwordHashSha256`, `saltHex`, `boundUsbVid`, `boundUsbPid`, `dongleSignatureHmac` | hash + salt heksadesimal |
| `will_messages` | `id`, `title`, `vigenerePreviewCipher`, `superCipherPayload`, `createdAt` | `HEX(IV):BASE64` super enkripsi |
| `encrypted_files` | `id`, `originalName`, `mimeType`, `originalSize`, `originalSha256Checksum`, `encryptedFilePath`, `encryptedAt` | metadata + path `.rc4vault` |
| `critical_notes` | `id`, `label`, `encryptedCategory3Des`, `encryptedAccountOrPin3Des`, `encryptedSecretNote3Des`, `updatedAt` | heksadesimal 3DES-ECB per kolom |
| `stego_images` | `id`, `title`, `coverImagePath`, `stegoImagePath`, `payloadBitLength`, `psnrScore`, `createdAt` | path PNG + nilai PSNR |
| `blockchain_ledger` | `blockIndex` (primary key), `timestamp`, `assetTitle`, `beneficiaryName`, `documentSha256`, `previousHash`, `nonce`, `currentHash` | blok utuh |

Berkas terenkripsi berada di `filesDir/vault`; hasil dekripsi sementara di `cacheDir/decrypted`.
Basis data berada di `databases/legacy_vault.db`.

---

## 12. Bukti pengujian terukur (dipakai di BAB IV)

Pengujian dilakukan pada perangkat fisik Poco F5, build debug, tanggal 11 November 2026.

| Parameter | Nilai terukur | Konteks |
|---|---|---|
| MSE stego-object | 0,000084 | Citra 540 × 1200, pesan 42 byte |
| PSNR stego-object | 88,87 dB | Ambang kasat mata 40 dB |
| Payload pesan | 336 bit | 42 karakter UTF-8 + header 32 bit |
| Kapasitas citra uji | 1.944.000 bit | 3 × 540 × 1200 |
| Nonce genesis | 3866 | Blok #0 |
| Nonce blok aset #1 | 867 | Sertifikat Tanah No.4471 |
| Ukuran berkas RC4 | 663.923 byte | Berkas JPG yang dienkripsi |
| Difficulty PoW | 3 nol (`000`) | Target hash |
| Jumlah blok saat verifikasi | 2 | Genesis + blok aset |
| Jumlah pengujian unit | 10 | `CryptoSelfCheckTest`, seluruhnya lulus |

Hasil per kriteria:

| Kriteria | Skenario | Hasil yang diamati |
|---|---|---|
| 1 | Aplikasi dibuka tanpa dongle | Berhenti di lapis 1, tombol lanjut tidak dapat dipakai |
| 1 | Mode simulasi debug diaktifkan | Status berubah valid, dapat lanjut ke biometrik |
| 1 | Dongle dicabut saat brankas terbuka | Kembali terkunci ke lapis 1 |
| 2 | Login kata sandi benar | Token sesi AES-CBC tersimpan, brankas terbuka |
| 2 | Login kata sandi salah | Ditolak, "Password salah. Hash tidak cocok." |
| 3 | Enkripsi surat wasiat | Tersimpan sebagai `HEX(IV):BASE64` |
| 3 | Dekripsi kunci benar | Plaintext kembali, kedua kunci dikosongkan |
| 3 | Dekripsi kunci salah | "Kunci salah - blok AES-256 gagal divalidasi (padding tidak cocok)." |
| 4 | Enkripsi berkas | Terbentuk `.rc4vault` |
| 4 | Dekripsi berkas | "SHA-256 cocok - integritas berkas asli terverifikasi.", dokumen terbuka di penampil |
| 5 | Simpan catatan | Kolom tersimpan sebagai heksadesimal, status baris TERKUNCI |
| 5 | Buka dengan kunci benar | Nomor rekening, PIN, dan seed phrase tampil |
| 5 | Buka dengan kunci salah | "Kunci salah - blok 3DES gagal divalidasi (padding tidak cocok)." |
| 6 | Sisip pesan lalu simpan | PNG terbentuk, MSE 0,000084, PSNR 88,87 dB |
| 6 | Ekstraksi kunci benar | "Pesan berhasil diekstrak." |
| 6 | Ekstraksi kunci salah | "Ekstraksi gagal: Header panjang tidak valid (...)"; field kunci dikosongkan |
| 7 | Tambang blok baru | Blok #1 ditambang dengan nonce sah |
| 7 | Verifikasi rantai | "Rantai utuh: 2 blok terverifikasi." |
| 7 | Simulasi ubah data | Blok #1 `RUSAK`, "Rantai rusak: Blok #1 tidak lagi cocok dengan hash yang tercatat." |
| 7 | Reset ledger | Kembali ke satu blok genesis |

---

## 13. Daftar pesan galat siap pakai (untuk tabel pengujian kasus gagal)

| Kasus | Pesan pada antarmuka |
|---|---|
| Berkas lisensi dimodifikasi | "HMAC tidak cocok. Berkas kemungkinan dimodifikasi atau secret berbeda." |
| Volume bukan media dongle | "Lokasi kunci bukan media dongle (penyimpanan internal ditolak)." |
| Dongle tidak tercolok saat buat kunci | "Pembuatan kunci hanya bisa saat dongle tercolok." |
| Dongle dicabut | "Dongle dilepas - lapis 1 terkunci kembali." |
| Username terlalu pendek | "Username minimal 3 karakter." |
| Password terlalu pendek | "Password minimal 6 karakter." |
| Akun ganda | "Username sudah terdaftar." |
| Kata sandi salah | "Password salah. Hash tidak cocok." |
| Kunci teks salah | "Kunci salah - blok AES-256 gagal divalidasi (padding tidak cocok)." |
| Kunci catatan salah | "Kunci salah - blok 3DES gagal divalidasi (padding tidak cocok)." |
| Stego-key salah | "Ekstraksi gagal: Header panjang tidak valid (N byte). Stego-key salah atau gambar bukan hasil sisipan." |
| Checksum berkas tidak cocok | "SHA-256 TIDAK cocok - kunci salah atau berkas berubah." |
| Data blok diubah | "currentHash tidak cocok dengan isi blok (data diubah)." |
| Rantai tidak valid | "Rantai RUSAK pada blok #N." |

Pesan sengaja ditulis dalam bahasa manusia, bukan keluaran mentah pustaka kriptografi, agar tidak
membocorkan detail internal ke layar.

---

## 14. Daftar gambar yang harus disiapkan untuk BAB IV

| Nomor | Isi tangkapan layar | Berkas sumber usulan |
|---|---|---|
| Gambar 4.1 | Layar gerbang dongle (lapis 1) dengan status media USB | `build/fig-gate.png` |
| Gambar 4.2 | Layar login brankas | `build/fig-login.png` |
| Gambar 4.3 | Dasbor brankas setelah login | `build/fig-dashboard.png` |
| Gambar 4.4 | Modul surat wasiat dengan panel Crypto Inspector | `build/fig-wasiat.png` |
| Gambar 4.5 | Modul dokumen warisan: `.rc4vault` + verifikasi SHA-256 | `build/fig-dokumen.png` |
| Gambar 4.6 | Modul catatan penting dengan status TERKUNCI | `build/fig-catatan.png` |
| Gambar 4.7 | Modul galeri memori dengan MSE dan PSNR | `build/r23.png` (tersedia) |
| Gambar 4.8 | Buku besar aset: rantai valid | `build/fig-chain.png` |
| Gambar 4.9 | Buku besar aset: rantai rusak setelah tamper | `build/r25.png` (tersedia) |

Gambar 4.1 sampai 4.6 dan 4.8 masih perlu diambil ulang dari perangkat; dua lainnya sudah ada.

---

## 15. Analisis keamanan dan batasan (wajib muncul di BAB IV)

1. **Kunci HMAC tertanam di APK.** Siapa pun yang memegang APK dapat membangkitkan lisensi dongle
   sendiri. Untuk penggunaan nyata, kunci harus berada di server penerbit lisensi dan berkas
   lisensi diperbarui berkala.
2. **Lisensi tidak terikat nomor seri perangkat.** Satu lisensi dapat disalin ke kartu lain.
   Pengikatan dapat ditambahkan dengan menyertakan nomor seri kartu ke dalam pesan yang
   ditandatangani.
3. **ECB bersifat deterministik.** Plaintext identik menghasilkan ciphertext identik. Mode ini
   dipilih sesuai tuntutan tugas agar baris basis data dapat didekripsi acak; untuk data produksi
   mode CBC atau GCM lebih tepat.
4. **LSB rapuh.** Perubahan ukuran gambar atau kompresi lossy merusak payload. Karena itu keluaran
   selalu PNG.
5. **Blockchain lokal tidak tahan manipulasi basis data.** Penyerang dengan akses ke SQLite dapat
   menambang ulang seluruh rantai sehingga tampak valid. Blockchain terdistribusi mengatasi ini
   melalui konsensus jaringan yang tidak diimplementasikan pada tugas ini.
6. **Difficulty 3 nol hanya setara ratusan hingga ribuan percobaan**, dipilih agar demonstrasi
   cepat, bukan untuk ketahanan komputasi.
7. **Biometrik bergantung pada perangkat.** Perangkat tanpa sensor sidik jari tidak dapat melewati
   lapis 2 kecuali pada build debug.
8. **Simulasi debug melemahkan gerbang.** Sakelar `ENABLE_DONGLE_SIMULATION` dan "Lewati biometrik
   (debug)" hanya ada pada build debug, tetapi tetap harus dinyatakan terbuka di laporan.

Kesimpulan yang jujur untuk subbab ini: LegacyVault layak sebagai media pembelajaran dan
demonstrasi alur kriptografi terapan, bukan sistem penyimpanan warisan bertingkat produksi.

Selama pengujian, validasi dongle fisik belum dapat dilakukan karena media kartu belum tersedia;
lapis 1 diuji melalui mode simulasi, sedangkan seluruh logika deteksi (kelas antarmuka mass storage
dan validasi volume eksternal) tercakup oleh pengujian unit.

---

## 16. Referensi yang boleh dipakai

- Android Developers. (2025). *BiometricPrompt: Show a biometric authentication dialog*.
  https://developer.android.com/reference/androidx/biometric/BiometricPrompt
- Android Developers. (2025). *Android Keystore system*.
  https://developer.android.com/privacy-and-security/keystore
- Android Developers. (2025). *USB host overview*.
  https://developer.android.com/develop/connectivity/usb/host
- Android Developers. (2025). *Access documents and other files from shared storage*.
  https://developer.android.com/training/data-storage/shared/documents-files
- Krawczyk, H., Bellare, M., & Canetti, R. (1997). *HMAC: Keyed-Hashing for Message
  Authentication*. RFC 2104. IETF.
- Munir, R. (2019). *Kriptografi* (Edisi Kedua). Bandung: Informatika.
- Nakamoto, S. (2008). *Bitcoin: A Peer-to-Peer Electronic Cash System*.
  https://bitcoin.org/bitcoin.pdf
- NIST. (2001). *FIPS PUB 197: Advanced Encryption Standard (AES)*. Gaithersburg: NIST.
- NIST. (2015). *FIPS PUB 180-4: Secure Hash Standard*. Gaithersburg: NIST.
- NIST. (2016). *SP 800-38A: Recommendation for Block Cipher Modes of Operation*. Gaithersburg: NIST.
- Schneier, B. (1996). *Applied Cryptography: Protocols, Algorithms, and Source Code in C*
  (2nd ed.). New York: John Wiley & Sons.
- Stallings, W. (2017). *Cryptography and Network Security: Principles and Practice* (7th ed.).
  Boston: Pearson.

---

## 17. Rumus ringkas untuk pengecekan cepat

```
SHA-256 kata sandi   : SHA256(password + salt_hex)
HMAC dongle          : HMAC-SHA256(K, "LEGACYVAULT-DONGLE-v1|serial|issuedEpoch")
Sesi login (AES-128) : Cᵢ = Eₖ(Pᵢ ⊕ Cᵢ₋₁), C₀ = IV,  envelope HEX(IV):BASE64
Vigenère diperluas   : Cᵢ = ((Pᵢ − 32 + (Kᵢ mod m − 32)) mod 95) + 32
AES-256-CBC teks     : kunci = SHA256(passphrase), IV 16 byte acak, HEX(IV):BASE64
RC4 KSA              : j = (j + S[i] + U[i mod 256]) mod 256; swap
RC4 PRGA             : t = (S[i] + S[j]) mod 256; c = S[t] ⊕ p
3DES-ECB             : Cᵢ = Eₖ₃(Dₖ₂(Eₖ₁(Pᵢ))), kunci 24 byte, simpan heksadesimal
LSB                  : p' = p − (p mod 2) + b ; pos(i) = (offset + i × step) mod totalKanal
MSE / PSNR           : MSE = Σ(I − I')²/(W×H) ; PSNR = 10 log₁₀(255²/MSE)
Blok                 : currentHash = SHA256(index‖timestamp‖assetTitle‖beneficiary‖docHash‖prevHash‖nonce)
PoW                  : cari nonce hingga currentHash berawalan "000"
```

---

## 18. Yang harus diisi manusia sebelum laporan dikirim

1. Nama, NIM, kelas, dan dosen pengampu pada sampul.
2. Tangkapan layar Gambar 4.1–4.6 dan 4.8.
3. Tanggal dan tempat presentasi.
4. Konfirmasi apakah validasi dongle fisik sudah dilakukan; bila sudah, perbarui Bagian 15
   paragraf terakhir.
