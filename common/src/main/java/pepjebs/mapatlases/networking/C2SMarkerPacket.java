package pepjebs.mapatlases.networking;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MoonlightCompat;
import pepjebs.mapatlases.mixin.MapItemSavedDataAccessor;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Optional;

public class C2SMarkerPacket implements Message {

    private final ColumnPos pos;
    private final String mapId;
    private final String name;
    private final int index;

    public C2SMarkerPacket(FriendlyByteBuf buf) {
        this.pos = fromLong(buf.readLong());
        this.mapId = buf.readUtf();
        this.name = buf.readOptional(FriendlyByteBuf::readUtf).orElse(null);
        this.index = buf.readVarInt();
    }

    public ColumnPos fromLong(long combinedValue) {
        var x = (int) (combinedValue);
        var z = (int) (combinedValue >>> 32);
        return new ColumnPos(x,z);
    }

    public C2SMarkerPacket(ColumnPos pos, String map, @Nullable String name, int index) {
        this.pos = pos;
        this.mapId = map;
        this.name = name;
        this.index = index;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeUtf(mapId);
        buf.writeOptional(Optional.ofNullable(name), FriendlyByteBuf::writeUtf);
        buf.writeVarInt(index);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (!(context.getSender() instanceof ServerPlayer player)) return;

        Level level = player.level();
        MapItemSavedData data = level.getMapData(new MapId(MapAtlasesAccessUtils.findMapIntFromString(mapId)));

        if (data instanceof MapItemSavedDataAccessor d) {

            double d0 = pos.x() + 0.5D;
            double d1 = pos.z() + 0.5D;
            String str = MapAtlasesConfig.pinMarkerId.get();
            if (!str.isEmpty()) {
                Identifier id = Identifier.parse(str);

                MutableComponent literal = name == null ? null : Component.literal(name);
                if (id.getNamespace().equals("minecraft")) {
                    Optional<Holder<MapDecorationType>> opt = getVanillaDecorationType(id.getPath());
                    opt.ifPresent(type -> d.invokeAddDecoration(
                            type
                            , level,
                            "pin_" + pos,
                            d0, d1, 180.0D, literal));
                } else {
                    MoonlightCompat.addDecoration(data, new BlockPos(pos.x(), 0, pos.z()), id, literal, index);
                }
            }
        }

    }

    private static Optional<Holder<MapDecorationType>> getVanillaDecorationType(String path) {
        return Optional.ofNullable(switch (path) {
            case "player" -> MapDecorationTypes.PLAYER;
            case "frame" -> MapDecorationTypes.FRAME;
            case "red_marker" -> MapDecorationTypes.RED_MARKER;
            case "blue_marker" -> MapDecorationTypes.BLUE_MARKER;
            case "target_x" -> MapDecorationTypes.TARGET_X;
            case "target_point" -> MapDecorationTypes.TARGET_POINT;
            case "player_off_map" -> MapDecorationTypes.PLAYER_OFF_MAP;
            case "player_off_limits" -> MapDecorationTypes.PLAYER_OFF_LIMITS;
            case "mansion" -> MapDecorationTypes.WOODLAND_MANSION;
            case "monument" -> MapDecorationTypes.OCEAN_MONUMENT;
            case "banner_white" -> MapDecorationTypes.WHITE_BANNER;
            case "banner_orange" -> MapDecorationTypes.ORANGE_BANNER;
            case "banner_magenta" -> MapDecorationTypes.MAGENTA_BANNER;
            case "banner_light_blue" -> MapDecorationTypes.LIGHT_BLUE_BANNER;
            case "banner_yellow" -> MapDecorationTypes.YELLOW_BANNER;
            case "banner_lime" -> MapDecorationTypes.LIME_BANNER;
            case "banner_pink" -> MapDecorationTypes.PINK_BANNER;
            case "banner_gray" -> MapDecorationTypes.GRAY_BANNER;
            case "banner_light_gray" -> MapDecorationTypes.LIGHT_GRAY_BANNER;
            case "banner_cyan" -> MapDecorationTypes.CYAN_BANNER;
            case "banner_purple" -> MapDecorationTypes.PURPLE_BANNER;
            case "banner_blue" -> MapDecorationTypes.BLUE_BANNER;
            case "banner_brown" -> MapDecorationTypes.BROWN_BANNER;
            case "banner_green" -> MapDecorationTypes.GREEN_BANNER;
            case "banner_red" -> MapDecorationTypes.RED_BANNER;
            case "banner_black" -> MapDecorationTypes.BLACK_BANNER;
            case "red_x" -> MapDecorationTypes.RED_X;
            case "village_desert" -> MapDecorationTypes.DESERT_VILLAGE;
            case "village_plains" -> MapDecorationTypes.PLAINS_VILLAGE;
            case "village_savanna" -> MapDecorationTypes.SAVANNA_VILLAGE;
            case "village_snowy" -> MapDecorationTypes.SNOWY_VILLAGE;
            case "village_taiga" -> MapDecorationTypes.TAIGA_VILLAGE;
            case "jungle_temple" -> MapDecorationTypes.JUNGLE_TEMPLE;
            case "swamp_hut" -> MapDecorationTypes.SWAMP_HUT;
            case "trial_chambers" -> MapDecorationTypes.TRIAL_CHAMBERS;
            default -> null;
        });
    }
}
