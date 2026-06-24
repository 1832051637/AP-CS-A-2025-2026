package com.crystalbreak.modes;

import com.crystalbreak.model.Ball;
import com.crystalbreak.model.BallGroup;
import com.crystalbreak.model.GameState;
import com.crystalbreak.model.SkillChallenge;
import com.crystalbreak.model.ShotResult;

public class SkillShotModeHandler implements GameModeHandler {
    @Override
    public void onGameStart(GameState state) {
        generateChallenge(state);
    }

    @Override
    public void onTick(GameState state, double deltaSeconds) {
        SkillChallenge challenge = state.getSkillChallenge();
        if (challenge == null) {
            generateChallenge(state);
            return;
        }
        challenge.tick(deltaSeconds);
        if (challenge.getRemainingSeconds() <= 0.0) {
            generateChallenge(state);
            state.setStatusMessage("Challenge refreshed.");
        }
    }

    @Override
    public void onShotEnded(GameState state, ShotResult result) {
        SkillChallenge challenge = state.getSkillChallenge();
        if (challenge == null) {
            return;
        }
        boolean completed = switch (challenge.getType()) {
            case ONE_BANK -> result.getRailContacts() >= 1 && result.pottedAnyObjectBall();
            case TWO_BANK -> result.getRailContacts() >= 2 && result.pottedAnyObjectBall();
            case TWO_BALL_COMBO -> result.getPottedBalls().stream().filter(Ball::isObjectBall).count() >= 2;
            case TARGET_GROUP -> result.getPottedBalls().stream().anyMatch(ball -> ball.getGroup() == challenge.getTargetGroup());
            case TIMED_POT -> challenge.getRemainingSeconds() > 0.0 && result.pottedAnyObjectBall();
        };
        if (completed) {
            int reward = state.isDoubleScore() ? 150 : 75;
            state.getCurrentPlayer().addScore(reward);
            state.getCurrentPlayer().getProgress().addExperience(35);
            challenge.incrementStreak();
            state.setStatusMessage("Skill completed! +" + reward);
            generateChallenge(state);
        }
    }

    @Override
    public String modeStatus(GameState state) {
        SkillChallenge challenge = state.getSkillChallenge();
        return challenge == null ? "" : challenge.description();
    }

    private void generateChallenge(GameState state) {
        SkillChallenge.Type[] types = SkillChallenge.Type.values();
        SkillChallenge.Type type = types[state.getRandom().nextInt(types.length)];
        BallGroup target = state.getRandom().nextBoolean() ? BallGroup.SOLID : BallGroup.STRIPE;
        double timeLimit = type == SkillChallenge.Type.TIMED_POT ? 22.0 : 45.0;
        state.setSkillChallenge(new SkillChallenge(type, target, timeLimit));
    }
}
