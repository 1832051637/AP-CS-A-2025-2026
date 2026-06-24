package com.crystalbreak.ai;

public enum Difficulty {
    EASY(0.58, 0.22),
    NORMAL(0.72, 0.13),
    HARD(0.86, 0.07),
    EXPERT(0.96, 0.025);

    private final double tacticalDepth;
    private final double angularNoise;

    Difficulty(double tacticalDepth, double angularNoise) {
        this.tacticalDepth = tacticalDepth;
        this.angularNoise = angularNoise;
    }

    public double tacticalDepth() {
        return tacticalDepth;
    }

    public double angularNoise() {
        return angularNoise;
    }
}
