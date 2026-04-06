package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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

    public enum OpenSource {
        HAND,
        ACTIVE_ATLAS,
        LECTERN
    }

    private final OpenSource source;
    @Nullable
    private final InteractionHand hand;
    @Nullable
    private final BlockPos lecternPos;
    private final boolean pinOnly;

    public C2S2COpenAtlasScreenPacket(FriendlyByteBuf buf) {
        this.source = buf.readEnum(OpenSource.class);
        this.hand = buf.readOptional(b -> b.readEnum(InteractionHand.class)).orElse(null);
        this.lecternPos = buf.readOptional(b -> b.readBlockPos()).orElse(null);
        this.pinOnly = buf.readBoolean();
    }

    public C2S2COpenAtlasScreenPacket(OpenSource source, @Nullable InteractionHand hand, @Nullable BlockPos lecternPos, boolean pinOnly) {
        this.source = source;
        this.hand = hand;
        this.lecternPos = lecternPos;
        this.pinOnly = pinOnly;
    }

    public static C2S2COpenAtlasScreenPacket forHand(InteractionHand hand) {
        return new C2S2COpenAtlasScreenPacket(OpenSource.HAND, hand, null, false);
    }

    public static C2S2COpenAtlasScreenPacket forActiveAtlas(boolean pinOnly) {
        return new C2S2COpenAtlasScreenPacket(OpenSource.ACTIVE_ATLAS, null, null, pinOnly);
    }

    public static C2S2COpenAtlasScreenPacket forLectern(BlockPos lecternPos, boolean pinOnly) {
        return new C2S2COpenAtlasScreenPacket(OpenSource.LECTERN, null, lecternPos, pinOnly);
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeEnum(source);
        buf.writeOptional(Optional.ofNullable(hand), FriendlyByteBuf::writeEnum);
        buf.writeOptional(Optional.ofNullable(lecternPos), (b, pos) -> b.writeBlockPos(pos));
        buf.writeBoolean(pinOnly);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (context.getDirection() == NetworkDir.PLAY_TO_CLIENT) {
            MapAtlasesMod.LOGGER.info(
                    "[atlas-open] packet client handle source={} hand={} lecternPos={} pinOnly={}",
                    source,
                    hand,
                    lecternPos,
                    pinOnly);
            MapAtlasesClient.openScreen(source, hand, lecternPos, pinOnly);
            return;
        }

        MapAtlasesMod.LOGGER.info(
                "[atlas-open] packet server handle sender={} source={} hand={} lecternPos={} pinOnly={}",
                context.getSender() instanceof ServerPlayer player ? player.getName().getString() : "<no-sender>",
                source,
                hand,
                lecternPos,
                pinOnly);
        if (!(context.getSender() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack atlas = switch (source) {
            case HAND -> hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
            case ACTIVE_ATLAS -> MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            case LECTERN -> {
                if (lecternPos != null && player.level().getBlockEntity(lecternPos) instanceof LecternBlockEntity le) {
                    yield le.getBook();
                }
                yield ItemStack.EMPTY;
            }
        };

        MapAtlasesMod.LOGGER.info(
                "[atlas-open] packet server resolved atlas_empty={} atlas_item={}",
                atlas.isEmpty(),
                atlas.isEmpty() ? "<empty>" : atlas.getItem());
        if (atlas.getItem() instanceof MapAtlasItem) {
            if (pinOnly) {
                player.level().playSound(null, player, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 1.7F, 2f);
            }
            if (pinOnly && MapAtlasesMod.MOONLIGHT && MoonlightCompat.maybePlaceMarkerInFront(player, atlas)) {
                return;
            }

            MapAtlasItem.syncAndOpenGui(player, atlas, source, hand, lecternPos, pinOnly);
        }
    }
}
