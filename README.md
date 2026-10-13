<div align="center">

# 🏥 HiDoc — Sistem Informasi Manajemen Klinik

![Java](https://img.shields.io/badge/Java-24-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-17.0.6-1E6BA3?style=for-the-badge&logo=java&logoColor=white)
![SQL Server](https://img.shields.io/badge/SQL_Server-2022-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Apache POI](https://img.shields.io/badge/Apache_POI-5.2.5-darkblue?style=for-the-badge)
![iText PDF](https://img.shields.io/badge/iTextPDF-5.5.13-orange?style=for-the-badge)

**Aplikasi manajemen klinik berbasis desktop dengan sistem multi-role (Admin, Apoteker, Manajer), manajemen obat, layanan check-up, dan laporan otomatis.**

</div>

---

## ✨ Fitur Unggulan

| Fitur | Deskripsi |
|-------|-----------|
| 🔐 **Multi-Role Login** | 3 role berbeda: Admin, Apoteker, Manajer |
| 👥 **Manajemen Karyawan** | CRUD data karyawan klinik |
| 💊 **Manajemen Obat** | Data obat, kategori obat & supplier |
| 🛒 **Pengadaan Obat** | Kelola pembelian obat dari supplier |
| 💰 **Penjualan Obat** | Transaksi penjualan obat ke pasien |
| 🩺 **Layanan Check-Up** | Data & pencatatan layanan pemeriksaan |
| 📊 **Laporan Lengkap** | Export laporan ke PDF & Excel (iText + Apache POI) |
| 🎵 **Background Audio** | Musik latar saat aplikasi berjalan |
| 🔍 **Fitur Pencarian** | Search data di semua tabel |

---

## 👤 Role & Hak Akses

| Role | Akses |
|------|-------|
| 🛡️ **Admin** | Kelola karyawan, obat, kategori obat, supplier, layanan check-up |
| 💊 **Apoteker** | Penjualan obat, pengadaan obat, data cek kesehatan |
| 📈 **Manajer** | Lihat laporan penjualan, pengadaan, & cek kesehatan |

---

## 🛠️ Tech Stack

- **Language:** Java 24
- **UI Framework:** JavaFX 17.0.6 + FXML
- **Database:** Microsoft SQL Server
- **PDF Report:** iText PDF 5.5.13
- **Excel Report:** Apache POI 5.2.5
- **Build Tool:** Apache Maven
- **IDE:** IntelliJ IDEA

---

## 🔄 Alur Aplikasi

```
🟢 Loading Screen
      ↓
🔐 Login Screen (pilih role)
      ↓
┌─────────────────────────────────┐
│  Admin  │  Apoteker  │  Manajer │
└─────────────────────────────────┘
      ↓
  Dashboard sesuai role
```

---

## 🚀 Cara Menjalankan

### Prerequisites
- Java JDK 24+
- Apache Maven 3.x
- Microsoft SQL Server (running)
- IntelliJ IDEA (rekomendasi)

### Setup Database
Sesuaikan konfigurasi koneksi di file `DBConnect.java`:
```java
String url = "jdbc:sqlserver://localhost:1433;databaseName=NamaDatabase";
String user = "username";
String password = "password";
```

### Run Aplikasi
```bash
# Clone repo
git clone https://github.com/Zaldigengs32/HiDoc-Clinic-Management-System.git
cd HiDoc-Clinic-Management-System

# Build & Run
mvn javafx:run
```

---

## 📁 Struktur Project

```
HiDocProject/
├── src/
│   └── main/
│       ├── java/
│       │   ├── Interface/          # Loading & Login screen
│       │   ├── Connection/         # DBConnect - koneksi database
│       │   ├── Admin/
│       │   │   ├── Controller/     # Controller dashboard admin
│       │   │   ├── Main/           # Entry point tiap modul admin
│       │   │   └── *.java          # Model: Karyawan, Obat, Supplier, dll
│       │   ├── Apoteker/
│       │   │   ├── Controller/     # Controller apoteker
│       │   │   └── Main/           # Entry point modul apoteker
│       │   └── Manajer/
│       │       └── Controller/     # Controller laporan manajer
│       └── resources/
│           ├── assets/             # Gambar & logo
│           ├── audio/              # File musik background
│           ├── icons/              # Ikon UI
│           ├── Interface/          # FXML loading & login
│           ├── Admin/              # FXML halaman admin
│           ├── Apoteker/           # FXML halaman apoteker
│           └── Manajer/            # FXML halaman manajer
└── pom.xml
```

---

<div align="center">
<sub>⭐ Kalau project ini bermanfaat, jangan lupa kasih bintang!</sub>
</div>
