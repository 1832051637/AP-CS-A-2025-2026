package com.crystalbreak.modes;

import com.crystalbreak.model.ActiveEffect;
import com.crystalbreak.model.EffectType;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.Pocket;
import com.crystalbreak.physics.Vector2;

import java.util.List;

public class ChaosModeHandler implements GameModeHandler {
    private static final List<EffectType> CHAOS_EVENTS = List.of(
            EffectType.REVERSE_GRAVITY,
            EffectType.TABLE_TILT,
            EffectType.SPEED_BOOST,
            EffectType.SHRINK_TABLE,
            EffectType.MOVING_POCKETS,
            EffectType.BLACK_HOLE
    );

    private double nextEventSeconds;

    @Override
    public void onGameStart(GameState state) {
        nextEventSeconds = 60.0;
        state.setStatusMessage("Chaos event in 60 seconds.");
    }

    @Override
    public void onTick(GameState state, double deltaSeconds) {
        nextEventSeconds -= deltaSeconds;
        if (nextEventSeconds <= 0.0) {
            EffectType event = CHAOS_EVENTS.get(state.getRandom().nextInt(CHAOS_EVENTS.size()));
            state.addEffect(new ActiveEffect(event, 18.0, 1.0));
            nextEventSeconds = 60.0;
            state.setStatusMessage("Chaos: " + event.label());
        }
        if (state.hasEffect(EffectType.MOVING_POCKETS)) {
            animatePockets(state);
        } else {
            state.getTable().resetPockets();
        }
    }

    @Override
    public String modeStatus(GameState state) {
        return "Chaos in " + (int) Math.ceil(nextEventSeconds) + "s";
    }

    private void animatePockets(GameState state) {
        double time = state.getElapsedSeconds();
        int index = 0;
        for (Pocket pocket : state.getTable().getPockets()) {
            double dx = Math.sin(time * 1.1 + index * 0.9) * 18.0;
            double dy = Math.cos(time * 1.3 + index * 0.7) * 14.0;
            pocket.setPosition(pocket.getBasePosition().add(new Vector2(dx, dy)));
            index++;
        }
    }
}
