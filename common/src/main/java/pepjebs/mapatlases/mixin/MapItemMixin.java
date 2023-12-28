package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MapItem.class, priority = 1200)
public class MapItemMixin {

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    public LevelChunk reduceUpdateNonGeneratedChunks(Level instance, int chunkX, int chunkZ, Operation<LevelChunk> original) {
        var c = instance.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (c instanceof LevelChunk lc) {
            return lc;
        }
        //return empty
        return new EmptyLevelChunk(instance, new ChunkPos(chunkX, chunkZ),
                instance.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST));
    }


}
