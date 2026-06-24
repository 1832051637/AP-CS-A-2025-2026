package com.crystalbreak.view;

import com.crystalbreak.ai.Difficulty;
import com.crystalbreak.model.GameMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class MenuView extends BorderPane {
    public record StartOptions(GameMode mode, boolean versusAi, Difficulty difficulty) {
    }

    public MenuView(Consumer<StartOptions> startGame, Runnable openShop, Runnable openStatistics, Runnable openSettings, Runnable exit) {
        getStyleClass().add("menu-root");

        Label title = new Label("CrystalBreak");
        title.getStyleClass().add("menu-title");
        Label subtitle = new Label("2D 8-Ball Pool | RPG | Mining | Crystal Events");
        subtitle.getStyleClass().add("menu-subtitle");

        ComboBox<GameMode> modeBox = new ComboBox<>();
        modeBox.getItems().setAll(GameMode.values());
        modeBox.setValue(GameMode.CLASSIC);
        modeBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Difficulty> difficultyBox = new ComboBox<>();
        difficultyBox.getItems().setAll(Difficulty.values());
        difficultyBox.setValue(Difficulty.NORMAL);
        difficultyBox.setMaxWidth(Double.MAX_VALUE);

        CheckBox versusAi = new CheckBox("Versus AI");
        versusAi.setSelected(true);

        Button start = menuButton("Start Game");
        start.setOnAction(event -> startGame.accept(new StartOptions(modeBox.getValue(), versusAi.isSelected(), difficultyBox.getValue())));

        Button shop = menuButton("Shop");
        shop.setOnAction(event -> openShop.run());

        Button statistics = menuButton("Statistics");
        statistics.setOnAction(event -> openStatistics.run());

        Button settings = menuButton("Settings");
        settings.setOnAction(event -> openSettings.run());

        Button exitButton = menuButton("Exit");
        exitButton.setOnAction(event -> exit.run());

        VBox controls = new VBox(12,
                title,
                subtitle,
                labeledRow("Mode", modeBox),
                labeledRow("AI", difficultyBox),
                versusAi,
                start,
                shop,
                statistics,
                settings,
                exitButton
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(42));
        controls.getStyleClass().add("menu-panel");
        controls.setMaxWidth(430);

        setCenter(controls);
        BorderPane.setAlignment(controls, Pos.CENTER);
    }

    private Button menuButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("menu-button");
        return button;
    }

    private HBox labeledRow(String labelText, ComboBox<?> comboBox) {
        Label label = new Label(labelText);
        label.setMinWidth(64);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, label, spacer, comboBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
