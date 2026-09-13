package community.dye.speedrun.client;

import community.dye.speedrun.api.ApiModels;

import java.util.List;

public final class RunSnapshot {
    public final String runId;
    public final int attemptNumber;
    public final String seed;
    public String status;
    public long activeTimeMs;
    public boolean paused;
    public List<String> dyes;

    public RunSnapshot(ApiModels.Run run) {
        this.runId = run.runId();
        this.attemptNumber = run.attemptNumber();
        this.seed = run.seed();
        this.status = run.status();
        this.activeTimeMs = run.activeTimeMs();
        this.paused = run.paused();
        this.dyes = run.dyes() == null ? List.of() : List.copyOf(run.dyes());
    }
}
