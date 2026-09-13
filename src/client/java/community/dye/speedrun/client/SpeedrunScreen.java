package community.dye.speedrun.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SpeedrunScreen extends Screen {
    private final Screen parent;
    private EditBox linkCode;

    public SpeedrunScreen(Screen parent) {
        super(Component.literal("Dye Community Speedrun"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = Math.max(32, this.height / 2 - 92);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> SpeedrunController.INSTANCE.refreshPending(this.minecraft, true))
                .bounds(cx - 140, top + 34, 280, 20).build());

        linkCode = new EditBox(this.font, cx - 140, top + 75, 175, 20, Component.literal("Discord link code"));
        linkCode.setMaxLength(32);
        addRenderableWidget(linkCode);

        addRenderableWidget(Button.builder(Component.literal("Link Discord"), b -> SpeedrunController.INSTANCE.claimLink(this.minecraft, linkCode.getValue()))
                .bounds(cx + 45, top + 75, 95, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Prepare Official World"), b -> SpeedrunController.INSTANCE.prepareOfficialWorld(this.minecraft))
                .bounds(cx - 140, top + 105, 180, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Abort Attempt"), b -> SpeedrunController.INSTANCE.abortCurrent(this.minecraft))
                .bounds(cx + 50, top + 105, 90, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Unlink"), b -> SpeedrunController.INSTANCE.unlink())
                .bounds(cx - 140, top + 135, 85, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + 55, top + 135, 85, 20).build());

        SpeedrunController.INSTANCE.refreshPending(this.minecraft, true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = Math.max(32, this.height / 2 - 92);
        drawCentered(graphics, "DYE COMMUNITY — OFFICIAL SPEEDRUN", cx, top - 10, 0xFFFFFFFF, true);
        drawCentered(graphics, "Official cloud API configured automatically", cx, top + 10, 0xFF66CCFF, false);
        graphics.text(this.font, "Discord linking code", cx - 140, top + 62, 0xFFB0B0B0, true);

        var run = SpeedrunController.INSTANCE.pendingRun();
        String runLine = run == null ? "Run: none" : "Run: Attempt " + run.attemptNumber() + " / " + run.status() + " — Seed received";
        drawCentered(graphics, runLine, cx, top + 164, 0xFFE0E0E0, true);

        int statusColor = SpeedrunController.INSTANCE.statusError() ? 0xFFFF6666 : 0xFF66FF88;
        drawCentered(graphics, SpeedrunController.INSTANCE.status(), cx, top + 181, statusColor, true);
        drawCentered(graphics, "Press K anytime to open this screen.", cx, top + 198, 0xFFAAAAAA, false);
    }

    private void drawCentered(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color, boolean shadow) {
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, shadow);
    }

    @Override
    public void onClose() {
        if (this.parent != null) this.minecraft.gui.setScreen(this.parent);
        else this.minecraft.gui.setScreen(null);
    }
}
