package pepjebs.mapatlases;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class PlatStuff {
    @Contract
    @PlatformImpl
    public static boolean isShear(ItemStack bottomItem) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static boolean isBoss(EntityType<?> type) {
        throw new AssertionError();
    }



    @PlatformImpl
    public static boolean isSimple(NonNullList<Ingredient> ingredients) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static boolean findMatches(List<ItemStack> inputs, NonNullList<Ingredient> ingredients) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static Pair<Boolean, Vec3> fireTeleportEvent(ServerPlayer player, double pX, double pY, double pZ) {
        throw new AssertionError();
    }

    @ClientOnly
    @PlatformImpl
    public static boolean renderForgeMapDecoration(MapDecoration mapdecoration, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                                   MapItemSavedData data, MapDecorationTextureManager decorationTextures, boolean active, int packedLight, int index) {
        throw new AssertionError();
    }
    @ClientOnly
    @PlatformImpl
    public static void drawString(GuiGraphics g, Font font, String text, float x, float y, int i, boolean b) {
        throw new AssertionError();
    }
}
