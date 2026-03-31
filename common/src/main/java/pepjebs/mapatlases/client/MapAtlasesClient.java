package pepjebs.mapatlases.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.MapKey;
import pepjebs.mapatlases.networking.S2CMapPacketWrapper;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.Slice;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapAtlasesClient {

    public static final List<String> DIMENSION_TEXTURE_ORDER = List.of(
            Level.OVERWORLD.location().toString(),
            Level.NETHER.location().toString(),
            Level.END.location().toString()
    );

    public static final KeyMapping OPEN_ATLAS_KEYBIND = new KeyMapping(
            "key.map_atlases.open_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping PLACE_PIN_KEYBIND = new KeyMapping(
            "key.map_atlases.place_pin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping INCREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_in_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_ADD,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping DECREASE_MINIMAP_ZOOM = new KeyMapping(
            "key.map_atlases.zoom_out_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_SUBTRACT,
            "category.map_atlases.minimap"
    );

    public static final KeyMapping INCREASE_SLICE = new KeyMapping(
            "key.map_atlases.increase_slice",
            InputConstants.UNKNOWN.getValue(),
            "category.map_atlases.minimap"
    );

    public static final KeyMapping DECREASE_SLICE = new KeyMapping(
            "key.map_atlases.decrease_slice",
            InputConstants.UNKNOWN.getValue(),
            "category.map_atlases.minimap"
    );

    @Nullable
    private static MapKey currentActiveMapKey;
    private static MapDataHolder currentActiveMap;
    private static ItemStack currentActiveAtlas = ItemStack.EMPTY;
    private static boolean isDrawingAtlas;
    private static float decorationScale = 1.0F;
    private static float decorationTextScale = 1.0F;
    private static float decorationRotation;

    public static void init() {
    }

    public static void cachePlayerState(Player player) {
        if (player != Minecraft.getInstance().player) {
            return;
        }

        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        currentActiveAtlas = atlas;
        currentActiveMap = null;
        currentActiveMapKey = null;
        if (!atlas.isEmpty()) {
            var maps = MapAtlasItem.getMaps(atlas, player.level());
            maps.addNotSynced(player.level());
            Slice slice = MapAtlasItem.getSelectedSlice(atlas, player.level().dimension());
            currentActiveMapKey = MapKey.at(maps.getScale(), player, slice);
            MapDataHolder selected = maps.select(currentActiveMapKey);
            if (selected == null) {
                selected = maps.getClosest(player, slice);
            }
            if (selected != null) {
                currentActiveMapKey = selected.makeKey();
                currentActiveMap = selected;
            }
        }
    }

    public static ItemStack getCurrentActiveAtlas() {
        return currentActiveAtlas;
    }

    public static MapKey getActiveMapKey() {
        return currentActiveMapKey;
    }

    public static MapDataHolder getActiveMap() {
        return currentActiveMap;
    }

    public static void setIsDrawingAtlas(boolean state) {
        isDrawingAtlas = state;
    }

    public static boolean isDrawingAtlas() {
        return isDrawingAtlas;
    }

    public static float getPredicateForAtlas(ItemStack stack, ClientLevel world, LivingEntity entity, int seed) {
        if (world == null && entity != null) {
            world = (ClientLevel) entity.level();
        }
        if (world == null) {
            return 0.0F;
        }
        boolean unlocked = !MapAtlasItem.isLocked(stack);
        ResourceKey<Level> dimension = world.dimension();
        int i = DIMENSION_TEXTURE_ORDER.indexOf(dimension.location().toString());
        if (i == -1) {
            return unlocked ? 0.96F : 1.0F;
        }
        return i / 10.0F + (unlocked ? 0.0F : 0.05F);
    }

    public static void handleMapPacketWrapperPacket(S2CMapPacketWrapper packet) {
    }

    public static void openScreen(@Nullable BlockPos lecternPos, boolean pinOnly) {
    }

    public static void openScreen(ItemStack atlas, @Nullable LecternBlockEntity lectern, boolean pinOnly) {
    }

    public static ContainerLevelAccess getClientAccess() {
        return ContainerLevelAccess.create(Minecraft.getInstance().level, BlockPos.ZERO);
    }

    public static void modifyTextDecorationTransform(PoseStack poseStack, float textWidth, float textScale) {
        float s = textWidth * textScale / 2.0F;
        poseStack.translate(s, -4, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(decorationRotation));
        poseStack.translate(-s * decorationTextScale, 4 * decorationTextScale, 0);
        poseStack.scale(decorationTextScale, decorationTextScale, 1);
    }

    public static void modifyDecorationTransform(PoseStack poseStack) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(decorationRotation));
        poseStack.scale(decorationScale, decorationScale, 1);
    }

    public static void setDecorationsScale(float i) {
        decorationScale = i;
    }

    public static void setDecorationsTextScale(float i) {
        decorationTextScale = i;
    }

    public static void setDecorationRotation(float i) {
        decorationRotation = i;
    }

    public static void debugMapUpdated(String mapId) {
        CACHE.put(mapId, 10);
    }

    public static int debugIsMapUpdated(int light, String stringId) {
        return light;
    }

    public static int uploadFrequency() {
        return 1;
    }

    public static void decreaseHoodZoom() {
    }

    public static void increaseHoodZoom() {
    }

    private static final Cache<String, Integer> CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();
}
