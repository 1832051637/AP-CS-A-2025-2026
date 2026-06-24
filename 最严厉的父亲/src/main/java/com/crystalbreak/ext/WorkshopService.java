package com.crystalbreak.ext;

import java.nio.file.Path;

public interface WorkshopService {
    void publishMap(Path mapFile);

    void installItem(String workshopItemId);
}
