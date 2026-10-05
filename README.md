# Hening

Launcher Android minimalis, bergaya Olauncher dan Niagara, dibuat dengan Kotlin dan Jetpack Compose.
Tanpa ikon di beranda, tanpa iklan, tanpa internet. Ditulis sendiri dari nol (bukan salinan Olauncher).

## Fitur versi 0.1
- **Beranda polos:** jam, tanggal, dan favorit sebagai teks besar. Latar polos (hitam, abu gelap, biru malam, putih).
- **Gestur**
  - Geser ke atas: daftar aplikasi.
  - Geser ke bawah: panel notifikasi.
  - Geser ke kanan atau kiri: pintasan kiri (bawaan Telepon) atau kanan (bawaan Kamera), bisa diganti aplikasi apa saja.
  - Tekan lama beranda: pengaturan.
- **Daftar aplikasi:** pencarian instan (buka otomatis bila hanya satu hasil) dan **penggeser alfabet** di sisi kanan.
- **Ruang:** dua daftar favorit, Santai dan Kerja. Ketuk namanya di bawah beranda untuk berpindah.
- **Tekan lama aplikasi:** jadikan favorit, pindah ke atas, ganti nama, sembunyikan, info, copot.
- Pengaturan: warna latar, ukuran dan rata teks, jumlah favorit (3-8), format jam, nama Ruang, aplikasi tersembunyi.

## Cara pakai
1. Pasang APK dari **Releases** atau artifact di tab **Actions**.
2. Tekan Home, pilih **Hening**, lalu **Selalu**.
3. Geser ke atas, tekan lama aplikasi, lalu pilih **Jadikan favorit**.

## Rencana
- v0.2: jeda sadar sebelum membuka aplikasi tertentu, ketuk dua kali untuk mengunci layar (opsional).
- v0.3: batas waktu dan statistik layar.
- v0.4: Mode Desktop untuk layar lebar (menggabungkan PC Launcher).

## Build
Push ke GitHub, lalu tab **Actions** membuat `Hening-debug.apk` otomatis.
Untuk rilis: `git tag v0.1 && git push origin v0.1`. Atau buka folder ini di Android Studio dan tekan Run.

## Lisensi
MIT, lihat `LICENSE`.
