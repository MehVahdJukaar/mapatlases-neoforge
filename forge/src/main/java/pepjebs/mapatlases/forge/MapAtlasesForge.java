package pepjebs.mapatlases.forge;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.forge.MapAtlasesClientImpl;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;
import pepjebs.mapatlases.map_collection.forge.IMapCollectionImpl;

@Mod(MapAtlasesMod.MOD_ID)
public class MapAtlasesForge {

    public MapAtlasesForge() {
        MapAtlasesMod.init();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(IMapCollectionImpl::register);

        MinecraftForge.EVENT_BUS.register(this);

        if (PlatHelper.getPhysicalSide().isClient()){
            MapAtlasesClientImpl.init();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDimensionUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel)
            MapAtlasesServerEvents.onDimensionUnload();
    }

    @SubscribeEvent
    public static void mapAtlasesPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side == LogicalSide.CLIENT) {
            MapAtlasesClient.cachePlayerState(event.player);
        } else {
            MapAtlasesServerEvents.onPlayerTick(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MapAtlasesServerEvents.onPlayerJoin(sp);
        }
    }
}
