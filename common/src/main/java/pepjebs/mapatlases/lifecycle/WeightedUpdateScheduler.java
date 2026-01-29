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
    private final List<UpdateTicket> selectionBuffer = new ArrayList<>();

    @Override
    public void performUpdate(ServerPlayer player, List<MapDataHolder> visibleMaps) {
        // 1️⃣ Remove tickets for maps no longer visible
        Set<MapId> visibleIds = visibleMaps.stream()
                .map(m -> m.id)
                .collect(Collectors.toSet());
        tickets.entrySet().removeIf(entry -> !visibleIds.contains(entry.getKey()));

        // 2️⃣ Add new tickets for newly visible maps
        for (MapDataHolder map : visibleMaps) {
            tickets.computeIfAbsent(map.id, id -> new UpdateTicket(map));
        }

        // 3️⃣ Update priority for all tickets
        for (UpdateTicket ticket : tickets.values()) {
            ticket.updatePriority(player.getBlockX(), player.getBlockZ());
            ticket.updateHasBlankPixels();
        }
        super.performUpdate(player, visibleMaps);
    }

    @Nullable
    @Override
    protected MapDataHolder poll() {
        // Sort tickets by priority descending
        if (tickets.isEmpty()) return null;
        selectionBuffer.clear();
        selectionBuffer.addAll(tickets.values());
        selectionBuffer.sort(UpdateTicket.COMPARATOR.reversed());

        UpdateTicket first = selectionBuffer.getFirst();
        first.waitTime = 0; // reset waitTime after update
        return first.holder;
    }

    // --- Inner class for ticket ---
    private static class UpdateTicket {
        private static final Comparator<UpdateTicket> COMPARATOR = Comparator.comparingDouble(UpdateTicket::getPriority);

        private final MapDataHolder holder;
        private int waitTime = 20;
        private double lastDistance = 1_000_000;
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
            double deltaDist = Math.max(0, lastDistance - distSquared);

            // weights can be tuned
            double movingDistanceWeight = 1;
            double staticDistanceWeight = 5_000;
            double waitTimeWeight = 1;

            this.currentPriority = (movingDistanceWeight * deltaDist) +
                    (waitTimeWeight * waitTime * waitTime) +
                    (staticDistanceWeight * Mth.fastInvSqrt(distSquared));

            this.lastDistance = distSquared;
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