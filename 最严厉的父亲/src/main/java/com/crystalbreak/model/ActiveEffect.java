package com.crystalbreak.model;

/**
 * Time-limited modifier created by Crystal Core, Chaos and Boss events.
 */
public class ActiveEffect {
    private final EffectType type;
    private double remainingSeconds;
    private final double intensity;

    public ActiveEffect(EffectType type, double remainingSeconds, double intensity) {
        this.type = type;
        this.remainingSeconds = remainingSeconds;
        this.intensity = intensity;
    }

    public EffectType getType() {
        return type;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public double getIntensity() {
        return intensity;
    }

    public void tick(double deltaSeconds) {
        remainingSeconds = Math.max(0.0, remainingSeconds - deltaSeconds);
    }

    public boolean isExpired() {
        return remainingSeconds <= 0.0;
    }
}
