package community.dye.speedrun.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class DyeSpeedrunClient implements ClientModInitializer {
    public static final String MOD_ID = "dye-speedrun";

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        KeyMapping open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dye-speedrun.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.consumeClick()) {
                client.gui.setScreen(new SpeedrunScreen(client.gui.screen()));
            }
            SpeedrunController.INSTANCE.tick(client);
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "official_hud"), (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            var run = SpeedrunController.INSTANCE.activeRun();
            if (run == null || !community.dye.speedrun.config.ConfigStore.get().showHud) return;

            long ms = SpeedrunController.INSTANCE.hudTime(mc);
            List<String> dyes = SpeedrunController.INSTANCE.hudDyes(mc);
            int x = 8, y = 8, w = 176, h = 62;
            graphics.fill(x - 4, y - 4, x + w, y + h, 0xAA101018);
            graphics.text(mc.font, "DYE SPEEDRUN", x, y, 0xFFFFFFFF, true);
            graphics.text(mc.font, SpeedrunController.formatTime(ms) + (mc.isPaused() ? "  PAUSED" : ""), x, y + 13, mc.isPaused() ? 0xFFFFFF66 : 0xFF66FF88, true);
            graphics.text(mc.font, "Dyes: " + dyes.size() + "/16", x, y + 26, 0xFFFFFFFF, true);
            graphics.text(mc.font, "Attempt: " + run.attemptNumber(), x, y + 39, 0xFFBBBBBB, true);
            graphics.text(mc.font, "OFFICIAL RUN", x + 88, y + 39, 0xFF66CCFF, true);
        });
    }
}
