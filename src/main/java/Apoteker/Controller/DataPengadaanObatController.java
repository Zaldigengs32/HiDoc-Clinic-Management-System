package Apoteker.Controller;

import Connection.DBConnect;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javafx.scene.control.DatePicker;
import java.text.DecimalFormat;
import java.text.ParseException;

public class DataPengadaanObatController implements Initializable {

    private DBConnect dbConnect;
    private SessionManager sessionManager;
    private DecimalFormat rupiahFormat = new DecimalFormat("#,###");

    @FXML private TableView<PengadaanObat> tabelPengadaan;
    @FXML private TableColumn<PengadaanObat, String> colIDPengadaan;
    @FXML private TextField tfJumlahUang;
    @FXML private TextField tfKembalian;
    @FXML private TableColumn<PengadaanObat, String> colNamaKaryawan;
    @FXML private TableColumn<PengadaanObat, String> colNamaSupplier;
    @FXML private TableColumn<PengadaanObat, String> colTanggalPembelian;
    @FXML private TableColumn<PengadaanObat, String> colTotalHarga;
    @FXML private TableColumn<PengadaanObat, Void> colAksi;
    @FXML private ComboBox<String> cbBulanFilter;
    @FXML private TextField searchField;
    @FXML private Button btnTambah;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfIDPengadaan;
    @FXML private TextField tfNamaKaryawan;
    @FXML private ComboBox<Supplier> cbSupplier;
    @FXML private TextField tfTanggalPembelian;
    @FXML private ComboBox<Obat> cbObat;
    @FXML private TextField tfKuantitas;
    @FXML private TextField tfHargaSatuan;
    @FXML private Button btnTambahKeranjang;
    @FXML private TableView<KeranjangObat> tabelKeranjang;
    @FXML private TableColumn<KeranjangObat, String> colNamaObat;
    @FXML private TableColumn<KeranjangObat, Integer> colKuantitas;
    @FXML private TableColumn<KeranjangObat, String> colHargaSatuan;
    @FXML private TableColumn<KeranjangObat, String> colSubTotal;
    @FXML private TableColumn<KeranjangObat, Void> colHapus;
    @FXML private Label lblTotalBelanja;
    @FXML private VBox detailPopup;
    @FXML private Label lblDetailID;
    @FXML private Label lblDetailKaryawan;
    @FXML private Label lblDetailSupplier;
    @FXML private Label lblDetailTanggal;
    @FXML private TableView<DetailPengadaan> tabelDetail;
    @FXML private TableColumn<DetailPengadaan, String> colDetailNamaObat;
    @FXML private TableColumn<DetailPengadaan, Integer> colDetailKuantitas;
    @FXML private TableColumn<DetailPengadaan, String> colDetailHargaSatuan;
    @FXML private TableColumn<DetailPengadaan, String> colDetailSubTotal;
    @FXML private Label lblDetailTotal;
    @FXML private DatePicker dpTanggalFilter;

    private ObservableList<PengadaanObat> pengadaanList;
    private ObservableList<PengadaanObat> filteredList;
    private ObservableList<KeranjangObat> keranjangList;
    private ObservableList<Supplier> supplierList;
    private ObservableList<Obat> obatList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dbConnect = new DBConnect();
        sessionManager = SessionManager.getInstance();
        System.out.println("🔧 Controller initialized - SessionManager: " +
                (sessionManager != null ? "OK" : "NULL"));
        sessionManager.printCurrentSession();
        initializeData();
        setupTableColumns();
        setupKeranjangTableColumns();
        setupDetailTableColumns();
        loadDataFromDatabase();
        populateComboBoxes();
        populateTable();
        setupSearch();
        setupTextField();
        setupHargaSatuanValidation();
        setupObatSelection();
        setupJumlahUangListener();
        resetFormPembayaran();
        setupDateFilter();
        tfHargaSatuan.setDisable(false);
        generateNewPengadaanId();
    }

    private void initializeData() {
        pengadaanList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();
        keranjangList = FXCollections.observableArrayList();
        supplierList = FXCollections.observableArrayList();
        obatList = FXCollections.observableArrayList();
    }

    private void setupDateFilter() {
        dpTanggalFilter.setValue(null);
        dpTanggalFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private String formatRupiah(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        try {
            String cleanInput = input.replaceAll("[^\\d]", "");
            if (cleanInput.isEmpty()) {
                return "";
            }
            long value = Long.parseLong(cleanInput);
            return rupiahFormat.format(value);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private double parseRupiah(String input) throws ParseException {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }
        String cleanInput = input.replaceAll("[^\\d]", "");
        if (cleanInput.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(cleanInput);
    }

    private void loadDataFromDatabase() {
        loadPengadaanData();
        loadSupplierData();
        loadObatData();
    }

    private String formatDateForDisplay(String dateFromDB) {
        try {
            String dateOnly = dateFromDB.length() > 10 ? dateFromDB.substring(0, 10) : dateFromDB;
            LocalDate date = LocalDate.parse(dateOnly);
            return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("en", "US")));
        } catch (Exception e) {
            System.err.println("Error formatting date: " + e.getMessage());
            return dateFromDB;
        }
    }

    private void loadPengadaanData() {
        try {
            String query = """
        SELECT 
            tp.IDPengadaan,
            tp.IDKaryawan,
            k.NamaKaryawan,
            tp.IDSupplier,
            s.NamaSupplier,
            tp.TanggalPembelian,
            tp.TotalHarga
        FROM TransaksiPengadaanObat tp
        LEFT JOIN Karyawan k ON tp.IDKaryawan = k.IDKaryawan
        LEFT JOIN Supplier s ON tp.IDSupplier = s.IDSupplier
        ORDER BY tp.TanggalPembelian DESC
        """;
            ResultSet rs = dbConnect.stat.executeQuery(query);
            pengadaanList.clear();
            while (rs.next()) {
                String formattedDate = formatDateForDisplay(rs.getString("TanggalPembelian"));
                PengadaanObat pengadaan = new PengadaanObat(
                        rs.getString("IDPengadaan"),
                        rs.getString("NamaKaryawan"),
                        rs.getString("NamaSupplier"),
                        formattedDate, // Menggunakan tanggal yang sudah diformat
                        rs.getDouble("TotalHarga")
                );
                pengadaanList.add(pengadaan);
            }
            filteredList.setAll(pengadaanList);
            System.out.println("✅ Successfully loaded " + pengadaanList.size() + " pengadaan records");
        } catch (SQLException e) {
            System.err.println("Error loading pengadaan data: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data pengadaan dari database: " + e.getMessage());
        }
    }

    private void loadSupplierData() {
        try {
            String query = "SELECT * FROM Supplier ORDER BY NamaSupplier";
            ResultSet rs = dbConnect.stat.executeQuery(query);
            supplierList.clear();
            while (rs.next()) {
                Supplier supplier = new Supplier(
                        rs.getString("IDSupplier"),
                        rs.getString("NamaSupplier"),
                        rs.getString("AlamatSupplier"),
                        rs.getString("TeleponSupplier")
                );
                supplierList.add(supplier);
            }
        } catch (SQLException e) {
            System.err.println("Error loading supplier data: " + e.getMessage());
            showAlert("Error", "Gagal memuat data supplier dari database!");
        }
    }

    private void loadObatData() {
        try {
            String query = "SELECT IDObat, NamaObat, Stok, Status FROM Obat WHERE Status = 'Tidak Tersedia' ORDER BY NamaObat";
            ResultSet rs = dbConnect.stat.executeQuery(query);
            obatList.clear();
            while (rs.next()) {
                Obat obat = new Obat(
                        rs.getString("IDObat"),
                        rs.getString("NamaObat"),
                        0.0,                         // Set harga ke 0 karena input manual
                        rs.getInt("Stok"),
                        rs.getString("Status")
                );
                obatList.add(obat);
            }
        } catch (SQLException e) {
            System.err.println("Error loading obat data: " + e.getMessage());
            showAlert("Error", "Gagal memuat data obat dari database!");
        }
    }

    private void setupTableColumns() {
        colIDPengadaan.setCellValueFactory(new PropertyValueFactory<>("idPengadaan"));
        colNamaKaryawan.setCellValueFactory(new PropertyValueFactory<>("namaKaryawan"));
        colNamaSupplier.setCellValueFactory(new PropertyValueFactory<>("namaSupplier"));
        colTanggalPembelian.setCellValueFactory(new PropertyValueFactory<>("tanggalPembelian"));
        colTotalHarga.setCellValueFactory(new PropertyValueFactory<>("totalHargaFormatted"));
        colAksi.setCellFactory(new Callback<TableColumn<PengadaanObat, Void>, TableCell<PengadaanObat, Void>>() {
            @Override
            public TableCell<PengadaanObat, Void> call(TableColumn<PengadaanObat, Void> param) {
                return new TableCell<PengadaanObat, Void>() {
                    private final Button detailBtn = new Button("👁 Detail");

                    {
                        detailBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " + "-fx-background-radius: 8; -fx-padding: 5 10; -fx-font-size: 12px;");
                        detailBtn.setOnAction(e -> {
                            PengadaanObat pengadaan = getTableView().getItems().get(getIndex());
                            showDetailModal(pengadaan);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(detailBtn);
                        }
                    }
                };
            }
        });
    }

    private void setupKeranjangTableColumns() {
        colNamaObat.setCellValueFactory(new PropertyValueFactory<>("namaObat"));
        colKuantitas.setCellValueFactory(new PropertyValueFactory<>("kuantitas"));
        colHargaSatuan.setCellValueFactory(new PropertyValueFactory<>("hargaSatuanFormatted"));
        colSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotalFormatted"));
        colHapus.setCellFactory(new Callback<TableColumn<KeranjangObat, Void>, TableCell<KeranjangObat, Void>>() {
            @Override
            public TableCell<KeranjangObat, Void> call(TableColumn<KeranjangObat, Void> param) {
                return new TableCell<KeranjangObat, Void>() {
                    private final Button hapusBtn = new Button("🗑");
                    {
                        hapusBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                "-fx-background-radius: 8; -fx-padding: 5 8;");
                        hapusBtn.setOnAction(e -> {
                            KeranjangObat item = getTableView().getItems().get(getIndex());
                            hapusDariKeranjang(item);
                            if (keranjangList.isEmpty()){
                                cbSupplier.setDisable(false);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(hapusBtn);
                        }
                    }
                };
            }
        });

        tabelKeranjang.setItems(keranjangList);
    }

    private void hitungKembalian() {
        try {
            String jumlahUangText = tfJumlahUang.getText().trim();
            if (jumlahUangText.equals("Rp. ") || jumlahUangText.isEmpty()) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                tfKembalian.setText(formatter.format(0));
                return;
            }

            double jumlahUang = parseRupiah(jumlahUangText);
            double totalBelanja = keranjangList.stream().mapToDouble(KeranjangObat::getSubTotal).sum();
            double kembalian = jumlahUang - totalBelanja;
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            if (kembalian < 0) {
                tfKembalian.setText("Kurang: " + formatter.format(Math.abs(kembalian)));
                tfKembalian.setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); -fx-border-color: #e57373; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #c62828;");
            } else {
                tfKembalian.setText(formatter.format(kembalian));
                tfKembalian.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e8, #c8e6c8); -fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #2e7d32;");
            }
        } catch (Exception e) {
            tfKembalian.setText("Format tidak valid");
            tfKembalian.setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); -fx-border-color: #e57373; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #c62828;");
        }
    }

    private void setupDetailTableColumns() {
        colDetailNamaObat.setCellValueFactory(new PropertyValueFactory<>("namaObat"));
        colDetailKuantitas.setCellValueFactory(new PropertyValueFactory<>("kuantitas"));
        colDetailHargaSatuan.setCellValueFactory(new PropertyValueFactory<>("hargaSatuanFormatted"));
        colDetailSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotalFormatted"));
    }

    private void populateComboBoxes() {
        cbSupplier.setItems(supplierList);
        cbSupplier.setCellFactory(param -> new ListCell<Supplier>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNamaSupplier());
                }
            }
        });
        cbSupplier.setButtonCell(new ListCell<Supplier>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNamaSupplier());
                }
            }
        });

        cbObat.setItems(obatList);
        cbObat.setCellFactory(param -> new ListCell<Obat>() {
            @Override
            protected void updateItem(Obat item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNamaObat()); // HANYA NAMA OBAT
                }
            }
        });
        cbObat.setButtonCell(new ListCell<Obat>() {
            @Override
            protected void updateItem(Obat item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNamaObat()); // HANYA NAMA OBAT
                }
            }
        });
    }

    private void populateTable() {
        tabelPengadaan.setItems(filteredList);
    }
    private void setupSearch() {searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());}

    private void setupTextField() {
        if (tfKuantitas != null) {
            tfKuantitas.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    tfKuantitas.setText(newValue.replaceAll("[^\\d]", ""));
                }
                if (newValue.length() > 3) {
                    tfKuantitas.setText(newValue.substring(0, 3));
                }
            });

            tfKuantitas.setText("1");
            tfKuantitas.setPromptText("Masukkan kuantitas");
        } else {
            System.err.println("Error: tfKuantitas is null - check FXML file");
        }
    }

    private void setupObatSelection() {
        cbObat.setOnAction(e -> {
            Obat selectedObat = cbObat.getValue();
            if (selectedObat != null) {
                tfHargaSatuan.setText("Rp. ");
                tfHargaSatuan.requestFocus();
                Platform.runLater(() -> tfHargaSatuan.positionCaret(tfHargaSatuan.getText().length()));
            }
        });
    }

    private void showBlockingAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            Window owner = formTitle.getScene().getWindow();
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        } catch (Exception e) {
            alert.initModality(Modality.APPLICATION_MODAL);
        }
        alert.showAndWait();
    }

    private void applyFilters() {
        filteredList.clear();
        String searchText = searchField.getText().toLowerCase();
        LocalDate selectedDate = dpTanggalFilter.getValue();
        for (PengadaanObat pengadaan : pengadaanList) {
            boolean matchesSearch = searchText.isEmpty() ||
                    pengadaan.getIdPengadaan().toLowerCase().contains(searchText) ||
                    pengadaan.getNamaKaryawan().toLowerCase().contains(searchText) ||
                    pengadaan.getNamaSupplier().toLowerCase().contains(searchText);

            boolean matchesDate = selectedDate == null;
            if (selectedDate != null) {
                try {
                    String displayDate = pengadaan.getTanggalPembelian();
                    LocalDate pengadaanDate = LocalDate.parse(displayDate,
                            DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("en", "US")));
                    matchesDate = pengadaanDate.equals(selectedDate);
                } catch (Exception e) {
                    matchesDate = pengadaan.getTanggalPembelian().equals(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }
            }
            if (matchesSearch && matchesDate) {
                filteredList.add(pengadaan);
            }
        }
    }

    private String generateNewPengadaanId() {
        String newId = generateNewPengadaanIdString();
        tfIDPengadaan.setText(newId);
        return newId;
    }

    private String generateNewPengadaanIdString() {
        try {
            String query = "SELECT TOP 1 IDPengadaan FROM TransaksiPengadaanObat " +
                    "WHERE IDPengadaan LIKE 'TRPO%' ORDER BY IDPengadaan DESC";
            dbConnect.stat = dbConnect.conn.createStatement();
            ResultSet rs = dbConnect.stat.executeQuery(query);
            if (rs.next()) {
                String lastId = rs.getString("IDPengadaan");
                int number = Integer.parseInt(lastId.substring(4)) + 1;
                return "TRPO" + String.format("%03d", number);
            } else {
                return "TRPO001";
            }
        } catch (SQLException e) {
            System.err.println("Error generating ID: " + e.getMessage());
            return "TRPO001";
        }
    }

    @FXML
    private void tampilkanFormPengadaan() {
        formTitle.setText("📋 Form Pengadaan Obat");
        System.out.println("=== FORM PENGADAAN DEBUG START ===");
        System.out.println("SessionManager: " + (sessionManager != null ? "EXISTS" : "NULL"));
        if (sessionManager != null) {
            sessionManager.printCurrentSession();
        } else {
            System.out.println("❌ ERROR: SessionManager is NULL! Re-initializing...");
            sessionManager = SessionManager.getInstance();
            sessionManager.printCurrentSession();
        }

        String newId = generateNewPengadaanId();
        tfIDPengadaan.setText(newId);
        tfTanggalPembelian.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            String displayName = sessionManager.getDisplayName(); // Prioritas fullName
            tfNamaKaryawan.setText(displayName);
            tfNamaKaryawan.setEditable(false);
            tfNamaKaryawan.setStyle("-fx-background-color: linear-gradient(to bottom, #caeaf7, #caeaf7); " +
                    "-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 12; " +
                    "-fx-background-radius: 12; -fx-padding: 12; -fx-font-size: 14px; " +
                    "-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
            System.out.println("✅ SUCCESS: Form loaded for user: " + displayName +
                    " (ID: " + sessionManager.getLoggedInUserId() + ")");
        } else {
            tfNamaKaryawan.setText("User belum login");
            tfNamaKaryawan.setEditable(false);
            System.out.println("❌ ERROR: User not logged in!");
            if (sessionManager != null) {
                sessionManager.printCurrentSession();
            }
            showAlert("Warning", "Tidak ada user yang login! Silakan login terlebih dahulu.");
            return;
        }
        System.out.println("=== FORM PENGADAAN DEBUG END ===");
        resetFormPembayaran();
        cbSupplier.setDisable(false);
        cbSupplier.setValue(null);
        keranjangList.clear();
        updateTotalBelanja();
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    @FXML
    private void tambahKeKeranjang() {
        Obat selectedObat = cbObat.getValue();
        Integer kuantitas = getKuantitas();
        String hargaText = tfHargaSatuan.getText().trim();
        if (selectedObat == null || kuantitas == null || kuantitas <= 0) {
            showAlert("Error", "Mohon pilih obat dan masukkan kuantitas yang valid!");
            return;
        }
        if (hargaText.isEmpty()) {
            showAlert("Error", "Mohon masukkan harga satuan!");
            return;
        }
        if (cbSupplier.getValue() == null) {
            showBlockingAlert("Error", "Mohon pilih supplier terlebih dahulu!");
            return;
        }
        double hargaSatuan;
        try {
            hargaSatuan = parseRupiah(hargaText);
            if (hargaSatuan <= 0) {
                showAlert("Error", "Harga satuan harus lebih dari 0!");
                return;
            }
        } catch (Exception e) {
            showAlert("Error", "Format harga tidak valid!");
            return;
        }
        cbSupplier.setDisable(true);
        for (KeranjangObat item : keranjangList) {
            if (item.getNamaObat().equals(selectedObat.getNamaObat())) {
                showAlert("Warning", "Obat sudah ada dalam keranjang! Hapus terlebih dahulu jika ingin mengubah kuantitas.");
                return;
            }
        }

        double subTotal = hargaSatuan * kuantitas;
        System.out.println("Harga Satuan: " + hargaSatuan);
        System.out.println("Sub Total: " + subTotal);
        KeranjangObat newItem = new KeranjangObat(selectedObat.getNamaObat(), kuantitas, hargaSatuan, subTotal);
        keranjangList.add(newItem);
        cbObat.setValue(null);
        tfKuantitas.setText("1");
        tfHargaSatuan.setText("Rp. "); // Reset dengan prefix
        updateTotalBelanja();
        showSuccessAlert("Obat berhasil ditambahkan ke keranjang!");
    }

    private void setupHargaSatuanValidation() {
        tfHargaSatuan.setText("Rp. ");
        tfHargaSatuan.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.startsWith("Rp. ")) {
                tfHargaSatuan.setText("Rp. " + newValue.replaceFirst("^Rp\\. ?", ""));
                return;
            }
            String numberPart = newValue.substring(4); // Hapus "Rp. "
            String formatted = formatRupiah(numberPart);
            if (!numberPart.equals(formatted)) {
                tfHargaSatuan.setText("Rp. " + formatted);
            }
        });

        tfHargaSatuan.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Platform.runLater(() -> {
                    if (tfHargaSatuan.getCaretPosition() < 4) {
                        tfHargaSatuan.positionCaret(tfHargaSatuan.getText().length());
                    }
                });
            }
        });
        tfHargaSatuan.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() < 4) {
                Platform.runLater(() -> tfHargaSatuan.positionCaret(Math.max(4, tfHargaSatuan.getText().length())));
            }
        });
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            Stage currentStage = (Stage) cbObat.getScene().getWindow();
            alert.initOwner(currentStage);
            alert.initModality(Modality.WINDOW_MODAL);
        } catch (Exception e) {
            alert.initModality(Modality.APPLICATION_MODAL);
        }
        alert.showAndWait();
    }

    private void hapusDariKeranjang(KeranjangObat item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus item dari keranjang?");
        alert.setContentText("Apakah Anda yakin ingin menghapus \"" + item.getNamaObat() + "\" dari keranjang?");
        try {
            if (tabelKeranjang != null && tabelKeranjang.getScene() != null) {
                alert.initOwner(tabelKeranjang.getScene().getWindow());
            }
            alert.initModality(Modality.WINDOW_MODAL);
        } catch (Exception e) {
            alert.initModality(Modality.APPLICATION_MODAL);
        }
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            keranjangList.remove(item);
            updateTotalBelanja();
            showSuccessAlert("Obat \"" + item.getNamaObat() + "\" berhasil dihapus dari keranjang!");
            if (keranjangList.isEmpty()) {
                cbSupplier.setDisable(false);
            }
        }
    }

    private void updateTotalBelanja() {
        double total = keranjangList.stream().mapToDouble(KeranjangObat::getSubTotal).sum();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        lblTotalBelanja.setText(formatter.format(total));
    }

    @FXML
    private void simpanPengadaan() {
        if (!sessionManager.isLoggedIn()) {
            showAlert("Error", "Mohon login terlebih dahulu sebelum melakukan transaksi!");
            return;
        }
        if (cbSupplier.getValue() == null) {
            showAlert("Error", "Mohon pilih supplier!");
            return;
        }
        if (keranjangList.isEmpty()) {
            showAlert("Error", "Mohon tambahkan minimal satu obat ke keranjang!");
            return;
        }
        if (!validasiPembayaran()) {
            return;
        }
        if (!showConfirmationDialog("Konfirmasi Simpan",
                "Apakah Anda yakin ingin menyimpan pengadaan ini?\n\n" +
                        "Total Item: " + keranjangList.size() + "\n" +
                        "Total Harga: " + String.format("Rp %,.2f",
                        keranjangList.stream().mapToDouble(KeranjangObat::getSubTotal).sum()))) {
            return;
        }
        try {
            dbConnect.conn.setAutoCommit(false);
            String pengadaanQuery = "INSERT INTO TransaksiPengadaanObat (IDPengadaan, IDKaryawan, IDSupplier, TanggalPembelian, TotalHarga) VALUES (?, ?, ?, ?, ?)";
            dbConnect.pstat = dbConnect.conn.prepareStatement(pengadaanQuery);
            String idPengadaan = tfIDPengadaan.getText();
            String idKaryawan = sessionManager.getLoggedInUserId();
            if (idKaryawan == null || idKaryawan.trim().isEmpty()) {
                showAlert("Error", "ID Karyawan tidak valid. Mohon login ulang!");
                dbConnect.conn.rollback();
                return;
            }
            String idSupplier = getSupplierIdFromComboBox();
            if (idSupplier == null) {
                showAlert("Error", "ID Supplier tidak valid!");
                dbConnect.conn.rollback();
                return;
            }
            String tanggalPembelian = tfTanggalPembelian.getText();
            double totalHarga = keranjangList.stream().mapToDouble(KeranjangObat::getSubTotal).sum();
            dbConnect.pstat.setString(1, idPengadaan);
            dbConnect.pstat.setString(2, idKaryawan);
            dbConnect.pstat.setString(3, idSupplier);
            dbConnect.pstat.setString(4, tanggalPembelian);
            dbConnect.pstat.setDouble(5, totalHarga);
            dbConnect.pstat.executeUpdate();
            String detailQuery = "INSERT INTO DetailTransaksiPengadaanObat (IDPengadaan, IDObat, Kuantitas, SubTotal) VALUES (?, ?, ?, ?)";
            for (KeranjangObat item : keranjangList) {
                String idObat = getObatIdByName(item.getNamaObat());
                if (idObat == null) {
                    showAlert("Error", "Obat dengan nama '" + item.getNamaObat() + "' tidak ditemukan!");
                    dbConnect.conn.rollback();
                    return;
                }
                dbConnect.pstat = dbConnect.conn.prepareStatement(detailQuery);
                dbConnect.pstat.setString(1, idPengadaan);
                dbConnect.pstat.setString(2, idObat); // Gunakan IDObat, bukan NamaObat
                dbConnect.pstat.setInt(3, item.getKuantitas());
                dbConnect.pstat.setDouble(4, item.getSubTotal());
                dbConnect.pstat.executeUpdate();
                Double satuan = item.getHargaSatuan();
                Double persenJual = (satuan * 10) / 100;
                Double jual = persenJual + satuan;
                String updateStockQuery = "UPDATE Obat SET Stok = Stok + ?, Status = 'Tersedia', HargaBeli = ?, HargaJual = ? WHERE IDObat = ?";
                dbConnect.pstat = dbConnect.conn.prepareStatement(updateStockQuery);
                dbConnect.pstat.setInt(1, item.getKuantitas());
                dbConnect.pstat.setDouble(2, satuan);
                dbConnect.pstat.setDouble(3, jual);
                dbConnect.pstat.setString(4, idObat); // Gunakan IDObat untuk update
                dbConnect.pstat.executeUpdate();
            }
            dbConnect.conn.commit();
            dbConnect.conn.setAutoCommit(true);
            loadDataFromDatabase();
            applyFilters();
            clearFormForNextEntry();
            showSuccessAlert("Pengadaan berhasil disimpan!");

        } catch (SQLException e) {
            try {
                dbConnect.conn.rollback();
                dbConnect.conn.setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            System.err.println("Error saving pengadaan: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging
            showAlert("Error", "Gagal menyimpan pengadaan: " + e.getMessage());
        }
    }

    private void clearFormForNextEntry() {
        keranjangList.clear();
        cbObat.setValue(null);
        tfHargaSatuan.setText("Rp. ");
        generateNewPengadaanId();
        resetFormPembayaran();
        updateTotalBelanja();
        cbSupplier.setDisable(false);
        if (tabelKeranjang != null) {
            tabelKeranjang.refresh();
        }
    }

    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            Stage currentStage = (Stage) cbObat.getScene().getWindow();
            alert.initOwner(currentStage);
            alert.initModality(Modality.WINDOW_MODAL);
        } catch (Exception e) {
            alert.initModality(Modality.APPLICATION_MODAL);
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String getObatIdByName(String namaObat) {
        String query = "SELECT IDObat FROM Obat WHERE NamaObat = ?";
        try (PreparedStatement pstat = dbConnect.conn.prepareStatement(query)) {
            pstat.setString(1, namaObat);
            try (ResultSet rs = pstat.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("IDObat");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting IDObat: " + e.getMessage());
        }
        return null;
    }

    private String getSupplierIdFromComboBox() {
        String idSupplier = null;
        try {
            Object selectedSupplier = cbSupplier.getValue();
            if (selectedSupplier != null) {
                if (selectedSupplier instanceof Supplier) {
                    idSupplier = ((Supplier) selectedSupplier).getIdSupplier();
                }
                else if (selectedSupplier instanceof String) {
                    String namaSupplier = (String) selectedSupplier;
                    String query = "SELECT IDSupplier FROM Supplier WHERE NamaSupplier = ?";
                    dbConnect.pstat = dbConnect.conn.prepareStatement(query);
                    dbConnect.pstat.setString(1, namaSupplier);
                    dbConnect.result = dbConnect.pstat.executeQuery();
                    if (dbConnect.result.next()) {
                        idSupplier = dbConnect.result.getString("IDSupplier");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting supplier ID: " + e.getMessage());
        }
        return idSupplier;
    }

    // 1. Method untuk tombol BATAL (🔄 Batal) - hanya membatalkan transaksi
    @FXML
    private void batalPengadaan() {
        // Cek apakah ada barang di keranjang
        if (!keranjangList.isEmpty()) {
            // Jika ada barang, tampilkan konfirmasi pembatalan
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Konfirmasi Pembatalan");
            alert.setHeaderText("Yakin ingin membatalkan transaksi?");
            alert.setContentText("Semua data di keranjang akan dihapus dan transaksi akan dibatalkan.");

            if (formPopup != null && formPopup.getScene() != null) {
                alert.initOwner(formPopup.getScene().getWindow());
            }
            alert.initModality(Modality.WINDOW_MODAL);

            ButtonType buttonYa = new ButtonType("Ok", ButtonBar.ButtonData.YES);
            ButtonType buttonTidak = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(buttonYa, buttonTidak);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonYa) {
                // Hanya reset keranjang dan form field, TIDAK menutup form
                resetFormFields();
                showAlert("Informasi", "Transaksi berhasil dibatalkan. Keranjang telah dikosongkan.", Alert.AlertType.INFORMATION);
            }
        } else {
            // Jika keranjang kosong, tampilkan pesan informasi
            showAlert("Informasi", "Tidak ada barang di keranjang untuk dibatalkan.", Alert.AlertType.INFORMATION);
        }
    }

    // 2. Method untuk tombol TUTUP FORM (✕) - menutup form
    @FXML
    private void tutupForm() {
        // Cek apakah ada barang di keranjang sebelum menutup form
        if (!keranjangList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Konfirmasi Penutupan");
            alert.setHeaderText("Yakin ingin menutup form?");
            alert.setContentText("Terdapat data di keranjang yang belum disimpan. Data akan hilang jika form ditutup.");

            if (formPopup != null && formPopup.getScene() != null) {
                alert.initOwner(formPopup.getScene().getWindow());
            }
            alert.initModality(Modality.WINDOW_MODAL);

            ButtonType buttonYa = new ButtonType("Ok", ButtonBar.ButtonData.YES);
            ButtonType buttonTidak = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(buttonYa, buttonTidak);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonYa) {
                tutupFormPengadaan();
            }
        } else {
            tutupFormPengadaan();
        }
    }

    private void resetFormFields() {
        keranjangList.clear();
        cbObat.setValue(null);
        tfKuantitas.setText("1");
        tfHargaSatuan.clear();
        tfJumlahUang.clear();
        tfKembalian.clear();
        updateTotalBelanja();
        resetFormPembayaran();
        cbSupplier.setDisable(false);
    }

    private void tutupFormPengadaan() {
        formPopup.setVisible(false);
        formPopup.setManaged(false);

        keranjangList.clear();
        tfIDPengadaan.clear();
        tfNamaKaryawan.clear();
        cbSupplier.setValue(null);
        tfTanggalPembelian.clear();
        cbObat.setValue(null);
        tfKuantitas.setText("1");
        tfHargaSatuan.clear();
        tfJumlahUang.clear();
        tfKembalian.clear();

        updateTotalBelanja();
        resetFormPembayaran();
        cbSupplier.setDisable(false);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (formPopup != null && formPopup.getScene() != null) {
            alert.initOwner(formPopup.getScene().getWindow());
        }
        alert.initModality(Modality.WINDOW_MODAL);
        alert.showAndWait();
    }

    private void showDetailModal(PengadaanObat pengadaan) {
        if (pengadaan == null || pengadaan.getIdPengadaan() == null) {
            showAlert("Error", "Data pengadaan tidak valid!");
            return;
        }
        if (dbConnect == null || dbConnect.conn == null) {
            showAlert("Error", "Database connection not available!");
            return;
        }

        lblDetailID.setText("ID Transaksi\t: " + pengadaan.getIdPengadaan());
        lblDetailKaryawan.setText("Karyawan\t: " + pengadaan.getNamaKaryawan());
        lblDetailSupplier.setText("Supplier\t: " + pengadaan.getNamaSupplier());
        lblDetailTanggal.setText("Tanggal\t: " + pengadaan.getTanggalPembelian());
        lblDetailTotal.setText(pengadaan.getTotalHargaFormatted());
        PreparedStatement pstat = null;
        ResultSet rs = null;
        try {
            String query = """
            SELECT d.IDPengadaan, d.IDObat, o.NamaObat, d.Kuantitas, d.SubTotal
            FROM DetailTransaksiPengadaanObat d 
            JOIN Obat o ON d.IDObat = o.IDObat 
            WHERE d.IDPengadaan = ?
            """;
            pstat = dbConnect.conn.prepareStatement(query);
            pstat.setString(1, pengadaan.getIdPengadaan());
            rs = pstat.executeQuery();
            ObservableList<DetailPengadaan> detailList = FXCollections.observableArrayList();
            while (rs.next()) {
                double subTotal = rs.getDouble("SubTotal");
                int kuantitas = rs.getInt("Kuantitas");
                double hargaSatuan = kuantitas > 0 ? subTotal / kuantitas : 0;
                DetailPengadaan detail = new DetailPengadaan(
                        rs.getString("IDPengadaan"),
                        rs.getString("NamaObat"),
                        kuantitas,
                        hargaSatuan,
                        subTotal
                );
                detailList.add(detail);
            }
            tabelDetail.setItems(detailList);
        } catch (SQLException e) {
            System.err.println("Error loading detail data: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal memuat detail pengadaan: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstat != null) pstat.close();
            } catch (SQLException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
        detailPopup.setVisible(true);
        detailPopup.setManaged(true);
    }

    @FXML
    private void tutupDetail() {
        detailPopup.setVisible(false);
        detailPopup.setManaged(false);
    }

    private int getKuantitas() {
        try {
            String text = tfKuantitas.getText().trim();
            if (text.isEmpty()) {
                return 1;
            }
            int value = Integer.parseInt(text);
            return value > 0 ? value : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        try {
            Stage currentStage = (Stage) cbObat.getScene().getWindow();
            alert.initOwner(currentStage);
            alert.initModality(Modality.WINDOW_MODAL);
        } catch (Exception e) {
            alert.initModality(Modality.APPLICATION_MODAL);
        }
        alert.showAndWait();
    }

    public static class PengadaanObat {
        private String idPengadaan;
        private String namaKaryawan;
        private String namaSupplier;
        private String tanggalPembelian;
        private double totalHarga;
        public PengadaanObat(String idPengadaan, String namaKaryawan, String namaSupplier,
                             String tanggalPembelian, double totalHarga) {
            this.idPengadaan = idPengadaan;
            this.namaKaryawan = namaKaryawan;
            this.namaSupplier = namaSupplier;
            this.tanggalPembelian = tanggalPembelian;
            this.totalHarga = totalHarga;
        }

        public String getIdPengadaan() { return idPengadaan; }
        public void setIdPengadaan(String idPengadaan) { this.idPengadaan = idPengadaan; }
        public String getNamaKaryawan() { return namaKaryawan; }
        public void setNamaKaryawan(String namaKaryawan) { this.namaKaryawan = namaKaryawan; }
        public String getNamaSupplier() { return namaSupplier; }
        public void setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }
        public String getTanggalPembelian() { return tanggalPembelian; }
        public void setTanggalPembelian(String tanggalPembelian) { this.tanggalPembelian = tanggalPembelian; }
        public double getTotalHarga() { return totalHarga; }
        public void setTotalHarga(double totalHarga) { this.totalHarga = totalHarga; }
        public String getTotalHargaFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(totalHarga);
        }
    }

    public static class KeranjangObat {
        private String namaObat;
        private int kuantitas;
        private double hargaSatuan;
        private double subTotal;
        public KeranjangObat(String namaObat, int kuantitas, double hargaSatuan, double subTotal) {
            this.namaObat = namaObat;
            this.kuantitas = kuantitas;
            this.hargaSatuan = hargaSatuan;
            this.subTotal = subTotal;
        }

        public String getNamaObat() { return namaObat; }
        public void setNamaObat(String namaObat) { this.namaObat = namaObat; }
        public int getKuantitas() {return kuantitas;}
        public void setKuantitas(int kuantitas) { this.kuantitas = kuantitas; }
        public double getHargaSatuan() { return hargaSatuan; }
        public void setHargaSatuan(double hargaSatuan) { this.hargaSatuan = hargaSatuan; }
        public double getSubTotal() { return subTotal; }
        public void setSubTotal(double subTotal) { this.subTotal = subTotal; }
        public String getHargaSatuanFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(hargaSatuan);
        }
        public String getSubTotalFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(subTotal);
        }
    }

    public static class DetailPengadaan {
        private String idPengadaan;
        private String namaObat;
        private int kuantitas;
        private double hargaSatuan;
        private double subTotal;

        public DetailPengadaan(String idPengadaan, String namaObat, int kuantitas, double hargaSatuan, double subTotal) {
            this.idPengadaan = idPengadaan;
            this.namaObat = namaObat;
            this.kuantitas = kuantitas;
            this.hargaSatuan = hargaSatuan;
            this.subTotal = subTotal;
        }

        public String getIdPengadaan() { return idPengadaan; }
        public void setIdPengadaan(String idPengadaan) { this.idPengadaan = idPengadaan; }
        public String getNamaObat() { return namaObat; }
        public void setNamaObat(String namaObat) { this.namaObat = namaObat; }
        public int getKuantitas() { return kuantitas; }
        public void setKuantitas(int kuantitas) { this.kuantitas = kuantitas; }
        public double getHargaSatuan() { return hargaSatuan; }
        public void setHargaSatuan(double hargaSatuan) { this.hargaSatuan = hargaSatuan; }
        public double getSubTotal() { return subTotal; }
        public void setSubTotal(double subTotal) { this.subTotal = subTotal; }
        public String getHargaSatuanFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(hargaSatuan);
        }
        public String getSubTotalFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(subTotal);
        }
    }

    public static class Supplier {
        private String idSupplier;
        private String namaSupplier;
        private String alamatSupplier;
        private String teleponSupplier;

        public Supplier(String idSupplier, String namaSupplier, String alamatSupplier, String teleponSupplier) {
            this.idSupplier = idSupplier;
            this.namaSupplier = namaSupplier;
            this.alamatSupplier = alamatSupplier;
            this.teleponSupplier = teleponSupplier;
        }

        public String getIdSupplier() { return idSupplier; }
        public void setIdSupplier(String idSupplier) { this.idSupplier = idSupplier; }
        public String getNamaSupplier() { return namaSupplier; }
        public void setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }
        public String getAlamatSupplier() { return alamatSupplier; }
        public void setAlamatSupplier(String alamatSupplier) { this.alamatSupplier = alamatSupplier; }
        public String getTeleponSupplier() { return teleponSupplier; }
        public void setTeleponSupplier(String teleponSupplier) { this.teleponSupplier = teleponSupplier; }

        @Override
        public String toString() {
            return namaSupplier;
        }
    }

    public static class Obat {
        private String idObat;
        private String namaObat;
        private double hargaObat;
        private int stokObat;
        private String statusObat;

        public Obat(String idObat, String namaObat, double hargaObat, int stokObat, String statusObat) {
            this.idObat = idObat;
            this.namaObat = namaObat;
            this.hargaObat = hargaObat;
            this.stokObat = stokObat;
            this.statusObat = statusObat;
        }

        public String getIdObat() { return idObat; }
        public void setIdObat(String idObat) { this.idObat = idObat; }
        public String getNamaObat() { return namaObat; }
        public void setNamaObat(String namaObat) { this.namaObat = namaObat; }
        public double getHargaObat() { return hargaObat; }
        public void setHargaObat(double hargaObat) { this.hargaObat = hargaObat; }
        public int getStokObat() { return stokObat; }
        public void setStokObat(int stokObat) { this.stokObat = stokObat; }
        public String getStatusObat() { return statusObat; }
        public void setStatusObat(String statusObat) { this.statusObat = statusObat; }
        public String getHargaObatFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(hargaObat);
        }

        @Override
        public String toString() {
            return namaObat + " - " + getHargaObatFormatted();
        }
    }

    private void setupJumlahUangListener() {
        tfJumlahUang.setText("Rp. ");
        tfJumlahUang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.startsWith("Rp. ")) {
                tfJumlahUang.setText("Rp. " + newValue.replaceFirst("^Rp\\. ?", ""));
                return;
            }
            String numberPart = newValue.substring(4); // Hapus "Rp. "
            String formatted = formatRupiah(numberPart);
            if (!numberPart.equals(formatted)) {
                tfJumlahUang.setText("Rp. " + formatted);
            }
            hitungKembalian();
        });
        tfJumlahUang.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Platform.runLater(() -> {
                    if (tfJumlahUang.getCaretPosition() < 4) {
                        tfJumlahUang.positionCaret(tfJumlahUang.getText().length());
                    }
                });
            }
        });
        tfJumlahUang.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() < 4) {
                Platform.runLater(() -> tfJumlahUang.positionCaret(Math.max(4, tfJumlahUang.getText().length())));
            }
        });
    }

    private boolean validasiPembayaran() {
        try {
            String jumlahUangText = tfJumlahUang.getText().trim();
            if (jumlahUangText.equals("Rp. ") || jumlahUangText.isEmpty()) {
                showAlert("Error", "Jumlah uang belum diisi!");
                return false;
            }
            double jumlahUang = parseRupiah(jumlahUangText);
            double totalBelanja = keranjangList.stream().mapToDouble(KeranjangObat::getSubTotal).sum();
            if (jumlahUang < totalBelanja) {
                showAlert("Error", "Jumlah uang tidak mencukupi!");
                return false;
            }
            return true;
        } catch (Exception e) {
            showAlert("Error", "Format jumlah uang tidak valid!");
            return false;
        }
    }

    @FXML
    private void refreshData() {
        dpTanggalFilter.setValue(null);
        searchField.clear();
        loadDataFromDatabase();
    }

    private void resetFormPembayaran() {
        tfJumlahUang.setText("Rp. ");
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        tfKembalian.setText(formatter.format(0));
    }
}