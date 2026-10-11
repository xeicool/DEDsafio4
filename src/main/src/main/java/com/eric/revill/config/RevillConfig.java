package com.eric.revill.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Configuración del evento Revill (config/revill.json). Sin dependencias de Minecraft. */
public final class RevillConfig {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("revill.json");

    public boolean enabled = true;
    public int durationSeconds = 600;
    public boolean affectsOverworld = true;
    public boolean affectsAllDimensions = false;
    public List<String> affectedDimensions = new ArrayList<>();

    public boolean announcementEnabled = true;
    public int announcementDurationSeconds = 10;
    public boolean announcementFlashEnabled = true;
    public boolean announcementFlashLoop = true;
    public double announcementFlashSpeed = 1.5D;

    public boolean skyEnabled = true;

    public boolean meteoritesEnabled = true;
    public int meteoritesMaxActive = 20;
    public int meteoritesMinHeight = 80;
    public int meteoritesMaxHeight = 180;
    public boolean meteoritesCreateExplosion = true;
    public double meteoritesExplosionPower = 6.0D;
    public boolean meteoritesDamagePlayers = true;
    public boolean meteoritesDestroyBlocks = false;
    public int meteoritesCraterRadius = 12;

    public boolean bunkerRequired = true;

    public static RevillConfig load() {
        RevillConfig config = null;
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, RevillConfig.class);
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (config == null) {
            config = new RevillConfig();
        }
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
