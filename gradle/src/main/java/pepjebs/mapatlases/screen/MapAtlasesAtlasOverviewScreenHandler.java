package pepjebs.mapatlases.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasesAtlasOverviewScreenHandler extends ScreenHandler {

    public ItemStack atlas = ItemStack.EMPTY;
    public String centerMapId = "";
    public int atlasScale = 128;
    public Map<Integer, Pair<String,List<Integer>>> idsToCenters = new HashMap<>();

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, PlayerInventory _playerInventory, PacketByteBuf buf) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        atlas = buf.readItemStack();
        centerMapId = buf.readString();
        atlasScale = buf.readInt();
        int numToRead = buf.readInt();
        for (int i = 0; i < numToRead; i++) {
            int id = buf.readInt();
            var dim = buf.readString();
            var centers = Arrays.asList(buf.readInt(), buf.readInt());
            idsToCenters.put(id, new Pair<>(dim, centers));
        }
    }

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, PlayerInventory _playerInventory,
                                                Map<Integer, Pair<String,List<Integer>>> idsToCenters1,
                                                ItemStack atlas1,
                                                String centerMapId1,
                                                int atlasScale1) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        idsToCenters = idsToCenters1;
        atlas = atlas1;
        centerMapId = centerMapId1;
        atlasScale = atlasScale1;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {return true;}
}
