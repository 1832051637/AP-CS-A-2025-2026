package com.crystalbreak.model;

import com.crystalbreak.physics.Vector2;
import com.crystalbreak.util.GameConstants;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GameState {
    private final Table table = new Table(GameConstants.TABLE_X, GameConstants.TABLE_Y, GameConstants.TABLE_WIDTH, GameConstants.TABLE_HEIGHT);
    private final List<Ball> balls = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final Random random = new Random();
    private GameMode mode = GameMode.CLASSIC;
    private GamePhase phase = GamePhase.AIMING;
    private int currentPlayerIndex;
    private boolean ballInHand;
    private String statusMessage = "Ready";
    private CrystalCore crystalCore;
    private SkillChallenge skillChallenge;
    private double elapsedSeconds;
    private int shotCount;

    public void reset(GameMode mode, PlayerProgress humanProgress, boolean versusAi) {
        this.mode = mode;
        phase = GamePhase.AIMING;
        currentPlayerIndex = 0;
        ballInHand = false;
        statusMessage = mode.displayName();
        crystalCore = null;
        skillChallenge = null;
        elapsedSeconds = 0.0;
        shotCount = 0;
        activeEffects.clear();
        table.resetPockets();
        players.clear();
        players.add(new Player("Player", true, humanProgress));
        players.add(new Player(versusAi ? "Computer" : "Player 2", !versusAi, new PlayerProgress()));
        rackBalls();
    }

    private void rackBalls() {
        balls.clear();
        Ball cue = new Ball(0, 0, BallGroup.CUE, "Cue", new Vector2(table.getX() + table.getWidth() * 0.25, table.getY() + table.getHeight() / 2.0),
                GameConstants.CUE_BALL_RADIUS, 1.0, Color.WHITE);
        balls.add(cue);

        double startX = table.getX() + table.getWidth() * 0.68;
        double startY = table.getY() + table.getHeight() / 2.0;
        int[] numbers = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};
        int index = 0;
        double spacing = GameConstants.BALL_RADIUS * 2.05;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col <= row; col++) {
                int number = numbers[index++];
                double x = startX + row * spacing;
                double y = startY + (col - row / 2.0) * spacing;
                balls.add(createNumberedBall(index, number, new Vector2(x, y)));
            }
        }
    }

    private Ball createNumberedBall(int id, int number, Vector2 position) {
        BallGroup group = number == 8 ? BallGroup.EIGHT : number <= 7 ? BallGroup.SOLID : BallGroup.STRIPE;
        Color color = switch (number) {
            case 1, 9 -> Color.web("#f3c64e");
            case 2, 10 -> Color.web("#3377d6");
            case 3, 11 -> Color.web("#d94e4e");
            case 4, 12 -> Color.web("#6d52a8");
            case 5, 13 -> Color.web("#e8843a");
            case 6, 14 -> Color.web("#2f9a62");
            case 7, 15 -> Color.web("#8b3f32");
            case 8 -> Color.web("#161616");
            default -> Color.LIGHTGRAY;
        };
        return new Ball(id, number, group, String.valueOf(number), position, GameConstants.BALL_RADIUS, 1.0, color);
    }

    public Table getTable() {
        return table;
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public Player getOtherPlayer() {
        return players.get(1 - currentPlayerIndex);
    }

    public void switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public Optional<Ball> getCueBall() {
        return balls.stream().filter(Ball::isCueBall).findFirst();
    }

    public Optional<Ball> findBallByNumber(int number) {
        return balls.stream().filter(ball -> ball.getNumber() == number).findFirst();
    }

    public Optional<Ball> getBossBall() {
        return balls.stream().filter(ball -> ball.getGroup() == BallGroup.BOSS && !ball.isPocketed()).findFirst();
    }

    public List<Ball> getObjectBallsRemaining() {
        return balls.stream()
                .filter(ball -> !ball.isPocketed())
                .filter(Ball::isObjectBall)
                .sorted(Comparator.comparingInt(Ball::getNumber))
                .toList();
    }

    public GameMode getMode() {
        return mode;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public boolean isBallInHand() {
        return ballInHand;
    }

    public void setBallInHand(boolean ballInHand) {
        this.ballInHand = ballInHand;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public List<ActiveEffect> getActiveEffects() {
        return activeEffects;
    }

    public void addEffect(ActiveEffect effect) {
        activeEffects.removeIf(existing -> existing.getType() == effect.getType());
        activeEffects.add(effect);
        statusMessage = "Effect: " + effect.getType().label();
    }

    public boolean hasEffect(EffectType type) {
        return activeEffects.stream().anyMatch(effect -> effect.getType() == type && !effect.isExpired());
    }

    public double getPowerMultiplier() {
        return hasEffect(EffectType.POWER_BOOST) ? 1.5 : 1.0;
    }

    public double getBallSpeedMultiplier() {
        double multiplier = 1.0;
        if (hasEffect(EffectType.SLOW_MOTION)) {
            multiplier *= 0.5;
        }
        if (hasEffect(EffectType.SPEED_BOOST)) {
            multiplier *= 2.0;
        }
        return multiplier;
    }

    public double getBallRadiusScale() {
        if (hasEffect(EffectType.BIG_BALLS)) {
            return 1.25;
        }
        if (hasEffect(EffectType.SMALL_BALLS)) {
            return 0.78;
        }
        return 1.0;
    }

    public double getPocketScale() {
        if (hasEffect(EffectType.LARGE_POCKETS)) {
            return 1.35;
        }
        if (hasEffect(EffectType.SMALL_POCKETS)) {
            return 0.72;
        }
        return 1.0;
    }

    public double getTableInset() {
        return hasEffect(EffectType.SHRINK_TABLE) ? 45.0 : 0.0;
    }

    public boolean isDoubleScore() {
        return hasEffect(EffectType.DOUBLE_SCORE);
    }

    public CrystalCore getCrystalCore() {
        return crystalCore;
    }

    public void setCrystalCore(CrystalCore crystalCore) {
        this.crystalCore = crystalCore;
    }

    public SkillChallenge getSkillChallenge() {
        return skillChallenge;
    }

    public void setSkillChallenge(SkillChallenge skillChallenge) {
        this.skillChallenge = skillChallenge;
    }

    public Random getRandom() {
        return random;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void addElapsedSeconds(double delta) {
        elapsedSeconds += delta;
    }

    public int getShotCount() {
        return shotCount;
    }

    public void incrementShotCount() {
        shotCount++;
    }
}
