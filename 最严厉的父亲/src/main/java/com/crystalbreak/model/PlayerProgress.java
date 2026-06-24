package com.crystalbreak.model;

public class PlayerProgress {
    private int level = 1;
    private int experience;
    private int coins;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
    }

    public void addExperience(int amount) {
        experience += Math.max(0, amount);
        while (experience >= experienceForNextLevel()) {
            experience -= experienceForNextLevel();
            level++;
        }
    }

    public void addCoins(int amount) {
        coins += Math.max(0, amount);
    }

    public boolean spendCoins(int amount) {
        if (amount < 0 || coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    public int experienceForNextLevel() {
        return 100 + (level - 1) * 45;
    }
}
