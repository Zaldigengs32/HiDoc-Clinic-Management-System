package Manajer.Controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import Manajer.Controller.BerandaManajerController;
import Apoteker.Controller.SessionManager; // Import SessionManager
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DashboardManajerController {

    @FXML private Button btnBeranda;
    @FXML private Button btnLaporanPenjualan;
    @FXML private Button btnLaporanPengadaan;
    @FXML private Button btnLaporanCekKesehatan;
    @FXML private Button btnKeluar;
    @FXML private Label pageTitle;
    @FXML private Label loginTimeLabel;
    @FXML private Label loginDateLabel;
    @FXML private Label usernameLabel;
    @FXML private Label userInitial;
    @FXML private ScrollPane contentScrollPane;
    @FXML private AnchorPane mainContent;
    @FXML private TextField searchField;

    // Remove hardcoded values - use SessionManager instead
    private SessionManager sessionManager;

    @FXML
    public void initialize() {
        // Initialize session manager
        sessionManager = SessionManager.getInstance();

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            System.out.println("⚠️ No active session found, redirecting to login");
            openLogin();
            return;
        }

        // Verify user role
        if (!"Manajer".equals(sessionManager.getLoggedInUserRole())) {
            System.out.println("⚠️ Access denied: User role is " + sessionManager.getLoggedInUserRole() + ", expected Manajer");
            showErrorAlert("Access Denied", "Anda tidak memiliki akses ke halaman ini.");
            openLogin();
            return;
        }

        setupUserInfo();
        setupLoginInfo();
        setupCardEventHandlers();

        // Print current session for debugging
        sessionManager.printCurrentSession();
    }

    private void setupUserInfo() {
        // Get user information from session
        String displayName = sessionManager.getDisplayName();
        String username = sessionManager.getLoggedInUserName();

        // Set username label to display name (full name if available, otherwise username)
        usernameLabel.setText(displayName != null ? displayName : "Unknown User");

        // Set user initial from display name
        if (displayName != null && !displayName.isEmpty()) {
            userInitial.setText(displayName.substring(0, 1).toUpperCase());
        } else {
            userInitial.setText("M"); // Default to M for Manager
        }

        System.out.println("✅ User info set - Display Name: " + displayName + ", Username: " + username);
    }

    private void setupLoginInfo() {
        // Get login time from session if available
        if (sessionManager.getLoginTime() != null) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

            loginTimeLabel.setText("Login: " + sessionManager.getLoginTime().format(timeFormatter) + " WIB");
            loginDateLabel.setText(sessionManager.getLoginTime().format(dateFormatter));
        } else {
            // Fallback to current time if no login time in session
            LocalTime currentTime = LocalTime.now();
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

            loginTimeLabel.setText("Login: " + currentTime.format(timeFormatter) + " WIB");
            loginDateLabel.setText(currentDate.format(dateFormatter));
        }
    }

    private void setupCardEventHandlers() {
        // Implementation for card event handlers if needed
    }

    @FXML
    private void handleBeranda() {
        try {
            System.out.println("Opening Beranda");
            setActiveMenu(btnBeranda);
            updatePageTitle("Dashboard");

            // Load BerandaManajer.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Manajer/BerandaManajer.fxml"));
            Parent berandaContent = loader.load();

            // Get the controller to access its methods if needed
            BerandaManajerController berandaController = loader.getController();

            // Clear and set the content
            if (mainContent != null) {
                mainContent.getChildren().clear();
                mainContent.getChildren().add(berandaContent);

                // Set anchor constraints
                AnchorPane.setTopAnchor(berandaContent, 0.0);
                AnchorPane.setBottomAnchor(berandaContent, 0.0);
                AnchorPane.setLeftAnchor(berandaContent, 0.0);
                AnchorPane.setRightAnchor(berandaContent, 0.0);

                System.out.println("✅ BerandaManajer.fxml loaded successfully");
            } else {
                System.out.println("⚠️ mainContent is null");
                showErrorAlert("Error", "Container tidak tersedia");
            }

            clearSearchField();

        } catch (Exception e) {
            System.out.println("Error opening Beranda: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Error", "Gagal memuat halaman Beranda: " + e.getMessage());
        }
    }

    @FXML
    private void handleLaporanPenjualan() {
        setActiveMenu(btnLaporanPenjualan);
        pageTitle.setText("Laporan Penjualan Obat");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Manajer/LaporanPenjualanObat.fxml"));
            Parent laporanPenjualan = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(laporanPenjualan);
        } catch (IOException e) {
            showErrorAlert("Error", "Gagal memuat halaman Laporan Penjualan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLaporanPengadaan() {
        setActiveMenu(btnLaporanPengadaan);
        pageTitle.setText("Laporan Pengadaan Obat");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Manajer/LaporanPengadaanObat.fxml"));
            Parent laporanPengadaan = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(laporanPengadaan);
        } catch (IOException e) {
            showErrorAlert("Error", "Gagal memuat halaman Laporan Pengadaan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLaporanCekKesehatan() {
        setActiveMenu(btnLaporanCekKesehatan);
        pageTitle.setText("Laporan Cek Kesehatan");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Manajer/LaporanCekKesehatan.fxml"));
            Parent laporanCekKesehatan = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(laporanCekKesehatan);
            AnchorPane.setTopAnchor(laporanCekKesehatan, 0.0);
            AnchorPane.setBottomAnchor(laporanCekKesehatan, 0.0);
            AnchorPane.setLeftAnchor(laporanCekKesehatan, 0.0);
            AnchorPane.setRightAnchor(laporanCekKesehatan, 0.0);
            System.out.println("Laporan Cek Kesehatan berhasil dimuat");
        } catch (IOException e) {
            showErrorAlert("Error", "Gagal memuat halaman Laporan Cek Kesehatan: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showErrorAlert("Error", "Terjadi kesalahan saat memuat laporan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePageTitle(String title) {
        if (pageTitle == null) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), pageTitle);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            pageTitle.setText(title);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), pageTitle);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    @FXML
    private void handleKeluar() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner((Stage) btnKeluar.getScene().getWindow());
        alert.setTitle("Konfirmasi Keluar");
        alert.setHeaderText("Apakah Anda yakin ingin keluar?");
        alert.setContentText("Anda akan keluar dari sistem dan kembali ke halaman login.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Clear session when logging out
            sessionManager.clearSession();
            System.out.println("🚪 Manager logged out, session cleared");

            openLogin();
        }
    }


    private void openLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Interface/MenuLogin.fxml"));
            Parent root = loader.load();
            Stage newStage = new Stage();
            newStage.setTitle("Login");
            newStage.setScene(new Scene(root));
            newStage.setFullScreenExitHint("");
            newStage.setFullScreen(true);
            newStage.show();
            Stage currentStage = (Stage) btnKeluar.getScene().getWindow();
            currentStage.close();
            System.out.println("Login page opened in new window.");
        } catch (IOException e) {
            System.err.println("Failed to open Login page: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Login", e);
        }
    }

    private boolean loadContentFromFXML(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlPath));
            Parent content = loader.load();
            if (mainContent != null) {
                mainContent.getChildren().setAll(content);
                AnchorPane.setTopAnchor(content, 0.0);
                AnchorPane.setBottomAnchor(content, 0.0);
                AnchorPane.setLeftAnchor(content, 0.0);
                AnchorPane.setRightAnchor(content, 0.0);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error loading FXML: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void handleViewError(String viewName, Exception e) {
        clearMainContent();
        createPlaceholderContent("⚠️ Error Loading " + viewName, "Terjadi kesalahan saat memuat halaman " + viewName, "Error: " + e.getMessage());
    }

    private void createPlaceholderContent(String title, String description, String features) {
        try {
            VBox container = new VBox();
            container.setLayoutX(40);
            container.setLayoutY(40);
            container.setPrefWidth(1020);
            container.setSpacing(20);
            container.setStyle("-fx-background-color: rgba(255,255,255,0.9); " + "-fx-background-radius: 20; " + "-fx-border-radius: 20; " + "-fx-padding: 40; " + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 10);");

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 36px; " + "-fx-font-weight: bold; " + "-fx-text-fill: #1e293b;");

            Label descLabel = new Label(description);
            descLabel.setStyle("-fx-font-size: 18px; " + "-fx-text-fill: #64748b;");
            descLabel.setWrapText(true);

            Label featuresLabel = new Label(features);
            featuresLabel.setStyle("-fx-font-size: 16px; " + "-fx-text-fill: #475569; " + "-fx-font-style: italic;");
            featuresLabel.setWrapText(true);

            Label comingSoonLabel = new Label("🚧 Halaman dalam pengembangan");
            comingSoonLabel.setStyle("-fx-font-size: 14px; " + "-fx-text-fill: #f59e0b; " + "-fx-font-weight: bold;");

            container.getChildren().addAll(titleLabel, descLabel, featuresLabel, comingSoonLabel);

            if (mainContent != null) {
                mainContent.getChildren().clear();
                mainContent.getChildren().add(container);
            }
        } catch (Exception e) {
            System.err.println("Error creating placeholder content: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearMainContent() {
        if (mainContent != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), mainContent);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                mainContent.getChildren().clear();
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), mainContent);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    private void clearSearchField() {
        if (searchField != null) {
            searchField.clear();
        }
    }

    private void setActiveMenu(Button activeButton) {
        resetAllMenuButtons();
        activeButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand;"
        );
    }

    private void resetAllMenuButtons() {
        String inactiveStyle =
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: rgba(255,255,255,0.9); " +
                        "-fx-font-size: 16px; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-cursor: hand;";
        btnBeranda.setStyle(inactiveStyle);
        btnLaporanPenjualan.setStyle(inactiveStyle);
        btnLaporanPengadaan.setStyle(inactiveStyle);
        btnLaporanCekKesehatan.setStyle(inactiveStyle);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Updated getter methods to use SessionManager
    public String getCurrentUsername() {
        return sessionManager.getLoggedInUserName();
    }

    public String getCurrentUserRole() {
        return sessionManager.getLoggedInUserRole();
    }

    public String getCurrentUserFullName() {
        return sessionManager.getLoggedInUserFullName();
    }

    public String getCurrentUserId() {
        return sessionManager.getLoggedInUserId();
    }

    // Remove the setUserInfo method as it's no longer needed
    // User info is now managed through SessionManager
}