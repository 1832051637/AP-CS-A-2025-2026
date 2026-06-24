package com.crystalbreak.model;

public enum EffectType {
    POWER_BOOST("Power +150%"),
    SLOW_MOTION("Ball Speed -50%"),
    BIG_BALLS("Big Balls"),
    SMALL_BALLS("Small Balls"),
    DOUBLE_SCORE("Double Score"),
    LARGE_POCKETS("Large Pockets"),
    SMALL_POCKETS("Small Pockets"),
    REVERSE_GRAVITY("Reverse Gravity"),
    TABLE_TILT("Table Tilt"),
    SPEED_BOOST("Speed x2"),
    SHRINK_TABLE("Small Table"),
    MOVING_POCKETS("Moving Pockets"),
    BLACK_HOLE("Black Hole"),
    BOSS_SHOCKWAVE("Boss Shockwave");

    private final String label;

    EffectType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
