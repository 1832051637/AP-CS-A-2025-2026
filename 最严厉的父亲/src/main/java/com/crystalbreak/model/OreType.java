package com.crystalbreak.model;

import javafx.scene.paint.Color;

public enum OreType {
    COAL("Coal", 10, Color.web("#3b3f46")),
    IRON("Iron", 25, Color.web("#9b7f68")),
    GOLD("Gold", 50, Color.web("#f6c453")),
    DIAMOND("Diamond", 100, Color.web("#8ce6ff")),
    EMERALD("Emerald", 150, Color.web("#2bd17e"));

    private final String displayName;
    private final int coinValue;
    private final Color color;

    OreType(String displayName, int coinValue, Color color) {
        this.displayName = displayName;
        this.coinValue = coinValue;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int coinValue() {
        return coinValue;
    }

    public Color color() {
        return color;
    }
}
