package com.crystalbreak.modes;

import com.crystalbreak.model.ActiveEffect;
import com.crystalbreak.model.Ball;
import com.crystalbreak.model.CrystalCore;
import com.crystalbreak.model.EffectType;
import com.crystalbreak.model.GameState;
import com.crystalbreak.physics.Vector2;

import java.util.List;

public class CrystalCoreModeHandler implements GameModeHandler {
    private static final List<EffectType> CORE_EFFECTS = List.of(
            EffectType.POWER_BOOST,
            EffectType.SLOW_MOTION,
            EffectType.BIG_BALLS,
            EffectType.SMALL_BALLS,
            EffectType.DOUBLE_SCORE,
            EffectType.LARGE_POCKETS,
            EffectType.SMALL_POCKETS
    );

    @Override
    public void onGameStart(GameState state) {
        CrystalCore core = new CrystalCore();
        core.setRefreshTimer(1.0);
        state.setCrystalCore(core);
    }

    @Override
    public void onTick(GameState state, double deltaSeconds) {
        CrystalCore core = state.getCrystalCore();
        if (core == null) {
            return;
        }
        if (!core.isActive()) {
            core.setRefreshTimer(core.getRefreshTimer() - deltaSeconds);
            if (core.getRefreshTimer() <= 0.0) {
                spawnCore(state, core);
            }
            return;
        }

        for (Ball ball : state.getBalls()) {
            if (ball.isPocketed()) {
                continue;
            }
            double hitDistance = ball.getRadius() * state.getBallRadiusScale() + core.getRadius();
            if (ball.getPosition().distance(core.getPosition()) <= hitDistance) {
                EffectType type = CORE_EFFECTS.get(state.getRandom().nextInt(CORE_EFFECTS.size()));
                state.addEffect(new ActiveEffect(type, 15.0, 1.0));
                core.setActive(false);
                core.setRefreshTimer(30.0);
                Vector2 bounce = ball.getPosition().subtract(core.getPosition()).normalized().multiply(120.0);
                ball.setVelocity(ball.getVelocity().add(bounce));
                break;
            }
        }
    }

    @Override
    public String modeStatus(GameState state) {
        CrystalCore core = state.getCrystalCore();
        if (core == null) {
            return "";
        }
        return core.isActive() ? "Crystal Core active" : "Core refresh: " + (int) Math.ceil(core.getRefreshTimer()) + "s";
    }

    private void spawnCore(GameState state, CrystalCore core) {
        double x = state.getTable().getX() + state.getTable().getWidth() * (0.38 + state.getRandom().nextDouble() * 0.24);
        double y = state.getTable().getY() + state.getTable().getHeight() * (0.34 + state.getRandom().nextDouble() * 0.32);
        core.setPosition(new Vector2(x, y));
        core.setActive(true);
        state.setStatusMessage("Crystal Core spawned!");
    }
}
