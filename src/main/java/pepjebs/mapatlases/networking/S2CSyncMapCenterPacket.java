package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class S2CSyncMapCenterPacket {


    private final String mapId;
    private final int centerX;
    private final int centerZ;

    public S2CSyncMapCenterPacket(FriendlyByteBuf buf) {
        mapId = buf.readUtf();
        centerX = buf.readVarInt();
        centerZ = buf.readVarInt();
    }

    public S2CSyncMapCenterPacket(String mapId, int x, int z) {
        this.mapId = mapId;
        this.centerX = x;
        this.centerZ = z;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
        buf.writeVarInt(centerX);
        buf.writeVarInt(centerZ);
    }

    public void apply(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            //TODO: only do thison request
            var data = level.getMapData(mapId);
            if (data != null) {
                setCenter(data, centerX, centerZ);
            }
            context.get().setPacketHandled(true);
        });

    }

    public static void setCenter(MapItemSavedData data, int centerX, int centerZ) {
        CENTERX.setAccessible(true);
        CENTERZ.setAccessible(true);
        try {
            CENTERX.set(data, centerX);
            CENTERZ.set(data, centerZ);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Field CENTERX = ObfuscationReflectionHelper.findField(MapItemSavedData.class, "centerX");
    private static final Field CENTERZ = ObfuscationReflectionHelper.findField(MapItemSavedData.class, "centerZ");
}
