package pepjebs.mapatlases.lifecycle;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.networking.C2SOpenAtlasPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

public class MapAtlasesClientEvents {

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (event.getKey() == MapAtlasesClient.OPEN_ATLAS_KEYBIND.getKey().getValue() && Minecraft.getInstance().screen == null) {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) return;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
            if (atlas.getItem() instanceof MapAtlasItem) {
                MapAtlasesNetowrking.sendToServer(new C2SOpenAtlasPacket());
                MapAtlasesClient.openScreen(atlas);
            }
        }
    }


}
