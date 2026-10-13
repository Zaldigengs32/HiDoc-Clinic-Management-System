package Apoteker.Controller;

import Connection.DBConnect;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class BerandaApotekerController implements Initializable {

    @FXML private Label labelTotalKaryawan;
    @FXML private Label labelTotalSupplier;
    @FXML private Label labelTotalObat;
    private DBConnect dbConnect;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dbConnect = new DBConnect();
        loadDashboardData();
    }

    private void loadDashboardData() {
        loadTotalPengadaanObat();
        loadTotalPenjualanObat();
        loadTotalCheckUp();
    }

    private void loadTotalPengadaanObat() {
        try {
            String query = "SELECT COUNT(*) as total FROM TransaksiPengadaanObat";
            ResultSet rs = dbConnect.stat.executeQuery(query);
            if (rs.next()) {
                int total = rs.getInt("total");
                labelTotalKaryawan.setText(String.valueOf(total));
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error loading total pengadaan obat: " + e.getMessage());
            labelTotalKaryawan.setText("0");
        }
    }

    private void loadTotalPenjualanObat() {
        try {
            String query = "SELECT COUNT(*) as total FROM TransaksiPenjualanObat";
            ResultSet rs = dbConnect.stat.executeQuery(query);
            if (rs.next()) {
                int total = rs.getInt("total");
                labelTotalSupplier.setText(String.valueOf(total));
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error loading total penjualan obat: " + e.getMessage());
            labelTotalSupplier.setText("0");
        }
    }

    private void loadTotalCheckUp() {
        try {
            String query = "SELECT COUNT(*) as total FROM TransaksiCheckUp";
            ResultSet rs = dbConnect.stat.executeQuery(query);
            if (rs.next()) {
                int total = rs.getInt("total");
                labelTotalObat.setText(String.valueOf(total));
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error loading total checkup: " + e.getMessage());
            labelTotalObat.setText("0");
        }
    }
}