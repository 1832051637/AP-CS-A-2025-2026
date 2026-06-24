package com.crystalbreak.ext;

import com.crystalbreak.model.Table;

import java.util.List;

public interface CustomMapProvider {
    List<String> availableMaps();

    Table loadTable(String mapId);
}
