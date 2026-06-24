package com.crystalbreak.model;

public class SkillChallenge {
    public enum Type {
        ONE_BANK("One Bank Shot"),
        TWO_BANK("Two Bank Shot"),
        TWO_BALL_COMBO("Pot Two Balls"),
        TARGET_GROUP("Pot Target Group"),
        TIMED_POT("Timed Pot");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Type type;
    private final BallGroup targetGroup;
    private double remainingSeconds;
    private int streak;

    public SkillChallenge(Type type, BallGroup targetGroup, double remainingSeconds) {
        this.type = type;
        this.targetGroup = targetGroup;
        this.remainingSeconds = remainingSeconds;
    }

    public Type getType() {
        return type;
    }

    public BallGroup getTargetGroup() {
        return targetGroup;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    public void tick(double deltaSeconds) {
        remainingSeconds = Math.max(0.0, remainingSeconds - deltaSeconds);
    }

    public int getStreak() {
        return streak;
    }

    public void incrementStreak() {
        streak++;
    }

    public String description() {
        return switch (type) {
            case ONE_BANK -> "Make one bank shot";
            case TWO_BANK -> "Make two bank shots";
            case TWO_BALL_COMBO -> "Pot two balls in one shot";
            case TARGET_GROUP -> "Pot target group: " + targetGroup;
            case TIMED_POT -> "Timed pot: " + Math.ceil(remainingSeconds) + "s";
        };
    }
}
