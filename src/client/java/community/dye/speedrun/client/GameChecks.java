package community.dye.speedrun.client;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;

public final class GameChecks {
    private GameChecks() {}

    public static long worldSeed(Minecraft minecraft) {
        try {
            Object server = minecraft.getSingleplayerServer();
            if (server == null) return Long.MIN_VALUE;
            Method overworld = server.getClass().getMethod("overworld");
            Object level = overworld.invoke(server);
            Method getSeed = level.getClass().getMethod("getSeed");
            return ((Number) getSeed.invoke(level)).longValue();
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    public static boolean cheatsEnabled(Minecraft minecraft) {
        try {
            Object server = minecraft.getSingleplayerServer();
            if (server == null) return true;
            Object playerList = server.getClass().getMethod("getPlayerList").invoke(server);
            try {
                return (boolean) playerList.getClass().getMethod("isAllowCommandsForAllPlayers").invoke(playerList);
            } catch (NoSuchMethodException ignored) {
                return (boolean) playerList.getClass().getMethod("isAllowCheatsForAllPlayers").invoke(playerList);
            }
        } catch (Exception e) {
            // Fail closed for an official run: if the Mod cannot verify this state, do not claim it is safe.
            return true;
        }
    }

    public static String gameMode(Minecraft minecraft) {
        try {
            if (minecraft.gameMode == null) return "unknown";
            Object mode = minecraft.gameMode.getClass().getMethod("getPlayerMode").invoke(minecraft.gameMode);
            String text = String.valueOf(mode).toLowerCase();
            if (text.contains("survival")) return "survival";
            if (text.contains("creative")) return "creative";
            if (text.contains("spectator")) return "spectator";
            if (text.contains("adventure")) return "adventure";
            return text;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
