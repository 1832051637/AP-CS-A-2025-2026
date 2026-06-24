package com.crystalbreak.model;

/**
 * RPG-upgradeable cue stick. Multipliers are deliberately modest to keep classic
 * 8-ball readable even after several upgrades.
 */
public class CueStick {
    private int level = 1;
    private double power = 1.0;
    private double accuracy = 1.0;
    private double spin = 1.0;
    private double stability = 1.0;

    public int getLevel() {
        return level;
    }

    public double getPower() {
        return power;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getSpin() {
        return spin;
    }

    public double getStability() {
        return stability;
    }

    public int upgradeCost() {
        return 120 + level * 80;
    }

    public boolean upgrade(PlayerProgress progress) {
        int cost = upgradeCost();
        if (!progress.spendCoins(cost)) {
            return false;
        }
        applyLevel(level + 1);
        return true;
    }

    public void applyLevel(int level) {
        this.level = Math.max(1, level);
        int upgrades = this.level - 1;
        power = 1.0 + upgrades * 0.08;
        accuracy = 1.0 + upgrades * 0.05;
        spin = 1.0 + upgrades * 0.07;
        stability = 1.0 + upgrades * 0.05;
    }
}
