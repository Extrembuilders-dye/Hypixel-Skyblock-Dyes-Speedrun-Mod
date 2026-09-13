package community.dye.speedrun.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import community.dye.speedrun.config.ConfigStore;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ApiClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ApiClient() {}

    private static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer("dye-speedrun")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static String base() {
        String url = ConfigStore.get().apiBaseUrl == null ? "" : ConfigStore.get().apiBaseUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private static HttpRequest.Builder request(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base() + path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "DyeSpeedrunMod/" + modVersion());
        String token = ConfigStore.get().sessionToken;
        if (token != null && !token.isBlank()) b.header("Authorization", "Bearer " + token);
        return b;
    }

    private static <T> CompletableFuture<T> parse(HttpRequest req, Class<T> type) {
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() < 200 || res.statusCode() >= 300) {
                        String message = "HTTP " + res.statusCode();
                        try {
                            JsonObject obj = GSON.fromJson(res.body(), JsonObject.class);
                            if (obj != null && obj.has("error")) message = obj.get("error").getAsString();
                        } catch (Exception ignored) {}
                        throw new ApiException(res.statusCode(), message);
                    }
                    return GSON.fromJson(res.body(), type);
                });
    }

    public static CompletableFuture<ApiModels.InfoResponse> info() {
        return parse(request("/api/v1/info").GET().build(), ApiModels.InfoResponse.class);
    }

    public static CompletableFuture<ApiModels.LinkResponse> claimLink(String code, String uuid, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        body.addProperty("minecraftUuid", uuid);
        body.addProperty("minecraftName", name);
        body.addProperty("minecraftVersion", "26.2");
        body.addProperty("modVersion", modVersion());
        HttpRequest req = request("/api/v1/link/claim")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        return parse(req, ApiModels.LinkResponse.class);
    }

    public static CompletableFuture<ApiModels.MeResponse> me() {
        return parse(request("/api/v1/me").GET().build(), ApiModels.MeResponse.class);
    }

    public static CompletableFuture<ApiModels.PendingResponse> pending() {
        return parse(request("/api/v1/runs/pending").GET().build(), ApiModels.PendingResponse.class);
    }

    public static CompletableFuture<ApiModels.RunResponse> start(String runId, String seed, String gameMode, boolean cheatsEnabled) {
        JsonObject body = common(seed, gameMode, cheatsEnabled);
        body.addProperty("minecraftVersion", "26.2");
        body.addProperty("modVersion", modVersion());
        return postRun(runId, "start", body);
    }

    public static CompletableFuture<ApiModels.RunResponse> state(String runId, String seed, String gameMode, boolean cheatsEnabled,
                                                                  long activeTimeMs, boolean paused, List<String> dyes) {
        JsonObject body = common(seed, gameMode, cheatsEnabled);
        body.addProperty("activeTimeMs", activeTimeMs);
        body.addProperty("paused", paused);
        body.add("dyes", GSON.toJsonTree(dyes));
        return postRun(runId, "state", body);
    }

    public static CompletableFuture<ApiModels.RunResponse> finish(String runId, String seed, String gameMode, boolean cheatsEnabled,
                                                                   long activeTimeMs, List<String> dyes) {
        JsonObject body = common(seed, gameMode, cheatsEnabled);
        body.addProperty("activeTimeMs", activeTimeMs);
        body.addProperty("paused", false);
        body.add("dyes", GSON.toJsonTree(dyes));
        return postRun(runId, "finish", body);
    }

    public static CompletableFuture<Void> abort(String runId) {
        HttpRequest req = request("/api/v1/runs/" + runId + "/abort")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
            if (res.statusCode() < 200 || res.statusCode() >= 300) throw new ApiException(res.statusCode(), res.body());
            return null;
        });
    }

    private static JsonObject common(String seed, String gameMode, boolean cheatsEnabled) {
        JsonObject body = new JsonObject();
        body.addProperty("seed", seed);
        body.addProperty("gameMode", gameMode);
        body.addProperty("cheatsEnabled", cheatsEnabled);
        return body;
    }

    private static CompletableFuture<ApiModels.RunResponse> postRun(String runId, String action, JsonObject body) {
        HttpRequest req = request("/api/v1/runs/" + runId + "/" + action)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        return parse(req, ApiModels.RunResponse.class);
    }
}
