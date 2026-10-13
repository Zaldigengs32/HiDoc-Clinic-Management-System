package Apoteker;

import javafx.beans.property.*;
import java.text.NumberFormat;
import java.util.Locale;

public class DetailPengadaanObat {

    private final StringProperty idPengadaan;
    private final StringProperty idObat;
    private final StringProperty namaObat;
    private final IntegerProperty kuantitas;
    private final DoubleProperty hargaSatuan;
    private final DoubleProperty subTotal;
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    public DetailPengadaanObat(String idPengadaan, String idObat, String namaObat, int kuantitas, double hargaSatuan) {
        this.idPengadaan = new SimpleStringProperty(idPengadaan);
        this.idObat = new SimpleStringProperty(idObat);
        this.namaObat = new SimpleStringProperty(namaObat);
        this.kuantitas = new SimpleIntegerProperty(kuantitas);
        this.hargaSatuan = new SimpleDoubleProperty(hargaSatuan);
        this.subTotal = new SimpleDoubleProperty(kuantitas * hargaSatuan);
    }

    public DetailPengadaanObat() {
        this("", "", "", 0, 0.0);
    }

    public StringProperty idPengadaanProperty() {return idPengadaan;}
    public StringProperty idObatProperty() {return idObat;}
    public StringProperty namaObatProperty() {return namaObat;}
    public IntegerProperty kuantitasProperty() {return kuantitas;}
    public DoubleProperty hargaSatuanProperty() {return hargaSatuan;}
    public DoubleProperty subTotalProperty() {return subTotal;}
    public String getIdPengadaan() {return idPengadaan.get();}
    public String getIdObat() {return idObat.get();}
    public String getNamaObat() {return namaObat.get();}
    public int getKuantitas() {return kuantitas.get();}
    public double getHargaSatuan() {return hargaSatuan.get();}
    public double getSubTotal() {return subTotal.get();}
    public String getHargaSatuanFormatted() {
        return CURRENCY_FORMATTER.format(getHargaSatuan());
    }
    public String getSubTotalFormatted() {
        return CURRENCY_FORMATTER.format(getSubTotal());
    }
    public void setIdPengadaan(String idPengadaan) {this.idPengadaan.set(idPengadaan);}
    public void setIdObat(String idObat) {this.idObat.set(idObat);}
    public void setNamaObat(String namaObat) {this.namaObat.set(namaObat);}
    public void setKuantitas(int kuantitas) {this.kuantitas.set(kuantitas);updateSubTotal();
    }

    public void setHargaSatuan(double hargaSatuan) {
        this.hargaSatuan.set(hargaSatuan);
        updateSubTotal();
    }

    private void updateSubTotal() {
        this.subTotal.set(getKuantitas() * getHargaSatuan());
    }
    public boolean isValid() {
        return !getIdObat().trim().isEmpty() &&
                !getNamaObat().trim().isEmpty() &&
                getKuantitas() > 0 &&
                getHargaSatuan() > 0;
    }

    @Override
    public String toString() {
        return String.format("DetailPengadaanObat{idPengadaan='%s', obat='%s', kuantitas=%d, harga=%s, subtotal=%s}",
                getIdPengadaan(), getNamaObat(), getKuantitas(),
                getHargaSatuanFormatted(), getSubTotalFormatted());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DetailPengadaanObat that = (DetailPengadaanObat) obj;
        return getIdPengadaan().equals(that.getIdPengadaan()) &&
                getIdObat().equals(that.getIdObat());
    }

    @Override
    public int hashCode() {
        return getIdPengadaan().hashCode() + getIdObat().hashCode();
    }
}