"""
Generator laporan Tugas Akhir Kriptografi LegacyVault.

Mengikuti format contoh: 009_011_021_LAPORAN AKHIR KRIPTOGRAFI.docx
  - A4, margin atas 3 cm, bawah 3 cm, kiri 2,5 cm, kanan 2,5 cm
  - Judul utama (Title) 16 pt bold rata tengah
  - BAB (Heading 2) 14 pt bold rata tengah
  - Subbab (Heading 3) bold, nomor ditulis manual
  - Isi 12 pt, spasi 1,5, rata kanan-kiri

Jalankan ulang kapan saja:
    python tools/build_report.py

Untuk mengganti jenis huruf menjadi Times New Roman, ubah BODY_FONT di bawah.
"""

from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "dist" / "LAPORAN AKHIR KRIPTOGRAFI - LegacyVault.docx"

BODY_SIZE = 12
BODY_FONT = None          # None = ikut tema Word (Calibri), seperti contoh
MONO_FONT = "Consolas"

IDENTITAS = [
    ("[NAMA ANGGOTA 1]", "[NIM 1]"),
    ("[NAMA ANGGOTA 2]", "[NIM 2]"),
    ("[NAMA ANGGOTA 3]", "[NIM 3]"),
]
KELAS = "[KELAS]"
TAHUN = "2026"

# Tangkapan layar yang sudah terbukti memuat modul terkait.
# None = belum tersedia; generator menulis placeholder dan caption tetap bernomor.
FIG = ROOT / "build"
FIGURES = {
    "gate": None,
    "login": None,
    "dashboard": None,
    "wasiat": None,
    "dokumen": None,
    "catatan": None,
    "stego": FIG / "r23.png",
    "chain_valid": None,
    "chain_rusak": FIG / "r25.png",
}


# ---------------------------------------------------------------- utilitas --

def set_font(run, size=None, bold=None, italic=None, mono=False):
    if mono:
        run.font.name = MONO_FONT
        run.font.size = Pt(size or 10)
    else:
        if BODY_FONT:
            run.font.name = BODY_FONT
        if size:
            run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic


def para(doc, text="", *, justify=True, indent_left=1.25, first_line=0.75,
         space_before=0, space_after=0, line=1.5, size=BODY_SIZE, bold=False,
         italic=False, align=None):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.alignment = align or (WD_ALIGN_PARAGRAPH.JUSTIFY if justify else WD_ALIGN_PARAGRAPH.LEFT)
    pf.left_indent = Cm(indent_left)
    pf.first_line_indent = Cm(first_line)
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    pf.line_spacing = line
    if text:
        r = p.add_run(text)
        set_font(r, size=size, bold=bold, italic=italic)
    return p


def bullets(doc, items):
    for item in items:
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        pf.left_indent = Cm(1.9)
        pf.first_line_indent = Cm(-0.65)
        pf.line_spacing = 1.5
        r = p.add_run("•\t" + item)
        set_font(r)


def formula(doc, text):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf.left_indent = Cm(1.25)
    pf.space_before = Pt(6)
    pf.space_after = Pt(6)
    pf.line_spacing = 1.0
    r = p.add_run(text)
    set_font(r, mono=True)


def h2(doc, bab, judul, page_break=True):
    if page_break:
        doc.add_page_break()
    p = doc.add_paragraph(style="Heading 2")
    p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_after = Pt(5)
    r1 = p.add_run(bab.upper())
    set_font(r1, size=14, bold=True)
    r1.add_break()
    r2 = p.add_run(judul.upper())
    set_font(r2, size=14, bold=True)
    return p


def h3(doc, judul):
    p = doc.add_paragraph(style="Heading 3")
    pf = p.paragraph_format
    pf.alignment = WD_ALIGN_PARAGRAPH.LEFT
    pf.left_indent = Cm(1.25)
    pf.first_line_indent = Cm(0)
    pf.space_before = Pt(10)
    pf.space_after = Pt(4)
    pf.line_spacing = 1.5
    r = p.add_run(judul)
    set_font(r, size=12, bold=True)
    return p


def caption(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.left_indent = Cm(1.25)
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(10)
    p.paragraph_format.line_spacing = 1.0
    r = p.add_run(text)
    set_font(r, size=10, italic=True)
    return p


def figure(doc, key, nomor, teks, width_cm=6.8):
    path = FIGURES.get(key)
    p = doc.add_paragraph()
    p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.line_spacing = 1.0
    if path and Path(path).exists():
        p.add_run().add_picture(str(path), width=Cm(width_cm))
    else:
        r = p.add_run("[tangkap layar: %s]" % teks)
        set_font(r, size=10, italic=True)
        r.font.color.rgb = RGBColor(0x80, 0x80, 0x80)
    caption(doc, "Gambar %s %s" % (nomor, teks))


def table(doc, nomor, judul, headers, rows, widths=None, size=10):
    caption(doc, "Tabel %s %s" % (nomor, judul))
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = True
    hdr = t.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = ""
        p = hdr[i].paragraphs[0]
        p.paragraph_format.line_spacing = 1.0
        p.paragraph_format.space_after = Pt(2)
        r = p.add_run(h)
        set_font(r, size=size, bold=True)
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            p.paragraph_format.line_spacing = 1.0
            p.paragraph_format.space_after = Pt(2)
            mono = val.startswith("`") and val.endswith("`")
            r = p.add_run(val.strip("`"))
            set_font(r, size=size if not mono else size - 0.5, mono=mono)
    if widths:
        for row in t.rows:
            for i, w in enumerate(widths):
                row.cells[i].width = Cm(w)
    doc.add_paragraph()
    return t


def toc(doc):
    p = doc.add_paragraph()
    run = p.add_run()
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), 'TOC \\o "1-3" \\h \\z \\u')
    inner = OxmlElement("w:r")
    t = OxmlElement("w:t")
    t.text = "Klik kanan di sini lalu pilih Update Field untuk memuat daftar isi."
    inner.append(t)
    fld.append(inner)
    run._r.addnext(fld)


def page_number_footer(section):
    p = section.footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    for kind, text in (("begin", None), (None, "PAGE"), ("end", None)):
        if kind:
            el = OxmlElement("w:fldChar")
            el.set(qn("w:fldCharType"), kind)
        else:
            el = OxmlElement("w:instrText")
            el.set(qn("xml:space"), "preserve")
            el.text = text
        run._r.append(el)
    set_font(run, size=10)


# -------------------------------------------------------------- dokumen --

def build():
    doc = Document()

    normal = doc.styles["Normal"]
    normal.font.size = Pt(BODY_SIZE)
    if BODY_FONT:
        normal.font.name = BODY_FONT

    sec = doc.sections[0]
    sec.page_width = Cm(21)
    sec.page_height = Cm(29.7)
    sec.top_margin = Cm(3)
    sec.bottom_margin = Cm(3)
    sec.left_margin = Cm(2.5)
    sec.right_margin = Cm(2.5)
    page_number_footer(sec)

    # ------------------------------------------------------------- sampul --
    cover = doc.add_paragraph()
    cover.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cover.paragraph_format.space_after = Pt(12)
    set_font(cover.add_run("Laporan Tugas Akhir Kriptografi"), size=16, bold=True)

    judul = doc.add_paragraph()
    judul.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    judul.paragraph_format.space_after = Pt(24)
    judul.paragraph_format.line_spacing = 1.15
    set_font(judul.add_run(
        "Perancangan dan Implementasi Brankas Wasiat Digital Berbasis Android "
        "dengan Dongle Perangkat Keras dan Tujuh Algoritma Kriptografi (LegacyVault)"
    ), size=16, bold=True)

    for _ in range(4):
        doc.add_paragraph()

    lead = doc.add_paragraph()
    lead.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(lead.add_run("Disusun oleh:"), size=12)

    for nama, nim in IDENTITAS:
        p = doc.add_paragraph()
        p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(2)
        set_font(p.add_run("%s / %s" % (nama, nim)), size=12)

    kelas = doc.add_paragraph()
    kelas.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    kelas.paragraph_format.space_before = Pt(6)
    set_font(kelas.add_run("Kelas : %s" % KELAS), size=12)

    for _ in range(6):
        doc.add_paragraph()

    for line in ("PROGRAM STUDI INFORMATIKA", "JURUSAN INFORMATIKA",
                 "FAKULTAS TEKNIK INDUSTRI",
                 "UNIVERSITAS PEMBANGUNAN NASIONAL \u201cVETERAN\u201d YOGYAKARTA",
                 TAHUN):
        p = doc.add_paragraph()
        p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(2)
        set_font(p.add_run(line), size=12, bold=line.startswith("UNIVERSITAS"))

    # ---------------------------------------------------------- daftar isi --
    doc.add_page_break()
    h = doc.add_paragraph(style="Heading 2")
    h.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(h.add_run("DAFTAR ISI"), size=14, bold=True)
    toc(doc)

    # ---------------------------------------------------------------- BAB I --
    h2(doc, "BAB I", "Pendahuluan")

    h3(doc, "1.1  Latar Belakang")
    para(doc, "Kriptografi telah lama menjadi tulang punggung perlindungan informasi, mulai dari "
              "penyandian pesan militer hingga pengamanan transaksi perbankan digital. Meskipun "
              "demikian, pembelajaran kriptografi di bangku kuliah sering kali berhenti pada "
              "perhitungan manual dan simulasi di atas kertas. Akibatnya, mahasiswa memahami rumus "
              "tetapi tidak melihat bagaimana algoritma tersebut bekerja pada data nyata, berapa "
              "besar perubahan yang ditimbulkan, dan apa yang terjadi ketika kunci yang dipakai "
              "salah. Diperlukan sebuah perangkat lunak yang tidak hanya menjalankan algoritma, "
              "tetapi juga memperlihatkan seluruh prosesnya secara terbuka.")
    para(doc, "Kebutuhan tersebut relevan dengan konteks nyata. Sebuah keluarga yang memiliki "
              "harta, rekening, dan dokumen kepemilikan perlu menyimpan catatan warisan secara "
              "aman, sekaligus memastikan bahwa catatan itu hanya dapat dibuka oleh pihak yang "
              "berhak. Data semacam ini menuntut perlindungan berlapis: kredensial akses, "
              "enkripsi teks, enkripsi berkas, enkripsi basis data, penyamaran pada citra, hingga "
              "bukti integritas yang tidak dapat diubah diam-diam.")
    para(doc, "Berdasarkan kebutuhan itu, tugas akhir ini membangun LegacyVault, aplikasi Android "
              "asli berbasis Kotlin dan Jetpack Compose yang berperan sebagai brankas wasiat "
              "digital. Aplikasi tidak dapat dijalankan tanpa dongle perangkat keras berupa media "
              "penyimpanan USB yang membawa berkas lisensi bertanda tangan HMAC-SHA256, lalu "
              "dilanjutkan dengan autentikasi biometrik yang diikat pada kunci Android Keystore. "
              "Setelah gerbang terbuka, lima modul kriptografi dapat digunakan, masing-masing "
              "dengan algoritma yang berbeda dan tidak saling tumpang tindih.")
    para(doc, "Bagian penting dari aplikasi ini adalah panel Crypto Inspector. Setiap modul "
              "menampilkan plaintext, kunci, nilai antara, hingga ciphertext melalui panel yang "
              "dapat dibuka kapan saja. Panel tersebut membuat sifat satu arah fungsi hash, "
              "pengaruh Initialization Vector, kerapuhan Least Significant Bit, dan kegagalan "
              "validasi rantai blok dapat diamati langsung, bukan sekadar dijelaskan secara "
              "teoretis.")

    h3(doc, "1.2  Rumusan Masalah")
    para(doc, "Berdasarkan latar belakang di atas, rumusan masalah pada tugas akhir ini adalah:")
    bullets(doc, [
        "Bagaimana merancang aplikasi brankas wasiat digital berbasis Android yang hanya dapat "
        "dijalankan ketika dongle perangkat keras terpasang dan autentikasi biometrik berhasil?",
        "Bagaimana menerapkan fungsi hash SHA-256 dengan salt acak untuk kata sandi serta "
        "mengamankan token sesi login menggunakan JCrypto AES-CBC?",
        "Bagaimana menggabungkan kriptografi klasik Extended Vigen\u00e8re dan kriptografi modern "
        "AES-256-CBC menjadi super enkripsi pada pesan teks?",
        "Bagaimana mengenkripsi berkas dokumen menggunakan stream cipher RC4 yang diimplementasikan "
        "mandiri beserta verifikasi integritasnya?",
        "Bagaimana mengenkripsi kolom sensitif pada basis data Room/SQLite menggunakan Triple DES "
        "mode ECB sehingga setiap baris dapat didekripsi secara acak?",
        "Bagaimana menyisipkan pesan rahasia ke dalam citra dengan steganografi LSB yang urutan "
        "pikselnya diacak oleh PRNG ber-seed, serta mengukur kualitas citra hasilnya?",
        "Bagaimana membangun buku besar aset berbasis blockchain sederhana dengan Proof-of-Work "
        "dan mekanisme verifikasi rantai?",
    ])

    h3(doc, "1.3  Tujuan")
    para(doc, "Tujuan dari pelaksanaan tugas akhir dan penyusunan laporan ini adalah:")
    bullets(doc, [
        "Membangun aplikasi Android Native bernama LegacyVault sebagai brankas wasiat digital "
        "beserta dokumen pendukungnya.",
        "Menerapkan mekanisme gerbang berlapis berupa validasi dongle USB dan autentikasi "
        "biometrik sebelum brankas dapat diakses.",
        "Menerapkan tujuh kriteria kriptografi wajib pada modul-modul aplikasi dengan algoritma "
        "yang berbeda untuk setiap kriteria.",
        "Mengimplementasikan algoritma klasik, stream cipher, steganografi, dan blockchain secara "
        "mandiri tanpa pustaka pihak ketiga agar logika perhitungannya transparan.",
        "Membuktikan hasil implementasi melalui pengujian fungsional dan pengujian kasus gagal "
        "pada perangkat Android nyata.",
        "Menganalisis batas keamanan dari rancangan yang dibuat agar penggunaan aplikasi tidak "
        "disalahartikan sebagai jaminan keamanan penuh.",
    ])

    h3(doc, "1.4  Batasan Masalah")
    para(doc, "Agar pembahasan terarah, tugas akhir ini dibatasi pada hal-hal berikut:")
    bullets(doc, [
        "Aplikasi berjalan luring sepenuhnya pada satu perangkat Android; tidak ada server, "
        "sinkronisasi, maupun layanan awan.",
        "Target perangkat adalah Android 8.0 (API 26) hingga Android 14 (API 34).",
        "Dongle yang diuji adalah media penyimpanan USB (kartu microSDHC di dalam card reader atau "
        "flashdisk) yang terhubung melalui OTG dan mengandung berkas lisensi bertanda tangan HMAC.",
        "Blockchain yang dibangun bersifat lokal dan tidak terdistribusi; konsensus jaringan tidak "
        "diimplementasikan.",
        "Aplikasi berfungsi sebagai media demonstrasi akademik, bukan produk komersial dengan "
        "audit keamanan.",
    ])

    # --------------------------------------------------------------- BAB II --
    h2(doc, "BAB II", "Dasar Teori")

    h3(doc, "2.1  Dongle Perangkat Keras dan Autentikasi Biometrik")
    para(doc, "Dongle perangkat keras adalah media fisik yang harus hadir agar sistem mau "
              "beroperasi. Berbeda dengan kunci yang disimpan sebagai berkas biasa, dongle "
              "mengikat akses pada keberadaan benda nyata sehingga pencurian data digital saja "
              "tidak cukup untuk membuka sistem. Pada Android, kehadiran dongle dideteksi melalui "
              "dua jalur: lapisan USB dan lapisan penyimpanan.")
    para(doc, "Lapisan USB memanfaatkan UsbManager. Ketika perangkat USB dicolokkan, sistem "
              "mengirim broadcast ACTION_USB_DEVICE_ATTACHED yang membawa deskriptor perangkat. "
              "Aplikasi memeriksa apakah perangkat tersebut menyediakan antarmuka dengan kelas "
              "mass storage (kode 0x08). Pemeriksaan kelas antarmuka dipilih, bukan pemeriksaan "
              "vendorId dan productId, karena kode kelas bersifat standar untuk semua media "
              "penyimpanan USB, sedangkan VID/PID hanya berlaku untuk satu model perangkat.")
    para(doc, "Lapisan penyimpanan memanfaatkan Storage Access Framework (SAF). Pengguna memilih "
              "akar volume kartu melalui ACTION_OPEN_DOCUMENT_TREE, lalu aplikasi membaca berkas "
              "lisensi .legacy_dongle.key pada volume tersebut. Isi berkas berformat "
              "LEGACYVAULT-DONGLE-v1|serial|issuedEpoch|hmacHex, dan keabsahannya diperiksa dengan "
              "HMAC-SHA256. Fungsi HMAC didefinisikan sebagai berikut.")
    formula(doc, "HMAC(K, m) = H((K' \u2295 opad) \u2016 H((K' \u2295 ipad) \u2016 m))")
    para(doc, "Keterangan: H adalah fungsi hash SHA-256, K adalah kunci rahasia, K' adalah kunci "
              "yang telah disesuaikan panjangnya, opad dan ipad adalah padding luar dan dalam, "
              "serta m adalah pesan yang diautentikasi. HMAC dipilih karena mampu mendeteksi "
              "perubahan satu bit pun pada isi berkas lisensi, sehingga berkas lisensi palsu yang "
              "disalin dari perangkat lain tidak akan lolos verifikasi.")
    para(doc, "Setelah dongle dinyatakan valid, aplikasi menampilkan BiometricPrompt yang diikat "
              "pada CryptoObject. Kunci AES dibuat di dalam Android Keystore dengan "
              "setUserAuthenticationRequired(true), sehingga operasi kriptografi pada kunci "
              "tersebut hanya dapat dilakukan setelah pengguna memverifikasi sidik jari. Pola ini "
              "membuat keberhasilan biometrik tidak dapat dipalsukan oleh aplikasi lain karena "
              "verifikasi terjadi di dalam sistem operasi, bukan di dalam aplikasi.")

    h3(doc, "2.2  Hash Kata Sandi SHA-256 dan Sesi AES-CBC")
    para(doc, "Kata sandi tidak boleh disimpan dalam bentuk asli. Aplikasi menyimpan hash satu arah "
              "yang dihitung dengan SHA-256 dan ditambah salt acak 16 byte per akun.")
    formula(doc, "Hash = SHA256(password + salt_hex)")
    para(doc, "Salt acak memastikan dua pengguna dengan kata sandi identik tetap menghasilkan hash "
              "berbeda, sehingga tabel hash yang telah dihitung sebelumnya (rainbow table) tidak "
              "dapat dipakai ulang. SHA-256 bersifat satu arah: dari nilai hash tidak dimungkinkan "
              "memperoleh kembali kata sandi aslinya.")
    para(doc, "Token sesi setelah login tidak disimpan sebagai teks biasa. Payload sesi dienkripsi "
              "dengan JCrypto, yaitu pustaka kriptografi standar Java/Android, menggunakan "
              "AES/CBC/PKCS5Padding berukuran kunci 128 bit. Mode Cipher Block Chaining mengaitkan "
              "setiap blok ciphertext dengan blok sebelumnya melalui operasi XOR.")
    formula(doc, "C\u1d62 = E\u2096(P\u1d62 \u2295 C\u1d62\u208b\u2081),  C\u2080 = IV")
    para(doc, "Keterangan: P adalah blok plaintext, C adalah blok ciphertext, E adalah fungsi "
              "enkripsi AES, K adalah kunci, dan IV adalah Initialization Vector 16 byte acak. "
              "Karena setiap blok bergantung pada blok sebelumnya, pola data yang berulang pada "
              "plaintext tidak terlihat pada ciphertext. Format simpan sesi adalah "
              "HEX(IV):BASE64(ciphertext) agar IV dapat dipakai kembali saat dekripsi.")

    h3(doc, "2.3  Super Enkripsi Teks: Extended Vigen\u00e8re dan AES-256-CBC")
    para(doc, "Super enkripsi berarti pesan dienkripsi dua kali dengan dua algoritma berbeda: satu "
              "dari keluarga kriptografi klasik dan satu dari keluarga kriptografi modern. "
              "Pendekatan ini dipakai pada modul penulisan surat wasiat.")
    para(doc, "Tahap pertama menggunakan Extended Vigen\u00e8re Cipher. Vigen\u00e8re standar hanya "
              "menggeser huruf A\u2013Z sehingga spasi, angka, dan tanda baca hilang atau rusak. "
              "Varian diperluas bekerja pada seluruh karakter ASCII yang dapat dicetak, yaitu "
              "kode desimal 32 sampai 126 sebanyak 95 karakter, sehingga surat tetap terbaca "
              "sebagai teks saat diperlukan.")
    formula(doc, "C\u1d62 = ((P\u1d62 \u2212 32 + (K\u1d62 \u2098\u2092\u2091 \u2098 \u2212 32)) mod 95) + 32")
    formula(doc, "P\u1d62 = ((C\u1d62 \u2212 32 \u2212 (K\u1d62 \u2098\u2092\u2091 \u2098 \u2212 32) + 95) mod 95) + 32")
    para(doc, "Keterangan: P\u1d62 adalah nilai desimal karakter plaintext ke-i, C\u1d62 adalah "
              "ciphertext ke-i, K adalah kunci, dan m adalah panjang kunci. Kunci berulang secara "
              "siklik mengikuti indeks karakter pesan.")
    para(doc, "Tahap kedua mengenkripsi keluaran tahap pertama dengan AES-256-CBC. Kunci 256 bit "
              "diturunkan dari passphrase melalui SHA-256, sedangkan IV 16 byte dibangkitkan acak "
              "untuk setiap pesan. Hasil akhir disimpan dengan format HEX(IV):BASE64(ciphertext). "
              "Dengan dua lapis ini, ciphertext tidak lagi memperlihatkan pola pergeseran "
              "Vigen\u00e8re yang mudah dianalisis secara statistik.")

    h3(doc, "2.4  Enkripsi Berkas dengan Stream Cipher RC4")
    para(doc, "Berkas dokumen seperti sertifikat tanah atau akta notaris berukuran besar dan tidak "
              "selalu berformat teks, sehingga memerlukan algoritma yang berbeda dari modul teks. "
              "RC4 (ARCFOUR) adalah stream cipher yang bekerja byte per byte dan sangat ringan, "
              "sehingga cocok untuk berkas berukuran besar. RC4 terdiri atas dua tahap.")
    para(doc, "Tahap pertama adalah Key-Scheduling Algorithm (KSA) yang menyiapkan larik permutasi "
              "S berisi 256 nilai berdasarkan byte kunci U.")
    formula(doc, "j = (j + S[i] + U[i mod 256]) mod 256;  swap(S[i], S[j])")
    para(doc, "Tahap kedua adalah Pseudo-Random Generation Algorithm (PRGA) yang membangkitkan "
              "aliran kunci K dan melakukan operasi XOR terhadap setiap byte plaintext.")
    formula(doc, "i = (i + 1) mod 256;  j = (j + S[i]) mod 256;  swap(S[i], S[j]);")
    formula(doc, "t = (S[i] + S[j]) mod 256;  K = S[t];  c = K \u2295 p")
    para(doc, "Keterangan: S adalah larik permutasi 256 byte, U adalah kunci, K adalah byte aliran "
              "kunci, p adalah byte plaintext, dan c adalah byte ciphertext. Karena operasi XOR "
              "bersifat simetris, dekripsi dilakukan dengan algoritma yang sama memakai kunci yang "
              "sama. Berkas hasil enkripsi disimpan dengan ekstensi .rc4vault, sedangkan nilai "
              "SHA-256 berkas asli dicatat untuk memverifikasi integritas hasil dekripsi.")

    h3(doc, "2.5  Enkripsi Basis Data dengan Triple DES Mode ECB")
    para(doc, "Catatan finansial seperti nomor rekening, PIN brankas fisik, seed phrase dompet "
              "digital, dan nilai taksiran harta disimpan pada tabel basis data. Sesuai teori "
              "perkuliahan, kolom sensitif tersebut dienkripsi dengan Triple DES mode Electronic "
              "Codebook (ECB) menggunakan kunci 24 byte atau 192 bit.")
    formula(doc, "C\u1d62 = E\u2096\u2083(D\u2096\u2082(E\u2096\u2081(P\u1d62)))")
    para(doc, "Keterangan: P\u1d62 adalah blok plaintext ke-i, E adalah enkripsi DES, D adalah "
              "dekripsi DES, serta K1, K2, dan K3 adalah tiga subkunci 8 byte. Proses dekripsi "
              "membalik urutan menjadi D\u2096\u2081(E\u2096\u2082(D\u2096\u2083(C))).")
    para(doc, "Mode ECB mengenkripsi setiap blok secara independen tanpa Initialization Vector. "
              "Sifat ini menjadi alasan pemilihan mode pada konteks basis data: satu baris dapat "
              "didekripsi tanpa harus membaca baris lain, sehingga data tetap dapat ditampilkan "
              "per catatan. Konsekuensinya juga harus disadari, yaitu plaintext yang identik "
              "menghasilkan ciphertext identik dan pola blok dapat terlihat; karena itu mode ini "
              "tidak dianjurkan untuk enkripsi berkas atau lalu lintas jaringan. Seluruh nilai "
              "kolom sensitif disimpan sebagai string heksadesimal.")

    h3(doc, "2.6  Steganografi LSB dengan PRNG Seed")
    para(doc, "Kriptografi menyembunyikan isi pesan, sedangkan steganografi menyembunyikan "
              "keberadaan pesan. Pada citra digital 24 bit (ARGB_8888), setiap piksel memiliki "
              "kanal merah, hijau, dan biru yang masing-masing disimpan dalam 8 bit. Bit paling "
              "tidak berarti (Least Significant Bit) dari setiap kanal dapat diganti tanpa "
              "perubahan warna yang terlihat mata.")
    formula(doc, "p' = p \u2212 (p mod 2) + b;   b = p' mod 2")
    para(doc, "Keterangan: p adalah nilai kanal asli, p' adalah nilai kanal setelah disisipi, dan b "
              "adalah bit pesan. Sisipan LSB sederhana masih mudah dipecahkan karena urutan piksel "
              "dapat ditebak. Aplikasi ini menutup kelemahan tersebut dengan PRNG: string "
              "stego-key diubah menjadi nilai Long melalui hash dan dipakai sebagai seed "
              "java.util.Random, lalu urutan indeks piksel diambil dari PRNG tersebut.")
    para(doc, "Format payload yang disisipkan adalah 32 bit pertama sebagai panjang pesan dalam "
              "satuan byte, diikuti bit-bit pesan dengan 8 bit per karakter UTF-8. Dengan jumlah "
              "piksel W \u00d7 H dan satu bit per kanal, kapasitas maksimum adalah 3 \u00d7 W "
              "\u00d7 H bit dikurangi header 32 bit.")
    para(doc, "Kualitas citra hasil diukur dengan Mean Squared Error dan Peak Signal-to-Noise "
              "Ratio:")
    formula(doc, "MSE = (1 / (W\u00d7H)) \u03a3 (I(x,y) \u2212 I'(x,y))\u00b2")
    formula(doc, "PSNR = 10 log\u2081\u2080 (MAX\u00b2 / MSE)")
    para(doc, "Keterangan: I adalah citra asli, I' adalah citra hasil, dan MAX adalah nilai "
              "maksimum kanal yaitu 255. Semakin kecil MSE dan semakin besar PSNR, semakin kecil "
              "distorsi yang ditimbulkan. Nilai PSNR di atas 40 dB umumnya dianggap tidak kasat "
              "mata. Hasil wajib disimpan sebagai PNG karena kompresi lossy seperti JPEG akan "
              "mengubah bit LSB dan merusak pesan.")

    h3(doc, "2.7  Blockchain Sederhana dan Proof-of-Work")
    para(doc, "Blockchain adalah rantai blok data yang setiap bloknya menyimpan hash blok "
              "sebelumnya, sehingga perubahan pada satu blok memutus keterkaitan seluruh blok "
              "sesudahnya. Pada aplikasi ini blockchain dipakai sebagai buku besar aset warisan "
              "yang mencatat nama aset, nama ahli waris, dan sidik jari dokumen.")
    formula(doc, "currentHash = SHA256(index \u2016 timestamp \u2016 assetTitle \u2016 "
                "beneficiaryName \u2016 documentSha256 \u2016 previousHash \u2016 nonce)")
    para(doc, "Keterangan: \u2016 menyatakan penyambungan string, timestamp berupa epoch "
              "millisecond, dan nonce adalah bilangan yang dicari. Blok pertama (genesis) memakai "
              "previousHash berisi 64 karakter nol.")
    para(doc, "Proof-of-Work mewajibkan hash blok memenuhi target kesulitan tertentu. Aplikasi "
              "memakai difficulty 3, yaitu hash harus diawali tiga karakter nol. Karena keluaran "
              "fungsi hash tidak dapat diprediksi, satu-satunya cara menemukan nonce yang sah "
              "adalah mencoba berulang kali. Biaya komputasi inilah yang membuat pemalsuan riwayat "
              "menjadi mahal: penyerang tidak cukup mengubah data, tetapi harus menambang ulang "
              "blok yang diubah beserta seluruh blok setelahnya.")
    para(doc, "Validasi rantai dilakukan dengan tiga pemeriksaan pada setiap blok: kecocokan hash "
              "hasil hitung ulang dengan hash yang tercatat, kecocokan previousHash dengan hash "
              "blok di depannya, dan pemenuhan target kesulitan. Blok yang gagal pada salah satu "
              "pemeriksaan ditandai rusak, dan status rantai secara keseluruhan dinyatakan tidak "
              "valid.")

    # -------------------------------------------------------------- BAB III --
    h2(doc, "BAB III", "Perancangan dan Arsitektur Sistem")

    h3(doc, "3.1  Arsitektur Aplikasi")
    para(doc, "LegacyVault dibangun sebagai aplikasi Android Native dengan bahasa Kotlin penuh, "
              "antarmuka Jetpack Compose Material 3, dan pola arsitektur Model-View-ViewModel "
              "yang dipertegas dengan lapisan kriptografi terpisah (Clean Crypto Layer). Pemisahan "
              "ini penting agar logika algoritma tidak tercampur dengan kode tampilan dan dapat "
              "diuji secara terpisah.")
    table(doc, "3.1", "Spesifikasi teknis aplikasi",
          ["Aspek", "Keterangan"],
          [["Bahasa", "Kotlin 100%"],
           ["Minimum SDK", "26 (Android 8.0)"],
           ["Target SDK", "34 (Android 14)"],
           ["Antarmuka", "Jetpack Compose, Material 3, tema terang tunggal"],
           ["Arsitektur", "MVVM + lapisan kriptografi terpisah"],
           ["Basis data", "Room (SQLite), enkripsi per kolom oleh modul internal"],
           ["Kriptografi standar", "javax.crypto (JCrypto) dan java.security.MessageDigest"],
           ["Kriptografi mandiri", "Extended Vigen\u00e8re, RC4, LSB + PRNG, blockchain"],
           ["Penyimpanan berkas", "Direktori internal aplikasi, ekstensi .rc4vault"],
           ["Nama paket", "id.upnyk.legacyvault"]],
          widths=[5.0, 10.5])
    para(doc, "Struktur paket dibagi menjadi empat bagian. Paket core/crypto memuat seluruh mesin "
              "algoritma. Paket core/hardware memuat deteksi dongle USB, pengelola penyimpanan, "
              "verifikasi HMAC, dan pembantu biometrik. Paket core/database memuat entitas Room, "
              "DAO, dan basis data. Paket ui memuat ViewModel, navigasi, komponen, serta layar "
              "untuk setiap modul. Seluruh operasi berat dijalankan pada Dispatchers.IO atau "
              "Dispatchers.Default agar antarmuka tidak membeku.")

    h3(doc, "3.2  Mesin Status Aplikasi")
    para(doc, "Akses ke brankas diatur oleh mesin status deterministik dengan empat keadaan. "
              "Aplikasi tidak dapat melompati keadaan, dan perpindahan hanya terjadi setelah "
              "tindakan pengguna yang eksplisit.")
    table(doc, "3.2", "Transisi keadaan aplikasi",
          ["Keadaan", "Syarat masuk", "Aksi pengguna", "Keadaan berikutnya"],
          [["DONGLE_LOCKED", "Aplikasi dibuka", "Dongle USB mass storage terpasang dan berkas "
            "lisensi lolos HMAC, atau mode simulasi debug aktif", "DONGLE_VERIFIED"],
           ["DONGLE_VERIFIED", "Lapis 1 selesai", "Menekan \u201cLanjut ke biometrik\u201d",
            "BIOMETRIC_LOCKED"],
           ["BIOMETRIC_LOCKED", "Lapis 1 selesai", "BiometricPrompt memvalidasi CryptoObject "
            "Keystore", "AUTH_LOGIN"],
           ["AUTH_LOGIN", "Biometrik selesai", "Username dan kata sandi cocok, token AES dibuat",
            "VAULT_UNLOCKED"],
           ["VAULT_UNLOCKED", "Login berhasil", "Mencabut dongle USB", "DONGLE_LOCKED"]],
          widths=[3.6, 3.6, 5.6, 2.9], size=9.5)
    para(doc, "Aturan penting pada mesin status ini adalah pencabutan dongle saat brankas terbuka "
              "langsung mengunci aplikasi kembali ke keadaan pertama. Dengan demikian dongle "
              "berfungsi sebagai kunci fisik yang harus tetap berada di tempat selama sesi "
              "berlangsung, bukan sekadar syarat saat aplikasi dibuka.")
    para(doc, "Untuk keperluan demonstrasi tanpa perangkat keras, tersedia sakelar simulasi yang "
              "hanya aktif pada build debug. Sakelar ini diikat pada BuildConfig.DEBUG sehingga "
              "tidak ikut terbawa ke build rilis.")

    h3(doc, "3.3  Rancangan Modul Kriptografi")
    para(doc, "Tujuh kriteria wajib dipetakan ke algoritma yang berbeda agar tidak ada modul yang "
              "memakai algoritma yang sama.")
    table(doc, "3.3", "Pemetaan kriteria terhadap algoritma dan konteks penggunaan",
          ["Kriteria", "Konteks", "Algoritma", "Keluaran"],
          [["1", "Gerbang dongle dan biometrik", "USB mass storage, SAF, HMAC-SHA256, Android "
            "Keystore + BiometricPrompt", "Status valid atau terkunci"],
           ["2", "Login aplikasi", "SHA-256 + salt 16 byte, sesi AES-128-CBC (JCrypto)",
            "Heksadesimal hash, token HEX(IV):BASE64"],
           ["3", "Surat wasiat (teks)", "Extended Vigen\u00e8re ASCII 95 karakter + AES-256-CBC",
            "HEX(IV):BASE64"],
           ["4", "Dokumen warisan (berkas)", "Stream cipher RC4 (KSA + PRGA mandiri)",
            "Berkas .rc4vault + SHA-256 asli"],
           ["5", "Catatan finansial (basis data)", "Triple DES 192 bit mode ECB",
            "String heksadesimal per kolom"],
           ["6", "Galeri memori (citra)", "LSB kanal R, G, B + PRNG seed", "Berkas PNG + MSE/PSNR"],
           ["7", "Buku besar aset", "SHA-256 rantai blok + Proof-of-Work 3 nol",
            "Rantai blok terverifikasi"]],
          widths=[1.4, 3.4, 5.9, 4.8], size=9.5)

    h3(doc, "3.4  Rancangan Basis Data dan Format Simpan")
    para(doc, "Basis data Room menyimpan enam entitas. Kolom yang bersifat sensitif tidak disimpan "
              "sebagai teks biasa, melainkan sebagai hasil enkripsi modul kriptografi internal.")
    table(doc, "3.4", "Entitas basis data dan bentuk penyimpanan",
          ["Entitas", "Isi", "Bentuk penyimpanan"],
          [["users", "Akun pengguna", "hash SHA-256 + salt heksadesimal"],
           ["will_messages", "Surat wasiat", "HEX(IV):BASE64 hasil super enkripsi"],
           ["encrypted_files", "Metadata berkas terenkripsi", "Nama .rc4vault + SHA-256 berkas asli"],
           ["critical_notes", "Catatan finansial", "Setiap kolom berupa heksadesimal 3DES-ECB"],
           ["stego_images", "Citra hasil steganografi", "Path PNG, judul, nilai PSNR"],
           ["blockchain_blocks", "Blok buku besar aset", "Seluruh field blok + hash + nonce"]],
          widths=[3.6, 4.6, 7.3], size=9.5)
    para(doc, "Berkas hasil enkripsi RC4 disimpan di direktori internal aplikasi, bukan di "
              "penyimpanan bersama, agar tidak dapat dibaca aplikasi lain. Berkas hasil dekripsi "
              "ditulis sementara ke direktori cache dan dibuka melalui FileProvider ketika "
              "pengguna ingin melihatnya.")
    para(doc, "Dongle menyimpan satu berkas lisensi bernama .legacy_dongle.key pada akar volume "
              "kartu. Aplikasi hanya diizinkan menulis ke volume kartu yang dipilih, bukan ke "
              "penyimpanan internal perangkat, sehingga pembuatan kunci tidak pernah menghasilkan "
              "dongle semu di memori internal.")

    h3(doc, "3.5  Rancangan Antarmuka dan Crypto Inspector")
    para(doc, "Antarmuka dirancang agar dapat dipakai sebagai alat demonstrasi di depan penguji. "
              "Palet yang dipakai adalah krem dan hijau pinus dengan latar terang, sudut kartu "
              "8 dp, tanpa bayangan, serta judul berhuruf serif sistem. Ciphertext tidak pernah "
              "ditampilkan terbuka di daftar; nilai tersimpan disembunyikan di balik tombol "
              "\u201cCiphertext tersimpan\u201d agar layar tetap bersih.")
    para(doc, "Setiap modul memiliki panel Crypto Inspector yang dapat dibuka untuk memperlihatkan "
              "tahapan perhitungan. Panel menampilkan algoritma yang dipakai, masukan, nilai "
              "antara, keluaran, dan kesimpulan singkat. Contohnya pada modul teks, panel "
              "memperlihatkan hasil tahap Vigen\u00e8re sebelum masuk ke AES, sehingga penguji "
              "dapat memastikan bahwa super enkripsi benar-benar dua tahap dan bukan satu algoritma "
              "yang diberi nama berbeda.")
    para(doc, "Setiap formulir mengosongkan bidang masukan setelah diproses, baik pada operasi "
              "enkripsi maupun dekripsi. Tujuannya mencegah pengguna tidak sengaja mengenkripsi "
              "ulang data lama, sekaligus mencegah plaintext tertinggal di layar.")

    # --------------------------------------------------------------- BAB IV --
    h2(doc, "BAB IV", "Hasil Implementasi dan Pengujian")

    h3(doc, "4.1  Tampilan Antarmuka")
    para(doc, "Bagian ini menampilkan hasil akhir implementasi pada perangkat Android. Pengujian "
              "dilakukan pada perangkat fisik dengan resolusi 1080 \u00d7 2400 piksel.")
    figure(doc, "gate", "4.1", "Layar gerbang dongle (lapis 1) dengan status media USB")
    figure(doc, "login", "4.2", "Layar login brankas dengan autentikasi kata sandi")
    figure(doc, "dashboard", "4.3", "Dasbor brankas setelah login berhasil")
    figure(doc, "wasiat", "4.4", "Modul surat wasiat dengan panel Crypto Inspector")
    figure(doc, "dokumen", "4.5", "Modul dokumen warisan: berkas terenkripsi RC4 dan verifikasi "
                                  "SHA-256")
    figure(doc, "catatan", "4.6", "Modul catatan penting: kolom tersimpan dalam bentuk 3DES-ECB")
    figure(doc, "stego", "4.7", "Modul galeri memori: stego-object PNG beserta nilai MSE dan PSNR")
    figure(doc, "chain_valid", "4.8", "Buku besar aset: rantai blok valid setelah verifikasi")
    figure(doc, "chain_rusak", "4.9", "Buku besar aset: rantai rusak setelah simulasi perubahan "
                                      "data")

    h3(doc, "4.2  Pengujian Fungsional")
    para(doc, "Pengujian fungsional dilakukan dengan menjalankan setiap modul memakai data nyata "
              "pada perangkat. Hasilnya dirangkum pada tabel berikut.")
    table(doc, "4.1", "Hasil pengujian fungsional tujuh kriteria",
          ["Kriteria", "Skenario uji", "Hasil yang diamati", "Status"],
          [["1", "Dongle tidak terpasang, lalu mode simulasi diaktifkan",
            "Tombol lanjut terkunci saat dongle tidak ada; setelah simulasi aktif status berubah "
            "menjadi valid dan tombol lanjut dapat ditekan", "Sesuai"],
           ["2", "Login dengan kata sandi benar dan salah",
            "Kata sandi benar membentuk token AES dan membuka brankas; kata sandi salah ditolak",
            "Sesuai"],
           ["3", "Enkripsi surat wasiat, lalu dekripsi dengan kunci benar dan kunci salah",
            "Dekripsi dengan kunci benar mengembalikan teks asli; kunci salah ditolak dengan pesan "
            "blok AES-256 gagal divalidasi", "Sesuai"],
           ["4", "Enkripsi berkas gambar 663.923 byte, lalu dekripsi",
            "Terbentuk berkas .rc4vault; hasil dekripsi cocok dengan SHA-256 berkas asli dan dapat "
            "dibuka pada penampil gambar", "Sesuai"],
           ["5", "Simpan catatan finansial, buka dengan kunci benar dan salah",
            "Baris tersimpan sebagai heksadesimal; kunci benar menampilkan nomor rekening, PIN, dan "
            "seed phrase; kunci salah ditolak", "Sesuai"],
           ["6", "Sisipkan pesan 42 byte pada citra 540 \u00d7 1200, lalu ekstraksi",
            "Terbentuk PNG; MSE 0,000084 dan PSNR 88,87 dB; ekstraksi mengembalikan pesan asli "
            "hanya dengan stego-key benar", "Sesuai"],
           ["7", "Tambang blok aset, verifikasi rantai, lalu simulasi perubahan data",
            "Blok ditambang dengan nonce sah; verifikasi menyatakan rantai utuh; setelah data "
            "diubah blok ditandai rusak", "Sesuai"]],
          widths=[1.3, 4.4, 7.3, 1.5], size=9)
    para(doc, "Selain pengujian di atas, dilakukan pengujian otomatis pada tingkat unit. Sepuluh "
              "pengujian dijalankan tanpa emulator melalui Gradle dan mencakup vektor uji "
              "Vigen\u00e8re, RC4, 3DES-ECB, LSB, blockchain, hash kata sandi, dan aturan gerbang "
              "dongle. Seluruh pengujian berstatus lulus.")
    table(doc, "4.2", "Hasil pengukuran kuantitatif",
          ["Parameter", "Nilai terukur", "Keterangan"],
          [["MSE stego-object", "0,000084", "Selisih rata-rata kuadrat per kanal warna"],
           ["PSNR stego-object", "88,87 dB", "Jauh di atas ambang 40 dB, perubahan tidak kasat mata"],
           ["Payload pesan", "336 bit", "42 karakter UTF-8 termasuk header panjang 32 bit"],
           ["Nonce blok genesis", "3866", "Jumlah percobaan hingga hash berawalan 000"],
           ["Nonce blok aset #1", "867", "Jumlah percobaan pada blok sertifikat tanah"],
           ["Ukuran berkas uji", "663.923 byte", "Berkas gambar yang dienkripsi dengan RC4"],
           ["Difficulty", "3 nol", "Target hash: 000 pada tiga karakter pertama"]],
          widths=[4.4, 3.4, 7.0], size=9.5)

    h3(doc, "4.3  Pengujian Kasus Gagal")
    para(doc, "Pengujian kasus gagal penting untuk memastikan sistem menolak masukan yang tidak "
              "berhak, bukan hanya berhasil pada jalur normal.")
    table(doc, "4.3", "Hasil pengujian kasus gagal",
          ["Kasus", "Tindakan", "Hasil yang diharapkan", "Hasil"],
          [["Dongle tidak terpasang", "Membuka aplikasi tanpa media USB",
            "Aplikasi berhenti pada lapis 1 dan tidak dapat dilanjutkan", "Sesuai"],
           ["Berkas lisensi diubah", "Mengubah satu karakter pada .legacy_dongle.key",
            "Verifikasi HMAC gagal dan dongle dinyatakan tidak valid", "Sesuai"],
           ["Kata sandi salah", "Login dengan kata sandi tidak cocok",
            "Akses ditolak, tidak ada token sesi dibuat", "Sesuai"],
           ["Kunci teks salah", "Dekripsi wasiat dengan passphrase salah",
            "Pesan: blok AES-256 gagal divalidasi (padding tidak cocok)", "Sesuai"],
           ["Stego-key salah", "Ekstraksi pesan dengan kunci berbeda",
            "Pesan: header panjang tidak valid, ekstraksi dibatalkan", "Sesuai"],
           ["Kunci catatan salah", "Membuka baris catatan dengan kunci salah",
            "Pesan: blok 3DES gagal divalidasi (padding tidak cocok)", "Sesuai"],
           ["Data blok diubah", "Menekan simulasi ubah data pada blok #1",
            "Blok ditandai RUSAK dan rantai dinyatakan tidak valid", "Sesuai"],
           ["Dongle dicabut", "Mencabut media USB saat brankas terbuka",
            "Aplikasi kembali terkunci ke lapis 1", "Sesuai"]],
          widths=[3.4, 4.0, 5.6, 1.5], size=9)
    para(doc, "Pesan kesalahan yang tampil pada antarmuka sengaja ditulis dalam bahasa manusia, "
              "bukan pesan mentah pustaka kriptografi. Tujuannya agar pengguna memahami penyebab "
              "kegagalan tanpa harus membaca keluaran teknis, sekaligus menghindari kebocoran "
              "informasi internal ke layar.")

    h3(doc, "4.4  Analisis Keamanan dan Batasan")
    para(doc, "Rancangan yang dibuat memenuhi tujuh kriteria tugas, tetapi memiliki batas yang "
              "perlu dinyatakan terbuka agar tidak disalahartikan sebagai jaminan keamanan penuh.")
    bullets(doc, [
        "Kunci HMAC untuk menandatangani berkas lisensi dongle tertanam di dalam APK. Siapa pun "
        "yang memegang APK dapat membangkitkan lisensi sendiri. Untuk penggunaan nyata, kunci "
        "harus berada di server penerbit lisensi dan berkas lisensi diperbarui secara berkala.",
        "Berkas lisensi tidak terikat pada nomor seri perangkat keras atau nomor seri kartu, "
        "sehingga satu lisensi dapat disalin ke kartu lain. Pengikatan dapat ditambahkan dengan "
        "menyertakan nomor seri kartu ke dalam pesan yang ditandatangani.",
        "Triple DES mode ECB bersifat deterministik: plaintext identik menghasilkan ciphertext "
        "identik dan pola blok dapat terlihat. Mode ini dipilih sesuai tuntutan tugas agar baris "
        "basis data dapat didekripsi secara acak. Untuk data produksi, mode CBC atau GCM dengan "
        "IV acak lebih tepat.",
        "Steganografi LSB rapuh terhadap perubahan apa pun, termasuk pengubahan ukuran gambar dan "
        "kompresi lossy. Kerapuhan ini merupakan konsekuensi sifat LSB, bukan kesalahan "
        "implementasi, dan karena itu keluaran disimpan sebagai PNG.",
        "Blockchain yang dibangun bersifat lokal. Penyerang yang memiliki akses ke basis data dapat "
        "menambang ulang seluruh rantai sehingga hasilnya tampak valid. Blockchain terdistribusi "
        "mengatasi hal ini melalui konsensus jaringan, yang tidak diimplementasikan pada tugas ini.",
        "Difficulty 3 nol hanya membutuhkan ratusan hingga ribuan percobaan, sehingga tidak "
        "memberikan ketahanan komputasi yang berarti. Nilai tersebut dipilih agar demonstrasi tetap "
        "cepat di perangkat.",
        "Autentikasi biometrik bergantung pada Android Keystore perangkat. Perangkat tanpa sensor "
        "sidik jari tidak dapat melewati lapis kedua kecuali pada build debug.",
    ])
    para(doc, "Dengan batasan tersebut, LegacyVault layak diposisikan sebagai media pembelajaran dan "
              "demonstrasi alur kriptografi terapan, bukan sebagai sistem penyimpanan warisan "
              "bertingkat produksi.")

    # ---------------------------------------------------------------- BAB V --
    h2(doc, "BAB V", "Penutup")

    h3(doc, "5.1  Kesimpulan")
    para(doc, "Berdasarkan perancangan, implementasi, dan pengujian yang telah dilakukan, dapat "
              "disimpulkan beberapa hal berikut.")
    bullets(doc, [
        "Aplikasi brankas wasiat digital LegacyVault berhasil dibangun sebagai aplikasi Android "
        "Native berbasis Kotlin dan Jetpack Compose dengan pola MVVM dan lapisan kriptografi "
        "terpisah.",
        "Ketujuh kriteria kriptografi terpenuhi dengan algoritma yang berbeda untuk setiap "
        "kriteria, yaitu HMAC-SHA256 dan biometrik untuk gerbang, SHA-256 bersalt dan AES-CBC "
        "untuk login, Extended Vigen\u00e8re dan AES-256-CBC untuk teks, RC4 untuk berkas, "
        "3DES-ECB untuk basis data, LSB dengan PRNG untuk citra, serta SHA-256 dengan "
        "Proof-of-Work untuk buku besar aset.",
        "Algoritma klasik, stream cipher, steganografi, dan blockchain diimplementasikan mandiri "
        "dalam Kotlin murni sehingga setiap langkah perhitungan dapat diperiksa melalui panel "
        "Crypto Inspector.",
        "Gerbang berlapis bekerja sesuai rancangan: aplikasi tidak dapat diakses tanpa dongle "
        "valid dan biometrik berhasil, serta kembali terkunci ketika dongle dicabut.",
        "Pengujian kasus gagal menunjukkan sistem menolak kunci yang salah, berkas lisensi yang "
        "diubah, dan data blok yang dimanipulasi.",
        "Kualitas stego-object sangat baik dengan PSNR 88,87 dB, dan integritas berkas hasil "
        "dekripsi RC4 terverifikasi melalui SHA-256.",
    ])

    h3(doc, "5.2  Saran")
    para(doc, "Beberapa pengembangan yang dapat dilakukan pada penelitian atau tugas berikutnya:")
    bullets(doc, [
        "Memindahkan kunci HMAC penerbit lisensi ke server dan mengikat lisensi pada nomor seri "
        "perangkat keras dongle.",
        "Mengganti mode ECB pada basis data menjadi CBC atau GCM dengan IV acak, disertai migrasi "
        "data lama.",
        "Menambahkan skema Encrypt-then-MAC pada enkripsi berkas agar integritas dapat diperiksa "
        "sebelum dekripsi.",
        "Menerapkan blockchain dengan beberapa simpul lokal agar konsensus dan penolakan rantai "
        "palsu dapat didemonstrasikan.",
        "Menambahkan fitur pencadangan terenkripsi dan pemulihan akun tanpa melemahkan model "
        "keamanan.",
    ])

    # ------------------------------------------------------- daftar pustaka --
    doc.add_page_break()
    h = doc.add_paragraph(style="Heading 2")
    h.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_font(h.add_run("DAFTAR PUSTAKA"), size=14, bold=True)

    refs = [
        "Android Developers. (2025). BiometricPrompt: Show a biometric authentication dialog. "
        "Diakses dari https://developer.android.com/reference/androidx/biometric/BiometricPrompt",
        "Android Developers. (2025). Android Keystore system. Diakses dari "
        "https://developer.android.com/privacy-and-security/keystore",
        "Android Developers. (2025). USB host overview. Diakses dari "
        "https://developer.android.com/develop/connectivity/usb/host",
        "Android Developers. (2025). Access documents and other files from shared storage. "
        "Diakses dari https://developer.android.com/training/data-storage/shared/documents-files",
        "Krawczyk, H., Bellare, M., & Canetti, R. (1997). HMAC: Keyed-Hashing for Message "
        "Authentication. RFC 2104. IETF.",
        "Munir, R. (2019). Kriptografi (Edisi Kedua). Bandung: Informatika.",
        "Nakamoto, S. (2008). Bitcoin: A Peer-to-Peer Electronic Cash System. "
        "https://bitcoin.org/bitcoin.pdf",
        "National Institute of Standards and Technology. (2001). FIPS PUB 197: Advanced "
        "Encryption Standard (AES). Gaithersburg: NIST.",
        "National Institute of Standards and Technology. (2015). FIPS PUB 180-4: Secure Hash "
        "Standard. Gaithersburg: NIST.",
        "National Institute of Standards and Technology. (2016). Special Publication 800-38A: "
        "Recommendation for Block Cipher Modes of Operation. Gaithersburg: NIST.",
        "Schneier, B. (1996). Applied Cryptography: Protocols, Algorithms, and Source Code in C "
        "(2nd ed.). New York: John Wiley & Sons.",
        "Stallings, W. (2017). Cryptography and Network Security: Principles and Practice "
        "(7th ed.). Boston: Pearson.",
    ]
    for ref in refs:
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        pf.left_indent = Cm(1.25)
        pf.first_line_indent = Cm(-1.25)
        pf.line_spacing = 1.0
        pf.space_after = Pt(8)
        set_font(p.add_run(ref), size=12)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc.save(OUT)
    print("tersimpan:", OUT)
    print("paragraf:", len(doc.paragraphs), "| tabel:", len(doc.tables))


if __name__ == "__main__":
    build()
