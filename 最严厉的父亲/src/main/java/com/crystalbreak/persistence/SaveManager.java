package com.crystalbreak.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveManager {
    private final ObjectMapper mapper;
    private final Path savePath;

    public SaveManager() {
        this(Path.of(System.getProperty("user.home"), ".crystalbreak", "save.json"));
    }

    public SaveManager(Path savePath) {
        this.savePath = savePath;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public SaveData load() {
        if (!Files.exists(savePath)) {
            return new SaveData();
        }
        try {
            return mapper.readValue(savePath.toFile(), SaveData.class);
        } catch (IOException ex) {
            System.err.println("Failed to load save file: " + ex.getMessage());
            return new SaveData();
        }
    }

    public void save(SaveData data) {
        try {
            Files.createDirectories(savePath.getParent());
            mapper.writeValue(savePath.toFile(), data);
        } catch (IOException ex) {
            System.err.println("Failed to save game: " + ex.getMessage());
        }
    }

    public Path getSavePath() {
        return savePath;
    }
}
