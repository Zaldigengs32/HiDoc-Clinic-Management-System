package Manajer.Controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BerandaManajerController implements Initializable {

    @FXML private VBox welcomeCard;
    @FXML private HBox statsCardsContainer;
    @FXML private VBox cardPenjualan;
    @FXML private VBox cardPengadaan;
    @FXML private VBox cardCekKesehatan;
    @FXML private Label currentDateLabel;
    @FXML private Label currentTimeLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCardEventHandlers();
    }


    private void setupCardEventHandlers() {
        if (cardPenjualan != null) {
            setupCardHoverEffect(cardPenjualan);
            cardPenjualan.setOnMouseClicked(event -> {
                System.out.println("Navigating to Laporan Penjualan...");
            });
        }

        if (cardPengadaan != null) {
            setupCardHoverEffect(cardPengadaan);
            cardPengadaan.setOnMouseClicked(event -> {
                System.out.println("Navigating to Laporan Pengadaan...");
                // This will be handled by the parent controller
            });
        }

        if (cardCekKesehatan != null) {
            setupCardHoverEffect(cardCekKesehatan);
            cardCekKesehatan.setOnMouseClicked(event -> {
                System.out.println("Navigating to Laporan Cek Kesehatan...");
                // This will be handled by the parent controller
            });
        }
    }

    private void setupCardHoverEffect(VBox card) {
        if (card == null) return;
        String originalStyle = card.getStyle();
        String hoverStyle = originalStyle + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 25, 0, 0, 15); -fx-scale-x: 1.02; -fx-scale-y: 1.02;";

        card.setOnMouseEntered(event -> {
            card.setStyle(hoverStyle);
            FadeTransition hover = new FadeTransition(Duration.millis(150), card);
            hover.setFromValue(card.getOpacity());
            hover.setToValue(0.95);
            hover.play();
        });

        card.setOnMouseExited(event -> {
            card.setStyle(originalStyle);
            FadeTransition unhover = new FadeTransition(Duration.millis(150), card);
            unhover.setFromValue(card.getOpacity());
            unhover.setToValue(1.0);
            unhover.play();
        });
    }
}