package Interface;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.animation.FadeTransition;
import java.io.IOException;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class LoadingController {

    private MediaPlayer startupMediaPlayer;
    @FXML private ProgressBar progressBar;
    @FXML private ImageView backgroundImage;
    @FXML private Label percentLabel;
    private double progress = 0.0;
    @FXML public void initialize() {
        playStartupMusic();
        loadImages();
        progressBar.setVisible(false);
        percentLabel.setVisible(false);
        progressBar.setOpacity(0);
        percentLabel.setOpacity(0);
        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.seconds(2.5), e -> {
            fadeIn(progressBar);
            fadeIn(percentLabel);
            animateLoading();
        }));
        delayTimeline.play();
        Platform.runLater(() -> {
            if (backgroundImage.getScene() != null) {
                backgroundImage.fitWidthProperty().bind(backgroundImage.getScene().widthProperty());
                backgroundImage.fitHeightProperty().bind(backgroundImage.getScene().heightProperty());
            }
        });
    }

    private void loadImages() {
        try {
            Image bg = new Image(getClass().getResourceAsStream("/assets/loading-page-animate.gif"));
            backgroundImage.setImage(bg);
        } catch (Exception e) {
            System.out.println("⚠ Gambar tidak ditemukan: " + e.getMessage());
        }
    }

    private void animateLoading() {
        Timeline timeline = new Timeline();
        Duration totalDuration = Duration.millis(2000);
        int steps = 100;
        for (int i = 0; i <= steps; i++) {
            int percent = i;
            Duration timepoint = totalDuration.divide(steps).multiply(i);
            timeline.getKeyFrames().add(
                    new KeyFrame(timepoint, e -> {
                        progress = percent / 100.0;
                        progressBar.setProgress(progress);
                        percentLabel.setText(percent + "%");
                    })
            );
        }
        timeline.setOnFinished(e -> openMainMenu());
        timeline.play();
    }

    private void fadeIn(Node node) {
        node.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(600), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void playStartupMusic() {
        try {
            URL musicFile = getClass().getResource("/audio/BackgroundMusic.mp3");
            if (musicFile != null) {
                Media media = new Media(musicFile.toString());
                startupMediaPlayer = new MediaPlayer(media);
                startupMediaPlayer.setVolume(0.5);
                startupMediaPlayer.setOnEndOfMedia(() -> {
                    startupMediaPlayer.dispose();
                });
                startupMediaPlayer.play();
            }
        } catch (Exception e) {
            System.err.println("Error playing startup music: " + e.getMessage());
        }
    }

    private void openMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Interface/MenuLogin.fxml"));
            Scene scene = new Scene(loader.load());
            Stage currentStage = (Stage) progressBar.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Menu Login HiDoc");
            currentStage.setFullScreenExitHint("");
            currentStage.setFullScreen(true);
        } catch (IOException e) {
            System.err.println("❌ Gagal membuka MenuLogin.fxml");
            e.printStackTrace();
        }
    }
}
