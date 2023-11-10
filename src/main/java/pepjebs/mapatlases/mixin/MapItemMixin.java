package pepjebs.mapatlases.mixin;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MapLightHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = MapItem.class, priority = 1200)
public class MapItemMixin {
    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/LinkedHashMultiset;create()Lcom/google/common/collect/LinkedHashMultiset;"),
            cancellable = true)
    public void reduceUpdateNonGeneratedChunks(Level pLevel, Entity pViewer, MapItemSavedData pData, CallbackInfo ci,
                                               @Local(ordinal = 9) int worldX, @Local(ordinal = 10) int worldZ) {
        if (worldZ % 16 == 0 && worldX % 16 == 0) {
            if (!pLevel.hasChunkAt(worldX, worldZ)) {
                ci.cancel();
            }
        }
    }

    @WrapOperation(method = "update", at = @At(
            value = "INVOKE",
            ordinal = 3,
            target = "Lnet/minecraft/world/level/block/state/BlockState;getMapColor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/MapColor;"))
    public MapColor addLight(BlockState instance, BlockGetter level, BlockPos pos, Operation<MapColor> operation,
                             @Local Level l,
                             @Local(ordinal = 6) int k1,
                             @Local(ordinal = 7) int l1,
                             @Share("customLightMap") LocalRef<Map<Vector2i, List<Vector2i>>> lightMap) {
        if (lightMap.get() != null) {
            int brightness = l.getBrightness(LightLayer.BLOCK, pos.above());
            int sky = l.getBrightness(LightLayer.SKY, pos.above());
            lightMap.get().computeIfAbsent(new Vector2i(k1, l1), p -> new ArrayList<>())
                    .add(new Vector2i(brightness,sky));
        }

        return operation.call(instance, level, pos);
    }


    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;updateColor(IIB)Z"
    ))
    public boolean updateLightmap(boolean original,
                                     Level level, Entity viewer, MapItemSavedData data,
                                     @Local(ordinal = 6) int x,
                                     @Local(ordinal = 7) int z,
                                     @Share("customLightMap") LocalRef<Map<Vector2i, List<Vector2i>>> lightMap) {

        if(MapAtlasesConfig.lightMap.get()) {
            if (lightMap.get() == null) lightMap.set(new HashMap<>());
            var l = lightMap.get().get(new Vector2i(x, z));
            if (l != null) {
                int blockLight = (int) l.stream().mapToDouble(v->v.x).average().orElse(0);
                int skyLight = (int) l.stream().mapToDouble(v->v.y).average().orElse(0);
                var c = MapLightHandler.getLightData(data);
                c.setLightLevel(x, z, blockLight, skyLight, data);
            }
        }

        return original;
    }


}
