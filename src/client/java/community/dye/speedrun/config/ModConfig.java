package community.dye.speedrun.config;

public final class ModConfig {
    public static final String OFFICIAL_API_URL = "https://dye-speedrun-api.dyecommunity.workers.dev";

    public String apiBaseUrl = OFFICIAL_API_URL;
    public String sessionToken = "";
    public String lastMinecraftUuid = "";
    public String lastMinecraftName = "";
    public String lastRunId = "";
    public String lastWorldName = "";
    public boolean showHud = true;
}
