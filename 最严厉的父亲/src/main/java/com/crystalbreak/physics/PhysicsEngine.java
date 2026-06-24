package com.crystalbreak.physics;

import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.EffectType;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.Pocket;
import com.crystalbreak.model.ShotResult;
import com.crystalbreak.model.Table;
import com.crystalbreak.util.GameConstants;

import java.util.List;

/**
 * Lightweight deterministic billiards physics. It implements elastic ball
 * collisions, rail rebounds, pocket capture, friction and a deliberately simple
 * spin model suitable for arcade-readable 8-ball.
 */
public class PhysicsEngine {
    private static final double CUE_TOP_SPIN_CONTACT_RATIO = 0.28;
    private static final double CUE_SIDE_SPIN_THROW_RATIO = 0.10;
    private static final double MAX_TOP_SPIN_CONTACT_SPEED = 210.0;
    private static final double MAX_SIDE_SPIN_THROW_SPEED = 82.0;
    private static final double MIN_SPIN_CONTACT_SPEED = 28.0;

    public boolean update(GameState state, double deltaSeconds, ShotResult result) {
        double scaledDelta = deltaSeconds * state.getBallSpeedMultiplier();
        int steps = Math.max(1, Math.min(8, (int) Math.ceil(maxSpeed(state) * scaledDelta / (GameConstants.BALL_RADIUS * 0.8))));
        double step = scaledDelta / steps;
        for (int i = 0; i < steps; i++) {
            integrateBalls(state, step, result);
            resolveBallCollisions(state, result);
        }
        return state.getBalls().stream().anyMatch(ball -> !ball.isPocketed() && ball.getVelocity().length() > GameConstants.MIN_MOVING_SPEED);
    }

    private double maxSpeed(GameState state) {
        return state.getBalls().stream()
                .filter(ball -> !ball.isPocketed())
                .mapToDouble(ball -> ball.getVelocity().length())
                .max()
                .orElse(0.0);
    }

    private void integrateBalls(GameState state, double deltaSeconds, ShotResult result) {
        for (Ball ball : state.getBalls()) {
            if (ball.isPocketed()) {
                continue;
            }
            if (ball.getBossHitCooldown() > 0.0) {
                ball.setBossHitCooldown(Math.max(0.0, ball.getBossHitCooldown() - deltaSeconds));
            }

            Vector2 acceleration = environmentAcceleration(state, ball);
            Vector2 velocity = ball.getVelocity();
            double speed = velocity.length();

            if (speed > 1.0 && ball.getSpin().hasSpin()) {
                Vector2 curve = velocity.normalized().perpendicular()
                        .multiply(ball.getSpin().getSideSpin() * GameConstants.SPIN_CURVE_FORCE * (speed / 280.0));
                acceleration = acceleration.add(curve);
            }

            velocity = velocity.add(acceleration.multiply(deltaSeconds));
            ball.setPosition(ball.getPosition().add(velocity.multiply(deltaSeconds)));
            ball.setVelocity(applyFriction(velocity, ball, deltaSeconds));
            ball.getSpin().decay(GameConstants.SPIN_DECAY * deltaSeconds);

            capturePocketedBalls(state, ball, result);
            if (!ball.isPocketed()) {
                resolveRailCollision(state, ball, result);
            }
        }
    }

    private Vector2 environmentAcceleration(GameState state, Ball ball) {
        Vector2 acceleration = Vector2.ZERO;
        if (state.hasEffect(EffectType.REVERSE_GRAVITY)) {
            acceleration = acceleration.add(new Vector2(0.0, -95.0));
        }
        if (state.hasEffect(EffectType.TABLE_TILT)) {
            acceleration = acceleration.add(new Vector2(70.0, 38.0));
        }
        if (state.hasEffect(EffectType.BLACK_HOLE)) {
            Vector2 center = new Vector2(
                    state.getTable().getX() + state.getTable().getWidth() / 2.0,
                    state.getTable().getY() + state.getTable().getHeight() / 2.0
            );
            Vector2 pull = center.subtract(ball.getPosition());
            double distance = Math.max(80.0, pull.length());
            acceleration = acceleration.add(pull.normalized().multiply(18000.0 / distance));
        }
        if (state.hasEffect(EffectType.BOSS_SHOCKWAVE)) {
            state.getBossBall().ifPresent(boss -> {
                // This lambda intentionally does not mutate acceleration. Shockwave is
                // applied by the Boss mode as an impulse when it is released.
            });
        }
        return acceleration;
    }

    private Vector2 applyFriction(Vector2 velocity, Ball ball, double deltaSeconds) {
        double speed = velocity.length();
        if (speed < 1.0) {
            return Vector2.ZERO;
        }
        double topSpinFactor = 1.0 - ball.getSpin().getTopSpin() * 0.45;
        topSpinFactor = Math.max(0.48, Math.min(1.52, topSpinFactor));
        double newSpeed = Math.max(0.0, speed - GameConstants.LINEAR_FRICTION * topSpinFactor * deltaSeconds);
        if (newSpeed < GameConstants.MIN_MOVING_SPEED * 0.35) {
            return Vector2.ZERO;
        }
        return velocity.withLength(newSpeed);
    }

    private void resolveRailCollision(GameState state, Ball ball, ShotResult result) {
        Table table = state.getTable();
        double inset = state.getTableInset();
        double radius = ball.getRadius() * state.getBallRadiusScale();
        double minX = table.getMinX(inset) + radius;
        double maxX = table.getMaxX(inset) - radius;
        double minY = table.getMinY(inset) + radius;
        double maxY = table.getMaxY(inset) - radius;
        Vector2 position = ball.getPosition();
        Vector2 velocity = ball.getVelocity();
        boolean hit = false;

        if (position.x() < minX) {
            position = new Vector2(minX, position.y());
            velocity = new Vector2(Math.abs(velocity.x()) * GameConstants.RAIL_RESTITUTION, velocity.y() + ball.getSpin().getSideSpin() * 88.0);
            hit = true;
        } else if (position.x() > maxX) {
            position = new Vector2(maxX, position.y());
            velocity = new Vector2(-Math.abs(velocity.x()) * GameConstants.RAIL_RESTITUTION, velocity.y() - ball.getSpin().getSideSpin() * 88.0);
            hit = true;
        }

        if (position.y() < minY) {
            position = new Vector2(position.x(), minY);
            velocity = new Vector2(velocity.x() - ball.getSpin().getSideSpin() * 88.0, Math.abs(velocity.y()) * GameConstants.RAIL_RESTITUTION);
            hit = true;
        } else if (position.y() > maxY) {
            position = new Vector2(position.x(), maxY);
            velocity = new Vector2(velocity.x() + ball.getSpin().getSideSpin() * 88.0, -Math.abs(velocity.y()) * GameConstants.RAIL_RESTITUTION);
            hit = true;
        }

        if (hit) {
            result.incrementRailContacts();
            ball.setPosition(position);
            ball.setVelocity(velocity);
            ball.getSpin().setSideSpin(ball.getSpin().getSideSpin() * 0.72);
        }
    }

    private void capturePocketedBalls(GameState state, Ball ball, ShotResult result) {
        if (ball.getGroup() == BallGroup.BOSS) {
            return;
        }
        double scale = state.getPocketScale();
        for (Pocket pocket : state.getTable().getPockets()) {
            if (ball.getPosition().distance(pocket.getPosition()) <= pocket.getRadius() * scale) {
                ball.setPocketed(true);
                ball.setVelocity(Vector2.ZERO);
                if (ball.isCueBall()) {
                    result.setCueBallPotted(true);
                }
                result.addPottedBall(ball);
                return;
            }
        }
    }

    private void resolveBallCollisions(GameState state, ShotResult result) {
        List<Ball> balls = state.getBalls();
        double radiusScale = state.getBallRadiusScale();
        for (int i = 0; i < balls.size(); i++) {
            Ball a = balls.get(i);
            if (a.isPocketed()) {
                continue;
            }
            for (int j = i + 1; j < balls.size(); j++) {
                Ball b = balls.get(j);
                if (b.isPocketed()) {
                    continue;
                }
                double radiusA = a.getRadius() * radiusScale;
                double radiusB = b.getRadius() * radiusScale;
                Vector2 delta = b.getPosition().subtract(a.getPosition());
                double distance = delta.length();
                double minDistance = radiusA + radiusB;
                if (distance >= minDistance || distance < 1.0e-9) {
                    continue;
                }

                if (a.isCueBall() && b.getGroup() != BallGroup.CUE) {
                    result.setFirstCueContactNumber(b.getNumber());
                } else if (b.isCueBall() && a.getGroup() != BallGroup.CUE) {
                    result.setFirstCueContactNumber(a.getNumber());
                }

                Vector2 normal = distance < 1.0e-9 ? new Vector2(1.0, 0.0) : delta.divide(distance);
                double penetration = minDistance - distance;
                double totalMass = a.getMass() + b.getMass();
                a.setPosition(a.getPosition().subtract(normal.multiply(penetration * (b.getMass() / totalMass))));
                b.setPosition(b.getPosition().add(normal.multiply(penetration * (a.getMass() / totalMass))));

                Vector2 relativeVelocity = b.getVelocity().subtract(a.getVelocity());
                double velocityAlongNormal = relativeVelocity.dot(normal);
                if (velocityAlongNormal > 0) {
                    continue;
                }

                double invMassA = 1.0 / a.getMass();
                double invMassB = 1.0 / b.getMass();
                double impactSpeed = -velocityAlongNormal;
                double impulseMagnitude = (1.0 + GameConstants.BALL_RESTITUTION) * impactSpeed / (invMassA + invMassB);
                Vector2 impulse = normal.multiply(impulseMagnitude);
                a.setVelocity(a.getVelocity().subtract(impulse.multiply(invMassA)));
                b.setVelocity(b.getVelocity().add(impulse.multiply(invMassB)));

                applyCueSpinCollisionEffect(a, b, normal, impactSpeed);
                transferTangentSpin(a, b, normal);
            }
        }
    }

    private void transferTangentSpin(Ball a, Ball b, Vector2 normal) {
        Vector2 tangent = normal.perpendicular();
        double tangentSpeed = b.getVelocity().subtract(a.getVelocity()).dot(tangent);
        double spinImpulse = tangentSpeed * 0.02 + (a.getSpin().getSideSpin() - b.getSpin().getSideSpin()) * 18.0;
        a.setVelocity(a.getVelocity().add(tangent.multiply(spinImpulse / a.getMass())));
        b.setVelocity(b.getVelocity().subtract(tangent.multiply(spinImpulse / b.getMass())));
        double averageSide = (a.getSpin().getSideSpin() + b.getSpin().getSideSpin()) * 0.3;
        a.getSpin().setSideSpin(averageSide);
        b.getSpin().setSideSpin(averageSide);
    }

    private void applyCueSpinCollisionEffect(Ball a, Ball b, Vector2 normal, double impactSpeed) {
        Ball cue;
        Ball object;
        Vector2 cueToObject;
        if (a.isCueBall() && !b.isCueBall()) {
            cue = a;
            object = b;
            cueToObject = normal;
        } else if (b.isCueBall() && !a.isCueBall()) {
            cue = b;
            object = a;
            cueToObject = normal.multiply(-1.0);
        } else {
            return;
        }

        double topSpin = cue.getSpin().getTopSpin();
        double sideSpin = cue.getSpin().getSideSpin();
        if (Math.abs(topSpin) < 0.02 && Math.abs(sideSpin) < 0.02) {
            return;
        }

        double topImpulse = Math.min(MAX_TOP_SPIN_CONTACT_SPEED,
                Math.max(MIN_SPIN_CONTACT_SPEED, impactSpeed * CUE_TOP_SPIN_CONTACT_RATIO));
        double sideThrow = Math.min(MAX_SIDE_SPIN_THROW_SPEED,
                Math.max(MIN_SPIN_CONTACT_SPEED, impactSpeed * CUE_SIDE_SPIN_THROW_RATIO));
        Vector2 tangent = cueToObject.perpendicular();

        if (Math.abs(topSpin) >= 0.02) {
            cue.setVelocity(cue.getVelocity().add(cueToObject.multiply(topSpin * topImpulse)));
            cue.getSpin().setTopSpin(topSpin * 0.48);
        }
        if (Math.abs(sideSpin) >= 0.02) {
            object.setVelocity(object.getVelocity().add(tangent.multiply(sideSpin * sideThrow)));
            cue.setVelocity(cue.getVelocity().subtract(tangent.multiply(sideSpin * sideThrow * 0.38)));
            cue.getSpin().setSideSpin(sideSpin * 0.58);
        }
    }
}
