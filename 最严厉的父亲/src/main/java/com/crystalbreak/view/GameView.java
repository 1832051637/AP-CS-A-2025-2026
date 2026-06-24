package com.crystalbreak.view;

import com.crystalbreak.controller.GameController;
import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.CrystalCore;
import com.crystalbreak.model.GamePhase;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.OreType;
import com.crystalbreak.model.Player;
import com.crystalbreak.model.Pocket;
import com.crystalbreak.physics.Vector2;
import com.crystalbreak.util.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.stream.Collectors;

public class GameView extends BorderPane {
    private static final double MAX_CUE_PULLBACK = 145.0;
    private static final double MIN_SHOT_POWER = 0.05;
    private static final double POWER_RING_RADIUS = 26.0;
    private static final double STRIKE_CONTROL_SIZE = 132.0;
    private static final double STRIKE_BALL_RADIUS = 50.0;
    private static final double GROUP_ICON_SIZE = 26.0;

    private final GameController controller;
    private final Canvas canvas = new Canvas(GameConstants.CANVAS_WIDTH, GameConstants.CANVAS_HEIGHT);
    private final Canvas strikePointControl = new Canvas(STRIKE_CONTROL_SIZE, STRIKE_CONTROL_SIZE);
    private final Label modeLabel = new Label();
    private final Label turnLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label statusLabel = new Label();
    private final HBox targetHintBox = new HBox(8);
    private final VBox tableHintBox = new VBox(8);
    private double sideSpin;
    private double topSpin;
    private Vector2 aimPoint = new Vector2(GameConstants.TABLE_X + 300.0, GameConstants.TABLE_Y + GameConstants.TABLE_HEIGHT / 2.0);
    private Vector2 strokeStartPoint;
    private Vector2 strokeDirection = new Vector2(1.0, 0.0);
    private Vector2 impactPosition;
    private Vector2 impactDirection = new Vector2(1.0, 0.0);
    private boolean cuePullActive;
    private double cuePullback;
    private double strokePower;
    private double impactFlash;
    private AnimationTimer timer;
    private long lastFrameNanos;
    private String tableHintSignature = "";

    public GameView(GameController controller, Runnable backToMenu, Runnable openShop) {
        this.controller = controller;
        getStyleClass().add("game-root");
        buildHud(backToMenu, openShop);
        bindCanvasInput();
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = now;
                }
                double deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
                lastFrameNanos = now;
                controller.update(deltaSeconds);
                updateImpactFlash(deltaSeconds);
                render();
                refreshLabels();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void buildHud(Runnable backToMenu, Runnable openShop) {
        modeLabel.getStyleClass().add("hud-title");
        turnLabel.getStyleClass().add("turn-pill");
        HBox topBar = new HBox(18, modeLabel, turnLabel, scoreLabel, statusLabel);
        topBar.setPadding(new Insets(12, 18, 8, 18));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        configureStrikePointControl();

        Button menu = new Button("Menu");
        menu.setOnAction(event -> backToMenu.run());
        Button shop = new Button("Shop");
        shop.setOnAction(event -> openShop.run());
        targetHintBox.setAlignment(Pos.CENTER_LEFT);
        targetHintBox.getStyleClass().add("target-hint");
        tableHintBox.setFillWidth(true);
        tableHintBox.getStyleClass().add("table-hints");

        VBox sidePanel = new VBox(12,
                strikePointPane(),
                targetHintBox,
                tableHintBox,
                new HBox(10, menu, shop)
        );
        sidePanel.setPadding(new Insets(18));
        sidePanel.setPrefWidth(280);
        sidePanel.getStyleClass().add("side-panel");

        setTop(topBar);
        setCenter(canvas);
        setRight(sidePanel);
    }

    private void configureStrikePointControl() {
        strikePointControl.setFocusTraversable(true);
        strikePointControl.setAccessibleText("Cue ball strike position");
        strikePointControl.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                resetStrikePoint();
            } else {
                updateStrikePoint(event.getX(), event.getY());
            }
        });
        strikePointControl.setOnMouseDragged(event -> updateStrikePoint(event.getX(), event.getY()));
        drawStrikePointControl();
    }

    private VBox strikePointPane() {
        Button center = new Button("Reset");
        center.getStyleClass().add("compact-button");
        center.setOnAction(event -> resetStrikePoint());
        HBox controls = new HBox(center);
        controls.setAlignment(Pos.CENTER);
        VBox box = new VBox(5, strikePointControl, controls);
        box.setAlignment(Pos.CENTER);
        box.setFillWidth(true);
        return box;
    }

    private void resetStrikePoint() {
        sideSpin = 0.0;
        topSpin = 0.0;
        drawStrikePointControl();
    }

    private void updateStrikePoint(double x, double y) {
        double center = STRIKE_CONTROL_SIZE / 2.0;
        double dx = x - center;
        double dy = y - center;
        double distance = Math.hypot(dx, dy);
        if (distance > STRIKE_BALL_RADIUS && distance > 0.0) {
            dx = dx / distance * STRIKE_BALL_RADIUS;
            dy = dy / distance * STRIKE_BALL_RADIUS;
        }
        sideSpin = clamp(dx / STRIKE_BALL_RADIUS, -1.0, 1.0);
        topSpin = clamp(-dy / STRIKE_BALL_RADIUS, -1.0, 1.0);
        drawStrikePointControl();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawStrikePointControl() {
        GraphicsContext gc = strikePointControl.getGraphicsContext2D();
        double center = STRIKE_CONTROL_SIZE / 2.0;
        double radius = STRIKE_BALL_RADIUS;
        double markerX = center + sideSpin * radius;
        double markerY = center - topSpin * radius;

        gc.clearRect(0, 0, STRIKE_CONTROL_SIZE, STRIKE_CONTROL_SIZE);
        gc.setFill(Color.web("#000000", 0.25));
        gc.fillOval(center - radius + 4.0, center - radius + 6.0, radius * 2.0, radius * 2.0);
        RadialGradient ballGradient = new RadialGradient(0, 0, 0.38, 0.32, 0.78, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.WHITE),
                new Stop(0.62, Color.web("#f2f5f2")),
                new Stop(1.0, Color.web("#b7c2bf")));
        gc.setFill(ballGradient);
        gc.fillOval(center - radius, center - radius, radius * 2.0, radius * 2.0);
        gc.setStroke(Color.web("#ffffff", 0.72));
        gc.setLineWidth(1.2);
        gc.strokeOval(center - radius, center - radius, radius * 2.0, radius * 2.0);

        gc.setStroke(Color.web("#203036", 0.5));
        gc.setLineWidth(1.0);
        gc.strokeLine(center - radius, center, center + radius, center);
        gc.strokeLine(center, center - radius, center, center + radius);

        gc.setFill(Color.web("#f2c14e", 0.22));
        gc.fillOval(markerX - 10.0, markerY - 10.0, 20.0, 20.0);
        gc.setStroke(Color.web("#f2c14e"));
        gc.setLineWidth(2.0);
        gc.strokeOval(markerX - 6.0, markerY - 6.0, 12.0, 12.0);
        gc.setFill(Color.web("#111a1d"));
        gc.fillOval(markerX - 2.4, markerY - 2.4, 4.8, 4.8);
    }

    private void bindCanvasInput() {
        canvas.setFocusTraversable(true);
        canvas.setOnMouseMoved(event -> {
            if (!cuePullActive) {
                updateAimPoint(new Vector2(event.getX(), event.getY()));
            }
        });
        canvas.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            GameState state = controller.getState();
            if (!state.getCurrentPlayer().isHuman() || state.getPhase() != GamePhase.AIMING) {
                return;
            }
            Vector2 point = new Vector2(event.getX(), event.getY());
            if (state.isBallInHand()) {
                if (!controller.placeCueBall(point)) {
                    state.setStatusMessage("Free ball: choose an open spot on the table.");
                }
                return;
            }
            beginCuePull(point);
        });
        canvas.setOnMouseDragged(event -> {
            Vector2 point = new Vector2(event.getX(), event.getY());
            if (cuePullActive) {
                updateCuePull(point);
            } else {
                updateAimPoint(point);
            }
        });
        canvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY && cuePullActive) {
                releaseCuePull(new Vector2(event.getX(), event.getY()));
            }
        });
    }

    private void updateAimPoint(Vector2 point) {
        aimPoint = point;
        controller.getState().getCueBall().ifPresent(cue -> {
            Vector2 direction = aimPoint.subtract(cue.getPosition()).normalized();
            if (!direction.isNearlyZero()) {
                strokeDirection = direction;
            }
        });
    }

    private void beginCuePull(Vector2 point) {
        updateAimPoint(point);
        strokeStartPoint = point;
        cuePullActive = true;
        cuePullback = 0.0;
        strokePower = 0.0;
    }

    private void updateCuePull(Vector2 point) {
        if (strokeStartPoint == null) {
            return;
        }
        double projectedPull = strokeStartPoint.subtract(point).dot(strokeDirection);
        cuePullback = Math.max(0.0, Math.min(MAX_CUE_PULLBACK, projectedPull));
        strokePower = cuePullback / MAX_CUE_PULLBACK;
        controller.getState().getCueBall()
                .ifPresent(cue -> aimPoint = cue.getPosition().add(strokeDirection.multiply(320.0)));
    }

    private void releaseCuePull(Vector2 point) {
        updateCuePull(point);
        boolean shouldStrike = strokePower >= MIN_SHOT_POWER;
        if (shouldStrike) {
            controller.getState().getCueBall().ifPresent(cue -> impactPosition = cue.getPosition());
            impactDirection = strokeDirection;
            if (controller.strikeDirection(strokeDirection, strokePower, selectedSideSpin(), topSpin)) {
                impactFlash = 1.0;
            }
        }
        cuePullActive = false;
        cuePullback = 0.0;
        strokePower = 0.0;
        strokeStartPoint = null;
    }

    private double selectedSideSpin() {
        return -sideSpin;
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        GameState state = controller.getState();
        gc.setFill(Color.web("#0d1215"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawTable(gc, state);
        drawAimAssist(gc, state);
        drawShotGuide(gc, state);
        drawCrystalCore(gc, state.getCrystalCore());
        drawBalls(gc, state);
        drawCuePlacementPrompt(gc, state);
        drawImpactFlash(gc);
        drawPowerRing(gc, state);
        drawGameOver(gc, state);
    }

    private void drawTable(GraphicsContext gc, GameState state) {
        double rail = GameConstants.RAIL_WIDTH;
        double x = state.getTable().getX();
        double y = state.getTable().getY();
        double w = state.getTable().getWidth();
        double h = state.getTable().getHeight();

        gc.setFill(Color.web("#4a291b"));
        gc.fillRoundRect(x - rail, y - rail, w + rail * 2, h + rail * 2, 38, 38);
        gc.setFill(Color.web("#0f6b55"));
        gc.fillRoundRect(x, y, w, h, 20, 20);
        gc.setStroke(Color.web("#55b58f"));
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(x + 10, y + 10, w - 20, h - 20, 16, 16);

        if (state.getTableInset() > 0.0) {
            double inset = state.getTableInset();
            gc.setStroke(Color.web("#f2d57a"));
            gc.setLineWidth(2.0);
            gc.strokeRect(x + inset, y + inset, w - inset * 2, h - inset * 2);
        }

        for (Pocket pocket : state.getTable().getPockets()) {
            double radius = pocket.getRadius() * state.getPocketScale();
            gc.setFill(Color.web("#050607"));
            gc.fillOval(pocket.getPosition().x() - radius, pocket.getPosition().y() - radius, radius * 2, radius * 2);
            gc.setStroke(Color.web("#1c2726"));
            gc.strokeOval(pocket.getPosition().x() - radius, pocket.getPosition().y() - radius, radius * 2, radius * 2);
        }
    }

    private void drawAimAssist(GraphicsContext gc, GameState state) {
        if (state.getPhase() != GamePhase.AIMING || state.isBallInHand() || !state.getCurrentPlayer().isHuman()) {
            return;
        }
        if (!controller.getSaveData().getSettings().isAimAssist()) {
            return;
        }
        List<Vector2> path = controller.aimAssistPath(aimPoint);
        if (path.size() < 2) {
            return;
        }
        Vector2 start = path.get(0);
        Vector2 end = path.get(1);
        gc.setStroke(Color.web("#f6e7a1", 0.75));
        gc.setLineWidth(2.0);
        gc.setLineDashes(12, 9);
        gc.strokeLine(start.x(), start.y(), end.x(), end.y());
        gc.setLineDashes(null);

        Vector2 back = start.subtract(end.subtract(start).normalized().multiply(34.0));
        gc.setStroke(Color.web("#d6c48a", 0.55));
        gc.strokeLine(start.x(), start.y(), back.x(), back.y());
    }

    private void drawShotGuide(GraphicsContext gc, GameState state) {
        if (state.getPhase() != GamePhase.AIMING || state.isBallInHand() || !state.getCurrentPlayer().isHuman()) {
            return;
        }
        state.getCueBall().ifPresent(cue -> {
            if (cue.isPocketed()) {
                return;
            }
            Vector2 direction = strokeDirection.normalized();
            if (direction.isNearlyZero()) {
                direction = aimPoint.subtract(cue.getPosition()).normalized();
            }
            if (direction.isNearlyZero()) {
                return;
            }

            double radius = cue.getRadius() * state.getBallRadiusScale();
            gc.setLineCap(StrokeLineCap.ROUND);

            if (!controller.getSaveData().getSettings().isAimAssist()) {
                Vector2 forwardStart = cue.getPosition().add(direction.multiply(radius + 8.0));
                Vector2 forwardEnd = cue.getPosition().add(direction.multiply(180.0));
                gc.setStroke(Color.web("#f6e7a1", 0.36));
                gc.setLineWidth(1.6);
                gc.setLineDashes(8.0, 8.0);
                gc.strokeLine(forwardStart.x(), forwardStart.y(), forwardEnd.x(), forwardEnd.y());
                gc.setLineDashes(null);
            }

            if (cuePullActive) {
                Vector2 pullStart = cue.getPosition().subtract(direction.multiply(radius + 10.0));
                Vector2 pullEnd = pullStart.subtract(direction.multiply(30.0 + cuePullback));
                gc.setStroke(Color.web("#f2c14e", 0.42 + strokePower * 0.34));
                gc.setLineWidth(1.8 + strokePower * 1.8);
                gc.strokeLine(pullStart.x(), pullStart.y(), pullEnd.x(), pullEnd.y());
            }

            gc.setLineCap(StrokeLineCap.BUTT);
        });
    }

    private void drawCrystalCore(GraphicsContext gc, CrystalCore core) {
        if (core == null || !core.isActive()) {
            return;
        }
        double r = core.getRadius();
        RadialGradient gradient = new RadialGradient(0, 0, 0.45, 0.45, 0.72, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#ffffff")),
                new Stop(0.35, Color.web("#80f6ff")),
                new Stop(1.0, Color.web("#6b42ff")));
        gc.setFill(gradient);
        gc.fillOval(core.getPosition().x() - r, core.getPosition().y() - r, r * 2, r * 2);
        gc.setStroke(Color.web("#d9fbff"));
        gc.setLineWidth(2.0);
        gc.strokeOval(core.getPosition().x() - r, core.getPosition().y() - r, r * 2, r * 2);
    }

    private void drawBalls(GraphicsContext gc, GameState state) {
        for (Ball ball : state.getBalls()) {
            if (ball.isPocketed()) {
                continue;
            }
            if (state.isBallInHand() && state.getCurrentPlayer().isHuman() && ball.isCueBall()) {
                continue;
            }
            double radius = ball.getRadius() * state.getBallRadiusScale();
            double x = ball.getPosition().x();
            double y = ball.getPosition().y();
            gc.setFill(Color.web("#000000", 0.28));
            gc.fillOval(x - radius + 3, y - radius + 4, radius * 2, radius * 2);

            if (ball.getGroup() == BallGroup.STRIPE) {
                gc.setFill(Color.WHITE);
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                gc.setFill(ball.getColor());
                gc.fillRoundRect(x - radius * 0.94, y - radius * 0.45, radius * 1.88, radius * 0.9, radius * 0.35, radius * 0.35);
            } else {
                gc.setFill(ball.getColor());
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            }

            gc.setStroke(Color.web("#ffffff", 0.55));
            gc.setLineWidth(ball.getGroup() == BallGroup.BOSS ? 2.6 : 1.2);
            gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            drawBallLabel(gc, ball, x, y, radius);
        }
    }

    private void drawBallLabel(GraphicsContext gc, Ball ball, double x, double y, double radius) {
        String label = ball.getLabel();
        if (ball.getOreType() != null) {
            label = oreSymbol(ball.getOreType());
        }
        if (ball.getGroup() == BallGroup.BOSS) {
            label = "HP" + ball.getHitPoints();
        }
        gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(8, radius * 0.62)));
        gc.setFill(ball.getGroup() == BallGroup.EIGHT || ball.getGroup() == BallGroup.BOSS ? Color.WHITE : Color.web("#111111"));
        double width = label.length() * radius * 0.34;
        gc.fillText(label, x - width, y + radius * 0.22);
    }

    private String oreSymbol(OreType oreType) {
        return switch (oreType) {
            case COAL -> "C";
            case IRON -> "Fe";
            case GOLD -> "Au";
            case DIAMOND -> "D";
            case EMERALD -> "E";
        };
    }

    private void drawCuePlacementPrompt(GraphicsContext gc, GameState state) {
        if (state.getPhase() != GamePhase.AIMING || !state.isBallInHand() || !state.getCurrentPlayer().isHuman()) {
            return;
        }
        double radius = GameConstants.CUE_BALL_RADIUS * state.getBallRadiusScale();
        boolean valid = controller.canPlaceCueBall(aimPoint);
        Color accent = valid ? Color.web("#b6ffd1") : Color.web("#ff8a80");

        gc.setFill(Color.web(valid ? "#b6ffd1" : "#ff8a80", 0.22));
        gc.fillOval(aimPoint.x() - radius, aimPoint.y() - radius, radius * 2.0, radius * 2.0);
        gc.setStroke(accent);
        gc.setLineWidth(2.0);
        gc.strokeOval(aimPoint.x() - radius, aimPoint.y() - radius, radius * 2.0, radius * 2.0);
        gc.setLineDashes(5.0, 5.0);
        gc.strokeOval(aimPoint.x() - radius - 7.0, aimPoint.y() - radius - 7.0, (radius + 7.0) * 2.0, (radius + 7.0) * 2.0);
        gc.setLineDashes(null);

        double promptX = state.getTable().getX() + 28.0;
        double promptY = state.getTable().getY() + 38.0;
        gc.setFill(Color.web("#06110e", 0.76));
        gc.fillRoundRect(promptX - 14.0, promptY - 27.0, 304.0, 40.0, 8.0, 8.0);
        gc.setFill(accent);
        gc.setFont(Font.font("System", FontWeight.BOLD, 15.0));
        gc.fillText(valid ? state.getCurrentPlayer().getName() + " free ball" : "Free ball: blocked spot", promptX, promptY);
    }

    private void drawImpactFlash(GraphicsContext gc) {
        if (impactFlash <= 0.0 || impactPosition == null) {
            return;
        }
        double alpha = Math.min(1.0, impactFlash);
        double radius = 14.0 + (1.0 - alpha) * 24.0;
        gc.setGlobalAlpha(alpha);
        gc.setStroke(Color.web("#fff3b0", 0.82));
        gc.setLineWidth(2.0 + alpha * 2.0);
        gc.strokeOval(impactPosition.x() - radius, impactPosition.y() - radius, radius * 2.0, radius * 2.0);
        Vector2 start = impactPosition.subtract(impactDirection.multiply(20.0));
        Vector2 end = impactPosition.add(impactDirection.multiply(34.0));
        gc.setStroke(Color.web("#ffffff", 0.8));
        gc.setLineWidth(3.0);
        gc.strokeLine(start.x(), start.y(), end.x(), end.y());
        gc.setGlobalAlpha(1.0);
    }

    private void drawPowerRing(GraphicsContext gc, GameState state) {
        if (state.getPhase() != GamePhase.AIMING || state.isBallInHand() || !state.getCurrentPlayer().isHuman()) {
            return;
        }
        state.getCueBall().ifPresent(cue -> {
            if (cue.isPocketed()) {
                return;
            }
            double centerX = cue.getPosition().x();
            double centerY = cue.getPosition().y();
            double ringRadius = POWER_RING_RADIUS + strokePower * 5.0;
            double diameter = ringRadius * 2.0;
            double x = centerX - ringRadius;
            double y = centerY - ringRadius;

            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setStroke(Color.web("#d9f2ea", cuePullActive ? 0.28 : 0.16));
            gc.setLineWidth(3.0);
            gc.strokeOval(x, y, diameter, diameter);

            if (strokePower > 0.0) {
                gc.setStroke(Color.web("#f2c14e", 0.72 + strokePower * 0.22));
                gc.setLineWidth(4.0);
                gc.strokeArc(x, y, diameter, diameter, 90.0, -strokePower * 360.0, ArcType.OPEN);
            }
            gc.setLineCap(StrokeLineCap.BUTT);
        });
    }

    private void drawGameOver(GraphicsContext gc, GameState state) {
        if (state.getPhase() != GamePhase.GAME_OVER) {
            return;
        }
        gc.setFill(Color.web("#08110f", 0.76));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 36));
        gc.fillText(state.getStatusMessage(), GameConstants.TABLE_X + 120, GameConstants.TABLE_Y + GameConstants.TABLE_HEIGHT / 2.0);
    }

    private void refreshLabels() {
        GameState state = controller.getState();
        Player current = state.getCurrentPlayer();
        modeLabel.setText(state.getMode().displayName());
        String currentText = current.getName() + (state.isBallInHand() ? " - Ball in hand" : "");
        turnLabel.setText("Turn: " + currentText);
        scoreLabel.setText(state.getPlayers().stream()
                .map(player -> player.getName() + " " + player.getScore())
                .collect(Collectors.joining("   ")));
        String modeStatus = controller.getModeHandler().modeStatus(state);
        String status = state.getStatusMessage();
        if (state.isBallInHand()) {
            status = status + " | " + freeBallHint(state);
        }
        statusLabel.setText(modeStatus.isBlank() ? status : status + " | " + modeStatus);
        refreshTableHints(state);
    }

    private void refreshTableHints(GameState state) {
        String signature = tableHintSignature(state);
        if (signature.equals(tableHintSignature)) {
            return;
        }
        tableHintSignature = signature;
        targetHintBox.getChildren().clear();
        tableHintBox.getChildren().clear();

        if (usesEightBallGroups(state)) {
            addTargetGroupHint(state);
            for (Player player : state.getPlayers()) {
                addPlayerGroupHint(state, player);
            }
        } else {
            addTargetText(formatModeGoal(state));
        }
        if (state.isBallInHand()) {
            addHintText(freeBallHint(state));
        }
    }

    private String tableHintSignature(GameState state) {
        StringBuilder signature = new StringBuilder()
                .append(state.getMode()).append('|')
                .append(state.getCurrentPlayerIndex()).append('|')
                .append(state.isBallInHand()).append('|')
                .append(remainingGroupCount(state, BallGroup.SOLID)).append('|')
                .append(remainingGroupCount(state, BallGroup.STRIPE));
        for (Player player : state.getPlayers()) {
            signature.append('|')
                    .append(player.getName())
                    .append(':')
                    .append(player.getAssignedGroup().map(BallGroup::name).orElse("OPEN"));
        }
        return signature.toString();
    }

    private void addPlayerGroupHint(GameState state, Player player) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("hint-row");

        Label name = new Label(player.getName() + ":");
        name.getStyleClass().add("hint-name");
        name.setMinWidth(66.0);
        if (player == state.getCurrentPlayer()) {
            name.setFont(Font.font("System", FontWeight.BOLD, 12.0));
        }

        row.getChildren().addAll(name, groupBadge(player.getAssignedGroup().orElse(null)));
        tableHintBox.getChildren().add(row);
    }

    private void addTargetGroupHint(GameState state) {
        Label label = new Label("Target");
        label.getStyleClass().add("hint-name");
        label.setFont(Font.font("System", FontWeight.BOLD, 12.0));

        targetHintBox.getChildren().addAll(label, groupBadge(currentTargetGroup(state)));
    }

    private void addHintText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("control-caption");
        tableHintBox.getChildren().add(label);
    }

    private void addTargetText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("control-caption");
        targetHintBox.getChildren().add(label);
    }

    private HBox groupBadge(BallGroup group) {
        HBox badge = new HBox(5);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.getStyleClass().add("group-badge");
        if (group == null) {
            badge.getChildren().addAll(
                    createGroupIcon(BallGroup.SOLID),
                    createGroupIcon(BallGroup.STRIPE));
            return badge;
        }
        badge.getChildren().add(createGroupIcon(group));
        return badge;
    }

    private Canvas createGroupIcon(BallGroup group) {
        Canvas icon = new Canvas(GROUP_ICON_SIZE, GROUP_ICON_SIZE);
        drawGroupIcon(icon.getGraphicsContext2D(), group, GROUP_ICON_SIZE);
        return icon;
    }

    private void drawGroupIcon(GraphicsContext gc, BallGroup group, double size) {
        double center = size / 2.0;
        double radius = size * 0.42;
        gc.setFill(Color.web("#000000", 0.25));
        gc.fillOval(center - radius + 1.2, center - radius + 1.8, radius * 2.0, radius * 2.0);

        if (group == BallGroup.STRIPE) {
            gc.setFill(Color.WHITE);
            gc.fillOval(center - radius, center - radius, radius * 2.0, radius * 2.0);
            gc.setFill(groupIconColor(group));
            gc.fillRoundRect(center - radius * 0.82, center - radius * 0.34,
                    radius * 1.64, radius * 0.68, radius * 0.18, radius * 0.18);
        } else {
            gc.setFill(groupIconColor(group));
            gc.fillOval(center - radius, center - radius, radius * 2.0, radius * 2.0);
        }

        gc.setStroke(Color.web("#ffffff", 0.62));
        gc.setLineWidth(1.0);
        gc.strokeOval(center - radius, center - radius, radius * 2.0, radius * 2.0);

        String number = groupIconNumber(group);
        if (!number.isBlank()) {
            if (group != BallGroup.EIGHT && group != BallGroup.BOSS) {
                gc.setFill(Color.WHITE);
                gc.fillOval(center - radius * 0.34, center - radius * 0.34, radius * 0.68, radius * 0.68);
                gc.setFill(Color.web("#101719"));
            } else {
                gc.setFill(Color.WHITE);
            }
            gc.setFont(Font.font("System", FontWeight.BOLD, radius * 0.74));
            double textWidth = number.length() * radius * 0.24;
            gc.fillText(number, center - textWidth, center + radius * 0.22);
        }
    }

    private boolean usesEightBallGroups(GameState state) {
        return switch (state.getMode()) {
            case CLASSIC, CRYSTAL_CORE, SKILL_SHOT, CHAOS -> true;
            case MINING_POOL, BOSS_CHALLENGE -> false;
        };
    }

    private BallGroup currentTargetGroup(GameState state) {
        Player current = state.getCurrentPlayer();
        return current.getAssignedGroup()
                .map(group -> allGroupCleared(state, group) ? BallGroup.EIGHT : group)
                .orElse(null);
    }

    private String formatModeGoal(GameState state) {
        return switch (state.getMode()) {
            case MINING_POOL -> "Goal: pocket ore balls for coins.";
            case BOSS_CHALLENGE -> "Goal: damage the boss ball.";
            default -> "Goal: clear your legal balls.";
        };
    }

    private Color groupIconColor(BallGroup group) {
        return switch (group) {
            case SOLID -> Color.web("#f3c64e");
            case STRIPE -> Color.web("#3377d6");
            case EIGHT -> Color.web("#151515");
            case ORE -> Color.web("#2f9a62");
            case BOSS -> Color.web("#6b42ff");
            case CUE -> Color.WHITE;
        };
    }

    private String groupIconNumber(BallGroup group) {
        return switch (group) {
            case SOLID -> "1";
            case STRIPE -> "9";
            case EIGHT -> "8";
            case BOSS -> "B";
            default -> "";
        };
    }

    private long remainingGroupCount(GameState state, BallGroup group) {
        return state.getBalls().stream()
                .filter(ball -> ball.getGroup() == group && !ball.isPocketed())
                .count();
    }

    private boolean allGroupCleared(GameState state, BallGroup group) {
        return state.getBalls().stream()
                .noneMatch(ball -> ball.getGroup() == group && !ball.isPocketed());
    }

    private String freeBallHint(GameState state) {
        if (state.getCurrentPlayer().isHuman()) {
            return "Free ball: place the cue ball on an open spot.";
        }
        return "Free ball: AI is placing the cue ball.";
    }

    private void updateImpactFlash(double deltaSeconds) {
        if (impactFlash > 0.0) {
            impactFlash = Math.max(0.0, impactFlash - deltaSeconds * 4.0);
        }
    }
}
