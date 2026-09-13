package community.dye.speedrun.mixin;

import community.dye.speedrun.client.SpeedrunController;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void dyeSpeedrun$applyOfficialSettings(CallbackInfo ci) {
        var plan = SpeedrunController.INSTANCE.launchPlan();
        if (plan == null || !"ready".equals(plan.status())) return;

        CreateWorldScreen self = (CreateWorldScreen) (Object) this;
        WorldCreationUiState state = self.getUiState();
        state.setName("Dye Speedrun - Attempt " + plan.attemptNumber());
        state.setSeed(plan.seed());
        state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
        state.setAllowCommands(false);
        state.setBonusChest(false);
        state.setGenerateStructures(true);
        state.onChanged();
        SpeedrunController.INSTANCE.onOfficialCreateScreenConfigured();
    }
}
