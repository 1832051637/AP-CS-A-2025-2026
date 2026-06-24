package com.crystalbreak.app;

import com.crystalbreak.audio.SoundManager;
import com.crystalbreak.controller.GameController;
import com.crystalbreak.model.CueStick;
import com.crystalbreak.persistence.GameSettings;
import com.crystalbreak.persistence.SaveData;
import com.crystalbreak.persistence.SaveManager;
import com.crystalbreak.view.GameView;
import com.crystalbreak.view.MenuView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.stream.Collectors;

public class CrystalBreakApp extends Application {
    private Stage stage;
    private SaveManager saveManager;
    private SaveData saveData;
    private SoundManager soundManager;
    private GameController controller;
    private GameView activeGameView;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        saveManager = new SaveManager();
        saveData = saveManager.load();
        soundManager = new SoundManager();
        soundManager.setMasterVolume(saveData.getSettings().getMasterVolume());
        controller = new GameController(soundManager, saveManager, saveData);
        stage.setTitle("CrystalBreak Billiards");
        showMenu();
        stage.show();
    }

    private void showMenu() {
        stopActiveGame();
        MenuView menuView = new MenuView(this::startGame, this::showShop, this::showStatistics, this::showSettings, this::exit);
        stage.setScene(scene(menuView, 920, 720));
    }

    private void startGame(MenuView.StartOptions options) {
        stopActiveGame();
        controller.newGame(options.mode(), options.versusAi(), options.difficulty());
        activeGameView = new GameView(controller, this::showMenu, this::showShop);
        stage.setScene(scene(activeGameView, 1460, 770));
        activeGameView.start();
    }

    private void showShop() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Shop");
        Label summary = new Label();
        Button upgradeCue = new Button("Upgrade Cue");
        Runnable refresh = () -> {
            CueStick cue = controller.getState().getPlayers().get(0).getCueStick();
            summary.setText("Coins: " + saveData.getProgress().getCoins()
                    + "\nCue Level: " + cue.getLevel()
                    + "\nPower: " + String.format("%.2f", cue.getPower())
                    + "\nAccuracy: " + String.format("%.2f", cue.getAccuracy())
                    + "\nSpin: " + String.format("%.2f", cue.getSpin())
                    + "\nStability: " + String.format("%.2f", cue.getStability())
                    + "\nUpgrade Cost: " + cue.upgradeCost()
                    + "\n\nFuture shelves: special skills, visual skins, custom cues.");
        };
        upgradeCue.setOnAction(event -> {
            if (!controller.upgradeCue()) {
                summary.setText(summary.getText() + "\nNot enough coins.");
            }
            refresh.run();
        });
        refresh.run();
        VBox content = new VBox(14, summary, upgradeCue);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showStatistics() {
        String modeScores = saveData.getBestScoresByMode().isEmpty()
                ? "No mode records yet."
                : saveData.getBestScoresByMode().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Statistics");
        alert.setHeaderText("CrystalBreak Statistics");
        alert.setContentText("High Score: " + saveData.getHighScore()
                + "\nLevel: " + saveData.getProgress().getLevel()
                + "\nCoins: " + saveData.getProgress().getCoins()
                + "\nCue Level: " + saveData.getCueLevel()
                + "\nSave: " + saveManager.getSavePath()
                + "\n\n" + modeScores);
        alert.showAndWait();
    }

    private void showSettings() {
        GameSettings settings = saveData.getSettings();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        Slider volume = new Slider(0.0, 1.0, settings.getMasterVolume());
        volume.setShowTickMarks(true);
        volume.setShowTickLabels(true);
        CheckBox showFps = new CheckBox("Show FPS");
        showFps.setSelected(settings.isShowFps());
        CheckBox aimAssist = new CheckBox("Aim Assist");
        aimAssist.setSelected(settings.isAimAssist());
        CheckBox music = new CheckBox("Background Music");
        music.setSelected(settings.isBackgroundMusic());
        VBox content = new VBox(12,
                new Label("Master Volume"), volume,
                showFps,
                aimAssist,
                music
        );
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                settings.setMasterVolume(volume.getValue());
                settings.setShowFps(showFps.isSelected());
                settings.setAimAssist(aimAssist.isSelected());
                settings.setBackgroundMusic(music.isSelected());
                soundManager.setMasterVolume(settings.getMasterVolume());
                saveManager.save(saveData);
            }
            return null;
        });
        dialog.showAndWait();
    }

    private Scene scene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        String css = CrystalBreakApp.class.getResource("/com/crystalbreak/css/app.css").toExternalForm();
        scene.getStylesheets().add(css);
        return scene;
    }

    private void stopActiveGame() {
        if (activeGameView != null) {
            activeGameView.stop();
            activeGameView = null;
        }
    }

    private void exit() {
        saveManager.save(saveData);
        stopActiveGame();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
