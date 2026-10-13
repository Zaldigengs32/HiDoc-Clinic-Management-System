package Interface;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

public class MainLoading extends Application {
    private static Stage primaryStage;
    public static void main(String[] args) {
        launch(args); // hanya dipanggil satu kali
    }

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            primaryStage.setFullScreenExitHint(""); // Hilangkan "Press ESC..."
            showLoading();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLoading() throws Exception {loadScene("MenuLoading.fxml", "Loading Aplikasi - Fullscreen");}
    public static void showLoginAdmin() throws Exception {loadScene("MenuLogin.fxml", "Login Apotek - Apotek");}
    public static void showDashboardAdmin() throws Exception {loadScene("DashboardAdmin.fxml", "Dashboard Admin - Apotek");}
    public static void showMainApoteker() throws Exception {loadScene("DashboardApoeteker.fxml", "Dashboard Apoeteker - Apoteker");}
    private static void loadScene(String fxmlFile, String title) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(MainLoading.class.getResource(fxmlFile));
            Parent root = loader.load();
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            primaryStage.setTitle(title);
            primaryStage.setX(screenBounds.getMinX());
            primaryStage.setY(screenBounds.getMinY());
            primaryStage.setWidth(screenBounds.getWidth());
            primaryStage.setHeight(screenBounds.getHeight());
            primaryStage.setScene(scene);
            primaryStage.setFullScreen(true);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("❌ Gagal memuat file FXML: " + fxmlFile);
            e.printStackTrace();
            throw e;
        }
    }
}
