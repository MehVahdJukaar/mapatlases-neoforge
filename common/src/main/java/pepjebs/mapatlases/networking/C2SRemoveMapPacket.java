package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.AtlasLookup;
import pepjebs.mapatlases.utils.MapType;

import java.util.Optional;

public record C2SRemoveMapPacket(MapId mapId, MapType mapType, Optional<BlockPos> lecternPos) implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2SRemoveMapPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("remove_map"),
            C2SRemoveMapPacket::new
    );

    public C2SRemoveMapPacket(RegistryFriendlyByteBuf buf) {
        this(MapId.STREAM_CODEC.decode(buf), MapType.STREAM_CODEC.decode(buf),
                buf.readOptional(BlockPos.STREAM_CODEC));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        MapId.STREAM_CODEC.encode(buf, mapId);
        MapType.STREAM_CODEC.encode(buf, mapType);
        buf.writeOptional(lecternPos, BlockPos.STREAM_CODEC);
    }

    @Override
    public void handle(Context context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;
        if (lecternPos.isPresent() && !player.mayBuild()) return;

        ItemStack atlas = AtlasLookup.getAtlasFromLecternOrPlayer(player, lecternPos);
        if (atlas.isEmpty()) return;
        MapAtlasItem.removeAndDropMap(mapId, mapType, atlas, player);
        AtlasLookup.syncLecternAtlas(player, lecternPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
