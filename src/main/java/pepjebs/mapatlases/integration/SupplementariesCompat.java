package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.supplementaries.common.items.SliceMapItem;
import net.mehvahdjukaar.supplementaries.common.misc.AntiqueInkHelper;
import net.mehvahdjukaar.supplementaries.common.misc.map_markers.WeatheredMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.BookmarkButton;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Locale;

public class SupplementariesCompat {

    public static Integer getSlice(MapItemSavedData data) {
        int i = SliceMapItem.getMapHeight(data);
        return i == Integer.MAX_VALUE ? null : i;
    }

    public static ItemStack createSliced(Level level, int destX, int destZ, byte scale, boolean b, boolean b1, Integer slice) {
        return SliceMapItem.createSliced(level, destX, destZ, scale, b, b1, slice);

    }

    public static int getSliceReach() {
        return (int) (SliceMapItem.getRangeMultiplier() * 128);
    }

    public static boolean canPlayerSeeDeathMarker(Player p){
        return false;// TODO  !MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(p).isEmpty();
    }

    public static boolean isAntiqueInk(ItemStack itemstack) {
        return AntiqueInkHelper.hasAntiqueInk(itemstack);
    }

    public static void maybeSetAntique(ItemStack newMap, Level level, ItemStack atlas) {
        if(isAntiqueInk(atlas)){
            WeatheredMap.setAntique(level, newMap, true);
        }
    }
}
