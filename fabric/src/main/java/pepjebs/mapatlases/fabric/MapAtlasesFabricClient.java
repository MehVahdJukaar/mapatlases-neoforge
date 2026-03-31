package pepjebs.mapatlases.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.fabric.MapAtlasesClientImpl;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class MapAtlasesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        clientInit();
    }

    public static void clientInit() {
        registerKeyMappings();

        MapAtlasesClientImpl.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) MapAtlasesClient.cachePlayerState(client.player);
            if (client.level != null) MapAtlasesClientEvents.onClientTick(client, client.level);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MapAtlasesClientEvents.onLoggedOut());
    }

    private static void registerKeyMappings() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options == null) {
            return;
        }

        Set<KeyMapping> merged = new LinkedHashSet<>(Arrays.asList(minecraft.options.keyMappings));
        merged.add(MapAtlasesClient.OPEN_ATLAS_KEYBIND);
        merged.add(MapAtlasesClient.PLACE_PIN_KEYBIND);
        merged.add(MapAtlasesClient.DECREASE_MINIMAP_ZOOM);
        merged.add(MapAtlasesClient.INCREASE_MINIMAP_ZOOM);
        merged.add(MapAtlasesClient.DECREASE_SLICE);
        merged.add(MapAtlasesClient.INCREASE_SLICE);
        setKeyMappings(minecraft, merged.toArray(KeyMapping[]::new));
        KeyMapping.resetMapping();
    }

    private static void setKeyMappings(Minecraft minecraft, KeyMapping[] keyMappings) {
        try {
            Field field = minecraft.options.getClass().getDeclaredField("keyMappings");
            field.setAccessible(true);
            field.set(minecraft.options, keyMappings);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register map atlas keybindings", e);
        }
    }
}
