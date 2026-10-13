package Apoteker.Controller;

import Admin.Obat;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import java.net.URL;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardPenjualanObatController implements Initializable {

    @FXML private TableView<PenjualanObat> tabelPenjualan;
    @FXML private TableColumn<PenjualanObat, String> colIDPenjualan;
    @FXML private TableColumn<PenjualanObat, String> colNamaKaryawan;
    @FXML private TableColumn<PenjualanObat, String> colTanggalPenjualan;
    @FXML private TableColumn<PenjualanObat, String> colTotalHarga;
    @FXML private TableColumn<PenjualanObat, Void> colAksi;
    @FXML private TextField searchField;
    @FXML private DatePicker dpTanggalFilter;
    @FXML private VBox formPopup;
    @FXML private TextField tfIDPenjualan;
    @FXML private TextField tfNamaKaryawan;
    @FXML private ComboBox<Obat> cbObat;
    @FXML private TextField tfTanggalPenjualan;
    @FXML private TextField tfSisaStok;
    @FXML private TextField tfHargaSatuan;
    @FXML private Spinner<Integer> spKuantitas;
    @FXML private TableView<ItemKeranjang> tabelKeranjang;
    @FXML private TableColumn<ItemKeranjang, String> colNamaObat;
    @FXML private TableColumn<ItemKeranjang, Integer> colKuantitas;
    @FXML private TableColumn<ItemKeranjang, String> colHargaSatuan;
    @FXML private TableColumn<ItemKeranjang, String> colSubTotal;
    @FXML private TableColumn<ItemKeranjang, Void> colHapus;
    @FXML private TextField tfJumlahUang;
    @FXML private TextField tfKembalian;
    @FXML private Label lblTotalBelanja;
    @FXML private VBox detailPopup;
    @FXML private Label lblDetailID;
    @FXML private Label lblDetailKaryawan;
    @FXML private Label lblDetailTotal;
    @FXML private TableView<ItemDetail> tabelDetail;
    @FXML private TableColumn<ItemDetail, String> colDetailNamaObat;
    @FXML private TableColumn<ItemDetail, Integer> colDetailKuantitas;
    @FXML private TableColumn<ItemDetail, String> colDetailHargaSatuan;
    @FXML private TableColumn<ItemDetail, String> colDetailSubTotal;
    @FXML private Label lblDetailJumlahItem;
    @FXML private Label lblDetailTanggal;
    @FXML private Label lblDetailTotalItem;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID")); // Tambahkan ini
    private ObservableList<PenjualanObat> dataPenjualan = FXCollections.observableArrayList();
    private ObservableList<PenjualanObat> dataFiltered = FXCollections.observableArrayList();
    private ObservableList<Obat> dataObat = FXCollections.observableArrayList();
    private ObservableList<ItemKeranjang> dataKeranjang = FXCollections.observableArrayList();
    private ObservableList<ItemDetail> dataDetail = FXCollections.observableArrayList();
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private String currentUser = SessionManager.getInstance().getDisplayName();
    private String currentUserId = SessionManager.getInstance().getLoggedInUserId();
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=HiDocProject;encrypt=true;trustServerCertificate=true";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "123";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.getInstance().isLoggedIn()) {
            showAlert("Error", "Session tidak valid. Silakan login kembali.");
            return;
        }
        currentUser = SessionManager.getInstance().getDisplayName();
        currentUserId = SessionManager.getInstance().getLoggedInUserId();
        testDatabaseConnection();
        setupTableColumns();
        setupFormComponents();
        setupEventHandlers();
        loadData();
        loadObatData();
        setupDefaultValues();
    }

    private void setupTableColumns() {
        colIDPenjualan.setCellValueFactory(new PropertyValueFactory<>("idPenjualan"));
        colNamaKaryawan.setCellValueFactory(new PropertyValueFactory<>("namaKaryawan"));
        colTanggalPenjualan.setCellValueFactory(new PropertyValueFactory<>("tanggalPenjualan"));
        colTotalHarga.setCellValueFactory(new PropertyValueFactory<>("totalHargaFormatted"));
        colAksi.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetail = new Button("👁 Detail");
            {
                btnDetail.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 8;");
                btnDetail.setOnAction(e -> {
                    PenjualanObat penjualan = getTableView().getItems().get(getIndex());
                    tampilkanDetail(penjualan);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new javafx.scene.layout.HBox(5, btnDetail));
                }
            }
        });
        colNamaObat.setCellValueFactory(new PropertyValueFactory<>("namaObat"));
        colKuantitas.setCellValueFactory(new PropertyValueFactory<>("kuantitas"));
        colHargaSatuan.setCellValueFactory(new PropertyValueFactory<>("hargaSatuanFormatted"));
        colSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotalFormatted"));
        colHapus.setCellFactory(param -> new TableCell<>() {
            private final Button btnHapus = new Button("🗑");
            {
                btnHapus.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand;");
                btnHapus.setOnAction(e -> {
                    ItemKeranjang item = getTableView().getItems().get(getIndex());
                    hapusDariKeranjang(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnHapus);
                }
            }
        });
        colDetailNamaObat.setCellValueFactory(new PropertyValueFactory<>("namaObat"));
        colDetailKuantitas.setCellValueFactory(new PropertyValueFactory<>("kuantitas"));
        colDetailHargaSatuan.setCellValueFactory(new PropertyValueFactory<>("hargaSatuanFormatted"));
        colDetailSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotalFormatted"));
    }

    private void setupFormComponents() {
        spKuantitas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1));
        spKuantitas.setEditable(true);
        cbObat.setPromptText("Pilih Obat");
        cbObat.setConverter(new StringConverter<Obat>() {
            @Override
            public String toString(Obat obat) {
                if (obat != null) {
                    return obat.getNamaObat();
                }
                return "";
            }
            @Override
            public Obat fromString(String string) {
                return null;
            }
        });

        tabelPenjualan.setItems(dataFiltered);
        tabelKeranjang.setItems(dataKeranjang);
        tabelDetail.setItems(dataDetail);
        cbObat.setItems(dataObat);
        setupCurrencyInputField();
    }

    private void setupEventHandlers() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterData());
        dpTanggalFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterData());
        cbObat.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateObatInfo(newValue);
            }
        });
        tfJumlahUang.textProperty().addListener((observable, oldValue, newValue) -> hitungKembalian());
        spKuantitas.valueProperty().addListener((observable, oldValue, newValue) -> {
        });
    }

    private void setupDefaultValues() {
        tfIDPenjualan.setText("Auto Generate");
        tfIDPenjualan.setEditable(false);
        tfNamaKaryawan.setText(SessionManager.getInstance().getDisplayName());
        tfNamaKaryawan.setEditable(false);
        tfTanggalPenjualan.setText(LocalDate.now().format(dateFormatter));
        tfTanggalPenjualan.setEditable(false);
        tfKembalian.setText("Rp 0");
        lblTotalBelanja.setText("Rp 0");
    }

    @FXML
    private void refreshData() {
        loadData();
        loadObatData();
    }

    private boolean validateSession() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            showAlert("Session Error", "Session tidak valid. Silakan login kembali.");
            return false;
        }
        return true;
    }

    @FXML
    private void tampilkanFormPenjualan() {
        if (!validateSession()) return;
        resetForm();
        tfIDPenjualan.setText(generateIDPenjualan());
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    @FXML
    private void tambahKeKeranjang() {
        Obat selectedObat = cbObat.getSelectionModel().getSelectedItem();
        if (selectedObat == null) {
            showAlert("Peringatan", "Silakan pilih obat terlebih dahulu!");
            return;
        }

        int kuantitas = spKuantitas.getValue();
        if (kuantitas <= 0) {
            showAlert("Peringatan", "Kuantitas harus lebih dari 0!");
            return;
        }
        int totalKuantitasInCart = 0;
        for (ItemKeranjang item : dataKeranjang) {
            if (item.getIdObat().equals(selectedObat.getIdObat())) {
                totalKuantitasInCart = item.getKuantitas();
                break;
            }
        }

        if ((totalKuantitasInCart + kuantitas) > selectedObat.getStok()) {
            showAlert("Peringatan", "Kuantitas melebihi stok yang tersedia!\n" +
                    "Stok tersedia: " + selectedObat.getStok() + "\n" +
                    "Sudah di keranjang: " + totalKuantitasInCart + "\n" +
                    "Maksimal dapat ditambah: " + (selectedObat.getStok() - totalKuantitasInCart));
            return;
        }
        boolean itemFound = false;
        for (ItemKeranjang item : dataKeranjang) {
            if (item.getIdObat().equals(selectedObat.getIdObat())) {
                item.setKuantitas(item.getKuantitas() + kuantitas);
                item.updateSubTotal();
                tabelKeranjang.refresh();
                updateTotalBelanja();
                itemFound = true;
                break;
            }
        }

        if (!itemFound) {
            ItemKeranjang newItem = new ItemKeranjang(
                    selectedObat.getIdObat(),
                    selectedObat.getNamaObat(),
                    kuantitas,
                    selectedObat.getHargaJual()
            );
            dataKeranjang.add(newItem);
            updateTotalBelanja();
        }

        spKuantitas.getValueFactory().setValue(1);
    }

    @FXML
    private void simpanPenjualan() {
        if (dataKeranjang.isEmpty()) {
            showAlert("Peringatan", "Keranjang belanja kosong!");
            return;
        }
        double jumlahUang = getCurrencyValue(tfJumlahUang.getText());
        if (jumlahUang <= 0) {
            showAlert("Peringatan", "Masukkan jumlah uang pembayaran!");
            return;
        }
        double totalBelanja = getTotalBelanja();
        if (jumlahUang < totalBelanja) {
            showAlert("Peringatan", "Jumlah uang tidak mencukupi!");
            return;
        }
        if (simpanKeDatabase()) {
            showAlert("Sukses", "Transaksi berhasil disimpan!");
            resetFormForNewTransaction();
            loadData();
            tfIDPenjualan.setText(generateIDPenjualan());
        }
    }

    private void resetFormForNewTransaction() {
        dataKeranjang.clear();
        cbObat.getSelectionModel().clearSelection();
        tfSisaStok.clear();
        tfHargaSatuan.clear();
        tfJumlahUang.setText("Rp. ");
        tfKembalian.setText("Rp 0");
        lblTotalBelanja.setText("Rp 0");
        spKuantitas.getValueFactory().setValue(1);
        loadObatData();
        Platform.runLater(() -> cbObat.requestFocus());
    }

    @FXML
    private void batalPenjualan() {
        if (!dataKeranjang.isEmpty()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Konfirmasi Batal Transaksi");
            confirmAlert.setHeaderText("Apakah yakin membatalkan transaksi?");
            confirmAlert.setContentText("Semua barang di keranjang akan dihapus.");
            confirmAlert.initOwner(tabelPenjualan.getScene().getWindow());
            confirmAlert.initModality(Modality.WINDOW_MODAL);

            ButtonType buttonYes = new ButtonType("Ok", ButtonBar.ButtonData.YES);
            ButtonType buttonNo = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
            confirmAlert.getButtonTypes().setAll(buttonYes, buttonNo);

            Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == buttonYes) {
                resetForm();
                showAlert("Informasi", "Transaksi telah dibatalkan. Keranjang kosong.", Alert.AlertType.INFORMATION);
            }
        } else {
            showAlert("Informasi", "Tidak ada barang di keranjang untuk dibatalkan.", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void tutupForm() {
        if (!dataKeranjang.isEmpty()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Konfirmasi Tutup Form");
            confirmAlert.setHeaderText("Masih ada barang di keranjang!");
            confirmAlert.setContentText("Apakah yakin ingin menutup form? Data yang belum disimpan akan hilang.");
            confirmAlert.initOwner(tabelPenjualan.getScene().getWindow());
            confirmAlert.initModality(Modality.WINDOW_MODAL);

            ButtonType buttonYes = new ButtonType("Ok", ButtonBar.ButtonData.YES);
            ButtonType buttonNo = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
            confirmAlert.getButtonTypes().setAll(buttonYes, buttonNo);

            Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == buttonYes) {
                formPopup.setVisible(false);
                formPopup.setManaged(false);
                resetForm();
            }
        } else {
            formPopup.setVisible(false);
            formPopup.setManaged(false);
            resetForm();
        }
    }



    private void showSuccessAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.setGraphic(null);
            alert.initOwner(tabelPenjualan.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.show();
        });
    }

    @FXML
    private void tutupDetail() {
        detailPopup.setVisible(false);
        detailPopup.setManaged(false);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void loadData() {
        dataPenjualan.clear();
        if (!validateSession()) return;
        String query = """
        SELECT tp.IDPenjualan, tp.TanggalPenjualan, tp.TotalHarga, 
               COALESCE(k.NamaKaryawan, 'Unknown') as NamaKaryawan
        FROM TransaksiPenjualanObat tp
        LEFT JOIN Karyawan k ON tp.IDKaryawan = k.IDKaryawan
        ORDER BY tp.TanggalPenjualan DESC
    """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String tanggalOriginal = rs.getTimestamp("TanggalPenjualan").toLocalDateTime().format(dateFormatter);
                String tanggalDisplay = convertToDisplayFormat(tanggalOriginal);
                PenjualanObat penjualan = new PenjualanObat(
                        rs.getString("IDPenjualan"),
                        rs.getString("NamaKaryawan"),
                        tanggalDisplay,
                        rs.getDouble("TotalHarga")
                );
                dataPenjualan.add(penjualan);
            }
            dataFiltered.setAll(dataPenjualan);
        } catch (SQLException e) {
            logError("Error loading data", e);
            showAlert("Error", "Gagal memuat data: " + e.getMessage());
        }
    }

    private void loadObatData() {
        dataObat.clear();
        String query = """
    SELECT IDObat, NamaObat, HargaBeli, HargaJual, Stok, IDKategori, Status
    FROM Obat 
    WHERE Stok > 0 AND Status = 'Tersedia'
    ORDER BY NamaObat ASC
    """;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Obat obat = new Obat(
                        rs.getString("IDObat"),
                        rs.getString("NamaObat"),
                        rs.getDouble("HargaBeli"),
                        rs.getDouble("HargaJual"),
                        rs.getInt("Stok"),
                        rs.getString("IDKategori"),
                        rs.getString("Status"),
                        "General"
                );
                dataObat.add(obat);
            }
            cbObat.setItems(null);
            cbObat.setItems(dataObat);
            System.out.println("Loaded " + dataObat.size() + " obat yang tersedia");
        } catch (SQLException e) {
            System.err.println("Error loading obat data: " + e.getMessage());
            showAlert("Error", "Gagal memuat data obat: " + e.getMessage());
        }
    }

    private void updateStatusObatIfStockAvailable(String idObat) {
        String queryCheckAndUpdate = """
    UPDATE Obat 
    SET Status = 'Tersedia' 
    WHERE IDObat = ? AND Stok > 0 AND Status = 'Tidak Tersedia'
    """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryCheckAndUpdate)) {
            stmt.setString(1, idObat);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("Status obat " + idObat + " telah diubah kembali menjadi 'Tersedia'");
            }
        } catch (SQLException e) {
            logError("Error updating obat status to available", e);
        }
    }

    private void testDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connection successful");
            String[] testTables = {"Obat", "TransaksiPenjualanObat", "DetailTransaksiPenjualanObat", "Karyawan"};
            for (String table : testTables) {
                try {
                    String testQuery = "SELECT COUNT(*) as total FROM " + table;
                    try (PreparedStatement stmt = conn.prepareStatement(testQuery);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("Table " + table + " exists with " + rs.getInt("total") + " records");
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Table " + table + " not found or error: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterData() {
        dataFiltered.clear();
        String searchText = searchField.getText().toLowerCase();
        LocalDate filterDate = dpTanggalFilter.getValue();
        for (PenjualanObat penjualan : dataPenjualan) {
            boolean matchSearch = searchText.isEmpty() ||
                    penjualan.getIdPenjualan().toLowerCase().contains(searchText) ||
                    penjualan.getNamaKaryawan().toLowerCase().contains(searchText);
            boolean matchDate = filterDate == null ||
                    penjualan.getTanggalPenjualan().equals(filterDate.format(dateFormatter));
            if (matchSearch && matchDate) {
                dataFiltered.add(penjualan);
            }
        }
    }

    private void updateObatInfo(Obat obat) {
        tfSisaStok.setText(String.valueOf(obat.getStok()));
        tfHargaSatuan.setText(currencyFormat.format(obat.getHargaJual()));
        spKuantitas.getValueFactory().setValue(1);
    }

    private void updateTotalBelanja() {
        double total = getTotalBelanja();
        lblTotalBelanja.setText(currencyFormat.format(total));
        hitungKembalian();
    }

    private double getTotalBelanja() {
        return dataKeranjang.stream()
                .mapToDouble(ItemKeranjang::getSubTotal)
                .sum();
    }

    private void hapusDariKeranjang(ItemKeranjang item) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Hapus");
        confirmAlert.setHeaderText("Hapus Obat dari Keranjang");
        confirmAlert.setContentText("Apakah Anda yakin ingin menghapus " + item.getNamaObat() + " dari keranjang?");
        confirmAlert.initOwner(tabelPenjualan.getScene().getWindow());
        confirmAlert.initModality(Modality.WINDOW_MODAL);
        ButtonType buttonYes = new ButtonType("Ya", ButtonBar.ButtonData.YES);
        ButtonType buttonNo = new ButtonType("Tidak", ButtonBar.ButtonData.NO);
        confirmAlert.getButtonTypes().setAll(buttonYes, buttonNo);
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == buttonYes) {
            dataKeranjang.remove(item);
            updateTotalBelanja();
            showSuccessAlert("Sukses", "Obat berhasil dihapus dari keranjang");
        }
    }

    private void tampilkanDetail(PenjualanObat penjualan) {
        lblDetailID.setText("ID Penjualan\t: " + penjualan.getIdPenjualan());
        lblDetailKaryawan.setText("Karyawan\t: " + penjualan.getNamaKaryawan());
        lblDetailTotal.setText(" " + penjualan.getTotalHargaFormatted());

        loadDetailPenjualan(penjualan.getIdPenjualan());
        if (lblDetailJumlahItem != null) {
            int jumlahItem = dataDetail.stream().mapToInt(ItemDetail::getKuantitas).sum();
            lblDetailJumlahItem.setText("Jumlah Item: " + jumlahItem);
        }
        detailPopup.setVisible(true);
        detailPopup.setManaged(true);
    }

    private void loadDetailPenjualan(String idPenjualan) {
        dataDetail.clear();

        // Ambil data tanggal penjualan dan nama karyawan
        String queryHeader = """
    SELECT T.TanggalPenjualan, K.NamaKaryawan
    FROM TransaksiPenjualanObat T
    JOIN Karyawan K ON T.IDKaryawan = K.IDKaryawan
    WHERE T.IDPenjualan = ?
""";

        try (Connection conn = getConnection();
             PreparedStatement stmtHeader = conn.prepareStatement(queryHeader)) {

            stmtHeader.setString(1, idPenjualan);
            ResultSet rsHeader = stmtHeader.executeQuery();
            if (rsHeader.next()) {
                LocalDate tanggal = rsHeader.getDate("TanggalPenjualan").toLocalDate();
                String namaKaryawan = rsHeader.getString("NamaKaryawan");

                // Format: 05 Juli 2025
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));
                String tanggalFormatted = tanggal.format(formatter);

                lblDetailID.setText("ID Penjualan\t: " + idPenjualan);
                lblDetailKaryawan.setText("Karyawan\t: " + namaKaryawan);
                lblDetailTanggal.setText("Tanggal\t\t: " + tanggalFormatted);
            }

        } catch (SQLException e) {
            logError("Gagal load header penjualan", e);
        }

        String queryDetail = """
    SELECT dt.Kuantitas, dt.SubTotal, o.NamaObat, o.HargaJual as HargaSatuan
    FROM DetailTransaksiPenjualanObat dt
    JOIN Obat o ON dt.IDObat = o.IDObat
    WHERE dt.IDPenjualan = ?
    ORDER BY o.NamaObat ASC
""";

        double totalHarga = 0;
        int jumlahJenisObat = 0;  // Hitung jumlah jenis obat (bukan total kuantitas)

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryDetail)) {

            stmt.setString(1, idPenjualan);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int qty = rs.getInt("Kuantitas");
                double harga = rs.getDouble("HargaSatuan");
                double subtotal = rs.getDouble("SubTotal");

                ItemDetail detail = new ItemDetail(
                        rs.getString("NamaObat"),
                        qty,
                        harga,
                        subtotal
                );
                dataDetail.add(detail);

                jumlahJenisObat++;  // Increment untuk setiap jenis obat
                totalHarga += subtotal;
            }

            tabelDetail.setItems(dataDetail);

            // PERBAIKAN: Gunakan lblDetailTotalItem bukan lblDetailJumlahItem
            lblDetailTotalItem.setText("Jumlah Item\t: " + jumlahJenisObat);
            lblDetailTotal.setText(" " + formatRupiah(totalHarga));

        } catch (SQLException e) {
            logError("Error loading detail penjualan", e);
            showAlert("Error", "Gagal memuat detail penjualan: " + e.getMessage());
        }
    }

    private String formatRupiah(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return format.format(amount);
    }

    private boolean simpanKeDatabase() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            conn.setAutoCommit(false);
            String idPenjualan = generateIDPenjualan();
            String queryPenjualan = """
        INSERT INTO TransaksiPenjualanObat (IDPenjualan, IDKaryawan, TanggalPenjualan, TotalHarga)
        VALUES (?, ?, ?, ?)
        """;
            PreparedStatement stmtPenjualan = conn.prepareStatement(queryPenjualan);
            stmtPenjualan.setString(1, idPenjualan);
            stmtPenjualan.setString(2, getCurrentUserId());
            stmtPenjualan.setTimestamp(3, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            stmtPenjualan.setDouble(4, getTotalBelanja());
            stmtPenjualan.executeUpdate();
            String queryDetail = """
        INSERT INTO DetailTransaksiPenjualanObat (IDPenjualan, IDObat, Kuantitas, SubTotal)
        VALUES (?, ?, ?, ?)
        """;
            PreparedStatement stmtDetail = conn.prepareStatement(queryDetail);
            for (ItemKeranjang item : dataKeranjang) {
                stmtDetail.setString(1, idPenjualan);
                stmtDetail.setString(2, item.getIdObat());
                stmtDetail.setInt(3, item.getKuantitas());
                stmtDetail.setDouble(4, item.getSubTotal());
                stmtDetail.executeUpdate();
                String queryUpdateStok = "UPDATE Obat SET Stok = Stok - ? WHERE IDObat = ?";
                PreparedStatement stmtUpdateStok = conn.prepareStatement(queryUpdateStok);
                stmtUpdateStok.setInt(1, item.getKuantitas());
                stmtUpdateStok.setString(2, item.getIdObat());
                stmtUpdateStok.executeUpdate();
                updateStatusObatIfStockEmpty(conn, item.getIdObat());
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackEx) {
                logError("Rollback error", rollbackEx);
            }
            showAlert("Error", "Gagal menyimpan data: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                logError("Connection close error", e);
            }
        }
    }

    private void updateStatusObatIfStockEmpty(Connection conn, String idObat) throws SQLException {
        String queryCheckStock = "SELECT Stok FROM Obat WHERE IDObat = ?";
        try (PreparedStatement stmtCheck = conn.prepareStatement(queryCheckStock)) {
            stmtCheck.setString(1, idObat);
            ResultSet rs = stmtCheck.executeQuery();
            if (rs.next()) {
                int currentStock = rs.getInt("Stok");
                if (currentStock <= 0) {
                    String queryUpdateStatus = "UPDATE Obat SET Status = 'Tidak Tersedia' WHERE IDObat = ?";
                    try (PreparedStatement stmtUpdateStatus = conn.prepareStatement(queryUpdateStatus)) {
                        stmtUpdateStatus.setString(1, idObat);
                        int updated = stmtUpdateStatus.executeUpdate();
                        if (updated > 0) {
                            System.out.println("Status obat " + idObat + " telah diubah menjadi 'Tidak Tersedia' karena stok habis");
                        }
                    }
                }
            }
        }
    }

    private String generateIDPenjualan() {
        String query = "SELECT TOP 1 IDPenjualan FROM TransaksiPenjualanObat ORDER BY IDPenjualan DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String lastId = rs.getString("IDPenjualan");
                if (lastId.startsWith("TRPJ")) {
                    int number = Integer.parseInt(lastId.substring(4)) + 1;
                    return String.format("TRPJ%03d", number);
                } else if (lastId.startsWith("PJ")) {
                    int number = Integer.parseInt(lastId.substring(2)) + 1;
                    return String.format("TRPJ%03d", number);
                }
            }
            return "TRPJ001";
        } catch (SQLException | NumberFormatException e) {
            logError("Error generating ID", e);
            return "TRPJ" + System.currentTimeMillis() % 10000;
        }
    }

    private void resetForm() {
        dataKeranjang.clear();
        cbObat.getSelectionModel().clearSelection();
        tfSisaStok.clear();
        tfHargaSatuan.clear();
        tfJumlahUang.setText("Rp. ");
        tfKembalian.setText("Rp 0");
        lblTotalBelanja.setText("Rp 0");
        spKuantitas.getValueFactory().setValue(1);
        tfIDPenjualan.setText("Auto Generate");
    }

    private void showAlert(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(tabelPenjualan.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
            alert.show();
        });
    }

    private String getCurrentUserId() {
        String sessionUserId = SessionManager.getInstance().getLoggedInUserId();
        if (sessionUserId != null && !sessionUserId.isEmpty()) {
            return sessionUserId;
        }
        String query = "SELECT IDKaryawan FROM Karyawan WHERE NamaKaryawan = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, SessionManager.getInstance().getDisplayName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("IDKaryawan");
            }
        } catch (SQLException e) {
            logError("Error getting user ID", e);
        }
        return "KRY001";
    }

    private void logError(String message, Exception e) {
        System.err.println("Error: " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    public static class PenjualanObat {
        private final SimpleStringProperty idPenjualan;
        private final SimpleStringProperty namaKaryawan;
        private final SimpleStringProperty tanggalPenjualan;
        private final double totalHarga;

        public PenjualanObat(String idPenjualan, String namaKaryawan, String tanggalPenjualan, double totalHarga) {
            this.idPenjualan = new SimpleStringProperty(idPenjualan);
            this.namaKaryawan = new SimpleStringProperty(namaKaryawan);
            this.tanggalPenjualan = new SimpleStringProperty(tanggalPenjualan);
            this.totalHarga = totalHarga;
        }

        private String convertDateFormat(String originalDate) {
            try {
                LocalDate date = LocalDate.parse(originalDate, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID")));
            } catch (Exception e) {
                return originalDate;
            }
        }

        public String getIdPenjualan() { return idPenjualan.get(); }
        public String getNamaKaryawan() { return namaKaryawan.get(); }
        public String getTanggalPenjualan() { return tanggalPenjualan.get(); }
        public double getTotalHarga() { return totalHarga; }
        public String getTotalHargaFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(totalHarga);
        }
    }

    public static class ItemKeranjang {
        private final String idObat;
        private final SimpleStringProperty namaObat;
        private int kuantitas;
        private final double hargaSatuan;
        private double subTotal;
        public ItemKeranjang(String idObat, String namaObat, int kuantitas, double hargaSatuan) {
            this.idObat = idObat;
            this.namaObat = new SimpleStringProperty(namaObat);
            this.kuantitas = kuantitas;
            this.hargaSatuan = hargaSatuan;
            this.subTotal = kuantitas * hargaSatuan;
        }
        public String getIdObat() { return idObat; }
        public String getNamaObat() { return namaObat.get(); }
        public int getKuantitas() { return kuantitas; }
        public void setKuantitas(int kuantitas) {
            this.kuantitas = kuantitas;
            updateSubTotal();
        }
        public double getHargaSatuan() { return hargaSatuan; }
        public double getSubTotal() { return subTotal; }
        public String getHargaSatuanFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(hargaSatuan);
        }
        public String getSubTotalFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(subTotal);
        }
        public void updateSubTotal() {
            this.subTotal = this.kuantitas * this.hargaSatuan;
        }
    }

    public static class ItemDetail {
        private final SimpleStringProperty namaObat;
        private final int kuantitas;
        private final double hargaSatuan;
        private final double subTotal;
        public ItemDetail(String namaObat, int kuantitas, double hargaSatuan, double subTotal) {
            this.namaObat = new SimpleStringProperty(namaObat);
            this.kuantitas = kuantitas;
            this.hargaSatuan = hargaSatuan;
            this.subTotal = subTotal;
        }
        public String getNamaObat() { return namaObat.get(); }
        public int getKuantitas() { return kuantitas; }
        public double getHargaSatuan() { return hargaSatuan; }
        public double getSubTotal() { return subTotal; }
        public String getHargaSatuanFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(hargaSatuan);
        }
        public String getSubTotalFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(subTotal);
        }
    }

    private void setupCurrencyInputField() {
        tfJumlahUang.setText("Rp. ");
        tfJumlahUang.textProperty().addListener((observable, oldValue, newValue) -> {
            formatCurrencyInput(tfJumlahUang, oldValue, newValue);
        });
        tfJumlahUang.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { // When focused
                Platform.runLater(() -> {
                    if (tfJumlahUang.getText().length() <= 4) {
                        tfJumlahUang.positionCaret(tfJumlahUang.getText().length());
                    }
                });
            }
        });
        tfJumlahUang.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE ||
                    event.getCode() == KeyCode.DELETE) {
                if (tfJumlahUang.getCaretPosition() <= 4) {
                    event.consume();
                }
            }
        });
        tfJumlahUang.setOnMouseClicked(event -> {
            if (tfJumlahUang.getCaretPosition() < 4) {
                tfJumlahUang.positionCaret(4);
            }
        });
    }

    private void formatCurrencyInput(TextField textField, String oldValue, String newValue) {
        if (!newValue.startsWith("Rp. ")) {
            Platform.runLater(() -> {
                textField.setText("Rp. ");
                textField.positionCaret(4);
            });
            return;
        }
        String numericPart = newValue.substring(4);
        String digitsOnly = numericPart.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            if (!newValue.equals("Rp. ")) {
                Platform.runLater(() -> {
                    textField.setText("Rp. ");
                    textField.positionCaret(4);
                });
            }
            return;
        }
        String formattedNumber = formatWithThousandSeparator(digitsOnly);
        String formattedText = "Rp. " + formattedNumber;
        if (!newValue.equals(formattedText)) {
            Platform.runLater(() -> {
                int caretPos = textField.getCaretPosition();
                int oldLength = oldValue.length();
                int newLength = formattedText.length();
                textField.setText(formattedText);
                int newCaretPos = caretPos + (newLength - oldLength);
                newCaretPos = Math.max(4, Math.min(newCaretPos, formattedText.length()));
                textField.positionCaret(newCaretPos);
            });
        }
    }

    private String formatWithThousandSeparator(String number) {
        if (number.isEmpty()) return "";
        try {
            long value = Long.parseLong(number);
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id", "ID"));
            return nf.format(value);
        } catch (NumberFormatException e) {
            return number;
        }
    }

    private double getCurrencyValue(String currencyText) {
        if (currencyText == null || currencyText.length() <= 4) {
            return 0;
        }
        String numericPart = currencyText.substring(4).replaceAll("\\.", "");
        try {
            return Double.parseDouble(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void hitungKembalian() {
        try {
            String jumlahUangText = tfJumlahUang.getText().trim();
            if (jumlahUangText.equals("Rp. ") || jumlahUangText.isEmpty()) {
                tfKembalian.setText(currencyFormat.format(0));
                return;
            }
            double jumlahUang = getCurrencyValue(tfJumlahUang.getText());
            double totalBelanja = getTotalBelanja();
            double kembalian = jumlahUang - totalBelanja;
            if (kembalian < 0) {
                tfKembalian.setText("Kurang: " + currencyFormat.format(Math.abs(kembalian)));
                tfKembalian.setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); -fx-border-color: #e57373; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #c62828;");
            } else {
                tfKembalian.setText(currencyFormat.format(kembalian));
                tfKembalian.setStyle("-fx-background-color: linear-gradient(to bottom, #e8f5e8, #c8e6c8); -fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #2e7d32;");
            }
        } catch (Exception e) {
            tfKembalian.setText("Format tidak valid");
            tfKembalian.setStyle("-fx-background-color: linear-gradient(to bottom, #ffebee, #ffcdd2); -fx-border-color: #e57373; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 14px; -fx-text-fill: #c62828;");
        }
    }

    private String convertToDisplayFormat(String dateString) {
        try {
            LocalDate date = LocalDate.parse(dateString, dateFormatter);
            return date.format(displayDateFormatter);
        } catch (Exception e) {
            return dateString;
        }
    }
}