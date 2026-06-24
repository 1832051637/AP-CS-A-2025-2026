package com.crystalbreak.persistence;

public class GameSettings {
    private double masterVolume = 0.75;
    private boolean showFps = true;
    private boolean aimAssist = true;
    private boolean backgroundMusic = true;

    public double getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(double masterVolume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, masterVolume));
    }

    public boolean isShowFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public boolean isAimAssist() {
        return aimAssist;
    }

    public void setAimAssist(boolean aimAssist) {
        this.aimAssist = aimAssist;
    }

    public boolean isBackgroundMusic() {
        return backgroundMusic;
    }

    public void setBackgroundMusic(boolean backgroundMusic) {
        this.backgroundMusic = backgroundMusic;
    }
}
