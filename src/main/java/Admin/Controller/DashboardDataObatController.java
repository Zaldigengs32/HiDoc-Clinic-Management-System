package Admin.Controller;

import Admin.KategoriObat;
import Admin.Obat;
import Interface.SearchableController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import Connection.DBConnect;
import java.sql.*;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;


public class DashboardDataObatController implements SearchableController {

    @FXML private TableView<Obat> tabelObat;
    @FXML private TableColumn<Obat, String> colIDObat;
    @FXML private TableColumn<Obat, String> colNamaObat;
    @FXML private TableColumn<Obat, Double> colHargaBeli;
    @FXML private TableColumn<Obat, Double> colHargaJual;
    @FXML private TableColumn<Obat, Integer> colStok;
    @FXML private TableColumn<Obat, String> colNamaKategori;
    @FXML private TableColumn<Obat, String> colStatusObat;
    @FXML private TableColumn<Obat, Void> colAksi;
    @FXML private ComboBox<String> cbFilterStatusObat;
    @FXML private TextField searchField;
    @FXML private Button btnTambahObat;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfIDObat;
    @FXML private TextField tfNamaObat;
    @FXML private ComboBox<KategoriObat> cbKategori;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;
    @FXML private Button btnCloseForm; // Tombol untuk menutup form


    private final ObservableList<Obat> allData = FXCollections.observableArrayList();
    private FilteredList<Obat> filteredData;
    private boolean isEditMode = false;
    private String currentEditId = null;
    private Obat originalObat = null;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();
        setupEventHandlers();
        filteredData = new FilteredList<>(allData, p -> true);
        tabelObat.setItems(filteredData);
        getDataObat();
        loadKategoriData();
        setupAksiKolom();
        cbFilterStatusObat.setValue("Semua Status");
        cbFilterStatusObat.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void setupTableColumns() {
        colIDObat.setCellValueFactory(cellData -> cellData.getValue().idObatProperty());
        colNamaObat.setCellValueFactory(cellData -> cellData.getValue().namaObatProperty());
        colHargaBeli.setCellValueFactory(cellData -> cellData.getValue().hargaBeliProperty().asObject());
        colHargaBeli.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Rp. " + formatCurrency(item.longValue()));
                }
            }
        });

        colHargaJual.setCellValueFactory(cellData -> cellData.getValue().hargaJualProperty().asObject());
        colHargaJual.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Rp. " + formatCurrency(item.longValue()));
                }
            }
        });
        colStok.setCellValueFactory(cellData -> cellData.getValue().stokProperty().asObject());
        colNamaKategori.setCellValueFactory(cellData -> cellData.getValue().namaKategoriProperty());
        colStatusObat.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
    }

    private void setupComboBoxes() {
        cbFilterStatusObat.setItems(FXCollections.observableArrayList("Semua Status", "Tersedia", "Tidak Tersedia", "Dihapus"));
    }

    private void setupEventHandlers() {
        cbFilterStatusObat.setOnAction(e -> applyFilters());
        btnTambahObat.setOnAction(e -> showAddForm());
        btnSimpan.setOnAction(e -> handleSimpan());
        btnBatal.setOnAction(e -> handleBatal());        // Untuk membatalkan transaksi
        btnCloseForm.setOnAction(e -> tutupForm());      // Untuk menutup form
        formPopup.setOnMouseClicked(e -> {
            if (e.getTarget() == formPopup) {
                tutupForm();
            }
        });
    }

    private void handleBatal() {
        if (isEditMode) {
            // Mode edit: batalkan perubahan dan kembalikan ke data asli
            boolean adaPerubahan = !tfNamaObat.getText().equals(originalObat.getNamaObat()) ||
                    !cbKategori.getValue().getIdKategori().equals(originalObat.getIdKategori());

            if (adaPerubahan) {
                showConfirmationDialog("Konfirmasi Batal", "Batalkan Perubahan",
                        "Yakin ingin membatalkan perubahan dan mengembalikan data ke kondisi awal?",
                        () -> {
                            populateFormFields(originalObat); // Kembalikan ke data asli
                            showNonBlockingAlert(Alert.AlertType.INFORMATION, "Berhasil", "Perubahan Dibatalkan",
                                    "Perubahan berhasil dibatalkan dan data dikembalikan ke kondisi awal.");
                        });
            } else {
                showNonBlockingAlert(Alert.AlertType.WARNING, "Tidak Ada Perubahan", "Data Tidak Berubah",
                        "Tidak ada perubahan yang perlu dibatalkan.");
            }
        } else {
            // Mode tambah: kosongkan form (kode yang sudah ada)
            boolean adaDataDiForm = (tfNamaObat.getText() != null && !tfNamaObat.getText().trim().isEmpty()) ||
                    (cbKategori.getValue() != null);

            if (adaDataDiForm) {
                showConfirmationDialog("Konfirmasi Batal", "Batalkan Input",
                        "Yakin ingin membatalkan input dan mengosongkan form?",
                        () -> {
                            clearFormFields();
                            generateNextId();
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

    private void showConfirmationDialog(String title, String header, String content, Runnable onConfirm) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle(title);
        confirmAlert.setHeaderText(header);
        confirmAlert.setContentText(content);

        DialogPane dialogPane = confirmAlert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-background-color: #f8f9fa;");
        confirmAlert.initOwner(formPopup.getScene().getWindow());

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                onConfirm.run();
            }
        });
    }

    private void showNonBlockingAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-background-color: #f8f9fa;");
        alert.initOwner(formPopup.getScene().getWindow());
        alert.show();
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(obat -> {
            String searchText = searchField != null ? searchField.getText() : "";
            boolean matchesSearch = true;
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase().trim();
                matchesSearch = obat.getIdObat().toLowerCase().contains(lowerCaseFilter) ||
                        obat.getNamaObat().toLowerCase().contains(lowerCaseFilter) ||
                        obat.getNamaKategori().toLowerCase().contains(lowerCaseFilter) ||
                        obat.getStatus().toLowerCase().contains(lowerCaseFilter) ||
                        String.valueOf(obat.getStok()).toLowerCase().contains(lowerCaseFilter) ||
                        String.valueOf(obat.getHargaBeli()).toLowerCase().contains(lowerCaseFilter) ||
                        String.valueOf(obat.getHargaJual()).toLowerCase().contains(lowerCaseFilter);
            }

            String statusFilter = cbFilterStatusObat.getValue();
            boolean matchesStatus = true;
            if (statusFilter != null && !statusFilter.equals("Semua Status")) {
                matchesStatus = obat.getStatus().equalsIgnoreCase(statusFilter);
            }

            return matchesSearch && matchesStatus;
        });

        Platform.runLater(() -> {
            tabelObat.refresh();
        });
    }

    @Override
    public void filterTable(String keyword) {
        if (searchField != null) {
            searchField.setText(keyword);
        }
        applyFilters();
    }

    private void showAddForm() {
        isEditMode = false;
        currentEditId = null;
        formTitle.setText("💊 Tambah Data Obat");
        clearFormFields();
        generateNextId();
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    private void showEditForm(Obat obat) {
        isEditMode = true;
        currentEditId = obat.getIdObat();
        originalObat = new Obat(obat.getIdObat(), obat.getNamaObat(), obat.getHargaBeli(),
                obat.getHargaJual(), obat.getStok(), obat.getIdKategori(),
                obat.getStatus(), obat.getNamaKategori()); // Simpan data asli
        formTitle.setText("💊 Edit Data Obat");
        populateFormFields(obat);
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    private void hideForm() {
        formPopup.setVisible(false);
        formPopup.setManaged(false);
        clearFormFields();
        isEditMode = false;
        currentEditId = null;
        originalObat = null; // Reset data asli
    }

    private void clearFormFields() {
        tfIDObat.setText("Auto Generate");
        tfNamaObat.clear();
        cbKategori.setValue(null);
    }


    private void populateFormFields(Obat obat) {
        tfIDObat.setText(obat.getIdObat());
        tfNamaObat.setText(obat.getNamaObat());
        for (KategoriObat kategori : cbKategori.getItems()) {
            if (kategori.getIdKategori().equals(obat.getIdKategori())) {
                cbKategori.setValue(kategori);
                break;
            }
        }
    }

    private void generateNextId() {
        try {
            DBConnect db = new DBConnect();
            String query = "SELECT IDObat FROM Obat WHERE IDObat LIKE 'OBT%'";
            ResultSet rs = db.stat.executeQuery(query);

            int maxNumber = 0;
            while (rs.next()) {
                String id = rs.getString("IDObat");
                if (id != null && id.startsWith("OBT") && id.length() >= 6) { // OBT + 3 digit = 6 karakter
                    try {
                        String numberPart = id.substring(3); // Ambil bagian setelah "OBT"
                        int number = Integer.parseInt(numberPart);
                        if (number > maxNumber) {
                            maxNumber = number;
                        }
                    } catch (NumberFormatException e) {
                        // Skip ID yang tidak valid (tidak bisa di-parse ke angka)
                        System.out.println("Skipping invalid ID: " + id);
                    }
                }
            }

            int nextNumber = maxNumber + 1;
            String newId = "OBT" + String.format("%03d", nextNumber);
            tfIDObat.setText(newId);

            System.out.println("Generated new ID: " + newId); // Debug log

            rs.close();
            db.stat.close();
            db.conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generating next ID: " + e.getMessage());
            // Fallback: set ID default
            tfIDObat.setText("OBT001");
        }
    }

    private void handleSimpan() {
        if (!validateForm()) {
            return;
        }
        try {
            DBConnect db = new DBConnect();
            String sql;
            PreparedStatement pst;
            if (isEditMode) {
                sql = "UPDATE Obat SET namaObat=?, idKategori=? WHERE idObat=?";
                pst = db.conn.prepareStatement(sql);
                pst.setString(1, tfNamaObat.getText());
                pst.setString(2, cbKategori.getValue().getIdKategori());
                pst.setString(3, currentEditId);
            } else {
                sql = "INSERT INTO Obat (idObat, namaObat, hargaBeli, hargaJual, stok, idKategori, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                pst = db.conn.prepareStatement(sql);
                pst.setString(1, tfIDObat.getText());
                pst.setString(2, tfNamaObat.getText());
                pst.setDouble(3, 0.0);
                pst.setDouble(4, 0.0);
                pst.setInt(5, 0);
                pst.setString(6, cbKategori.getValue().getIdKategori());
                pst.setString(7, "Tidak Tersedia");
            }
            pst.executeUpdate();
            db.conn.close();
            showSuccessAlert(isEditMode ? "Data obat berhasil diperbarui!" : "Data obat berhasil ditambahkan!");
            getDataObat();
            if (isEditMode) {
                hideForm();
            } else {
                clearFormFields();
                generateNextId();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Gagal menyimpan data: " + e.getMessage());
        }
    }

    private void showSuccessAlert(String message) {
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Berhasil");
        successAlert.setHeaderText(null);
        successAlert.setContentText(message);
        DialogPane dialogPane = successAlert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-background-color: #f8f9fa;");
        successAlert.initOwner(formPopup.getScene().getWindow());
        successAlert.show();
    }

    private void showErrorAlert(String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Error");
        errorAlert.setHeaderText(null);
        errorAlert.setContentText(message);
        DialogPane dialogPane = errorAlert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI'; -fx-background-color: #f8f9fa;");
        errorAlert.initOwner(formPopup.getScene().getWindow());
        errorAlert.show();
    }

    private boolean validateForm() {
        if (tfNamaObat.getText().trim().isEmpty()) {
            showErrorAlert("Nama obat tidak boleh kosong!");
            return false;
        }
        if (cbKategori.getValue() == null) {
            showErrorAlert("Kategori harus dipilih!");
            return false;
        }
        return true;
    }

    private void loadKategoriData() {
        try {
            DBConnect db = new DBConnect();
            String query = "SELECT id_kategori, nama_kategori FROM KategoriObat";
            ResultSet rs = db.stat.executeQuery(query);
            ObservableList<KategoriObat> kategoriList = FXCollections.observableArrayList();
            while (rs.next()) {
                String id = rs.getString("id_kategori");
                String nama = rs.getString("nama_kategori");
                kategoriList.add(new KategoriObat(id, nama));
            }
            cbKategori.setItems(kategoriList);
            rs.close();
            db.stat.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatusBasedOnStock() {
        try {
            DBConnect db = new DBConnect();
            String sql = "UPDATE Obat SET status = 'Tidak Tersedia' WHERE stok = 0 AND status != 'Dihapus'";
            db.stat.executeUpdate(sql);
            sql = "UPDATE Obat SET status = 'Tersedia' WHERE stok > 0 AND status != 'Dihapus'";
            db.stat.executeUpdate(sql);
            db.stat.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDataObat() {
        updateStatusBasedOnStock();
        allData.clear();
        try {
            DBConnect db = new DBConnect();
            // Perbaikan: Sesuaikan nama kolom dengan yang ada di database
            String query = "SELECT o.IDObat, o.NamaObat, o.HargaBeli, " +
                    "o.HargaJual, o.Stok, o.IDKategori, k.nama_kategori AS namaKategori, " +
                    "o.Status FROM Obat o " +
                    "JOIN KategoriObat k ON o.IDKategori = k.id_kategori " +
                    "ORDER BY o.IDObat";

            ResultSet rs = db.stat.executeQuery(query);
            while (rs.next()) {
                Obat obat = new Obat(
                        rs.getString("IDObat"),
                        rs.getString("NamaObat"),
                        rs.getDouble("HargaBeli"),
                        rs.getDouble("HargaJual"),
                        rs.getInt("Stok"),
                        rs.getString("IDKategori"),
                        rs.getString("Status"),
                        rs.getString("namaKategori")
                );
                allData.add(obat);
            }
            rs.close();
            db.stat.close();
            db.conn.close();
            Platform.runLater(() -> {
                tabelObat.refresh();
                applyFilters();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.ERROR, "Gagal ambil data obat dari database:\n" + e.getMessage()).show();
            });
        }
    }

    private String formatCurrency(long value) {
        String str = String.valueOf(value);
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = str.length() - 1; i >= 0; i--) {
            if (count == 3) {
                formatted.insert(0, ".");
                count = 0;
            }
            formatted.insert(0, str.charAt(i));
            count++;
        }
        return formatted.toString();
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
                btnEdit.setPrefHeight(28);
                btnEdit.setPrefWidth(32);
                btnEdit.setStyle("-fx-background-color: orange; -fx-background-radius: 6; -fx-cursor: hand;");
                btnEdit.setOnAction(event -> {
                    Obat obat = getTableView().getItems().get(getIndex());
                    showEditForm(obat);
                });
                Label deleteIcon = new Label("🗑");
                deleteIcon.setStyle("-fx-font-size: 10px; -fx-font-family: 'Segoe UI Emoji';");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setText("");
                btnDelete.setPrefHeight(28);
                btnDelete.setPrefWidth(32);
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setOnAction(event -> {
                    Obat obat = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Konfirmasi Penghapusan");
                    confirm.setHeaderText("Apakah Anda yakin ingin menghapus data obat ini?");
                    confirm.setContentText("Nama Obat: " + obat.getNamaObat() +
                            "\nKategori: " + obat.getNamaKategori() +
                            "\n\nCatatan: Data akan dinonaktifkan, tidak dihapus permanen.");

                    DialogPane dialogPane = confirm.getDialogPane();
                    dialogPane.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
                    confirm.initOwner(getTableView().getScene().getWindow());
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            softDeleteObat(obat.getIdObat());
                        }
                    });
                });
            }

            private void softDeleteObat(String idObat) {
                try {
                    DBConnect db = new DBConnect();
                    String sql = "UPDATE Obat SET status = 'Dihapus' WHERE idObat = ?";
                    PreparedStatement pst = db.conn.prepareStatement(sql);
                    pst.setString(1, idObat);
                    int rowsAffected = pst.executeUpdate();
                    if (rowsAffected > 0) {
                        getDataObat();
                        applyFilters();
                        showSuccessAlert("Data obat berhasil dihapus.");
                    } else {
                        showErrorAlert("Gagal menghapus data: Data tidak ditemukan.");
                    }
                    pst.close();
                    db.conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorAlert("Gagal menghapus data: " + e.getMessage());
                }
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }
}