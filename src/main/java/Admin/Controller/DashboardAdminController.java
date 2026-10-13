package Admin.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.event.ActionEvent;
import java.io.IOException;
import Connection.DBConnect;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardAdminController {

    @FXML private Button btnBeranda;
    @FXML private Button btnKaryawan;
    @FXML private Button btnSupplier;
    @FXML private Button btnKategoriObat;
    @FXML private Button btnDataObat;
    @FXML private Button btnLayananCheckUp;
    @FXML private Button btnKeluar;
    @FXML private Label labelTotalKaryawan;
    @FXML private Label labelTotalSupplier;
    @FXML private Label labelTotalObat;
    @FXML private Label labelTotalCheckup;
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private Label loginTimeLabel;
    @FXML private Label loginDateLabel;
    @FXML private Label userInitial;
    @FXML private Label usernameLabel;
    @FXML private ScrollPane contentScrollPane;
    @FXML private AnchorPane mainContent;

    private Button currentActiveButton;
    private String currentUsername = "Admin";
    private String currentUserFullName = "Administrator";
    private LocalDateTime loginTime;

    @FXML
    public void initialize() {
        System.out.println("Dashboard Admin Controller initialized");
        loginTime = LocalDateTime.now();
        setupMenuHandlers();
        setupSearchField();
        setupInitialState();
        setupUserProfile();
        loadDashboardContent();
        loadDashboardStatistics();
    }

    private void setupUserProfile() {
        if (usernameLabel != null) {
            String displayName = (currentUserFullName != null && !currentUserFullName.isEmpty()) ? currentUserFullName : currentUsername;usernameLabel.setText(displayName);
        }
        if (userInitial != null) {
            String nameForInitial = (currentUserFullName != null && !currentUserFullName.isEmpty()) ? currentUserFullName : currentUsername;
            String initial = nameForInitial.length() > 0 ? nameForInitial.substring(0, 1).toUpperCase() : "A";
            userInitial.setText(initial);
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

    // Untuk mengatur siapa user yang login supaya terlihat identitasnya
    public void setLoggedInUser(String userId, String username, String fullName) {
        this.currentUsername = username != null ? username : "Admin";
        this.currentUserFullName = fullName != null ? fullName : username;
        System.out.println("Setting logged in user - ID: " + userId + ", Username: " + username + ", Full Name: " + fullName);
        setupUserProfile();
    }

    private void loadDashboardStatistics() {
        if (labelTotalKaryawan != null) {
            labelTotalKaryawan.setText(String.valueOf(getJumlahData("Karyawan")));
        }
        if (labelTotalSupplier != null) {
            labelTotalSupplier.setText(String.valueOf(getJumlahData("Supplier")));
        }
        if (labelTotalObat != null) {
            labelTotalObat.setText(String.valueOf(getJumlahData("Obat")));
        }
        if (labelTotalCheckup != null) {
            labelTotalCheckup.setText(String.valueOf(getJumlahData("LayananCheckUp")));
        }
    }

    // Untuk tampilan jumlah karyawan, dll di dashboard
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

    // Action untuk semua button
    private void setupMenuHandlers() {
        if (btnBeranda != null) {
            btnBeranda.setOnAction(e -> openBeranda());
            addHoverEffect(btnBeranda);
        }
        if (btnKaryawan != null) {
            btnKaryawan.setOnAction(e -> openKaryawan());
            addHoverEffect(btnKaryawan);
        }
        if (btnSupplier != null) {
            btnSupplier.setOnAction(e -> openSupplier());
            addHoverEffect(btnSupplier);
        }
        if (btnKategoriObat != null) {
            btnKategoriObat.setOnAction(e -> openKategoriObat());
            addHoverEffect(btnKategoriObat);
        }
        if (btnDataObat != null) {
            btnDataObat.setOnAction(e -> openDataObat());
            addHoverEffect(btnDataObat);
        }
        if (btnLayananCheckUp != null) {
            btnLayananCheckUp.setOnAction(e -> openLayananCheckUp());
            addHoverEffect(btnLayananCheckUp);
        }
        if (btnKeluar != null) {
            btnKeluar.setOnAction(event -> handleKeluar(event));
        }
    }

    @FXML
    private void handleKeluar(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Keluar");
        alert.setHeaderText("Apakah Anda yakin ingin keluar?");
        alert.setContentText("Aplikasi akan kembali ke menu Login.");
        alert.initModality(Modality.NONE);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        alert.initOwner(stage);
        alert.show();
        alert.setOnHidden(e -> {
            ButtonType result = alert.getResult();
            if (result == ButtonType.OK) {
                openLogin();
            }
        });
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
        updatePageTitle("Dashboard Admin");
        setActiveMenu(btnBeranda);
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
        if (currentActiveButton == btnKaryawan) {
            searchKaryawan(searchText);
        } else if (currentActiveButton == btnSupplier) {
            searchSupplier(searchText);
        } else if (currentActiveButton == btnDataObat) {
            searchDataObat(searchText);
        } else {
            performGlobalSearch(searchText);
        }
    }

    private void searchKaryawan(String searchText) {
        System.out.println("Searching employees for: " + searchText);
    }
    private void searchSupplier(String searchText) {
        System.out.println("Searching suppliers for: " + searchText);
    }
    private void searchDataObat(String searchText) {
        System.out.println("Searching medicines for: " + searchText);
    }
    private void performGlobalSearch(String searchText) {System.out.println("Performing global search for: " + searchText);}

    private void openBeranda() {
        try {
            System.out.println("Opening Beranda");
            setActiveMenu(btnBeranda);
            updatePageTitle("Dashboard");
            if (loadContentFromFXML("Admin/Dashboard.fxml")) {
                System.out.println("✅ DashboardAdmin.fxml loaded successfully");
            }else {
                System.out.println("⚠️ Fallback to placeholder");
                clearMainContent();
                createPlaceholderContent("Dashboard Admin", "Halaman data Admin", "Fitur: Melihat Jumlah Data Admin");
            }
            clearSearchField();
        }catch (Exception e) {
            System.out.println("Error opening Beranda: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Dashboard", e);
        }
    }

    private void openKaryawan() {
        try {
            System.out.println("Opening Karyawan");
            setActiveMenu(btnKaryawan);
            updatePageTitle("Kelola Karyawan");
            if (loadContentFromFXML("Admin/DataKaryawan.fxml")) {
                System.out.println("✅ DataKaryawan.fxml loaded successfully");
            } else {
                System.out.println("⚠️ Fallback to placeholder");
                clearMainContent();
                createPlaceholderContent("👥 Kelola Karyawan", "Halaman untuk mengelola data karyawan", "Fitur: Tambah, Edit, Hapus, dan Cari data karyawan"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Karyawan view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Karyawan", e);
        }
    }

    private void openSupplier() {
        try {
            System.out.println("Opening Supplier");
            setActiveMenu(btnSupplier);
            updatePageTitle("Kelola Supplier");
            if (loadContentFromFXML("Admin/DataSupplier.fxml")) {
                System.out.println("✅ DataSupplier.fxml loaded successfully");
            } else {
                System.out.println("⚠️ Fallback to placeholder");
                clearMainContent();
                createPlaceholderContent("🏢 Kelola Supplier", "Halaman untuk mengelola data supplier", "Fitur: Tambah, Edit, Hapus, dan Cari data supplier"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Supplier view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Supplier", e);
        }
    }

    private void openKategoriObat() {
        try {
            System.out.println("Opening Kategori Obat");
            setActiveMenu(btnKategoriObat);
            updatePageTitle("Kelola Kategori Obat");
            if (loadContentFromFXML("Admin/DataKategoriObat.fxml")) {
                System.out.println("Kategori Obat FXML loaded successfully");
            } else {
                clearMainContent();
                createPlaceholderContent("📋 Kelola Kategori Obat", "Halaman untuk mengelola kategori obat", "Fitur: Tambah, Edit, Hapus kategori obat"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Kategori Obat view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Kategori Obat", e);
        }
    }

    private void openDataObat() {
        try {
            System.out.println("Opening Data Obat");
            setActiveMenu(btnDataObat);
            updatePageTitle("Kelola Obat");
            if (loadContentFromFXML("Admin/DataObat.fxml")) {
                System.out.println("Data Obat FXML loaded successfully");
            } else {
                clearMainContent();
                createPlaceholderContent("💊 Kelola Obat", "Halaman untuk mengelola data obat", "Fitur: Tambah, Edit, Hapus, dan Cari data obat"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Data Obat view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Data Obat", e);
        }
    }

    private void openLayananCheckUp() {
        try {
            System.out.println("Opening Layanan CheckUp");
            setActiveMenu(btnLayananCheckUp);
            updatePageTitle("Kelola Layanan CheckUp");
            if (loadContentFromFXML("Admin/DataLayananCheckUp.fxml")) {
                System.out.println("Layanan CheckUp FXML loaded successfully");
            } else {
                clearMainContent();
                createPlaceholderContent("🩺 Kelola Layanan CheckUp", "Halaman untuk mengelola layanan checkup", "Fitur: Jadwal, Riwayat, dan Laporan checkup"
                );
            }
            clearSearchField();
        } catch (Exception e) {
            System.err.println("Error opening Layanan CheckUp view: " + e.getMessage());
            e.printStackTrace();
            handleViewError("Layanan CheckUp", e);
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
        if (btnKaryawan != null) btnKaryawan.setStyle(defaultStyle);
        if (btnSupplier != null) btnSupplier.setStyle(defaultStyle);
        if (btnKategoriObat != null) btnKategoriObat.setStyle(defaultStyle);
        if (btnDataObat != null) btnDataObat.setStyle(defaultStyle);
        if (btnLayananCheckUp != null) btnLayananCheckUp.setStyle(defaultStyle);
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

    private void clearSearchField() {if (searchField != null) {searchField.clear();}}
    private void loadDashboardContent() {
        if (mainContent != null) {
            mainContent.setVisible(true);
            mainContent.toFront();
        }
        refreshDashboardStats();
    }

    private void refreshDashboardStats() {System.out.println("Refreshing dashboard statistics");loadDashboardStatistics();}
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
        createPlaceholderContent("⚠️ Error Loading " + viewName, "Terjadi kesalahan saat memuat halaman " + viewName, "Error: " + e.getMessage()
        );
    }

    @Deprecated
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        setupUserProfile();
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
        updateLoginTimeDisplay();
    }
}