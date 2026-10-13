package Apoteker;

import javafx.beans.property.*;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PengadaanObat {
    private final StringProperty idPengadaan;
    private final StringProperty idKaryawan;
    private final StringProperty namaKaryawan;
    private final StringProperty idSupplier;
    private final StringProperty namaSupplier;
    private final ObjectProperty<LocalDateTime> tanggalPembelian;
    private final DoubleProperty totalHarga;
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public PengadaanObat(String idPengadaan, String idKaryawan, String namaKaryawan,
                         String idSupplier, String namaSupplier,
                         LocalDateTime tanggalPembelian, double totalHarga) {
        this.idPengadaan = new SimpleStringProperty(idPengadaan);
        this.idKaryawan = new SimpleStringProperty(idKaryawan);
        this.namaKaryawan = new SimpleStringProperty(namaKaryawan);
        this.idSupplier = new SimpleStringProperty(idSupplier);
        this.namaSupplier = new SimpleStringProperty(namaSupplier);
        this.tanggalPembelian = new SimpleObjectProperty<>(tanggalPembelian);
        this.totalHarga = new SimpleDoubleProperty(totalHarga);
    }
    public PengadaanObat(String idKaryawan, String namaKaryawan) {
        this(generateIdPengadaan(), idKaryawan, namaKaryawan, "", "",
                LocalDateTime.now(), 0.0);
    }

    public PengadaanObat() {
        this("", "", "", "", "", LocalDateTime.now(), 0.0);
    }
    public StringProperty idPengadaanProperty() {
        return idPengadaan;
    }
    public StringProperty idKaryawanProperty() {
        return idKaryawan;
    }
    public StringProperty namaKaryawanProperty() {
        return namaKaryawan;
    }
    public StringProperty idSupplierProperty() {
        return idSupplier;
    }
    public StringProperty namaSupplierProperty() {
        return namaSupplier;
    }
    public ObjectProperty<LocalDateTime> tanggalPembelianProperty() {
        return tanggalPembelian;
    }
    public DoubleProperty totalHargaProperty() {
        return totalHarga;
    }
    public String getIdPengadaan() {
        return idPengadaan.get();
    }
    public String getIdKaryawan() {
        return idKaryawan.get();
    }
    public String getNamaKaryawan() {
        return namaKaryawan.get();
    }
    public String getIdSupplier() {
        return idSupplier.get();
    }
    public String getNamaSupplier() {
        return namaSupplier.get();
    }
    public LocalDateTime getTanggalPembelian() {
        return tanggalPembelian.get();
    }
    public double getTotalHarga() {
        return totalHarga.get();
    }
    public String getTotalHargaFormatted() {
        return CURRENCY_FORMATTER.format(getTotalHarga());
    }
    public String getTanggalPembelianFormatted() {
        return getTanggalPembelian() != null ?
                getTanggalPembelian().format(DATE_FORMATTER) : "";
    }
    public String getTanggalPembelianForDatabase() {
        return getTanggalPembelian() != null ?
                getTanggalPembelian().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }

    public void setIdPengadaan(String idPengadaan) {
        this.idPengadaan.set(idPengadaan);
    }
    public void setIdKaryawan(String idKaryawan) {
        this.idKaryawan.set(idKaryawan);
    }
    public void setNamaKaryawan(String namaKaryawan) {
        this.namaKaryawan.set(namaKaryawan);
    }
    public void setIdSupplier(String idSupplier) {
        this.idSupplier.set(idSupplier);
    }
    public void setNamaSupplier(String namaSupplier) {
        this.namaSupplier.set(namaSupplier);
    }
    public void setTanggalPembelian(LocalDateTime tanggalPembelian) {
        this.tanggalPembelian.set(tanggalPembelian);
    }
    public void setTotalHarga(double totalHarga) {
        this.totalHarga.set(totalHarga);
    }
    public static String generateIdPengadaan() {
        long timestamp = System.currentTimeMillis();
        int lastThreeDigits = (int) (timestamp % 1000);
        return String.format("TRPO%03d", lastThreeDigits);
    }

    public boolean isValid() {
        return !getIdPengadaan().trim().isEmpty() &&
                !getIdKaryawan().trim().isEmpty() &&
                !getIdSupplier().trim().isEmpty() &&
                getTanggalPembelian() != null &&
                getTotalHarga() >= 0;
    }

    public PengadaanObat copy() {
        return new PengadaanObat(
                getIdPengadaan(),
                getIdKaryawan(),
                getNamaKaryawan(),
                getIdSupplier(),
                getNamaSupplier(),
                getTanggalPembelian(),
                getTotalHarga()
        );
    }

    @Override
    public String toString() {
        return String.format("PengadaanObat{id='%s', karyawan='%s', supplier='%s', tanggal=%s, total=%s}",
                getIdPengadaan(), getNamaKaryawan(), getNamaSupplier(),
                getTanggalPembelianFormatted(), getTotalHargaFormatted());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PengadaanObat that = (PengadaanObat) obj;
        return getIdPengadaan() != null ?
                getIdPengadaan().equals(that.getIdPengadaan()) :
                that.getIdPengadaan() == null;
    }

    @Override
    public int hashCode() {
        return getIdPengadaan() != null ? getIdPengadaan().hashCode() : 0;
    }
}