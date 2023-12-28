package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MapItem.class)
public class MapItemMixin {

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getChunkAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    public LevelChunk reduceUpdateNonGeneratedChunks(Level instance, BlockPos pPos, Operation<LevelChunk> original) {
        int pChunkX = SectionPos.blockToSectionCoord(pPos.getX());
        int pChunkZ = SectionPos.blockToSectionCoord(pPos.getZ());
        var c = instance.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, false);
        if (c instanceof LevelChunk lc) {
            return lc;
        }
        //return empty
        return new EmptyLevelChunk(instance, new ChunkPos(pChunkX, pChunkZ),
                instance.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(Biomes.FOREST));
    }

}
