package pepjebs.mapatlases.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pepjebs.mapatlases.networking.C2SRequestMapCenterPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;
import pepjebs.mapatlases.networking.S2CSyncMapCenterPacket;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleMapItemData", at = @At(value = "INVOKE",
    shift = At.Shift.BEFORE,
    target = "Lnet/minecraft/client/multiplayer/ClientLevel;overrideMapData(Ljava/lang/String;Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;)V"),
    locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void keepCenter(ClientboundMapItemDataPacket pPacket, CallbackInfo ci,
                           MapRenderer maprenderer, int i, String s, MapItemSavedData mapitemsaveddata){
        MapAtlasesNetowrking.sendToServer(new C2SRequestMapCenterPacket(s));
    }
}
