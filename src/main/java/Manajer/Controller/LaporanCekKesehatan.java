package Manajer.Controller;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class LaporanCekKesehatan implements Initializable {

    @FXML private ComboBox<String> comboBoxDariBulan;
    @FXML private ComboBox<String> comboBoxSampaiBulan;
    @FXML private ComboBox<Integer> comboBoxTahun;
    @FXML private LineChart<String, Number> chartPendapatan;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Label labelTotalPendapatan;
    @FXML private Label labelRataRata;
    @FXML private Label labelJumlahTransaksi;
    @FXML private Button btnExportPDF;
    @FXML private Button btnRefresh;

    private Map<Integer, Integer> transaksiPerBulanGlobal = new HashMap<>();
    private DBConnect dbConnect;
    private NumberFormat currencyFormatter;
    private String[] namaBulan = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        ObservableList<String> bulanList = FXCollections.observableArrayList(namaBulan);
        comboBoxDariBulan.setItems(bulanList);
        comboBoxSampaiBulan.setItems(bulanList);
        comboBoxDariBulan.setValue("Januari");
        comboBoxSampaiBulan.setValue("Desember");
        btnRefresh.setOnAction(e -> handleRefresh());
        btnExportPDF.setOnAction(e -> handleExportExcel());

        ObservableList<Integer> tahunList = FXCollections.observableArrayList();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear; i++) {
            tahunList.add(i);
        }
        comboBoxTahun.setItems(tahunList);
        comboBoxTahun.setValue(currentYear);

        xAxis.setLabel("Bulan");
        yAxis.setLabel("Pendapatan (Rp)");
        chartPendapatan.setTitle("");
        chartPendapatan.setLegendVisible(false);

        setupEventListeners();
        try {
            dbConnect = new DBConnect();
            if (dbConnect.conn != null) {
                System.out.println("Database connection untuk laporan cek kesehatan berhasil");
            }
        } catch (Exception e) {
            showAlert("Error", "Gagal terhubung ke database: " + e.getMessage());
        }
        loadLaporanData();
    }

    private void setupEventListeners() {
        comboBoxDariBulan.setOnAction(e -> loadLaporanData());
        comboBoxSampaiBulan.setOnAction(e -> loadLaporanData());
        comboBoxTahun.setOnAction(e -> loadLaporanData());
    }

    @FXML
    private void handleExportExcel() {
        try {
            if (chartPendapatan.getData().isEmpty()) {
                showAlert("Warning", "Tidak ada data untuk diekspor. Silakan tampilkan laporan terlebih dahulu.");
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Simpan Laporan Excel");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );
            fileChooser.setInitialFileName("Laporan_Cek_Kesehatan_" +
                    comboBoxTahun.getValue() + ".xlsx");

            File file = fileChooser.showSaveDialog(btnExportPDF.getScene().getWindow());
            if (file != null) {
                generateExcelReport(file);
                showAlert("Success", "Laporan berhasil diekspor ke: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert("Error", "Gagal mengekspor Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateExcelReport(File file) throws IOException, SQLException {
        Workbook workbook = new XSSFWorkbook();
        Sheet summarySheet = workbook.createSheet("Ringkasan");
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        int rowIdx = 0;

        Row titleRow = summarySheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("LAPORAN CEK KESEHATAN");
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        Row periodeRow = summarySheet.createRow(rowIdx++);
        periodeRow.createCell(0).setCellValue("Periode: " + comboBoxDariBulan.getValue() + " - " + comboBoxSampaiBulan.getValue() + " " + comboBoxTahun.getValue());

        rowIdx++;

        Row ringkasanRow1 = summarySheet.createRow(rowIdx++);
        ringkasanRow1.createCell(0).setCellValue("Total Pendapatan: " + labelTotalPendapatan.getText());
        Row ringkasanRow2 = summarySheet.createRow(rowIdx++);
        ringkasanRow2.createCell(0).setCellValue("Rata-rata per Bulan: " + labelRataRata.getText());
        Row ringkasanRow3 = summarySheet.createRow(rowIdx++);
        ringkasanRow3.createCell(0).setCellValue("Total Transaksi: " + labelJumlahTransaksi.getText());

        rowIdx++;

        Row headerRow = summarySheet.createRow(rowIdx++);
        headerRow.createCell(0).setCellValue("Bulan");
        headerRow.createCell(1).setCellValue("Pendapatan");
        headerRow.createCell(2).setCellValue("Jumlah Transaksi");
        for (int i = 0; i < 3; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        if (!chartPendapatan.getData().isEmpty()) {
            XYChart.Series<String, Number> series = chartPendapatan.getData().get(0);
            for (XYChart.Data<String, Number> data : series.getData()) {
                Row dataRow = summarySheet.createRow(rowIdx++);
                dataRow.createCell(0).setCellValue(data.getXValue());
                dataRow.createCell(1).setCellValue(formatCurrency(data.getYValue().doubleValue()));

                int bulanIndex = getBulanIndex(data.getXValue()) + 1;
                int jumlahTransaksi = transaksiPerBulanGlobal.getOrDefault(bulanIndex, 0);
                dataRow.createCell(2).setCellValue(jumlahTransaksi);
            }
        } else {
            Row dataRow = summarySheet.createRow(rowIdx++);
            dataRow.createCell(0).setCellValue("Tidak ada data");
            dataRow.createCell(1).setCellValue("-");
            dataRow.createCell(2).setCellValue("-");
        }

        for (int i = 0; i < 3; i++) {
            summarySheet.autoSizeColumn(i);
        }

        int dariBulanIndex = getBulanIndex(comboBoxDariBulan.getValue());
        int sampaiBulanIndex = getBulanIndex(comboBoxSampaiBulan.getValue());

        for (int i = dariBulanIndex; i <= sampaiBulanIndex; i++) {
            int bulanNumber = i + 1;
            String namaBulanSheet = namaBulan[i];
            String query = "SELECT d.IDCheckUp, l.NamaLayanan, d.Kuantitas, d.SubTotal " +
                    "FROM DetailTransaksiCheckUp d " +
                    "JOIN LayananCheckUp l ON d.IDLayanan = l.IDLayanan " +
                    "JOIN TransaksiCheckUp t ON d.IDCheckUp = t.IDCheckUp " +
                    "WHERE YEAR(t.TanggalTransaksi) = ? AND MONTH(t.TanggalTransaksi) = ?";

            PreparedStatement stmt = dbConnect.conn.prepareStatement(query);
            stmt.setInt(1, comboBoxTahun.getValue());
            stmt.setInt(2, bulanNumber);
            ResultSet rs = stmt.executeQuery();
            boolean hasData = false;
            Sheet bulanSheet = workbook.createSheet(namaBulanSheet);
            int rowDetail = 0;
            Row titleRowDetail = bulanSheet.createRow(rowDetail++);
            Cell titleCellDetail = titleRowDetail.createCell(0);
            titleCellDetail.setCellValue("Detail Check Up - " + namaBulanSheet + " " + comboBoxTahun.getValue());
            titleCellDetail.setCellStyle(titleStyle);
            bulanSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            rowDetail++;

            Row headerDetail = bulanSheet.createRow(rowDetail++);
            headerDetail.createCell(0).setCellValue("ID CheckUp");
            headerDetail.createCell(1).setCellValue("Nama Layanan");
            headerDetail.createCell(2).setCellValue("Kuantitas");
            headerDetail.createCell(3).setCellValue("Subtotal");

            for (int col = 0; col < 4; col++) {
                headerDetail.getCell(col).setCellStyle(headerStyle);
            }

            while (rs.next()) {
                hasData = true;
                Row dataRow = bulanSheet.createRow(rowDetail++);
                dataRow.createCell(0).setCellValue(rs.getString("IDCheckUp"));
                dataRow.createCell(1).setCellValue(rs.getString("NamaLayanan"));
                dataRow.createCell(2).setCellValue(rs.getInt("Kuantitas"));
                dataRow.createCell(3).setCellValue(formatCurrency(rs.getDouble("SubTotal")));
            }

            for (int col = 0; col < 4; col++) {
                bulanSheet.autoSizeColumn(col);
            }

            rs.close();
            stmt.close();

            if (!hasData) {
                int index = workbook.getSheetIndex(bulanSheet);
                workbook.removeSheetAt(index);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    @FXML
    private void handleRefresh() {
        try {
            comboBoxDariBulan.setValue("Januari");
            comboBoxSampaiBulan.setValue("Desember");
            comboBoxTahun.setValue(LocalDate.now().getYear());
            chartPendapatan.getData().clear();
            labelTotalPendapatan.setText("Rp 0");
            labelRataRata.setText("Rp 0");
            labelJumlahTransaksi.setText("0");
            loadLaporanData();
        } catch (Exception e) {
            showAlert("Error", "Gagal refresh data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLaporanData() {
        try {
            String dariBulan = comboBoxDariBulan.getValue();
            String sampaiBulan = comboBoxSampaiBulan.getValue();
            Integer tahun = comboBoxTahun.getValue();

            if (dariBulan == null || sampaiBulan == null || tahun == null) {
                showAlert("Warning", "Silakan pilih periode yang valid");
                return;
            }

            int dariBulanIndex = getBulanIndex(dariBulan);
            int sampaiBulanIndex = getBulanIndex(sampaiBulan);

            if (dariBulanIndex > sampaiBulanIndex) {
                showAlert("Warning", "Bulan 'Dari' tidak boleh lebih besar dari bulan 'Sampai'");
                return;
            }
            chartPendapatan.getData().clear();
            transaksiPerBulanGlobal.clear();
            String query = "SELECT MONTH(TanggalTransaksi) as bulan, SUM(TotalHarga) as total_pendapatan, COUNT(*) as jumlah_transaksi " +
                    "FROM TransaksiCheckUp " +
                    "WHERE YEAR(TanggalTransaksi) = ? AND MONTH(TanggalTransaksi) BETWEEN ? AND ? " +
                    "GROUP BY MONTH(TanggalTransaksi) " +
                    "ORDER BY MONTH(TanggalTransaksi)";

            PreparedStatement statement = dbConnect.conn.prepareStatement(query);
            statement.setInt(1, tahun);
            statement.setInt(2, dariBulanIndex + 1);
            statement.setInt(3, sampaiBulanIndex + 1);
            ResultSet resultSet = statement.executeQuery();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Pendapatan");
            Map<Integer, Double> pendapatanPerBulan = new HashMap<>();
            Map<Integer, Integer> transaksiPerBulan = new HashMap<>();
            double totalPendapatan = 0;
            int totalTransaksi = 0;

            while (resultSet.next()) {
                int bulan = resultSet.getInt("bulan");
                double pendapatan = resultSet.getDouble("total_pendapatan");
                int jumlahTransaksi = resultSet.getInt("jumlah_transaksi");

                pendapatanPerBulan.put(bulan, pendapatan);
                transaksiPerBulan.put(bulan, jumlahTransaksi);
                transaksiPerBulanGlobal.put(bulan, jumlahTransaksi);
                totalPendapatan += pendapatan;
                totalTransaksi += jumlahTransaksi;
                series.getData().add(new XYChart.Data<>(namaBulan[bulan - 1], pendapatan));
            }

            for (int i = dariBulanIndex; i <= sampaiBulanIndex; i++) {
                if (!pendapatanPerBulan.containsKey(i + 1)) {
                    series.getData().add(new XYChart.Data<>(namaBulan[i], 0));
                    transaksiPerBulan.put(i + 1, 0);
                    transaksiPerBulanGlobal.put(i + 1, 0);
                }
            }

            series.getData().sort((a, b) -> {
                int indexA = getBulanIndex(a.getXValue());
                int indexB = getBulanIndex(b.getXValue());
                return Integer.compare(indexA, indexB);
            });
            chartPendapatan.getData().add(series);
            javafx.application.Platform.runLater(() -> {
                addTooltipsToChart(transaksiPerBulan);
            });

            labelTotalPendapatan.setText(formatCurrency(totalPendapatan));
            int jumlahBulan = sampaiBulanIndex - dariBulanIndex + 1;
            double rataRata = jumlahBulan > 0 ? totalPendapatan / jumlahBulan : 0;
            labelRataRata.setText(formatCurrency(rataRata));
            labelJumlahTransaksi.setText(String.valueOf(totalTransaksi));

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            showAlert("Error", "Gagal memuat data: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Error", "Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addTooltipsToChart(Map<Integer, Integer> transaksiPerBulan) {
        for (XYChart.Series<String, Number> series : chartPendapatan.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    String bulan = data.getXValue();
                    double pendapatan = data.getYValue().doubleValue();
                    int bulanIndex = getBulanIndex(bulan) + 1;
                    int jumlahTransaksi = transaksiPerBulan.getOrDefault(bulanIndex, 0);

                    Tooltip tooltip = new Tooltip(
                            "Bulan: " + bulan + "\n" +
                                    "Pendapatan: " + formatCurrency(pendapatan) + "\n" +
                                    "Jumlah Check Up: " + jumlahTransaksi
                    );

                    tooltip.setStyle(
                            "-fx-background-color: rgba(0, 0, 0, 0.8); " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-padding: 10;"
                    );

                    Tooltip.install(node, tooltip);
                    node.setOnMouseEntered(e -> {
                        node.setStyle("-fx-background-color: #ff6b6b; -fx-background-radius: 6;");
                        node.setScaleX(1.3);
                        node.setScaleY(1.3);
                    });

                    node.setOnMouseExited(e -> {
                        node.setStyle("");
                        node.setScaleX(1.0);
                        node.setScaleY(1.0);
                    });
                }
            }
        }
    }

    private int getBulanIndex(String namaBulan) {
        for (int i = 0; i < this.namaBulan.length; i++) {
            if (this.namaBulan[i].equals(namaBulan)) {
                return i;
            }
        }
        return 0;
    }

    private String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###,###");
        return "Rp " + formatter.format(amount);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (btnRefresh.getScene() != null && btnRefresh.getScene().getWindow() != null) {
            alert.initOwner(btnRefresh.getScene().getWindow());
        }

        alert.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);" +
                        "-fx-border-radius: 15;" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 20;" +
                        "-fx-font-size: 14px;"
        );

        alert.showAndWait();
    }
}