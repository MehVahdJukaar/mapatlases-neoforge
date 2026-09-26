package pepjebs.mapatlases.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.List;

public abstract class UpdateScheduler {

     //Accumulator for fractional updates per tick
    protected float accumulator = 0f;
    protected Vec2 lastPlayerPos = null;

    protected float computeUpdateRate(Player player) {
        Vec2 currentPos = new Vec2((float) player.getX(), (float) player.getZ());
        if (lastPlayerPos == null) {
            lastPlayerPos = currentPos;
        }
        double speed = new Vec2(currentPos.x - lastPlayerPos.x, currentPos.y - lastPlayerPos.y).length();
        lastPlayerPos = currentPos;
        //magic numbers yay
        return Mth.clampedMap((float) speed, 0.001f, 1.2f, 0.1f, 2f) * MapAtlasesConfig.mapUpdatePerTick.get();
    }

    public void performUpdate(ServerPlayer player, List<MapDataHolder> visibleMaps) {
        syncTickets(player, visibleMaps);
        accumulator += computeUpdateRate(player);

        while (accumulator >= 1f) {
            MapDataHolder next = poll();
            if (next != null) next.updateMapColorsAndMarkers(player);
            accumulator -= 1f;
        }
    }

    //drop tickets for maps no longer in view and add ones for maps that just entered it
    protected abstract void syncTickets(ServerPlayer player, List<MapDataHolder> visibleMaps);

    @Nullable
    protected abstract MapDataHolder poll();
}
