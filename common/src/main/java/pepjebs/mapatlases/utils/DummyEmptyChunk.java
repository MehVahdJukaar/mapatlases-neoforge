package pepjebs.mapatlases.utils;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;

//what map updates get instead of generating chunks they walk over
public class DummyEmptyChunk {

    private static volatile LevelChunk cachedChunk;

    public static LevelChunk get(Level level) {
        LevelChunk cached = cachedChunk;
        if (cached != null && cached.getLevel() == level) return cached;

        LevelChunk fresh = new EmptyLevelChunk(level, ChunkPos.ZERO,
                level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST));
        MinecraftServer server = level.getServer();
        if (server != null && server.isRunning()) {
            cachedChunk = fresh;
        }
        return fresh;
    }

    public static void clear() {
        cachedChunk = null;
    }
}
