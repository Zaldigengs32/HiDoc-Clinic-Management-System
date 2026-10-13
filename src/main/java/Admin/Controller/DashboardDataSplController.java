package Admin.Controller;

import Admin.Supplier;
import Interface.SearchableController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import Connection.DBConnect;
import java.sql.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.application.Platform;

public class DashboardDataSplController implements SearchableController {

    @FXML private TableView<Supplier> tabelSupplier;
    @FXML private TableColumn<Supplier, String> colIDSupplier;
    @FXML private TableColumn<Supplier, String> colNamaSupplier;
    @FXML private TableColumn<Supplier, String> colAlamatSupplier;
    @FXML private TableColumn<Supplier, String> colTeleponSupplier;
    @FXML private TableColumn<Supplier, String> colStatusSupplier;
    @FXML private TableColumn<Supplier, Void> colAksi;
    @FXML private ComboBox<String> cbFilterStatusSupplier;
    @FXML private TextField searchField;
    @FXML private Button btnTambahSupplier;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfID;
    @FXML private TextField tfNama;
    @FXML private TextField tfAlamat;
    @FXML private TextField tfTelepon;
    @FXML private TextField tfStatusDisplay;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;

    private final ObservableList<Supplier> allData = FXCollections.observableArrayList();
    private FilteredList<Supplier> filteredData;
    private boolean isEditMode = false;
    private String currentEditId = null;
    private Supplier originalSupplier;

    @FXML
    public void initialize() {
        colIDSupplier.setCellValueFactory(new PropertyValueFactory<>("idSupplier"));
        colNamaSupplier.setCellValueFactory(new PropertyValueFactory<>("namaSupplier"));
        colAlamatSupplier.setCellValueFactory(new PropertyValueFactory<>("alamatSupplier"));
        colTeleponSupplier.setCellValueFactory(new PropertyValueFactory<>("teleponSupplier"));
        colStatusSupplier.setCellValueFactory(new PropertyValueFactory<>("status"));
        filteredData = new FilteredList<>(allData, p -> true);
        tabelSupplier.setItems(filteredData);
        cbFilterStatusSupplier.setItems(FXCollections.observableArrayList("Semua Status", "Aktif", "Tidak Aktif"));
        cbFilterStatusSupplier.setValue("Aktif");

        getDataSupplier();
        setupAksiKolom();
        setupSearchAndFilters();
        setupButtonActions();
        setupInputValidation();
        applyFilters();
        cbFilterStatusSupplier.setOnAction(e -> applyFilters());
        btnTambahSupplier.setOnAction(e -> showTambahForm());
        btnBatal.setOnAction(e -> batalSupplier()); // Ubah dari hideForm() ke batalSupplier()
        btnSimpan.setOnAction(e -> {
            if (isEditMode) {
                updateSupplier();
            } else {
                simpanSupplierBaru();
            }
        });
    }

    @FXML
    private void batalSupplier() {
        if (isEditMode) {
            // Mode edit - kembalikan ke data original
            showConfirmationDialog("Konfirmasi Batal Edit", "Batalkan Perubahan",
                    "Yakin ingin membatalkan perubahan dan kembali ke data asli?",
                    () -> {
                        // Kembalikan ke data original
                        tfNama.setText(originalSupplier.getNamaSupplier());
                        tfAlamat.setText(originalSupplier.getAlamatSupplier());
                        tfTelepon.setText(originalSupplier.getTeleponSupplier());
                        tfStatusDisplay.setText(originalSupplier.getStatus());

                        showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Perubahan Dibatalkan",
                                "Perubahan berhasil dibatalkan dan data dikembalikan ke kondisi asli.");
                    });
        } else {
            // Mode tambah - kosongkan form (kode yang sudah ada)
            boolean adaData = (tfNama.getText() != null && !tfNama.getText().trim().isEmpty()) ||
                    (tfAlamat.getText() != null && !tfAlamat.getText().trim().isEmpty()) ||
                    (tfTelepon.getText() != null && !tfTelepon.getText().trim().isEmpty());

            if (adaData) {
                showConfirmationDialog("Konfirmasi Batal", "Batalkan Input",
                        "Yakin ingin membatalkan input dan mengosongkan form?",
                        () -> {
                            clearFormFields();
                            tfID.setText(generateIdSupplier());
                            tfStatusDisplay.setText("Aktif");
                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Input Dibatalkan",
                                    "Input berhasil dibatalkan dan form dikosongkan.");
                        });
            } else {
                showNonBlockingAlert(Alert.AlertType.WARNING, "Tidak Ada Data", "Form Kosong",
                        "Tidak ada data yang perlu dibatalkan.");
            }
        }
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

    private void setupButtonActions() {
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
        cbFilterStatusSupplier.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(supplier -> {
            String searchText = searchField != null ? searchField.getText() : "";
            boolean matchesSearch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase().trim();
                matchesSearch = supplier.getIdSupplier().toLowerCase().contains(lowerCaseFilter) ||
                        supplier.getNamaSupplier().toLowerCase().contains(lowerCaseFilter) ||
                        supplier.getAlamatSupplier().toLowerCase().contains(lowerCaseFilter) ||
                        supplier.getTeleponSupplier().toLowerCase().contains(lowerCaseFilter) ||
                        supplier.getStatus().toLowerCase().contains(lowerCaseFilter);
            }

            String statusFilter = cbFilterStatusSupplier.getValue();
            boolean matchesStatus = true;
            if (statusFilter != null && !statusFilter.equals("Semua Status")) {
                matchesStatus = supplier.getStatus().equalsIgnoreCase(statusFilter);
            }
            return matchesSearch && matchesStatus;
        });
        setupAksiKolom();
        tabelSupplier.refresh();
    }

    @Override
    public void filterTable(String keyword) {
        if (searchField != null) {
            searchField.setText(keyword);
        }
        applyFilters();
    }

    private void showTambahForm() {
        isEditMode = false;
        currentEditId = null;
        formPopup.setVisible(true);
        formPopup.setManaged(true);
        formPopup.toFront();
        clearFormFields();
        String nextId = generateIdSupplier();
        if (tfID != null) {
            tfID.setText(nextId);
            tfID.setEditable(false);
        }
        if (tfStatusDisplay != null) {
            tfStatusDisplay.setText("Aktif");
        }
        formTitle.setText("📋 Tambah Supplier Baru");
    }

    private void showEditForm(Supplier supplier) {
        originalSupplier = new Supplier(
                supplier.getIdSupplier(),
                supplier.getNamaSupplier(),
                supplier.getAlamatSupplier(),
                supplier.getTeleponSupplier(),
                supplier.getStatus()
        );

        isEditMode = true;
        currentEditId = supplier.getIdSupplier();
        formPopup.setVisible(true);
        formPopup.setManaged(true);
        formPopup.toFront();
        if (tfID != null) {
            tfID.setText(supplier.getIdSupplier());
            tfID.setEditable(false);
        }
        tfNama.setText(supplier.getNamaSupplier());
        tfAlamat.setText(supplier.getAlamatSupplier());
        tfTelepon.setText(supplier.getTeleponSupplier());
        if (tfStatusDisplay != null) {
            tfStatusDisplay.setText(supplier.getStatus());
        }
        formTitle.setText("📋 Edit Data Supplier");
    }

    private void hideForm() {
        formPopup.setVisible(false);
        formPopup.setManaged(false);
        isEditMode = false;
        currentEditId = null;
        originalSupplier = null; // Reset data original
        clearFormFields();
    }

    private void clearFormFields() {
        if (tfID != null) tfID.clear();
        if (tfNama != null) tfNama.clear();
        if (tfAlamat != null) tfAlamat.clear();
        if (tfTelepon != null) tfTelepon.clear();
        if (tfStatusDisplay != null) tfStatusDisplay.clear();
    }

    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();
        if (tfNama.getText() == null || tfNama.getText().trim().isEmpty()) {
            errorMessage.append("• Nama supplier harus diisi\n");
        } else {
            String nama = tfNama.getText().trim();
            if (!nama.matches("[a-zA-Z\\s]+")) {
                errorMessage.append("• Nama supplier hanya boleh berisi huruf dan spasi\n");
            }
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
        if (errorMessage.length() > 0) {
            showNonBlockingAlert(Alert.AlertType.WARNING, "Validasi Form",
                    "Data tidak lengkap atau tidak valid:", errorMessage.toString());
            return false;
        }
        return true;
    }

    private void getDataSupplier() {
        allData.clear();
        try {
            DBConnect db = new DBConnect();
            String query = "SELECT * FROM Supplier ORDER BY idSupplier";
            ResultSet rs = db.stat.executeQuery(query);
            while (rs.next()) {
                Supplier s = new Supplier(
                        rs.getString("idSupplier"),
                        rs.getString("namaSupplier"),
                        rs.getString("alamatSupplier"),
                        rs.getString("teleponSupplier"),
                        rs.getString("status")
                );
                allData.add(s);
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

    private String generateIdSupplier() {
        String newId = "SPL001";
        try {
            DBConnect db = new DBConnect();
            Statement stmt = db.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT TOP 1 idSupplier FROM Supplier ORDER BY idSupplier DESC");
            if (rs.next()) {
                String lastId = rs.getString("idSupplier");
                int number = Integer.parseInt(lastId.substring(3)) + 1;
                newId = String.format("SPL%03d", number);
            }
            rs.close();
            stmt.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newId;
    }

    private void simpanSupplierBaru() {
        if (!validateForm()) {
            return;
        }
        String idBaru = tfID.getText().trim();
        String nama = tfNama.getText().trim();
        String alamat = tfAlamat.getText().trim();
        String telepon = tfTelepon.getText().trim();
        String status = "Aktif"; // Default status untuk supplier baru

        try {
            DBConnect db = new DBConnect();
            String sql = "INSERT INTO Supplier (idSupplier, namaSupplier, alamatSupplier, teleponSupplier, status) " +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = db.conn.prepareStatement(sql);
            pst.setString(1, idBaru);
            pst.setString(2, nama);
            pst.setString(3, alamat);
            pst.setString(4, telepon);
            pst.setString(5, status);
            int rows = pst.executeUpdate();
            if (rows > 0) {
                showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Tersimpan",
                        "Data supplier '" + nama + "' berhasil disimpan ke database dengan ID: " + idBaru,
                        () -> {
                            getDataSupplier();
                            applyFilters();
                            resetFormSupplierBaru();
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

    private void resetFormSupplierBaru() {
        tfNama.clear();
        tfAlamat.clear();
        tfTelepon.clear();;
        tfID.setText(generateIdSupplier());
        tfNama.requestFocus();
    }

    private void updateSupplier() {
        if (!validateForm()) {
            return;
        }
        String nama = tfNama.getText().trim();
        String alamat = tfAlamat.getText().trim();
        String telepon = tfTelepon.getText().trim();
        String status = tfStatusDisplay.getText().trim();
        try {
            DBConnect db = new DBConnect();
            String sql = "UPDATE Supplier SET namaSupplier=?, alamatSupplier=?, teleponSupplier=?, status=? WHERE idSupplier=?";
            PreparedStatement pst = db.conn.prepareStatement(sql);
            pst.setString(1, nama);
            pst.setString(2, alamat);
            pst.setString(3, telepon);
            pst.setString(4, status);
            pst.setString(5, currentEditId);
            int rows = pst.executeUpdate();
            if (rows > 0) {
                showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Diperbarui",
                        "Data supplier '" + nama + "' berhasil diperbarui.",
                        () -> {
                            hideForm();
                            getDataSupplier();
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

    private void handleDelete(Supplier supplier) {
        if ("Aktif".equalsIgnoreCase(supplier.getStatus())) {
            showConfirmationDialog("Konfirmasi Nonaktifkan", "Nonaktifkan Supplier",
                    "Status supplier '" + supplier.getNamaSupplier() + "' akan diubah menjadi 'Tidak Aktif'. Lanjutkan?",
                    () -> {
                        try {
                            DBConnect db = new DBConnect();
                            String sql = "UPDATE Supplier SET status = 'Tidak Aktif' WHERE idSupplier = ?";
                            PreparedStatement pst = db.conn.prepareStatement(sql);
                            pst.setString(1, supplier.getIdSupplier());
                            pst.executeUpdate();
                            pst.close();
                            db.conn.close();
                            getDataSupplier();
                            applyFilters();
                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Supplier Dinonaktifkan",
                                    "Supplier '" + supplier.getNamaSupplier() + "' berhasil dinonaktifkan.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menonaktifkan",
                                    "Gagal mengubah status supplier.");
                        }
                    });
        } else {
            showConfirmationDialog("Konfirmasi Hapus Permanen", "PERINGATAN: Data akan dihapus permanen!",
                    "Data supplier '" + supplier.getNamaSupplier() + "' akan dihapus secara permanen dari database. Tindakan ini tidak dapat dibatalkan. Lanjutkan?",
                    () -> {
                        try {
                            DBConnect db = new DBConnect();
                            String sql = "DELETE FROM Supplier WHERE idSupplier = ?";
                            PreparedStatement pst = db.conn.prepareStatement(sql);
                            pst.setString(1, supplier.getIdSupplier());
                            pst.executeUpdate();
                            pst.close();
                            db.conn.close();
                            allData.remove(supplier);
                            applyFilters();
                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Dihapus",
                                    "Data supplier '" + supplier.getNamaSupplier() + "' berhasil dihapus permanen.");
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
                btnEdit.setTooltip(new Tooltip("Edit data supplier"));
                btnEdit.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    showEditForm(s);
                });
                Label deleteIcon = new Label("🗑");
                deleteIcon.setStyle("-fx-font-size: 10px; -fx-font-family: 'Segoe UI Emoji';");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setText("");
                btnDelete.setPrefSize(32, 28);
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setTooltip(new Tooltip("Hapus/nonaktifkan supplier"));
                btnDelete.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    handleDelete(s);
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
            confirm.initOwner(tabelSupplier.getScene().getWindow());
            confirm.setOnHidden(e -> {
                if (confirm.getResult() == ButtonType.OK && onConfirm != null) {
                    onConfirm.run();
                }
            });
            confirm.show();
        });
    }
}