package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MapItem.class, priority = 1200)
public class MapItemMixin {

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getChunkAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    public LevelChunk reduceUpdateNonGeneratedChunks(Level instance, BlockPos pos, Operation<LevelChunk> original) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        //also checks the range early
        var c = instance.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (c instanceof LevelChunk lc) {
            //original
            return lc;
        }
        //return empty
        return new EmptyLevelChunk(instance, new ChunkPos(chunkX, chunkZ),
                instance.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(Biomes.FOREST));
    }

    // fixes issues with vanilla maps where first strips takes ages to update by incrementing step after map calculation
    @Inject(method = "update", at = @At(value = "NEW", target = "()Lnet/minecraft/core/BlockPos$MutableBlockPos;",
            ordinal = 0), require = 1)
    public void startFromZeroStep(Level level, Entity viewer, MapItemSavedData data, CallbackInfo ci,
                                  @Local MapItemSavedData.HoldingPlayer holdingPlayer, @Share("needsPostIncrement") LocalRef<MapItemSavedData.HoldingPlayer> needsPostInc) {
        holdingPlayer.step--;
        needsPostInc.set(holdingPlayer);
    }

    @Inject(method = "update", at = @At(value = "RETURN"))
    public void doPostIncrement(Level level, Entity viewer, MapItemSavedData data, CallbackInfo ci,
                                @Share("needsPostIncrement") LocalRef<MapItemSavedData.HoldingPlayer> needsPostInc) {
        MapItemSavedData.HoldingPlayer holdingPlayer = needsPostInc.get();
        if (holdingPlayer != null) {
            holdingPlayer.step++;
            needsPostInc.set(null);
        }
    }

}
