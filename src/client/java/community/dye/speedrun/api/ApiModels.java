package community.dye.speedrun.api;

import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public record InfoResponse(boolean ok, String minecraftVersion, String modVersion, int maxAttempts, List<String> dyes) {}
    public record LinkResponse(boolean ok, String token, String minecraftUuid, String error) {}
    public record PendingResponse(boolean ok, Run run, String error) {}
    public record RunResponse(boolean ok, Run run, Long bestTimeMs, String error) {}
    public record MeResponse(boolean ok, String discordId, String minecraftUuid, String minecraftName, long bestTimeMs, int attemptsUsed, String error) {}

    public record Run(
            String runId,
            int attemptNumber,
            String seed,
            String status,
            long createdAt,
            long startedAt,
            long activeTimeMs,
            boolean paused,
            List<String> dyes,
            List<String> requiredDyes,
            String minecraftVersion,
            String modVersion
    ) {}
}
