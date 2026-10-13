package Interface;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import Connection.DBConnect;
import Apoteker.Controller.SessionManager; // Import SessionManager
import Admin.Controller.DashboardAdminController;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button exitButton;
    @FXML private ImageView backgroundImage;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;

    private boolean isPasswordVisible = false;
    private final Label eyeIcon = new Label("🙈");
    private final Label eyeSlashIcon = new Label("👁");
    private SessionManager sessionManager;

    @FXML
    private void initialize() {
        sessionManager = SessionManager.getInstance();
        try {
            Image bg = new Image(getClass().getResource("/assets/login-page-background.png").toExternalForm());
            backgroundImage.setImage(bg);
        } catch (Exception e) {
            System.out.println("⚠ Gagal load background: " + e.getMessage());
        }
        eyeIcon.setStyle("-fx-font-size: 16;");
        eyeSlashIcon.setStyle("-fx-font-size: 16;");
        togglePasswordButton.setGraphic(eyeIcon);
        passwordVisibleField.setVisible(false);
        Platform.runLater(() -> {
            backgroundImage.fitWidthProperty().bind(backgroundImage.getScene().widthProperty());
            backgroundImage.fitHeightProperty().bind(backgroundImage.getScene().heightProperty());
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordVisibleField.setVisible(false);
            togglePasswordButton.setGraphic(eyeIcon);
            isPasswordVisible = false;
        } else {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordField.setVisible(false);
            togglePasswordButton.setGraphic(eyeSlashIcon);
            isPasswordVisible = true;
        }
    }

    private String getPassword() {
        if (isPasswordVisible) {
            return passwordVisibleField.getText();
        } else {
            return passwordField.getText();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = getPassword(); // Gunakan method ini
        if (username.isEmpty() || password.isEmpty()) {
            showToast("⚠ Silakan isi username dan password.", "warning");
            return;
        }

        try {
            DBConnect db = new DBConnect();
            String query = "SELECT IDKaryawan, Username, NamaKaryawan, JabatanKaryawan FROM Karyawan WHERE Username = ? AND Password = ? AND Status = 'Aktif'";
            PreparedStatement pstmt = db.conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String userId = rs.getString("IDKaryawan");
                String dbUsername = rs.getString("Username");
                String fullName = rs.getString("NamaKaryawan");
                String jabatan = rs.getString("JabatanKaryawan");
                sessionManager.setLoggedInUser(userId, dbUsername, fullName, jabatan);
                System.out.println("🔑 LOGIN SUCCESS - Session verification:");
                sessionManager.printCurrentSession();
                showToast("✅ Login Berhasil! Selamat datang, " + (fullName != null && !fullName.isEmpty() ? fullName : dbUsername) + " (" + jabatan + ")", "success");
                PauseTransition delay = new PauseTransition(Duration.seconds(2));
                delay.setOnFinished(event -> {
                    switch (jabatan.toLowerCase()) {
                        case "admin":
                            openDashboardAdmin();
                            break;
                        case "apoteker":
                            openDashboardApoteker();
                            break;
                        case "manajer":
                            openDashboardManajer();
                            break;
                        default:
                            showToast("⚠ Jabatan tidak dikenali.", "warning");
                            break;
                    }
                });
                delay.play();
            } else {
                showToast("❌ Login Gagal! Username, password salah, atau status tidak aktif.", "error");
            }
            rs.close();
            pstmt.close();
            db.conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("❌ Terjadi kesalahan: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }
    private void openDashboardAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Admin/DashboardAdmin.fxml"));
            Parent root = loader.load();
            DashboardAdminController controller = loader.getController();
            if (controller != null) {
                controller.setLoggedInUser(
                        sessionManager.getLoggedInUserId(),
                        sessionManager.getLoggedInUserName(),
                        sessionManager.getLoggedInUserFullName()
                );
                controller.setLoginTime(sessionManager.getLoginTime());
            }
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
            System.out.println("✅ Dashboard Admin opened successfully");
        } catch (IOException e) {
            e.printStackTrace();
            showToast("❗ Gagal membuka dashboard Admin.", "error");
        }
    }

    private void openDashboardApoteker() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Apoteker/DashboardApoteker.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller != null) {
                System.out.println("✅ Apoteker controller loaded: " + controller.getClass().getSimpleName());
            }
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
            System.out.println("✅ Dashboard Apoteker opened successfully");
        } catch (IOException e) {
            e.printStackTrace();
            showToast("❗ Gagal membuka dashboard Apoteker.", "error");
        }
    }

    private void openDashboardManajer() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Manajer/DashboardManajer.fxml"));
                Parent root = loader.load();
                Object controller = loader.getController();
                if (controller != null) {
                    System.out.println("✅ Manajer controller loaded: " + controller.getClass().getSimpleName());
                }
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setFullScreen(true);
                stage.show();
                System.out.println("✅ Dashboard Manajer opened successfully");
            } catch (IOException e) {
                e.printStackTrace();
                showToast("❗ Gagal membuka dashboard Manajer.", "error");
            }
    }

    private void showToast(String message, String type) {
        Label toastLabel = new Label(message);
        String bgColor;
        switch (type.toLowerCase()) {
            case "success": bgColor = "rgba(40,167,69,0.9)"; break;
            case "error": bgColor = "rgba(220,53,69,0.9)"; break;
            case "warning": bgColor = "rgba(255,193,7,0.9)"; break;
            default: bgColor = "rgba(108,117,125,0.9)";
        }
        toastLabel.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10px 20px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 14px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 6, 0, 0, 2);"
        );
        toastLabel.setOpacity(0);
        StackPane rootPane = (StackPane) usernameField.getScene().getRoot();
        rootPane.getChildren().add(toastLabel);
        StackPane.setMargin(toastLabel, new Insets(20));
        StackPane.setAlignment(toastLabel, Pos.TOP_CENTER);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), toastLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        PauseTransition stay = new PauseTransition(Duration.seconds(2));
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), toastLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.setOnFinished(event -> rootPane.getChildren().remove(toastLabel));
        seq.play();
    }
}