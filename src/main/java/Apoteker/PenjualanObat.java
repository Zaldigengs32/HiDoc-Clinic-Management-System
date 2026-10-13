package Apoteker;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PenjualanObat {
    private final StringProperty idPenjualan;
    private final StringProperty idKaryawan;
    private final StringProperty tanggalPembelian;
    private final StringProperty totalHarga;

    public PenjualanObat(String idPenjualan, String idKaryawan, String tanggalPembelian, String totalHarga) {
        this.idPenjualan = new SimpleStringProperty(idPenjualan);
        this.idKaryawan = new SimpleStringProperty(idKaryawan);
        this.tanggalPembelian = new SimpleStringProperty(tanggalPembelian);
        this.totalHarga = new SimpleStringProperty(totalHarga);
    }

    public StringProperty idPenjualanProperty() {
        return idPenjualan;
    }
    public StringProperty idKaryawanProperty() {
        return idKaryawan;
    }
    public StringProperty tanggalPembelianProperty() {
        return tanggalPembelian;
    }
    public StringProperty totalHargaProperty() {
        return totalHarga;
    }
    public boolean getIdPenjualan() {
    return null != idPenjualan.get();}
    public boolean getIdKaryawan() {
        return null != idKaryawan.get();}
    public String getTanggalPembelian() {
        return null;
    }
}
