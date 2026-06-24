package com.pixelbasketball;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class Player {
    private static final double WIDTH = 30.0;
    private static final double HEIGHT = 58.0;
    private static final double HEAD_SIZE = 18.0;
    private static final double MOVE_SPEED = 245.0;
    private static final double JUMP_SPEED = 610.0;
    private static final double GRAVITY = 1_520.0;
    private static final double COURT_FRICTION = 0.82;
    private static final double HAND_FORWARD_OFFSET = 25.0;
    private static final double HAND_UP_OFFSET = 24.0;

    private final String name;
    private final Color jerseyColor;
    private final Hoop.Side attackingSide;
    private final double startX;

    private double x;
    private double y;
    private double vx;
    private double vy;
    private boolean facingRight;
    private boolean onGround = true;
    private boolean hasBall;

    public Player(String name, Color jerseyColor, Hoop.Side attackingSide, double startX, double floorY) {
        this.name = name;
        this.jerseyColor = jerseyColor;
        this.attackingSide = attackingSide;
        this.startX = startX;
        this.x = startX;
        this.y = floorY - HEIGHT;
        this.facingRight = attackingSide == Hoop.Side.RIGHT;
    }

    public void update(double dt,
                       boolean moveLeft,
                       boolean moveRight,
                       boolean jump,
                       double courtWidth,
                       double floorY) {
        if (moveLeft == moveRight) {
            vx *= COURT_FRICTION;
            if (Math.abs(vx) < 4.0) {
                vx = 0.0;
            }
        } else if (moveLeft) {
            vx = -MOVE_SPEED;
            facingRight = false;
        } else {
            vx = MOVE_SPEED;
            facingRight = true;
        }

        if (jump && onGround) {
            vy = -JUMP_SPEED;
            onGround = false;
        }

        vy += GRAVITY * dt;
        x += vx * dt;
        y += vy * dt;

        if (x < 0.0) {
            x = 0.0;
            vx = 0.0;
        } else if (x + WIDTH > courtWidth) {
            x = courtWidth - WIDTH;
            vx = 0.0;
        }

        double standingY = floorY - HEIGHT;
        if (y >= standingY) {
            y = standingY;
            vy = 0.0;
            onGround = true;
        }
    }

    public void reset(double floorY) {
        x = startX;
        y = floorY - HEIGHT;
        vx = 0.0;
        vy = 0.0;
        onGround = true;
        hasBall = false;
        facingRight = attackingSide == Hoop.Side.RIGHT;
    }

    public Rectangle2D bounds() {
        return new Rectangle2D(x, y, WIDTH, HEIGHT);
    }

    public Point2D center() {
        return new Point2D(x + WIDTH * 0.5, y + HEIGHT * 0.5);
    }

    public Point2D handPosition() {
        double forward = facingRight ? HAND_FORWARD_OFFSET : -HAND_FORWARD_OFFSET;
        return new Point2D(center().getX() + forward, y + HAND_UP_OFFSET);
    }

    public Point2D dribbleFloorPosition() {
        double forward = facingRight ? WIDTH * 0.42 : -WIDTH * 0.42;
        return new Point2D(center().getX() + forward, y + HEIGHT - 7.0);
    }

    public Hoop.Side attackingSide() {
        return attackingSide;
    }

    public String name() {
        return name;
    }

    public boolean hasBall() {
        return hasBall;
    }

    public void setHasBall(boolean hasBall) {
        this.hasBall = hasBall;
    }

    public double velocityX() {
        return vx;
    }

    public double velocityY() {
        return vy;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        gc.setImageSmoothing(false);

        double headX = x + WIDTH * 0.5 - HEAD_SIZE * 0.5;
        double headY = y - 4.0;

        gc.setFill(Color.rgb(245, 182, 124));
        gc.fillRect(snap(headX), snap(headY), HEAD_SIZE, HEAD_SIZE);

        gc.setFill(jerseyColor);
        gc.fillRect(snap(x + 3.0), snap(y + 18.0), WIDTH - 6.0, 25.0);

        gc.setFill(Color.rgb(34, 42, 64));
        gc.fillRect(snap(x + 6.0), snap(y + 43.0), 8.0, 17.0);
        gc.fillRect(snap(x + WIDTH - 14.0), snap(y + 43.0), 8.0, 17.0);

        gc.setFill(Color.rgb(21, 28, 43));
        gc.fillRect(snap(x + 4.0), snap(y + 58.0), 12.0, 5.0);
        gc.fillRect(snap(x + WIDTH - 16.0), snap(y + 58.0), 12.0, 5.0);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Monospaced", 13.0));
        gc.fillText(name, snap(x - 4.0), snap(y - 11.0));
        gc.restore();
    }

    private static double snap(double value) {
        return Math.round(value);
    }
}
