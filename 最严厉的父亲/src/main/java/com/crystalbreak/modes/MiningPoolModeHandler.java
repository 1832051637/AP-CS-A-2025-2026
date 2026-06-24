package com.crystalbreak.modes;

import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.OreType;
import com.crystalbreak.model.Player;
import com.crystalbreak.model.ShotResult;

public class MiningPoolModeHandler implements GameModeHandler {
    private static final OreType[] ORES = {
            OreType.COAL, OreType.IRON, OreType.GOLD, OreType.COAL, OreType.DIAMOND,
            OreType.IRON, OreType.GOLD, OreType.EMERALD, OreType.COAL, OreType.IRON,
            OreType.GOLD, OreType.COAL, OreType.DIAMOND, OreType.IRON, OreType.EMERALD
    };

    @Override
    public void onGameStart(GameState state) {
        int oreIndex = 0;
        for (Ball ball : state.getBalls()) {
            if (ball.getGroup() == BallGroup.CUE) {
                continue;
            }
            OreType ore = ORES[oreIndex++ % ORES.length];
            ball.setGroup(BallGroup.ORE);
            ball.setOreType(ore);
            ball.setLabel(ore.displayName());
            ball.setColor(ore.color());
        }
        state.setStatusMessage("Mine ores into pockets to earn coins.");
    }

    @Override
    public void onShotEnded(GameState state, ShotResult result) {
        Player shooter = state.getCurrentPlayer();
        int earned = 0;
        for (Ball ball : result.getPottedBalls()) {
            if (ball.getOreType() != null) {
                earned += ball.getOreType().coinValue();
            }
        }
        if (earned > 0) {
            if (state.isDoubleScore()) {
                earned *= 2;
            }
            shooter.getProgress().addCoins(earned);
            shooter.getProgress().addExperience(earned / 3);
            shooter.addScore(earned);
            state.setStatusMessage("Mined +" + earned + " coins.");
        }
    }
}
