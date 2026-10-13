package Apoteker.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.animation.FadeTransition;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.io.IOException;
import Connection.DBConnect;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class DashboardApotekerController {

    @FXML private Button btnBeranda;
    @FXML private Button btnCekKesehatan;
    @FXML private Button btnKeluar;
    @FXML private Button btnPengadaanObat;
    @FXML private Button btnPenjualanObat;
    @FXML private Label labelTotalCekKesehatan;
    @FXML private Label labelTotalPengadaanObat;
    @FXML private Label labelTotalPenjualanObat;
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private Label loginTimeLabel;
    @FXML private Label loginDateLabel;
    @FXML private Label userInitial;
    @FXML private Label usernameLabel;
    @FXML private ScrollPane contentScrollPane;
    @FXML private AnchorPane mainContent;

    private Button currentActiveButton;
    private String currentUsername = "Apotek";
    private String currentUserFullName;
    private LocalDateTime loginTime;

    @FXML
    public void initialize() {
        System.out.println("Dashboard Apoteker Controller initialized");
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            currentUsername = sessionManager.getLoggedInUserName();
            currentUserFullName = sessionManager.getLoggedInUserFullName();
            loginTime = sessionManager.getLoginTime();
            System.out.println("✅ User data loaded from session:");
            System.out.println("   Username: " + currentUsername);
            System.out.println("   Full Name: " + currentUserFullName);
        } else {
            System.out.println("⚠️ No user logged in, using default values");
            loginTime = LocalDateTime.now();
        }

        setupMenuHandlers();
        setupSearchField();
        setupInitialState();
        setupUserProfile();
        loadDashboardContent();
        loadDashboardStatistics();
        openBeranda();
    }

    private void setupUserProfile() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isLoggedIn()) {
            String displayName = sessionManager.getDisplayName();
            String nameForInitial = sessionManager.getLoggedInUserFullName() != null ?
                    sessionManager.getLoggedInUserFullName() :
                    sessionManager.getLoggedInUserName();
            if (usernameLabel != null) {
                usernameLabel.setText(displayName);
            }
            if (userInitial != null) {
                String initial = nameForInitial != null && nameForInitial.length() > 0 ?
                        nameForInitial.substring(0, 1).toUpperCase() : "A";
                userInitial.setText(initial);
            }
            if (sessionManager.getLoginTime() != null) {
                loginTime = sessionManager.getLoginTime();
            }
        } else {
            if (usernameLabel != null) {
                usernameLabel.setText("Apotek");
            }
            if (userInitial != null) {
                userInitial.setText("A");
            }
        }
        updateLoginTimeDisplay();
    }

    private void updateLoginTimeDisplay() {
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", new Locale("id", "ID"));
        String timeText = "Login: " + loginTime.format(timeFormatter) + " WIB";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));
        String dateText = loginTime.format(dateFormatter);
        if (loginTimeLabel != null) {
            loginTimeLabel.setText(timeText);
        }
        if (loginDateLabel != null) {
            loginDateLabel.setText(dateText);
        }
    }

    private void loadDashboardStatistics() {
        if (labelTotalPenjualanObat != null) {
            labelTotalPenjualanObat.setText(String.valueOf(getJumlahData("Karyawan")));
        }
        if (labelTotalPengadaanObat != null) {
            labelTotalPengadaanObat.setText(String.valueOf(getJumlahData("Supplier")));
        }
        if (labelTotalCekKesehatan != null) {
            labelTotalCekKesehatan.setText(String.valueOf(getJumlahData("Obat")));
        }
    }

    private int getJumlahData(String namaTabel) {
        int jumlah = 0;
        DBConnect db = new DBConnect();
        String query = "SELECT COUNT(*) FROM " + namaTabel;
        try {
            db.result = db.stat.executeQuery(query);
            if (db.result.next()) {
                jumlah = db.result.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting data count for " + namaTabel + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (db.result != null) db.result.close();
                if (db.stat != null) db.stat.close();
                if (db.conn != null) db.conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return jumlah;
    }

    private void setupMenuHandlers() {
        if (btnBeranda != null) {
            btnBeranda.setOnAction(e -> openBeranda());
            addHoverEffect(btnBeranda);
        }
        if (btnPenjualanObat != null) {
            btnPenjualanObat.setOnAction(e -> openPenjualanObat());
            addHoverEffect(btnPenjualanObat);
        }
        if (btnPengadaanObat != null) {
            btnPengadaanObat.setOnAction(e -> openPengadaanObat());
            addHoverEffect(btnPengadaanObat);
        }
        if (btnCekKesehatan != null) {
            btnCekKesehatan.setOnAction(e -> openCekKesehatan());
            addHoverEffect(btnCekKesehatan);
        }
        if (btnKeluar != null) {
            btnKeluar.setOnAction(event -> handleKeluar());
        }
    }

    @FXML
    private void handleKeluar() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Keluar");
        alert.setHeaderText("Apakah Anda yakin ingin keluar?");
        alert.setContentText("Aplikasi akan kembali ke menu Login.");
        Stage currentStage = (Stage) Stage.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);
        if (currentStage != null) {
            alert.initOwner(currentStage);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SessionManager.getInstance().clearSession();
            openLogin();
        }
    }

    private void setupSearchField() {
        if (searchField != null) {
            searchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    handleSearch();
                }
            });
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            });
        }
    }

    private void setupInitialState() {
        if (contentScrollPane != null) {
            contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            contentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }
    }


    private void addHoverEffect(Button button) {
        if (button == null) return;
        button.setOnMouseEntered(e -> {
            if (button != currentActiveButton) {
                button.setStyle(getHoverStyle());
            }
        });
        button.setOnMouseExited(e -> {
            if (button != currentActiveButton) {
                button.setStyle(getDefaultStyle());
            }
        });
    }

    private void handleSearch() {
        if (searchField == null) return;
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            System.out.println("Search field is empty");
            return;
        }
        System.out.println("Searching for: " + searchText);
        performSearch(searchText);
    }

    private void performSearch(String searchText) {
        if (currentActiveButton == btnPenjualanObat) {
            searchPenjualanObat(searchText);
        } else if (currentActiveButton == btnPengadaanObat) {
            searchPengadaanObat(searchText);
        } else if (currentActiveButton == btnCekKesehatan) {
            searchCekKesehatan(searchText);
        } else {
            performGlobalSearch(searchText);
        }
    }

    private void searchPenjualanObat(String searchText) {
        System.out.println("Searching employees for: " + searchText);
    }
    private void searchPengadaanObat(String searchText) {
        System.out.println("Searching suppliers for: " + searchText);
    }
    private void searchCekKesehatan(String searchText) {
        System.out.println("Searching medicines for: " + searchText);
    }
    private void performGlobalSearch(String searchText) {
        System.out.println("Performing global search for: " + searchText);
    }

    private void openBeranda() {
        try {
            System.out.println("Opening Beranda");
            setActiveMenu(btnBeranda);
            updatePageTitle("Dashboard Apoteker");
            if (loadContentFromFXML("Apoteker/BerandaApoteker.fxml")) {
                System.out.println("✅ BerandaApoteker.fxml loaded successfully");
            }else {
                System.out.println("⚠️ Fallback to placeholder");
                clearMainContent();
                createPlaceholderContent("Dashboard Apoteker", "Halaman data Admin", "Fitur: Melihat Jumlah Data Admin");
            }
            clearSearchField();
        }catch (Exception e) {
            System.out.println("Error opening Beranda: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Dashboard", e);
        }
    }

    private void openPenjualanObat() {
        try {
            System.out.println("Opening Penjualan Obat");
            setActiveMenu(btnPenjualanObat);
            updatePageTitle("Kelola Penjualan Obat");
            if (loadContentFromFXML("Apoteker/DataPenjualanObat.fxml")) {
                System.out.println("✅ DataPenjualanObat.fxml loaded successfully");
            } else {
                System.out.println("⚠️ Fallback to placeholder");
                clearMainContent();
                createPlaceholderContent(
                        "👥 Kelola Data Penjualan Obat",
                        "Halaman untuk mengelola data penjualan obat",
                        "Fitur: Tambah, Edit, Hapus, dan Cari data penjualan obat"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Penjualan Obat view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Penjualan Obat", e);
        }
    }

    private void openPengadaanObat() {
        try {
            System.out.println("Opening Pengadaan Obat");
            setActiveMenu(btnPengadaanObat);
            updatePageTitle("Kelola Pengadaan Obat");
            if (loadContentFromFXML("Apoteker/DataPengadaanObat.fxml")) {
                System.out.println("✅ DataPengadaanObat.fxml loaded successfully");
            } else {
                System.out.println("⚠️ Fallback to placeholder");
                clearMainContent();
                createPlaceholderContent(
                        "🏢 Kelola Data Pengadaan Obat",
                        "Halaman untuk mengelola data pengadaan obat",
                        "Fitur: Tambah, Edit, Hapus, dan Cari data pengadaan obat"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Pengadaan Obat view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Pengadaan Obat", e);
        }
    }

    private void openCekKesehatan() {
        try {
            System.out.println("Opening Cek Kesehatan");
            setActiveMenu(btnCekKesehatan);
            updatePageTitle("Kelola Cek Kesehatan");
            if (loadContentFromFXML("Apoteker/DataCekKesehatan.fxml")) {
                System.out.println("Cek Kesehatan FXML loaded successfully");
            } else {
                clearMainContent();
                createPlaceholderContent(
                        "📋 Kelola Data Cek Kesehatan",
                        "Halaman untuk mengelola cek kesehatan",
                        "Fitur: Tambah, Edit, Hapus cek kesehatan"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Cek Kesehatan view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Cek Kesehatan", e);
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

    private void setActiveMenu(Button activeButton) {
        resetAllMenuStyles();
        currentActiveButton = activeButton;
        if (activeButton != null) {
            activeButton.setStyle(getActiveStyle());
        }
    }

    private void resetAllMenuStyles() {
        String defaultStyle = getDefaultStyle();
        if (btnBeranda != null) btnBeranda.setStyle(defaultStyle);
        if (btnPenjualanObat != null) btnPenjualanObat.setStyle(defaultStyle);
        if (btnPengadaanObat != null) btnPengadaanObat.setStyle(defaultStyle);
        if (btnCekKesehatan != null) btnCekKesehatan.setStyle(defaultStyle);
    }

    private String getDefaultStyle() {return "-fx-background-color: transparent; " + "-fx-text-fill: rgba(255,255,255,0.9); " + "-fx-font-size: 16px; " + "-fx-background-radius: 15; " + "-fx-border-radius: 15; " + "-fx-cursor: hand;";}
    private String getActiveStyle() {return "-fx-background-color: rgba(255,255,255,0.2); " + "-fx-text-fill: white; " + "-fx-font-size: 16px; " + "-fx-font-weight: bold; " + "-fx-background-radius: 15; " + "-fx-border-radius: 15; " + "-fx-cursor: hand; " + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3.5);";}
    private String getHoverStyle() {return "-fx-background-color: rgba(255,255,255,0.1); " + "-fx-text-fill: white; " + "-fx-font-size: 16px; " + "-fx-background-radius: 15; " + "-fx-border-radius: 15; " + "-fx-cursor: hand; " + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);";}
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

    private void loadDashboardContent() {
        if (mainContent != null) {
            mainContent.setVisible(true);
            mainContent.toFront();
        }
        refreshDashboardStats();
    }

    private void refreshDashboardStats() {
        System.out.println("Refreshing dashboard statistics");
        loadDashboardStatistics();
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
            System.out.println("Data Obat page opened in new window.");
        } catch (IOException e) {
            System.err.println("Failed to open Data Obat page: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Data Obat", e);
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
        createPlaceholderContent("⚠️ Error Loading " + viewName, "Terjadi kesalahan saat memuat halaman " + viewName, "Error: " + e.getMessage()
        );
    }
}