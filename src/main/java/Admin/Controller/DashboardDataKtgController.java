package Admin.Controller;

import Admin.KategoriObat;
import Interface.SearchableController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import Connection.DBConnect;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DashboardDataKtgController implements SearchableController {

    @FXML private TableView<KategoriObat> tabelKategori;
    @FXML private TableColumn<KategoriObat, String> colIDKategori;
    @FXML private TableColumn<KategoriObat, String> colNamaKategori;
    @FXML private TableColumn<KategoriObat, String> colDeskripsi;
    @FXML private TableColumn<KategoriObat, String> colStatus;
    @FXML private TableColumn<KategoriObat, Void> colAksi;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private TextField searchField;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfID;
    @FXML private TextField tfNamaKategori;
    @FXML private TextField tfDeskripsi;
    @FXML private TextField tfStatusDisplay;
    @FXML private Button btnTambahKategori;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;

    private final ObservableList<KategoriObat> dataKategori = FXCollections.observableArrayList();
    private FilteredList<KategoriObat> filteredData;
    private boolean isEditMode = false;
    private String currentEditId = null;

    @FXML
    public void initialize() {
        colIDKategori.setCellValueFactory(new PropertyValueFactory<>("idKategori"));
        colNamaKategori.setCellValueFactory(new PropertyValueFactory<>("namaKategori"));
        colDeskripsi.setCellValueFactory(new PropertyValueFactory<>("deskripsi"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        filteredData = new FilteredList<>(dataKategori, p -> true);
        tabelKategori.setItems(filteredData);
        cbFilterStatus.setItems(FXCollections.observableArrayList("Semua Status", "Aktif", "Tidak Aktif"));
        cbFilterStatus.setValue("Aktif");
        getDataKategori();
        setupSearch();
        applyFilters();
        setupAksiKolom();
        setupButtonHandlers();
        setupInputValidation();
        cbFilterStatus.setOnAction(e -> applyFilters());
    }

    private void setupButtonHandlers() {
        btnTambahKategori.setOnAction(e -> showFormTambah());
        btnSimpan.setOnAction(e -> handleSimpan());

        // Ubah ini untuk menggunakan fungsi batal yang berbeda
        btnBatal.setOnAction(e -> {
            if (isEditMode) {
                batalEdit(); // Untuk form edit
            } else {
                batalKategori(); // Untuk form tambah
            }
        });
    }

    @FXML
    private void batalKategori() {
        boolean adaData = (tfNamaKategori.getText() != null && !tfNamaKategori.getText().trim().isEmpty()) ||
                (tfDeskripsi.getText() != null && !tfDeskripsi.getText().trim().isEmpty());

        if (adaData) {
            showConfirmationDialog("Konfirmasi Batal", "Batalkan Input",
                    "Yakin ingin membatalkan input dan mengosongkan form?",
                    () -> {
                        clearFormFields();
                        if (!isEditMode) {
                            tfID.setText(generateIdKategori());
                            tfStatusDisplay.setText("Aktif");
                        }
                        showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Input Dibatalkan",
                                "Input berhasil dibatalkan dan form dikosongkan.");
                    });
        } else {
            showNonBlockingAlert(Alert.AlertType.WARNING, "Tidak Ada Data", "Form Kosong",
                    "Tidak ada data yang perlu dibatalkan.");
        }
    }

    @FXML
    private void batalEdit() {
        // Kembalikan data ke nilai asli sebelum diedit
        if (currentEditId != null) {
            showConfirmationDialog("Konfirmasi Batal Edit", "Batalkan Perubahan",
                    "Yakin ingin membatalkan perubahan dan mengembalikan data ke nilai asli?",
                    () -> {
                        // Cari data asli dari database atau dari tabel
                        restoreOriginalData();
                        showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Perubahan Dibatalkan",
                                "Perubahan berhasil dibatalkan dan data dikembalikan ke nilai asli.");
                    });
        }
    }

    private void restoreOriginalData() {
        if (currentEditId != null) {
            // Cari data asli dari database
            try {
                DBConnect db = new DBConnect();
                String query = "SELECT * FROM KategoriObat WHERE id_kategori = ?";
                PreparedStatement pst = db.conn.prepareStatement(query);
                pst.setString(1, currentEditId);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    tfNamaKategori.setText(rs.getString("nama_kategori"));
                    tfDeskripsi.setText(rs.getString("deskripsi"));
                    tfStatusDisplay.setText(rs.getString("status"));
                }

                rs.close();
                pst.close();
                db.conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void clearFormFields() {
        tfNamaKategori.clear();
        tfDeskripsi.clear();
        Platform.runLater(() -> {
            tfNamaKategori.requestFocus();
        });
    }

    private void setupInputValidation() {
        if (tfNamaKategori != null) {
            tfNamaKategori.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    String filteredText = newValue.replaceAll("[^a-zA-Z\\s]", "");
                    if (!filteredText.equals(newValue)) {
                        tfNamaKategori.setText(filteredText);
                    }
                }
            });
        }
    }

    private void showFormTambah() {
        isEditMode = false;
        currentEditId = null;
        formTitle.setText("📋 Tambah Data Kategori Obat");
        String nextId = generateIdKategori();
        tfID.setText(nextId);
        tfID.setEditable(false);
        tfNamaKategori.clear();
        tfDeskripsi.clear();
        tfStatusDisplay.setText("Aktif");
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    private void showFormEdit(KategoriObat kategori) {
        isEditMode = true;
        currentEditId = kategori.getIdKategori();
        formTitle.setText("📋 Edit Data Kategori Obat");
        tfID.setText(kategori.getIdKategori());
        tfID.setEditable(false);
        tfNamaKategori.setText(kategori.getNamaKategori());
        tfDeskripsi.setText(kategori.getDeskripsi());
        tfStatusDisplay.setText(kategori.getStatus());
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    private void hideForm() {
        formPopup.setVisible(false);
        formPopup.setManaged(false);
        tfID.setText("");
        tfNamaKategori.clear();
        tfDeskripsi.clear();
        tfStatusDisplay.setText("Aktif");
        isEditMode = false;
        currentEditId = null;
        formTitle.setText("📋 Form Data Kategori Obat");
    }

    @FXML
    public void tutupForm() {
        hideForm();
    }

    private boolean hasUnsavedChanges() {
        boolean hasNama = tfNamaKategori.getText() != null && !tfNamaKategori.getText().trim().isEmpty();
        boolean hasDeskripsi = tfDeskripsi.getText() != null && !tfDeskripsi.getText().trim().isEmpty();
        return hasNama || hasDeskripsi;
    }

    // Generate new category ID
    private String generateIdKategori() {
        String newId = "KTG001";
        try {
            DBConnect db = new DBConnect();
            Statement stmt = db.conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT TOP 1 id_kategori FROM KategoriObat ORDER BY id_kategori DESC");
            if (rs.next()) {
                String lastId = rs.getString("id_kategori");
                int number = Integer.parseInt(lastId.substring(3)) + 1;
                newId = String.format("KTG%03d", number);
            }
            rs.close();
            stmt.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newId;
    }

    // Validate form input
    private boolean validateForm() {
        StringBuilder errorMessage = new StringBuilder();

        // Validasi nama kategori
        if (tfNamaKategori.getText() == null || tfNamaKategori.getText().trim().isEmpty()) {
            errorMessage.append("• Nama kategori harus diisi\n");
        } else {
            String namaKategori = tfNamaKategori.getText().trim();
            if (!namaKategori.matches("^[a-zA-Z\\s]+$")) {
                errorMessage.append("• Nama kategori hanya boleh berisi huruf dan spasi\n");
            }
        }

        // Validasi deskripsi
        if (tfDeskripsi.getText() == null || tfDeskripsi.getText().trim().isEmpty()) {
            errorMessage.append("• Deskripsi harus diisi\n");
        }

        // Tampilkan error jika ada
        if (errorMessage.length() > 0) {
            showNonBlockingAlert(Alert.AlertType.WARNING, "Validasi Form",
                    "Data tidak lengkap atau tidak valid:", errorMessage.toString());
            return false;
        }
        return true;
    }

    private void handleSimpan() {
        if (!validateForm()) {return;}
        if (isEditMode) {updateKategori();}
        else {simpanKategoriBaru();}
    }

    private void simpanKategoriBaru() {
        String idBaru = tfID.getText().trim();
        String nama = tfNamaKategori.getText().trim();
        String deskripsi = tfDeskripsi.getText().trim();
        String status = "Aktif"; // Default status for new category
        try {
            DBConnect db = new DBConnect();
            String sql = "INSERT INTO KategoriObat (id_kategori, nama_kategori, deskripsi, status) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = db.conn.prepareStatement(sql);
            pst.setString(1, idBaru);
            pst.setString(2, nama);
            pst.setString(3, deskripsi);
            pst.setString(4, status);
            int result = pst.executeUpdate();
            if (result > 0) {
                showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Tersimpan",
                        "Data kategori '" + nama + "' berhasil disimpan dengan ID: " + idBaru,
                        () -> {
                            getDataKategori();
                            refreshData();
                            resetFormKategoriBaru();
                        });
            } else {
                showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menyimpan",
                        "Gagal menyimpan data kategori!");
            }
            pst.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Database Error",
                    "Error saat menyimpan data: " + e.getMessage());
        }
    }

    private void resetFormKategoriBaru() {
        tfNamaKategori.clear();
        tfDeskripsi.clear();
        tfID.setText(generateIdKategori());
        tfNamaKategori.requestFocus();
    }

    private void updateKategori() {
        String nama = tfNamaKategori.getText().trim();
        String deskripsi = tfDeskripsi.getText().trim();
        String status = tfStatusDisplay.getText().trim();
        try {
            DBConnect db = new DBConnect();
            String sql = "UPDATE KategoriObat SET nama_kategori = ?, deskripsi = ?, status = ? WHERE id_kategori = ?";
            PreparedStatement pst = db.conn.prepareStatement(sql);
            pst.setString(1, nama);
            pst.setString(2, deskripsi);
            pst.setString(3, status);
            pst.setString(4, currentEditId);
            int result = pst.executeUpdate();
            if (result > 0) {
                showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Diperbarui",
                        "Data kategori '" + nama + "' berhasil diperbarui.",
                        () -> {
                            hideForm();
                            refreshData();
                        });
            } else {
                showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Memperbarui",
                        "Gagal memperbarui data kategori!");
            }
            pst.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Database Error",
                    "Error saat memperbarui data: " + e.getMessage());
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
    }

    @Override
    public void filterTable(String keyword) {
        if (searchField != null) {
            searchField.setText(keyword);
        }
        applyFilters();
    }

    private void applyFilters() {
        filteredData.setPredicate(kategori -> {
            // Filter berdasarkan search text (semua kolom)
            boolean matchesSearch = true;
            if (searchField != null) {
                String searchText = searchField.getText();
                if (searchText != null && !searchText.trim().isEmpty()) {
                    String lowerCaseFilter = searchText.toLowerCase().trim();
                    matchesSearch = containsIgnoreCase(kategori.getIdKategori(), lowerCaseFilter) ||
                            containsIgnoreCase(kategori.getNamaKategori(), lowerCaseFilter) ||
                            containsIgnoreCase(kategori.getDeskripsi(), lowerCaseFilter) ||
                            containsIgnoreCase(kategori.getStatus(), lowerCaseFilter);
                }
            }

            // Filter berdasarkan status
            String selectedStatus = cbFilterStatus.getValue();
            boolean matchesStatus = true;
            if (selectedStatus != null && !selectedStatus.equals("Semua Status")) {
                matchesStatus = equalsIgnoreCase(kategori.getStatus(), selectedStatus);
            }
            return matchesSearch && matchesStatus;
        });
        tabelKategori.refresh();
        setupAksiKolom();
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) return false;
        return source.toLowerCase().contains(target);
    }

    private boolean equalsIgnoreCase(String source, String target) {
        if (source == null && target == null) return true;
        if (source == null || target == null) return false;
        return source.equalsIgnoreCase(target);
    }

    private void getDataKategori() {
        dataKategori.clear();
        try {
            DBConnect db = new DBConnect();
            String query = "SELECT * FROM KategoriObat ORDER BY id_kategori";
            ResultSet rs = db.stat.executeQuery(query);

            while (rs.next()) {
                KategoriObat kategori = new KategoriObat(
                        rs.getString("id_kategori"),
                        rs.getString("nama_kategori"),
                        rs.getString("deskripsi"),
                        rs.getString("status")
                );
                dataKategori.add(kategori);
            }
            rs.close();
            db.stat.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showNonBlockingAlert(Alert.AlertType.ERROR, "Database Error",
                    "Gagal ambil data dari database:", e.getMessage());
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
                btnEdit.setTooltip(new Tooltip("Edit data kategori"));
                btnEdit.setOnAction(event -> {
                    KategoriObat kategori = getTableView().getItems().get(getIndex());
                    showFormEdit(kategori);
                });
                Label deleteIcon = new Label("🗑");
                deleteIcon.setStyle("-fx-font-size: 10px; -fx-font-family: 'Segoe UI Emoji';");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setText("");
                btnDelete.setPrefSize(32, 28);
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setTooltip(new Tooltip("Hapus/nonaktifkan kategori"));
                btnDelete.setOnAction(event -> {
                    KategoriObat kategori = getTableView().getItems().get(getIndex());
                    handleDelete(kategori);
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

    private void handleDelete(KategoriObat kategori) {
        if ("Aktif".equalsIgnoreCase(kategori.getStatus())) {
            showConfirmationDialog("Konfirmasi Nonaktifkan", "Nonaktifkan Kategori",
                    "Status kategori '" + kategori.getNamaKategori() + "' akan diubah menjadi 'Tidak Aktif'. Lanjutkan?",
                    () -> {
                        try {
                            DBConnect db = new DBConnect();
                            String sql = "UPDATE KategoriObat SET status = 'Tidak Aktif' WHERE id_kategori = ?";
                            PreparedStatement pst = db.conn.prepareStatement(sql);
                            pst.setString(1, kategori.getIdKategori());
                            pst.executeUpdate();
                            pst.close();
                            db.conn.close();

                            refreshData();

                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Kategori Dinonaktifkan",
                                    "Kategori '" + kategori.getNamaKategori() + "' berhasil dinonaktifkan.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menonaktifkan",
                                    "Gagal mengubah status kategori.");
                        }
                    });
        } else {
            showConfirmationDialog("Konfirmasi Hapus Permanen", "PERINGATAN: Data akan dihapus permanen!",
                    "Data kategori '" + kategori.getNamaKategori() + "' akan dihapus secara permanen dari database. Tindakan ini tidak dapat dibatalkan. Lanjutkan?",
                    () -> {
                        try {
                            DBConnect db = new DBConnect();
                            String sql = "DELETE FROM KategoriObat WHERE id_kategori = ?";
                            PreparedStatement pst = db.conn.prepareStatement(sql);
                            pst.setString(1, kategori.getIdKategori());
                            pst.executeUpdate();
                            pst.close();
                            db.conn.close();

                            dataKategori.remove(kategori);
                            applyFilters();

                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data Dihapus",
                                    "Data kategori '" + kategori.getNamaKategori() + "' berhasil dihapus permanen.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showNonBlockingAlert(Alert.AlertType.ERROR, "Error", "Gagal Menghapus",
                                    "Gagal menghapus data dari database.");
                        }
                    });
        }
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
            confirm.initOwner(tabelKategori.getScene().getWindow());
            confirm.setOnHidden(e -> {
                if (confirm.getResult() == ButtonType.OK && onConfirm != null) {
                    onConfirm.run();
                }
            });
            confirm.show();
        });
    }

    public void refreshData() {
        getDataKategori();
        applyFilters();
    }
}