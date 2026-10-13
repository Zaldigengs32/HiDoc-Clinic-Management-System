package Admin.Controller;

import Admin.Karyawan;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import java.sql.ResultSet;
import Connection.DBConnect;
import java.sql.PreparedStatement;
import javafx.scene.layout.VBox;
import javafx.scene.control.PasswordField;
import javafx.geometry.Pos;
import java.sql.Statement;
import javafx.application.Platform;

public class DashboardDataKryController {

    @FXML private TableView<Karyawan> tabelKaryawan;
    @FXML private TableColumn<Karyawan, String> colIDKaryawan, colNamaKaryawan, colJabatanKaryawan, colAlamatKaryawan, colTeleponKaryawan, colUsername, colPassword, colStatus;
    @FXML private TableColumn<Karyawan, Void> colAksi;
    @FXML private ComboBox<String> cbStatusFilter, cbJabatanFilter;
    @FXML private TextField searchField;
    @FXML private Button btnTambah, btnSimpan, btnBatal;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfID, tfNama, tfAlamat, tfTelepon, tfUsername, tfStatus;
    @FXML private ComboBox<String> tfJabatan;
    @FXML private PasswordField tfPassword;
    @FXML private TextField tfPasswordVisible;
    @FXML private Button btnTogglePassword;

    private final ObservableList<Karyawan> allData = FXCollections.observableArrayList();
    private FilteredList<Karyawan> filteredData;
    private boolean isEditMode = false;
    private String currentEditId = null;
    private Karyawan originalKaryawan = null;

    @FXML
    public void initialize() {
        colIDKaryawan.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNamaKaryawan.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colJabatanKaryawan.setCellValueFactory(new PropertyValueFactory<>("jabatan"));
        colAlamatKaryawan.setCellValueFactory(new PropertyValueFactory<>("alamat"));
        colTeleponKaryawan.setCellValueFactory(new PropertyValueFactory<>("telepon"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        filteredData = new FilteredList<>(allData, p -> true);
        tabelKaryawan.setItems(filteredData);
        getDataKaryawan();
        setupAksiKolom();
        setupSearchAndFilters();
        setupInputValidation();
        cbStatusFilter.setOnAction(e -> applyFilters());
        cbStatusFilter.setValue("Aktif");
        applyFilters();
        cbJabatanFilter.setValue("Semua Jabatan");
        btnTambah.setOnAction(e -> showTambahForm());
        btnBatal.setOnAction(e -> batalKaryawan());
        btnSimpan.setOnAction(e -> {
            if (isEditMode) {
                updateKaryawan();
            } else {
                simpanKaryawanBaru();
            }
        });
    }

    @FXML
    private void tutupForm() {
        hideForm();
    }

    private void setupInputValidation() {
        if (tfNama != null) {
            tfNama.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.matches("[a-zA-Z\\s]*")) {
                    tfNama.setText(oldValue);
                }
            });
        }

        if (tfTelepon != null) {
            tfTelepon.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.matches("\\d*")) {
                    tfTelepon.setText(oldValue);
                }
            });
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        if (tfPassword.isVisible()) {
            tfPasswordVisible.setText(tfPassword.getText());
            tfPassword.setVisible(false);
            tfPassword.setManaged(false);
            tfPasswordVisible.setVisible(true);
            tfPasswordVisible.setManaged(true);
            btnTogglePassword.setText("🙈");
        } else {
            tfPassword.setText(tfPasswordVisible.getText());
            tfPasswordVisible.setVisible(false);
            tfPasswordVisible.setManaged(false);
            tfPassword.setVisible(true);
            tfPassword.setManaged(true);
            btnTogglePassword.setText("👁️");
        }
    }

    @FXML
    private void syncPassword() {
        if (tfPassword.isVisible()) {
            tfPasswordVisible.setText(tfPassword.getText());
        } else {
            tfPassword.setText(tfPasswordVisible.getText());
        }
    }

    @FXML
    private void simpanKaryawan() {
        if (isEditMode) {
            updateKaryawan();
        } else {
            simpanKaryawanBaru();
        }
    }

    @FXML
    private void batalKaryawan() {
        if (isEditMode) {
            batalEdit(); // Panggil method batal edit
            return;
        }
        // Cek apakah ada data di form
        boolean adaData = (tfNama.getText() != null && !tfNama.getText().trim().isEmpty()) ||
                (tfJabatan.getValue() != null) ||
                (tfAlamat.getText() != null && !tfAlamat.getText().trim().isEmpty()) ||
                (tfTelepon.getText() != null && !tfTelepon.getText().trim().isEmpty()) ||
                (tfUsername.getText() != null && !tfUsername.getText().trim().isEmpty()) ||
                (tfPassword.getText() != null && !tfPassword.getText().trim().isEmpty());

        if (adaData) {
            showConfirmationDialog("Konfirmasi Batal", "Batalkan Input",
                    "Yakin ingin membatalkan input dan mengosongkan form?",
                    () -> {
                        clearFormFields();
                        if (!isEditMode) {
                            tfID.setText(generateIdKaryawan());
                            tfStatus.setText("Aktif");
                        }
                        showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Input Dibatalkan",
                                "Input berhasil dibatalkan dan form dikosongkan.");
                    });
        } else {
            showNonBlockingAlert(Alert.AlertType.WARNING, "Tidak Ada Data", "Form Kosong",
                    "Tidak ada data yang perlu dibatalkan.");
        }
    }

    private void setupSearchAndFilters() {
        if (searchField != null) {
            searchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    applyFilters();
                }
            });
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
        cbStatusFilter.setOnAction(e -> applyFilters());
        cbJabatanFilter.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(karyawan -> {
            String searchText = searchField.getText();
            boolean matchesSearch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase().trim();
                matchesSearch = karyawan.getId().toLowerCase().contains(lowerCaseFilter) ||
                        karyawan.getNama().toLowerCase().contains(lowerCaseFilter) ||
                        karyawan.getJabatan().toLowerCase().contains(lowerCaseFilter) ||
                        karyawan.getAlamat().toLowerCase().contains(lowerCaseFilter) ||
                        karyawan.getTelepon().toLowerCase().contains(lowerCaseFilter) ||
                        karyawan.getUsername().toLowerCase().contains(lowerCaseFilter);
            }
            String statusFilter = cbStatusFilter.getValue();
            boolean matchesStatus = true;
            if (statusFilter != null && !statusFilter.equals("Semua Status")) {
                matchesStatus = karyawan.getStatus().equalsIgnoreCase(statusFilter);
            }
            String jabatanFilter = cbJabatanFilter.getValue();
            boolean matchesJabatan = true;

            if (jabatanFilter != null && !jabatanFilter.equals("Semua Jabatan")) {
                matchesJabatan = karyawan.getJabatan().equalsIgnoreCase(jabatanFilter);
            }
            return matchesSearch && matchesStatus && matchesJabatan;
        });
        setupAksiKolom();
        tabelKaryawan.refresh();
    }

    private void showTambahForm() {
        isEditMode = false;
        currentEditId = null;
        formPopup.setVisible(true);
        formPopup.setManaged(true);
        formPopup.toFront();
        clearFormFields();
        String newId = generateIdKaryawan();
        if (tfID != null) {
            tfID.setText(newId);
            tfID.setEditable(false);
        }
        if (tfStatus != null) {
            tfStatus.setText("Aktif");
        }
        formTitle.setText("📋 Tambah Karyawan Baru");
    }

    private void showEditForm(Karyawan karyawan) {
        isEditMode = true;
        currentEditId = karyawan.getId();
        formPopup.setVisible(true);
        formPopup.setManaged(true);
        formPopup.toFront();
        if (tfID != null) {
            tfID.setText(karyawan.getId());
            tfID.setEditable(false);
        }

        originalKaryawan = new Karyawan(
                karyawan.getId(),
                karyawan.getNama(),
                karyawan.getJabatan(),
                karyawan.getAlamat(),
                karyawan.getTelepon(),
                karyawan.getUsername(),
                karyawan.getPassword(),
                karyawan.getStatus()
        );

        tfNama.setText(karyawan.getNama());
        tfJabatan.setValue(karyawan.getJabatan());
        tfAlamat.setText(karyawan.getAlamat());
        tfTelepon.setText(karyawan.getTelepon());
        tfUsername.setText(karyawan.getUsername());
        tfPassword.setText(karyawan.getPassword());
        if (tfStatus != null) {
            tfStatus.setText(karyawan.getStatus());
        }
        formTitle.setText("📋 Edit Data Karyawan");
    }

    @FXML
    private void batalEdit() {
        // Cek apakah ada perubahan
        boolean adaPerubahan = !tfNama.getText().equals(originalKaryawan.getNama()) ||
                !tfJabatan.getValue().equals(originalKaryawan.getJabatan()) ||
                !tfAlamat.getText().equals(originalKaryawan.getAlamat()) ||
                !tfTelepon.getText().equals(originalKaryawan.getTelepon()) ||
                !tfUsername.getText().equals(originalKaryawan.getUsername()) ||
                !getCurrentPassword().equals(originalKaryawan.getPassword());

        if (adaPerubahan) {
            showConfirmationDialog("Konfirmasi Batal Edit", "Batalkan Perubahan",
                    "Yakin ingin membatalkan perubahan dan kembali ke data asli?",
                    () -> {
                        // Kembalikan ke data original
                        tfNama.setText(originalKaryawan.getNama());
                        tfJabatan.setValue(originalKaryawan.getJabatan());
                        tfAlamat.setText(originalKaryawan.getAlamat());
                        tfTelepon.setText(originalKaryawan.getTelepon());
                        tfUsername.setText(originalKaryawan.getUsername());
                        tfPassword.setText(originalKaryawan.getPassword());
                        tfPasswordVisible.setText(originalKaryawan.getPassword());
                        tfStatus.setText(originalKaryawan.getStatus());

                        showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Perubahan Dibatalkan",
                                "Data dikembalikan ke kondisi asli.");
                    });
        } else {
            showNonBlockingAlert(Alert.AlertType.WARNING, "Tidak Ada Perubahan", "Tidak Ada Data yang Diubah",
                    "Tidak ada perubahan yang perlu dibatalkan.");
        }
    }

    private String getCurrentPassword() {
        return tfPassword.isVisible() ? tfPassword.getText() : tfPasswordVisible.getText();
    }

    private void hideForm() {
        formPopup.setVisible(false);
        formPopup.setManaged(false);
        isEditMode = false;
        currentEditId = null;
        originalKaryawan = null;
        clearFormFields();
    }

    private void clearFormFields() {
        if (tfID != null) tfID.clear();
        if (tfNama != null) tfNama.clear();
        if (tfJabatan != null) tfJabatan.getSelectionModel().clearSelection();
        if (tfAlamat != null) tfAlamat.clear();
        if (tfTelepon != null) tfTelepon.clear();
        if (tfUsername != null) tfUsername.clear();
        if (tfPassword != null) tfPassword.clear();
        if (tfPasswordVisible != null) tfPasswordVisible.clear();
        if (tfStatus != null) tfStatus.clear();
    }

    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();
        if (tfNama.getText() == null || tfNama.getText().trim().isEmpty()) {
            errorMessage.append("• Nama karyawan harus diisi\n");
        } else {
            String nama = tfNama.getText().trim();
            if (!nama.matches("[a-zA-Z\\s]+")) {
                errorMessage.append("• Nama hanya boleh berisi huruf dan spasi\n");
            }
        }
        if (tfJabatan.getValue() == null || tfJabatan.getValue().trim().isEmpty()) {
            errorMessage.append("• Jabatan harus dipilih\n");
        }
        if (tfAlamat.getText() == null || tfAlamat.getText().trim().isEmpty()) {
            errorMessage.append("• Alamat harus diisi\n");
        }
        if (tfTelepon.getText() == null || tfTelepon.getText().trim().isEmpty()) {
            errorMessage.append("• Nomor telepon harus diisi\n");
        } else {
            String telepon = tfTelepon.getText().trim();
            if (!telepon.matches("\\d+")) {
                errorMessage.append("• Nomor telepon hanya boleh berisi angka\n");
            }
        }
        if (tfUsername.getText() == null || tfUsername.getText().trim().isEmpty()) {
            errorMessage.append("• Username harus diisi\n");
        } else {
            String username = tfUsername.getText().trim();
            if (isUsernameExists(username, currentEditId)) {
                errorMessage.append("• Username sudah digunakan, pilih username lain\n");
            }
        }
        String password = tfPassword.isVisible() ? tfPassword.getText() : tfPasswordVisible.getText();
        if (password == null || password.trim().isEmpty()) {
            errorMessage.append("• Password harus diisi\n");
        } else if (password.trim().length() < 6) {
            errorMessage.append("• Password minimal 6 karakter\n");
        }
        if (errorMessage.length() > 0) {
            showNonBlockingAlert(Alert.AlertType.WARNING, "Validasi Form",
                    "Data tidak lengkap atau tidak valid:", errorMessage.toString());
            return false;
        }
        return true;
    }

    private boolean isUsernameExists(String username, String excludeId) {
        try {
            DBConnect db = new DBConnect();
            String query = "SELECT COUNT(*) as count FROM Karyawan WHERE Username = ?";
            if (excludeId != null) {
                query += " AND IDKaryawan != ?";
            }
            PreparedStatement pst = db.conn.prepareStatement(query);
            pst.setString(1, username);
            if (excludeId != null) {
                pst.setString(2, excludeId);
            }
            ResultSet rs = pst.executeQuery();
            rs.next();
            int count = rs.getInt("count");
            rs.close();
            pst.close();
            db.conn.close();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void getDataKaryawan() {
        allData.clear();
        try {
            DBConnect db = new DBConnect();
            String query = "SELECT * FROM Karyawan WHERE JabatanKaryawan = 'Manajer'";
            ResultSet rs = db.stat.executeQuery(query);
            while (rs.next()) {
                Karyawan k = new Karyawan(
                        rs.getString("IDKaryawan"),
                        rs.getString("NamaKaryawan"),
                        rs.getString("JabatanKaryawan"),
                        rs.getString("AlamatKaryawan"),
                        rs.getString("TeleponKaryawan"),
                        rs.getString("Username"),
                        rs.getString("Password"),
                        rs.getString("Status")
                );
                allData.add(k);
            }
            rs.close();
            db.stat.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showNonBlockingAlert(Alert.AlertType.ERROR, "Database Error",
                    "Gagal mengambil data dari database:", e.getMessage());
        }
    }

    private String generateIdKaryawan() {
        String newId = "KRY001";
        try {
            DBConnect db = new DBConnect();
            Statement stmt = db.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT TOP 1 IDKaryawan FROM Karyawan ORDER BY IDKaryawan DESC");
            if (rs.next()) {
                String lastId = rs.getString("IDKaryawan");
                int number = Integer.parseInt(lastId.substring(3)) + 1;
                newId = String.format("KRY%03d", number);
            }

            rs.close();
            stmt.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newId;
    }

    private void simpanKaryawanBaru() {
        if (!validateForm()) {
            return;
        }
        String idBaru = tfID.getText().trim();
        String nama = tfNama.getText().trim();
        String jabatan = tfJabatan.getValue();
        String alamat = tfAlamat.getText().trim();
        String telepon = tfTelepon.getText().trim();
        String username = tfUsername.getText().trim();
        String password = tfPassword.isVisible() ? tfPassword.getText().trim() : tfPasswordVisible.getText().trim();
        String status = "Aktif";
        try {
            DBConnect db = new DBConnect();
            String sql = "INSERT INTO Karyawan (IDKaryawan, NamaKaryawan, JabatanKaryawan, AlamatKaryawan, TeleponKaryawan, Username, Password, Status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pst = db.conn.prepareStatement(sql);
            pst.setString(1, idBaru);
            pst.setString(2, nama);
            pst.setString(3, jabatan);
            pst.setString(4, alamat);
            pst.setString(5, telepon);
            pst.setString(6, username);
            pst.setString(7, password);
            pst.setString(8, status);

            int rows = pst.executeUpdate();
            if (rows > 0) {
                showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Tersimpan",
                        "Data karyawan '" + nama + "' berhasil disimpan ke database dengan ID: " + idBaru,
                        () -> {
                            getDataKaryawan();
                            applyFilters();
                            resetFormKaryawanBaru(); // Panggil reset setelah simpan
                        });
            }
            pst.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menyimpan Data",
                    "Terjadi kesalahan saat menyimpan ke database:\n" + e.getMessage());
        }
    }

    private void resetFormKaryawanBaru() {
        tfNama.clear();
        tfJabatan.setValue(null);
        tfAlamat.clear();
        tfTelepon.clear();
        tfUsername.clear();
        tfPassword.clear();
        tfPasswordVisible.clear();
        tfID.setText(generateIdKaryawan());
        tfNama.requestFocus();
    }

    private void updateKaryawan() {
        if (!validateForm()) {
            return;
        }
        String nama = tfNama.getText().trim();
        String jabatan = tfJabatan.getValue();
        String alamat = tfAlamat.getText().trim();
        String telepon = tfTelepon.getText().trim();
        String username = tfUsername.getText().trim();
        String password = tfPassword.isVisible() ? tfPassword.getText().trim() : tfPasswordVisible.getText().trim();
        String status = tfStatus.getText().trim();
        try {
            DBConnect db = new DBConnect();
            String sql = "UPDATE Karyawan SET NamaKaryawan=?, JabatanKaryawan=?, AlamatKaryawan=?, TeleponKaryawan=?, Username=?, Password=?, Status=? WHERE IDKaryawan=?";
            PreparedStatement pst = db.conn.prepareStatement(sql);
            pst.setString(1, nama);
            pst.setString(2, jabatan);
            pst.setString(3, alamat);
            pst.setString(4, telepon);
            pst.setString(5, username);
            pst.setString(6, password);
            pst.setString(7, status);
            pst.setString(8, currentEditId);
            int rows = pst.executeUpdate();
            if (rows > 0) {
                showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Diperbarui",
                        "Data karyawan '" + nama + "' berhasil diperbarui.",
                        () -> {
                            hideForm();
                            getDataKaryawan();
                            applyFilters();
                        });
            }
            pst.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Memperbarui Data",
                    "Terjadi kesalahan saat memperbarui data:\n" + e.getMessage());
        }
    }

    private void handleDelete(Karyawan karyawan) {
        if ("Aktif".equalsIgnoreCase(karyawan.getStatus())) {
            showConfirmationDialog("Konfirmasi Nonaktifkan", "Nonaktifkan Karyawan",
                    "Status karyawan '" + karyawan.getNama() + "' akan diubah menjadi 'Tidak Aktif'. Lanjutkan?",
                    () -> {
                        try {
                            DBConnect db = new DBConnect();
                            String sql = "UPDATE Karyawan SET Status = 'Tidak Aktif' WHERE IDKaryawan = ?";
                            PreparedStatement pst = db.conn.prepareStatement(sql);
                            pst.setString(1, karyawan.getId());
                            pst.executeUpdate();
                            pst.close();
                            db.conn.close();

                            getDataKaryawan();
                            applyFilters();

                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Karyawan Dinonaktifkan",
                                    "Karyawan '" + karyawan.getNama() + "' berhasil dinonaktifkan.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menonaktifkan",
                                    "Gagal mengubah status karyawan.");
                        }
                    });
        } else {
            showConfirmationDialog("Konfirmasi Hapus Permanen", "PERINGATAN: Data akan dihapus permanen!",
                    "Data karyawan '" + karyawan.getNama() + "' akan dihapus secara permanen dari database. Tindakan ini tidak dapat dibatalkan. Lanjutkan?",
                    () -> {
                        try {
                            DBConnect db = new DBConnect();
                            String sql = "DELETE FROM Karyawan WHERE IDKaryawan = ?";
                            PreparedStatement pst = db.conn.prepareStatement(sql);
                            pst.setString(1, karyawan.getId());
                            pst.executeUpdate();
                            pst.close();
                            db.conn.close();
                            allData.remove(karyawan);
                            applyFilters();
                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Dihapus",
                                    "Data karyawan '" + karyawan.getNama() + "' berhasil dihapus permanen.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menghapus",
                                    "Gagal menghapus data dari database.");
                        }
                    });
        }
    }

    private void setupAksiKolom() {
        colAksi.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete);
            {
                Label editIcon = new Label("✏");
                editIcon.setStyle("-fx-font-size: 10px; -fx-font-family: 'Segoe UI Emoji';");

                btnEdit.setGraphic(editIcon);
                btnEdit.setText("");
                btnEdit.setPrefSize(32, 28);
                btnEdit.setStyle("-fx-background-color: orange; -fx-background-radius: 6; -fx-cursor: hand;");
                btnEdit.setTooltip(new Tooltip("Edit data karyawan"));

                btnEdit.setOnAction(e -> {
                    Karyawan k = getTableView().getItems().get(getIndex());
                    showEditForm(k);
                });

                Label deleteIcon = new Label("🗑");
                deleteIcon.setStyle("-fx-font-size: 10px; -fx-font-family: 'Segoe UI Emoji';");

                btnDelete.setGraphic(deleteIcon);
                btnDelete.setText("");
                btnDelete.setPrefSize(32, 28);
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setTooltip(new Tooltip("Hapus/nonaktifkan karyawan"));

                btnDelete.setOnAction(e -> {
                    Karyawan k = getTableView().getItems().get(getIndex());
                    handleDelete(k);
                });

                pane.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void showNonBlockingAlert(Alert.AlertType type, String title, String header, String content) {
        showNonBlockingAlert(type, title, header, content, null);
    }

    private void showNonBlockingAlert(Alert.AlertType type, String title, String header, String content, Runnable onClose) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);

            alert.initOwner(formPopup.getScene().getWindow());
            if (onClose != null) {
                alert.setOnHidden(e -> onClose.run());
            }
            alert.show();
        });
    }

    private void showConfirmationDialog(String title, String header, String content, Runnable onConfirm) {
        Platform.runLater(() -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(title);
            confirm.setHeaderText(header);
            confirm.setContentText(content);
            confirm.initOwner(tabelKaryawan.getScene().getWindow());
            confirm.setOnHidden(e -> {
                if (confirm.getResult() == ButtonType.OK && onConfirm != null) {
                    onConfirm.run();
                }
            });

            confirm.show();
        });
    }
}