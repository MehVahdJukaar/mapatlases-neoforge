package pepjebs.mapatlases.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.util.*;
import java.util.stream.Collectors;

//TODO: improve . lower updates when stationary
public class WeightedUpdateScheduler extends UpdateScheduler {

    private final Map<MapId, UpdateTicket> tickets = new HashMap<>();

    @Override
    public void performUpdate(ServerPlayer player, List<MapDataHolder> visibleMaps) {
        //Remove tickets for maps no longer visible
        Set<MapId> visibleIds = visibleMaps.stream()
                .map(m -> m.id)
                .collect(Collectors.toSet());
        tickets.entrySet().removeIf(entry -> !visibleIds.contains(entry.getKey()));

        //Add new tickets for newly visible maps
        for (MapDataHolder map : visibleMaps) {
            tickets.computeIfAbsent(map.id, id -> new UpdateTicket(map));
        }

        //Update priority for all tickets
        for (UpdateTicket ticket : tickets.values()) {
            ticket.updatePriority(player.getBlockX(), player.getBlockZ());
            ticket.updateHasBlankPixels();
        }
        super.performUpdate(player, visibleMaps);
    }

    @Nullable
    @Override
    protected MapDataHolder poll() {
        UpdateTicket best = null;
        for (UpdateTicket ticket : tickets.values()) {
            if (best == null || ticket.getPriority() > best.getPriority()) best = ticket;
        }
        if (best == null) return null;
        best.markUpdated();
        return best.holder;
    }

    private static class UpdateTicket {
        private final MapDataHolder holder;
        private int waitTime = 20;
        private double lastDistance = 1_000_000;
        private double approachSpeed = 0;
        private double closeness = 0;
        private double currentPriority;
        private boolean hasBlankPixels = true;
        private int lastI = 0;
        private final float lowUpdateWeight;

        private UpdateTicket(MapDataHolder data) {
            this.holder = data;
            this.updateHasBlankPixels();
            if (data.type == MapType.VANILLA && data.slice.height().isPresent()) {
                hasBlankPixels = false; //why?
                lowUpdateWeight = 0.1f;
            } else lowUpdateWeight = 0.03f;
        }

        public double getPriority() {
            return hasBlankPixels ? currentPriority : currentPriority * lowUpdateWeight;
        }

        public void updatePriority(int px, int pz) {
            this.waitTime++;
            double distSquared = Mth.lengthSquared(px - holder.data.centerX, pz - holder.data.centerZ);
            this.approachSpeed = Math.max(0, lastDistance - distSquared);
            this.closeness = Mth.fastInvSqrt(distSquared);
            this.lastDistance = distSquared;
            recomputePriority();
        }

        // so a second poll in the same tick picks a different map
        public void markUpdated() {
            this.waitTime = 0;
            recomputePriority();
        }

        private void recomputePriority() {
            double movingDistanceWeight = 1;
            double staticDistanceWeight = 5000;
            double waitTimeWeight = 1;

            this.currentPriority = (movingDistanceWeight * approachSpeed) +
                    (waitTimeWeight * waitTime * waitTime) +
                    (staticDistanceWeight * closeness);
        }

        public void updateHasBlankPixels() {
            if (!hasBlankPixels) return;
            for (; lastI < holder.data.colors.length; lastI++) {
                if (holder.data.colors[lastI] == 0) return;
            }
            hasBlankPixels = false;
        }
    }
}
