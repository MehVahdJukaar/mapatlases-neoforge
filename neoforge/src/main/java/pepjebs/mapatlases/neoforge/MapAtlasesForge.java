package pepjebs.mapatlases.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.client.neoforge.MapAtlasesClientImpl;
import pepjebs.mapatlases.lifecycle.MapAtlasesClientEvents;
import pepjebs.mapatlases.lifecycle.MapAtlasesServerEvents;

@Mod(MapAtlasesMod.MOD_ID)
public class MapAtlasesForge {

    public MapAtlasesForge(IEventBus bus) {
        RegHelper.startRegisteringFor(bus);
        MapAtlasesMod.init();

        NeoForge.EVENT_BUS.register(this);

        if (PlatHelper.getPhysicalSide().isClient()) {
            MapAtlasesClientImpl.init(bus);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDimensionUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel)
            MapAtlasesServerEvents.onDimensionUnload();
    }

    @SubscribeEvent
    public void mapAtlasesPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            MapAtlasesClient.cachePlayerState(event.getEntity());
        } else {
            MapAtlasesServerEvents.onPlayerTick(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MapAtlasesServerEvents.onPlayerJoin(sp);
        }
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.Key event) {
        if (event.getAction() == InputConstants.PRESS) {
            MapAtlasesClientEvents.onKeyPressed(event.getKey(), event.getScanCode());
        }
    }
}
