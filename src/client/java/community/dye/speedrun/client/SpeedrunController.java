package community.dye.speedrun.client;

import community.dye.speedrun.api.ApiClient;
import community.dye.speedrun.api.ApiException;
import community.dye.speedrun.api.ApiModels;
import community.dye.speedrun.config.ConfigStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpeedrunController {
    public static final SpeedrunController INSTANCE = new SpeedrunController();

    private final SpeedrunTimer timer = new SpeedrunTimer();
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);

    private volatile ApiModels.Run pendingRun;
    private volatile ApiModels.Run activeRun;
    private volatile ApiModels.Run launchPlan;
    private volatile String status = "Not linked yet.";
    private volatile boolean statusError;
    private volatile long lastPollAt;
    private volatile long lastHeartbeatAt;
    private volatile List<String> lastDyes = List.of();
    private volatile boolean finishing;
    private volatile boolean starting;
    private volatile long startRetryNotBefore;
    private volatile boolean worldSeenForRun;

    private SpeedrunController() {}

    public String status() { return status; }
    public boolean statusError() { return statusError; }
    public ApiModels.Run pendingRun() { return pendingRun; }
    public ApiModels.Run activeRun() { return activeRun; }
    public boolean isLinked() {
        String token = ConfigStore.get().sessionToken;
        return token != null && !token.isBlank();
    }

    public void setStatus(String text, boolean error) {
        this.status = text;
        this.statusError = error;
    }

    public CompletableFuture<Void> claimLink(Minecraft minecraft, String code) {
        if (code == null || code.isBlank()) {
            setStatus("Enter the Discord linking code first.", true);
            return CompletableFuture.completedFuture(null);
        }
        setStatus("Linking Minecraft to Discord...", false);
        String uuid = minecraft.getGameProfile().id().toString();
        String name = minecraft.getGameProfile().name();
        return ApiClient.claimLink(code.trim(), uuid, name)
                .thenAccept(result -> minecraft.execute(() -> {
                    ConfigStore.get().sessionToken = result.token();
                    ConfigStore.get().lastMinecraftUuid = uuid;
                    ConfigStore.get().lastMinecraftName = name;
                    ConfigStore.save();
                    setStatus("Linked successfully. Run /speedrun-start in Discord.", false);
                    refreshPending(minecraft, true);
                }))
                .exceptionally(error -> { minecraft.execute(() -> setStatus(message(error), true)); return null; });
    }

    public void unlink() {
        ConfigStore.get().sessionToken = "";
        ConfigStore.get().lastRunId = "";
        ConfigStore.get().lastWorldName = "";
        ConfigStore.save();
        pendingRun = null;
        activeRun = null;
        launchPlan = null;
        starting = false;
        startRetryNotBefore = 0;
        setStatus("Unlinked from Discord.", false);
    }

    public void refreshPending(Minecraft minecraft, boolean force) {
        if (!isLinked()) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastPollAt < 5000) return;
        if (!requestInFlight.compareAndSet(false, true)) return;
        lastPollAt = now;
        ApiClient.pending().whenComplete((result, error) -> {
            requestInFlight.set(false);
            minecraft.execute(() -> {
                if (error != null) {
                    setStatus("API: " + message(error), true);
                    return;
                }
                pendingRun = result.run();
                if (pendingRun == null) {
                    if (activeRun == null) setStatus("No official run is waiting. Use /speedrun-start in Discord.", false);
                    return;
                }
                if (activeRun == null && ("running".equals(pendingRun.status()) || "paused".equals(pendingRun.status()))) {
                    setStatus("Existing attempt found. Open its official world to resume.", false);
                } else if ("ready".equals(pendingRun.status())) {
                    setStatus("Official attempt #" + pendingRun.attemptNumber() + " is ready.", false);
                }
            });
        });
    }

    public void prepareOfficialWorld(Minecraft minecraft) {
        ApiModels.Run run = pendingRun;
        if (run == null) {
            setStatus("No pending run. Use /speedrun-start in Discord, then Refresh.", true);
            return;
        }
        if (!"ready".equals(run.status())) {
            setStatus("This attempt has already started. Re-open the same official world to resume it.", true);
            return;
        }
        launchPlan = run;
        String worldName = "Dye Speedrun - Attempt " + run.attemptNumber();
        ConfigStore.get().lastRunId = run.runId();
        ConfigStore.get().lastWorldName = worldName;
        ConfigStore.save();
        setStatus("Opening official world creation. Seed, Survival and cheats-off are preconfigured.", false);
        CreateWorldScreen.openFresh(minecraft, () -> minecraft.gui.setScreen(new SpeedrunScreen(null)));
    }

    public ApiModels.Run launchPlan() { return launchPlan; }

    public void onOfficialCreateScreenConfigured() {
        setStatus("Official settings applied. Create the world to begin.", false);
    }

    public void tick(Minecraft minecraft) {
        if (isLinked() && activeRun == null) refreshPending(minecraft, false);
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            if (activeRun != null && worldSeenForRun) {
                // Leaving the world is effectively a pause: no local active time advances while unloaded.
                timer.sample(true);
                worldSeenForRun = false;
            }
            return;
        }

        if (activeRun == null) {
            ApiModels.Run candidate = pendingRun;
            if (candidate != null && ("ready".equals(candidate.status()) || "running".equals(candidate.status()) || "paused".equals(candidate.status()))) {
                if (System.currentTimeMillis() < startRetryNotBefore) return;
                long actualSeed = GameChecks.worldSeed(minecraft);
                if (actualSeed != Long.MIN_VALUE && Objects.equals(Long.toString(actualSeed), candidate.seed())) {
                    beginOrResume(minecraft, candidate);
                }
            }
            return;
        }

        worldSeenForRun = true;
        long actualSeed = GameChecks.worldSeed(minecraft);
        if (actualSeed == Long.MIN_VALUE || !Long.toString(actualSeed).equals(activeRun.seed())) {
            setStatus("Official run stopped locally: world seed mismatch.", true);
            return;
        }

        boolean paused = minecraft.isPaused();
        long activeMs = timer.sample(paused);
        if (starting) return;
        List<String> dyes = DyeDetector.currentInventoryDyes(minecraft);
        boolean dyesChanged = !dyes.equals(lastDyes);
        long now = System.currentTimeMillis();

        if (!finishing && dyes.size() == DyeDetector.ORDER.size()) {
            finishing = true;
            long finalMs = timer.stop(paused);
            sendFinish(minecraft, activeMs == 0 ? finalMs : finalMs, dyes);
            return;
        }

        if (dyesChanged || now - lastHeartbeatAt >= 5000) {
            lastHeartbeatAt = now;
            lastDyes = List.copyOf(dyes);
            sendState(minecraft, activeMs, paused, dyes);
        }
    }

    private void beginOrResume(Minecraft minecraft, ApiModels.Run run) {
        String gameMode = GameChecks.gameMode(minecraft);
        boolean cheats = GameChecks.cheatsEnabled(minecraft);
        boolean paused = minecraft.isPaused();

        activeRun = run;
        launchPlan = null;
        pendingRun = run;
        lastDyes = DyeDetector.currentInventoryDyes(minecraft);
        finishing = false;
        worldSeenForRun = true;
        timer.start(run.activeTimeMs(), paused);

        if ("ready".equals(run.status())) {
            starting = true;
            setStatus("Starting official attempt #" + run.attemptNumber() + "...", false);
            sendStartAttempt(minecraft, run, gameMode, cheats, 0);
        } else {
            starting = false;
            setStatus("Official attempt resumed.", false);
        }
    }

    private void sendStartAttempt(Minecraft minecraft, ApiModels.Run run, String gameMode, boolean cheats, int retry) {
        ApiClient.start(run.runId(), run.seed(), gameMode, cheats)
                .thenAccept(result -> minecraft.execute(() -> {
                    ApiModels.Run current = activeRun;
                    if (current == null || !current.runId().equals(run.runId())) return;
                    starting = false;
                    startRetryNotBefore = 0;
                    activeRun = result.run();
                    pendingRun = result.run();
                    setStatus("Official run started.", false);
                }))
                .exceptionally(error -> {
                    Throwable root = rootCause(error);
                    if (root instanceof ApiException api && api.statusCode() == 429 && retry < 5) {
                        long delaySeconds = Math.min(16, 1L << retry);
                        minecraft.execute(() -> setStatus("Start busy — retrying in " + delaySeconds + "s...", false));
                        CompletableFuture.runAsync(
                                () -> sendStartAttempt(minecraft, run, gameMode, cheats, retry + 1),
                                CompletableFuture.delayedExecutor(delaySeconds, java.util.concurrent.TimeUnit.SECONDS));
                    } else {
                        minecraft.execute(() -> {
                            timer.stop(minecraft.isPaused());
                            activeRun = null;
                            starting = false;
                            startRetryNotBefore = System.currentTimeMillis() + 30_000L;
                            setStatus("Run rejected: " + message(error) + " (auto-retry paused for 30s)", true);
                        });
                    }
                    return null;
                });
    }

    private void sendState(Minecraft minecraft, long activeMs, boolean paused, List<String> dyes) {
        ApiModels.Run run = activeRun;
        if (run == null) return;
        ApiClient.state(run.runId(), run.seed(), GameChecks.gameMode(minecraft), GameChecks.cheatsEnabled(minecraft), activeMs, paused, dyes)
                .thenAccept(result -> minecraft.execute(() -> {
                    ApiModels.Run current = activeRun;
                    if (current != null && current.runId().equals(run.runId()) && !finishing) {
                        activeRun = result.run();
                        pendingRun = result.run();
                    }
                }))
                .exceptionally(error -> {
                    minecraft.execute(() -> setStatus("Run validation: " + message(error), true));
                    return null;
                });
    }

    private void sendFinish(Minecraft minecraft, long activeMs, List<String> dyes) {
        ApiModels.Run run = activeRun;
        if (run == null) return;
        setStatus("16/16! Submitting official time...", false);
        sendFinishAttempt(minecraft, run, activeMs, List.copyOf(dyes), 0);
    }

    private void sendFinishAttempt(Minecraft minecraft, ApiModels.Run run, long activeMs, List<String> dyes, int retry) {
        ApiClient.finish(run.runId(), run.seed(), GameChecks.gameMode(minecraft), GameChecks.cheatsEnabled(minecraft), activeMs, dyes)
                .thenAccept(result -> minecraft.execute(() -> {
                    activeRun = null;
                    pendingRun = null;
                    lastDyes = List.of();
                    finishing = false;
                    setStatus("RUN COMPLETE — " + formatTime(activeMs) + " — submitted and verified.", false);
                }))
                .exceptionally(error -> {
                    Throwable root = rootCause(error);
                    if (root instanceof ApiException api && api.statusCode() == 429 && retry < 5) {
                        long delaySeconds = Math.min(16, 1L << retry);
                        minecraft.execute(() -> setStatus("Finish busy — retrying in " + delaySeconds + "s...", false));
                        CompletableFuture.runAsync(
                                () -> sendFinishAttempt(minecraft, run, activeMs, dyes, retry + 1),
                                CompletableFuture.delayedExecutor(delaySeconds, java.util.concurrent.TimeUnit.SECONDS));
                    } else {
                        // Keep finishing=true after a permanent rejection. This prevents a 16/16
                        // inventory from spamming the finish endpoint every client tick.
                        minecraft.execute(() -> setStatus("Finish rejected: " + message(error), true));
                    }
                    return null;
                });
    }

    public void abortCurrent(Minecraft minecraft) {
        ApiModels.Run run = activeRun != null ? activeRun : pendingRun;
        if (run == null) {
            setStatus("No active attempt to abort.", true);
            return;
        }
        ApiClient.abort(run.runId()).thenRun(() -> minecraft.execute(() -> {
            timer.stop(minecraft.isPaused());
            activeRun = null;
            pendingRun = null;
            launchPlan = null;
            finishing = false;
            starting = false;
            startRetryNotBefore = 0;
            setStatus("Attempt aborted. It still counts toward the attempt limit.", false);
        })).exceptionally(error -> { minecraft.execute(() -> setStatus("Abort failed: " + message(error), true)); return null; });
    }

    public long hudTime(Minecraft minecraft) {
        return timer.value();
    }

    public List<String> hudDyes(Minecraft minecraft) {
        return minecraft.player == null ? List.of() : DyeDetector.currentInventoryDyes(minecraft);
    }

    public static String formatTime(long ms) {
        ms = Math.max(0, ms);
        long min = ms / 60_000;
        long sec = (ms % 60_000) / 1000;
        long milli = ms % 1000;
        return String.format("%02d:%02d.%03d", min, sec, milli);
    }

    private static Throwable rootCause(Throwable error) {
        Throwable t = error;
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    private static String message(Throwable error) {
        Throwable t = rootCause(error);
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }
}
