package com.crystalbreak.audio;

public enum SoundType {
    HIT("hit.wav"),
    COLLISION("collision.wav"),
    POCKET("pocket.wav"),
    VICTORY("victory.wav"),
    BACKGROUND("background.mp3");

    private final String fileName;

    SoundType(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
