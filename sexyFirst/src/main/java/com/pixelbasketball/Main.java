package com.pixelbasketball;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class Main extends Application {
    private static final double COURT_WIDTH = 1_000.0;
    private static final double COURT_HEIGHT = 560.0;
    private static final double FLOOR_Y = 500.0;
    private static final double CENTER_CIRCLE_RADIUS = 54.0;
    private static final double PLAYER_START_OFFSET = 235.0;
    private static final double BALL_START_HEIGHT = 96.0;
    private static final double SCORE_RESET_DELAY = 0.82;
    private static final double SCORE_LOCK_RELEASE_Y = 42.0;
    private static final double SHOT_POWER_MIN = 0.86;
    private static final double SHOT_POWER_START = 0.94;
    private static final double SHOT_POWER_STANDARD = 1.0;
    private static final double SHOT_POWER_MAX = 1.22;
    private static final double SHOT_CHARGE_RATE = 0.48;
    private static final double POWER_METER_WIDTH = 50.0;
    private static final double POWER_METER_HEIGHT = 7.0;
    private static final double STEAL_RANGE = 62.0;
    private static final double STEAL_COOLDOWN = 0.82;
    private static final double STEAL_BASE_CHANCE = 0.30;
    private static final double STEAL_CLOSENESS_BONUS = 0.36;
    private static final double STEAL_FACING_BONUS = 0.14;
    private static final double STEAL_LOOSE_BASE_CHANCE = 0.24;
    private static final double STEAL_LOOSE_CLOSENESS_BONUS = 0.20;
    private static final double STEAL_NOTICE_TIME = 0.42;

    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private final Set<KeyCode> previousKeys = EnumSet.noneOf(KeyCode.class);
    private final Map<Hoop.Side, Integer> scoreByHoop = new EnumMap<>(Hoop.Side.class);
    private final ShotCharge playerOneShotCharge = new ShotCharge();
    private final ShotCharge playerTwoShotCharge = new ShotCharge();

    private Canvas canvas;
    private GraphicsContext gc;
    private List<Hoop> hoops;
    private Player playerOne;
    private Player playerTwo;
    private Ball ball;
    private Hoop.Side lockedScoringHoop;
    private double scoreResetTimer;
    private double playerOneStealCooldown;
    private double playerTwoStealCooldown;
    private double stealNoticeTimer;
    private String stealNotice = "";

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(COURT_WIDTH, COURT_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        hoops = List.of(
                Hoop.forSide(Hoop.Side.LEFT, COURT_WIDTH, COURT_HEIGHT),
                Hoop.forSide(Hoop.Side.RIGHT, COURT_WIDTH, COURT_HEIGHT)
        );
        scoreByHoop.put(Hoop.Side.LEFT, 0);
        scoreByHoop.put(Hoop.Side.RIGHT, 0);

        playerOne = new Player(
                "P1",
                Color.rgb(64, 154, 255),
                Hoop.Side.RIGHT,
                COURT_WIDTH * 0.5 - PLAYER_START_OFFSET,
                FLOOR_Y
        );
        playerTwo = new Player(
                "P2",
                Color.rgb(255, 88, 88),
                Hoop.Side.LEFT,
                COURT_WIDTH * 0.5 + PLAYER_START_OFFSET,
                FLOOR_Y
        );
        ball = new Ball(COURT_WIDTH * 0.5, FLOOR_Y - BALL_START_HEIGHT);

        Scene scene = new Scene(new Group(canvas), COURT_WIDTH, COURT_HEIGHT);
        scene.setFill(Color.rgb(12, 16, 28));
        scene.setOnKeyPressed(event -> pressedKeys.add(event.getCode()));
        scene.setOnKeyReleased(event -> pressedKeys.remove(event.getCode()));

        stage.setTitle("Pixel Basketball 1v1");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        new AnimationTimer() {
            private long previousFrameNanos;

            @Override
            public void handle(long now) {
                if (previousFrameNanos == 0L) {
                    previousFrameNanos = now;
                    return;
                }
                double dt = Math.min((now - previousFrameNanos) / 1_000_000_000.0, 1.0 / 30.0);
                previousFrameNanos = now;
                update(dt);
                render();
                previousKeys.clear();
                previousKeys.addAll(pressedKeys);
            }
        }.start();
    }

    private void update(double dt) {
        updateTimers(dt);
        playerOne.update(
                dt,
                pressedKeys.contains(KeyCode.A),
                pressedKeys.contains(KeyCode.D),
                pressedKeys.contains(KeyCode.W),
                COURT_WIDTH,
                FLOOR_Y
        );
        playerTwo.update(
                dt,
                pressedKeys.contains(KeyCode.LEFT),
                pressedKeys.contains(KeyCode.RIGHT),
                pressedKeys.contains(KeyCode.UP),
                COURT_WIDTH,
                FLOOR_Y
        );

        updateShotCharge(playerOne, playerOneShotCharge, dt, KeyCode.F);
        updateShotCharge(playerTwo, playerTwoShotCharge, dt, KeyCode.SLASH, KeyCode.M);
        handleStealInput(playerOne, playerTwo, playerOneShotCharge, true, KeyCode.G);
        handleStealInput(playerTwo, playerOne, playerTwoShotCharge, false, KeyCode.PERIOD);

        ball.update(dt, COURT_WIDTH, FLOOR_Y, hoops);
        detectScores();
        handleAutoPickup();
        handleScoreReset(dt);
    }

    private void updateTimers(double dt) {
        playerOneStealCooldown = Math.max(0.0, playerOneStealCooldown - dt);
        playerTwoStealCooldown = Math.max(0.0, playerTwoStealCooldown - dt);
        stealNoticeTimer = Math.max(0.0, stealNoticeTimer - dt);
    }

    private void updateShotCharge(Player player, ShotCharge shotCharge, double dt, KeyCode... shootKeys) {
        boolean shootHeld = isAnyPressed(shootKeys);

        if (!shotCharge.isCharging()) {
            if (shootHeld && ball.carrier() == player && scoreResetTimer <= 0.0) {
                shotCharge.start();
            }
            return;
        }

        if (ball.carrier() != player || scoreResetTimer > 0.0) {
            shotCharge.cancel();
            return;
        }

        if (shootHeld) {
            shotCharge.charge(dt);
        } else {
            ball.shootAt(hoopForSide(player.attackingSide()), shotCharge.power());
            shotCharge.cancel();
        }
    }

    private boolean isAnyPressed(KeyCode... keys) {
        for (KeyCode key : keys) {
            if (pressedKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean wasAnyJustPressed(KeyCode... keys) {
        for (KeyCode key : keys) {
            if (pressedKeys.contains(key) && !previousKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private void handleStealInput(Player thief,
                                  Player defender,
                                  ShotCharge thiefShotCharge,
                                  boolean playerOneAttempt,
                                  KeyCode... stealKeys) {
        if (!wasAnyJustPressed(stealKeys) || scoreResetTimer > 0.0 || ball.carrier() != defender) {
            return;
        }

        double cooldown = playerOneAttempt ? playerOneStealCooldown : playerTwoStealCooldown;
        if (cooldown > 0.0) {
            return;
        }

        double distance = thief.center().distance(defender.center());
        if (distance > STEAL_RANGE) {
            return;
        }

        if (playerOneAttempt) {
            playerOneStealCooldown = STEAL_COOLDOWN;
        } else {
            playerTwoStealCooldown = STEAL_COOLDOWN;
        }
        thiefShotCharge.cancel();

        double closeness = 1.0 - clamp(distance / STEAL_RANGE, 0.0, 1.0);
        double stealChance = STEAL_BASE_CHANCE + closeness * STEAL_CLOSENESS_BONUS;
        if (isFacing(thief, defender)) {
            stealChance += STEAL_FACING_BONUS;
        }
        stealChance = clamp(stealChance, 0.12, 0.82);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() < stealChance) {
            ball.attachTo(thief);
            showStealNotice(thief.name() + " steal!");
            return;
        }

        double looseChance = STEAL_LOOSE_BASE_CHANCE + closeness * STEAL_LOOSE_CLOSENESS_BONUS;
        if (random.nextDouble() < looseChance) {
            double direction = defender.center().getX() - thief.center().getX();
            if (Math.abs(direction) < 0.01) {
                direction = thief.isFacingRight() ? 1.0 : -1.0;
            }
            ball.knockLooseFrom(defender, direction);
            showStealNotice("Loose ball!");
        } else {
            showStealNotice("Steal missed");
        }
    }

    private boolean isFacing(Player source, Player target) {
        double direction = target.center().getX() - source.center().getX();
        return (direction >= 0.0) == source.isFacingRight();
    }

    private void showStealNotice(String text) {
        stealNotice = text;
        stealNoticeTimer = STEAL_NOTICE_TIME;
    }

    private void detectScores() {
        if (ball.isCarried()) {
            lockedScoringHoop = null;
            return;
        }
        if (lockedScoringHoop != null) {
            Hoop lockedHoop = hoopForSide(lockedScoringHoop);
            if (ball.y() > lockedHoop.scoreWindow().planeY() + SCORE_LOCK_RELEASE_Y) {
                lockedScoringHoop = null;
            }
            return;
        }

        for (Hoop hoop : hoops) {
            if (hoop.isMadeBasket(
                    ball.previousX(),
                    ball.previousY(),
                    ball.x(),
                    ball.y(),
                    ball.velocityY()
            )) {
                scoreByHoop.merge(hoop.side(), 1, Integer::sum);
                lockedScoringHoop = hoop.side();
                scoreResetTimer = SCORE_RESET_DELAY;
                break;
            }
        }
    }

    private void handleAutoPickup() {
        if (scoreResetTimer > 0.0 || ball.isCarried()) {
            return;
        }
        if (ball.canBePickedUpBy(playerOne)) {
            ball.attachTo(playerOne);
        } else if (ball.canBePickedUpBy(playerTwo)) {
            ball.attachTo(playerTwo);
        }
    }

    private void handleScoreReset(double dt) {
        if (scoreResetTimer <= 0.0) {
            return;
        }
        scoreResetTimer -= dt;
        if (scoreResetTimer <= 0.0) {
            resetAfterScore();
        }
    }

    private void resetAfterScore() {
        playerOne.reset(FLOOR_Y);
        playerTwo.reset(FLOOR_Y);
        ball.reset(COURT_WIDTH * 0.5, FLOOR_Y - BALL_START_HEIGHT);
        lockedScoringHoop = null;
        playerOneShotCharge.cancel();
        playerTwoShotCharge.cancel();
        playerOneStealCooldown = 0.0;
        playerTwoStealCooldown = 0.0;
    }

    private Hoop hoopForSide(Hoop.Side side) {
        for (Hoop hoop : hoops) {
            if (hoop.side() == side) {
                return hoop;
            }
        }
        throw new IllegalArgumentException("No hoop configured for side: " + side);
    }

    private void render() {
        drawCourt();
        for (Hoop hoop : hoops) {
            hoop.draw(gc);
        }
        ball.draw(gc);
        playerOne.draw(gc);
        playerTwo.draw(gc);
        drawPowerMeter(playerOne, playerOneShotCharge);
        drawPowerMeter(playerTwo, playerTwoShotCharge);
        drawStealNotice();
        drawHud();
    }

    private void drawCourt() {
        gc.setImageSmoothing(false);
        gc.setFill(Color.rgb(16, 22, 38));
        gc.fillRect(0.0, 0.0, COURT_WIDTH, COURT_HEIGHT);

        gc.setFill(Color.rgb(35, 49, 72));
        gc.fillRect(0.0, FLOOR_Y, COURT_WIDTH, COURT_HEIGHT - FLOOR_Y);

        gc.setStroke(Color.rgb(74, 93, 124));
        gc.setLineWidth(4.0);
        gc.strokeLine(COURT_WIDTH * 0.5, FLOOR_Y, COURT_WIDTH * 0.5, COURT_HEIGHT);
        gc.strokeOval(
                COURT_WIDTH * 0.5 - CENTER_CIRCLE_RADIUS,
                FLOOR_Y - CENTER_CIRCLE_RADIUS * 0.45,
                CENTER_CIRCLE_RADIUS * 2.0,
                CENTER_CIRCLE_RADIUS * 0.9
        );

        gc.setStroke(Color.rgb(56, 74, 102));
        gc.setLineWidth(1.0);
        for (int row = 0; row < 6; row++) {
            double y = FLOOR_Y + row * 10.0;
            gc.strokeLine(0.0, y, COURT_WIDTH, y);
        }
    }

    private void drawHud() {
        gc.save();
        gc.setFont(Font.font("Monospaced", 24.0));
        gc.setFill(Color.WHITE);
        gc.fillText("P2 " + scoreByHoop.get(Hoop.Side.LEFT), 34.0, 42.0);
        gc.fillText(scoreByHoop.get(Hoop.Side.RIGHT) + " P1", COURT_WIDTH - 118.0, 42.0);

        gc.setFont(Font.font("Monospaced", 13.0));
        gc.setFill(Color.rgb(210, 220, 235));
        gc.fillText("P1: A/D move, W jump, hold F, G steal", 284.0, 34.0);
        gc.fillText("P2: Arrows move, hold / or M, . steal", 284.0, 54.0);
        gc.restore();
    }

    private void drawStealNotice() {
        if (stealNoticeTimer <= 0.0 || stealNotice.isBlank()) {
            return;
        }

        gc.save();
        gc.setFont(Font.font("Monospaced", 18.0));
        gc.setFill(Color.rgb(255, 238, 158, Math.min(1.0, stealNoticeTimer / STEAL_NOTICE_TIME + 0.15)));
        gc.fillText(stealNotice, COURT_WIDTH * 0.5 - 68.0, 90.0);
        gc.restore();
    }

    private void drawPowerMeter(Player player, ShotCharge shotCharge) {
        if (!shotCharge.isCharging() || ball.carrier() != player) {
            return;
        }

        Rectangle2D bounds = player.bounds();
        double x = bounds.getMinX() + bounds.getWidth() * 0.5 - POWER_METER_WIDTH * 0.5;
        double y = bounds.getMinY() - 31.0;
        double fillWidth = POWER_METER_WIDTH * normalizePower(shotCharge.power());
        double standardX = x + POWER_METER_WIDTH * normalizePower(SHOT_POWER_STANDARD);

        gc.save();
        gc.setFill(Color.rgb(7, 10, 18, 0.78));
        gc.fillRect(Math.round(x - 2.0), Math.round(y - 2.0), POWER_METER_WIDTH + 4.0, POWER_METER_HEIGHT + 4.0);
        gc.setFill(powerColor(shotCharge.power()));
        gc.fillRect(Math.round(x), Math.round(y), Math.round(fillWidth), POWER_METER_HEIGHT);
        gc.setStroke(Color.rgb(235, 242, 255));
        gc.setLineWidth(1.0);
        gc.strokeRect(Math.round(x), Math.round(y), POWER_METER_WIDTH, POWER_METER_HEIGHT);
        gc.strokeLine(Math.round(standardX), Math.round(y - 2.0), Math.round(standardX), Math.round(y + POWER_METER_HEIGHT + 2.0));
        gc.restore();
    }

    private static Color powerColor(double power) {
        if (power < 1.0) {
            return Color.rgb(82, 183, 255);
        }
        if (power < 1.12) {
            return Color.rgb(112, 224, 118);
        }
        return Color.rgb(255, 185, 75);
    }

    private static double normalizePower(double power) {
        return clamp((power - SHOT_POWER_MIN) / (SHOT_POWER_MAX - SHOT_POWER_MIN), 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ShotCharge {
        private double power = SHOT_POWER_START;
        private boolean charging;

        void start() {
            power = SHOT_POWER_START;
            charging = true;
        }

        void charge(double dt) {
            power = Math.min(SHOT_POWER_MAX, power + SHOT_CHARGE_RATE * dt);
        }

        void cancel() {
            power = SHOT_POWER_START;
            charging = false;
        }

        double power() {
            return power;
        }

        boolean isCharging() {
            return charging;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
