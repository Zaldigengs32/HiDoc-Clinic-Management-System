package Admin;

public class Karyawan {
    private String id, nama, jabatan, alamat, telepon, username, password, status;
    public Karyawan(String id, String nama, String jabatan, String alamat, String telepon, String username, String password, String status) {
        this.id = id; this.nama = nama; this.jabatan = jabatan; this.alamat = alamat;
        this.telepon = telepon; this.username = username; this.password = password; this.status = status;
    }
    public String getId() {return id;}
    public void setId(String id) {this.id = id;}
    public String getNama() {return nama;}
    public void setNama(String nama) {this.nama = nama;}
    public String getJabatan() {return jabatan;}
    public void setJabatan(String jabatan) {this.jabatan = jabatan;}
    public String getAlamat() {return alamat;}
    public void setAlamat(String alamat) {this.alamat = alamat;}
    public String getTelepon() {return telepon;}
    public void setTelepon(String telepon) {this.telepon = telepon;}
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
}
