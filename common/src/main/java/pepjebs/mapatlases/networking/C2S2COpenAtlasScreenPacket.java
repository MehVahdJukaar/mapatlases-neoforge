package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Optional;

public class C2S2COpenAtlasScreenPacket implements Message {

    @Nullable
    private final BlockPos lecternPos;
    private final boolean pinOnly;

    public C2S2COpenAtlasScreenPacket(FriendlyByteBuf buf) {
        lecternPos = buf.readOptional(FriendlyByteBuf::readBlockPos).orElse(null);
        pinOnly = buf.readBoolean();
    }

    public C2S2COpenAtlasScreenPacket() {
        this(null, false);
    }

    public C2S2COpenAtlasScreenPacket(BlockPos lecternPos, boolean pinOnly) {
        this.lecternPos = lecternPos;
        this.pinOnly = pinOnly;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeOptional(Optional.ofNullable(lecternPos), FriendlyByteBuf::writeBlockPos);
        buf.writeBoolean(pinOnly);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        // we need all this craziness as we need to ensure maps are sent before gui is opened

        if (context.getDirection() == NetworkDir.PLAY_TO_CLIENT) {
            // open screen
            MapAtlasesClient.openScreen(lecternPos, pinOnly);
        } else {
            // sends all atlas and then send this but to client
            if (!(context.getSender() instanceof ServerPlayer player)) return;

            ItemStack atlas = ItemStack.EMPTY;
            if (lecternPos != null) {
                if (player.level.getBlockEntity(lecternPos) instanceof LecternBlockEntity le) {
                    atlas = le.getBook();
                }
            } else {
                atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            }
            if (atlas.getItem() instanceof MapAtlasItem) {
                if(pinOnly && MapAtlasesMod.MOONLIGHT && MoonlightCompat.maybePlacePinInFront(player, atlas)){
                    return;
                }

                MapAtlasItem.syncAndOpenGui(player, atlas, lecternPos, pinOnly);
            }
        }
    }
}
