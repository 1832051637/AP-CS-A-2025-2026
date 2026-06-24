package com.crystalbreak.persistence;

import com.crystalbreak.model.PlayerProgress;

import java.util.HashMap;
import java.util.Map;

public class SaveData {
    private PlayerProgress progress = new PlayerProgress();
    private GameSettings settings = new GameSettings();
    private int highScore;
    private int cueLevel = 1;
    private Map<String, Integer> bestScoresByMode = new HashMap<>();

    public PlayerProgress getProgress() {
        return progress;
    }

    public void setProgress(PlayerProgress progress) {
        this.progress = progress == null ? new PlayerProgress() : progress;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public void setSettings(GameSettings settings) {
        this.settings = settings == null ? new GameSettings() : settings;
    }

    public int getHighScore() {
        return highScore;
    }

    public void setHighScore(int highScore) {
        this.highScore = Math.max(0, highScore);
    }

    public int getCueLevel() {
        return cueLevel;
    }

    public void setCueLevel(int cueLevel) {
        this.cueLevel = Math.max(1, cueLevel);
    }

    public Map<String, Integer> getBestScoresByMode() {
        return bestScoresByMode;
    }

    public void setBestScoresByMode(Map<String, Integer> bestScoresByMode) {
        this.bestScoresByMode = bestScoresByMode == null ? new HashMap<>() : bestScoresByMode;
    }

    public void recordScore(String mode, int score) {
        highScore = Math.max(highScore, score);
        bestScoresByMode.merge(mode, score, Math::max);
    }
}
