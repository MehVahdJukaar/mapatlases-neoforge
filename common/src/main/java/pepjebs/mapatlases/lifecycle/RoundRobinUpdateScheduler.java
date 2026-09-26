package pepjebs.mapatlases.lifecycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.AtlasMap;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoundRobinUpdateScheduler extends UpdateScheduler {

    private final Deque<AtlasMap> ticketQueue = new ArrayDeque<>();
    private final Set<MapId> queuedIds = new HashSet<>();

    @Override
    protected void syncTickets(ServerPlayer player, List<AtlasMap> visibleMaps) {
        Set<MapId> visibleIds = new HashSet<>();
        for (AtlasMap map : visibleMaps) visibleIds.add(map.id);
        ticketQueue.removeIf(t -> !visibleIds.contains(t.id));
        queuedIds.retainAll(visibleIds);

        for (AtlasMap map : visibleMaps) {
            if (queuedIds.add(map.id)) ticketQueue.addLast(map);
        }
    }

    @Nullable
    @Override
    protected AtlasMap poll() {
        if (ticketQueue.isEmpty()) return null;

        // Pop from front, update, push to back
        AtlasMap ticket = ticketQueue.pollFirst();

        ticketQueue.addLast(ticket);
        return ticket;
    }
}
