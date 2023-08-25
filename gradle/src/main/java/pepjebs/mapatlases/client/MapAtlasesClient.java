package pepjebs.mapatlases.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientLifecycleEvents;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.mapatlases.networking.MapAtlasesInitAtlasS2CPacket;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreen;

public class MapAtlasesClient implements ClientModInitializer {

    private static final ThreadLocal<Float> worldMapZoomLevel = new ThreadLocal<>();
    public static KeyBinding displayMapGUIBinding;
    public static String currentMapStateId = null;
    public static final Identifier LECTERN_OVERWORLD_ID = new Identifier(MapAtlasesMod.MOD_ID, "entity/lectern_atlas");
    public static final Identifier LECTERN_NETHER_ID = new Identifier(MapAtlasesMod.MOD_ID, "entity/lectern_atlas_nether");
    public static final Identifier LECTERN_END_ID = new Identifier(MapAtlasesMod.MOD_ID, "entity/lectern_atlas_end");
    public static final Identifier LECTERN_OTHER_ID = new Identifier(MapAtlasesMod.MOD_ID, "entity/lectern_atlas_unknown");

    @Override
    public void onInitializeClient() {
        // Register client screen
        HandledScreens.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, MapAtlasesAtlasOverviewScreen::new);

        // Register client events
        ClientTickEvents.END_CLIENT_TICK.register(MapAtlasesClientLifecycleEvents::mapAtlasClientTick);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_INIT,
                MapAtlasesClientLifecycleEvents::mapAtlasClientInit);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesInitAtlasS2CPacket.MAP_ATLAS_SYNC,
                MapAtlasesClientLifecycleEvents::mapAtlasClientSync);
        ClientPlayNetworking.registerGlobalReceiver(MapAtlasesServerLifecycleEvents.MAP_ATLAS_ACTIVE_STATE_CHANGE, (
            MinecraftClient _client,
            ClientPlayNetworkHandler _handler,
            PacketByteBuf buf,
            PacketSender _sender) -> {
                String str = buf.readString();
                if (str.compareTo("null") == 0)
                    currentMapStateId = null;
                else
                    currentMapStateId = str;
            });

        // Register Keybind
        displayMapGUIBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.map_atlases.open_minimap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.map_atlases.minimap"
        ));

        // Register ModelPredicate
        ModelPredicateProviderRegistry.register(
                MapAtlasesMod.MAP_ATLAS,
                new Identifier("atlas"),
                this::getPredicateForAtlas);
    }

    public static float getWorldMapZoomLevel() {
        if (worldMapZoomLevel.get() == null) return 1.0f;
        return worldMapZoomLevel.get();
    }

    public static void setWorldMapZoomLevel(float i) {
        worldMapZoomLevel.set(i);
    }

    public float getPredicateForAtlas(
            ItemStack _stack,
            ClientWorld world,
            LivingEntity entity,
            int _seed) {
        // Using ClientWorld will render default Atlas in inventories
        World queryWorld = world;
        if (queryWorld == null && entity != null)
            queryWorld = entity.getWorld();
        if (queryWorld == null)
            return 0.0f;
        if (queryWorld.getRegistryKey() == World.OVERWORLD) return 0.1f;
        if (queryWorld.getRegistryKey() == World.NETHER) return 0.2f;
        if (queryWorld.getRegistryKey() == World.END) return 0.3f;
        return 0.0f;
    }
}
