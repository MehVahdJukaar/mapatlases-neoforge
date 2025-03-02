package pepjebs.mapatlases.integration;

import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecorationType;
import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapMarker;
import net.mehvahdjukaar.moonlight.api.map.decoration.SimpleMapMarker;
import net.mehvahdjukaar.moonlight.api.misc.HolderReference;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class XaeroMinimapCompat {

    private static final Map<String, List<Waypoint>> WAYPOINTS_MAP = new HashMap<>();

    public static void parseXaeroWaypoints(String worldFolderName) {
        WAYPOINTS_MAP.clear();
        //just parses local ones
        Path path = PlatHelper.getGamePath().resolve("XaeroWaypoints/" + worldFolderName + "/");
        //Pattern pattern = Pattern.compile("waypoint:[^#\\n]*");

        try (DirectoryStream<Path> directories = Files.newDirectoryStream(path, Files::isDirectory);) {
            for (Path directory : directories) {
                Path waypointsFile = Paths.get(directory.toString(), "waypoints.txt");

                if (Files.exists(waypointsFile)) {
                    MapAtlasesMod.LOGGER.info("Loaded XaeroMinimap waypoint data for world {}", worldFolderName);
                    List<String> lines = Files.readAllLines(waypointsFile);
                    for (String line : lines) {

                        if (line.startsWith("waypoint:")) {
                            String[] parts = line.split(":");
                            if (parts.length >= 8) {
                                String name = parts[1];
                                int x = Integer.parseInt(parts[3]);
                                int y = Integer.parseInt(parts[4]);
                                int z = Integer.parseInt(parts[5]);
                                int color = Integer.parseInt(parts[6]);

                                Waypoint waypoint = new Waypoint(name, x, y, z, color);
                                String dim = directory.getFileName().toString();
                                WAYPOINTS_MAP.computeIfAbsent(dim, j -> new ArrayList<>())
                                        .add(waypoint);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            MapAtlasesMod.LOGGER.error("Failed to parse Xero waypoints: ", e);
        }
    }

    private static final HolderReference<MLMapDecorationType<?, ?>> DEF_TYPE = HolderReference.of(
            ResourceLocation.fromNamespaceAndPath("moonlight", "generic_structure"),
            MapDataRegistry.REGISTRY_KEY);

    public static void loadXaeroWaypoints(MapId pMapName, MapItemSavedData data, RegistryAccess registries) {
        if (!WAYPOINTS_MAP.isEmpty()) {
            var dim = data.dimension;
            String dimKey = getDimensionDirectoryName(dim);
            var waypoints = WAYPOINTS_MAP.get(dimKey);
            List<Waypoint> toRemove = new ArrayList<>();
            if (waypoints != null) {
                MapDataHolder holder = new MapDataHolder(pMapName, MapType.VANILLA, data);
                for (var w : waypoints) {
                    if (w.y > holder.slice.heightOrTop()) continue;
                    //hack to see if it will be contained
                    MLMapMarker<?> marker = new SimpleMapMarker(DEF_TYPE.getHolder(registries),
                            new BlockPos(w.x, w.y, w.z), 0f, Optional.empty());
                    if (marker.createDecorationFromMarker(data) != null) {
                        ClientMarkers.addMarker(holder, new ColumnPos(w.x, w.z), w.name, w.color);
                        //toRemove.add(w);
                        //MapAtlasesMod.LOGGER.info("Added converted Xaero waypoint {} to pins ", w);
                    }
                }
                waypoints.removeAll(toRemove);
            }
        }
    }

    public record Waypoint(
            String name,
            int x, int y, int z,
            int color) {
    }


    // Xaero logic for dim string
    private static ResourceKey<Level> findDimensionKey(String validatedName) {
        Minecraft minecraft = Minecraft.getInstance();
        Set<ResourceKey<Level>> allDimensions = minecraft.player.connection.levels();

        for (ResourceKey<Level> dimensionKey : allDimensions) {
            String dimensionPath = dimensionKey.location().getPath().replaceAll("\\W+", "");
            if (validatedName.equals(dimensionPath)) {
                return dimensionKey;
            }
        }

        return null;
    }

    private static String getDimensionDirectoryName(ResourceKey<Level> dimKey) {
        if (dimKey == Level.OVERWORLD) {
            return "dim%0";
        } else if (dimKey == Level.NETHER) {
            return "dim%-1";
        } else if (dimKey == Level.END) {
            return "dim%1";
        } else {
            ResourceLocation identifier = dimKey.location();
            String var10000 = identifier.getNamespace();
            return "dim%" + var10000 + "$" + identifier.getPath().replace('/', '%');
        }
    }

}

