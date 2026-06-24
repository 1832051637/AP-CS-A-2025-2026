package com.crystalbreak.ai;

import com.crystalbreak.model.Ball;
import com.crystalbreak.model.Pocket;
import com.crystalbreak.physics.Vector2;

public record ShotPlan(Vector2 direction, double power, double sideSpin, double topSpin, Ball targetBall, Pocket targetPocket, double confidence) {
}
