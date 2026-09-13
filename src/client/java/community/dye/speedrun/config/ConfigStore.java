package community.dye.speedrun.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("dye-speedrun.json");
    private static ModConfig config;

    private ConfigStore() {}

    public static synchronized ModConfig get() {
        if (config == null) config = load();
        return config;
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(get()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Dye Speedrun config", e);
        }
    }

    private static ModConfig load() {
        ModConfig parsed;
        if (!Files.exists(PATH)) {
            parsed = new ModConfig();
        } else {
            try {
                parsed = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), ModConfig.class);
                if (parsed == null) parsed = new ModConfig();
            } catch (Exception e) {
                parsed = new ModConfig();
            }
        }

        // Migrate development/local installs to the official cloud API automatically.
        String url = parsed.apiBaseUrl == null ? "" : parsed.apiBaseUrl.trim();
        if (url.isBlank()
                || url.equalsIgnoreCase("http://localhost:3000")
                || url.equalsIgnoreCase("http://127.0.0.1:3000")
                || url.equalsIgnoreCase("https://localhost:3000")) {
            parsed.apiBaseUrl = ModConfig.OFFICIAL_API_URL;
            try {
                Files.createDirectories(PATH.getParent());
                Files.writeString(PATH, GSON.toJson(parsed), StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }
        return parsed;
    }
}
