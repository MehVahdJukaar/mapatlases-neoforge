
package pepjebs.mapatlases.capabilities;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record MapKey(ResourceKey<Level> dimension, int mapX, int mapZ, @Nullable Integer slice) {
}