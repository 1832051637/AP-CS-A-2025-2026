package com.crystalbreak.model;

import com.crystalbreak.physics.Vector2;

public class CrystalCore {
    private Vector2 position;
    private double radius = 18.0;
    private boolean active;
    private double refreshTimer;

    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(Vector2 position) {
        this.position = position;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getRefreshTimer() {
        return refreshTimer;
    }

    public void setRefreshTimer(double refreshTimer) {
        this.refreshTimer = refreshTimer;
    }
}
