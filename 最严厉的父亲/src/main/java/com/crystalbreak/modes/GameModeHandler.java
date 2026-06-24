package com.crystalbreak.modes;

import com.crystalbreak.model.GameState;
import com.crystalbreak.model.ShotParameters;
import com.crystalbreak.model.ShotResult;

public interface GameModeHandler {
    default void onGameStart(GameState state) {
    }

    default void onTick(GameState state, double deltaSeconds) {
    }

    default void afterPhysics(GameState state, double deltaSeconds, ShotResult activeShot) {
    }

    default void onShotStarted(GameState state, ShotParameters parameters) {
    }

    default void onShotEnded(GameState state, ShotResult result) {
    }

    default String modeStatus(GameState state) {
        return "";
    }
}
