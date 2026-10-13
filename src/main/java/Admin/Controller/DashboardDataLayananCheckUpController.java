package Admin.Controller;

import Admin.LayananCheckUp;
import Connection.DBConnect;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javafx.scene.input.KeyCode;
import java.util.Optional;
import java.util.function.UnaryOperator;
import javafx.scene.control.TextFormatter;
import javafx.stage.Modality;

public class DashboardDataLayananCheckUpController {

    @FXML private TableView<LayananCheckUp> tabelLayanan;
    @FXML private TableColumn<LayananCheckUp, String> colIDLayanan, colNamaLayanan, colDeskripsiLayanan, colBiayaLayanan, colStatus;
    @FXML private TableColumn<LayananCheckUp, Void> colAksi;
    @FXML private ComboBox<String> comboFilterStatus;
    @FXML private TextField searchField;
    @FXML private Button btnTambahLayanan;
    @FXML private VBox formPopup;
    @FXML private Label formTitle;
    @FXML private TextField tfIDLayanan;
    @FXML private TextField tfNamaLayanan, tfDeskripsiLayanan, tfBiayaLayanan;
    @FXML private Button btnSimpanLayanan, btnBatalLayanan;
    @FXML private TextField tfStatusLayanan;

    private final ObservableList<LayananCheckUp> dataLayanan = FXCollections.observableArrayList();
    private FilteredList<LayananCheckUp> filteredData;
    private LayananCheckUp selectedLayanan = null;
    private DecimalFormat currencyFormat;
    private boolean isUpdatingBiaya = false;
    private LayananCheckUp originalData = null;

    @FXML
    public void initialize() {
        tfIDLayanan.setText(generateNewId());
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        currencyFormat = new DecimalFormat("#,###", symbols);
        colIDLayanan.setCellValueFactory(new PropertyValueFactory<>("IDLayanan"));
        colNamaLayanan.setCellValueFactory(new PropertyValueFactory<>("namaLayanan"));
        colDeskripsiLayanan.setCellValueFactory(new PropertyValueFactory<>("deskripsi"));
        colBiayaLayanan.setCellValueFactory(new PropertyValueFactory<>("biayaLayanan"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (tfStatusLayanan != null) {
            tfStatusLayanan.setText("Aktif");
        }

        colBiayaLayanan.setCellFactory(column -> new TableCell<LayananCheckUp, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                } else {
                    try {
                        double biaya = Double.parseDouble(item);
                        setText("Rp. " + currencyFormat.format(biaya));
                    } catch (NumberFormatException e) {
                        setText("Rp. " + item);
                    }
                }
            }
        });

        filteredData = new FilteredList<>(dataLayanan, p -> true);
        tabelLayanan.setItems(filteredData);
        if (comboFilterStatus != null) {
            // Clear existing items dulu, baru tambahkan
            comboFilterStatus.getItems().clear();
            comboFilterStatus.getItems().addAll("Semua Status", "Aktif", "Tidak Aktif");
            comboFilterStatus.setValue("Aktif");
        }
        getDataLayanan();
        setupEventHandlers();
        setupFieldValidation();
        setupSearch();
        Platform.runLater(() -> {
            setupAksiKolom();
            applyFilters();
        });
    }

    private void setupFieldValidation() {
        if (tfNamaLayanan != null) {
            tfNamaLayanan.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    String filtered = newValue.replaceAll("[^a-zA-Z\\s-]", "");
                    if (!filtered.equals(newValue)) {
                        tfNamaLayanan.setText(filtered);
                        tfNamaLayanan.positionCaret(filtered.length());
                    }
                }
            });
        }
        setupCurrencyField();
    }

    private void setupCurrencyField() {
        if (tfBiayaLayanan == null) return;
        if (tfBiayaLayanan.getText().isEmpty()) {
            tfBiayaLayanan.setText("Rp. ");
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (!newText.startsWith("Rp. ")) {
                return null;
            }
            String numberPart = newText.substring(4);
            if (newText.length() < 4) {
                return null;
            }
            if (!numberPart.matches("^[0-9.]*$")) {
                return null;
            }
            long dotCount = numberPart.chars().filter(ch -> ch == '.').count();
            if (dotCount > (numberPart.length() - 1) / 4) { // Maksimal 1 titik per 4 karakter
                return null;
            }
            return change;
        };

        TextFormatter<String> formatter = new TextFormatter<>(filter);
        tfBiayaLayanan.setTextFormatter(formatter);
        tfBiayaLayanan.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingBiaya) return; // Prevent recursive updates

            if (!newVal.startsWith("Rp. ")) {
                isUpdatingBiaya = true;
                tfBiayaLayanan.setText("Rp. ");
                tfBiayaLayanan.positionCaret(4);
                isUpdatingBiaya = false;
                return;
            }
            String numberPart = newVal.substring(4);
            String cleaned = numberPart.replaceAll("[^\\d]", "");
            if (!cleaned.isEmpty()) {
                try {
                    long value = Long.parseLong(cleaned);
                    String formatted = "Rp. " + formatCurrency(value);

                    if (!newVal.equals(formatted)) {
                        isUpdatingBiaya = true;
                        int caretPos = tfBiayaLayanan.getCaretPosition();
                        int endPos = newVal.length();
                        int relativePos = endPos - caretPos;
                        tfBiayaLayanan.setText(formatted);
                        int newCaretPos = Math.max(4, formatted.length() - relativePos);
                        tfBiayaLayanan.positionCaret(Math.min(newCaretPos, formatted.length()));
                        isUpdatingBiaya = false;
                    }
                } catch (NumberFormatException e) {
                    isUpdatingBiaya = true;
                    tfBiayaLayanan.setText("Rp. ");
                    tfBiayaLayanan.positionCaret(4);
                    isUpdatingBiaya = false;
                }
            } else if (numberPart.isEmpty()) {
                if (!newVal.equals("Rp. ")) {
                    isUpdatingBiaya = true;
                    tfBiayaLayanan.setText("Rp. ");
                    tfBiayaLayanan.positionCaret(4);
                    isUpdatingBiaya = false;
                }
            }
        });

        tfBiayaLayanan.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                if (tfBiayaLayanan.getText().equals("Rp. ")) {
                    tfBiayaLayanan.positionCaret(4);
                }
            }
        });

        tfBiayaLayanan.setOnMouseClicked(e -> {
            if (tfBiayaLayanan.getCaretPosition() < 4) {
                tfBiayaLayanan.positionCaret(4);
            }
        });

        tfBiayaLayanan.setOnKeyPressed(e -> {
            if (tfBiayaLayanan.getCaretPosition() < 4) {
                if (e.getCode() != KeyCode.RIGHT && e.getCode() != KeyCode.END) {
                    tfBiayaLayanan.positionCaret(4);
                }
            }
        });
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

    public double extractNumber(String text) {
        String cleaned = text.replace("Rp. ", "").replaceAll("[^\\d]", "");
        return cleaned.isEmpty() ? 0 : Double.parseDouble(cleaned);
    }

    private void setupEventHandlers() {
        if (comboFilterStatus != null) {
            comboFilterStatus.setOnAction(e -> applyFilters());
        }
        if (btnTambahLayanan != null) {
            btnTambahLayanan.setOnAction(e -> showForm(null));
        }
        if (btnSimpanLayanan != null) {
            btnSimpanLayanan.setOnAction(e -> simpanData());
        }
        if (btnBatalLayanan != null) {
            btnBatalLayanan.setOnAction(e -> batalLayanan());
        }
    }

    @FXML
    private void batalLayanan() {
        if (selectedLayanan != null) {
            // Mode Edit - Reset ke data original
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Konfirmasi Batal");
            alert.setHeaderText("Batalkan Perubahan");
            alert.setContentText("Yakin ingin membatalkan perubahan dan kembali ke data original?");

            if (tabelLayanan != null && tabelLayanan.getScene() != null) {
                alert.initOwner(tabelLayanan.getScene().getWindow());
            }

            ButtonType buttonYes = new ButtonType("Ya", ButtonBar.ButtonData.YES);
            ButtonType buttonNo = new ButtonType("Tidak", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(buttonYes, buttonNo);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonYes) {
                // Reset ke data original
                if (originalData != null) {
                    tfIDLayanan.setText(originalData.getIDLayanan());
                    tfNamaLayanan.setText(originalData.getNamaLayanan());
                    tfDeskripsiLayanan.setText(originalData.getDeskripsi());
                    try {
                        double biaya = Double.parseDouble(originalData.getBiayaLayanan());
                        tfBiayaLayanan.setText("Rp. " + formatCurrency((long)biaya));
                    } catch (NumberFormatException e) {
                        tfBiayaLayanan.setText("Rp. " + originalData.getBiayaLayanan());
                    }
                    tfStatusLayanan.setText(originalData.getStatus());
                }
                showSuccessAlert("Perubahan berhasil dibatalkan, data dikembalikan ke kondisi original.");
            }
        } else {
            // Mode Tambah - Logic yang sudah ada
            boolean adaData = (tfNamaLayanan.getText() != null && !tfNamaLayanan.getText().trim().isEmpty()) ||
                    (tfDeskripsiLayanan.getText() != null && !tfDeskripsiLayanan.getText().trim().isEmpty()) ||
                    (tfBiayaLayanan.getText() != null && !tfBiayaLayanan.getText().equals("Rp. ") && !tfBiayaLayanan.getText().trim().isEmpty());

            if (adaData) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Konfirmasi Batal");
                alert.setHeaderText("Batalkan Input");
                alert.setContentText("Yakin ingin membatalkan input dan mengosongkan form?");

                if (tabelLayanan != null && tabelLayanan.getScene() != null) {
                    alert.initOwner(tabelLayanan.getScene().getWindow());
                }

                ButtonType buttonYes = new ButtonType("Ok", ButtonBar.ButtonData.YES);
                ButtonType buttonNo = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
                alert.getButtonTypes().setAll(buttonYes, buttonNo);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == buttonYes) {
                    clearFormFields();
                    tfIDLayanan.setText(generateNewId());
                    tfStatusLayanan.setText("Aktif");
                    showSuccessAlert("Input berhasil dibatalkan dan form dikosongkan.");
                }
            } else {
                showAlert("Tidak ada data yang perlu dibatalkan.");
            }
        }
    }


    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
        }
    }

    private void applyFilters() {
        if (filteredData == null) return;
        filteredData.setPredicate(layanan -> {
            boolean matchesSearch = true;
            if (searchField != null) {
                String searchText = searchField.getText();
                if (searchText != null && !searchText.trim().isEmpty()) {
                    String lowerCaseFilter = searchText.toLowerCase().trim();
                    matchesSearch = containsIgnoreCase(layanan.getIDLayanan(), lowerCaseFilter) ||
                            containsIgnoreCase(layanan.getNamaLayanan(), lowerCaseFilter) ||
                            containsIgnoreCase(layanan.getDeskripsi(), lowerCaseFilter) ||
                            containsIgnoreCase(layanan.getBiayaLayanan(), lowerCaseFilter) ||
                            containsIgnoreCase(layanan.getStatus(), lowerCaseFilter);
                }
            }

            String statusFilter = comboFilterStatus != null ? comboFilterStatus.getValue() : null;
            boolean matchesStatus = true;
            if (statusFilter != null && !statusFilter.equals("Semua Status")) {
                matchesStatus = layanan.getStatus().equalsIgnoreCase(statusFilter);
            }
            return matchesSearch && matchesStatus;
        });
        if (tabelLayanan != null) {
            tabelLayanan.refresh();
        }
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) return false;
        return source.toLowerCase().contains(target);
    }

    private void setupAksiKolom() {
        if (colAksi == null) return;
        colAksi.setCellFactory(param -> new TableCell<LayananCheckUp, Void>() {
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
                btnEdit.setOnAction(e -> {
                    LayananCheckUp l = getTableView().getItems().get(getIndex());
                    showForm(l);
                });

                Label deleteIcon = new Label("🗑");
                deleteIcon.setStyle("-fx-font-size: 10px; -fx-font-family: 'Segoe UI Emoji';");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setText("");
                btnDelete.setPrefSize(32, 28);
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> {
                    LayananCheckUp l = getTableView().getItems().get(getIndex());
                    showConfirmHapus(l);
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

    private void getDataLayanan() {
        dataLayanan.clear();
        try {
            DBConnect db = new DBConnect();
            String sql = "SELECT * FROM LayananCheckUp";
            ResultSet rs = db.stat.executeQuery(sql);
            while (rs.next()) {
                LayananCheckUp l = new LayananCheckUp(
                        rs.getString("IDLayanan"),
                        rs.getString("NamaLayanan"),
                        rs.getString("Deskripsi"),
                        rs.getString("BiayaLayanan"),
                        rs.getString("Status")
                );
                dataLayanan.add(l);
            }
            rs.close();
            db.stat.close();
            db.conn.close();

            System.out.println("Data loaded: " + dataLayanan.size() + " records"); // Debug line

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Gagal mengambil data: " + e.getMessage());
        }
    }

    private void showForm(LayananCheckUp layanan) {
        if (formPopup == null) return;
        if (layanan != null) {
            originalData = new LayananCheckUp(
                    layanan.getIDLayanan(),
                    layanan.getNamaLayanan(),
                    layanan.getDeskripsi(),
                    layanan.getBiayaLayanan(),
                    layanan.getStatus()
            );
            if (formTitle != null) {
                formTitle.setText("🏥 Edit Layanan Check Up");
            }
            if (tfIDLayanan != null) {
                tfIDLayanan.setText(layanan.getIDLayanan());
            }
            if (tfNamaLayanan != null) {
                tfNamaLayanan.setText(layanan.getNamaLayanan());
            }
            if (tfDeskripsiLayanan != null) {
                tfDeskripsiLayanan.setText(layanan.getDeskripsi());
            }
            if (tfBiayaLayanan != null) {
                try {
                    double biaya = Double.parseDouble(layanan.getBiayaLayanan());
                    tfBiayaLayanan.setText("Rp. " + formatCurrency((long)biaya));
                } catch (NumberFormatException e) {
                    tfBiayaLayanan.setText("Rp. " + layanan.getBiayaLayanan());
                }
            }
            if (tfStatusLayanan != null) {
                tfStatusLayanan.setText(layanan.getStatus());
            }
            selectedLayanan = layanan;
        } else {
            if (formTitle != null) {
                formTitle.setText("🏥 Form Layanan Check Up");
            }
            String nextId = generateNewId();
            if (tfIDLayanan != null) {
                tfIDLayanan.setText(nextId);
            }
            if (tfNamaLayanan != null) {
                tfNamaLayanan.clear();
            }
            if (tfDeskripsiLayanan != null) {
                tfDeskripsiLayanan.clear();
            }
            if (tfBiayaLayanan != null) {
                tfBiayaLayanan.setText("Rp. ");
            }
            if (tfStatusLayanan != null) {
                tfStatusLayanan.setText("Aktif");
            }
            selectedLayanan = null;
        }
        formPopup.setVisible(true);
        formPopup.setManaged(true);
    }

    @FXML
    public void tutupForm() {
        if (formPopup != null) {
            formPopup.setVisible(false);
            formPopup.setManaged(false);
        }
        selectedLayanan = null;
        originalData = null; // Reset original data
    }

    private void clearFormFields() {
        if (tfNamaLayanan != null) {
            tfNamaLayanan.clear();
        }
        if (tfDeskripsiLayanan != null) {
            tfDeskripsiLayanan.clear();
        }
        if (tfBiayaLayanan != null) {
            tfBiayaLayanan.setText("Rp. ");
        }
    }

    private void showConfirmHapus(LayananCheckUp layanan) {
        String title, message, buttonText;
        if ("Aktif".equals(layanan.getStatus())) {
            title = "Konfirmasi Hapus";
            message = "Layanan ini akan dihapus. \nApakah Anda yakin ingin menonaktifkan layanan ini?";
            buttonText = "Nonaktifkan";
        } else {
            title = "Konfirmasi Hapus Permanen";
            message = "PERINGATAN: Layanan ini akan dihapus secara PERMANEN dan tidak dapat dikembalikan!\n\nApakah Anda yakin ingin menghapus layanan ini secara permanen?";
            buttonText = "Hapus Permanen";
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (tabelLayanan != null && tabelLayanan.getScene() != null) {
            alert.initOwner(tabelLayanan.getScene().getWindow());
        }
        ButtonType buttonYes = new ButtonType(buttonText, ButtonBar.ButtonData.YES);
        ButtonType buttonNo = new ButtonType("Batal", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(buttonYes, buttonNo);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == buttonYes) {
            hapusLayanan(layanan, "Aktif".equals(layanan.getStatus()));
        }
    }

    private void hapusLayanan(LayananCheckUp layanan, boolean isSoftDelete) {
        try {
            DBConnect db = new DBConnect();
            String sql;
            PreparedStatement pst;

            if (isSoftDelete) {
                sql = "UPDATE LayananCheckUp SET Status = 'Tidak Aktif' WHERE IDLayanan = ?";
                pst = db.conn.prepareStatement(sql);
                pst.setString(1, layanan.getIDLayanan());
                pst.executeUpdate();
                layanan.setStatus("Tidak Aktif");

                showSuccessAlert("Layanan berhasil dihapus!");
            } else {
                sql = "DELETE FROM LayananCheckUp WHERE IDLayanan = ?";
                pst = db.conn.prepareStatement(sql);
                pst.setString(1, layanan.getIDLayanan());
                pst.executeUpdate();
                dataLayanan.remove(layanan);
                showSuccessAlert("Layanan berhasil dihapus secara permanen!");
            }
            pst.close();
            db.conn.close();
            if (tabelLayanan != null) {
                tabelLayanan.refresh();
            }
            applyFilters();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Gagal memproses data: " + ex.getMessage());
        }
    }

    private void simpanData() {
        String nama = tfNamaLayanan != null ? tfNamaLayanan.getText() : "";
        String deskripsi = tfDeskripsiLayanan != null ? tfDeskripsiLayanan.getText() : "";
        String biayaText = tfBiayaLayanan != null ? tfBiayaLayanan.getText() : "";
        String status = tfStatusLayanan != null ? tfStatusLayanan.getText() : "";
        if (nama.isEmpty() || deskripsi.isEmpty() || biayaText.equals("Rp. ") || status == null) {
            showAlert("Semua field harus diisi!");
            return;
        }
        double biayaValue = extractNumber(biayaText);
        if (biayaValue <= 0) {
            showAlert("Biaya layanan harus diisi dengan angka yang valid!");
            return;
        }
        String biaya = String.valueOf((long)biayaValue);
        try {
            DBConnect db = new DBConnect();
            Connection conn = db.conn;
            if (selectedLayanan != null) {
                // Update existing record
                String sql = "UPDATE LayananCheckUp SET NamaLayanan=?, Deskripsi=?, BiayaLayanan=?, Status=? WHERE IDLayanan=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, nama);
                pst.setString(2, deskripsi);
                pst.setString(3, biaya);
                pst.setString(4, status);
                pst.setString(5, selectedLayanan.getIDLayanan());
                pst.executeUpdate();
                pst.close();
                showSuccessAlert("Data layanan berhasil diperbarui!");
            } else {
                String newId = tfIDLayanan != null ? tfIDLayanan.getText() : "";
                String sql = "INSERT INTO LayananCheckUp (IDLayanan, NamaLayanan, Deskripsi, BiayaLayanan, Status) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, newId);
                pst.setString(2, nama);
                pst.setString(3, deskripsi);
                pst.setString(4, biaya);
                pst.setString(5, status);
                pst.executeUpdate();
                pst.close();
                showSuccessAlert("Data layanan berhasil ditambahkan!");
            }
            conn.close();

            getDataLayanan();
            applyFilters();
            clearForm();
            tfIDLayanan.setText(generateNewId()); // Generate ID baru
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Gagal menyimpan data: " + e.getMessage());
        }
    }

    private void clearForm() {
        clearFormFields();
        tfIDLayanan.setText(generateNewId());
        tfStatusLayanan.setText("Aktif");
        selectedLayanan = null;
    }

    private String generateNewId() {
        int maxId = 0;

        for (LayananCheckUp layanan : dataLayanan) {
            String id = layanan.getIDLayanan();
            if (id.startsWith("LYN")) {
                try {
                    int numId = Integer.parseInt(id.substring(3));
                    if (numId > maxId) {
                        maxId = numId;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return "LYN" + String.format("%03d", maxId + 1);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Peringatan");
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (tabelLayanan != null && tabelLayanan.getScene() != null) {
            alert.initOwner(tabelLayanan.getScene().getWindow());
        }

        alert.initModality(Modality.NONE);
        alert.show();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sukses");
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (tabelLayanan != null && tabelLayanan.getScene() != null) {
            alert.initOwner(tabelLayanan.getScene().getWindow());
        }

        alert.initModality(Modality.NONE);
        alert.show();
    }
}