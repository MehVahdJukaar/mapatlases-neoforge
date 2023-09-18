package pepjebs.mapatlases.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

import java.util.List;

public class CuriosCompat {

    public static ItemStack getAtlasInCurio(Player player) {
        List<SlotResult> found = CuriosApi.getCuriosHelper().findCurios(player, MapAtlasesMod.MAP_ATLAS.get());
        if (!found.isEmpty()) {
            return found.get(0).stack();
        }
        return ItemStack.EMPTY;
    }
}
