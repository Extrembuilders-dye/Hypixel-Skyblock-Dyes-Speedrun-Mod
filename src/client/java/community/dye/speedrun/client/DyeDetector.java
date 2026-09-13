package community.dye.speedrun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class DyeDetector {
    public static final List<String> ORDER = List.of(
            "white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"
    );

    private DyeDetector() {}

    public static List<String> currentInventoryDyes(Minecraft minecraft) {
        if (minecraft.player == null) return List.of();
        boolean[] found = new boolean[ORDER.size()];
        var inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null || !"minecraft".equals(id.getNamespace())) continue;
            String path = id.getPath();
            if (!path.endsWith("_dye")) continue;
            String key = path.substring(0, path.length() - 4);
            int index = ORDER.indexOf(key);
            if (index >= 0) found[index] = true;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < ORDER.size(); i++) if (found[i]) result.add(ORDER.get(i));
        return result;
    }
}
