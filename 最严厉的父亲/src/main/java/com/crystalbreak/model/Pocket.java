package com.crystalbreak.model;

import com.crystalbreak.physics.Vector2;

public class Pocket {
    private final Vector2 basePosition;
    private Vector2 position;
    private final double radius;

    public Pocket(Vector2 position, double radius) {
        this.basePosition = position;
        this.position = position;
        this.radius = radius;
    }

    public Vector2 getBasePosition() {
        return basePosition;
    }

    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(Vector2 position) {
        this.position = position;
    }

    public double getRadius() {
        return radius;
    }

    public void resetPosition() {
        this.position = basePosition;
    }
}
