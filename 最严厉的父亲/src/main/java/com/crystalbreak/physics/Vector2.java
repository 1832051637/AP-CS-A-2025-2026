package com.crystalbreak.physics;

import java.util.Objects;

/**
 * Immutable 2D vector used by physics, AI and view calculations.
 */
public record Vector2(double x, double y) {
    public static final Vector2 ZERO = new Vector2(0.0, 0.0);

    public Vector2 add(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }

    public Vector2 subtract(Vector2 other) {
        return new Vector2(x - other.x, y - other.y);
    }

    public Vector2 multiply(double scalar) {
        return new Vector2(x * scalar, y * scalar);
    }

    public Vector2 divide(double scalar) {
        if (Math.abs(scalar) < 1.0e-9) {
            return ZERO;
        }
        return new Vector2(x / scalar, y / scalar);
    }

    public double dot(Vector2 other) {
        return x * other.x + y * other.y;
    }

    public double cross(Vector2 other) {
        return x * other.y - y * other.x;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public double distance(Vector2 other) {
        return subtract(other).length();
    }

    public Vector2 normalized() {
        double length = length();
        if (length < 1.0e-9) {
            return ZERO;
        }
        return divide(length);
    }

    public Vector2 perpendicular() {
        return new Vector2(-y, x);
    }

    public Vector2 limit(double maxLength) {
        double length = length();
        if (length <= maxLength || length < 1.0e-9) {
            return this;
        }
        return normalized().multiply(maxLength);
    }

    public Vector2 withLength(double length) {
        return normalized().multiply(length);
    }

    public boolean isNearlyZero() {
        return lengthSquared() < 1.0e-6;
    }

    public static Vector2 fromAngle(double radians) {
        return new Vector2(Math.cos(radians), Math.sin(radians));
    }

    public static Vector2 lerp(Vector2 a, Vector2 b, double t) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return a.multiply(1.0 - t).add(b.multiply(t));
    }
}
