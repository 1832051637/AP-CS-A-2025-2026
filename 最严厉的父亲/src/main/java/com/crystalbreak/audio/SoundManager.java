package com.crystalbreak.audio;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

/**
 * Optional audio layer. Missing audio resources are treated as a no-op so the
 * project runs immediately from source while leaving clear extension points.
 */
public class SoundManager {
    private final Map<SoundType, AudioClip> clips = new EnumMap<>(SoundType.class);
    private double masterVolume = 0.75;

    public SoundManager() {
        for (SoundType type : SoundType.values()) {
            URL resource = SoundManager.class.getResource("/com/crystalbreak/audio/" + type.fileName());
            if (resource != null) {
                clips.put(type, new AudioClip(resource.toExternalForm()));
            }
        }
    }

    public void setMasterVolume(double masterVolume) {
        this.masterVolume = Math.max(0.0, Math.min(1.0, masterVolume));
    }

    public double getMasterVolume() {
        return masterVolume;
    }

    public void play(SoundType type) {
        AudioClip clip = clips.get(type);
        if (clip != null) {
            clip.setVolume(masterVolume);
            clip.play();
        }
    }

    public boolean hasClip(SoundType type) {
        return clips.containsKey(type);
    }
}
