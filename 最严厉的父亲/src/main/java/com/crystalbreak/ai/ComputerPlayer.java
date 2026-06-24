package com.crystalbreak.ai;

import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.GameMode;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.Pocket;
import com.crystalbreak.model.Player;
import com.crystalbreak.physics.Vector2;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Rule-aware computer player. It searches direct pocket routes, penalizes
 * blocked lines, estimates collision outcome from a ghost-ball point and adds
 * difficulty-dependent aiming error.
 */
public class ComputerPlayer {
    private final Difficulty difficulty;
    private final Random random = new Random();

    public ComputerPlayer(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public ShotPlan chooseShot(GameState state) {
        Ball cue = state.getCueBall().orElseThrow();
        return legalTargets(state).stream()
                .flatMap(target -> state.getTable().getPockets().stream().map(pocket -> evaluateDirectShot(state, cue, target, pocket)))
                .max(Comparator.comparingDouble(ShotPlan::confidence))
                .orElseGet(() -> fallbackBankShot(state, cue));
    }

    private List<Ball> legalTargets(GameState state) {
        if (state.getMode() == GameMode.MINING_POOL) {
            return state.getObjectBallsRemaining();
        }
        if (state.getMode() == GameMode.BOSS_CHALLENGE && state.getBossBall().isPresent()) {
            return List.of(state.getBossBall().get());
        }
        Player player = state.getCurrentPlayer();
        Optional<BallGroup> group = player.getAssignedGroup();
        if (group.isEmpty()) {
            return state.getObjectBallsRemaining().stream()
                    .filter(ball -> ball.getGroup() == BallGroup.SOLID || ball.getGroup() == BallGroup.STRIPE)
                    .toList();
        }
        boolean cleared = state.getBalls().stream().noneMatch(ball -> ball.getGroup() == group.get() && !ball.isPocketed());
        if (cleared) {
            return state.findBallByNumber(8).filter(ball -> !ball.isPocketed()).stream().toList();
        }
        return state.getObjectBallsRemaining().stream().filter(ball -> ball.getGroup() == group.get()).toList();
    }

    private ShotPlan evaluateDirectShot(GameState state, Ball cue, Ball target, Pocket pocket) {
        Vector2 targetToPocket = pocket.getPosition().subtract(target.getPosition()).normalized();
        Vector2 ghostBall = target.getPosition().subtract(targetToPocket.multiply(cue.getRadius() + target.getRadius()));
        Vector2 cueToGhost = ghostBall.subtract(cue.getPosition());
        Vector2 direction = cueToGhost.normalized();
        double cutAlignment = Math.max(0.0, direction.dot(targetToPocket));
        double cueDistance = Math.max(1.0, cueToGhost.length());
        double pocketDistance = Math.max(1.0, target.getPosition().distance(pocket.getPosition()));
        double blockPenalty = lineBlockPenalty(state, cue, ghostBall, target) + lineBlockPenalty(state, target, pocket.getPosition(), cue);
        double confidence = cutAlignment * 1.25 - (cueDistance + pocketDistance) / 1800.0 - blockPenalty;
        confidence *= difficulty.tacticalDepth();

        double angleError = (random.nextDouble() - 0.5) * difficulty.angularNoise();
        direction = rotate(direction, angleError);
        double power = Math.min(1.0, 0.34 + (cueDistance + pocketDistance) / 1400.0);
        return new ShotPlan(direction, power, 0.0, 0.15, target, pocket, confidence);
    }

    private double lineBlockPenalty(GameState state, Ball startBall, Vector2 end, Ball ignored) {
        Vector2 start = startBall.getPosition();
        double penalty = 0.0;
        for (Ball ball : state.getBalls()) {
            if (ball == startBall || ball == ignored || ball.isPocketed()) {
                continue;
            }
            double distance = distancePointToSegment(ball.getPosition(), start, end);
            if (distance < ball.getRadius() * 2.2) {
                penalty += 0.42;
            }
        }
        return penalty;
    }

    private ShotPlan fallbackBankShot(GameState state, Ball cue) {
        Ball target = state.getObjectBallsRemaining().stream().findFirst().orElse(cue);
        double railX = state.getTable().getX() + state.getTable().getWidth() - 22.0;
        Vector2 mirrorTarget = new Vector2(railX + (railX - target.getPosition().x()), target.getPosition().y());
        Vector2 direction = mirrorTarget.subtract(cue.getPosition()).normalized();
        direction = rotate(direction, (random.nextDouble() - 0.5) * difficulty.angularNoise() * 2.0);
        return new ShotPlan(direction, 0.58, 0.1, 0.0, target, null, 0.15);
    }

    private Vector2 rotate(Vector2 vector, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector2(vector.x() * cos - vector.y() * sin, vector.x() * sin + vector.y() * cos).normalized();
    }

    private double distancePointToSegment(Vector2 point, Vector2 a, Vector2 b) {
        Vector2 ab = b.subtract(a);
        double denominator = ab.lengthSquared();
        if (denominator < 1.0e-9) {
            return point.distance(a);
        }
        double t = Math.max(0.0, Math.min(1.0, point.subtract(a).dot(ab) / denominator));
        return point.distance(a.add(ab.multiply(t)));
    }
}
