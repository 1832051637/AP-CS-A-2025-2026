package com.crystalbreak.model;

public enum GameMode {
    CLASSIC("Classic 8-Ball"),
    CRYSTAL_CORE("Crystal Core"),
    MINING_POOL("Mining Pool"),
    SKILL_SHOT("Skill Shot"),
    CHAOS("Chaos Mode"),
    BOSS_CHALLENGE("Boss Challenge");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
