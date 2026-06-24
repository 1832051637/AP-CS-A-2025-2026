package com.crystalbreak.modes;

import com.crystalbreak.model.GameMode;

public final class ModeFactory {
    private ModeFactory() {
    }

    public static GameModeHandler create(GameMode mode) {
        return switch (mode) {
            case CLASSIC -> new ClassicModeHandler();
            case CRYSTAL_CORE -> new CrystalCoreModeHandler();
            case MINING_POOL -> new MiningPoolModeHandler();
            case SKILL_SHOT -> new SkillShotModeHandler();
            case CHAOS -> new ChaosModeHandler();
            case BOSS_CHALLENGE -> new BossChallengeModeHandler();
        };
    }
}
