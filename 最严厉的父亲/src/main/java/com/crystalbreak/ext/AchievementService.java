package com.crystalbreak.ext;

public interface AchievementService {
    void unlock(String achievementId);

    void reportProgress(String achievementId, double percent);
}
