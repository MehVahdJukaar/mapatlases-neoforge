package pepjebs.mapatlases.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RoundRobinUpdateScheduler extends UpdateScheduler {

    private final Deque<MapDataHolder> ticketQueue = new ArrayDeque<>();

    @Override
    public void performUpdate(ServerPlayer player, List<MapDataHolder> visibleMaps) {
        // Remove tickets for maps no longer visible
        Set<MapId> visibleIds = visibleMaps.stream()
                .map(m -> m.id)
                .collect(Collectors.toSet());
        ticketQueue.removeIf(t -> !visibleIds.contains(t.id));

        // Add new tickets for newly visible maps
        for (MapDataHolder map : visibleMaps) {
            boolean exists = ticketQueue.stream().anyMatch(t -> t.id.equals(map.id));
            if (!exists) {
                ticketQueue.addLast(map);
            }
        }
        super.performUpdate(player, visibleMaps);
    }

    @Nullable
    @Override
    protected MapDataHolder poll() {
        if (ticketQueue.isEmpty()) return null;

        // Pop from front, update, push to back
        MapDataHolder ticket = ticketQueue.pollFirst();

        ticketQueue.addLast(ticket);
        return ticket;
    }
}
