package com.crystalbreak.model;

import com.crystalbreak.physics.Vector2;

public record ShotParameters(Vector2 direction, double power, double sideSpin, double topSpin) {
    public ShotParameters {
        direction = direction.normalized();
        power = Math.max(0.0, Math.min(1.0, power));
        sideSpin = Math.max(-1.0, Math.min(1.0, sideSpin));
        topSpin = Math.max(-1.0, Math.min(1.0, topSpin));
    }
}
