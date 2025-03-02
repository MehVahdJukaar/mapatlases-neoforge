package pepjebs.mapatlases.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.integration.XaeroMinimapCompat;

@Mixin(value = ClientboundMapItemDataPacket.class, priority = 1500)
public abstract class ClientBoundMapPacketMixin {

    @Shadow
    @Final
    private MapId mapId;

    @Inject(method = "applyToMap", at = @At("RETURN"))
    public void onClientMapAdded(MapItemSavedData pMapdata, CallbackInfo ci) {
        if (MapAtlasesMod.MOONLIGHT && MapAtlasesClientConfig.convertXaero.get()) XaeroMinimapCompat.loadXaeroWaypoints(
                this.mapId, pMapdata, Minecraft.getInstance().level.registryAccess());
    }
}
