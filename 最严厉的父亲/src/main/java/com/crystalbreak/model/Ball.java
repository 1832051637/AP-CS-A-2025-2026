package com.crystalbreak.model;

import com.crystalbreak.physics.SpinState;
import com.crystalbreak.physics.Vector2;
import javafx.scene.paint.Color;

/**
 * Dynamic ball entity. Boss and ore balls reuse the same physical body and add
 * mode-specific metadata instead of forking the physics engine.
 */
public class Ball {
    private final int id;
    private int number;
    private BallGroup group;
    private String label;
    private Vector2 position;
    private Vector2 velocity = Vector2.ZERO;
    private double radius;
    private double mass;
    private Color color;
    private boolean pocketed;
    private final SpinState spin = new SpinState();
    private OreType oreType;
    private int hitPoints;
    private double bossHitCooldown;

    public Ball(int id, int number, BallGroup group, String label, Vector2 position, double radius, double mass, Color color) {
        this.id = id;
        this.number = number;
        this.group = group;
        this.label = label;
        this.position = position;
        this.radius = radius;
        this.mass = mass;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public BallGroup getGroup() {
        return group;
    }

    public void setGroup(BallGroup group) {
        this.group = group;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(Vector2 position) {
        this.position = position;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2 velocity) {
        this.velocity = velocity;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isPocketed() {
        return pocketed;
    }

    public void setPocketed(boolean pocketed) {
        this.pocketed = pocketed;
    }

    public SpinState getSpin() {
        return spin;
    }

    public OreType getOreType() {
        return oreType;
    }

    public void setOreType(OreType oreType) {
        this.oreType = oreType;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }

    public double getBossHitCooldown() {
        return bossHitCooldown;
    }

    public void setBossHitCooldown(double bossHitCooldown) {
        this.bossHitCooldown = bossHitCooldown;
    }

    public boolean isMoving() {
        return !pocketed && velocity.length() > 1.0;
    }

    public boolean isCueBall() {
        return group == BallGroup.CUE;
    }

    public boolean isObjectBall() {
        return group != BallGroup.CUE && group != BallGroup.BOSS;
    }
}
