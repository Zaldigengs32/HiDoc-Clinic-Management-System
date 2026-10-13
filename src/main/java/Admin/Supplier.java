package Admin;

public class Supplier {
    private String idSupplier;
    private String namaSupplier;
    private String alamatSupplier;
    private String teleponSupplier;
    private String status;

    public Supplier(String idSupplier, String namaSupplier, String alamatSupplier, String teleponSupplier, String status) {
        this.idSupplier = idSupplier;
        this.namaSupplier = namaSupplier;
        this.alamatSupplier = alamatSupplier;
        this.teleponSupplier = teleponSupplier;
        this.status = status;
    }

    public String getIdSupplier() {
        return idSupplier;
    }
    public void setIdSupplier(String idSupplier) {
        this.idSupplier = idSupplier;
    }
    public String getNamaSupplier() {
        return namaSupplier;
    }
    public void setNamaSupplier(String namaSupplier) {
        this.namaSupplier = namaSupplier;
    }
    public String getAlamatSupplier() {
        return alamatSupplier;
    }
    public void setAlamatSupplier(String alamatSupplier) {
        this.alamatSupplier = alamatSupplier;
    }
    public String getTeleponSupplier() {
        return teleponSupplier;
    }
    public void setTeleponSupplier(String teleponSupplier) {
        this.teleponSupplier = teleponSupplier;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
