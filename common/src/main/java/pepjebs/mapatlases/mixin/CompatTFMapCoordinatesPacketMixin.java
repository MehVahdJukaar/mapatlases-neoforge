package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.core.misc.IMapDataPacketExtension;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import twilightforest.item.MagicMapItem;
import twilightforest.item.MazeMapItem;
import twilightforest.item.mapdata.TFMagicMapData;
import twilightforest.item.mapdata.TFMazeMapData;
import twilightforest.network.MagicMapPacket;
import twilightforest.network.MazeMapPacket;

import java.util.List;

@Pseudo
@Mixin(targets = {"twilightforest.network.MagicMapPacket$1"})
class CompatTFMapCoordinatesMagic1PacketMixin {

    @Shadow @Final
    MagicMapPacket val$message;

    @ModifyArg(method = "run",
            index = 0,
            remap = false,
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/item/mapdata/TFMagicMapData;<init>(IIBZZZLnet/minecraft/resources/ResourceKey;)V"))
    private int mapAtlases$setX(int x) {
        return ((IMapDataPacketExtension) (Object) val$message.inner()).moonlight$getMapCenterX();
    }

    @ModifyArg(method = "run",
            index = 1,
            remap = false,
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/item/mapdata/TFMagicMapData;<init>(IIBZZZLnet/minecraft/resources/ResourceKey;)V"))
    private int mapAtlases$setZ(int z) {
        return ((IMapDataPacketExtension) (Object) val$message.inner()).moonlight$getMapCenterZ();
    }
}

@Pseudo
@Mixin(targets = {"twilightforest.network.MazeMapPacket$1"})
class CompatTFMapCoordinatesMaze1PacketMixin {

    @Shadow @Final
    MazeMapPacket val$message;

    @ModifyArg(method = "run",
            index = 0,
            remap = false,
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/item/mapdata/TFMazeMapData;<init>(IIBZZZLnet/minecraft/resources/ResourceKey;)V"))
    private int mapAtlases$setX(int x) {
        return ((IMapDataPacketExtension) (Object) val$message.inner()).moonlight$getMapCenterX();
    }

    @ModifyArg(method = "run",
            index = 1,
            at = @At(value = "INVOKE",
                    target = "Ltwilightforest/item/mapdata/TFMazeMapData;<init>(IIBZZZLnet/minecraft/resources/ResourceKey;)V"))
    private int mapAtlases$setZ(int z) {
        return ((IMapDataPacketExtension) (Object) val$message.inner()).moonlight$getMapCenterZ();
    }
}

@Pseudo
@Mixin(MagicMapPacket.class)
class CompatTFMapCoordinatesMagic2PacketMixin {

    @Inject(method = "<init>",
            remap = false,
            at = @At("TAIL"))
    public void mapAtlases$setExtraData(ClientboundMapItemDataPacket inner, List conqueredStructures, CallbackInfo ci) {
        if ((Object) inner instanceof IMapDataPacketExtension exp) {

            var server = PlatHelper.getCurrentServer();
            // on server side we add extra data like this
            if (server != null && server.overworld() instanceof ServerLevel sl) {
                MapItemSavedData data = TFMagicMapData.getMagicMapData(sl,
                        MagicMapItem.getMapName(inner.mapId().id()));
                //we are assuming here that this packet i FOR the vanilla map. we'll need to add additional constructor logic to se these for another mod map in a mixin... I dont see a way around this, we are missing information
                if (data != null) {
                    exp.moonlight$setMapCenter(data.centerX, data.centerZ);
                    exp.moonlight$setDimension(data.dimension.location());
                }
            }
        }
    }

}

@Pseudo
@Mixin(MazeMapPacket.class)
class CompatTFMapCoordinatesMaze2PacketMixin {

    @Inject(method = "<init>",
            remap = false,
            at = @At("TAIL"))
    public void mapAtlases$setExtraData(ClientboundMapItemDataPacket inner, boolean ore, int yCenter, CallbackInfo ci){
        if ((Object) inner instanceof IMapDataPacketExtension exp) {

            var server = PlatHelper.getCurrentServer();
            // on server side we add extra data like this
            if (server != null && server.overworld() instanceof ServerLevel sl) {
                MapItemSavedData data = TFMazeMapData.getMazeMapData(sl,
                        MazeMapItem.getMapName(inner.mapId().id()));
                //we are assuming here that this packet i FOR the vanilla map. we'll need to add additional constructor logic to se these for another mod map in a mixin... I dont see a way around this, we are missing information
                if (data != null) {
                    exp.moonlight$setMapCenter(data.centerX, data.centerZ);
                    exp.moonlight$setDimension(data.dimension.location());
                }
            }
        }
    }

}