package com.uwdie.stafftools.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Path path;

    private StaffToolsConfig config;

    public ConfigManager() {
        this.path = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("stafftools.json");

        this.config = new StaffToolsConfig();
    }

    public void load() {
        try {
            if (!Files.exists(path)) {
                save();
                return;
            }

            String json = Files.readString(path);

            StaffToolsConfig loaded =
                    GSON.fromJson(json, StaffToolsConfig.class);

            if (loaded != null) {
                config = loaded;
            }

        } catch (Exception e) {
            System.err.println(
                    "[StaffTools] Failed to load config."
            );

            e.printStackTrace();

            config = new StaffToolsConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());

            Files.writeString(
                    path,
                    GSON.toJson(config)
            );

        } catch (IOException e) {
            System.err.println(
                    "[StaffTools] Failed to save config."
            );

            e.printStackTrace();
        }
    }

    public StaffToolsConfig getConfig() {
        return config;
    }
}
