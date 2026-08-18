package com.agenticbabymode.config;

import com.agenticbabymode.AgenticBabymodeMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads/saves config/agentic-babymode.json. Missing fields fall back to defaults.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig config = ModConfig.defaults();
	private static Path path;

	private ConfigManager() {
	}

	public static void init() {
		path = FabricLoader.getInstance().getConfigDir().resolve(AgenticBabymodeMod.MOD_ID + ".json");
		reload();
	}

	public static synchronized ModConfig reload() {
		ModConfig loaded = null;
		if (path != null && Files.exists(path)) {
			try {
				loaded = GSON.fromJson(Files.readString(path), ModConfig.class);
			} catch (IOException e) {
				AgenticBabymodeMod.LOGGER.error("Failed to read config {}, using defaults", path, e);
			}
		}
		if (loaded == null) {
			loaded = ModConfig.defaults();
		} else {
			loaded.applyDefaults();
		}
		config = loaded;
		save();
		return config;
	}

	public static synchronized void save() {
		if (path == null) {
			return;
		}
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(config));
		} catch (IOException e) {
			AgenticBabymodeMod.LOGGER.error("Failed to write config {}", path, e);
		}
	}

	public static ModConfig get() {
		return config;
	}
}
