package pepjebs.mapatlases.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.AtlasMap;
import pepjebs.mapatlases.utils.MapType;

import java.util.*;

//TODO: improve . lower updates when stationary
public class WeightedUpdateScheduler extends UpdateScheduler {

    private final Map<MapId, UpdateTicket> tickets = new HashMap<>();

    @Override
    protected void syncTickets(ServerPlayer player, List<AtlasMap> visibleMaps) {
        Set<MapId> visibleIds = new HashSet<>();
        for (AtlasMap map : visibleMaps) visibleIds.add(map.id);
        tickets.keySet().retainAll(visibleIds);

        for (AtlasMap map : visibleMaps) {
            tickets.computeIfAbsent(map.id, id -> new UpdateTicket(map));
        }

        //Update priority for all tickets
        for (UpdateTicket ticket : tickets.values()) {
            ticket.updatePriority(player.getBlockX(), player.getBlockZ());
            ticket.updateHasBlankPixels();
        }
    }

    @Nullable
    @Override
    protected AtlasMap poll() {
        UpdateTicket best = null;
        for (UpdateTicket ticket : tickets.values()) {
            if (best == null || ticket.getPriority() > best.getPriority()) best = ticket;
        }
        if (best == null) return null;
        best.markUpdated();
        return best.holder;
    }

    private static class UpdateTicket {
        private final AtlasMap holder;
        private int waitTime = 20;
        private double lastDistance = 1_000_000;
        private double approachSpeed = 0;
        private double closeness = 0;
        private double currentPriority;
        private boolean hasBlankPixels = true;
        private int lastI = 0;
        private final float lowUpdateWeight;

        private UpdateTicket(AtlasMap data) {
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
