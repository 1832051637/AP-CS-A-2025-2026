package com.pixelbasketball;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Ball {
    public static final double RADIUS = 10.0;

    private static final double GRAVITY = 980.0;
    private static final double FLOOR_RESTITUTION = 0.70;
    private static final double WALL_RESTITUTION = 0.68;
    private static final double BACKBOARD_RESTITUTION = 0.74;
    private static final double RIM_RESTITUTION = 0.58;
    private static final double AIR_DRAG = 0.9998;
    private static final double MIN_BOUNCE_SPEED = 36.0;
    private static final double BASE_SHOT_TIME = 0.78;
    private static final double DISTANCE_SHOT_TIME_SCALE = 0.00045;
    private static final double SHOT_SPEED_REDUCTION = 0.40;
    private static final double MIN_SHOT_POWER = 0.76;
    private static final double MAX_SHOT_POWER = 1.24;
    private static final double BASE_AIM_RANDOM_X = 7.0;
    private static final double BASE_AIM_RANDOM_Y = 4.0;
    private static final double POWER_AIM_RANDOM_X = 42.0;
    private static final double POWER_AIM_RANDOM_Y = 17.0;
    private static final double SHOT_TIME_RANDOM = 0.028;
    private static final double PICKUP_COOLDOWN_AFTER_SHOT = 0.34;
    private static final double PICKUP_COOLDOWN_AFTER_STEAL = 0.30;
    private static final double DRIBBLE_CYCLES_PER_SECOND = 3.15;
    private static final double DRIBBLE_HEIGHT = 29.0;

    private double x;
    private double y;
    private double previousX;
    private double previousY;
    private double vx;
    private double vy;
    private double dribbleTime;
    private double pickupCooldown;
    private Player carrier;
    private Player lastShooter;
    private Hoop targetHoop;

    public Ball(double x, double y) {
        reset(x, y);
    }

    public void reset(double x, double y) {
        this.x = x;
        this.y = y;
        this.previousX = x;
        this.previousY = y;
        this.vx = 0.0;
        this.vy = 0.0;
        this.dribbleTime = 0.0;
        this.pickupCooldown = 0.0;
        this.carrier = null;
        this.lastShooter = null;
        this.targetHoop = null;
    }

    public void update(double dt,
                       double courtWidth,
                       double floorY,
                       List<Hoop> hoops) {
        previousX = x;
        previousY = y;

        if (carrier != null) {
            updateCarriedBall(dt);
            previousX = x;
            previousY = y;
            return;
        }

        pickupCooldown = Math.max(0.0, pickupCooldown - dt);
        vy += GRAVITY * dt;
        vx *= AIR_DRAG;
        vy *= AIR_DRAG;

        x += vx * dt;
        y += vy * dt;

        collideWithCourt(courtWidth, floorY);
        for (Hoop hoop : hoops) {
            collideWithBackboard(hoop);
            collideWithRim(hoop);
        }
    }

    public void attachTo(Player player) {
        if (carrier != null) {
            carrier.setHasBall(false);
        }
        carrier = player;
        carrier.setHasBall(true);
        lastShooter = null;
        targetHoop = null;
        pickupCooldown = 0.0;
        vx = 0.0;
        vy = 0.0;
        Point2D hand = player.handPosition();
        x = hand.getX();
        y = hand.getY();
        previousX = x;
        previousY = y;
    }

    public void shootAt(Hoop hoop) {
        shootAt(hoop, 1.0);
    }

    public void shootAt(Hoop hoop, double shotPower) {
        if (carrier == null) {
            return;
        }

        Player shooter = carrier;
        Point2D release = shooter.handPosition();
        Point2D target = hoop.shotTarget();
        double power = clamp(shotPower, MIN_SHOT_POWER, MAX_SHOT_POWER);
        target = applyShotRandomness(target, release, power);
        double dx = target.getX() - release.getX();
        double dy = target.getY() - release.getY();
        double distance = Math.abs(dx);
        double normalFlightTime = BASE_SHOT_TIME + distance * DISTANCE_SHOT_TIME_SCALE;
        double flightTime = normalFlightTime / (1.0 - SHOT_SPEED_REDUCTION) / power;
        flightTime *= ThreadLocalRandom.current().nextDouble(1.0 - SHOT_TIME_RANDOM, 1.0 + SHOT_TIME_RANDOM);
        double launchVx = dx / flightTime;
        double launchVy = (dy - 0.5 * GRAVITY * flightTime * flightTime) / flightTime;

        shooter.setHasBall(false);
        carrier = null;
        lastShooter = shooter;
        targetHoop = hoop;
        pickupCooldown = PICKUP_COOLDOWN_AFTER_SHOT;
        x = release.getX();
        y = release.getY();
        previousX = x;
        previousY = y;
        vx = launchVx;
        vy = launchVy;
    }

    public void knockLooseFrom(Player player, double directionX) {
        if (carrier != player) {
            return;
        }

        Point2D release = player.handPosition();
        double direction = directionX < 0.0 ? -1.0 : 1.0;
        player.setHasBall(false);
        carrier = null;
        lastShooter = null;
        targetHoop = null;
        pickupCooldown = PICKUP_COOLDOWN_AFTER_STEAL;
        x = release.getX();
        y = release.getY();
        previousX = x;
        previousY = y;
        vx = direction * 235.0 + player.velocityX() * 0.25;
        vy = -185.0 + player.velocityY() * 0.15;
    }

    public boolean canBePickedUpBy(Player player) {
        if (carrier != null || pickupCooldown > 0.0) {
            return false;
        }
        double distance = player.center().distance(x, y);
        boolean slowEnough = Math.hypot(vx, vy) < 760.0 || y > player.bounds().getMinY();
        return distance < 48.0 && slowEnough;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double previousX() {
        return previousX;
    }

    public double previousY() {
        return previousY;
    }

    public double velocityY() {
        return vy;
    }

    public Player carrier() {
        return carrier;
    }

    public Player lastShooter() {
        return lastShooter;
    }

    public Hoop targetHoop() {
        return targetHoop;
    }

    public boolean isCarried() {
        return carrier != null;
    }

    public void draw(GraphicsContext gc) {
        gc.save();
        gc.setImageSmoothing(false);
        gc.setFill(Color.rgb(219, 104, 39));
        gc.fillOval(snap(x - RADIUS), snap(y - RADIUS), RADIUS * 2.0, RADIUS * 2.0);
        gc.setStroke(Color.rgb(98, 48, 25));
        gc.setLineWidth(2.0);
        gc.strokeOval(snap(x - RADIUS), snap(y - RADIUS), RADIUS * 2.0, RADIUS * 2.0);
        gc.strokeLine(snap(x - RADIUS + 3.0), snap(y), snap(x + RADIUS - 3.0), snap(y));
        gc.strokeLine(snap(x), snap(y - RADIUS + 3.0), snap(x), snap(y + RADIUS - 3.0));
        gc.restore();
    }

    private void updateCarriedBall(double dt) {
        dribbleTime += dt;
        Point2D hand = carrier.handPosition();
        Point2D lowPoint = carrier.dribbleFloorPosition();
        double bounce = (Math.sin(dribbleTime * Math.PI * 2.0 * DRIBBLE_CYCLES_PER_SECOND) + 1.0) * 0.5;
        double verticalDrop = DRIBBLE_HEIGHT * bounce;

        x = hand.getX() * 0.45 + lowPoint.getX() * 0.55;
        y = hand.getY() + verticalDrop;
        vx = carrier.velocityX();
        vy = carrier.velocityY();
    }

    private Point2D applyShotRandomness(Point2D target, Point2D release, double power) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double distance = Math.abs(target.getX() - release.getX());
        double distanceFactor = 1.0 + clamp((distance - 320.0) / 520.0, 0.0, 0.55);
        double powerError = Math.abs(power - 1.0);
        double xSpread = (BASE_AIM_RANDOM_X + powerError * POWER_AIM_RANDOM_X) * distanceFactor;
        double ySpread = BASE_AIM_RANDOM_Y + powerError * POWER_AIM_RANDOM_Y;
        return target.add(
                random.nextDouble(-xSpread, xSpread),
                random.nextDouble(-ySpread, ySpread)
        );
    }

    private void collideWithCourt(double courtWidth, double floorY) {
        if (x - RADIUS < 0.0) {
            x = RADIUS;
            vx = Math.abs(vx) * WALL_RESTITUTION;
        } else if (x + RADIUS > courtWidth) {
            x = courtWidth - RADIUS;
            vx = -Math.abs(vx) * WALL_RESTITUTION;
        }

        if (y + RADIUS > floorY) {
            y = floorY - RADIUS;
            vy = -Math.abs(vy) * FLOOR_RESTITUTION;
            vx *= 0.92;
            if (Math.abs(vy) < MIN_BOUNCE_SPEED) {
                vy = 0.0;
            }
        }
    }

    private void collideWithBackboard(Hoop hoop) {
        Rectangle2D board = hoop.backboardBounds();
        double nearestX = clamp(x, board.getMinX(), board.getMaxX());
        double nearestY = clamp(y, board.getMinY(), board.getMaxY());
        double dx = x - nearestX;
        double dy = y - nearestY;
        double distanceSquared = dx * dx + dy * dy;

        if (distanceSquared > RADIUS * RADIUS) {
            return;
        }

        Point2D normal;
        double distance = Math.sqrt(distanceSquared);
        if (distance > 0.0001) {
            normal = new Point2D(dx / distance, dy / distance);
        } else {
            double leftPenetration = Math.abs(x - board.getMinX());
            double rightPenetration = Math.abs(board.getMaxX() - x);
            double topPenetration = Math.abs(y - board.getMinY());
            double bottomPenetration = Math.abs(board.getMaxY() - y);
            double smallest = Math.min(Math.min(leftPenetration, rightPenetration), Math.min(topPenetration, bottomPenetration));
            if (smallest == leftPenetration) {
                normal = new Point2D(-1.0, 0.0);
            } else if (smallest == rightPenetration) {
                normal = new Point2D(1.0, 0.0);
            } else if (smallest == topPenetration) {
                normal = new Point2D(0.0, -1.0);
            } else {
                normal = new Point2D(0.0, 1.0);
            }
            distance = 0.0;
        }

        double penetration = RADIUS - distance;
        x += normal.getX() * penetration;
        y += normal.getY() * penetration;
        reflectVelocity(normal, BACKBOARD_RESTITUTION);
    }

    private void collideWithRim(Hoop hoop) {
        for (Hoop.Circle node : hoop.rimCollisionCircles()) {
            double combinedRadius = RADIUS + node.radius();
            double dx = x - node.center().getX();
            double dy = y - node.center().getY();
            double distanceSquared = dx * dx + dy * dy;
            if (distanceSquared > combinedRadius * combinedRadius) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            Point2D normal = distance > 0.0001
                    ? new Point2D(dx / distance, dy / distance)
                    : new Point2D(0.0, -1.0);
            double penetration = combinedRadius - distance;
            x += normal.getX() * penetration;
            y += normal.getY() * penetration;
            reflectVelocity(normal, RIM_RESTITUTION);
        }
    }

    private void reflectVelocity(Point2D normal, double restitution) {
        double dot = vx * normal.getX() + vy * normal.getY();
        if (dot >= 0.0) {
            return;
        }
        vx -= (1.0 + restitution) * dot * normal.getX();
        vy -= (1.0 + restitution) * dot * normal.getY();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double snap(double value) {
        return Math.round(value);
    }
}
