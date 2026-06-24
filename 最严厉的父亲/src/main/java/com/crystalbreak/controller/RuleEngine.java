package com.crystalbreak.controller;

import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.GameMode;
import com.crystalbreak.model.GamePhase;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.Player;
import com.crystalbreak.model.ShotResult;
import com.crystalbreak.physics.Vector2;

/**
 * Resolves turn ownership, fouls, scoring and win/loss after balls stop.
 */
public class RuleEngine {
    public void resolveShot(GameState state, ShotResult result) {
        if (state.getPhase() == GamePhase.GAME_OVER) {
            return;
        }
        if (state.getMode() == GameMode.MINING_POOL) {
            resolveMiningShot(state, result);
            return;
        }
        if (state.getMode() == GameMode.BOSS_CHALLENGE) {
            resolveBossShot(state, result);
            return;
        }
        resolveEightBallShot(state, result);
    }

    private void resolveMiningShot(GameState state, ShotResult result) {
        if (result.isCueBallPotted()) {
            foulAndSwitch(state, "Cue ball pocketed.");
            return;
        }
        if (state.getObjectBallsRemaining().isEmpty()) {
            state.getCurrentPlayer().addScore(100);
            state.getCurrentPlayer().getProgress().addExperience(120);
            endGame(state, state.getCurrentPlayer().getName() + " mined the table clean!");
            return;
        }
        if (!result.pottedAnyObjectBall()) {
            state.switchTurn();
            state.setStatusMessage("No ore pocketed. Turn switched.");
        }
    }

    private void resolveBossShot(GameState state, ShotResult result) {
        if (result.isCueBallPotted()) {
            foulAndSwitch(state, "Cue ball pocketed.");
            return;
        }
        if (state.getPhase() != GamePhase.GAME_OVER) {
            state.switchTurn();
            state.setStatusMessage("Boss challenge turn switched.");
        }
    }

    private void resolveEightBallShot(GameState state, ShotResult result) {
        Player shooter = state.getCurrentPlayer();
        boolean foul = result.isCueBallPotted() || !hasLegalFirstHit(state, result);
        if (result.pottedEightBall()) {
            boolean legalEight = !foul && shooter.getAssignedGroup().isPresent() && allGroupCleared(state, shooter.getAssignedGroup().get());
            Player winner = legalEight ? shooter : state.getOtherPlayer();
            winner.addScore(100);
            winner.getProgress().addExperience(120);
            endGame(state, legalEight ? shooter.getName() + " wins by sinking the 8!" : state.getOtherPlayer().getName() + " wins. Illegal 8-ball.");
            return;
        }

        assignOpenTableGroups(state, result);
        int scoringMultiplier = state.isDoubleScore() ? 2 : 1;
        result.getPottedBalls().stream()
                .filter(ball -> ball.getGroup() == BallGroup.SOLID || ball.getGroup() == BallGroup.STRIPE)
                .forEach(ball -> shooter.addScore(10 * scoringMultiplier));

        if (foul) {
            foulAndSwitch(state, "Foul.");
            return;
        }

        boolean keepsTurn = result.getPottedBalls().stream()
                .anyMatch(ball -> shooter.getAssignedGroup().map(group -> ball.getGroup() == group).orElse(ball.getGroup() == BallGroup.SOLID || ball.getGroup() == BallGroup.STRIPE));
        if (!keepsTurn) {
            state.switchTurn();
            state.setStatusMessage("Turn switched.");
        } else {
            state.setStatusMessage(shooter.getName() + " keeps the table.");
        }
    }

    private boolean hasLegalFirstHit(GameState state, ShotResult result) {
        if (result.getFirstCueContactNumber() == null) {
            return false;
        }
        Ball first = state.findBallByNumber(result.getFirstCueContactNumber()).orElse(null);
        if (first == null || first.getGroup() == BallGroup.BOSS) {
            return false;
        }
        Player shooter = state.getCurrentPlayer();
        if (shooter.getAssignedGroup().isEmpty()) {
            return first.getGroup() == BallGroup.SOLID || first.getGroup() == BallGroup.STRIPE;
        }
        BallGroup group = shooter.getAssignedGroup().get();
        if (allGroupCleared(state, group)) {
            return first.getGroup() == BallGroup.EIGHT;
        }
        return first.getGroup() == group;
    }

    private void assignOpenTableGroups(GameState state, ShotResult result) {
        Player shooter = state.getCurrentPlayer();
        if (shooter.getAssignedGroup().isPresent()) {
            return;
        }
        result.getPottedBalls().stream()
                .map(Ball::getGroup)
                .filter(group -> group == BallGroup.SOLID || group == BallGroup.STRIPE)
                .findFirst()
                .ifPresent(group -> {
                    shooter.setAssignedGroup(group);
                    state.getOtherPlayer().setAssignedGroup(group == BallGroup.SOLID ? BallGroup.STRIPE : BallGroup.SOLID);
                    state.setStatusMessage(shooter.getName() + " claims " + formatGroup(group) + ".");
                });
    }

    private boolean allGroupCleared(GameState state, BallGroup group) {
        return state.getBalls().stream()
                .noneMatch(ball -> ball.getGroup() == group && !ball.isPocketed());
    }

    private void foul(GameState state, String message) {
        state.getCurrentPlayer().addFoul();
        state.setBallInHand(true);
        respawnCueBall(state);
        state.setStatusMessage(message);
    }

    private void foulAndSwitch(GameState state, String reason) {
        Player offender = state.getCurrentPlayer();
        foul(state, reason);
        state.switchTurn();
        state.setStatusMessage(offender.getName() + " foul. " + state.getCurrentPlayer().getName() + " gets a free ball.");
    }

    private String formatGroup(BallGroup group) {
        return switch (group) {
            case SOLID -> "Solids (1-7)";
            case STRIPE -> "Stripes (9-15)";
            default -> group.name();
        };
    }

    private void respawnCueBall(GameState state) {
        state.getCueBall().ifPresent(cue -> {
            cue.setPocketed(false);
            cue.setVelocity(Vector2.ZERO);
            cue.setPosition(new Vector2(
                    state.getTable().getX() + state.getTable().getWidth() * 0.25,
                    state.getTable().getY() + state.getTable().getHeight() / 2.0
            ));
        });
    }

    private void endGame(GameState state, String message) {
        state.setPhase(GamePhase.GAME_OVER);
        state.setStatusMessage(message);
    }
}
