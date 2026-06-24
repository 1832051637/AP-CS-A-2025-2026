package com.crystalbreak.physics;

/**
 * Simplified spin model. Side spin curves rolling balls and changes rail exit
 * angle; top spin slightly reduces friction while back spin increases it.
 */
public class SpinState {
    private double sideSpin;
    private double topSpin;

    public SpinState() {
        this(0.0, 0.0);
    }

    public SpinState(double sideSpin, double topSpin) {
        this.sideSpin = clamp(sideSpin);
        this.topSpin = clamp(topSpin);
    }

    public double getSideSpin() {
        return sideSpin;
    }

    public void setSideSpin(double sideSpin) {
        this.sideSpin = clamp(sideSpin);
    }

    public double getTopSpin() {
        return topSpin;
    }

    public void setTopSpin(double topSpin) {
        this.topSpin = clamp(topSpin);
    }

    public void decay(double amount) {
        sideSpin = decayValue(sideSpin, amount);
        topSpin = decayValue(topSpin, amount);
    }

    public boolean hasSpin() {
        return Math.abs(sideSpin) > 0.01 || Math.abs(topSpin) > 0.01;
    }

    public SpinState copy() {
        return new SpinState(sideSpin, topSpin);
    }

    private static double decayValue(double value, double amount) {
        if (value > 0) {
            return Math.max(0.0, value - amount);
        }
        if (value < 0) {
            return Math.min(0.0, value + amount);
        }
        return 0.0;
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }
}
