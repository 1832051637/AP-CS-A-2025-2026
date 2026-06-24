package com.crystalbreak.model;

import com.crystalbreak.physics.Vector2;
import com.crystalbreak.util.GameConstants;

import java.util.List;

public class Table {
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final List<Pocket> pockets;

    public Table(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.pockets = List.of(
                new Pocket(new Vector2(x, y), GameConstants.POCKET_RADIUS),
                new Pocket(new Vector2(x + width / 2.0, y - 4.0), GameConstants.POCKET_RADIUS),
                new Pocket(new Vector2(x + width, y), GameConstants.POCKET_RADIUS),
                new Pocket(new Vector2(x, y + height), GameConstants.POCKET_RADIUS),
                new Pocket(new Vector2(x + width / 2.0, y + height + 4.0), GameConstants.POCKET_RADIUS),
                new Pocket(new Vector2(x + width, y + height), GameConstants.POCKET_RADIUS)
        );
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getMinX(double inset) {
        return x + inset;
    }

    public double getMaxX(double inset) {
        return x + width - inset;
    }

    public double getMinY(double inset) {
        return y + inset;
    }

    public double getMaxY(double inset) {
        return y + height - inset;
    }

    public List<Pocket> getPockets() {
        return pockets;
    }

    public void resetPockets() {
        pockets.forEach(Pocket::resetPosition);
    }

    public boolean contains(Vector2 point) {
        return point.x() >= x && point.x() <= x + width && point.y() >= y && point.y() <= y + height;
    }
}
