package com.crystalbreak.util;

/**
 * Central gameplay tuning. Values are intentionally collected here so new modes
 * can rebalance the game without searching through rendering or controller code.
 */
public final class GameConstants {
    public static final double CANVAS_WIDTH = 1180.0;
    public static final double CANVAS_HEIGHT = 680.0;

    public static final double TABLE_X = 70.0;
    public static final double TABLE_Y = 70.0;
    public static final double TABLE_WIDTH = 920.0;
    public static final double TABLE_HEIGHT = 500.0;
    public static final double RAIL_WIDTH = 34.0;

    public static final double BALL_RADIUS = 11.0;
    public static final double CUE_BALL_RADIUS = 11.0;
    public static final double POCKET_RADIUS = 25.0;

    public static final double MAX_SHOT_SPEED = 980.0;
    public static final double MIN_MOVING_SPEED = 7.0;
    public static final double LINEAR_FRICTION = 115.0;
    public static final double RAIL_RESTITUTION = 0.86;
    public static final double BALL_RESTITUTION = 0.94;
    public static final double SPIN_DECAY = 0.55;
    public static final double SPIN_CURVE_FORCE = 78.0;

    public static final int BALL_COUNT = 16;
    public static final int FPS_SAMPLE_SIZE = 24;

    private GameConstants() {
    }
}
