package com.crystalbreak.model;

import java.util.Optional;

public class Player {
    private final String name;
    private final boolean human;
    private int score;
    private int fouls;
    private BallGroup assignedGroup;
    private final PlayerProgress progress;
    private final CueStick cueStick = new CueStick();

    public Player(String name, boolean human, PlayerProgress progress) {
        this.name = name;
        this.human = human;
        this.progress = progress;
    }

    public String getName() {
        return name;
    }

    public boolean isHuman() {
        return human;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        score += Math.max(0, points);
    }

    public int getFouls() {
        return fouls;
    }

    public void addFoul() {
        fouls++;
    }

    public Optional<BallGroup> getAssignedGroup() {
        return Optional.ofNullable(assignedGroup);
    }

    public void setAssignedGroup(BallGroup assignedGroup) {
        this.assignedGroup = assignedGroup;
    }

    public PlayerProgress getProgress() {
        return progress;
    }

    public CueStick getCueStick() {
        return cueStick;
    }
}
