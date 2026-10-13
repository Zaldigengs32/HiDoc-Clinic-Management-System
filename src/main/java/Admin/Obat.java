package Admin;

import javafx.beans.property.*;

public class Obat {
    private final StringProperty idObat;
    private final StringProperty namaObat;
    private final DoubleProperty hargaBeli;
    private final DoubleProperty hargaJual;
    private final IntegerProperty stok;
    private final StringProperty idKategori;
    private final StringProperty namaKategori;
    private final StringProperty status;
    public Obat(String idObat, String namaObat, double hargaBeli, double hargaJual, int stok, String idKategori,  String status, String namaKategori) {
        this.idObat = new SimpleStringProperty(idObat);
        this.namaObat = new SimpleStringProperty(namaObat);
        this.hargaBeli = new SimpleDoubleProperty(hargaBeli);
        this.hargaJual = new SimpleDoubleProperty(hargaJual);
        this.stok = new SimpleIntegerProperty(stok);
        this.idKategori = new SimpleStringProperty(idKategori);
        this.status = new SimpleStringProperty(status);
        this.namaKategori = new SimpleStringProperty(namaKategori);
    }

    public String getIdObat() {
        return idObat.get();
    }
    public StringProperty idObatProperty() {
        return idObat;
    }
    public String getNamaObat() {
        return namaObat.get();
    }
    public StringProperty namaObatProperty() {
        return namaObat;
    }
    public double getHargaBeli() {
        return hargaBeli.get();
    }
    public DoubleProperty hargaBeliProperty() {
        return hargaBeli;
    }
    public double getHargaJual() {
        return hargaJual.get();
    }
    public DoubleProperty hargaJualProperty() {
        return hargaJual;
    }
    public int getStok() {
        return stok.get();
    }
    public IntegerProperty stokProperty() {
        return stok;
    }
    public String getIdKategori() {
        return idKategori.get();
    }
    public StringProperty idKategoriProperty() {
        return idKategori;
    }
    public String getNamaKategori() {
        return namaKategori.get();
    }
    public StringProperty namaKategoriProperty() {
        return namaKategori;
    }
    public String getStatus() {
        return status.get();
    }
    public StringProperty statusProperty() {
        return status;
    }
}