package Apoteker.Controller;

import Connection.DBConnect;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class DataCekKesehatanController implements Initializable {

    @FXML private TableView<TransaksiCheckUp> tabelPenjualan;
    @FXML private TableColumn<TransaksiCheckUp, String> colIDPenjualan;
    @FXML private TableColumn<TransaksiCheckUp, String> colNamaKaryawan;
    @FXML private TableColumn<TransaksiCheckUp, String> colNamaPasien;
    @FXML private TableColumn<TransaksiCheckUp, String> colTanggalPenjualan;
    @FXML private TableColumn<TransaksiCheckUp, String> colTotalHarga;
    @FXML private TableColumn<TransaksiCheckUp, Void> colAksi;
    @FXML private TextField searchField;
    @FXML private DatePicker dpTanggalFilter;
    @FXML private Button btnRefresh;
    @FXML private Button btnTambah;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfIDPenjualan;
    @FXML private TextField tfNamaKaryawan;
    @FXML private TextField tfNamaPasien;
    @FXML private TextField tfTanggalPenjualan;
    @FXML private ComboBox<LayananCheckUp> cbObat;
    @FXML private TextField tfHargaSatuan;
    @FXML private Button btnTambahKeranjang;
    @FXML private TableView<ItemKeranjang> tabelKeranjang;
    @FXML private TableColumn<ItemKeranjang, String> colNamaObat;
    @FXML private TableColumn<ItemKeranjang, String> colHargaSatuan;
    @FXML private TableColumn<ItemKeranjang, String> colSubTotal;
    @FXML private TableColumn<ItemKeranjang, Void> colHapus;
    @FXML private TextField tfJumlahUang;
    @FXML private TextField tfKembalian;
    @FXML private Label lblTotalBelanja;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;
    @FXML private VBox detailPopup;
    @FXML private Label detailTitle;
    @FXML private Label lblDetailID;
    @FXML private Label lblDetailKaryawan;
    @FXML private Label lblDetailPasien;
    @FXML private Label lblDetailTanggal;
    @FXML private TableView<ItemKeranjang> tabelDetail;
    @FXML private TableColumn<ItemKeranjang, String> colDetailNamaObat;
    @FXML private TableColumn<ItemKeranjang, String> colDetailHargaSatuan;
    @FXML private TableColumn<ItemKeranjang, String> colDetailSubTotal;
    @FXML private Label lblDetailTotal;
    @FXML private Button btnTutupDetail;
    @FXML private TextField tfNoKTP;
    @FXML private TextField tfNoTelepon;
    @FXML private TableColumn<TransaksiCheckUp, String> colNoKTP;
    @FXML private TableColumn<TransaksiCheckUp, String> colNoTelepon;
    @FXML private Label lblDetailKTP;
    @FXML private Label lblDetailTelepon;


    private ObservableList<TransaksiCheckUp> dataTransaksi = FXCollections.observableArrayList();
    private ObservableList<LayananCheckUp> dataLayanan = FXCollections.observableArrayList();
    private ObservableList<ItemKeranjang> keranjangItems = FXCollections.observableArrayList();
    private FilteredList<TransaksiCheckUp> filteredData;
    private DBConnect dbConnect;
    private SessionManager sessionManager;
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private ObservableList<TransaksiCheckUp> dataFiltered = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionManager = SessionManager.getInstance();
        System.out.println("🔧 DataCekKesehatanController initialized");
        sessionManager.printCurrentSession();
        if (!sessionManager.isLoggedIn()) {
            showAlert("Session Error", "Sesi login tidak valid. Silakan login kembali.");
            return;
        }

        tfNoKTP.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tfNoKTP.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        tfNoTelepon.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                tfNoTelepon.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        initializeDatabase();
        initializeTableColumns();
        initializeFormComponents();
        loadData();
        setupSearchAndFilter();

    }

    private void initializeDatabase() {
        try {
            dbConnect = new DBConnect();
            if (dbConnect.conn == null) {
                showAlert("Database Error", "Tidak dapat terhubung ke database!");
            }
        } catch (Exception e) {
            showAlert("Database Error", "Tidak dapat terhubung ke database: " + e.getMessage());
        }
    }

    private void initializeTableColumns() {
        colIDPenjualan.setCellValueFactory(new PropertyValueFactory<>("idTransaksi"));
        colNamaKaryawan.setCellValueFactory(new PropertyValueFactory<>("namaKaryawan"));
        colNamaPasien.setCellValueFactory(new PropertyValueFactory<>("namaPasien"));
        colNoKTP.setCellValueFactory(new PropertyValueFactory<>("noKTP"));
        colNoTelepon.setCellValueFactory(new PropertyValueFactory<>("noTelepon"));
        colTanggalPenjualan.setCellValueFactory(cellData -> {
            String originalDate = cellData.getValue().getTanggalTransaksi();
            try {
                LocalDate date;
                if (originalDate.contains(" ")) {
                    String datePart = originalDate.split(" ")[0]; // Get only the date part
                    date = LocalDate.parse(datePart);
                } else {
                    date = LocalDate.parse(originalDate);
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
                return new SimpleStringProperty(date.format(formatter));

            } catch (Exception e) {
                System.err.println("Error parsing date: " + originalDate + " - " + e.getMessage());
                try {
                    if (originalDate.length() >= 10) {
                        String datePart = originalDate.substring(0, 10); // Get first 10 characters (yyyy-MM-dd)
                        LocalDate date = LocalDate.parse(datePart);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
                        return new SimpleStringProperty(date.format(formatter));
                    }
                } catch (Exception e2) {
                    System.err.println("Secondary parsing also failed: " + e2.getMessage());
                }
                return new SimpleStringProperty(originalDate);
            }
        });

        colTotalHarga.setCellValueFactory(cellData -> new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getTotalHarga())));
        colAksi.setCellFactory(new Callback<TableColumn<TransaksiCheckUp, Void>, TableCell<TransaksiCheckUp, Void>>() {
            @Override
            public TableCell<TransaksiCheckUp, Void> call(TableColumn<TransaksiCheckUp, Void> param) {
                return new TableCell<TransaksiCheckUp, Void>() {
                    private final Button btnDetail = new Button("👁 Detail");
                    {
                        btnDetail.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 8;");
                        btnDetail.setOnAction(e -> {
                            TransaksiCheckUp transaksi = getTableView().getItems().get(getIndex());
                            tampilkanDetail(transaksi);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnDetail);
                        }
                    }
                };
            }
        });

        colNamaObat.setCellValueFactory(new PropertyValueFactory<>("namaLayanan"));
        colHargaSatuan.setCellValueFactory(cellData -> new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getHargaSatuan())));
        colSubTotal.setCellValueFactory(cellData -> new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getSubTotal())));
        colHapus.setCellFactory(new Callback<TableColumn<ItemKeranjang, Void>, TableCell<ItemKeranjang, Void>>() {
            @Override
            public TableCell<ItemKeranjang, Void> call(TableColumn<ItemKeranjang, Void> param) {
                return new TableCell<ItemKeranjang, Void>() {
                    private final Button btnHapus = new Button("🗑");
                    {
                        btnHapus.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 8;");
                        btnHapus.setOnAction(e -> {
                            ItemKeranjang item = getTableView().getItems().get(getIndex());

                            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                            confirmAlert.setTitle("Konfirmasi Hapus");
                            confirmAlert.setHeaderText(null);
                            confirmAlert.setContentText("Apakah Anda yakin ingin menghapus layanan \"" + item.getNamaLayanan() + "\" dari keranjang?");
                            confirmAlert.initOwner(getTableView().getScene().getWindow());
                            confirmAlert.initModality(Modality.WINDOW_MODAL);

                            Optional<ButtonType> result = confirmAlert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                keranjangItems.remove(item);
                                updateTotal();
                            }
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
                };
            }
        });

        colDetailNamaObat.setCellValueFactory(new PropertyValueFactory<>("namaLayanan"));
        colDetailHargaSatuan.setCellValueFactory(cellData -> new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getHargaSatuan())));
        colDetailSubTotal.setCellValueFactory(cellData -> new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getSubTotal())));
    }

    private void initializeFormComponents() {
        cbObat.setItems(dataLayanan);
        cbObat.setConverter(new javafx.util.StringConverter<LayananCheckUp>() {
            @Override
            public String toString(LayananCheckUp layanan) {
                return layanan != null ? layanan.getNamaLayanan() : "";
            }
            @Override
            public LayananCheckUp fromString(String string) {
                return null;
            }
        });

        cbObat.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tfHargaSatuan.setText(currencyFormatter.format(newVal.getBiayaLayanan()));
            } else {
                tfHargaSatuan.clear();
            }
        });

        tabelKeranjang.setItems(keranjangItems);

        tfJumlahUang.setText("Rp. ");
        tfJumlahUang.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.startsWith("Rp. ")) {
                tfJumlahUang.setText("Rp. ");
                return;
            }

            String numbersOnly = newVal.substring(4).replaceAll("[^\\d]", "");
            if (!numbersOnly.isEmpty()) {
                String formatted = formatRupiah(numbersOnly);
                if (!formatted.equals(newVal)) {
                    Platform.runLater(() -> {
                        tfJumlahUang.setText(formatted);
                        tfJumlahUang.positionCaret(formatted.length());
                    });
                }
            }
            calculateKembalian();
        });

        tfJumlahUang.setOnKeyPressed(event -> {
            if (tfJumlahUang.getCaretPosition() <= 4) {
                if (event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE ||
                        event.getCode() == javafx.scene.input.KeyCode.DELETE ||
                        event.getCode() == javafx.scene.input.KeyCode.LEFT) {
                    event.consume();
                }
            }
        });

        tfJumlahUang.setOnMouseClicked(event -> {
            if (tfJumlahUang.getCaretPosition() < 4) {
                tfJumlahUang.positionCaret(tfJumlahUang.getText().length());
            }
        });

        tfNamaPasien.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("[a-zA-Z\\s]*")) {
                tfNamaPasien.setText(oldVal);
            }
        });
        tfNamaKaryawan.setEditable(false);
    }

    private void loadData() {
        loadTransaksiData();
        loadLayananData();
    }

    @FXML
    private void tutupForm() {
        if (keranjangItems != null && !keranjangItems.isEmpty()) {
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
                keranjangItems.clear();
                formPopup.setVisible(false);
                formPopup.setManaged(false);
            }
        } else {
            formPopup.setVisible(false);
            formPopup.setManaged(false);
        }
    }

    private void loadTransaksiData() {
        dataTransaksi.clear();
        String query = "SELECT t.IDCheckUp, k.NamaKaryawan, t.NamaPasien, t.NoKTP, t.NoTelepon, t.TanggalTransaksi, t.TotalHarga " +
                "FROM TransaksiCheckUp t " +
                "JOIN Karyawan k ON t.IDKaryawan = k.IDKaryawan " +
                "ORDER BY t.TanggalTransaksi DESC";

        try {
            dbConnect.result = dbConnect.stat.executeQuery(query);
            while (dbConnect.result.next()) {
                TransaksiCheckUp transaksi = new TransaksiCheckUp(
                        dbConnect.result.getString("IDCheckUp"),
                        dbConnect.result.getString("NamaKaryawan"),
                        dbConnect.result.getString("NamaPasien"),
                        dbConnect.result.getString("NoKTP"),
                        dbConnect.result.getString("NoTelepon"),
                        dbConnect.result.getString("TanggalTransaksi"),
                        dbConnect.result.getDouble("TotalHarga")
                );
                dataTransaksi.add(transaksi);
            }
            filterData();
        } catch (SQLException e) {
            showAlert("Error", "Gagal memuat data transaksi: " + e.getMessage());
        }
    }

    private void loadLayananData() {
        dataLayanan.clear();
        String query = "SELECT * FROM LayananCheckUp WHERE Status = 'Aktif' ORDER BY NamaLayanan";
        try {
            dbConnect.result = dbConnect.stat.executeQuery(query);
            while (dbConnect.result.next()) {
                LayananCheckUp layanan = new LayananCheckUp(
                        dbConnect.result.getString("IDLayanan"),
                        dbConnect.result.getString("NamaLayanan"),
                        dbConnect.result.getString("Deskripsi"),
                        dbConnect.result.getDouble("BiayaLayanan"),
                        dbConnect.result.getString("Status")
                );
                dataLayanan.add(layanan);
            }
        } catch (SQLException e) {
            showAlert("Error", "Gagal memuat data layanan: " + e.getMessage());
        }
    }

    private boolean checkDateMatch(String tanggalTransaksi, LocalDate filterDate) {
        try {
            LocalDate transaksiDate;
            if (tanggalTransaksi.contains(" ")) {
                String datePart = tanggalTransaksi.split(" ")[0];
                transaksiDate = LocalDate.parse(datePart);
            } else {
                transaksiDate = LocalDate.parse(tanggalTransaksi);
            }
            return transaksiDate.equals(filterDate);
        } catch (Exception e) {
            System.err.println("Error parsing date for filter: " + tanggalTransaksi);
            return false;
        }
    }

    private void setupSearchAndFilter() {
        tabelPenjualan.setItems(dataFiltered);
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterData());
        dpTanggalFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterData());
        cbObat.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tfHargaSatuan.setText(currencyFormatter.format(newVal.getBiayaLayanan()));
            } else {
                tfHargaSatuan.clear();
            }
        });
        tfJumlahUang.textProperty().addListener((obs, oldVal, newVal) -> {
            calculateKembalian();
        });
        tfNamaPasien.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("[a-zA-Z\\s]*")) {
                tfNamaPasien.setText(oldVal);
            }
        });
    }

    private void filterData() {
        dataFiltered.clear();
        String searchText = searchField.getText().toLowerCase();
        LocalDate filterDate = dpTanggalFilter.getValue();

        for (TransaksiCheckUp transaksi : dataTransaksi) {
            boolean matchSearch = searchText.isEmpty() ||
                    transaksi.getIdTransaksi().toLowerCase().contains(searchText) ||
                    transaksi.getNamaPasien().toLowerCase().contains(searchText) ||
                    transaksi.getNamaKaryawan().toLowerCase().contains(searchText) ||
                    // TAMBAHKAN BARIS INI:
                    transaksi.getNoKTP().toLowerCase().contains(searchText) ||
                    transaksi.getNoTelepon().toLowerCase().contains(searchText);

            boolean matchDate = filterDate == null || checkDateMatch(transaksi.getTanggalTransaksi(), filterDate);

            if (matchSearch && matchDate) {
                dataFiltered.add(transaksi);
            }
        }
    }

    @FXML
    private void refreshData() {
        loadData();
        searchField.clear();
        dpTanggalFilter.setValue(null);
        filterData();
    }

    @FXML
    private void tampilkanFormPenjualan() {
        if (!sessionManager.isLoggedIn()) {
            showAlert("Session Error", "Sesi login tidak valid. Silakan login kembali.");
            return;
        }
        resetForm();
        generateNewTransactionID();
        setCurrentUserAndDate();
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    private void resetForm() {
        tfIDPenjualan.clear();
        tfNamaKaryawan.clear();
        tfNamaPasien.clear();
        tfTanggalPenjualan.clear();
        cbObat.getSelectionModel().clearSelection();
        tfHargaSatuan.clear();
        tfJumlahUang.setText("Rp. ");
        tfKembalian.clear();
        keranjangItems.clear();
        tfNoKTP.clear();
        tfNoTelepon.clear();
        updateTotal();
    }

    private void generateNewTransactionID() {
        String query = "SELECT MAX(CAST(SUBSTRING(IDCheckUp, 5, LEN(IDCheckUp)) AS INT)) as max_id " +
                "FROM TransaksiCheckUp WHERE IDCheckUp LIKE 'TRLC%'";
        try {
            dbConnect.result = dbConnect.stat.executeQuery(query);
            int nextNumber = 1;
            if (dbConnect.result.next() && dbConnect.result.getObject("max_id") != null) {
                nextNumber = dbConnect.result.getInt("max_id") + 1;
            }
            String newID = String.format("TRLC%03d", nextNumber);
            tfIDPenjualan.setText(newID);
        } catch (SQLException e) {
            showAlert("Error", "Gagal generate ID transaksi: " + e.getMessage());
        }
    }

    private void setCurrentUserAndDate() {
        String currentUserName = sessionManager.getDisplayName();
        System.out.println("🔍 Setting current user:");
        System.out.println("   Display Name: " + currentUserName);
        System.out.println("   User ID: " + sessionManager.getLoggedInUserId());
        System.out.println("   Full Name: " + sessionManager.getLoggedInUserFullName());
        tfNamaKaryawan.setText(currentUserName);
        tfTanggalPenjualan.setText(java.time.LocalDate.now().toString());
    }

    @FXML
    private void tambahKeKeranjang() {
        if (validateKeranjangInput()) {
            LayananCheckUp selectedLayanan = cbObat.getSelectionModel().getSelectedItem();
            Optional<ItemKeranjang> existingItem = keranjangItems.stream()
                    .filter(item -> item.getIdLayanan().equals(selectedLayanan.getIdLayanan()))
                    .findFirst();

            if (existingItem.isPresent()) {
                showAlert("Informasi", "Layanan \"" + selectedLayanan.getNamaLayanan() + "\" sudah ada di keranjang!");
            } else {
                ItemKeranjang newItem = new ItemKeranjang(
                        selectedLayanan.getIdLayanan(),
                        selectedLayanan.getNamaLayanan(),
                        1,
                        selectedLayanan.getBiayaLayanan()
                );
                keranjangItems.add(newItem);
                updateTotal();
            }

            cbObat.getSelectionModel().clearSelection();
            tfHargaSatuan.clear();
        }
    }

    private boolean validateKeranjangInput() {
        if (cbObat.getSelectionModel().getSelectedItem() == null) {
            showAlert("Validation Error", "Silakan pilih layanan check-up terlebih dahulu!");
            return false;
        }
        return true;
    }

    private void updateTotal() {
        double total = keranjangItems.stream()
                .mapToDouble(ItemKeranjang::getSubTotal)
                .sum();
        lblTotalBelanja.setText(currencyFormatter.format(total));
        calculateKembalian();
    }

    private void calculateKembalian() {
        try {
            String jumlahUangText = tfJumlahUang.getText().trim();
            if (jumlahUangText.equals("Rp. ") || jumlahUangText.isEmpty()) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                tfKembalian.setText(formatter.format(0));
                return;
            }

            double total = keranjangItems.stream()
                    .mapToDouble(ItemKeranjang::getSubTotal)
                    .sum();
            double jumlahUang = parseRupiah(jumlahUangText);
            double kembalian = jumlahUang - total;
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

    private double parseRupiah(String rupiahText) {
        String cleanText = rupiahText.replace("Rp. ", "").replaceAll("[^\\d]", "");
        if (cleanText.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(cleanText);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @FXML
    private void simpanPenjualan() {
        if (validateForm()) {
            try {
                dbConnect.conn.setAutoCommit(false);

                String currentUserId = sessionManager.getLoggedInUserId();
                if (currentUserId == null || currentUserId.isEmpty()) {
                    showAlert("Session Error", "ID karyawan tidak ditemukan dalam sesi. Silakan login kembali.");
                    return;
                }

                String idCheckUp = tfIDPenjualan.getText();
                String namaPasien = tfNamaPasien.getText();
                String noKTP = tfNoKTP.getText();
                String noTelepon = tfNoTelepon.getText();
                double total = keranjangItems.stream().mapToDouble(ItemKeranjang::getSubTotal).sum();

                if (noKTP.isEmpty() || noTelepon.isEmpty()) {
                    showAlert("Validasi", "No. KTP dan No. Telepon tidak boleh kosong.");
                    return;
                }

                String insertTransaksi = """
                INSERT INTO TransaksiCheckUp
                (IDCheckUp, IDKaryawan, NamaPasien, NoKTP, NoTelepon, TanggalTransaksi, TotalHarga)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
                dbConnect.pstat = dbConnect.conn.prepareStatement(insertTransaksi);
                dbConnect.pstat.setString(1, idCheckUp);
                dbConnect.pstat.setString(2, currentUserId);
                dbConnect.pstat.setString(3, namaPasien);
                dbConnect.pstat.setString(4, noKTP);
                dbConnect.pstat.setString(5, noTelepon);
                dbConnect.pstat.setDate(6, java.sql.Date.valueOf(java.time.LocalDate.now()));
                dbConnect.pstat.setDouble(7, total);
                dbConnect.pstat.executeUpdate();

                String insertDetail = """
    INSERT INTO DetailTransaksiCheckUp
    (IDCheckUp, IDLayanan, Kuantitas, SubTotal)
    VALUES (?, ?, ?, ?)
    """;
                for (ItemKeranjang item : keranjangItems) {
                    dbConnect.pstat = dbConnect.conn.prepareStatement(insertDetail);
                    dbConnect.pstat.setString(1, idCheckUp);
                    dbConnect.pstat.setString(2, item.getIdLayanan());
                    dbConnect.pstat.setInt(3, 1); // Kuantitas selalu 1
                    dbConnect.pstat.setDouble(4, item.getSubTotal());
                    dbConnect.pstat.executeUpdate();
                }

                dbConnect.conn.commit();
                showAlert("Success", "Transaksi berhasil disimpan!");
                clearFormForNextTransaction();
                loadTransaksiData();

            } catch (SQLException e) {
                try {
                    dbConnect.conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                showAlert("Error", "Gagal menyimpan transaksi: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    dbConnect.conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void clearFormForNextTransaction() {
        tfNamaPasien.clear();
        cbObat.getSelectionModel().clearSelection();
        tfHargaSatuan.clear();
        tfJumlahUang.setText("Rp. ");
        tfKembalian.clear();
        keranjangItems.clear();
        tfNoTelepon.clear();
        tfNoKTP.clear();
        generateNewTransactionID();
        setCurrentUserAndDate();
        updateTotal();
    }

    private boolean validateForm() {
        if (!sessionManager.isLoggedIn()) {
            showAlert("Session Error", "Sesi login tidak valid. Silakan login kembali.");
            return false;
        }
        if (tfNamaPasien.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Nama pasien harus diisi!");
            return false;
        }
        if (keranjangItems.isEmpty()) {
            showAlert("Validation Error", "Keranjang masih kosong! Tambahkan minimal satu layanan.");
            return false;
        }
        try {
            double total = keranjangItems.stream().mapToDouble(ItemKeranjang::getSubTotal).sum();
            double jumlahUang = parseRupiah(tfJumlahUang.getText());

            if (jumlahUang < total) {
                showAlert("Validation Error", "Jumlah uang tidak mencukupi!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Jumlah uang harus berupa angka!");
            return false;
        }
        return true;
    }

    @FXML
    private void batalPenjualan() {
        if (keranjangItems.isEmpty()) {
            showAlert("Tidak Ada Data", "Keranjang masih kosong! Tidak ada transaksi yang perlu dibatalkan.");
            return;
        }

        // Ada barang di keranjang, tampilkan konfirmasi
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Batal");
        confirmAlert.setHeaderText("Batalkan Transaksi");
        confirmAlert.setContentText("Yakin ingin membatalkan transaksi dan mengosongkan keranjang? Semua data yang ada di keranjang akan dihapus.");
        confirmAlert.initOwner(formPopup.getScene().getWindow());
        confirmAlert.initModality(Modality.WINDOW_MODAL);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Kosongkan keranjang dan reset form fields terkait transaksi
            keranjangItems.clear();
            tfNamaPasien.clear();
            cbObat.getSelectionModel().clearSelection();
            tfHargaSatuan.clear();
            tfJumlahUang.setText("Rp. ");
            tfKembalian.clear();
            updateTotal();

            // Generate ID transaksi baru dan set tanggal
            generateNewTransactionID();
            setCurrentUserAndDate();

            showAlert("Berhasil", "Transaksi berhasil dibatalkan dan keranjang dikosongkan.");
        }
    }


    private void tampilkanDetail(TransaksiCheckUp transaksi) {
        lblDetailID.setText("ID CheckUp\t: " + transaksi.getIdTransaksi());
        lblDetailKaryawan.setText("Karyawan\t: " + transaksi.getNamaKaryawan());
        lblDetailPasien.setText("Nama Pasien\t: " + transaksi.getNamaPasien());

        lblDetailKTP.setText("No. KTP\t\t: " + transaksi.getNoKTP());
        lblDetailTelepon.setText("No. Telepon\t: " + transaksi.getNoTelepon());

        String formattedDate = transaksi.getTanggalTransaksi();
        try {
            String originalDate = transaksi.getTanggalTransaksi();
            LocalDate date;
            if (originalDate.contains(" ")) {
                String datePart = originalDate.split(" ")[0];
                date = LocalDate.parse(datePart);
            } else {
                date = LocalDate.parse(originalDate);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
            formattedDate = date.format(formatter);
        } catch (Exception e) {
            System.err.println("Error parsing date in detail: " + transaksi.getTanggalTransaksi());
            try {
                String originalDate = transaksi.getTanggalTransaksi();
                if (originalDate.length() >= 10) {
                    String datePart = originalDate.substring(0, 10);
                    LocalDate date = LocalDate.parse(datePart);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
                    formattedDate = date.format(formatter);
                }
            } catch (Exception e2) {
            }
        }

        lblDetailTanggal.setText("Tanggal\t\t: " + formattedDate);
        lblDetailTotal.setText(currencyFormatter.format(transaksi.getTotalHarga()));
        ObservableList<ItemKeranjang> detailItems = FXCollections.observableArrayList();
        String query = "SELECT d.*, l.NamaLayanan, l.BiayaLayanan FROM DetailTransaksiCheckUp d " +
                "JOIN LayananCheckUp l ON d.IDLayanan = l.IDLayanan " +
                "WHERE d.IDCheckUp = ?";
        try {
            dbConnect.pstat = dbConnect.conn.prepareStatement(query);
            dbConnect.pstat.setString(1, transaksi.getIdTransaksi());
            dbConnect.result = dbConnect.pstat.executeQuery();
            while (dbConnect.result.next()) {
                double hargaSatuan = dbConnect.result.getDouble("BiayaLayanan");
                ItemKeranjang item = new ItemKeranjang(
                        dbConnect.result.getString("IDLayanan"),
                        dbConnect.result.getString("NamaLayanan"),
                        1,
                        hargaSatuan
                );
                detailItems.add(item);
            }
        } catch (SQLException e) {
            showAlert("Error", "Gagal memuat detail transaksi: " + e.getMessage());
        }
        tabelDetail.setItems(detailItems);
        detailPopup.setVisible(true);
        detailPopup.setManaged(true);
    }



    @FXML
    private void tutupDetail() {
        detailPopup.setVisible(false);
        detailPopup.setManaged(false);
    }

    private String formatRupiah(String input) {
        String numbersOnly = input.replaceAll("[^\\d]", "");
        if (numbersOnly.isEmpty()) {return "Rp. ";}
        try {
            long number = Long.parseLong(numbersOnly);
            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("id", "ID"));
            return "Rp. " + formatter.format(number);
        } catch (NumberFormatException e) {
            return "Rp. ";
        }
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

    public class TransaksiCheckUp {
        private String idTransaksi;
        private String namaKaryawan;
        private String namaPasien;
        private String noKTP;
        private String noTelepon;
        private String tanggalTransaksi;
        private double totalHarga;

        public TransaksiCheckUp(String idTransaksi, String namaKaryawan, String namaPasien,
                                String noKTP, String noTelepon, String tanggalTransaksi, double totalHarga) {
            this.idTransaksi = idTransaksi;
            this.namaKaryawan = namaKaryawan;
            this.namaPasien = namaPasien;
            this.noKTP = noKTP;
            this.noTelepon = noTelepon;
            this.tanggalTransaksi = tanggalTransaksi;
            this.totalHarga = totalHarga;
        }

        public String getIdTransaksi() { return idTransaksi; }
        public void setIdTransaksi(String idTransaksi) { this.idTransaksi = idTransaksi; }
        public String getNamaKaryawan() { return namaKaryawan; }
        public void setNamaKaryawan(String namaKaryawan) { this.namaKaryawan = namaKaryawan; }
        public String getNamaPasien() { return namaPasien; }
        public void setNamaPasien(String namaPasien) { this.namaPasien = namaPasien; }
        public String getTanggalTransaksi() { return tanggalTransaksi; }
        public void setTanggalTransaksi(String tanggalTransaksi) { this.tanggalTransaksi = tanggalTransaksi; }
        public double getTotalHarga() { return totalHarga; }
        public void setTotalHarga(double totalHarga) { this.totalHarga = totalHarga; }
        public String getNoKTP() { return noKTP; }
        public void setNoKTP(String noKTP) { this.noKTP = noKTP; }
        public String getNoTelepon() { return noTelepon; }
        public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }
    }

    public static class LayananCheckUp {
        private String idLayanan;
        private String namaLayanan;
        private String deskripsi;
        private double biayaLayanan;
        private String status;

        public LayananCheckUp(String idLayanan, String namaLayanan, String deskripsi, double biayaLayanan, String status) {
            this.idLayanan = idLayanan;
            this.namaLayanan = namaLayanan;
            this.deskripsi = deskripsi;
            this.biayaLayanan = biayaLayanan;
            this.status = status;
        }

        public String getIdLayanan() { return idLayanan; }
        public void setIdLayanan(String idLayanan) { this.idLayanan = idLayanan; }
        public String getNamaLayanan() { return namaLayanan; }
        public void setNamaLayanan(String namaLayanan) { this.namaLayanan = namaLayanan; }
        public String getDeskripsi() { return deskripsi; }
        public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
        public double getBiayaLayanan() { return biayaLayanan; }
        public void setBiayaLayanan(double biayaLayanan) { this.biayaLayanan = biayaLayanan; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ItemKeranjang {
        private String idLayanan;
        private String namaLayanan;
        private double hargaSatuan;

        public ItemKeranjang(String idLayanan, String namaLayanan, int kuantitas, double hargaSatuan) {
            this.idLayanan = idLayanan;
            this.namaLayanan = namaLayanan;
            this.hargaSatuan = hargaSatuan;
            // Parameter kuantitas diabaikan karena tidak digunakan lagi
        }

        // Getter dan Setter
        public String getIdLayanan() {
            return idLayanan;
        }

        public void setIdLayanan(String idLayanan) {
            this.idLayanan = idLayanan;
        }

        public String getNamaLayanan() {
            return namaLayanan;
        }

        public void setNamaLayanan(String namaLayanan) {
            this.namaLayanan = namaLayanan;
        }

        public double getHargaSatuan() {
            return hargaSatuan;
        }

        public void setHargaSatuan(double hargaSatuan) {
            this.hargaSatuan = hargaSatuan;
        }

        // Method untuk kompatibilitas dengan kode yang sudah ada
        public double getSubTotal() {
            return hargaSatuan; // Karena tidak ada kuantitas, subtotal = harga satuan
        }
    }
}