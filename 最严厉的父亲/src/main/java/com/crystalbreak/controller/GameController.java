package com.crystalbreak.controller;

import com.crystalbreak.ai.ComputerPlayer;
import com.crystalbreak.ai.Difficulty;
import com.crystalbreak.ai.ShotPlan;
import com.crystalbreak.audio.SoundManager;
import com.crystalbreak.audio.SoundType;
import com.crystalbreak.model.Ball;
import com.crystalbreak.model.GameMode;
import com.crystalbreak.model.GamePhase;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.Player;
import com.crystalbreak.model.ShotParameters;
import com.crystalbreak.model.ShotResult;
import com.crystalbreak.modes.GameModeHandler;
import com.crystalbreak.modes.ModeFactory;
import com.crystalbreak.persistence.SaveData;
import com.crystalbreak.persistence.SaveManager;
import com.crystalbreak.physics.PhysicsEngine;
import com.crystalbreak.physics.Vector2;
import com.crystalbreak.util.GameConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class GameController {
    private static final double MAX_STRIKE_DEFLECTION_RADIANS = Math.toRadians(2.4);
    private static final double OFF_CENTER_POWER_LOSS = 0.07;

    private final GameState state = new GameState();
    private final PhysicsEngine physicsEngine = new PhysicsEngine();
    private final RuleEngine ruleEngine = new RuleEngine();
    private final SoundManager soundManager;
    private final SaveManager saveManager;
    private final SaveData saveData;
    private final Random random = new Random();
    private GameModeHandler modeHandler = ModeFactory.create(GameMode.CLASSIC);
    private ComputerPlayer computerPlayer = new ComputerPlayer(Difficulty.NORMAL);
    private ShotResult activeShot;
    private double aiThinkTimer;

    public GameController(SoundManager soundManager, SaveManager saveManager, SaveData saveData) {
        this.soundManager = soundManager;
        this.saveManager = saveManager;
        this.saveData = saveData;
        newGame(GameMode.CLASSIC, true, Difficulty.NORMAL);
    }

    public void newGame(GameMode mode, boolean versusAi, Difficulty difficulty) {
        state.reset(mode, saveData.getProgress(), versusAi);
        state.getPlayers().get(0).getCueStick().applyLevel(saveData.getCueLevel());
        modeHandler = ModeFactory.create(mode);
        modeHandler.onGameStart(state);
        computerPlayer = new ComputerPlayer(difficulty);
        aiThinkTimer = 0.85;
        activeShot = null;
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || deltaSeconds > 0.08) {
            deltaSeconds = 1.0 / 60.0;
        }
        state.addElapsedSeconds(deltaSeconds);
        tickEffects(deltaSeconds);
        if (state.getPhase() == GamePhase.GAME_OVER) {
            return;
        }

        modeHandler.onTick(state, deltaSeconds);
        if (state.getPhase() == GamePhase.ROLLING && activeShot != null) {
            boolean moving = physicsEngine.update(state, deltaSeconds, activeShot);
            modeHandler.afterPhysics(state, deltaSeconds, activeShot);
            if (!moving) {
                finishShot();
            }
            return;
        }

        if (state.getPhase() == GamePhase.AIMING && !state.getCurrentPlayer().isHuman()) {
            aiThinkTimer -= deltaSeconds;
            if (aiThinkTimer <= 0.0) {
                executeAiShot();
            }
        }
    }

    public boolean strikeAt(Vector2 targetPoint, double power, double sideSpin, double topSpin) {
        Optional<Ball> cueBall = state.getCueBall();
        if (cueBall.isEmpty()) {
            return false;
        }
        return strikeDirection(targetPoint.subtract(cueBall.get().getPosition()).normalized(), power, sideSpin, topSpin);
    }

    public boolean strikeDirection(Vector2 direction, double power, double sideSpin, double topSpin) {
        if (state.getPhase() != GamePhase.AIMING || direction.isNearlyZero() || state.isBallInHand()) {
            return false;
        }
        Ball cue = state.getCueBall().orElse(null);
        if (cue == null || cue.isPocketed()) {
            return false;
        }
        Player shooter = state.getCurrentPlayer();
        double shotPower = Math.max(0.0, Math.min(1.0, power));
        if (shotPower <= 0.0) {
            return false;
        }
        double accuracyNoise = shooter.isHuman() ? 0.0 : (1.0 / shooter.getCueStick().getAccuracy()) * 0.01;
        direction = rotate(direction, (random.nextDouble() - 0.5) * accuracyNoise);
        direction = rotate(direction, sideSpin * MAX_STRIKE_DEFLECTION_RADIANS).normalized();

        double cuePower = shooter.getCueStick().getPower();
        double offCenter = Math.min(1.0, Math.hypot(sideSpin, topSpin));
        double speed = GameConstants.MAX_SHOT_SPEED * shotPower * cuePower * state.getPowerMultiplier()
                * (1.0 - offCenter * OFF_CENTER_POWER_LOSS);
        cue.setVelocity(direction.normalized().multiply(speed));
        cue.getSpin().setSideSpin(sideSpin * shooter.getCueStick().getSpin());
        cue.getSpin().setTopSpin(topSpin * shooter.getCueStick().getSpin());
        activeShot = new ShotResult();
        state.incrementShotCount();
        state.setPhase(GamePhase.ROLLING);
        state.setStatusMessage(shooter.getName() + " shot #" + state.getShotCount());
        modeHandler.onShotStarted(state, new ShotParameters(direction, shotPower, sideSpin, topSpin));
        soundManager.play(SoundType.HIT);
        return true;
    }

    public boolean placeCueBall(Vector2 position) {
        if (!state.isBallInHand() || !canPlaceCueBall(position)) {
            return false;
        }
        state.getCueBall().ifPresent(cue -> {
            cue.setPosition(position);
            cue.setVelocity(Vector2.ZERO);
            cue.setPocketed(false);
        });
        state.setBallInHand(false);
        state.setStatusMessage(state.getCurrentPlayer().getName() + " placed the free ball.");
        return true;
    }

    public boolean canPlaceCueBall(Vector2 position) {
        double radius = GameConstants.CUE_BALL_RADIUS * state.getBallRadiusScale();
        if (position.x() < state.getTable().getMinX(state.getTableInset()) + radius
                || position.x() > state.getTable().getMaxX(state.getTableInset()) - radius
                || position.y() < state.getTable().getMinY(state.getTableInset()) + radius
                || position.y() > state.getTable().getMaxY(state.getTableInset()) - radius) {
            return false;
        }
        return state.getBalls().stream()
                .filter(ball -> !ball.isCueBall() && !ball.isPocketed())
                .noneMatch(ball -> ball.getPosition().distance(position) < ball.getRadius() + radius + 1.0);
    }

    public boolean upgradeCue() {
        Player human = state.getPlayers().get(0);
        boolean upgraded = human.getCueStick().upgrade(saveData.getProgress());
        if (upgraded) {
            saveData.setCueLevel(human.getCueStick().getLevel());
            saveManager.save(saveData);
            state.setStatusMessage("Cue upgraded to level " + human.getCueStick().getLevel());
        }
        return upgraded;
    }

    public List<Vector2> aimAssistPath(Vector2 targetPoint) {
        List<Vector2> points = new ArrayList<>();
        state.getCueBall().ifPresent(cue -> {
            Vector2 start = cue.getPosition();
            Vector2 direction = targetPoint.subtract(start).normalized();
            if (!direction.isNearlyZero()) {
                points.add(start);
                points.add(start.add(direction.multiply(320.0)));
            }
        });
        return points;
    }

    public GameState getState() {
        return state;
    }

    public SaveData getSaveData() {
        return saveData;
    }

    public GameModeHandler getModeHandler() {
        return modeHandler;
    }

    public SaveManager getSaveManager() {
        return saveManager;
    }

    private void executeAiShot() {
        if (state.isBallInHand()) {
            Vector2 spot = new Vector2(
                    state.getTable().getX() + state.getTable().getWidth() * 0.24,
                    state.getTable().getY() + state.getTable().getHeight() * (0.35 + random.nextDouble() * 0.3)
            );
            if (!placeCueBall(spot)) {
                placeCueBall(new Vector2(state.getTable().getX() + 160.0, state.getTable().getY() + state.getTable().getHeight() / 2.0));
            }
        }
        ShotPlan plan = computerPlayer.chooseShot(state);
        strikeDirection(plan.direction(), plan.power(), plan.sideSpin(), plan.topSpin());
        aiThinkTimer = 1.0;
    }

    private void finishShot() {
        state.getBalls().forEach(ball -> ball.setVelocity(Vector2.ZERO));
        if (!activeShot.getPottedBalls().isEmpty()) {
            soundManager.play(SoundType.POCKET);
        }
        modeHandler.onShotEnded(state, activeShot);
        ruleEngine.resolveShot(state, activeShot);
        recordScores();
        if (state.getPhase() == GamePhase.GAME_OVER) {
            soundManager.play(SoundType.VICTORY);
        } else {
            state.setPhase(GamePhase.AIMING);
            aiThinkTimer = 0.85;
        }
        saveManager.save(saveData);
        activeShot = null;
    }

    private void tickEffects(double deltaSeconds) {
        state.getActiveEffects().forEach(effect -> effect.tick(deltaSeconds));
        state.getActiveEffects().removeIf(effect -> {
            boolean expired = effect.isExpired();
            if (expired) {
                state.setStatusMessage(effect.getType().label() + " ended.");
            }
            return expired;
        });
    }

    private void recordScores() {
        int bestScore = state.getPlayers().stream().mapToInt(Player::getScore).max().orElse(0);
        saveData.recordScore(state.getMode().name(), bestScore);
    }

    private Vector2 rotate(Vector2 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector2(vector.x() * cos - vector.y() * sin, vector.x() * sin + vector.y() * cos).normalized();
    }
}
