package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Optional;

public class C2S2COpenAtlasScreenPacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, C2S2COpenAtlasScreenPacket> TYPE = Message.makeType(
            MapAtlasesMod.res("open_atlas_screen"),
            C2S2COpenAtlasScreenPacket::new
    );

    @NotNull
    private final Optional<BlockPos> lecternPos;
    private final boolean pinOnly;

    public C2S2COpenAtlasScreenPacket(FriendlyByteBuf buf) {
        lecternPos = buf.readOptional(object -> object.readBlockPos());
        pinOnly = buf.readBoolean();
    }

    public C2S2COpenAtlasScreenPacket() {
        this(Optional.empty(), false);
    }

    public C2S2COpenAtlasScreenPacket(@NotNull Optional<BlockPos> lecternPos, boolean pinOnly) {
        this.lecternPos = lecternPos;
        this.pinOnly = pinOnly;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeOptional(lecternPos, (object, object2) -> object.writeBlockPos(object2));
        buf.writeBoolean(pinOnly);
    }

    @Override
    public void handle(Context context) {
        // we need all this craziness as we need to ensure maps are sent before gui is opened

        if (context.getDirection() == NetworkDir.CLIENT_BOUND) {
            // open screen
            MapAtlasesClient.openScreen(lecternPos, pinOnly);
        } else {
            // sends all atlas and then send this but to client
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ItemStack atlas = ItemStack.EMPTY;
            if (lecternPos.isPresent()) {
                if (player.level().getBlockEntity(lecternPos.get()) instanceof LecternBlockEntity le) {
                    atlas = le.getBook();
                }
            } else {
                atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            }
            if (atlas.getItem() instanceof MapAtlasItem) {
                if (pinOnly) {
                    player.level().playSound(null, player, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 1.7F, 2f);
                }
                if (pinOnly && MapAtlasesMod.MOONLIGHT && MoonlightCompat.maybePlaceMarkerInFront(player, atlas)) {
                    return;
                }

                MapAtlasItem.syncAndOpenGui(player, atlas, lecternPos, pinOnly);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
