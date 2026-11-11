# LegacyVault

Aplikasi Android untuk Tugas Akhir mata kuliah Kriptografi di Program Studi Informatika UPN
"Veteran" Yogyakarta. LegacyVault adalah brankas wasiat digital. Wasiat, dokumen, dan catatan
keuangan keluarga disimpan di dalam HP, dan aplikasinya tidak bisa dibuka begitu saja: harus ada
dongle USB yang tercolok, sidik jari pemiliknya cocok, lalu password akun benar.

Setelah brankas terbuka ada lima modul. Tiap modul memakai algoritma yang berbeda, karena tugasnya
meminta tujuh kriteria dengan algoritma yang tidak boleh berulang.

Aplikasinya berjalan tanpa internet. Tidak ada server dan tidak ada sinkronisasi, jadi data di HP
satu orang tidak bisa dilihat dari HP lain.

## Unduh APK

Berkasnya ada langsung di halaman ini, bukan di dalam folder:

| Berkas | Untuk siapa | Ukuran |
|---|---|---|
| [LegacyVault-1.0-release.apk](LegacyVault-1.0-release.apk) | penguji dan demo resmi | 9,5 MB |
| [LegacyVault-1.0-dev.apk](LegacyVault-1.0-dev.apk) | anggota kelompok, ada mode simulasi | 13,2 MB |

Klik nama berkasnya, lalu tekan **Download raw file** di kanan atas. Jangan pakai pratinjau GitHub,
hasilnya tidak bisa dibuka Android.

Cara memasang ada di [BACA-DULU.txt](BACA-DULU.txt). Checksum SHA-256 di
[SHA256SUMS.txt](SHA256SUMS.txt). Laporan akhirnya di
[dist/LAPORAN AKHIR KRIPTOGRAFI - LegacyVault.docx](dist/LAPORAN%20AKHIR%20KRIPTOGRAFI%20-%20LegacyVault.docx).

### Ambil satu saja, sebenarnya

Keduanya bisa dipasang bersamaan karena nama paketnya beda, dan datanya juga terpisah. Tapi untuk
penilaian cukup yang rilis.

| | Rilis | DEV |
|---|---|---|
| Nama paket | `id.upnyk.legacyvault` | `id.upnyk.legacyvault.dev` |
| Nama di layar | LegacyVault | LegacyVault DEV |
| Ikon | latar hijau | latar jingga |
| Tombol simulasi dongle | tidak ada | ada |
| Tombol lewati biometrik | tidak ada | ada |

Versi rilis tidak punya jalan pintas. Dua tombol debug yang ada di versi DEV hanya muncul kalau
`BuildConfig.DEBUG` bernilai benar, jadi tidak mungkin ikut ke build rilis. Itu bentuk aplikasi
yang benar-benar memenuhi syarat "hanya dapat dihidupkan menggunakan dongle".

## Tujuh kriteria

| Kriteria | Dipakai untuk | Algoritma | Yang tersimpan |
|---|---|---|---|
| 1 | Gerbang aplikasi | deteksi USB mass storage, SAF, HMAC-SHA256, Android Keystore, BiometricPrompt | status valid atau terkunci |
| 2 | Login | SHA-256 + salt 16 byte, sesi AES-128-CBC lewat JCrypto | hash heksadesimal dan token `HEX(IV):BASE64` |
| 3 | Surat wasiat | Extended Vigenère 95 karakter, lalu AES-256-CBC | `HEX(IV):BASE64` |
| 4 | Dokumen warisan | stream cipher RC4, KSA dan PRGA ditulis sendiri | berkas `.rc4vault` + SHA-256 asli |
| 5 | Catatan keuangan | Triple DES 192 bit mode ECB | string heksadesimal per kolom |
| 6 | Galeri memori | LSB kanal R/G/B dengan urutan acak dari PRNG | PNG + nilai MSE dan PSNR |
| 7 | Buku besar aset | rantai blok SHA-256 dengan Proof-of-Work tiga nol | tabel `blockchain_ledger` |

Yang dipakai: Kotlin, Jetpack Compose Material 3, MVVM, Room, minSdk 26, targetSdk 34. Vigenère,
RC4, steganografi, dan blockchain ditulis sendiri tanpa pustaka pihak ketiga. AES, 3DES, dan
SHA-256 pakai `javax.crypto` dan `java.security.MessageDigest` sesuai arahan tugas.

Tiap modul punya panel Crypto Inspector yang bisa dibuka untuk memperlihatkan plaintext, kunci,
nilai antara, dan ciphertext. Panel ini dibuat supaya bisa langsung didemokan waktu sidang, bukan
cuma jadi lampiran laporan.

## Struktur proyek

```
app/src/main/java/id/upnyk/legacyvault/
  core/crypto/     Vigenere+AES, RC4, 3DES, LSB, blockchain, HMAC dongle
  core/hardware/   deteksi USB, SAF, verifikasi HMAC, BiometricPrompt
  core/session/    token sesi AES-CBC di SharedPreferences
  core/database/   Room: 6 entitas dan DAO-nya
  ui/              ViewModel, navigasi, layar tiap modul, panel Crypto Inspector
app/src/test/      9 pengujian unit algoritma (CryptoSelfCheckTest)
docs/              dokumen arsitektur untuk bahan laporan
tools/             generator dokumen laporan (python-docx)
dist/              laporan akhir dan salinan dokumen arsitektur
```

Urutan gerbangnya:

```
DONGLE_LOCKED --(USB mass storage + HMAC valid)--> DONGLE_VERIFIED
              --(biometrik Keystore)--> AUTH_LOGIN
              --(SHA-256 + salt cocok)--> VAULT_UNLOCKED
              --(dongle dicabut)--> DONGLE_LOCKED
```

Penjelasan lengkap tiap algoritma, mesin status, dan skema basis data ada di
[docs/ARSITEKTUR-LegacyVault.md](docs/ARSITEKTUR-LegacyVault.md).

## Cara build

Butuh JDK 17 atau lebih baru dan Android SDK 37.

```bash
./gradlew assembleDebug      # LegacyVault DEV, id.upnyk.legacyvault.dev
./gradlew assembleRelease    # LegacyVault, id.upnyk.legacyvault
./gradlew testDebugUnitTest  # 9 pengujian unit algoritma
```

APK rilis ditandatangani pakai keystore debug, bukan keystore rilis. Ini memang disengaja karena
hanya untuk demo kampus dan tidak akan masuk Play Store. Akibatnya, APK rilis yang dibangun di
komputer lain punya tanda tangan berbeda. Tetap bisa dipasang, tapi tidak bisa menimpa pemasangan
yang sudah ada.

## Hasil pengujian

Diuji di Poco F5, layar 1080 × 2400.

| Parameter | Nilai |
|---|---|
| MSE stego-object | 0,000084 |
| PSNR stego-object | 88,87 dB |
| Pesan yang disisipkan | 336 bit (42 byte UTF-8) |
| Nonce blok genesis | 3866 |
| Nonce blok aset pertama | 867 |
| Difficulty Proof-of-Work | 3 nol |
| Berkas uji RC4 | 663.923 byte, checksum SHA-256 cocok |
| Pengujian unit | 9, semuanya lulus |

## Yang belum bisa dijamin

Aplikasi ini dibuat untuk demonstrasi di kelas, bukan untuk menyimpan wasiat sungguhan. Beberapa
hal sengaja kami tulis terbuka di sini supaya tidak terkesan lebih aman dari kenyataannya.

Kunci HMAC untuk dongle tertanam di dalam kode dan ikut terbawa ke APK. Karena repositori ini
publik, siapa pun bisa membuat berkas lisensi `.legacy_dongle.key` sendiri lalu dongle apa pun
dianggap sah. Untuk pemakaian nyata, kunci itu harus berada di server penerbit lisensi. Lisensi
juga tidak terikat nomor seri kartu, jadi bisa disalin ke kartu lain.

Triple DES mode ECB bersifat deterministik: plaintext yang sama selalu menghasilkan ciphertext
yang sama. Mode ini dipilih karena tuntutan tugas, supaya satu baris basis data bisa didekripsi
sendiri tanpa bergantung baris lain.

Steganografi LSB mudah rusak kalau gambarnya diubah ukuran atau dikompresi. Karena itu hasilnya
selalu disimpan sebagai PNG.

Blockchain di sini lokal, bukan terdistribusi. Orang yang punya akses ke berkas basis data bisa
menambang ulang seluruh rantai sehingga tampak valid. Blockchain sungguhan menutup celah itu lewat
konsensus antar komputer, dan bagian itu tidak ada di tugas ini. Difficulty tiga nol juga hanya
butuh ratusan sampai ribuan percobaan, dipilih supaya demonya cepat.

Terakhir, validasi dongle fisik belum sempat diuji dengan media kartu yang sebenarnya karena card
reader dan kartunya belum tersedia saat pengujian. Lapis 1 selama ini diuji lewat mode simulasi
yang cuma ada di build debug. Logika deteksi kelas mass storage dan validasi volume eksternal sudah
tercakup pengujian unit, tapi pengujian di perangkat keras asli masih jadi pekerjaan yang tersisa.

## Kredit

Tugas akhir Program Studi Informatika, Fakultas Teknik Industri, Universitas Pembangunan Nasional
"Veteran" Yogyakarta, 2026. Daftar penyusun ada di sampul laporan. Kode sumber ini dipublikasikan
untuk keperluan penilaian akademik.

## English

LegacyVault is a native Android app we built for a Cryptography final project. It works as a
digital will vault: it only unlocks when a USB dongle is plugged in, the owner's fingerprint
matches, and the account password is correct. After that, five modules are available, each using a
different algorithm.

The stack is Kotlin, Jetpack Compose Material 3, MVVM and Room, with minSdk 26 and targetSdk 34.
Extended Vigenère, RC4, LSB steganography and the blockchain are written from scratch; AES, 3DES
and SHA-256 use `javax.crypto` and `MessageDigest` as the assignment requires.

The APKs sit in the repository root. `LegacyVault-1.0-release.apk` is the real product with no
bypass buttons. `LegacyVault-1.0-dev.apk` adds a dongle simulation and a "skip biometric" button,
both gated behind `BuildConfig.DEBUG`. The release build is signed with the debug keystore because
it is only meant for a campus demo.

A fair warning: the dongle HMAC secret ships inside the APK, so anyone can mint a license file.
Other limitations are described in the Indonesian section above.
