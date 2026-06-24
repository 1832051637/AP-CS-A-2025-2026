package com.crystalbreak.modes;

import com.crystalbreak.model.ActiveEffect;
import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.EffectType;
import com.crystalbreak.model.GamePhase;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.ShotResult;
import com.crystalbreak.physics.Vector2;
import javafx.scene.paint.Color;

import java.util.List;

public class BossChallengeModeHandler implements GameModeHandler {
    private double specialTimer;

    @Override
    public void onGameStart(GameState state) {
        Ball boss = new Ball(99, 99, BallGroup.BOSS, "Boss",
                new Vector2(state.getTable().getX() + state.getTable().getWidth() * 0.52,
                        state.getTable().getY() + state.getTable().getHeight() * 0.5),
                19.0, 2.8, Color.web("#b44cff"));
        boss.setHitPoints(7);
        state.getBalls().add(boss);
        specialTimer = 12.0;
        state.setStatusMessage("Boss ball entered the table.");
    }

    @Override
    public void onTick(GameState state, double deltaSeconds) {
        state.getBossBall().ifPresent(boss -> {
            double t = state.getElapsedSeconds();
            Vector2 wander = new Vector2(Math.sin(t * 1.4) * 14.0, Math.cos(t * 1.1) * 12.0);
            boss.setVelocity(boss.getVelocity().add(wander.multiply(deltaSeconds)).limit(180.0));
        });
        specialTimer -= deltaSeconds;
        if (specialTimer <= 0.0) {
            releaseBossEffect(state);
            specialTimer = 12.0 + state.getRandom().nextDouble() * 8.0;
        }
    }

    @Override
    public void afterPhysics(GameState state, double deltaSeconds, ShotResult activeShot) {
        state.getBossBall().ifPresent(boss -> {
            for (Ball ball : state.getBalls()) {
                if (ball == boss || ball.isPocketed()) {
                    continue;
                }
                double hitDistance = boss.getRadius() + ball.getRadius() * state.getBallRadiusScale() + 2.0;
                if (boss.getBossHitCooldown() <= 0.0 && boss.getPosition().distance(ball.getPosition()) <= hitDistance && ball.getVelocity().length() > 70.0) {
                    boss.setHitPoints(boss.getHitPoints() - 1);
                    boss.setBossHitCooldown(0.65);
                    state.getCurrentPlayer().addScore(35);
                    state.setStatusMessage("Boss HP: " + boss.getHitPoints());
                    if (boss.getHitPoints() <= 0) {
                        boss.setPocketed(true);
                        state.getCurrentPlayer().addScore(500);
                        state.getCurrentPlayer().getProgress().addCoins(300);
                        state.getCurrentPlayer().getProgress().addExperience(180);
                        state.setPhase(GamePhase.GAME_OVER);
                        state.setStatusMessage(state.getCurrentPlayer().getName() + " defeated the Boss!");
                    }
                    break;
                }
            }
        });
    }

    @Override
    public String modeStatus(GameState state) {
        return state.getBossBall()
                .map(boss -> "Boss HP " + boss.getHitPoints() + " | Skill in " + (int) Math.ceil(specialTimer) + "s")
                .orElse("Boss defeated");
    }

    private void releaseBossEffect(GameState state) {
        List<EffectType> effects = List.of(EffectType.BLACK_HOLE, EffectType.SLOW_MOTION, EffectType.BOSS_SHOCKWAVE);
        EffectType effect = effects.get(state.getRandom().nextInt(effects.size()));
        state.addEffect(new ActiveEffect(effect, 8.0, 1.0));
        if (effect == EffectType.BOSS_SHOCKWAVE) {
            state.getBossBall().ifPresent(boss -> state.getBalls().forEach(ball -> {
                if (ball != boss && !ball.isPocketed()) {
                    Vector2 push = ball.getPosition().subtract(boss.getPosition()).normalized().multiply(150.0);
                    ball.setVelocity(ball.getVelocity().add(push));
                }
            }));
        }
    }
}
