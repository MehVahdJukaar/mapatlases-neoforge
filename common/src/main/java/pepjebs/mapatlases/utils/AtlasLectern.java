package pepjebs.mapatlases.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface AtlasLectern {

    boolean mapatlases$hasAtlas();

    boolean mapatlases$setAtlas(Player player, ItemStack atlas);

    ItemStack mapatlases$removeAtlas();
}
