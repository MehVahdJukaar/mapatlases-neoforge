package pepjebs.mapatlases.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import pepjebs.mapatlases.MapAtlasesMod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Is this even needed?
public class MapAtlasesAtlasOverviewContainerMenu extends AbstractContainerMenu {

    public final ItemStack atlas;
    public final String centerMapId;
    public final int atlasScale;
    public final Map<Integer, Pair<String,List<Integer>>> idsToCenters = new HashMap<>();

    public MapAtlasesAtlasOverviewContainerMenu(int syncId, Inventory _playerInventory, FriendlyByteBuf buf) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER.get(), syncId);
        atlas = buf.readItem();
        centerMapId = buf.readUtf();
        atlasScale = buf.readInt();
        int numToRead = buf.readInt();
        for (int i = 0; i < numToRead; i++) {
            int id = buf.readInt();
            var dim = buf.readUtf();
            var centers = Arrays.asList(buf.readInt(), buf.readInt());
            idsToCenters.put(id, new Pair<>(dim, centers));
        }
    }

    public MapAtlasesAtlasOverviewContainerMenu(int syncId, Inventory _playerInventory,
                                                Map<Integer, Pair<String,List<Integer>>> idsToCenters1,
                                                ItemStack atlas1,
                                                String centerMapId1,
                                                int atlasScale1) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER.get(), syncId);
        idsToCenters.putAll(idsToCenters1);
        atlas = atlas1;
        centerMapId = centerMapId1;
        atlasScale = atlasScale1;
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }


}
