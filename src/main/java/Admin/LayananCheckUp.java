package Admin;

public class LayananCheckUp {
    private String IDLayanan;
    private String namaLayanan;
    private String deskripsi;
    private String biayaLayanan;
    private String status;
    public LayananCheckUp(String IDLayanan, String namaLayanan, String deskripsi, String biayaLayanan, String status) {
        this.IDLayanan = IDLayanan;
        this.namaLayanan = namaLayanan;
        this.deskripsi = deskripsi;
        this.biayaLayanan = biayaLayanan;
        this.status = status;
    }

    public String getIDLayanan() {return IDLayanan;}
    public String getNamaLayanan() {return namaLayanan;}
    public String getDeskripsi() {return deskripsi;}
    public String getBiayaLayanan() {return biayaLayanan;}
    public String getStatus() {return status;}
    public void setIDLayanan(String IDLayanan) {this.IDLayanan = IDLayanan;}
    public void setNamaLayanan(String namaLayanan) {this.namaLayanan = namaLayanan;}
    public void setDeskripsi(String deskripsi) {this.deskripsi = deskripsi;}
    public void setBiayaLayanan(String biayaLayanan) {this.biayaLayanan = biayaLayanan;}
    public void setStatus(String status) {this.status = status;}
}